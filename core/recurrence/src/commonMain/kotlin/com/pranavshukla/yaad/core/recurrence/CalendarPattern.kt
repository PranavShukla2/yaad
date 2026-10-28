package com.pranavshukla.yaad.core.recurrence

import com.pranavshukla.yaad.core.model.Frequency
import com.pranavshukla.yaad.core.model.MonthlyOn
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number

/**
 * A day-or-longer rule as numbered periods: period k is the k-th day, week, month or year of the series counting
 * from the first occurrence's period. Every period has the same number of dates, so occurrences can be counted
 * without walking the series.
 */
internal sealed class CalendarPattern {
    /** The dates in period [k], in order, before dropping any that fall before the start. */
    abstract fun datesIn(k: Long): List<LocalDate>

    /** The period that contains [date] (negative before the series). */
    abstract fun periodOf(date: LocalDate): Long

    class Daily(private val start: LocalDate, private val interval: Int) : CalendarPattern() {
        override fun datesIn(k: Long) = listOf(start.plusDays(k * interval))
        override fun periodOf(date: LocalDate) = start.daysUntil(date).floorDiv(interval.toLong())
    }

    class Weekly(start: LocalDate, private val interval: Int, days: Collection<kotlinx.datetime.DayOfWeek>) :
        CalendarPattern() {
        private val firstWeek = start.weekStart()
        private val offsets = days.ifEmpty { listOf(start.dayOfWeek) }.map { it.isoDayNumber - 1L }.sorted()

        override fun datesIn(k: Long) = offsets.map { firstWeek.plusDays(7 * interval * k + it) }
        override fun periodOf(date: LocalDate) = (firstWeek.daysUntil(date.weekStart()) / 7).floorDiv(interval.toLong())
    }

    class Monthly(start: LocalDate, private val interval: Int, private val on: MonthlyOn) : CalendarPattern() {
        private val firstMonth = start.monthIndex

        override fun datesIn(k: Long): List<LocalDate> {
            val month = firstMonth + k * interval
            return listOf(on.dateIn(yearOf(month), monthOf(month)))
        }

        override fun periodOf(date: LocalDate) = (date.monthIndex - firstMonth).floorDiv(interval.toLong())
    }

    /** On the start's month and day; 29 February becomes 28 February in common years. */
    class Yearly(private val start: LocalDate, private val interval: Int) : CalendarPattern() {
        override fun datesIn(k: Long): List<LocalDate> {
            val year = (start.year + k * interval).toInt()
            val month = start.month.number
            return listOf(LocalDate(year, month, minOf(start.day, monthLength(year, month))))
        }

        override fun periodOf(date: LocalDate) = (date.year - start.year).toLong().floorDiv(interval.toLong())
    }

    companion object {
        fun of(frequency: Frequency, start: LocalDate): CalendarPattern = when (frequency) {
            is Frequency.Daily -> Daily(start, frequency.interval)
            is Frequency.Weekly -> Weekly(start, frequency.interval, frequency.days)
            is Frequency.Monthly -> Monthly(start, frequency.interval, frequency.on)
            is Frequency.Yearly -> Yearly(start, frequency.interval)
            is Frequency.Minutely, is Frequency.Hourly -> error("$frequency counts elapsed time, not calendar days")
        }
    }
}
