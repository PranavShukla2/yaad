package com.pranavshukla.yaad.feature.loans

import com.pranavshukla.yaad.core.finance.Emi
import com.pranavshukla.yaad.core.model.AnnualRate
import com.pranavshukla.yaad.core.model.Currency
import com.pranavshukla.yaad.core.model.Loan
import com.pranavshukla.yaad.core.model.LoanKind
import com.pranavshukla.yaad.core.model.Money
import kotlinx.datetime.LocalDate

/** The loan editor's fields as typed. [toLoan] is null until every field is valid. */
data class LoanForm(
    val id: String,
    val lender: String = "",
    val kind: LoanKind = LoanKind.Home,
    val principal: String = "",
    val rate: String = "",
    val tenure: String = "",
    val tenureInYears: Boolean = true,
    val firstEmiDate: LocalDate,
    val bankEmi: String = "",
    val base: Loan? = null,
) {
    private fun months(): Int? = tenure.trim().toIntOrNull()?.let { if (tenureInYears) it * 12 else it }?.takeIf { it in 1..600 }

    private fun amount(text: String, currency: Currency): Money? =
        runCatching { Money.parse(text, currency) }.getOrNull()?.takeIf { !it.isNegative && !it.isZero }

    fun toLoan(currency: Currency): Loan? {
        if (lender.isBlank()) return null
        val principal = amount(principal, currency) ?: return null
        val rate = runCatching { AnnualRate.percent(rate) }.getOrNull() ?: return null
        val months = months() ?: return null
        val emi = if (bankEmi.isBlank()) null else amount(bankEmi, currency) ?: return null
        val fields = Loan(id, lender.trim(), kind, principal, rate, months, firstEmiDate, emi)
        return base?.copy(
            lender = fields.lender, kind = kind, principal = principal, rate = rate, tenureMonths = months,
            firstEmiDate = firstEmiDate, emi = emi,
        ) ?: fields
    }

    /** The bank's EMI if given, otherwise Yaad's estimate; null until principal, rate and tenure are valid. */
    fun previewEmi(currency: Currency): Money? {
        if (bankEmi.isNotBlank()) return amount(bankEmi, currency)
        val principal = amount(principal, currency) ?: return null
        val rate = runCatching { AnnualRate.percent(rate) }.getOrNull() ?: return null
        return Emi.of(principal, rate, months() ?: return null)
    }

    companion object {
        fun from(loan: Loan): LoanForm {
            val years = loan.tenureMonths % 12 == 0
            return LoanForm(
                id = loan.id, lender = loan.lender, kind = loan.kind,
                principal = plain(loan.principal), rate = rateText(loan.rate),
                tenure = (if (years) loan.tenureMonths / 12 else loan.tenureMonths).toString(), tenureInYears = years,
                firstEmiDate = loan.firstEmiDate, bankEmi = loan.emi?.let(::plain).orEmpty(), base = loan,
            )
        }

        /** "1000000" or "8678.23": an amount as it would be typed. */
        private fun plain(money: Money): String {
            val per = money.currency.minorPerMajor
            val minor = money.minorUnits % per
            return (money.minorUnits / per).toString() +
                if (minor == 0L) "" else "." + minor.toString().padStart(money.currency.minorDigits, '0').trimEnd('0')
        }

        /** 8500 → "8.5". */
        fun rateText(rate: AnnualRate): String {
            val whole = rate.thousandthsOfPercent / 1000
            val fraction = (rate.thousandthsOfPercent % 1000).toString().padStart(3, '0').trimEnd('0')
            return if (fraction.isEmpty()) whole.toString() else "$whole.$fraction"
        }
    }
}
