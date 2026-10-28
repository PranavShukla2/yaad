package com.pranavshukla.yaad.feature.reminders

import com.pranavshukla.yaad.core.model.ChecklistItem
import com.pranavshukla.yaad.core.model.Frequency
import com.pranavshukla.yaad.core.model.MonthlyOn
import com.pranavshukla.yaad.core.model.Priority
import com.pranavshukla.yaad.core.model.RecurrenceEnd
import com.pranavshukla.yaad.core.model.RecurrenceRule
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.model.ReminderId
import com.pranavshukla.yaad.core.model.Schedule
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

enum class RepeatKind { None, Minutes, Hours, Daily, Weekly, Monthly, Yearly }
enum class MonthlyMode { Day, NthWeekday, LastDay }
enum class EndKind { Never, OnDate, AfterCount }

/**
 * The editor's form. [toReminder] and [from] convert to and from the model without losing anything the form does not
 * show (type, amount, linked loan): those are carried in [base].
 */
data class EditorState(
    val id: ReminderId,
    val title: String = "",
    val notes: String = "",
    val date: LocalDate,
    val time: LocalTime,
    val allDay: Boolean = false,
    val repeat: RepeatKind = RepeatKind.None,
    val interval: Int = 1,
    val weekdays: Set<DayOfWeek> = emptySet(),
    val monthlyMode: MonthlyMode = MonthlyMode.Day,
    val monthDay: Int = date.day,
    val nthOrdinal: Int = ordinalOf(date),
    val nthWeekday: DayOfWeek = date.dayOfWeek,
    val end: EndKind = EndKind.Never,
    val endDate: LocalDate? = null,
    val endCount: Int = 10,
    val earlyAlerts: Set<Duration> = emptySet(),
    val nagMinutes: Int? = null,
    val categoryId: String? = null,
    val priority: Priority = Priority.Normal,
    val checklist: List<ChecklistItem> = emptyList(),
    val timeZoneId: String? = null,
    val base: Reminder? = null,
) {
    val isValid: Boolean
        get() = title.isNotBlank() && interval >= 1 && endCount >= 1 &&
            (end != EndKind.OnDate || (endDate != null && endDate >= date))

    /** A new date also moves "day N" and "nth weekday" to match it. */
    fun withDate(newDate: LocalDate) =
        copy(date = newDate, monthDay = newDate.day, nthOrdinal = ordinalOf(newDate), nthWeekday = newDate.dayOfWeek)

    fun rule(): RecurrenceRule? {
        val frequency = when (repeat) {
            RepeatKind.None -> return null
            RepeatKind.Minutes -> Frequency.Minutely(interval)
            RepeatKind.Hours -> Frequency.Hourly(interval)
            RepeatKind.Daily -> Frequency.Daily(interval)
            RepeatKind.Weekly -> Frequency.Weekly(interval, weekdays.ifEmpty { setOf(date.dayOfWeek) })
            RepeatKind.Monthly -> Frequency.Monthly(
                interval,
                when (monthlyMode) {
                    MonthlyMode.Day -> MonthlyOn.Day(monthDay)
                    MonthlyMode.NthWeekday -> MonthlyOn.NthWeekday(nthOrdinal, nthWeekday)
                    MonthlyMode.LastDay -> MonthlyOn.LastDay
                },
            )
            RepeatKind.Yearly -> Frequency.Yearly(interval)
        }
        val ends = when (end) {
            EndKind.Never -> RecurrenceEnd.Never
            EndKind.OnDate -> RecurrenceEnd.OnDate(endDate ?: date)
            EndKind.AfterCount -> RecurrenceEnd.AfterCount(endCount)
        }
        return RecurrenceRule(frequency, ends)
    }

    fun toReminder(): Reminder {
        val start = LocalDateTime(date, if (allDay) LocalTime(0, 0) else time)
        val fields = Reminder(
            id = id,
            title = title.trim(),
            schedule = Schedule(start, rule(), timeZoneId),
            notes = notes.trim(),
            categoryId = categoryId,
            allDay = allDay,
            earlyAlerts = earlyAlerts.sortedDescending(),
            priority = priority,
            nagInterval = nagMinutes?.minutes,
            checklist = checklist.filter { it.text.isNotBlank() },
        )
        return base?.copy(
            title = fields.title, schedule = fields.schedule, notes = fields.notes, categoryId = fields.categoryId,
            allDay = fields.allDay, earlyAlerts = fields.earlyAlerts, priority = fields.priority,
            nagInterval = fields.nagInterval, checklist = fields.checklist,
        ) ?: fields
    }

    companion object {
        /** A blank reminder for today at the next whole hour. */
        fun new(id: ReminderId, now: Instant, zone: TimeZone): EditorState {
            val local = now.toLocalDateTime(zone)
            val next = if (local.hour == 23) {
                LocalDateTime(local.date.plus(1, DateTimeUnit.DAY), LocalTime(0, 0))
            } else {
                LocalDateTime(local.date, LocalTime(local.hour + 1, 0))
            }
            return EditorState(id = id, date = next.date, time = next.time)
        }

        fun from(reminder: Reminder): EditorState {
            val start = reminder.schedule.start
            var state = EditorState(
                id = reminder.id, title = reminder.title, notes = reminder.notes, date = start.date, time = start.time,
                allDay = reminder.allDay, earlyAlerts = reminder.earlyAlerts.toSet(),
                nagMinutes = reminder.nagInterval?.inWholeMinutes?.toInt(), categoryId = reminder.categoryId,
                priority = reminder.priority, checklist = reminder.checklist, timeZoneId = reminder.schedule.timeZoneId,
                base = reminder,
            )
            val rule = reminder.schedule.rule ?: return state
            val f = rule.frequency
            state = state.copy(
                interval = f.interval,
                repeat = when (f) {
                    is Frequency.Minutely -> RepeatKind.Minutes
                    is Frequency.Hourly -> RepeatKind.Hours
                    is Frequency.Daily -> RepeatKind.Daily
                    is Frequency.Weekly -> RepeatKind.Weekly
                    is Frequency.Monthly -> RepeatKind.Monthly
                    is Frequency.Yearly -> RepeatKind.Yearly
                },
            )
            if (f is Frequency.Weekly) state = state.copy(weekdays = f.days)
            if (f is Frequency.Monthly) {
                state = when (val on = f.on) {
                    is MonthlyOn.Day -> state.copy(monthlyMode = MonthlyMode.Day, monthDay = on.day)
                    MonthlyOn.LastDay -> state.copy(monthlyMode = MonthlyMode.LastDay)
                    is MonthlyOn.NthWeekday -> state.copy(monthlyMode = MonthlyMode.NthWeekday, nthOrdinal = on.ordinal, nthWeekday = on.weekday)
                }
            }
            return when (val end = rule.end) {
                RecurrenceEnd.Never -> state
                is RecurrenceEnd.OnDate -> state.copy(end = EndKind.OnDate, endDate = end.date)
                is RecurrenceEnd.AfterCount -> state.copy(end = EndKind.AfterCount, endCount = end.count)
            }
        }

        /** Which occurrence of its weekday [date] is in its month; the fifth counts as the last. */
        fun ordinalOf(date: LocalDate): Int {
            val ordinal = (date.day - 1) / 7 + 1
            return if (ordinal > 4) MonthlyOn.NthWeekday.LAST else ordinal
        }
    }
}
