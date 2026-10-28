package com.pranavshukla.yaad.feature.loans

import com.pranavshukla.yaad.core.finance.Amortisation
import com.pranavshukla.yaad.core.finance.AmortisationSchedule
import com.pranavshukla.yaad.core.finance.LoanSummary
import com.pranavshukla.yaad.core.finance.LoanTracker
import com.pranavshukla.yaad.core.finance.TrackedInstalment
import com.pranavshukla.yaad.core.model.Loan
import com.pranavshukla.yaad.core.model.Occurrence
import com.pranavshukla.yaad.core.model.OccurrenceStatus
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.recurrence.occurrences
import com.pranavshukla.yaad.core.scheduling.LoanLink
import com.pranavshukla.yaad.data.LoanStore
import com.pranavshukla.yaad.data.ReminderStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** A loan with everything the screens show about it, all computed by `core:finance`. */
data class LoanView(
    val loan: Loan,
    val schedule: AmortisationSchedule,
    val tracked: List<TrackedInstalment>,
    val summary: LoanSummary,
    val reminder: Reminder?,
)

data class LoansState(val loans: List<LoanView> = emptyList()) {
    fun loan(id: String): LoanView? = loans.firstOrNull { it.loan.id == id }
}

/**
 * The loan screens' state and actions. A loan's EMIs are one reminder (see [LoanLink]); marking an instalment paid
 * marks that occurrence done. Every change re-plans the notifications through [onChanged].
 */
class LoansModel(
    private val loans: LoanStore,
    private val reminders: ReminderStore,
    private val scope: CoroutineScope,
    private val clock: Clock = Clock.System,
    private val zone: () -> TimeZone = { TimeZone.currentSystemDefault() },
    private val onChanged: () -> Unit = {},
    private val io: CoroutineDispatcher = Dispatchers.Default,
    tick: Flow<Unit> = hourly(),
) {
    val state: StateFlow<LoansState> =
        combine(loans.loans(), reminders.reminders(), reminders.allOccurrences(), tick.onStart { emit(Unit) }) { list, all, history, _ ->
            val today = clock.now().toLocalDateTime(zone()).date
            LoansState(
                list.map { loan ->
                    val schedule = Amortisation.schedule(loan)
                    val reminder = all.firstOrNull { it.linkedLoanId == loan.id }
                    val paid = reminder?.let { LoanLink.paidInstalments(it, history, zone()) }.orEmpty()
                    LoanView(loan, schedule, LoanTracker.track(schedule, paid, today), LoanTracker.summary(schedule, paid), reminder)
                },
            )
        }.stateIn(scope, SharingStarted.Eagerly, LoansState())

    @OptIn(ExperimentalUuidApi::class)
    fun newId(): String = Uuid.random().toString()

    /** Saves [loan] and creates or updates its EMI reminder ([emiTitle] names a new one). */
    fun save(loan: Loan, emiTitle: String) = write {
        loans.save(loan)
        val schedule = Amortisation.schedule(loan)
        val existing = reminders.reminder(LoanLink.reminderId(loan))
        val next = schedule.instalments.first().payment
        reminders.save(LoanLink.reminderFor(loan, schedule.instalments.size, next, emiTitle, existing, zone()))
    }

    fun delete(loan: Loan) = write {
        reminders.delete(LoanLink.reminderId(loan))
        loans.delete(loan.id)
    }

    /** Marks instalment [number] paid (or, if it is paid, unpaid) through its EMI reminder occurrence. */
    fun markPaid(view: LoanView, number: Int) = write {
        val reminder = view.reminder ?: return@write
        val due = reminder.schedule.occurrences(reminder.schedule.timeZoneId?.let(TimeZone::of) ?: zone())
            .drop(number - 1).firstOrNull() ?: return@write
        val current = reminders.occurrences(reminder.id).firstOrNull { it.due == due }?.status
        if (current is OccurrenceStatus.Done) {
            reminders.clear(reminder.id, due)
        } else {
            reminders.record(Occurrence(reminder.id, due, OccurrenceStatus.Done(clock.now())))
        }
    }

    private val writes = Mutex()

    private fun write(block: () -> Unit) {
        scope.launch {
            writes.withLock { withContext(io) { block() } }
            onChanged()
        }
    }

    private companion object {
        /** Late instalments change with the date, so the state is recomputed hourly too. */
        fun hourly(): Flow<Unit> = flow {
            while (true) {
                delay(1.hours)
                emit(Unit)
            }
        }
    }
}
