package com.pranavshukla.yaad.core.scheduling

import com.pranavshukla.yaad.core.model.Occurrence
import com.pranavshukla.yaad.core.model.OccurrenceStatus
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.model.ReminderType
import com.pranavshukla.yaad.core.recurrence.nextAfter
import com.pranavshukla.yaad.core.recurrence.occurrencesBetween
import com.pranavshukla.yaad.core.recurrence.zoneOr
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** One occurrence on a list: which reminder, when it is due, and what has happened to it. */
data class AgendaItem(val reminder: Reminder, val due: Instant, val status: OccurrenceStatus, val date: LocalDate) {
    val isHandled: Boolean get() = status is OccurrenceStatus.Done || status is OccurrenceStatus.Skipped
}

/** What the Today and Upcoming screens show. */
data class AgendaView(
    val overdue: List<AgendaItem>,
    val today: List<AgendaItem>,
    val upcoming: Map<LocalDate, List<AgendaItem>>,
)

/**
 * Groups occurrences for the screens, so the UI never works out dates itself.
 *
 * - **Overdue:** unhandled occurrences before now (all-day items only once their day is over), up to the
 *   [maxOverduePerReminder] most recent of each reminder within [overdueDays].
 * - **Today:** the rest of today, handled ones included (shown as done).
 * - **Upcoming:** the next [upcomingDays] days, grouped by day.
 */
object Agenda {
    private const val maxOverduePerReminder = 10

    /** A birthday last week is not a task left undone. */
    private val NeverOverdue = setOf(ReminderType.Birthday)
    private const val overdueDays = 30
    private const val upcomingDays = 30

    fun build(reminders: List<Reminder>, history: List<Occurrence>, now: Instant, deviceZone: TimeZone): AgendaView {
        val statuses = history.associate { (it.reminderId to it.due.toEpochMilliseconds()) to it.status }
        val today = now.toLocalDateTime(deviceZone).date
        val overdue = mutableListOf<AgendaItem>()
        val todays = mutableListOf<AgendaItem>()
        val upcoming = mutableListOf<AgendaItem>()
        for (reminder in reminders.filter { !it.archived }) {
            val zone = reminder.schedule.zoneOr(deviceZone)
            val from = today.plus(-overdueDays, DateTimeUnit.DAY).atStartOfDayIn(zone)
            val until = today.plus(upcomingDays + 1, DateTimeUnit.DAY).atStartOfDayIn(zone)
            val missed = mutableListOf<AgendaItem>()
            for (due in reminder.schedule.occurrencesBetween(from, until, zone)) {
                val date = due.toLocalDateTime(zone).date
                val item = AgendaItem(reminder, due, statuses[reminder.id to due.toEpochMilliseconds()] ?: OccurrenceStatus.Pending, date)
                val isPast = isOverdue(reminder, due, now, zone)
                when {
                    isPast && date < today -> if (!item.isHandled && reminder.type !in NeverOverdue) missed += item
                    isPast -> if (item.isHandled) todays += item else missed += item
                    date == today -> todays += item
                    else -> upcoming += item
                }
            }
            overdue += missed.takeLast(maxOverduePerReminder)
        }
        val order = compareBy<AgendaItem>({ it.due }, { it.reminder.title })
        return AgendaView(
            overdue = overdue.sortedWith(order),
            today = todays.sortedWith(order),
            upcoming = upcoming.sortedWith(order).groupBy { it.date },
        )
    }

    /** Whether an occurrence due at [due] is past: all-day items only once their day is over. */
    fun isOverdue(reminder: Reminder, due: Instant, now: Instant, deviceZone: TimeZone): Boolean {
        if (!reminder.allDay) return due < now
        val zone = reminder.schedule.zoneOr(deviceZone)
        return due.toLocalDateTime(zone).date < now.toLocalDateTime(zone).date
    }

    /** The earliest unhandled occurrence, overdue or not (within the last [overdueDays] days), or null if none is left. */
    fun nextDue(reminder: Reminder, history: List<Occurrence>, now: Instant, deviceZone: TimeZone): Instant? {
        val handled = history.filter { it.reminderId == reminder.id }
            .filter { it.status is OccurrenceStatus.Done || it.status is OccurrenceStatus.Skipped }
            .mapTo(HashSet()) { it.due.toEpochMilliseconds() }
        val zone = reminder.schedule.zoneOr(deviceZone)
        var cursor = if (reminder.type in NeverOverdue) now - 1.days else now - overdueDays.days
        while (true) {
            val due = reminder.schedule.nextAfter(cursor, zone) ?: return null
            if (due.toEpochMilliseconds() !in handled) return due
            cursor = due
        }
    }
}
