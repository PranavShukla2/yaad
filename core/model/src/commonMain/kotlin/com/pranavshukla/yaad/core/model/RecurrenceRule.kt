package com.pranavshukla.yaad.core.model

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/**
 * How a reminder repeats, as in § 3.1 of the plan: every N minutes, hours, days, weeks (on chosen weekdays), months
 * (on a day, on the nth weekday, or on the last day) or years, ending never, on a date, or after N times.
 *
 * Minute and hour rules count elapsed time, so "every 2 hours" stays 2 hours apart across a DST change. Day rules and
 * longer follow the local calendar, so "every day at 9:00" stays at 9:00 local time.
 */
data class RecurrenceRule(
    val frequency: Frequency,
    val end: RecurrenceEnd = RecurrenceEnd.Never,
)

sealed interface Frequency {
    /** Every [interval] units; at least 1. */
    val interval: Int

    data class Minutely(override val interval: Int) : Frequency
    data class Hourly(override val interval: Int) : Frequency
    data class Daily(override val interval: Int = 1) : Frequency

    /** Every [interval] weeks on [days]; an empty set means the weekday of the first occurrence. Weeks start Monday. */
    data class Weekly(override val interval: Int = 1, val days: Set<DayOfWeek> = emptySet()) : Frequency

    data class Monthly(override val interval: Int = 1, val on: MonthlyOn) : Frequency

    /** Every [interval] years on the first occurrence's date; 29 February falls on 28 February in other years. */
    data class Yearly(override val interval: Int = 1) : Frequency
}

/** Which day of the month a monthly rule falls on. */
sealed interface MonthlyOn {
    /** Day [day] (1–31); in shorter months, the last day of the month. */
    data class Day(val day: Int) : MonthlyOn {
        init {
            require(day in 1..31) { "Day of month must be 1–31, was $day" }
        }
    }

    /** The [ordinal]th [weekday] (1–4), or the last one when [ordinal] is [LAST]. */
    data class NthWeekday(val ordinal: Int, val weekday: DayOfWeek) : MonthlyOn {
        init {
            require(ordinal in 1..4 || ordinal == LAST) { "Ordinal must be 1–4 or LAST, was $ordinal" }
        }

        companion object {
            const val LAST = -1
        }
    }

    data object LastDay : MonthlyOn
}

sealed interface RecurrenceEnd {
    data object Never : RecurrenceEnd

    /** The last occurrence is on or before [date] (in the reminder's local calendar). */
    data class OnDate(val date: LocalDate) : RecurrenceEnd

    /** Exactly [count] occurrences, counting the first. */
    data class AfterCount(val count: Int) : RecurrenceEnd {
        init {
            require(count >= 1) { "Count must be at least 1, was $count" }
        }
    }
}
