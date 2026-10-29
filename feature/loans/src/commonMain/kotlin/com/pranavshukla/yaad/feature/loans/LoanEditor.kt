package com.pranavshukla.yaad.feature.loans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pranavshukla.yaad.core.model.Currency
import com.pranavshukla.yaad.core.model.Loan
import com.pranavshukla.yaad.core.model.LoanKind
import com.pranavshukla.yaad.feature.loans.resources.Res
import com.pranavshukla.yaad.feature.loans.resources.*
import com.pranavshukla.yaad.ui.designsystem.components.AmountText
import com.pranavshukla.yaad.ui.designsystem.icons.YaadIcons
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

const val LoanLenderTag = "loan-lender"
const val LoanPrincipalTag = "loan-principal"
const val LoanRateTag = "loan-rate"
const val LoanTenureTag = "loan-tenure"
const val LoanSaveTag = "loan-save"

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LoanEditor(
    initial: LoanForm,
    currency: Currency,
    onSave: (Loan) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var form by remember(initial.id) { mutableStateOf(initial) }
    var pickDate by remember { mutableStateOf(false) }
    val loan = form.toLoan(currency)
    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancel) { Icon(YaadIcons.Close, stringResource(Res.string.cancel)) }
            Text(
                stringResource(if (initial.base == null) Res.string.new_loan else Res.string.edit_loan),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            Button(onClick = { loan?.let(onSave) }, enabled = loan != null, modifier = Modifier.padding(end = 8.dp).testTag(LoanSaveTag)) {
                Text(stringResource(Res.string.save))
            }
        }
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                form.lender, { form = form.copy(lender = it) }, label = { Text(stringResource(Res.string.field_lender)) },
                singleLine = true, modifier = Modifier.fillMaxWidth().testTag(LoanLenderTag),
            )
            Text(stringResource(Res.string.field_kind), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LoanKind.entries.forEach { kind ->
                    FilterChip(selected = form.kind == kind, onClick = { form = form.copy(kind = kind) }, label = { Text(kindLabel(kind)) })
                }
            }
            Row(Modifier.testTag(LoanPrincipalTag)) { NumberField(form.principal, { form = form.copy(principal = it) }, Res.string.field_principal, decimal = true) }
            Row(Modifier.testTag(LoanRateTag)) { NumberField(form.rate, { form = form.copy(rate = it) }, Res.string.field_rate, decimal = true) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.testTag(LoanTenureTag)) { NumberField(form.tenure, { form = form.copy(tenure = it) }, Res.string.field_tenure) }
            }
            Choice(listOf(true to Res.string.years, false to Res.string.months), form.tenureInYears) { form = form.copy(tenureInYears = it) }
            OutlinedButton(onClick = { pickDate = true }) {
                Text(stringResource(Res.string.field_first_emi, form.firstEmiDate.toString()))
            }
            NumberField(form.bankEmi, { form = form.copy(bankEmi = it) }, Res.string.field_bank_emi, decimal = true)
            form.previewEmi(currency)?.let {
                AmountText(stringResource(Res.string.preview_emi, it.text()), style = MaterialTheme.typography.titleLarge)
                if (form.bankEmi.isBlank()) Text(stringResource(Res.string.preview_estimated), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    if (pickDate) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = form.firstEmiDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds())
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let {
                        form = form.copy(firstEmiDate = Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date)
                    }
                    pickDate = false
                }) { Text(stringResource(Res.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(stringResource(Res.string.cancel)) } },
        ) { DatePicker(picker) }
    }
}
