package com.pranavshukla.yaad.core.scheduling

import com.pranavshukla.yaad.core.model.Occurrence
import com.pranavshukla.yaad.core.model.OccurrenceStatus
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.recurrence.nextAfter
import com.pranavshukla.yaad.core.recurrence.occurrencesBetween
import com.pranavshukla.yaad.core.recurrence.zoneOr
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * @property allDayTime when all-day items alert, on their day (and on the days of their early alerts).
 * @property horizon how far ahead to plan; platforms re-plan well before it runs out.
 * @property limit at most this many alerts, the earliest first (iOS keeps 64 pending notifications).
 * @property maxNagsPerOccurrence a cap on repeated alerts for one occurrence.
 */
data class PlanSettings(
    val allDayTime: LocalTime = LocalTime(9, 0),
    val horizon: Duration = 10.days,
    val limit: Int = Int.MAX_VALUE,
    val maxNagsPerOccurrence: Int = 48,
)

/**
 * Decides which alerts should exist. A pure function: the same inputs always give the same list, so platform code
 * only adds what is missing and removes what is stale.
 *
 * - Early alerts and the due alert fire for pending occurrences, if still in the future.
 * - Done and skipped occurrences get nothing.
 * - A snoozed occurrence alerts at the snooze time, and still at its due time if that is later.
 * - Nagging repeats every [Reminder.nagInterval] after the due (or snooze) time until the occurrence is handled,
 *   stopping at the next occurrence or after 24 hours.
 */
object AlertPlanner {
    /** Nagging for one occurrence stops after this long, even with no later occurrence. */
    private val NagWindow = 24.hours

    fun plan(
        reminders: List<Reminder>,
        history: List<Occurrence>,
        now: Instant,
        deviceZone: TimeZone,
        settings: PlanSettings = PlanSettings(),
    ): List<Alert> {
        // History is stored to the millisecond, so match occurrences at that precision.
        val statuses = history.associate { (it.reminderId to it.due.toEpochMilliseconds()) to it.status }
        val until = now + settings.horizon
        return reminders.asSequence()
            .filter { !it.archived }
            .flatMap { reminder ->
                val zone = reminder.schedule.zoneOr(deviceZone)
                // Look back far enough to keep nagging about recent missed occurrences.
                reminder.schedule.occurrencesBetween(now - NagWindow - 1.days, until, zone).asSequence().flatMap { due ->
                    val status = statuses[reminder.id to due.toEpochMilliseconds()] ?: OccurrenceStatus.Pending
                    alertsFor(reminder, due, status, zone, settings)
                }
            }
            .filter { it.fireAt > now && it.fireAt <= until }
            .distinctBy { it.id }
            .sortedWith(compareBy<Alert>({ it.fireAt }, { it.id }))
            .take(settings.limit)
            .toList()
    }

    /**
     * The alerts that should have fired in (from, to], for example while the phone was off: the latest one for each
     * occurrence still pending, so it can be shown now.
     */
    fun missedBetween(
        reminders: List<Reminder>,
        history: List<Occurrence>,
        from: Instant,
        to: Instant,
        deviceZone: TimeZone,
        settings: PlanSettings = PlanSettings(),
    ): List<Alert> =
        plan(reminders, history, from, deviceZone, settings.copy(horizon = to - from, limit = Int.MAX_VALUE))
            .groupBy { it.occurrenceKey }
            .map { (_, alerts) -> alerts.last() }

    private fun alertsFor(
        reminder: Reminder,
        due: Instant,
        status: OccurrenceStatus,
        zone: TimeZone,
        settings: PlanSettings,
    ): Sequence<Alert> = sequence {
        val fire = if (reminder.allDay) {
            LocalDateTime(due.toLocalDateTime(zone).date, settings.allDayTime).toInstant(zone)
        } else {
            due
        }
        val nagFrom = when (status) {
            is OccurrenceStatus.Done, is OccurrenceStatus.Skipped -> return@sequence
            OccurrenceStatus.Pending -> {
                reminder.earlyAlerts.forEach { yield(Alert(reminder.id, due, fire - it, AlertKind.Early, it)) }
                fire
            }
            is OccurrenceStatus.Snoozed -> {
                yield(Alert(reminder.id, due, status.until, AlertKind.Snoozed))
                maxOf(fire, status.until)
            }
        }
        yield(Alert(reminder.id, due, fire, AlertKind.Due))
        val nag = reminder.nagInterval ?: return@sequence
        val next = reminder.schedule.nextAfter(due, zone)
        val stop = minOf(nagFrom + NagWindow, next ?: Instant.DISTANT_FUTURE)
        var at = nagFrom + nag
        var count = 0
        while (at < stop && count < settings.maxNagsPerOccurrence) {
            yield(Alert(reminder.id, due, at, AlertKind.Nag, at - fire))
            at += nag
            count++
        }
    }
}
