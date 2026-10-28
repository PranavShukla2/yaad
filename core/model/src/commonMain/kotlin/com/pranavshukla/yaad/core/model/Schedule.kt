package com.pranavshukla.yaad.core.model

import kotlinx.datetime.LocalDateTime

/**
 * When a reminder is due: its first occurrence as a local date and time, and how it repeats (null for once).
 *
 * The time is local wall-clock time, turned into an instant with the time zone the schedule is read in, so a reminder
 * at 9:00 stays at 9:00 after travel or a DST change. [timeZoneId] pins it to one IANA zone instead (for example, an
 * appointment booked in another city); null follows the device's current zone.
 */
data class Schedule(
    val start: LocalDateTime,
    val rule: RecurrenceRule? = null,
    val timeZoneId: String? = null,
) {
    init {
        rule?.let { require(it.frequency.interval >= 1) { "Interval must be at least 1, was ${it.frequency.interval}" } }
    }
}
