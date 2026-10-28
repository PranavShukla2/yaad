package com.pranavshukla.yaad.core.model

import kotlin.time.Instant

/** What happened to one occurrence of a reminder. Occurrences with no record are pending. */
sealed interface OccurrenceStatus {
    data object Pending : OccurrenceStatus
    data class Done(val at: Instant) : OccurrenceStatus
    data class Skipped(val at: Instant) : OccurrenceStatus
    data class Snoozed(val until: Instant) : OccurrenceStatus
}

/**
 * One due date of a reminder, identified by [reminderId] and the instant it was due. A missed occurrence of a
 * repeating reminder stays pending (overdue) until it is handled.
 */
data class Occurrence(
    val reminderId: ReminderId,
    val due: Instant,
    val status: OccurrenceStatus = OccurrenceStatus.Pending,
    val paidAmount: Money? = null,
    val note: String = "",
)
