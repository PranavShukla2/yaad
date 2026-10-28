package com.pranavshukla.yaad.core.scheduling

import com.pranavshukla.yaad.core.model.Occurrence
import com.pranavshukla.yaad.core.model.OccurrenceStatus
import com.pranavshukla.yaad.core.model.ReminderId
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** What the user can do from a notification, even on the lock screen. Names are stored in intents: keep them. */
enum class AlertAction { Done, Skip, Snooze10Minutes, Snooze1Hour, SnoozeTomorrow }

/** The occurrence record an action leaves. "Tomorrow" means tomorrow at [morning], local time. */
fun AlertAction.toOccurrence(
    reminderId: ReminderId,
    due: Instant,
    now: Instant,
    zone: TimeZone,
    morning: LocalTime,
): Occurrence {
    val status = when (this) {
        AlertAction.Done -> OccurrenceStatus.Done(now)
        AlertAction.Skip -> OccurrenceStatus.Skipped(now)
        AlertAction.Snooze10Minutes -> OccurrenceStatus.Snoozed(now + 10.minutes)
        AlertAction.Snooze1Hour -> OccurrenceStatus.Snoozed(now + 1.hours)
        AlertAction.SnoozeTomorrow -> {
            val tomorrow = now.toLocalDateTime(zone).date.plus(1, DateTimeUnit.DAY)
            OccurrenceStatus.Snoozed(LocalDateTime(tomorrow, morning).toInstant(zone))
        }
    }
    return Occurrence(reminderId, due, status)
}
