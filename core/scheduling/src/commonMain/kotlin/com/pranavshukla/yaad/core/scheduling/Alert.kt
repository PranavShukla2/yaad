package com.pranavshukla.yaad.core.scheduling

import com.pranavshukla.yaad.core.model.ReminderId
import kotlin.time.Duration
import kotlin.time.Instant

enum class AlertKind { Early, Due, Nag, Snoozed }

/**
 * One notification that should be scheduled: for the occurrence of [reminderId] due at [occurrenceDue], fire at
 * [fireAt]. [offset] is how long before the due time (early alerts) or after it (nags).
 */
data class Alert(
    val reminderId: ReminderId,
    val occurrenceDue: Instant,
    val fireAt: Instant,
    val kind: AlertKind,
    val offset: Duration = Duration.ZERO,
) {
    /** Names the occurrence; every alert for it shares one notification slot, so they replace each other. */
    val occurrenceKey: String get() = "${reminderId.value}@${occurrenceDue.toEpochMilliseconds()}"

    /**
     * Stable across runs for the same reminder, occurrence and offset, so re-planning never duplicates an alert.
     * Snoozed alerts include their time, so snoozing again replaces the old one.
     */
    val id: String
        get() = "$occurrenceKey/" + when (kind) {
            AlertKind.Early -> "early${offset.inWholeSeconds}"
            AlertKind.Due -> "due"
            AlertKind.Nag -> "nag${offset.inWholeSeconds}"
            AlertKind.Snoozed -> "snoozed${fireAt.toEpochMilliseconds()}"
        }
}
