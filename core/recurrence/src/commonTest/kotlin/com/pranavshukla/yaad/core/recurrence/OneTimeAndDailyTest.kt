package com.pranavshukla.yaad.core.recurrence

import com.pranavshukla.yaad.core.model.Frequency
import com.pranavshukla.yaad.core.model.RecurrenceEnd
import com.pranavshukla.yaad.core.model.Schedule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OneTimeAndDailyTest {
    @Test
    fun oneTimeReminderOccursOnce() {
        val s = Schedule(at(2026, 11, 5, 19, 0))
        assertEquals(listOf(utc("2026-11-05T13:30:00Z")), s.occurrences(Kolkata).toList())
        assertEquals(utc("2026-11-05T13:30:00Z"), s.nextAfter(utc("2026-11-01T00:00:00Z"), Kolkata))
        assertNull(s.nextAfter(utc("2026-11-05T13:30:00Z"), Kolkata), "next is strictly after")
    }

    @Test
    fun everyDayAtNine() {
        val s = schedule(at(2026, 12, 30), Frequency.Daily())
        assertEquals(
            listOf(at(2026, 12, 30), at(2026, 12, 31), at(2027, 1, 1), at(2027, 1, 2)),
            s.firstLocal(4),
        )
    }

    @Test
    fun everyThreeDaysAcrossTheYearEnd() {
        val s = schedule(at(2026, 12, 27), Frequency.Daily(3))
        assertEquals(listOf(date(2026, 12, 27), date(2026, 12, 30), date(2027, 1, 2), date(2027, 1, 5)), s.firstDates(4))
    }

    @Test
    fun endsAfterACount() {
        val s = schedule(at(2026, 1, 1), Frequency.Daily(), RecurrenceEnd.AfterCount(10))
        assertEquals(10, s.occurrences(Kolkata).count())
        assertEquals(at(2026, 1, 10), s.firstLocal(20).last())
    }

    @Test
    fun endsOnADateInclusive() {
        val s = schedule(at(2026, 1, 1), Frequency.Daily(2), RecurrenceEnd.OnDate(date(2026, 1, 7)))
        assertEquals(listOf(date(2026, 1, 1), date(2026, 1, 3), date(2026, 1, 5), date(2026, 1, 7)), s.firstDates(10))
    }

    @Test
    fun nextAfterJumpsAheadAndKeepsTheCount() {
        // Ten daily occurrences from 1 January at 9:00 IST (03:30 UTC).
        val s = schedule(at(2026, 1, 1), Frequency.Daily(), RecurrenceEnd.AfterCount(10))
        assertEquals(utc("2026-01-10T03:30:00Z"), s.nextAfter(utc("2026-01-09T04:00:00Z"), Kolkata))
        assertNull(s.nextAfter(utc("2026-01-10T03:30:00Z"), Kolkata))
    }

    @Test
    fun nextAfterIsFastForOldReminders() {
        val s = schedule(at(1990, 1, 1), Frequency.Daily())
        assertEquals(utc("2026-10-29T03:30:00Z"), s.nextAfter(utc("2026-10-28T12:00:00Z"), Kolkata))
    }

    @Test
    fun occurrencesBetweenIsHalfOpen() {
        val s = schedule(at(2026, 1, 1), Frequency.Daily())
        val found = s.occurrencesBetween(utc("2026-01-03T03:30:00Z"), utc("2026-01-06T03:30:00Z"), Kolkata)
        assertEquals(
            listOf(utc("2026-01-03T03:30:00Z"), utc("2026-01-04T03:30:00Z"), utc("2026-01-05T03:30:00Z")),
            found,
        )
    }
}
