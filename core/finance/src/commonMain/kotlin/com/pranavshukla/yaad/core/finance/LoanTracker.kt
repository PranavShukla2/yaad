package com.pranavshukla.yaad.core.finance

import com.pranavshukla.yaad.core.model.Money
import kotlinx.datetime.LocalDate

enum class InstalmentStatus { Paid, Due, Late }

data class TrackedInstalment(val instalment: Instalment, val status: InstalmentStatus)

/**
 * @property repaidThousandthsOfPercent principal repaid as a share of the loan, in thousandths of a percent.
 * @property nextPayment the first unpaid instalment's amount, or null when all are paid.
 */
data class LoanSummary(
    val paid: Int,
    val left: Int,
    val outstanding: Money,
    val repaidThousandthsOfPercent: Int,
    val nextPayment: Money?,
)

/** Which instalments are paid, due or late, and where the loan stands. Paid instalments come from EMI reminders. */
object LoanTracker {
    fun track(schedule: AmortisationSchedule, paid: Set<Int>, today: LocalDate): List<TrackedInstalment> =
        schedule.instalments.map { row ->
            val status = when {
                row.number in paid -> InstalmentStatus.Paid
                row.date < today -> InstalmentStatus.Late
                else -> InstalmentStatus.Due
            }
            TrackedInstalment(row, status)
        }

    fun summary(schedule: AmortisationSchedule, paid: Set<Int>): LoanSummary {
        val rows = schedule.instalments
        val first = rows.first()
        val principal = first.balance + first.principal + first.prepaid
        val repaid = rows.filter { it.number in paid }.map { it.principal + it.prepaid }.fold(Money.zero(principal.currency), Money::plus)
        val count = rows.count { it.number in paid }
        return LoanSummary(
            paid = count,
            left = rows.size - count,
            outstanding = principal - repaid,
            repaidThousandthsOfPercent = divideHalfUp(repaid.minorUnits * 100_000, principal.minorUnits).toInt(),
            nextPayment = rows.firstOrNull { it.number !in paid }?.payment,
        )
    }
}
