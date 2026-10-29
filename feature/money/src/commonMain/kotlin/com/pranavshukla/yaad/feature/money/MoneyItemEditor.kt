package com.pranavshukla.yaad.feature.money

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pranavshukla.yaad.core.model.Currency
import com.pranavshukla.yaad.core.model.ReminderId
import com.pranavshukla.yaad.core.scheduling.BillCycle
import com.pranavshukla.yaad.feature.money.resources.Res
import com.pranavshukla.yaad.feature.money.resources.*
import com.pranavshukla.yaad.ui.designsystem.components.dateText
import com.pranavshukla.yaad.ui.designsystem.icons.YaadIcons
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

/** What the add button offers on the Money tab: a loan, or one of the money templates. */
sealed interface MoneyChoice {
    data object Loan : MoneyChoice
    data class Item(val kind: MoneyKind) : MoneyChoice
}

@Composable
fun MoneyChooser(onChoose: (MoneyChoice) -> Unit, onDismiss: () -> Unit) {
    val choices = listOf(MoneyChoice.Loan to Res.string.kind_loan) + MoneyKind.entries.map { MoneyChoice.Item(it) to kindLabel(it) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.add_title)) },
        text = {
            Column {
                choices.forEach { (choice, label) ->
                    Text(
                        stringResource(label),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().clickable { onChoose(choice) }.padding(vertical = 14.dp),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}

internal fun kindLabel(kind: MoneyKind): StringResource = when (kind) {
    MoneyKind.Bill -> Res.string.kind_bill
    MoneyKind.Subscription -> Res.string.kind_subscription
    MoneyKind.CreditCard -> Res.string.kind_card
    MoneyKind.Insurance -> Res.string.kind_insurance
    MoneyKind.Sip -> Res.string.kind_sip
    MoneyKind.Maturity -> Res.string.kind_maturity
    MoneyKind.Ppf -> Res.string.kind_ppf
}

private enum class DateField { Due, Trial }

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MoneyItemEditor(
    initial: MoneyItemForm,
    today: LocalDate,
    currency: Currency,
    onSave: (MoneyItemForm) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var form by remember(initial) { mutableStateOf(initial) }
    var picking by remember { mutableStateOf<DateField?>(null) }
    val valid = form.toReminders(ReminderId("preview"), currency) != null
    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancel) { Icon(YaadIcons.Close, stringResource(Res.string.cancel)) }
            Text(stringResource(kindLabel(form.kind)), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).semantics { heading() })
            Button(onClick = { onSave(form) }, enabled = valid, modifier = Modifier.padding(end = 8.dp)) { Text(stringResource(Res.string.save)) }
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(form.name, { form = form.copy(name = it) }, label = { Text(stringResource(Res.string.field_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            if (form.kind != MoneyKind.Maturity) {
                OutlinedTextField(
                    form.amount,
                    { text -> form = form.copy(amount = text.filter { it.isDigit() || it == '.' || it == ',' }) },
                    label = { Text(stringResource(if (form.kind.needsAmount) Res.string.field_amount else Res.string.field_amount_optional)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
            OutlinedButton(onClick = { picking = DateField.Due }) { Text(stringResource(Res.string.field_date, dateText(form.date, today))) }
            if (form.kind.hasCycle) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BillCycle.entries.forEach { cycle ->
                        FilterChip(selected = form.cycle == cycle, onClick = { form = form.copy(cycle = cycle) }, label = { Text(stringResource(cycleLabel(cycle))) })
                    }
                }
            }
            if (form.kind == MoneyKind.Subscription) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { picking = DateField.Trial }) {
                        Text(form.trialEnds?.let { stringResource(Res.string.field_trial, dateText(it, today)) } ?: stringResource(Res.string.no_trial))
                    }
                    if (form.trialEnds != null) TextButton(onClick = { form = form.copy(trialEnds = null) }) { Text(stringResource(Res.string.no_trial)) }
                }
            }
            if (form.kind != MoneyKind.Maturity && form.kind != MoneyKind.Ppf) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(Res.string.field_auto_pay))
                        Text(stringResource(Res.string.auto_pay_note), style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = form.autoPay, onCheckedChange = { form = form.copy(autoPay = it) })
                }
            }
        }
    }
    picking?.let { field ->
        val initialDate = if (field == DateField.Trial) form.trialEnds ?: form.date else form.date
        val picker = rememberDatePickerState(initialSelectedDateMillis = initialDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds())
        DatePickerDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let {
                        val date = Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date
                        form = if (field == DateField.Trial) form.copy(trialEnds = date) else form.copy(date = date)
                    }
                    picking = null
                }) { Text(stringResource(Res.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { picking = null }) { Text(stringResource(Res.string.cancel)) } },
        ) { DatePicker(picker) }
    }
}

private fun cycleLabel(cycle: BillCycle): StringResource = when (cycle) {
    BillCycle.Monthly -> Res.string.cycle_monthly
    BillCycle.Bimonthly -> Res.string.cycle_bimonthly
    BillCycle.Quarterly -> Res.string.cycle_quarterly
    BillCycle.HalfYearly -> Res.string.cycle_half_yearly
    BillCycle.Yearly -> Res.string.cycle_yearly
}
