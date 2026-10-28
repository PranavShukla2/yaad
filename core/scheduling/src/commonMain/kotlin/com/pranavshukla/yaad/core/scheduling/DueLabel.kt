package com.pranavshukla.yaad.core.scheduling

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.daysUntil

/** How to name a due date relative to today; the UI turns it into words in the user's language. */
sealed interface DayLabel {
    data object Today : DayLabel
    data object Tomorrow : DayLabel
    data object Yesterday : DayLabel

    /** Within the next six days: "Friday". */
    data class Weekday(val day: DayOfWeek) : DayLabel

    /** Anything further: "4 Nov". */
    data class Date(val date: LocalDate) : DayLabel

    companion object {
        fun of(date: LocalDate, today: LocalDate): DayLabel = when (val days = today.daysUntil(date)) {
            0 -> Today
            1 -> Tomorrow
            -1 -> Yesterday
            in 2..6 -> Weekday(date.dayOfWeek)
            else -> Date(date)
        }
    }
}

object TimeLabel {
    /** "9:05 AM" or "09:05". */
    fun format(time: LocalTime, twentyFourHour: Boolean): String {
        val minute = time.minute.toString().padStart(2, '0')
        if (twentyFourHour) return "${time.hour.toString().padStart(2, '0')}:$minute"
        val hour = (time.hour + 11) % 12 + 1
        return "$hour:$minute ${if (time.hour < 12) "AM" else "PM"}"
    }
}
