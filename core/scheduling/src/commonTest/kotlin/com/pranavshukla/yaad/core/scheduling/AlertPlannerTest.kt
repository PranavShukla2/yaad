package com.pranavshukla.yaad.core.scheduling

import com.pranavshukla.yaad.core.model.Frequency
import com.pranavshukla.yaad.core.model.Occurrence
import com.pranavshukla.yaad.core.model.OccurrenceStatus
import com.pranavshukla.yaad.core.model.RecurrenceRule
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.model.ReminderId
import com.pranavshukla.yaad.core.model.Schedule
import com.pranavshukla.yaad.core.scheduling.AlertKind.Due
import com.pranavshukla.yaad.core.scheduling.AlertKind.Early
import com.pranavshukla.yaad.core.scheduling.AlertKind.Nag
import com.pranavshukla.yaad.core.scheduling.AlertKind.Snoozed
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class AlertPlannerTest {
    private val kolkata = TimeZone.of("Asia/Kolkata")

    /** Wednesday 28 October 2026, 15:30 IST. */
    private val now = Instant.parse("2026-10-28T10:00:00Z")

    private fun ist(day: Int, hour: Int, minute: Int = 0, month: Int = 10) =
        LocalDateTime(2026, month, day, hour, minute)

    private fun instant(iso: String) = Instant.parse(iso)

    private fun reminder(
        id: String,
        start: LocalDateTime,
        rule: RecurrenceRule? = null,
        early: List<Duration> = emptyList(),
        nag: Duration? = null,
        allDay: Boolean = false,
        archived: Boolean = false,
    ) = Reminder(ReminderId(id), id, Schedule(start, rule), allDay = allDay, earlyAlerts = early, nagInterval = nag,
        archived = archived)

    private fun plan(
        reminders: List<Reminder>,
        history: List<Occurrence> = emptyList(),
        settings: PlanSettings = PlanSettings(),
        at: Instant = now,
        zone: TimeZone = kolkata,
    ) = AlertPlanner.plan(reminders, history, at, zone, settings)

    private fun List<Alert>.summary() = map { "${it.reminderId.value} ${it.kind} ${it.fireAt}" }

    @Test
    fun aOneTimeReminderGetsOneAlertAtItsTime() {
        val alerts = plan(listOf(reminder("call", ist(28, 19))))
        assertEquals(listOf("call Due 2026-10-28T13:30:00Z"), alerts.summary())
    }

    @Test
    fun earlyAlertsComeFirstAndPastOnesAreDropped() {
        // Due 29 October 9:00 IST; three days before has already passed.
        val r = reminder("rent", ist(29, 9), early = listOf(3.days, 12.hours, 1.hours))
        assertEquals(
            listOf("rent Early 2026-10-28T15:30:00Z", "rent Early 2026-10-29T02:30:00Z", "rent Due 2026-10-29T03:30:00Z"),
            plan(listOf(r)).summary(),
        )
    }

    @Test
    fun aPastOneTimeReminderGetsNothing() {
        assertEquals(emptyList(), plan(listOf(reminder("old", ist(27, 9)))))
    }

    @Test
    fun doneAndSkippedOccurrencesAreNotAlerted() {
        val daily = reminder("pill", ist(28, 21), RecurrenceRule(Frequency.Daily()))
        val history = listOf(
            Occurrence(daily.id, instant("2026-10-28T15:30:00Z"), OccurrenceStatus.Done(now)),
            Occurrence(daily.id, instant("2026-10-29T15:30:00Z"), OccurrenceStatus.Skipped(now)),
        )
        val alerts = plan(listOf(daily), history, PlanSettings(horizon = 3.days))
        assertEquals(listOf("pill Due 2026-10-30T15:30:00Z"), alerts.summary())
    }

    @Test
    fun aSnoozedOccurrenceAlertsAtTheSnoozeTime() {
        val r = reminder("call", ist(28, 15))
        val history = listOf(Occurrence(r.id, instant("2026-10-28T09:30:00Z"), OccurrenceStatus.Snoozed(now + 10.minutes)))
        assertEquals(listOf("call Snoozed 2026-10-28T10:10:00Z"), plan(listOf(r), history).summary())
    }

    @Test
    fun snoozingAnEarlyAlertKeepsTheDueAlert() {
        val r = reminder("emi", ist(28, 18), early = listOf(3.hours))
        val history = listOf(Occurrence(r.id, instant("2026-10-28T12:30:00Z"), OccurrenceStatus.Snoozed(now + 1.hours)))
        assertEquals(
            listOf("emi Snoozed 2026-10-28T11:00:00Z", "emi Due 2026-10-28T12:30:00Z"),
            plan(listOf(r), history).summary(),
        )
    }

    @Test
    fun naggingRepeatsUntilTheNextOccurrence() {
        val r = reminder("med", ist(28, 16), RecurrenceRule(Frequency.Daily(2)), nag = 4.hours)
        val alerts = plan(listOf(r), settings = PlanSettings(horizon = 2.days + 1.hours))
        assertEquals(
            listOf(
                "med Due 2026-10-28T10:30:00Z",
                "med Nag 2026-10-28T14:30:00Z", "med Nag 2026-10-28T18:30:00Z", "med Nag 2026-10-28T22:30:00Z",
                "med Nag 2026-10-29T02:30:00Z", "med Nag 2026-10-29T06:30:00Z",
                // 24 hours after the due time nagging stops, even though the next dose is two days away.
                "med Due 2026-10-30T10:30:00Z",
            ),
            alerts.summary(),
        )
    }

    @Test
    fun naggingContinuesForAMissedOccurrenceAndStopsWhenDone() {
        val r = reminder("bill", ist(28, 14), nag = 1.hours)
        assertEquals(
            listOf("2026-10-28T10:30:00Z", "2026-10-28T11:30:00Z"),
            plan(listOf(r), settings = PlanSettings(horizon = 2.hours)).map { it.fireAt.toString() },
        )
        val done = listOf(Occurrence(r.id, instant("2026-10-28T08:30:00Z"), OccurrenceStatus.Done(now)))
        assertEquals(emptyList(), plan(listOf(r), done))
    }

    @Test
    fun allDayItemsAlertAtTheChosenTime() {
        val r = reminder("passport", ist(30, 0), allDay = true, early = listOf(1.days))
        val alerts = plan(listOf(r), settings = PlanSettings(allDayTime = LocalTime(8, 30)))
        assertEquals(listOf("passport Early 2026-10-29T03:00:00Z", "passport Due 2026-10-30T03:00:00Z"), alerts.summary())
        assertEquals(setOf(instant("2026-10-29T18:30:00Z")), alerts.map { it.occurrenceDue }.toSet())
    }

    @Test
    fun theHorizonAndLimitKeepTheEarliest() {
        val a = reminder("a", ist(28, 16), RecurrenceRule(Frequency.Hourly(1)))
        val b = reminder("b", ist(28, 16, 30), RecurrenceRule(Frequency.Hourly(1)))
        val alerts = plan(listOf(a, b), settings = PlanSettings(limit = 5))
        assertEquals(
            listOf("a", "b", "a", "b", "a"),
            alerts.map { it.reminderId.value },
        )
        assertEquals(10.days, PlanSettings().horizon)
        assertEquals(24, plan(listOf(a), settings = PlanSettings(horizon = 1.days)).size)
    }

    @Test
    fun archivedRemindersAreSilent() {
        assertEquals(emptyList(), plan(listOf(reminder("old", ist(29, 9), archived = true))))
    }

    @Test
    fun idsAreStableAndDistinct() {
        val r = reminder("rent", ist(29, 9), RecurrenceRule(Frequency.Daily()), early = listOf(1.hours), nag = 30.minutes)
        val first = plan(listOf(r))
        assertEquals(first, plan(listOf(r)))
        assertEquals(first.size, first.map { it.id }.toSet().size)
        assertEquals("rent@1793244600000/due", first.first { it.kind == Due }.id)
        assertEquals("rent@1793244600000/early3600", first.first { it.kind == Early }.id)
        assertEquals("rent@1793244600000/nag1800", first.first { it.kind == Nag }.id)
    }

    @Test
    fun travellingMovesAlertsToTheNewLocalTime() {
        val r = reminder("walk", ist(29, 7), RecurrenceRule(Frequency.Daily()))
        val london = plan(listOf(r), zone = TimeZone.of("Europe/London"), settings = PlanSettings(horizon = 1.days))
        assertEquals(listOf("walk Due 2026-10-29T07:00:00Z"), london.summary())
    }

    @Test
    fun snoozedAlertsUseTheirOwnKind() {
        val r = reminder("x", ist(28, 15))
        val history = listOf(Occurrence(r.id, instant("2026-10-28T09:30:00Z"), OccurrenceStatus.Snoozed(now + 5.minutes)))
        assertEquals(Snoozed, plan(listOf(r), history).single().kind)
    }
}
