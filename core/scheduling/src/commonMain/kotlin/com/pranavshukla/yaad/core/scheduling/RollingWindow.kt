package com.pranavshukla.yaad.core.scheduling

import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** The alerts that fit the platform's limit, and when to ask the user to open Yaad if some did not fit. */
data class Window(val alerts: List<Alert>, val refillAt: Instant?)

/**
 * iOS keeps at most 64 pending notifications. When the plan is longer, keep the earliest and use the last slot for a
 * notice just after them ("open Yaad to keep reminders coming"), so reminders never stop silently if the app is not
 * opened and background refresh does not run.
 */
object RollingWindow {
    fun fit(plan: List<Alert>, capacity: Int): Window {
        if (plan.size <= capacity) return Window(plan, null)
        val kept = plan.take(capacity - 1)
        return Window(kept, kept.last().fireAt + 1.minutes)
    }
}
