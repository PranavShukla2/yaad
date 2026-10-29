package com.pranavshukla.yaad.feature.loans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pranavshukla.yaad.core.finance.Amortisation
import com.pranavshukla.yaad.core.finance.InstalmentStatus
import com.pranavshukla.yaad.core.finance.MoneyFormat
import com.pranavshukla.yaad.core.finance.TrackedInstalment
import com.pranavshukla.yaad.core.model.LoanKind
import com.pranavshukla.yaad.core.model.Money
import com.pranavshukla.yaad.feature.loans.resources.Res
import com.pranavshukla.yaad.feature.loans.resources.*
import com.pranavshukla.yaad.ui.designsystem.components.AmountText
import com.pranavshukla.yaad.ui.designsystem.components.DueStatus
import com.pranavshukla.yaad.ui.designsystem.components.EmptyState
import com.pranavshukla.yaad.ui.designsystem.components.SectionHeader
import com.pranavshukla.yaad.ui.designsystem.components.StatusBadge
import com.pranavshukla.yaad.ui.designsystem.icons.YaadIcons
import org.jetbrains.compose.resources.stringResource

internal fun Money.text(showMinor: Boolean = true) = MoneyFormat.format(this, showMinor = showMinor)

/** "6.514%" from thousandths of a percent, as a string of digits only. */
internal fun percentText(thousandths: Int): String {
    val whole = thousandths / 1000
    val fraction = ((thousandths % 1000) / 10).toString().padStart(2, '0')
    return "$whole.$fraction%"
}

@Composable
internal fun kindLabel(kind: LoanKind): String = stringResource(
    when (kind) {
        LoanKind.Home -> Res.string.kind_home
        LoanKind.Car -> Res.string.kind_car
        LoanKind.Personal -> Res.string.kind_personal
        LoanKind.Education -> Res.string.kind_education
        LoanKind.Gold -> Res.string.kind_gold
        LoanKind.ConsumerDurable -> Res.string.kind_consumer
        LoanKind.Other -> Res.string.kind_other
    },
)

@Composable
fun LoansList(
    state: LoansState,
    onOpen: (LoanView) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
) {
    if (state.loans.isEmpty()) {
        EmptyState(stringResource(Res.string.loans_empty_title), stringResource(Res.string.loans_empty_message), modifier)
        return
    }
    LazyColumn(modifier, contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.loans, key = { it.loan.id }) { view ->
            Card(
                onClick = { onOpen(view) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(view.loan.lender, style = MaterialTheme.typography.titleMedium)
                            Text(kindLabel(view.loan.kind), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        view.summary.nextPayment?.let {
                            AmountText(stringResource(Res.string.emi_per_month, it.text()), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    LinearProgressIndicator(
                        progress = { view.summary.repaidThousandthsOfPercent / 100_000f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row {
                        Text(
                            stringResource(Res.string.paid_of, view.summary.paid, view.schedule.instalments.size),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        AmountText(view.summary.outstanding.text(showMinor = false), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun LoanDetail(
    view: LoanView,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSave: (com.pranavshukla.yaad.core.model.Loan) -> Unit,
    onTogglePaid: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    var dialog by remember { mutableStateOf<LoanDialog?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val summary = view.summary
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) IconButton(onClick = onBack) { Icon(YaadIcons.Back, stringResource(Res.string.back)) }
                Box(Modifier.weight(1f))
                IconButton(onClick = onEdit) { Icon(YaadIcons.Edit, stringResource(Res.string.edit)) }
                IconButton(onClick = { confirmDelete = true }) { Icon(YaadIcons.Delete, stringResource(Res.string.delete)) }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(view.loan.lender, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                Text(kindLabel(view.loan.kind), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Figure(stringResource(if (view.loan.emi == null) Res.string.emi_estimated else Res.string.emi), view.schedule.instalments.first().payment.text())
                Figure(stringResource(Res.string.outstanding), summary.outstanding.text())
                Figure(stringResource(Res.string.emis_left), "${summary.left}")
                Figure(stringResource(Res.string.repaid), percentText(summary.repaidThousandthsOfPercent))
                Figure(stringResource(Res.string.total_interest), view.schedule.totalInterest.text())
                if (view.loan.prepayments.isNotEmpty()) {
                    Figure(stringResource(Res.string.interest_saved), Amortisation.interestSaved(view.loan).text())
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedButton(onClick = { dialog = LoanDialog.Prepay }) { Text(stringResource(Res.string.action_prepay)) }
                    OutlinedButton(onClick = { dialog = LoanDialog.Rate }) { Text(stringResource(Res.string.action_rate)) }
                }
                OutlinedButton(onClick = { dialog = LoanDialog.Foreclose }) { Text(stringResource(Res.string.action_foreclose)) }
            }
        }
        item {
            HorizontalDivider(Modifier.padding(top = 16.dp))
            SectionHeader(stringResource(Res.string.schedule), Modifier.padding(horizontal = 20.dp))
        }
        items(view.tracked, key = { it.instalment.number }) { row -> ScheduleRow(row, onToggle = { onTogglePaid(row.instalment.number) }) }
    }

    when (dialog) {
        LoanDialog.Prepay -> PrepaymentDialog(view, onSave = { onSave(it); dialog = null }, onDismiss = { dialog = null })
        LoanDialog.Rate -> RateChangeDialog(view, onSave = { onSave(it); dialog = null }, onDismiss = { dialog = null })
        LoanDialog.Foreclose -> ForeclosureDialog(view, onDismiss = { dialog = null })
        null -> Unit
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(Res.string.delete_loan_title)) },
            text = { Text(stringResource(Res.string.delete_loan_message)) },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text(stringResource(Res.string.delete)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(Res.string.cancel)) } },
        )
    }
}

internal enum class LoanDialog { Prepay, Rate, Foreclose }

@Composable
private fun Figure(label: String, value: String) {
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        AmountText(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ScheduleRow(row: TrackedInstalment, onToggle: () -> Unit) {
    val i = row.instalment
    val paid = row.status == InstalmentStatus.Paid
    Row(
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AmountText(stringResource(Res.string.instalment_row, i.number, i.payment.text()), style = MaterialTheme.typography.titleSmall)
                StatusBadge(
                    when (row.status) {
                        InstalmentStatus.Paid -> DueStatus.Done
                        InstalmentStatus.Late -> DueStatus.Overdue
                        InstalmentStatus.Due -> DueStatus.Upcoming
                    },
                )
            }
            Text("${i.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AmountText(
                stringResource(Res.string.interest_principal, i.interest.text(), i.principal.text(), i.balance.text()),
                style = MaterialTheme.typography.bodySmall,
            )
            if (!i.prepaid.isZero) AmountText(stringResource(Res.string.prepaid_row, i.prepaid.text()), style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onToggle) {
            Icon(
                YaadIcons.Check,
                contentDescription = stringResource(if (paid) Res.string.mark_unpaid else Res.string.mark_paid, i.number),
                tint = if (paid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            )
        }
    }
}
