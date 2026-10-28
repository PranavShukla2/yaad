package com.pranavshukla.yaad.core.recurrence

import com.pranavshukla.yaad.core.model.Frequency
import com.pranavshukla.yaad.core.model.RecurrenceEnd
import com.pranavshukla.yaad.core.model.Schedule
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** Series stop here at the latest, so a rule with no end never loops forever. */
private const val LastYear = 9999

/** The zone this schedule is read in: its pinned zone if it has one, otherwise the device's [deviceZone]. */
fun Schedule.zoneOr(deviceZone: TimeZone): TimeZone = timeZoneId?.let { TimeZone.of(it) } ?: deviceZone

/**
 * Every occurrence, in order, as instants in [zone].
 *
 * Local times that a DST change skips (2:30 on a spring-forward night) fire just after the gap, at 3:30; local times
 * that happen twice fire once, at the first of the two.
 */
fun Schedule.occurrences(zone: TimeZone): Sequence<Instant> = occurrencesFrom(null, zone)

/** The first occurrence strictly after [instant], or null if the series has ended. */
fun Schedule.nextAfter(instant: Instant, zone: TimeZone): Instant? =
    occurrencesFrom(instant, zone).firstOrNull { it > instant }

/** The occurrences in [from, until). */
fun Schedule.occurrencesBetween(from: Instant, until: Instant, zone: TimeZone): List<Instant> =
    occurrencesFrom(from, zone).dropWhile { it < from }.takeWhile { it < until }.toList()

/** Occurrences from a little before [near] (or from the first one), skipping the periods before it without walking them. */
private fun Schedule.occurrencesFrom(near: Instant?, zone: TimeZone): Sequence<Instant> {
    val rule = rule ?: return sequenceOf(start.toInstant(zone))
    return when (val frequency = rule.frequency) {
        is Frequency.Minutely -> elapsed(frequency.interval.minutes, rule.end, near, zone)
        is Frequency.Hourly -> elapsed(frequency.interval.hours, rule.end, near, zone)
        else -> calendar(CalendarPattern.of(frequency, start.date), rule.end, near?.toLocalDateTime(zone)?.date, zone)
    }
}

/** Minute and hour rules: fixed steps of elapsed time from the first occurrence. */
private fun Schedule.elapsed(step: Duration, end: RecurrenceEnd, near: Instant?, zone: TimeZone): Sequence<Instant> {
    val first = start.toInstant(zone)
    val stepSeconds = step.inWholeSeconds
    val firstIndex = if (near == null || near <= first) 0L else (near - first).inWholeSeconds / stepSeconds - 1
    return generateSequence(firstIndex.coerceAtLeast(0)) { it + 1 }
        .map { index -> index to first + (stepSeconds * index).seconds }
        .takeWhile { (index, instant) ->
            val date = instant.toLocalDateTime(zone).date
            date.year <= LastYear && when (end) {
                RecurrenceEnd.Never -> true
                is RecurrenceEnd.AfterCount -> index < end.count
                is RecurrenceEnd.OnDate -> date <= end.date
            }
        }
        .map { it.second }
}

/** Day rules and longer: dates from the [pattern] at the start's local time. */
private fun Schedule.calendar(
    pattern: CalendarPattern,
    end: RecurrenceEnd,
    near: LocalDate?,
    zone: TimeZone,
): Sequence<Instant> = sequence {
    val startDate = start.date
    // Start one period early: an occurrence on the day before [near] may still be after it as an instant.
    var period = ((near?.let(pattern::periodOf) ?: 0L) - 1).coerceAtLeast(0)
    var index = occurrencesBefore(period, pattern, startDate)
    while (true) {
        for (date in pattern.datesIn(period)) {
            if (date < startDate) continue
            if (date.year > LastYear) return@sequence
            when (end) {
                RecurrenceEnd.Never -> Unit
                is RecurrenceEnd.AfterCount -> if (index >= end.count) return@sequence
                is RecurrenceEnd.OnDate -> if (date > end.date) return@sequence
            }
            index++
            yield(LocalDateTime(date, start.time).toInstant(zone))
        }
        period++
    }
}

/** How many occurrences fall in the periods before [period]: every period is full except possibly the first. */
private fun occurrencesBefore(period: Long, pattern: CalendarPattern, startDate: LocalDate): Long {
    if (period == 0L) return 0
    val inFirst = pattern.datesIn(0).count { it >= startDate }
    return inFirst + (period - 1) * pattern.datesIn(1).size
}
