package com.pranavshukla.yaad.feature.money

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pranavshukla.yaad.core.finance.MoneyFormat
import com.pranavshukla.yaad.core.model.BuiltInCategory
import com.pranavshukla.yaad.core.model.Currency
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.model.ReminderType
import com.pranavshukla.yaad.core.scheduling.Agenda
import com.pranavshukla.yaad.core.scheduling.MoneySummary
import com.pranavshukla.yaad.feature.loans.LoanCard
import com.pranavshukla.yaad.feature.loans.LoanView
import com.pranavshukla.yaad.feature.loans.LoansState
import com.pranavshukla.yaad.feature.money.resources.Res
import com.pranavshukla.yaad.feature.money.resources.*
import com.pranavshukla.yaad.feature.reminders.RemindersState
import com.pranavshukla.yaad.feature.reminders.categoryLabel
import com.pranavshukla.yaad.ui.designsystem.components.AmountText
import com.pranavshukla.yaad.ui.designsystem.components.DueStatus
import com.pranavshukla.yaad.ui.designsystem.components.EmptyState
import com.pranavshukla.yaad.ui.designsystem.components.ItemCard
import com.pranavshukla.yaad.ui.designsystem.components.SectionHeader
import com.pranavshukla.yaad.ui.designsystem.components.dateText
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

/** This month's money, the loans, then every other money item; all figures come from `core:*`. */
@Composable
fun MoneyHome(
    reminders: RemindersState,
    loans: LoansState,
    onOpenLoan: (LoanView) -> Unit,
    onOpenItem: (Reminder) -> Unit,
    modifier: Modifier = Modifier,
    currency: Currency = Currency.INR,
    contentPadding: PaddingValues = PaddingValues(16.dp),
) {
    val today = reminders.now.toLocalDateTime(reminders.zone).date
    val month = remember(reminders) { MoneySummary.month(reminders.reminders, reminders.history, today.year, today.month.number, reminders.zone, currency) }
    val largest = remember(reminders) { MoneySummary.upcomingLargest(reminders.reminders, reminders.history, reminders.now, reminders.zone, currency = currency, limit = 3) }
    val items = reminders.reminders.filter { it.linkedLoanId == null && it.isMoneyItem() }
    if (loans.loans.isEmpty() && items.isEmpty()) {
        EmptyState(stringResource(Res.string.empty_title), stringResource(Res.string.empty_message), modifier)
        return
    }
    LazyColumn(modifier, contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "month") {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(Res.string.this_month), style = MaterialTheme.typography.titleMedium)
                    Figure(stringResource(Res.string.due), MoneyFormat.format(month.due))
                    Figure(stringResource(Res.string.paid), MoneyFormat.format(month.paid))
                    Figure(stringResource(Res.string.pending), MoneyFormat.format(month.pending))
                    Figure(stringResource(Res.string.emi_outgo), MoneyFormat.format(month.emiOutgo))
                    if (month.byCategory.size > 1) {
                        Text(stringResource(Res.string.by_category), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                        month.byCategory.entries.sortedByDescending { it.value.minorUnits }.forEach { (id, total) ->
                            val name = BuiltInCategory.entries.firstOrNull { it.id == id }?.let { categoryLabel(it) } ?: stringResource(Res.string.other_category)
                            Figure(name, MoneyFormat.format(total))
                        }
                    }
                    if (largest.isNotEmpty()) {
                        Text(stringResource(Res.string.coming_up), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                        largest.forEach { Figure("${it.reminder.title} · ${dateText(it.date, today)}", MoneyFormat.format(it.amount!!)) }
                    }
                }
            }
        }
        if (loans.loans.isNotEmpty()) {
            item(key = "loans") { SectionHeader(stringResource(Res.string.loans), trailing = loans.loans.size.toString()) }
            items(loans.loans, key = { "loan-${it.loan.id}" }) { LoanCard(it, onOpenLoan) }
        }
        if (items.isNotEmpty()) {
            item(key = "items") { SectionHeader(stringResource(Res.string.money_items), trailing = items.size.toString()) }
            items(items, key = { "item-${it.id.value}" }) { reminder ->
                val next = Agenda.nextDue(reminder, reminders.history, reminders.now, reminders.zone)
                ItemCard(
                    title = reminder.title,
                    dueText = next?.let { dateText(it.toLocalDateTime(reminders.zone).date, today) } ?: "",
                    status = when {
                        next == null -> DueStatus.Done
                        next < reminders.now -> DueStatus.Overdue
                        next.toLocalDateTime(reminders.zone).date == today -> DueStatus.DueToday
                        else -> DueStatus.Upcoming
                    },
                    onClick = { onOpenItem(reminder) },
                    amountText = reminder.amount?.let(MoneyFormat::format),
                    supportingText = if (reminder.amount == null) stringResource(Res.string.amount_on_paying) else null,
                )
            }
        }
    }
}

@Composable
private fun Figure(label: String, value: String) {
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        AmountText(value, style = MaterialTheme.typography.titleMedium)
    }
}

internal fun Reminder.isMoneyItem() = amount != null || type in MoneyTypes

private val MoneyTypes = setOf(
    ReminderType.Bill, ReminderType.Emi, ReminderType.CreditCard, ReminderType.Subscription, ReminderType.Insurance,
    ReminderType.Investment,
)
