package com.pranavshukla.yaad.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.SqlDriver
import com.pranavshukla.yaad.core.model.Occurrence
import com.pranavshukla.yaad.core.model.OccurrenceStatus
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.model.ReminderId
import com.pranavshukla.yaad.data.db.Checklist_item
import com.pranavshukla.yaad.data.db.YaadDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Reminders and what happened to their occurrences, on the device. Every write is one transaction; [reminders]
 * emits again after each one.
 */
class ReminderStore(
    driver: SqlDriver,
    private val clock: Clock = Clock.System,
    private val context: CoroutineContext = Dispatchers.Default,
) {
    internal val db = YaadDatabase(driver)
    private val q = db.yaadQueries

    /** Every reminder, archived ones included, by first due date. */
    fun reminders(): Flow<List<Reminder>> =
        q.selectReminders().asFlow().mapToList(context).map { rows ->
            val checklists = q.selectAllChecklists().executeAsList().groupBy { it.reminder_id }
            rows.map { it.toModel(checklists[it.id].orEmpty()) }
        }

    fun reminder(id: ReminderId): Reminder? =
        q.selectReminder(id.value).executeAsOneOrNull()?.toModel(q.selectChecklist(id.value).executeAsList())

    /** Adds [reminder], or replaces the one with its id, keeping that one's occurrence history. */
    fun save(reminder: Reminder) = q.transaction {
        val row = reminder.toRow(clock.now())
        if (q.reminderExists(row.id).executeAsOne()) {
            with(row) {
                q.updateReminder(
                    title, notes, type, category_id, start_local, rule, time_zone_id, all_day, early_alert_seconds,
                    priority, nag_seconds, amount_minor, amount_currency, amount_minor_digits, auto_pay, linked_loan_id,
                    archived, updated_at, id,
                )
            }
        } else {
            q.insertReminder(row)
        }
        q.deleteChecklist(row.id)
        reminder.checklist.forEachIndexed { i, item ->
            q.insertChecklistItem(Checklist_item(row.id, i.toLong(), item.text, item.done))
        }
    }

    /** Deletes the reminder with its checklist and history. */
    fun delete(id: ReminderId) = q.deleteReminder(id.value)

    /** The handled (done, skipped, snoozed) occurrences of one reminder, oldest first. */
    fun occurrences(id: ReminderId): List<Occurrence> = q.selectOccurrences(id.value).executeAsList().map { it.toModel() }

    /** Every handled occurrence, as a flow, for the schedule to read. */
    fun allOccurrences(): Flow<List<Occurrence>> =
        q.selectAllOccurrences().asFlow().mapToList(context).map { rows -> rows.map { it.toModel() } }

    /** Records what happened to an occurrence; recording [OccurrenceStatus.Pending] clears it. */
    fun record(occurrence: Occurrence) = q.transaction {
        q.deleteOccurrence(occurrence.reminderId.value, occurrence.due.toEpochMilliseconds())
        if (occurrence.status != OccurrenceStatus.Pending) q.insertOccurrence(occurrence.toRow())
    }

    fun clear(id: ReminderId, due: Instant) = q.deleteOccurrence(id.value, due.toEpochMilliseconds())
}
