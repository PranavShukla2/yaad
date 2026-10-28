package com.pranavshukla.yaad.core.scheduling

import com.pranavshukla.yaad.core.model.AnnualRate
import com.pranavshukla.yaad.core.model.BuiltInCategory
import com.pranavshukla.yaad.core.model.Currency
import com.pranavshukla.yaad.core.model.Frequency
import com.pranavshukla.yaad.core.model.Loan
import com.pranavshukla.yaad.core.model.LoanKind
import com.pranavshukla.yaad.core.model.Money
import com.pranavshukla.yaad.core.model.MonthlyOn
import com.pranavshukla.yaad.core.model.Occurrence
import com.pranavshukla.yaad.core.model.OccurrenceStatus
import com.pranavshukla.yaad.core.model.RecurrenceEnd
import com.pranavshukla.yaad.core.model.ReminderType
import com.pranavshukla.yaad.core.recurrence.occurrences
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class LoanLinkTest {
    private val zone = TimeZone.of("Asia/Kolkata")
    private val loan = Loan("home", "HDFC", LoanKind.Home, Money(100_000_000, Currency.INR), AnnualRate(8500), 240, LocalDate(2027, 1, 31))
    private val emi = Money(867_823, Currency.INR)

    @Test
    fun aLoanGetsAMonthlyAllDayEmiReminder() {
        val reminder = LoanLink.reminderFor(loan, instalments = 240, emi = emi, title = "HDFC EMI", existing = null, zone = zone)
        assertEquals(ReminderType.Emi, reminder.type)
        assertEquals("home", reminder.linkedLoanId)
        assertEquals(emi, reminder.amount)
        assertEquals(true, reminder.allDay)
        assertEquals(BuiltInCategory.Money.id, reminder.categoryId)
        assertEquals(listOf(3.days), reminder.earlyAlerts)
        assertEquals(Frequency.Monthly(1, MonthlyOn.Day(31)), reminder.schedule.rule!!.frequency)
        assertEquals(RecurrenceEnd.AfterCount(240), reminder.schedule.rule!!.end)
        assertEquals("Asia/Kolkata", reminder.schedule.timeZoneId)
        // The 31st falls on the last day of shorter months, as the bank's schedule does.
        assertEquals(
            listOf(LocalDate(2027, 1, 31), LocalDate(2027, 2, 28), LocalDate(2027, 3, 31)),
            reminder.schedule.occurrences(zone).take(3).map { it.toLocalDateTime(zone).date }.toList(),
        )
    }

    @Test
    fun editingTheLoanKeepsTheUsersChangesToTheReminder() {
        val mine = LoanLink.reminderFor(loan, 240, emi, "HDFC EMI", null, zone).copy(title = "House EMI", earlyAlerts = listOf(1.days))
        val updated = LoanLink.reminderFor(loan, 192, Money(780_000, Currency.INR), "HDFC EMI", mine, TimeZone.UTC)
        assertEquals("House EMI", updated.title)
        assertEquals(listOf(1.days), updated.earlyAlerts)
        assertEquals(RecurrenceEnd.AfterCount(192), updated.schedule.rule!!.end)
        assertEquals(Money(780_000, Currency.INR), updated.amount)
        assertEquals(mine.id, updated.id)
        assertEquals("Asia/Kolkata", updated.schedule.timeZoneId, "stays in the zone it was made in")
    }

    @Test
    fun doneOccurrencesAreThePaidInstalments() {
        val reminder = LoanLink.reminderFor(loan, 240, emi, "HDFC EMI", null, zone)
        val dues = reminder.schedule.occurrences(zone).take(4).toList()
        val history = listOf(
            Occurrence(reminder.id, dues[0], OccurrenceStatus.Done(Instant.DISTANT_PAST)),
            Occurrence(reminder.id, dues[1], OccurrenceStatus.Skipped(Instant.DISTANT_PAST)),
            Occurrence(reminder.id, dues[3], OccurrenceStatus.Done(Instant.DISTANT_PAST)),
        )
        assertEquals(setOf(1, 4), LoanLink.paidInstalments(reminder, history, zone))
    }
}
