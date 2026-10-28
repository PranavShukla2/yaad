package com.pranavshukla.yaad.core.model

import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

class ReminderTest {
    private fun reminder(earlyAlerts: List<Duration> = emptyList(), nag: Duration? = null) = Reminder(
        id = ReminderId("r1"),
        title = "Rent",
        schedule = Schedule(LocalDateTime(2026, 11, 5, 9, 0)),
        earlyAlerts = earlyAlerts,
        nagInterval = nag,
    )

    @Test
    fun earlyAlertsComeBeforeTheDueTime() {
        reminder(earlyAlerts = listOf(3.days, 1.days))
        assertFailsWith<IllegalArgumentException> { reminder(earlyAlerts = listOf(Duration.ZERO)) }
        assertFailsWith<IllegalArgumentException> { reminder(earlyAlerts = listOf((-1).days)) }
    }

    @Test
    fun nagIntervalIsPositive() {
        reminder(nag = 10.minutes)
        assertFailsWith<IllegalArgumentException> { reminder(nag = Duration.ZERO) }
    }
}
