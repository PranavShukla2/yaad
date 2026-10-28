package com.pranavshukla.yaad.data

import com.pranavshukla.yaad.core.model.ChecklistItem
import com.pranavshukla.yaad.core.model.Currency
import com.pranavshukla.yaad.core.model.Money
import com.pranavshukla.yaad.core.model.Occurrence
import com.pranavshukla.yaad.core.model.OccurrenceStatus
import com.pranavshukla.yaad.core.model.Priority
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.model.ReminderId
import com.pranavshukla.yaad.core.model.ReminderType
import com.pranavshukla.yaad.core.model.Schedule
import com.pranavshukla.yaad.data.db.Checklist_item
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import com.pranavshukla.yaad.data.db.Occurrence as OccurrenceRow
import com.pranavshukla.yaad.data.db.Reminder as ReminderRow

/** Conversions between database rows and model types. The row formats are documented in Yaad.sq. */
internal fun ReminderRow.toModel(checklist: List<Checklist_item>) = Reminder(
    id = ReminderId(id),
    title = title,
    notes = notes,
    type = ReminderType.valueOf(type),
    categoryId = category_id,
    schedule = Schedule(LocalDateTime.parse(start_local), rule?.let(RuleCodec::decode), time_zone_id),
    allDay = all_day,
    earlyAlerts = early_alert_seconds.split(',').filter { it.isNotEmpty() }.map { it.toLong().seconds },
    priority = Priority.valueOf(priority),
    nagInterval = nag_seconds?.seconds,
    amount = money(amount_minor, amount_currency, amount_minor_digits),
    autoPay = auto_pay,
    linkedLoanId = linked_loan_id,
    checklist = checklist.map { ChecklistItem(it.text, it.done) },
    archived = archived,
)

internal fun Reminder.toRow(updatedAt: Instant) = ReminderRow(
    id = id.value,
    title = title,
    notes = notes,
    type = type.name,
    category_id = categoryId,
    start_local = schedule.start.toString(),
    rule = schedule.rule?.let(RuleCodec::encode),
    time_zone_id = schedule.timeZoneId,
    all_day = allDay,
    early_alert_seconds = earlyAlerts.joinToString(",") { it.inWholeSeconds.toString() },
    priority = priority.name,
    nag_seconds = nagInterval?.inWholeSeconds,
    amount_minor = amount?.minorUnits,
    amount_currency = amount?.currency?.code,
    amount_minor_digits = amount?.currency?.minorDigits?.toLong(),
    auto_pay = autoPay,
    linked_loan_id = linkedLoanId,
    archived = archived,
    updated_at = updatedAt.toEpochMilliseconds(),
)

internal fun OccurrenceRow.toModel() = Occurrence(
    reminderId = ReminderId(reminder_id),
    due = Instant.fromEpochMilliseconds(due_at),
    status = Instant.fromEpochMilliseconds(status_at).let { at ->
        when (status) {
            "done" -> OccurrenceStatus.Done(at)
            "skipped" -> OccurrenceStatus.Skipped(at)
            "snoozed" -> OccurrenceStatus.Snoozed(at)
            else -> error("Unknown occurrence status '$status'")
        }
    },
    paidAmount = money(paid_minor, paid_currency, paid_minor_digits),
    note = note,
)

/** The row for a handled occurrence; pending ones have no row. */
internal fun Occurrence.toRow(): OccurrenceRow {
    val (name, at) = when (val s = status) {
        is OccurrenceStatus.Done -> "done" to s.at
        is OccurrenceStatus.Skipped -> "skipped" to s.at
        is OccurrenceStatus.Snoozed -> "snoozed" to s.until
        OccurrenceStatus.Pending -> error("Pending occurrences are not stored")
    }
    return OccurrenceRow(
        reminder_id = reminderId.value,
        due_at = due.toEpochMilliseconds(),
        status = name,
        status_at = at.toEpochMilliseconds(),
        paid_minor = paidAmount?.minorUnits,
        paid_currency = paidAmount?.currency?.code,
        paid_minor_digits = paidAmount?.currency?.minorDigits?.toLong(),
        note = note,
    )
}

private fun money(minor: Long?, code: String?, digits: Long?): Money? =
    if (minor == null || code == null || digits == null) null else Money(minor, Currency(code, digits.toInt()))
