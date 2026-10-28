package com.pranavshukla.yaad.data

import app.cash.sqldelight.db.SqlDriver
import com.pranavshukla.yaad.data.db.Category
import com.pranavshukla.yaad.data.db.Checklist_item
import com.pranavshukla.yaad.data.db.Occurrence
import com.pranavshukla.yaad.data.db.Reminder
import com.pranavshukla.yaad.data.db.YaadDatabase
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.time.Clock

class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exports everything to one JSON file and restores it, replacing what is on the device. A file is checked in full
 * before anything is written, so a bad file changes nothing. Encryption is added in Phase 10.
 */
class Backup(driver: SqlDriver, private val clock: Clock = Clock.System) {
    private val q = YaadDatabase(driver).yaadQueries

    fun export(): String = json.encodeToString(
        BackupFile.serializer(),
        BackupFile(
            exportedAt = clock.now().toString(),
            reminders = q.selectReminders().executeAsList().map { it.toDto() },
            checklist = q.selectAllChecklists().executeAsList().map { it.toDto() },
            occurrences = q.selectAllOccurrences().executeAsList().map { it.toDto() },
            categories = q.selectCategories().executeAsList().map { it.toDto() },
            settings = q.selectSettings().executeAsList().associate { it.key to it.value_ },
        ),
    )

    /** @throws BackupException if [text] is not a Yaad backup this version can read; the data is then unchanged. */
    fun import(text: String) {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: SerializationException) {
            throw BackupException("Not a Yaad backup", e)
        } catch (e: IllegalArgumentException) {
            throw BackupException("Not a Yaad backup", e)
        }
        if (file.format != Format) throw BackupException("Not a Yaad backup")
        if (file.version > Version) throw BackupException("This backup is from a newer version of Yaad")
        val reminders = file.reminders.map { it.toRow() }
        val occurrences = file.occurrences.map { it.toRow() }
        try {
            reminders.forEach { it.toModel(emptyList()) }
            occurrences.forEach { it.toModel() }
        } catch (e: IllegalArgumentException) {
            throw BackupException("The backup has an item Yaad cannot read", e)
        } catch (e: IllegalStateException) {
            throw BackupException("The backup has an item Yaad cannot read", e)
        }
        // A row that breaks a constraint (an occurrence of a missing reminder) rolls the whole import back.
        try {
            restore(file, reminders, occurrences)
        } catch (e: Exception) {
            throw BackupException("The backup could not be restored", e)
        }
    }

    private fun restore(
        file: BackupFile,
        reminders: List<Reminder>,
        occurrences: List<Occurrence>,
    ) {
        q.transaction {
            q.deleteAll()
            reminders.forEach(q::insertReminder)
            file.checklist.forEach { q.insertChecklistItem(it.toRow()) }
            occurrences.forEach(q::insertOccurrence)
            file.categories.forEach { q.insertCategory(it.toRow()) }
            file.settings.forEach { (key, value) -> q.putSetting(key, value) }
        }
    }

    private companion object {
        const val Format = "yaad-backup"
        const val Version = 1
        val json = Json { encodeDefaults = true }
    }

    /** Version 1 of the file: the database rows of schema version 1 (see Yaad.sq), with camelCase names. */
    @Serializable
    private data class BackupFile(
        val format: String = Format,
        val version: Int = Version,
        val exportedAt: String,
        val reminders: List<ReminderDto> = emptyList(),
        val checklist: List<ChecklistDto> = emptyList(),
        val occurrences: List<OccurrenceDto> = emptyList(),
        val categories: List<CategoryDto> = emptyList(),
        val settings: Map<String, String> = emptyMap(),
    )
}

@Serializable
private data class ReminderDto(
    val id: String, val title: String, val notes: String, val type: String, val categoryId: String?,
    val startLocal: String, val rule: String?, val timeZoneId: String?, val allDay: Boolean,
    val earlyAlertSeconds: String, val priority: String, val nagSeconds: Long?, val amountMinor: Long?,
    val amountCurrency: String?, val amountMinorDigits: Long?, val autoPay: Boolean, val linkedLoanId: String?,
    val archived: Boolean, val updatedAt: Long,
) {
    fun toRow() = Reminder(
        id, title, notes, type, categoryId, startLocal, rule, timeZoneId, allDay, earlyAlertSeconds, priority,
        nagSeconds, amountMinor, amountCurrency, amountMinorDigits, autoPay, linkedLoanId, archived, updatedAt,
    )
}

private fun Reminder.toDto() = ReminderDto(
    id, title, notes, type, category_id, start_local, rule, time_zone_id, all_day, early_alert_seconds, priority,
    nag_seconds, amount_minor, amount_currency, amount_minor_digits, auto_pay, linked_loan_id, archived, updated_at,
)

@Serializable
private data class ChecklistDto(val reminderId: String, val position: Long, val text: String, val done: Boolean) {
    fun toRow() = Checklist_item(reminderId, position, text, done)
}

private fun Checklist_item.toDto() = ChecklistDto(reminder_id, position, text, done)

@Serializable
private data class OccurrenceDto(
    val reminderId: String, val dueAt: Long, val status: String, val statusAt: Long, val paidMinor: Long?,
    val paidCurrency: String?, val paidMinorDigits: Long?, val note: String,
) {
    fun toRow() = Occurrence(reminderId, dueAt, status, statusAt, paidMinor, paidCurrency, paidMinorDigits, note)
}

private fun Occurrence.toDto() =
    OccurrenceDto(reminder_id, due_at, status, status_at, paid_minor, paid_currency, paid_minor_digits, note)

@Serializable
private data class CategoryDto(val id: String, val name: String, val color: Long?) {
    fun toRow() = Category(id, name, color)
}

private fun Category.toDto() = CategoryDto(id, name, color)
