package com.pranavshukla.yaad.core.model

import kotlinx.datetime.LocalDateTime
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
 *
 * @property dueLocal the wall-clock time it was due where it was recorded. Reminders that follow the device's zone
 * are matched by it, so a record still belongs to "today at 9:00" after the phone changes time zone.
 */
data class Occurrence(
    val reminderId: ReminderId,
    val due: Instant,
    val status: OccurrenceStatus = OccurrenceStatus.Pending,
    val paidAmount: Money? = null,
    val note: String = "",
    val dueLocal: LocalDateTime? = null,
)
