package com.pranavshukla.yaad.feature.loans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pranavshukla.yaad.core.finance.Amortisation
import com.pranavshukla.yaad.core.model.AnnualRate
import com.pranavshukla.yaad.core.model.Loan
import com.pranavshukla.yaad.core.model.Money
import com.pranavshukla.yaad.core.model.Prepayment
import com.pranavshukla.yaad.core.model.PrepaymentEffect
import com.pranavshukla.yaad.core.model.RateChange
import com.pranavshukla.yaad.core.model.RateChangeKeeps
import com.pranavshukla.yaad.feature.loans.resources.Res
import com.pranavshukla.yaad.feature.loans.resources.*
import com.pranavshukla.yaad.ui.designsystem.components.AmountText
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PrepaymentDialog(view: LoanView, onSave: (Loan) -> Unit, onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf("") }
    var after by remember { mutableStateOf(view.summary.paid.coerceAtLeast(1).toString()) }
    var effect by remember { mutableStateOf(PrepaymentEffect.Tenure) }
    val loan = runCatching {
        val money = Money.parse(amount, view.loan.principal.currency).takeIf { !it.isZero && !it.isNegative }!!
        val number = after.toInt().takeIf { it in 1 until view.schedule.instalments.size }!!
        view.loan.copy(prepayments = view.loan.prepayments + Prepayment(number, money, effect))
    }.getOrNull()
    LoanDialogFrame(Res.string.prepay_title, enabled = loan != null, onConfirm = { loan?.let(onSave) }, onDismiss = onDismiss) {
        NumberField(amount, { amount = it }, Res.string.field_amount, decimal = true)
        NumberField(after, { after = it }, Res.string.field_after_emi)
        Choice(listOf(PrepaymentEffect.Tenure to Res.string.reduce_tenure, PrepaymentEffect.Emi to Res.string.reduce_emi), effect) { effect = it }
        loan?.let { AmountText(stringResource(Res.string.saves, Amortisation.interestSaved(it).text()), style = MaterialTheme.typography.titleSmall) }
    }
}

@Composable
internal fun RateChangeDialog(view: LoanView, onSave: (Loan) -> Unit, onDismiss: () -> Unit) {
    var rate by remember { mutableStateOf(LoanForm.rateText(view.loan.rate)) }
    var after by remember { mutableStateOf(view.summary.paid.coerceAtLeast(1).toString()) }
    var keep by remember { mutableStateOf(RateChangeKeeps.Emi) }
    val loan = runCatching {
        val number = after.toInt().takeIf { it in 1 until view.schedule.instalments.size }!!
        view.loan.copy(rateChanges = view.loan.rateChanges + RateChange(number, AnnualRate.percent(rate), keep))
            .also { Amortisation.schedule(it) } // rejects an EMI that would never repay the loan
    }.getOrNull()
    LoanDialogFrame(Res.string.rate_title, enabled = loan != null, onConfirm = { loan?.let(onSave) }, onDismiss = onDismiss) {
        NumberField(rate, { rate = it }, Res.string.field_new_rate, decimal = true)
        NumberField(after, { after = it }, Res.string.field_after_emi)
        Choice(listOf(RateChangeKeeps.Emi to Res.string.keep_emi, RateChangeKeeps.Tenure to Res.string.keep_tenure), keep) { keep = it }
        loan?.let {
            val schedule = Amortisation.schedule(it)
            val next = schedule.instalments.getOrNull(after.toInt())?.payment
            AmountText(
                listOfNotNull(next?.let { emi -> stringResource(Res.string.emi_per_month, emi.text()) }, stringResource(Res.string.emis_total, schedule.instalments.size)).joinToString(" · "),
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

@Composable
internal fun ForeclosureDialog(view: LoanView, onDismiss: () -> Unit) {
    var charge by remember { mutableStateOf("2") }
    val estimate = runCatching {
        Amortisation.foreclosure(view.schedule, view.summary.paid, AnnualRate.percent(charge).thousandthsOfPercent)
    }.getOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.foreclose_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField(charge, { charge = it }, Res.string.field_charge, decimal = true)
                estimate?.let {
                    AmountText(stringResource(Res.string.foreclose_result, it.outstanding.text(), it.charge.text(), it.total.text()), style = MaterialTheme.typography.titleSmall)
                }
                Text(stringResource(Res.string.estimate_note), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.close)) } },
    )
}

@Composable
private fun LoanDialogFrame(
    title: StringResource,
    enabled: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { content() } },
        confirmButton = { TextButton(onClick = onConfirm, enabled = enabled) { Text(stringResource(Res.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}

@Composable
internal fun NumberField(value: String, onChange: (String) -> Unit, label: StringResource, decimal: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> onChange(text.filter { it.isDigit() || (decimal && (it == '.' || it == ',')) }) },
        label = { Text(stringResource(label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
    )
}

@Composable
internal fun <T> Choice(options: List<Pair<T, StringResource>>, selected: T, onSelect: (T) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(stringResource(label)) })
        }
    }
}
