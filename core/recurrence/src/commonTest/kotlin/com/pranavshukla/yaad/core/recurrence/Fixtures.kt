package com.pranavshukla.yaad.core.recurrence

import com.pranavshukla.yaad.core.model.Frequency
import com.pranavshukla.yaad.core.model.RecurrenceEnd
import com.pranavshukla.yaad.core.model.RecurrenceRule
import com.pranavshukla.yaad.core.model.Schedule
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

internal val Kolkata = TimeZone.of("Asia/Kolkata")
internal val NewYork = TimeZone.of("America/New_York")
internal val London = TimeZone.of("Europe/London")

internal fun at(year: Int, month: Int, day: Int, hour: Int = 9, minute: Int = 0) =
    LocalDateTime(year, month, day, hour, minute)

internal fun date(year: Int, month: Int, day: Int) = LocalDate(year, month, day)

internal fun schedule(start: LocalDateTime, frequency: Frequency, end: RecurrenceEnd = RecurrenceEnd.Never) =
    Schedule(start, RecurrenceRule(frequency, end))

/** The first [n] occurrences as local date-times in [zone]. */
internal fun Schedule.firstLocal(n: Int, zone: TimeZone = Kolkata): List<LocalDateTime> =
    occurrences(zone).take(n).map { it.toLocalDateTime(zone) }.toList()

internal fun Schedule.firstDates(n: Int, zone: TimeZone = Kolkata): List<LocalDate> = firstLocal(n, zone).map { it.date }

internal fun utc(iso: String): Instant = Instant.parse(iso)
