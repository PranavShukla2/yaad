package com.pranavshukla.yaad.feature.reminders

import com.pranavshukla.yaad.core.model.Occurrence
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.model.ReminderId
import com.pranavshukla.yaad.core.scheduling.Agenda
import com.pranavshukla.yaad.core.scheduling.AgendaItem
import com.pranavshukla.yaad.core.scheduling.AgendaView
import com.pranavshukla.yaad.core.scheduling.AlertAction
import com.pranavshukla.yaad.core.scheduling.toOccurrence
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
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Everything the reminder screens show, computed by `core:scheduling`. */
data class RemindersState(
    val reminders: List<Reminder> = emptyList(),
    val history: List<Occurrence> = emptyList(),
    val agenda: AgendaView = AgendaView(emptyList(), emptyList(), emptyMap()),
    val now: Instant = Instant.DISTANT_PAST,
    val zone: TimeZone = TimeZone.UTC,
) {
    fun reminder(id: ReminderId): Reminder? = reminders.firstOrNull { it.id == id }
    fun historyOf(id: ReminderId): List<Occurrence> = history.filter { it.reminderId == id }.sortedByDescending { it.due }
}

/**
 * The reminder screens' state and actions. Every change is saved, then [onChanged] re-plans the notifications. The
 * agenda is recomputed after each change and every minute, so "overdue" stays true to the clock.
 */
class RemindersModel(
    private val store: ReminderStore,
    private val scope: CoroutineScope,
    private val clock: Clock = Clock.System,
    private val zone: () -> TimeZone = { TimeZone.currentSystemDefault() },
    private val onChanged: () -> Unit = {},
    private val io: CoroutineDispatcher = Dispatchers.Default,
    tick: Flow<Unit> = everyMinute(),
) {
    val state: StateFlow<RemindersState> =
        combine(store.reminders(), store.allOccurrences(), tick.onStart { emit(Unit) }) { reminders, history, _ ->
            val now = clock.now()
            val zone = zone()
            RemindersState(reminders, history, Agenda.build(reminders, history, now, zone), now, zone)
        }.stateIn(scope, SharingStarted.Eagerly, RemindersState())

    @OptIn(ExperimentalUuidApi::class)
    fun newId(): ReminderId = ReminderId(Uuid.random().toString())

    fun save(reminder: Reminder) = write { store.save(reminder) }

    fun delete(id: ReminderId) = write { store.delete(id) }

    /** Done, Skip or Snooze from a list or the detail screen; "tomorrow" means tomorrow at 9:00. */
    fun act(item: AgendaItem, action: AlertAction) = write {
        store.record(action.toOccurrence(item.reminder.id, item.due, clock.now(), zone(), Morning))
    }

    /** Puts a handled occurrence back to pending. */
    fun undo(item: AgendaItem) = write { store.clear(item.reminder.id, item.due) }

    fun toggleChecklist(reminder: Reminder, index: Int) = write {
        val current = store.reminder(reminder.id) ?: return@write
        val items = current.checklist.mapIndexed { i, it -> if (i == index) it.copy(done = !it.done) else it }
        store.save(current.copy(checklist = items))
    }

    /** Writes run one at a time: SQLite allows one transaction per connection. */
    private fun write(block: () -> Unit) {
        scope.launch {
            writes.withLock { withContext(io) { block() } }
            onChanged()
        }
    }

    private val writes = Mutex()

    private companion object {
        val Morning = LocalTime(9, 0)

        fun everyMinute(): Flow<Unit> = flow {
            while (true) {
                delay(1.minutes)
                emit(Unit)
            }
        }
    }
}
