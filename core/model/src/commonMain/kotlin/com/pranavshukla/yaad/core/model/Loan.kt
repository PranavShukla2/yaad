package com.pranavshukla.yaad.core.model

import kotlinx.datetime.LocalDate

/**
 * An annual interest rate in thousandths of a percent, so 8.5% is 8500 and 8.375% is 8375. Whole numbers only: rates
 * are never floating point.
 */
data class AnnualRate(val thousandthsOfPercent: Int) {
    init {
        require(thousandthsOfPercent in 0..100_000) { "Rate must be 0–100%, was $thousandthsOfPercent thousandths" }
    }

    companion object {
        /** "8.5" or "8.375" → AnnualRate; at most three decimal places. */
        fun percent(text: String): AnnualRate {
            val (whole, fraction) = (text.trim().split('.') + "").let { it[0] to it[1] }
            require(whole.isNotEmpty() && whole.all(Char::isDigit) && fraction.length <= 3 && fraction.all(Char::isDigit)) {
                "Not a rate: '$text'"
            }
            return AnnualRate(whole.toInt() * 1000 + fraction.padEnd(3, '0').toInt())
        }
    }
}

enum class LoanKind { Home, Car, Personal, Education, Gold, ConsumerDurable, Other }

/** A part-payment made with the EMI of [afterInstalment]; it shortens the loan or lowers the EMI. */
data class Prepayment(val afterInstalment: Int, val amount: Money, val reduce: PrepaymentEffect)

enum class PrepaymentEffect { Tenure, Emi }

/** A floating-rate change that applies from the instalment after [afterInstalment]. */
data class RateChange(val afterInstalment: Int, val rate: AnnualRate, val keep: RateChangeKeeps)

enum class RateChangeKeeps { Emi, Tenure }

/**
 * A loan repaid in equal monthly instalments on a reducing balance.
 *
 * @property emi the EMI the bank gave, when known; otherwise Yaad computes it (and labels it an estimate).
 */
data class Loan(
    val id: String,
    val lender: String,
    val kind: LoanKind,
    val principal: Money,
    val rate: AnnualRate,
    val tenureMonths: Int,
    val firstEmiDate: LocalDate,
    val emi: Money? = null,
    val prepayments: List<Prepayment> = emptyList(),
    val rateChanges: List<RateChange> = emptyList(),
) {
    init {
        require(tenureMonths >= 1) { "Tenure must be at least one month" }
        require(!principal.isNegative && !principal.isZero) { "Principal must be positive" }
    }
}
