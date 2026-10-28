package com.pranavshukla.yaad.core.scheduling

import com.pranavshukla.yaad.core.model.ReminderId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class RollingWindowTest {
    private val start = Instant.parse("2026-10-28T10:00:00Z")
    private fun alerts(n: Int) = (0 until n).map { Alert(ReminderId("r$it"), start + it.hours, start + it.hours, AlertKind.Due) }

    @Test
    fun aPlanThatFitsIsKeptWhole() {
        val window = RollingWindow.fit(alerts(64), capacity = 64)
        assertEquals(64, window.alerts.size)
        assertNull(window.refillAt)
    }

    @Test
    fun aLongerPlanKeepsTheEarliestAndAsksToBeRefilledAfterTheLast() {
        val plan = alerts(200)
        val window = RollingWindow.fit(plan, capacity = 64)
        assertEquals(plan.take(63), window.alerts, "one slot is left for the refill notice")
        assertEquals(plan[62].fireAt + 1.minutes, window.refillAt)
    }
}
