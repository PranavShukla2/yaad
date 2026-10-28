package com.pranavshukla.yaad.core.recurrence

import com.pranavshukla.yaad.core.model.Frequency
import com.pranavshukla.yaad.core.model.RecurrenceEnd
import kotlinx.datetime.DayOfWeek.FRIDAY
import kotlinx.datetime.DayOfWeek.MONDAY
import kotlinx.datetime.DayOfWeek.THURSDAY
import kotlinx.datetime.DayOfWeek.TUESDAY
import kotlinx.datetime.DayOfWeek.WEDNESDAY
import kotlin.test.Test
import kotlin.test.assertEquals

class WeeklyTest {
    @Test
    fun chosenWeekdaysStartingMidWeek() {
        // 28 October 2026 is a Wednesday.
        val s = schedule(at(2026, 10, 28), Frequency.Weekly(days = setOf(MONDAY, WEDNESDAY, FRIDAY)))
        assertEquals(
            listOf(date(2026, 10, 28), date(2026, 10, 30), date(2026, 11, 2), date(2026, 11, 4)),
            s.firstDates(4),
        )
    }

    @Test
    fun weekdaysBeforeTheStartAreSkippedInTheFirstWeek() {
        // Starts on Thursday, so Tuesday of that week is not an occurrence.
        val s = schedule(at(2026, 10, 29), Frequency.Weekly(days = setOf(TUESDAY, THURSDAY)))
        assertEquals(listOf(date(2026, 10, 29), date(2026, 11, 3), date(2026, 11, 5)), s.firstDates(3))
    }

    @Test
    fun everyOtherWeek() {
        // Monday 5 January 2026; weeks 1, 3, 5... on Tuesday and Thursday.
        val s = schedule(at(2026, 1, 5), Frequency.Weekly(2, setOf(TUESDAY, THURSDAY)))
        assertEquals(
            listOf(date(2026, 1, 6), date(2026, 1, 8), date(2026, 1, 20), date(2026, 1, 22)),
            s.firstDates(4),
        )
    }

    @Test
    fun noDaysMeansTheStartWeekday() {
        val s = schedule(at(2026, 12, 25), Frequency.Weekly())
        assertEquals(listOf(date(2026, 12, 25), date(2027, 1, 1), date(2027, 1, 8)), s.firstDates(3))
    }

    @Test
    fun countIncludesEveryChosenDay() {
        val s = schedule(at(2026, 10, 28), Frequency.Weekly(days = setOf(MONDAY, FRIDAY)), RecurrenceEnd.AfterCount(3))
        assertEquals(listOf(date(2026, 10, 30), date(2026, 11, 2), date(2026, 11, 6)), s.firstDates(10))
    }

    @Test
    fun nextAfterInALaterWeekKeepsTheCount() {
        val s = schedule(at(2026, 1, 5), Frequency.Weekly(2, setOf(TUESDAY, THURSDAY)), RecurrenceEnd.AfterCount(5))
        // Occurrences: 6, 8, 20, 22 Jan, 3 Feb.
        assertEquals(utc("2026-02-03T03:30:00Z"), s.nextAfter(utc("2026-01-23T00:00:00Z"), Kolkata))
        assertEquals(null, s.nextAfter(utc("2026-02-03T03:30:00Z"), Kolkata))
    }
}
