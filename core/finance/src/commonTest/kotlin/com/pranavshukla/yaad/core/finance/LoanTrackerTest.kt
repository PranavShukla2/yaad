package com.pranavshukla.yaad.core.finance

import com.pranavshukla.yaad.core.model.AnnualRate
import com.pranavshukla.yaad.core.model.Currency
import com.pranavshukla.yaad.core.model.Loan
import com.pranavshukla.yaad.core.model.LoanKind
import com.pranavshukla.yaad.core.model.Money
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class LoanTrackerTest {
    private val loan = Loan(
        "home", "HDFC", LoanKind.Home, Money.parse("1000000", Currency.INR), AnnualRate.percent("8.5"), 240,
        LocalDate(2026, 8, 5),
    )
    private val schedule = Amortisation.schedule(loan)

    @Test
    fun eachInstalmentIsPaidDueOrLate() {
        // EMIs on 5 Aug, 5 Sep, 5 Oct, 5 Nov 2026; today is 28 October; August and October are paid.
        val tracked = LoanTracker.track(schedule, paid = setOf(1, 3), today = LocalDate(2026, 10, 28))
        assertEquals(
            listOf(InstalmentStatus.Paid, InstalmentStatus.Late, InstalmentStatus.Paid, InstalmentStatus.Due),
            tracked.take(4).map { it.status },
        )
    }

    @Test
    fun theSummaryCountsPaidInstalments() {
        val summary = LoanTracker.summary(schedule, paid = (1..36).toSet())
        assertEquals(36, summary.paid)
        assertEquals(204, summary.left)
        assertEquals(Money.parse("934860.16", Currency.INR), summary.outstanding)
        // 65,139.84 of 10,00,000 is 6.513984%: shown to a thousandth of a percent, rounded half up.
        assertEquals(6514, summary.repaidThousandthsOfPercent)
        assertEquals(Money.parse("8678.23", Currency.INR), summary.nextPayment)
    }

    @Test
    fun aFullyPaidLoanIsAtZero() {
        val summary = LoanTracker.summary(schedule, paid = (1..240).toSet())
        assertEquals(0, summary.left)
        assertEquals(true, summary.outstanding.isZero)
        assertEquals(100_000, summary.repaidThousandthsOfPercent)
        assertEquals(null, summary.nextPayment)
    }
}
