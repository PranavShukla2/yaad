package com.pranavshukla.yaad.core.scheduling

import com.pranavshukla.yaad.core.model.Frequency
import com.pranavshukla.yaad.core.model.Occurrence
import com.pranavshukla.yaad.core.model.OccurrenceStatus
import com.pranavshukla.yaad.core.model.RecurrenceRule
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.model.ReminderId
import com.pranavshukla.yaad.core.model.Schedule
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class AgendaTest {
    private val zone = TimeZone.of("Asia/Kolkata")

    /** Wednesday 28 October 2026, 15:30 IST. */
    private val now = Instant.parse("2026-10-28T10:00:00Z")

    private fun reminder(id: String, start: LocalDateTime, rule: RecurrenceRule? = null, allDay: Boolean = false) =
        Reminder(ReminderId(id), id, Schedule(start, rule), allDay = allDay)

    private fun agenda(reminders: List<Reminder>, history: List<Occurrence> = emptyList()) =
        Agenda.build(reminders, history, now, zone)

    private fun List<AgendaItem>.names() = map { "${it.reminder.id.value} ${it.due}" }

    @Test
    fun todayHoldsWhatIsDueLaterToday() {
        val a = agenda(listOf(reminder("call", LocalDateTime(2026, 10, 28, 19, 0)), reminder("tomorrow", LocalDateTime(2026, 10, 29, 9, 0))))
        assertEquals(listOf("call 2026-10-28T13:30:00Z"), a.today.names())
        assertEquals(listOf(LocalDate(2026, 10, 29)), a.upcoming.keys.toList())
        assertEquals(emptyList(), a.overdue)
    }

    @Test
    fun aMissedOccurrenceIsOverdueUntilHandled() {
        val daily = reminder("pill", LocalDateTime(2026, 10, 26, 9, 0), RecurrenceRule(Frequency.Daily()))
        val missed = agenda(listOf(daily))
        assertEquals(
            listOf("pill 2026-10-26T03:30:00Z", "pill 2026-10-27T03:30:00Z", "pill 2026-10-28T03:30:00Z"),
            missed.overdue.names(),
        )
        val handled = listOf(
            Occurrence(daily.id, Instant.parse("2026-10-26T03:30:00Z"), OccurrenceStatus.Done(now)),
            Occurrence(daily.id, Instant.parse("2026-10-27T03:30:00Z"), OccurrenceStatus.Skipped(now)),
        )
        assertEquals(listOf("pill 2026-10-28T03:30:00Z"), agenda(listOf(daily), handled).overdue.names())
    }

    @Test
    fun overdueKeepsTheTenMostRecentPerReminder() {
        val hourly = reminder("water", LocalDateTime(2026, 10, 27, 9, 0), RecurrenceRule(Frequency.Hourly(1)))
        val overdue = agenda(listOf(hourly)).overdue
        assertEquals(10, overdue.size)
        assertEquals(Instant.parse("2026-10-28T09:30:00Z"), overdue.last().due)
    }

    @Test
    fun anAllDayItemIsDueAllDayAndOverdueOnlyAfterIt() {
        val today = reminder("passport", LocalDateTime(2026, 10, 28, 0, 0), allDay = true)
        val yesterday = reminder("licence", LocalDateTime(2026, 10, 27, 0, 0), allDay = true)
        val a = agenda(listOf(today, yesterday))
        assertEquals(listOf("passport"), a.today.map { it.reminder.id.value })
        assertEquals(listOf("licence"), a.overdue.map { it.reminder.id.value })
    }

    @Test
    fun handledItemsStayOnTheirDayMarkedDone() {
        val call = reminder("call", LocalDateTime(2026, 10, 28, 19, 0))
        val done = Occurrence(call.id, Instant.parse("2026-10-28T13:30:00Z"), OccurrenceStatus.Done(now))
        val item = agenda(listOf(call), listOf(done)).today.single()
        assertEquals(OccurrenceStatus.Done(now), item.status)
    }

    @Test
    fun upcomingIsGroupedByDayForThirtyDays() {
        val weekly = reminder("gym", LocalDateTime(2026, 10, 29, 7, 0), RecurrenceRule(Frequency.Weekly()))
        val upcoming = agenda(listOf(weekly)).upcoming
        assertEquals(
            listOf(LocalDate(2026, 10, 29), LocalDate(2026, 11, 5), LocalDate(2026, 11, 12), LocalDate(2026, 11, 19), LocalDate(2026, 11, 26)),
            upcoming.keys.toList(),
        )
    }

    @Test
    fun aSnoozedItemShowsAsSnoozedAtItsDueTime() {
        val call = reminder("call", LocalDateTime(2026, 10, 28, 14, 0))
        val snoozed = Occurrence(call.id, Instant.parse("2026-10-28T08:30:00Z"), OccurrenceStatus.Snoozed(now + kotlin.time.Duration.parse("1h")))
        val a = agenda(listOf(call), listOf(snoozed))
        assertEquals(snoozed.status, a.overdue.single().status)
    }

    @Test
    fun nextDueIsTheEarliestUnhandledOccurrence() {
        val daily = reminder("pill", LocalDateTime(2026, 10, 27, 9, 0), RecurrenceRule(Frequency.Daily()))
        assertEquals(Instant.parse("2026-10-27T03:30:00Z"), Agenda.nextDue(daily, emptyList(), now, zone))
        val done = listOf(
            Occurrence(daily.id, Instant.parse("2026-10-27T03:30:00Z"), OccurrenceStatus.Done(now)),
            Occurrence(daily.id, Instant.parse("2026-10-28T03:30:00Z"), OccurrenceStatus.Done(now)),
        )
        assertEquals(Instant.parse("2026-10-29T03:30:00Z"), Agenda.nextDue(daily, done, now, zone))
        assertNull(Agenda.nextDue(reminder("old", LocalDateTime(2026, 1, 1, 9, 0)), listOf(Occurrence(ReminderId("old"), Instant.parse("2026-01-01T03:30:00Z"), OccurrenceStatus.Done(now))), now, zone))
    }

    @Test
    fun anAllDayItemIsNotOverdueOnItsDay() {
        // Found on the emulator: the Money list called today's all-day bill overdue.
        val bill = reminder("bill", LocalDateTime(2026, 10, 28, 0, 0), allDay = true)
        val due = Instant.parse("2026-10-27T18:30:00Z") // midnight IST on 28 October
        assertEquals(false, Agenda.isOverdue(bill, due, now, zone))
        assertEquals(true, Agenda.isOverdue(bill, due, Instant.parse("2026-10-28T18:30:00Z"), zone))
        val call = reminder("call", LocalDateTime(2026, 10, 28, 14, 0))
        assertEquals(true, Agenda.isOverdue(call, Instant.parse("2026-10-28T08:30:00Z"), now, zone))
    }

    @Test
    fun aPastBirthdayIsNotOverdue() {
        val asha = LifeTemplates.birthday(ReminderId("asha"), "Asha", LocalDate(1990, 10, 20))
        val a = agenda(listOf(asha))
        assertEquals(emptyList(), a.overdue)
        assertEquals(null, Agenda.nextDue(asha, emptyList(), now, zone)?.takeIf { it < now }, "the next one is next year")
    }
}
