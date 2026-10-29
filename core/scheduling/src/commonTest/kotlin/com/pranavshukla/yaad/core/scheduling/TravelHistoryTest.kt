package com.pranavshukla.yaad.core.scheduling

import com.pranavshukla.yaad.core.model.Frequency
import com.pranavshukla.yaad.core.model.RecurrenceRule
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.model.ReminderId
import com.pranavshukla.yaad.core.model.Schedule
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * The gap logged in Phase 6: "every day at 9:00" taken in Kolkata, then the phone flies to London. The record of
 * that morning's dose must still match, or it would show as overdue and start nagging again.
 */
class TravelHistoryTest {
    private val kolkata = TimeZone.of("Asia/Kolkata")
    private val london = TimeZone.of("Europe/London")
    private val pill = Reminder(
        ReminderId("pill"), "Pill",
        Schedule(LocalDateTime(2026, 10, 27, 9, 0), RecurrenceRule(Frequency.Daily())),
        nagInterval = 30.minutes,
    )

    @Test
    fun aDoneRecordFollowsTheReminderAcrossTimeZones() {
        val due = Instant.parse("2026-10-28T03:30:00Z") // 9:00 in Kolkata
        val taken = AlertAction.Done.toOccurrence(pill.id, due, due + 5.minutes, kolkata, LocalTime(9, 0))
        // Later that day, in London (UTC+0): 9:00 local was 09:00 UTC.
        val inLondon = Instant.parse("2026-10-28T12:00:00Z")
        val agenda = Agenda.build(listOf(pill), listOf(taken), inLondon, london)
        assertEquals(listOf(true), agenda.today.map { it.isHandled }, "today's 9:00 is the one taken")
        assertEquals(emptyList(), agenda.overdue.filter { it.date == agenda.today.first().date })
        val alerts = AlertPlanner.plan(listOf(pill), listOf(taken), inLondon, london, PlanSettings(horizon = 6.minutes * 10))
        assertEquals(emptyList(), alerts, "no nagging about a dose already taken")
    }
}
