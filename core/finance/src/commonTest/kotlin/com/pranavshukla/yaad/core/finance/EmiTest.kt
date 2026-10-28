package com.pranavshukla.yaad.core.finance

import com.pranavshukla.yaad.core.model.AnnualRate
import com.pranavshukla.yaad.core.model.Currency
import com.pranavshukla.yaad.core.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Fixtures computed independently (Python `decimal`, 60 digits) with the standard reducing-balance formula, interest
 * rounded half up to the paisa each month, and the last instalment absorbing the difference. The EMIs match the
 * rupee figures published by bank EMI calculators.
 */
class EmiTest {
    private fun rupees(text: String) = Money.parse(text, Currency.INR)
    private fun rate(text: String) = AnnualRate.percent(text)

    @Test
    fun theWorkedExampleOfThePlan() {
        assertEquals(rupees("8678.23"), Emi.of(rupees("1000000"), rate("8.5"), 240))
    }

    @Test
    fun otherBankCalculatorCases() {
        assertEquals(rupees("10746.95"), Emi.of(rupees("500000"), rate("10.5"), 60))
        assertEquals(rupees("21237.30"), Emi.of(rupees("2500000"), rate("9.15"), 300))
        assertEquals(rupees("9964.29"), Emi.of(rupees("300000"), rate("12"), 36))
    }

    @Test
    fun zeroInterestSplitsThePrincipal() {
        assertEquals(rupees("10000.00"), Emi.of(rupees("120000"), rate("0"), 12))
    }

    @Test
    fun monthsToRepayFromAGivenEmi() {
        assertEquals(175, Emi.monthsToRepay(rupees("1000000"), rate("8.5"), rupees("10000")))
        assertFailsWith<IllegalArgumentException>("an EMI below the interest never repays") {
            Emi.monthsToRepay(rupees("1000000"), rate("8.5"), rupees("7000"))
        }
    }

    @Test
    fun monthlyInterestIsRoundedHalfUpToThePaisa() {
        assertEquals(rupees("7083.33"), Emi.monthlyInterest(rupees("1000000"), rate("8.5")))
        // 0.5 paise rounds up: 6 paise at 100% a year is 0.5 paise a month.
        assertEquals(Money(1, Currency.INR), Emi.monthlyInterest(Money(6, Currency.INR), rate("100")))
    }
}
