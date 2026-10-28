package com.pranavshukla.yaad.feature.reminders

import androidx.compose.runtime.Composable
import com.pranavshukla.yaad.core.model.Frequency
import com.pranavshukla.yaad.core.model.MonthlyOn
import com.pranavshukla.yaad.core.model.RecurrenceEnd
import com.pranavshukla.yaad.core.model.RecurrenceRule
import com.pranavshukla.yaad.core.scheduling.DayLabel
import com.pranavshukla.yaad.core.scheduling.TimeLabel
import com.pranavshukla.yaad.feature.reminders.resources.Res
import com.pranavshukla.yaad.feature.reminders.resources.*
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration
import kotlin.time.Instant

/** Words for dates, times and rules. The dates and rules themselves come from `core:*`; this only names them. */

@Composable
internal fun weekdayName(day: DayOfWeek, short: Boolean = false): String =
    stringArrayResource(if (short) Res.array.weekdays_short else Res.array.weekdays)[day.isoDayNumber - 1]

@Composable
internal fun dateText(date: LocalDate, today: LocalDate): String {
    val month = stringArrayResource(Res.array.months_short)[date.month.number - 1]
    return if (date.year == today.year) {
        stringResource(Res.string.date_day_month, date.day, month)
    } else {
        stringResource(Res.string.date_day_month_year, date.day, month, date.year)
    }
}

@Composable
internal fun dayText(date: LocalDate, today: LocalDate): String = when (val label = DayLabel.of(date, today)) {
    DayLabel.Today -> stringResource(Res.string.day_today)
    DayLabel.Tomorrow -> stringResource(Res.string.day_tomorrow)
    DayLabel.Yesterday -> stringResource(Res.string.day_yesterday)
    is DayLabel.Weekday -> weekdayName(label.day)
    is DayLabel.Date -> dateText(label.date, today)
}

/** "Today, 7:00 PM", "Fri, 9:00 AM", "4 Nov" (all-day). */
@Composable
internal fun dueText(due: Instant, allDay: Boolean, now: Instant, zone: TimeZone, twentyFourHour: Boolean = false): String {
    val local = due.toLocalDateTime(zone)
    val day = dayText(local.date, now.toLocalDateTime(zone).date)
    return if (allDay) day else stringResource(Res.string.day_and_time, day, TimeLabel.format(local.time, twentyFourHour))
}

/** "Every 2 weeks on Mon, Fri, 10 times". */
@Composable
internal fun ruleText(rule: RecurrenceRule?, today: LocalDate): String {
    if (rule == null) return stringResource(Res.string.repeat_none)
    val f = rule.frequency
    val n = f.interval
    val base = when (f) {
        is Frequency.Minutely -> stringResource(Res.string.repeat_minutes, n)
        is Frequency.Hourly -> stringResource(Res.string.repeat_hours, n)
        is Frequency.Daily -> if (n == 1) stringResource(Res.string.repeat_daily) else stringResource(Res.string.repeat_days, n)
        is Frequency.Weekly -> {
            val every = if (n == 1) stringResource(Res.string.repeat_weekly) else stringResource(Res.string.repeat_weeks, n)
            if (f.days.isEmpty()) {
                every
            } else {
                val days = f.days.sortedBy { it.isoDayNumber }.map { weekdayName(it, short = true) }.joinToString(", ")
                stringResource(Res.string.repeat_on, every, days)
            }
        }
        is Frequency.Monthly -> {
            val every = if (n == 1) stringResource(Res.string.repeat_monthly) else stringResource(Res.string.repeat_months, n)
            val on = when (val o = f.on) {
                is MonthlyOn.Day -> stringResource(Res.string.monthly_day, o.day)
                MonthlyOn.LastDay -> stringResource(Res.string.monthly_last_day)
                is MonthlyOn.NthWeekday -> stringResource(
                    Res.string.monthly_nth,
                    if (o.ordinal == MonthlyOn.NthWeekday.LAST) {
                        stringResource(Res.string.ordinal_last)
                    } else {
                        stringArrayResource(Res.array.ordinals)[o.ordinal - 1]
                    },
                    weekdayName(o.weekday),
                )
            }
            stringResource(Res.string.repeat_on, every, on)
        }
        is Frequency.Yearly -> if (n == 1) stringResource(Res.string.repeat_yearly) else stringResource(Res.string.repeat_years, n)
    }
    return base + when (val end = rule.end) {
        RecurrenceEnd.Never -> ""
        is RecurrenceEnd.AfterCount -> stringResource(Res.string.ends_after, end.count)
        is RecurrenceEnd.OnDate -> stringResource(Res.string.ends_on, dateText(end.date, today))
    }
}

/** "1 day before", "3 days before", "2 h before", "15 min before". */
@Composable
internal fun beforeText(offset: Duration): String {
    val minutes = offset.inWholeMinutes.toInt()
    return when {
        minutes % (24 * 60) == 0 && minutes == 24 * 60 -> stringResource(Res.string.before_day)
        minutes % (24 * 60) == 0 -> stringResource(Res.string.before_days, minutes / (24 * 60))
        minutes % 60 == 0 -> stringResource(Res.string.before_hours, minutes / 60)
        else -> stringResource(Res.string.before_minutes, minutes)
    }
}
