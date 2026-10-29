package com.pranavshukla.yaad.core.scheduling

import com.pranavshukla.yaad.core.model.Money
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.model.ReminderType

enum class SearchStatus { Active, Archived, All }

/** Every field is optional; amounts compare only with amounts in the same currency. */
data class SearchFilter(
    val text: String = "",
    val categoryId: String? = null,
    val type: ReminderType? = null,
    val minAmount: Money? = null,
    val maxAmount: Money? = null,
    val status: SearchStatus = SearchStatus.Active,
)

object ReminderSearch {
    /** Reminders matching [filter], by title; text matches the title or notes, ignoring case. */
    fun filter(reminders: List<Reminder>, filter: SearchFilter): List<Reminder> {
        val words = filter.text.trim().lowercase().split(Regex("""\s+""")).filter { it.isNotEmpty() }
        return reminders.filter { r ->
            val haystack = "${r.title} ${r.notes}".lowercase()
            words.all { it in haystack } &&
                (filter.categoryId == null || r.categoryId == filter.categoryId) &&
                (filter.type == null || r.type == filter.type) &&
                (filter.minAmount == null || r.amount.let { it != null && it.currency == filter.minAmount.currency && it >= filter.minAmount }) &&
                (filter.maxAmount == null || r.amount.let { it != null && it.currency == filter.maxAmount.currency && it <= filter.maxAmount }) &&
                when (filter.status) {
                    SearchStatus.Active -> !r.archived
                    SearchStatus.Archived -> r.archived
                    SearchStatus.All -> true
                }
        }.sortedBy { it.title.lowercase() }
    }
}
