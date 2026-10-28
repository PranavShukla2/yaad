package com.pranavshukla.yaad.core.recurrence

import com.pranavshukla.yaad.core.model.Frequency
import com.pranavshukla.yaad.core.model.MonthlyOn
import com.pranavshukla.yaad.core.model.RecurrenceEnd
import com.pranavshukla.yaad.core.model.RecurrenceRule
import com.pranavshukla.yaad.core.model.Schedule
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Thousands of random rules, checked against an independent oracle that walks the calendar one day at a time and
 * tests each date against the rule's definition. Seeded, so a failure always reproduces.
 */
class RecurrencePropertyTest {
    private val zones = listOf(
        "UTC", "Asia/Kolkata", "America/New_York", "Europe/London", "Europe/Berlin", "Australia/Sydney",
        "Asia/Tokyo", "America/Los_Angeles",
    ).map { TimeZone.of(it) }

    @Test
    fun calendarRulesMatchTheOracle() {
        val random = Random(20261028)
        repeat(1500) { case ->
            val schedule = randomCalendarSchedule(random)
            val zone = zones.random(random)
            val windowEnd = schedule.start.date.plusDays(windowDays(schedule.rule!!.frequency))
            val expected = oracleDates(schedule, windowEnd)
            val actual = schedule.occurrences(zone).map { it.toLocalDateTime(zone) }
                .takeWhile { it.date <= windowEnd }.toList()
            assertEquals(expected, actual.map { it.date }, "case $case: $schedule")
            actual.forEach { assertAtStartTime(schedule.start, it, zone, "case $case: $schedule") }
        }
    }

    @Test
    fun nextAfterAndRangesAgreeWithTheFullSeries() {
        val random = Random(7)
        repeat(1500) { case ->
            val schedule = if (random.nextInt(4) == 0) randomElapsedSchedule(random) else randomCalendarSchedule(random)
            val zone = zones.random(random)
            val series = schedule.occurrences(zone).take(60).toList()
            if (series.isEmpty()) return@repeat
            assertTrue(series.zipWithNext().all { (a, b) -> a < b }, "case $case: strictly increasing")
            repeat(5) {
                val probe = randomInstantNear(series, random)
                val expected = series.firstOrNull { it > probe }
                if (expected != null || series.size < 60) {
                    assertEquals(expected, schedule.nextAfter(probe, zone), "case $case: next after $probe, $schedule")
                }
                val until = probe + random.nextInt(1, 24 * 90).hours
                if (series.size < 60 || until < series.last()) {
                    assertEquals(
                        series.filter { it >= probe && it < until },
                        schedule.occurrencesBetween(probe, until, zone),
                        "case $case: between $probe and $until, $schedule",
                    )
                }
            }
        }
    }

    @Test
    fun elapsedRulesStepByTheirInterval() {
        val random = Random(42)
        repeat(1000) { case ->
            val schedule = randomElapsedSchedule(random)
            val zone = zones.random(random)
            val frequency = schedule.rule!!.frequency
            val step = if (frequency is Frequency.Minutely) frequency.interval.minutes else frequency.interval.hours
            val series = schedule.occurrences(zone).take(50).toList()
            assertEquals(schedule.start.toInstant(zone), series.first(), "case $case")
            assertTrue(series.zipWithNext().all { (a, b) -> b - a == step }, "case $case: $schedule")
            when (val end = schedule.rule!!.end) {
                is RecurrenceEnd.AfterCount -> assertEquals(minOf(end.count, 50), series.size, "case $case")
                is RecurrenceEnd.OnDate -> assertTrue(series.all { it.toLocalDateTime(zone).date <= end.date })
                RecurrenceEnd.Never -> assertEquals(50, series.size)
            }
        }
    }

    /** The occurrence is at the start's wall-clock time, unless a DST gap skipped that time on that day. */
    private fun assertAtStartTime(start: LocalDateTime, actual: LocalDateTime, zone: TimeZone, message: String) {
        if (actual.time == start.time) return
        val wanted = LocalDateTime(actual.date, start.time)
        val roundTrip = wanted.toInstant(zone).toLocalDateTime(zone)
        assertTrue(roundTrip != wanted && roundTrip == actual, "$message: $actual is not at ${start.time}")
    }

    // The oracle: test every day from the start against the rule's definition.

