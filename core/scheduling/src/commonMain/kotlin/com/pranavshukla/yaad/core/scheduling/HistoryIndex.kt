package com.pranavshukla.yaad.core.scheduling

import com.pranavshukla.yaad.core.model.Occurrence
import com.pranavshukla.yaad.core.model.OccurrenceStatus
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.model.ReminderId
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Finds what was recorded for an occurrence. Reminders that follow the device's zone are matched by the local time
 * they were due (so "today at 9:00" stays done after a flight); pinned ones, and records made before local times
 * were kept, by instant. History is stored to the millisecond, so instants match at that precision.
 */
class HistoryIndex(history: List<Occurrence>) {
    private val byInstant = history.associateBy { it.reminderId to it.due.toEpochMilliseconds() }
    private val byLocal: Map<Pair<ReminderId, LocalDateTime>, Occurrence> =
        history.mapNotNull { o -> o.dueLocal?.let { (o.reminderId to it) to o } }.toMap()

    fun record(reminder: Reminder, due: Instant, zone: TimeZone): Occurrence? {
        val exact = byInstant[reminder.id to due.toEpochMilliseconds()]
        if (reminder.schedule.timeZoneId != null) return exact
        return byLocal[reminder.id to due.toLocalDateTime(zone)] ?: exact?.takeIf { it.dueLocal == null }
    }

    fun status(reminder: Reminder, due: Instant, zone: TimeZone): OccurrenceStatus =
        record(reminder, due, zone)?.status ?: OccurrenceStatus.Pending
}
