package com.pranavshukla.yaad.core.model

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RecurrenceRuleTest {
    @Test
    fun dayOfMonthMustBeACalendarDay() {
        MonthlyOn.Day(1)
        MonthlyOn.Day(31)
        assertFailsWith<IllegalArgumentException> { MonthlyOn.Day(0) }
        assertFailsWith<IllegalArgumentException> { MonthlyOn.Day(32) }
    }

    @Test
    fun nthWeekdayIsFirstToFourthOrLast() {
        MonthlyOn.NthWeekday(4, DayOfWeek.MONDAY)
        MonthlyOn.NthWeekday(MonthlyOn.NthWeekday.LAST, DayOfWeek.FRIDAY)
        assertFailsWith<IllegalArgumentException> { MonthlyOn.NthWeekday(5, DayOfWeek.MONDAY) }
        assertFailsWith<IllegalArgumentException> { MonthlyOn.NthWeekday(0, DayOfWeek.MONDAY) }
    }

    @Test
    fun countMustBePositive() {
        RecurrenceEnd.AfterCount(1)
        assertFailsWith<IllegalArgumentException> { RecurrenceEnd.AfterCount(0) }
    }

    @Test
    fun intervalMustBePositive() {
        val start = LocalDateTime(2026, 1, 1, 9, 0)
        Schedule(start, RecurrenceRule(Frequency.Daily(1)))
        assertFailsWith<IllegalArgumentException> { Schedule(start, RecurrenceRule(Frequency.Daily(0))) }
        assertFailsWith<IllegalArgumentException> { Schedule(start, RecurrenceRule(Frequency.Minutely(-5))) }
    }
}
