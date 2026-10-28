package com.pranavshukla.yaad.core.recurrence

import com.pranavshukla.yaad.core.model.Frequency
import com.pranavshukla.yaad.core.model.Schedule
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/** New York moves clocks forward 8 March 2026 (2:00 → 3:00) and back 1 November 2026 (2:00 → 1:00). */
class TimeZoneTest {
    @Test
    fun nineOClockStaysNineAcrossSpringForward() {
        val s = schedule(at(2026, 3, 7), Frequency.Daily())
        val local = s.firstLocal(3, NewYork)
        assertEquals(listOf(at(2026, 3, 7), at(2026, 3, 8), at(2026, 3, 9)), local)
        val instants = s.occurrences(NewYork).take(2).toList()
        assertEquals(23.hours, instants[1] - instants[0])
    }

    @Test
    fun nineOClockStaysNineAcrossFallBack() {
        val instants = schedule(at(2026, 10, 31), Frequency.Daily()).occurrences(NewYork).take(2).toList()
        assertEquals(listOf(utc("2026-10-31T13:00:00Z"), utc("2026-11-01T14:00:00Z")), instants)
    }

    @Test
    fun aTimeSkippedBySpringForwardFiresAfterTheGap() {
        // 2:30 does not exist on 8 March; the reminder fires at 3:30 EDT, then returns to 2:30.
        val instants = schedule(at(2026, 3, 7, 2, 30), Frequency.Daily()).occurrences(NewYork).take(3).toList()
        assertEquals(
            listOf(utc("2026-03-07T07:30:00Z"), utc("2026-03-08T07:30:00Z"), utc("2026-03-09T06:30:00Z")),
            instants,
        )
    }

    @Test
    fun aTimeRepeatedByFallBackFiresOnce() {
        // 1:30 happens twice on 1 November; the reminder fires once, at the first (EDT) one.
        val instants = schedule(at(2026, 10, 31, 1, 30), Frequency.Daily()).occurrences(NewYork).take(3).toList()
        assertEquals(
            listOf(utc("2026-10-31T05:30:00Z"), utc("2026-11-01T05:30:00Z"), utc("2026-11-02T06:30:00Z")),
            instants,
        )
    }

    @Test
    fun hourlyCountsElapsedTimeThroughFallBack() {
        val s = schedule(at(2026, 11, 1, 0, 0), Frequency.Hourly(1))
        val day = s.occurrencesBetween(utc("2026-11-01T04:00:00Z"), utc("2026-11-02T05:00:00Z"), NewYork)
        assertEquals(25, day.size, "the local day of 1 November is 25 hours long")
        assertEquals(setOf(1.hours), day.zipWithNext { a, b -> b - a }.toSet())
    }

    @Test
    fun everyNinetyMinutes() {
        val instants = schedule(at(2026, 1, 1, 8, 0), Frequency.Minutely(90)).occurrences(Kolkata).take(3).toList()
        assertEquals(listOf(0.minutes, 90.minutes, 180.minutes), instants.map { it - instants[0] })
    }

    @Test
    fun travellingKeepsTheLocalTime() {
        // The same "every day at 9:00" read on a phone in India, then in London.
        val s = schedule(at(2026, 1, 1), Frequency.Daily())
        assertEquals(utc("2026-01-05T03:30:00Z"), s.nextAfter(utc("2026-01-05T00:00:00Z"), Kolkata))
        assertEquals(utc("2026-01-05T09:00:00Z"), s.nextAfter(utc("2026-01-05T00:00:00Z"), London))
    }

    @Test
    fun aPinnedZoneIgnoresTheDeviceZone() {
        val s = Schedule(at(2026, 1, 5, 15, 0), timeZoneId = "America/New_York")
        val deviceZone = TimeZone.of("Asia/Kolkata")
        assertEquals(listOf(utc("2026-01-05T20:00:00Z")), s.occurrences(s.zoneOr(deviceZone)).toList())
    }
}
