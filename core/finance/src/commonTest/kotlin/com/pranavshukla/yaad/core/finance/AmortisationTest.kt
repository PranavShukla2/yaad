package com.pranavshukla.yaad.core.finance

import com.pranavshukla.yaad.core.model.AnnualRate
import com.pranavshukla.yaad.core.model.Currency
import com.pranavshukla.yaad.core.model.Loan
import com.pranavshukla.yaad.core.model.LoanKind
import com.pranavshukla.yaad.core.model.Money
import com.pranavshukla.yaad.core.model.Prepayment
import com.pranavshukla.yaad.core.model.PrepaymentEffect
import com.pranavshukla.yaad.core.model.RateChange
import com.pranavshukla.yaad.core.model.RateChangeKeeps
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AmortisationTest {
    private fun rupees(text: String) = Money.parse(text, Currency.INR)

    private val homeLoan = Loan(
        id = "home", lender = "Bank", kind = LoanKind.Home, principal = rupees("1000000"),
        rate = AnnualRate.percent("8.5"), tenureMonths = 240, firstEmiDate = LocalDate(2026, 11, 5),
    )

    @Test
    fun theWorkedExampleEndsAtExactlyZero() {
        val schedule = Amortisation.schedule(homeLoan)
        assertEquals(240, schedule.instalments.size)
        val first = schedule.instalments.first()
        assertEquals(rupees("7083.33"), first.interest)
        assertEquals(rupees("1594.90"), first.principal)
        assertEquals(rupees("8679.66"), schedule.instalments.last().payment, "the last instalment absorbs the rounding")
        assertEquals(rupees("1082776.63"), schedule.totalInterest)
        assertEquals(rupees("2082776.63"), schedule.totalPaid)
        assertTrue(schedule.instalments.last().balance.isZero)
        assertEquals(LocalDate(2046, 10, 5), schedule.instalments.last().date)
    }

    @Test
    fun everyRowAddsUp() {
        val schedule = Amortisation.schedule(homeLoan)
        var balance = homeLoan.principal
        for (row in schedule.instalments) {
            assertEquals(row.payment, row.interest + row.principal)
            balance -= row.principal + row.prepaid
            assertEquals(balance, row.balance)
        }
    }

    @Test
    fun otherLoansEndAtZeroWithTheirLastInstalment() {
        fun check(principal: String, rate: String, months: Int, last: String, interest: String) {
            val s = Amortisation.schedule(homeLoan.copy(principal = rupees(principal), rate = AnnualRate.percent(rate), tenureMonths = months))
            assertEquals(months, s.instalments.size)
            assertEquals(rupees(last), s.instalments.last().payment)
            assertEquals(rupees(interest), s.totalInterest)
            assertTrue(s.instalments.last().balance.isZero)
        }
        check("500000", "10.5", 60, "10746.94", "144816.99")
        check("2500000", "9.15", 300, "21235.39", "3871188.09")
        check("300000", "12", 36, "9964.43", "58714.58")
    }

    @Test
    fun aPrepaymentThatShortensTheLoan() {
        val loan = homeLoan.copy(prepayments = listOf(Prepayment(12, rupees("100000"), PrepaymentEffect.Tenure)))
        val schedule = Amortisation.schedule(loan)
        assertEquals(rupees("980097.73"), Amortisation.schedule(homeLoan).instalments[11].balance)
        assertEquals(192, schedule.instalments.size)
        assertEquals(rupees("4496.15"), schedule.instalments.last().payment)
        assertEquals(rupees("762038.08"), schedule.totalInterest)
        assertEquals(rupees("320738.55"), Amortisation.interestSaved(loan))
    }

    @Test
    fun aPrepaymentThatLowersTheEmi() {
        val loan = homeLoan.copy(prepayments = listOf(Prepayment(12, rupees("100000"), PrepaymentEffect.Emi)))
        val schedule = Amortisation.schedule(loan)
        assertEquals(240, schedule.instalments.size)
        assertEquals(rupees("7792.79"), schedule.instalments[12].payment)
        assertEquals(rupees("7791.02"), schedule.instalments.last().payment)
        assertEquals(rupees("980893.11"), schedule.totalInterest)
        assertEquals(rupees("101883.52"), Amortisation.interestSaved(loan))
    }

    @Test
    fun aRateRiseKeepingTheEmiLengthensTheLoan() {
        val loan = homeLoan.copy(rateChanges = listOf(RateChange(24, AnnualRate.percent("9"), RateChangeKeeps.Emi)))
        val schedule = Amortisation.schedule(loan)
        assertEquals(260, schedule.instalments.size)
        assertEquals(rupees("7143.32"), schedule.instalments.last().payment)
        assertTrue(schedule.instalments.last().balance.isZero)
    }

    @Test
    fun aRateRiseKeepingTheTenureRaisesTheEmi() {
        val loan = homeLoan.copy(rateChanges = listOf(RateChange(24, AnnualRate.percent("9"), RateChangeKeeps.Tenure)))
        val schedule = Amortisation.schedule(loan)
        assertEquals(240, schedule.instalments.size)
        assertEquals(rupees("8975.23"), schedule.instalments[24].payment)
        assertEquals(rupees("8973.58"), schedule.instalments.last().payment)
    }

    @Test
    fun theBanksOwnEmiIsUsedAsGiven() {
        val schedule = Amortisation.schedule(homeLoan.copy(emi = rupees("8678.00")))
        assertEquals(rupees("8678.00"), schedule.instalments.first().payment)
        assertEquals(240, schedule.instalments.size)
        assertTrue(schedule.instalments.last().balance.isZero)
    }

    @Test
    fun foreclosureIsTheOutstandingPrincipalPlusTheCharge() {
        val estimate = Amortisation.foreclosure(Amortisation.schedule(homeLoan), afterInstalment = 36, chargeThousandthsOfPercent = 2000)
        assertEquals(rupees("934860.16"), estimate.outstanding)
        assertEquals(rupees("18697.20"), estimate.charge)
        assertEquals(rupees("953557.36"), estimate.total)
    }

    @Test
    fun progressAfterSomeInstalments() {
        val progress = Amortisation.schedule(homeLoan).progressAfter(36)
        assertEquals(204, progress.instalmentsLeft)
        assertEquals(rupees("934860.16"), progress.outstanding)
        assertEquals(rupees("65139.84"), progress.principalRepaid)
    }
}
