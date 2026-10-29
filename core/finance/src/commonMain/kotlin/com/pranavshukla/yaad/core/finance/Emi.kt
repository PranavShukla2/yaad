package com.pranavshukla.yaad.core.finance

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.DecimalMode
import com.ionspin.kotlin.bignum.decimal.RoundingMode
import com.pranavshukla.yaad.core.model.AnnualRate
import com.pranavshukla.yaad.core.model.Money

/**
 * Reducing-balance EMI: EMI = P·r·(1+r)ⁿ / ((1+r)ⁿ − 1), r = annual rate / 12. Computed with 60-digit decimals and
 * rounded half up to the paisa; monthly interest is exact integer arithmetic in minor units.
 */
object Emi {
    private val mode = DecimalMode(60, RoundingMode.ROUND_HALF_AWAY_FROM_ZERO)

    /** Thousandths of a percent a year → per month: rate / (1000 × 100 × 12). */
    private const val MonthlyDivisor = 1_200_000L

    fun of(principal: Money, rate: AnnualRate, months: Int): Money {
        require(months >= 1) { "At least one instalment" }
        val minor = principal.minorUnits
        if (rate.thousandthsOfPercent == 0) return Money(divideHalfUp(minor, months.toLong()), principal.currency)
        val r = BigDecimal.fromInt(rate.thousandthsOfPercent).divide(BigDecimal.fromLong(MonthlyDivisor), mode)
        val growth = power(BigDecimal.ONE + r, months)
        val emi = BigDecimal.fromLong(minor).multiply(r, mode).multiply(growth, mode).divide(growth - BigDecimal.ONE, mode)
        return Money(emi.roundToDigitPositionAfterDecimalPoint(0, RoundingMode.ROUND_HALF_AWAY_FROM_ZERO).longValue(), principal.currency)
    }

    /** A month's interest on [balance], rounded half up to the minor unit. */
    fun monthlyInterest(balance: Money, rate: AnnualRate): Money =
        Money(divideHalfUp(balance.minorUnits * rate.thousandthsOfPercent, MonthlyDivisor), balance.currency)

    /**
     * How many instalments of [emi] repay [principal]. The last pays what is left; a last one under 1% of the EMI
     * is only rounding left over, so it is folded into the one before.
     *
     * @throws IllegalArgumentException if [emi] does not cover the first month's interest.
     */
    fun monthsToRepay(principal: Money, rate: AnnualRate, emi: Money): Int {
        require(emi > monthlyInterest(principal, rate)) { "An EMI of $emi never repays this loan" }
        var balance = principal
        var months = 0
        while (true) {
            months++
            val due = balance + monthlyInterest(balance, rate)
            if (due <= emi) break
            balance = due - emi
            if (balance.minorUnits * 100 < emi.minorUnits) {
                // What remains after this EMI is rounding: this is the last instalment.
                break
            }
        }
        return months
    }

    private fun power(base: BigDecimal, exponent: Int): BigDecimal {
        var result = BigDecimal.ONE
        var square = base
        var e = exponent
        while (e > 0) {
            if (e and 1 == 1) result = result.multiply(square, mode)
            square = square.multiply(square, mode)
            e = e shr 1
        }
        return result
    }
}

/** a / b rounded half up, for a ≥ 0 and b > 0. */
internal fun divideHalfUp(a: Long, b: Long): Long = (a * 2 + b) / (b * 2)
