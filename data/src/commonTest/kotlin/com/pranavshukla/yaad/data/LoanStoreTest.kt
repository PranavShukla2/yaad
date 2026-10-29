package com.pranavshukla.yaad.data

import com.pranavshukla.yaad.core.model.AnnualRate
import com.pranavshukla.yaad.core.model.Currency
import com.pranavshukla.yaad.core.model.Loan
import com.pranavshukla.yaad.core.model.LoanKind
import com.pranavshukla.yaad.core.model.Money
import com.pranavshukla.yaad.core.model.Prepayment
import com.pranavshukla.yaad.core.model.PrepaymentEffect
import com.pranavshukla.yaad.core.model.RateChange
import com.pranavshukla.yaad.core.model.RateChangeKeeps
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

class LoanStoreTest {
    private val clock = object : Clock {
        override fun now() = Instant.parse("2026-10-28T10:00:00Z")
    }

    private val loan = Loan(
        id = "home", lender = "HDFC", kind = LoanKind.Home, principal = Money(100_000_000, Currency.INR),
        rate = AnnualRate(8500), tenureMonths = 240, firstEmiDate = LocalDate(2026, 11, 5),
        emi = Money(867_800, Currency.INR),
        prepayments = listOf(Prepayment(12, Money(10_000_000, Currency.INR), PrepaymentEffect.Tenure)),
        rateChanges = listOf(RateChange(24, AnnualRate(9000), RateChangeKeeps.Emi), RateChange(36, AnnualRate(8750), RateChangeKeeps.Tenure)),
    )

    @Test
    fun loansRoundTripWithTheirEvents() = runTest {
        val store = LoanStore(testDriver(), clock, StandardTestDispatcher(testScheduler))
        store.save(loan)
        store.save(loan.copy(id = "car", lender = "Axis", kind = LoanKind.Car, emi = null, prepayments = emptyList(), rateChanges = emptyList()))
        assertEquals(listOf("car", "home"), store.loans().first().map { it.id })
        assertEquals(loan, store.loan("home"))
    }

    @Test
    fun savingAgainReplacesTheEvents() = runTest {
        val store = LoanStore(testDriver(), clock, StandardTestDispatcher(testScheduler))
        store.save(loan)
        val edited = loan.copy(prepayments = emptyList(), rateChanges = listOf(RateChange(6, AnnualRate(8000), RateChangeKeeps.Emi)))
        store.save(edited)
        assertEquals(edited, store.loan("home"))
        store.delete("home")
        assertEquals(emptyList(), store.loans().first())
    }
}
