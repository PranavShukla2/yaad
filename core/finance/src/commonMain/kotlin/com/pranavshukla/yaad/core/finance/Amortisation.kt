package com.pranavshukla.yaad.core.finance

import com.pranavshukla.yaad.core.model.Loan
import com.pranavshukla.yaad.core.model.Money
import com.pranavshukla.yaad.core.model.PrepaymentEffect
import com.pranavshukla.yaad.core.model.RateChangeKeeps
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** One EMI: what was paid, how much was interest and principal, any part-payment with it, and what is left. */
data class Instalment(
    val number: Int,
    val date: LocalDate,
    val payment: Money,
    val interest: Money,
    val principal: Money,
    val prepaid: Money,
    val balance: Money,
)

data class AmortisationSchedule(val instalments: List<Instalment>) {
    val totalInterest: Money get() = instalments.map { it.interest }.reduce(Money::plus)
    val totalPaid: Money get() = instalments.map { it.payment + it.prepaid }.reduce(Money::plus)

    /** Where the loan stands after [paid] instalments. */
    fun progressAfter(paid: Int): LoanProgress {
        val first = instalments.first()
        val start = first.balance + first.principal + first.prepaid
        val outstanding = if (paid == 0) start else instalments[paid - 1].balance
        return LoanProgress(instalments.size - paid, outstanding, start - outstanding)
    }
}

data class LoanProgress(val instalmentsLeft: Int, val outstanding: Money, val principalRepaid: Money)

data class Foreclosure(val outstanding: Money, val charge: Money) {
    val total: Money get() = outstanding + charge
}

/**
 * The month-by-month schedule of a loan, applying its prepayments and rate changes in order. Interest is rounded to
 * the paisa each month and the last instalment absorbs the difference, so the balance ends at exactly zero.
 */
object Amortisation {
    fun schedule(loan: Loan): AmortisationSchedule {
        var rate = loan.rate
        var emi = loan.emi ?: Emi.of(loan.principal, rate, loan.tenureMonths)
        var lastInstalment = loan.tenureMonths
        var balance = loan.principal
        val zero = Money.zero(loan.principal.currency)
        val rows = mutableListOf<Instalment>()
        var month = 0
        while (month < lastInstalment && !balance.isZero) {
            month++
            val interest = Emi.monthlyInterest(balance, rate)
            val payment = if (month == lastInstalment) balance + interest else minOf(emi, balance + interest)
            var principal = payment - interest
            balance -= principal

            for (change in loan.rateChanges.filter { it.afterInstalment == month }) {
                rate = change.rate
                when (change.keep) {
                    RateChangeKeeps.Tenure -> emi = Emi.of(balance, rate, lastInstalment - month)
                    RateChangeKeeps.Emi -> lastInstalment = month + Emi.monthsToRepay(balance, rate, emi)
                }
            }
            var prepaid = zero
            for (prepayment in loan.prepayments.filter { it.afterInstalment == month }) {
                val amount = minOf(prepayment.amount, balance)
                prepaid += amount
                balance -= amount
                if (balance.isZero) break
                when (prepayment.reduce) {
                    PrepaymentEffect.Tenure -> lastInstalment = month + Emi.monthsToRepay(balance, rate, emi)
                    PrepaymentEffect.Emi -> emi = Emi.of(balance, rate, lastInstalment - month)
                }
            }
            rows += Instalment(month, loan.firstEmiDate.plus(month - 1, DateTimeUnit.MONTH), payment, interest, principal, prepaid, balance)
        }
        return AmortisationSchedule(rows)
    }

    /** How much less interest the loan's prepayments cost, compared with the same loan without them. */
    fun interestSaved(loan: Loan): Money =
        schedule(loan.copy(prepayments = emptyList())).totalInterest - schedule(loan).totalInterest

    /**
     * Closing the loan after [afterInstalment] EMIs: the outstanding principal plus a charge of
     * [chargeThousandthsOfPercent] (2% is 2000) on it, rounded half up.
     */
    fun foreclosure(schedule: AmortisationSchedule, afterInstalment: Int, chargeThousandthsOfPercent: Int): Foreclosure {
        val outstanding = schedule.progressAfter(afterInstalment).outstanding
        val charge = Money(divideHalfUp(outstanding.minorUnits * chargeThousandthsOfPercent, 100_000), outstanding.currency)
        return Foreclosure(outstanding, charge)
    }
}
