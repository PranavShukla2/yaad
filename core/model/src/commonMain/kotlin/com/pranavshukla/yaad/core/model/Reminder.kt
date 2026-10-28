package com.pranavshukla.yaad.core.model

import kotlin.time.Duration

/** Ids are UUID strings, so backups from two devices can be merged without clashes. */
data class ReminderId(val value: String)

/** What a reminder is for; decides its template, icon and default alerts. */
enum class ReminderType {
    General, Bill, Emi, CreditCard, Subscription, Insurance, Investment, Document, Warranty, Medicine, Birthday,
    Appointment,
}

/** Built-in categories; users can add their own (stored by id in [Reminder.categoryId]). */
enum class BuiltInCategory(val id: String) {
    Money("money"), Health("health"), Personal("personal"), Work("work"), Home("home"), Vehicle("vehicle"),
}

enum class Priority { Low, Normal, High }

data class ChecklistItem(val text: String, val done: Boolean = false)

/**
 * A reminder, bill, EMI or other dated item. [schedule] says when it is due; everything else says how to remind.
 *
 * @property earlyAlerts extra alerts this long before each occurrence (for example 3 days and 1 hour).
 * @property nagInterval while an occurrence is not done, alert again this often (medicines, payments).
 * @property amount the amount due each time, for money items.
 * @property autoPay paid automatically; Yaad asks "did it go through?" instead of "pay now".
 */
data class Reminder(
    val id: ReminderId,
    val title: String,
    val schedule: Schedule,
    val type: ReminderType = ReminderType.General,
    val notes: String = "",
    val categoryId: String? = null,
    val allDay: Boolean = false,
    val earlyAlerts: List<Duration> = emptyList(),
    val priority: Priority = Priority.Normal,
    val nagInterval: Duration? = null,
    val amount: Money? = null,
    val autoPay: Boolean = false,
    val linkedLoanId: String? = null,
    val checklist: List<ChecklistItem> = emptyList(),
    val archived: Boolean = false,
) {
    init {
        require(earlyAlerts.all { it.isPositive() }) { "Early alerts must be before the due time" }
        require(nagInterval == null || nagInterval.isPositive()) { "Nag interval must be positive" }
    }
}
