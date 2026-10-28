package com.pranavshukla.yaad.feature.loans

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.pranavshukla.yaad.core.finance.InstalmentStatus
import com.pranavshukla.yaad.core.model.AnnualRate
import com.pranavshukla.yaad.core.model.Currency
import com.pranavshukla.yaad.core.model.Loan
import com.pranavshukla.yaad.core.model.LoanKind
import com.pranavshukla.yaad.core.model.Money
import com.pranavshukla.yaad.core.model.Prepayment
import com.pranavshukla.yaad.core.model.PrepaymentEffect
import com.pranavshukla.yaad.core.model.ReminderType
import com.pranavshukla.yaad.data.LoanStore
import com.pranavshukla.yaad.data.ReminderStore
import com.pranavshukla.yaad.data.db.YaadDatabase
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

class LoansModelTest {
    private val zone = TimeZone.of("Asia/Kolkata")
    private val clock = object : Clock {
        override fun now() = Instant.parse("2027-01-10T10:00:00Z")
    }
    private val loan = Loan(
        "home", "HDFC", LoanKind.Home, Money.parse("1000000", Currency.INR), AnnualRate.percent("8.5"), 240, LocalDate(2026, 11, 5),
    )
    private lateinit var reminders: ReminderStore
    private var changes = 0

    private fun TestScope.model(): LoansModel {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties().apply { put("foreign_keys", "true") })
        YaadDatabase.Schema.create(driver)
        reminders = ReminderStore(driver, clock, dispatcher)
        return LoansModel(LoanStore(driver, clock, dispatcher), reminders, backgroundScope, clock, { zone }, { changes++ }, dispatcher, emptyFlow())
    }

    @Test
    fun savingALoanAddsItsEmiReminder() = runTest {
        val model = model()
        model.save(loan, emiTitle = "HDFC EMI")
        runCurrent()
        val view = model.state.value.loans.single()
        assertEquals(240, view.schedule.instalments.size)
        val reminder = reminders.reminders().first().single()
        assertEquals(ReminderType.Emi, reminder.type)
        assertEquals("HDFC EMI", reminder.title)
        assertEquals(Money.parse("8678.23", Currency.INR), reminder.amount)
        assertEquals(1, changes)
    }

    @Test
    fun markingInstalmentsPaidUpdatesTheSummary() = runTest {
        val model = model()
        model.save(loan, "HDFC EMI")
        runCurrent()
        // EMIs on 5 Nov, 5 Dec and 5 Jan are due by 10 January 2027.
        model.markPaid(model.state.value.loans.single(), 1)
        model.markPaid(model.state.value.loans.single(), 3)
        runCurrent()
        val view = model.state.value.loans.single()
        assertEquals(listOf(InstalmentStatus.Paid, InstalmentStatus.Late, InstalmentStatus.Paid, InstalmentStatus.Due), view.tracked.take(4).map { it.status })
        assertEquals(2, view.summary.paid)
    }

    @Test
    fun aPrepaymentShortensTheEmiReminder() = runTest {
        val model = model()
        model.save(loan, "HDFC EMI")
        runCurrent()
        model.save(loan.copy(prepayments = listOf(Prepayment(12, Money.parse("100000", Currency.INR), PrepaymentEffect.Tenure))), "HDFC EMI")
        runCurrent()
        assertEquals(192, model.state.value.loans.single().schedule.instalments.size)
        assertEquals(1, reminders.reminders().first().size, "the same reminder, updated")
    }

    @Test
    fun deletingALoanDeletesItsReminder() = runTest {
        val model = model()
        model.save(loan, "HDFC EMI")
        runCurrent()
        model.delete(loan)
        runCurrent()
        assertEquals(emptyList(), model.state.value.loans)
        assertEquals(emptyList(), reminders.reminders().first())
    }
}
