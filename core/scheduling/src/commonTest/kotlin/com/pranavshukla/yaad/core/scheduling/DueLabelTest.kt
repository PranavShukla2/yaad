package com.pranavshukla.yaad.core.scheduling

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class DueLabelTest {
    private val today = LocalDate(2026, 10, 28) // a Wednesday

    @Test
    fun nearbyDaysHaveNames() {
        assertEquals(DayLabel.Today, DayLabel.of(today, today))
        assertEquals(DayLabel.Tomorrow, DayLabel.of(LocalDate(2026, 10, 29), today))
        assertEquals(DayLabel.Yesterday, DayLabel.of(LocalDate(2026, 10, 27), today))
    }

    @Test
    fun theComingWeekUsesWeekdays() {
        assertEquals(DayLabel.Weekday(DayOfWeek.FRIDAY), DayLabel.of(LocalDate(2026, 10, 30), today))
        assertEquals(DayLabel.Weekday(DayOfWeek.TUESDAY), DayLabel.of(LocalDate(2026, 11, 3), today))
        assertEquals(DayLabel.Date(LocalDate(2026, 11, 4)), DayLabel.of(LocalDate(2026, 11, 4), today))
        assertEquals(DayLabel.Date(LocalDate(2026, 10, 20)), DayLabel.of(LocalDate(2026, 10, 20), today))
    }

    @Test
    fun timesIn12And24HourStyle() {
        assertEquals("9:05 AM", TimeLabel.format(LocalTime(9, 5), twentyFourHour = false))
        assertEquals("12:00 PM", TimeLabel.format(LocalTime(12, 0), twentyFourHour = false))
        assertEquals("12:30 AM", TimeLabel.format(LocalTime(0, 30), twentyFourHour = false))
        assertEquals("19:00", TimeLabel.format(LocalTime(19, 0), twentyFourHour = true))
        assertEquals("09:05", TimeLabel.format(LocalTime(9, 5), twentyFourHour = true))
    }
}
