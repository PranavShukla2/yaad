package com.pranavshukla.yaad.core.recurrence

import com.pranavshukla.yaad.core.model.MonthlyOn
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number

/** Calendar arithmetic on whole days and months, without time zones. */
internal fun LocalDate.plusDays(days: Long): LocalDate = LocalDate.fromEpochDays(toEpochDays() + days)

internal fun LocalDate.daysUntil(other: LocalDate): Long = other.toEpochDays() - toEpochDays()

internal fun isLeapYear(year: Int): Boolean = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

internal fun monthLength(year: Int, month: Int): Int = when (month) {
    2 -> if (isLeapYear(year)) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
}

/** Months since year 0, so month arithmetic is plain integer arithmetic. */
internal val LocalDate.monthIndex: Long get() = year * 12L + (month.number - 1)

internal fun yearOf(monthIndex: Long): Int = monthIndex.floorDiv(12L).toInt()

internal fun monthOf(monthIndex: Long): Int = monthIndex.mod(12L).toInt() + 1

/** The Monday on or before this date. Weeks start on Monday, as in ISO 8601. */
internal fun LocalDate.weekStart(): LocalDate = plusDays(-(dayOfWeek.isoDayNumber - 1).toLong())

/** The date [on] picks in the given month. */
internal fun MonthlyOn.dateIn(year: Int, month: Int): LocalDate {
    val length = monthLength(year, month)
    return when (this) {
        is MonthlyOn.Day -> LocalDate(year, month, minOf(day, length))
        MonthlyOn.LastDay -> LocalDate(year, month, length)
        is MonthlyOn.NthWeekday -> if (ordinal == MonthlyOn.NthWeekday.LAST) {
            val last = LocalDate(year, month, length)
            LocalDate(year, month, length - daysBetween(weekday, last.dayOfWeek))
        } else {
            val first = LocalDate(year, month, 1)
            LocalDate(year, month, 1 + daysBetween(first.dayOfWeek, weekday) + 7 * (ordinal - 1))
        }
    }
}

/** Days from [from] forward to the next [to] (0–6). */
private fun daysBetween(from: DayOfWeek, to: DayOfWeek): Int = (to.isoDayNumber - from.isoDayNumber + 7) % 7
