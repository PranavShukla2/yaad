package com.pranavshukla.yaad.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.SqlDriver
import com.pranavshukla.yaad.core.model.AnnualRate
import com.pranavshukla.yaad.core.model.Currency
import com.pranavshukla.yaad.core.model.Loan
import com.pranavshukla.yaad.core.model.LoanKind
import com.pranavshukla.yaad.core.model.Money
import com.pranavshukla.yaad.core.model.Prepayment
import com.pranavshukla.yaad.core.model.PrepaymentEffect
import com.pranavshukla.yaad.core.model.RateChange
import com.pranavshukla.yaad.core.model.RateChangeKeeps
import com.pranavshukla.yaad.data.db.Loan_event
import com.pranavshukla.yaad.data.db.YaadDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock
import com.pranavshukla.yaad.data.db.Loan as LoanRow

/** Loans with their prepayments and rate changes, on the device. */
class LoanStore(
    driver: SqlDriver,
    private val clock: Clock = Clock.System,
    private val context: CoroutineContext = Dispatchers.Default,
) {
    private val q = YaadDatabase(driver).loanQueries

    fun loans(): Flow<List<Loan>> = q.selectLoans().asFlow().mapToList(context).map { rows ->
        val events = q.selectAllLoanEvents().executeAsList().groupBy { it.loan_id }
        rows.map { it.toModel(events[it.id].orEmpty()) }
    }

    fun loan(id: String): Loan? = q.selectLoan(id).executeAsOneOrNull()
        ?.toModel(q.selectAllLoanEvents().executeAsList().filter { it.loan_id == id })

    /** Adds or replaces [loan]. Deleting and re-inserting is safe: only its events refer to it. */
    fun save(loan: Loan) = q.transaction {
        q.deleteLoan(loan.id)
        q.insertLoan(
            LoanRow(
                loan.id, loan.lender, loan.kind.name, loan.principal.minorUnits, loan.principal.currency.code,
                loan.principal.currency.minorDigits.toLong(), loan.rate.thousandthsOfPercent.toLong(),
                loan.tenureMonths.toLong(), loan.firstEmiDate.toString(), loan.emi?.minorUnits,
                clock.now().toEpochMilliseconds(),
            ),
        )
        val events = loan.prepayments.map {
            Triple(it.afterInstalment, if (it.reduce == PrepaymentEffect.Tenure) "prepay_tenure" else "prepay_emi", it.amount.minorUnits to null)
        } + loan.rateChanges.map {
            Triple(it.afterInstalment, if (it.keep == RateChangeKeeps.Emi) "rate_keep_emi" else "rate_keep_tenure", null to it.rate.thousandthsOfPercent.toLong())
        }
        events.forEachIndexed { position, (after, kind, values) ->
            q.insertLoanEvent(Loan_event(loan.id, position.toLong(), after.toLong(), kind, values.first, values.second))
        }
    }

    fun delete(id: String) = q.deleteLoan(id)
}

private fun LoanRow.toModel(events: List<Loan_event>): Loan {
    val currency = Currency(currency, minor_digits.toInt())
    return Loan(
        id = id,
        lender = lender,
        kind = LoanKind.valueOf(kind),
        principal = Money(principal_minor, currency),
        rate = AnnualRate(rate_thousandths.toInt()),
        tenureMonths = tenure_months.toInt(),
        firstEmiDate = LocalDate.parse(first_emi_date),
        emi = emi_minor?.let { Money(it, currency) },
        prepayments = events.filter { it.kind.startsWith("prepay") }.map {
            Prepayment(
                it.after_instalment.toInt(),
                Money(requireNotNull(it.amount_minor), currency),
                if (it.kind == "prepay_tenure") PrepaymentEffect.Tenure else PrepaymentEffect.Emi,
            )
        },
        rateChanges = events.filter { it.kind.startsWith("rate") }.map {
            RateChange(
                it.after_instalment.toInt(),
                AnnualRate(requireNotNull(it.rate_thousandths).toInt()),
                if (it.kind == "rate_keep_emi") RateChangeKeeps.Emi else RateChangeKeeps.Tenure,
            )
        },
    )
}
