package com.pranavshukla.yaad.core.recurrence

import com.pranavshukla.yaad.core.model.Frequency
import com.pranavshukla.yaad.core.model.MonthlyOn
import com.pranavshukla.yaad.core.model.MonthlyOn.NthWeekday.Companion.LAST
import com.pranavshukla.yaad.core.model.RecurrenceEnd
import kotlinx.datetime.DayOfWeek.FRIDAY
import kotlinx.datetime.DayOfWeek.MONDAY
import kotlinx.datetime.DayOfWeek.TUESDAY
import kotlin.test.Test
import kotlin.test.assertEquals

class MonthlyAndYearlyTest {
    @Test
    fun the31stFallsOnTheLastDayOfShortMonthsWithoutDrifting() {
        val s = schedule(at(2026, 1, 31), Frequency.Monthly(on = MonthlyOn.Day(31)))
        assertEquals(
            listOf(date(2026, 1, 31), date(2026, 2, 28), date(2026, 3, 31), date(2026, 4, 30), date(2026, 5, 31)),
            s.firstDates(5),
        )
    }

    @Test
    fun the30thIn29DayFebruary() {
        val s = schedule(at(2028, 1, 30), Frequency.Monthly(on = MonthlyOn.Day(30)))
        assertEquals(listOf(date(2028, 1, 30), date(2028, 2, 29), date(2028, 3, 30)), s.firstDates(3))
    }

    @Test
    fun aDayEarlierThanTheStartBeginsNextMonth() {
        // Rent on the 5th, added on 20 November.
        val s = schedule(at(2026, 11, 20), Frequency.Monthly(on = MonthlyOn.Day(5)))
        assertEquals(listOf(date(2026, 12, 5), date(2027, 1, 5)), s.firstDates(2))
    }

    @Test
    fun lastDayOfTheMonth() {
        val s = schedule(at(2027, 12, 1), Frequency.Monthly(on = MonthlyOn.LastDay))
        assertEquals(listOf(date(2027, 12, 31), date(2028, 1, 31), date(2028, 2, 29), date(2028, 3, 31)), s.firstDates(4))
    }

    @Test
    fun secondTuesday() {
        val s = schedule(at(2026, 1, 1), Frequency.Monthly(on = MonthlyOn.NthWeekday(2, TUESDAY)))
        assertEquals(listOf(date(2026, 1, 13), date(2026, 2, 10), date(2026, 3, 10)), s.firstDates(3))
    }

    @Test
    fun lastFriday() {
        val s = schedule(at(2026, 1, 1), Frequency.Monthly(on = MonthlyOn.NthWeekday(LAST, FRIDAY)))
        assertEquals(listOf(date(2026, 1, 30), date(2026, 2, 27), date(2026, 3, 27), date(2026, 4, 24)), s.firstDates(4))
    }

    @Test
    fun fourthMondayInAMonthStartingOnMonday() {
        // June 2026 starts on a Monday.
        val s = schedule(at(2026, 6, 1), Frequency.Monthly(on = MonthlyOn.NthWeekday(4, MONDAY)))
        assertEquals(date(2026, 6, 22), s.firstDates(1).single())
    }

    @Test
    fun quarterlyAcrossTheYearEnd() {
        val s = schedule(at(2026, 11, 15), Frequency.Monthly(3, MonthlyOn.Day(15)))
        assertEquals(listOf(date(2026, 11, 15), date(2027, 2, 15), date(2027, 5, 15)), s.firstDates(3))
    }

    @Test
    fun twelveEmisEndAfterCount() {
        val s = schedule(at(2026, 11, 5), Frequency.Monthly(on = MonthlyOn.Day(5)), RecurrenceEnd.AfterCount(12))
        val dates = s.firstDates(20)
        assertEquals(12, dates.size)
        assertEquals(date(2027, 10, 5), dates.last())
    }

    @Test
    fun leapDayBirthdayFallsOn28FebruaryInOtherYears() {
        val s = schedule(at(2028, 2, 29), Frequency.Yearly())
        assertEquals(
            listOf(date(2028, 2, 29), date(2029, 2, 28), date(2030, 2, 28), date(2031, 2, 28), date(2032, 2, 29)),
            s.firstDates(5),
        )
    }

    @Test
    fun everyTwoYears() {
        val s = schedule(at(2026, 12, 31), Frequency.Yearly(2), RecurrenceEnd.OnDate(date(2032, 12, 31)))
        assertEquals(listOf(date(2026, 12, 31), date(2028, 12, 31), date(2030, 12, 31), date(2032, 12, 31)), s.firstDates(9))
    }

    @Test
    fun nextAfterManyYearsLater() {
        val s = schedule(at(1990, 1, 31), Frequency.Monthly(on = MonthlyOn.Day(31)))
        // 31 Jan 2026 is past at 10:00 IST; February has 28 days.
        assertEquals(utc("2026-02-28T03:30:00Z"), s.nextAfter(utc("2026-01-31T04:30:00Z"), Kolkata))
    }
}