    private fun oracleDates(schedule: Schedule, windowEnd: LocalDate): List<LocalDate> {
        val rule = schedule.rule!!
        val result = mutableListOf<LocalDate>()
        var date = schedule.start.date
        while (date <= windowEnd) {
            when (val end = rule.end) {
                is RecurrenceEnd.AfterCount -> if (result.size == end.count) break
                is RecurrenceEnd.OnDate -> if (date > end.date) break
                RecurrenceEnd.Never -> Unit
            }
            if (matches(rule.frequency, schedule.start.date, date)) result += date
            date = date.plusDays(1)
        }
        return result
    }

    private fun matches(frequency: Frequency, start: LocalDate, date: LocalDate): Boolean {
        val length = monthLength(date.year, date.month.number)
        return when (frequency) {
            is Frequency.Daily -> start.daysUntil(date) % frequency.interval == 0L
            is Frequency.Weekly -> {
                val days = frequency.days.ifEmpty { setOf(start.dayOfWeek) }
                val weeks = start.weekStart().daysUntil(date.weekStart()) / 7
                date.dayOfWeek in days && weeks % frequency.interval == 0L
            }
            is Frequency.Monthly -> {
                val months = date.monthIndex - start.monthIndex
                months % frequency.interval == 0L && when (val on = frequency.on) {
                    is MonthlyOn.Day -> date.day == minOf(on.day, length)
                    MonthlyOn.LastDay -> date.day == length
                    is MonthlyOn.NthWeekday -> date.dayOfWeek == on.weekday && if (on.ordinal == MonthlyOn.NthWeekday.LAST) {
                        date.day + 7 > length
                    } else {
                        (date.day - 1) / 7 + 1 == on.ordinal
                    }
                }
            }
            is Frequency.Yearly -> (date.year - start.year) % frequency.interval == 0 &&
                date.month == start.month && date.day == minOf(start.day, length)
            else -> error("not a calendar rule")
        }
    }

    private fun windowDays(frequency: Frequency): Long = when (frequency) {
        is Frequency.Daily, is Frequency.Weekly -> 3 * 366
        is Frequency.Monthly -> 12 * 366
        else -> 30 * 366
    }

    // Random inputs.

    private fun randomStart(random: Random): LocalDateTime {
        val year = random.nextInt(1995, 2036)
        val month = random.nextInt(1, 13)
        val day = random.nextInt(1, monthLength(year, month) + 1)
        return LocalDateTime(year, month, day, random.nextInt(24), random.nextInt(4) * 15)
    }

    private fun randomEnd(random: Random, start: LocalDateTime): RecurrenceEnd = when (random.nextInt(4)) {
        0 -> RecurrenceEnd.AfterCount(random.nextInt(1, 41))
        1 -> RecurrenceEnd.OnDate(start.date.plusDays(random.nextLong(0, 1500)))
        else -> RecurrenceEnd.Never
    }

    private fun randomCalendarSchedule(random: Random): Schedule {
        val start = randomStart(random)
        val frequency = when (random.nextInt(4)) {
            0 -> Frequency.Daily(random.nextInt(1, 11))
            1 -> Frequency.Weekly(
                random.nextInt(1, 5),
                DayOfWeek.entries.filter { random.nextInt(3) == 0 }.toSet(),
            )
            2 -> Frequency.Monthly(
                random.nextInt(1, 13),
                when (random.nextInt(3)) {
                    0 -> MonthlyOn.Day(if (random.nextBoolean()) random.nextInt(28, 32) else random.nextInt(1, 32))
                    1 -> MonthlyOn.LastDay
                    else -> MonthlyOn.NthWeekday(
                        if (random.nextInt(5) == 0) MonthlyOn.NthWeekday.LAST else random.nextInt(1, 5),
                        DayOfWeek.entries.random(random),
                    )
                },
            )
            else -> Frequency.Yearly(random.nextInt(1, 6))
        }
        return Schedule(start, RecurrenceRule(frequency, randomEnd(random, start)))
    }

    private fun randomElapsedSchedule(random: Random): Schedule {
        val start = randomStart(random)
        val frequency = if (random.nextBoolean()) {
            Frequency.Minutely(random.nextInt(1, 301))
        } else {
            Frequency.Hourly(random.nextInt(1, 49))
        }
        return Schedule(start, RecurrenceRule(frequency, randomEnd(random, start)))
    }

    private fun randomInstantNear(series: List<Instant>, random: Random): Instant {
        val base = series.random(random)
        return base + random.nextLong(-3 * 24 * 60, 3 * 24 * 60).minutes
    }
}
