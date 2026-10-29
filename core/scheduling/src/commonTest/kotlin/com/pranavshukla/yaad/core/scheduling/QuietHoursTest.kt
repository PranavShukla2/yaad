package com.pranavshukla.yaad.core.scheduling

import com.pranavshukla.yaad.core.model.Priority
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.model.ReminderId
import com.pranavshukla.yaad.core.model.Schedule
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class QuietHoursTest {
    private val zone = TimeZone.of("Asia/Kolkata")
    private val now = Instant.parse("2026-10-28T10:00:00Z") // 15:30 IST
    private val night = QuietHours(LocalTime(22, 0), LocalTime(7, 0))

    private fun plan(r: Reminder) = AlertPlanner.plan(listOf(r), emptyList(), now, zone, PlanSettings(horizon = 2.days, quietHours = night))

    @Test
    fun anAlertInQuietHoursWaitsUntilTheyEnd() {
        val early = Reminder(ReminderId("bill"), "Bill", Schedule(LocalDateTime(2026, 10, 29, 9, 0)), earlyAlerts = listOf(10.hours))
        // 10 hours before 9:00 is 23:00, inside 22:00–07:00: it moves to 07:00.
        assertEquals(
            listOf(Instant.parse("2026-10-29T01:30:00Z"), Instant.parse("2026-10-29T03:30:00Z")),
            plan(early).map { it.fireAt },
        )
    }

    @Test
    fun highPriorityStillRings() {
        val urgent = Reminder(ReminderId("u"), "Flight", Schedule(LocalDateTime(2026, 10, 28, 23, 30)), priority = Priority.High)
        assertEquals(listOf(Instant.parse("2026-10-28T18:00:00Z")), plan(urgent).map { it.fireAt })
    }

    @Test
    fun outsideQuietHoursNothingMoves() {
        val call = Reminder(ReminderId("c"), "Call", Schedule(LocalDateTime(2026, 10, 28, 19, 0)))
        assertEquals(listOf(Instant.parse("2026-10-28T13:30:00Z")), plan(call).map { it.fireAt })
        assertEquals(true, night.contains(LocalTime(23, 59)))
        assertEquals(true, night.contains(LocalTime(6, 59)))
        assertEquals(false, night.contains(LocalTime(7, 0)))
        assertEquals(true, QuietHours(LocalTime(13, 0), LocalTime(14, 0)).contains(LocalTime(13, 30)))
    }
}
