package com.pranavshukla.yaad.feature.loans

import com.pranavshukla.yaad.core.model.AnnualRate
import com.pranavshukla.yaad.core.model.Currency
import com.pranavshukla.yaad.core.model.Loan
import com.pranavshukla.yaad.core.model.LoanKind
import com.pranavshukla.yaad.core.model.Money
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LoanFormTest {
    private val form = LoanForm(
        id = "home", lender = "HDFC", kind = LoanKind.Home, principal = "10,00,000", rate = "8.5", tenure = "20",
        tenureInYears = true, firstEmiDate = LocalDate(2026, 11, 5),
    )

    @Test
    fun aCompleteFormMakesALoanAndPreviewsTheEmi() {
        val loan = form.toLoan(Currency.INR)!!
        assertEquals(Money.parse("1000000", Currency.INR), loan.principal)
        assertEquals(AnnualRate(8500), loan.rate)
        assertEquals(240, loan.tenureMonths)
        assertNull(loan.emi)
        assertEquals(Money.parse("8678.23", Currency.INR), form.previewEmi(Currency.INR))
    }

    @Test
    fun theBanksEmiIsUsedWhenGiven() {
        val loan = form.copy(bankEmi = "8678").toLoan(Currency.INR)!!
        assertEquals(Money.parse("8678", Currency.INR), loan.emi)
        assertEquals(Money.parse("8678", Currency.INR), form.copy(bankEmi = "8678").previewEmi(Currency.INR))
    }

    @Test
    fun incompleteOrWrongFieldsMakeNoLoan() {
        assertNull(form.copy(lender = " ").toLoan(Currency.INR))
        assertNull(form.copy(principal = "ten lakh").toLoan(Currency.INR))
        assertNull(form.copy(rate = "8.5%").toLoan(Currency.INR))
        assertNull(form.copy(tenure = "0").toLoan(Currency.INR))
        assertNull(form.copy(bankEmi = "abc").toLoan(Currency.INR))
        assertNull(form.copy(rate = "").previewEmi(Currency.INR))
    }

    @Test
    fun editingALoanKeepsItsEvents() {
        val loan = form.toLoan(Currency.INR)!!
        val back = LoanForm.from(loan.copy(tenureMonths = 18))
        assertEquals("18", back.tenure)
        assertEquals(false, back.tenureInYears)
        assertEquals(loan.copy(tenureMonths = 18), back.toLoan(Currency.INR))
        assertEquals(true, LoanForm.from(loan).tenureInYears)
    }
}
