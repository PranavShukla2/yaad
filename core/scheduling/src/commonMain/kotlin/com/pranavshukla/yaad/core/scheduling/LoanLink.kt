package com.pranavshukla.yaad.core.scheduling

import com.pranavshukla.yaad.core.model.BuiltInCategory
import com.pranavshukla.yaad.core.model.Frequency
import com.pranavshukla.yaad.core.model.Loan
import com.pranavshukla.yaad.core.model.Money
import com.pranavshukla.yaad.core.model.MonthlyOn
import com.pranavshukla.yaad.core.model.Occurrence
import com.pranavshukla.yaad.core.model.OccurrenceStatus
import com.pranavshukla.yaad.core.model.RecurrenceEnd
import com.pranavshukla.yaad.core.model.RecurrenceRule
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.model.ReminderId
import com.pranavshukla.yaad.core.model.ReminderType
import com.pranavshukla.yaad.core.model.Schedule
import com.pranavshukla.yaad.core.recurrence.occurrences
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.time.Duration.Companion.days

/**
 * A loan's EMIs as one reminder: all day on the EMI date every month, as many times as there are instalments. Its
 * occurrences are the instalments in order, so marking one done marks that instalment paid.
 */
object LoanLink {
    fun reminderId(loan: Loan) = ReminderId("loan-${loan.id}")

    /**
     * The EMI reminder for [loan] with [instalments] EMIs of [emi]. When updating an [existing] one, the user's title,
     * alerts and other choices are kept; only the dates, count and amount follow the loan.
     *
     * EMI dates are the bank's calendar dates, so the reminder is pinned to the [zone] it was made in: travelling
     * does not move them, and paid instalments keep matching their history.
     */
    fun reminderFor(loan: Loan, instalments: Int, emi: Money, title: String, existing: Reminder?, zone: TimeZone): Reminder {
        val schedule = Schedule(
            LocalDateTime(loan.firstEmiDate, LocalTime(0, 0)),
            RecurrenceRule(Frequency.Monthly(1, MonthlyOn.Day(loan.firstEmiDate.day)), RecurrenceEnd.AfterCount(instalments)),
            existing?.schedule?.timeZoneId ?: zone.id,
        )
        val base = existing ?: Reminder(
            id = reminderId(loan),
            title = title,
            schedule = schedule,
            categoryId = BuiltInCategory.Money.id,
            earlyAlerts = listOf(3.days),
        )
        return base.copy(schedule = schedule, type = ReminderType.Emi, allDay = true, amount = emi, linkedLoanId = loan.id)
    }

    /** Instalment numbers (1-based) whose occurrence is done. */
    fun paidInstalments(reminder: Reminder, history: List<Occurrence>, zone: TimeZone): Set<Int> {
        val done = history.filter { it.reminderId == reminder.id && it.status is OccurrenceStatus.Done }
            .mapTo(HashSet()) { it.due.toEpochMilliseconds() }
        if (done.isEmpty()) return emptySet()
        return reminder.schedule.occurrences(zone).withIndex()
            .filter { (_, due) -> due.toEpochMilliseconds() in done }
            .map { it.index + 1 }
            .toSet()
    }
}
