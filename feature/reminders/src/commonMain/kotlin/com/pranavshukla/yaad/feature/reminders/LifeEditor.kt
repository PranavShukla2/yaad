package com.pranavshukla.yaad.feature.reminders

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pranavshukla.yaad.core.model.ReminderId
import com.pranavshukla.yaad.core.scheduling.TimeLabel
import com.pranavshukla.yaad.feature.reminders.resources.Res
import com.pranavshukla.yaad.feature.reminders.resources.*
import com.pranavshukla.yaad.ui.designsystem.components.dateText
import com.pranavshukla.yaad.ui.designsystem.LocalTwentyFourHour
import com.pranavshukla.yaad.ui.designsystem.icons.YaadIcons
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** The add button's choices on Today and Upcoming: a plain reminder, or a life template. */
@Composable
fun LifeChooser(onReminder: () -> Unit, onLife: (LifeKind) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.add_title)) },
        text = {
            Column {
                Choice(Res.string.life_reminder) { onReminder() }
                LifeKind.entries.forEach { kind -> Choice(lifeLabel(kind)) { onLife(kind) } }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}

@Composable
private fun Choice(label: StringResource, onClick: () -> Unit) {
    Text(stringResource(label), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp))
}

internal fun lifeLabel(kind: LifeKind): StringResource = when (kind) {
    LifeKind.Birthday -> Res.string.life_birthday
    LifeKind.Anniversary -> Res.string.life_anniversary
    LifeKind.Medicine -> Res.string.life_medicine
    LifeKind.Document -> Res.string.life_document
    LifeKind.Warranty -> Res.string.life_warranty
    LifeKind.Appointment -> Res.string.life_appointment
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LifeEditor(initial: LifeForm, today: LocalDate, onSave: (LifeForm) -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    var form by remember(initial) { mutableStateOf(initial) }
    var picking by remember { mutableStateOf<Picker?>(null) }
    val valid = form.toReminders(ReminderId("preview")) != null
    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancel) { Icon(YaadIcons.Close, stringResource(Res.string.cancel)) }
            Text(stringResource(lifeLabel(form.kind)), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).semantics { heading() })
            Button(onClick = { onSave(form) }, enabled = valid, modifier = Modifier.padding(end = 8.dp)) { Text(stringResource(Res.string.save)) }
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(form.name, { form = form.copy(name = it) }, label = { Text(stringResource(Res.string.field_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            val dateLabel = when (form.kind) {
                LifeKind.Birthday -> Res.string.field_born
                LifeKind.Anniversary -> Res.string.field_married
                LifeKind.Document -> Res.string.field_expires
                LifeKind.Warranty -> Res.string.field_bought
                LifeKind.Medicine -> Res.string.field_starting
                LifeKind.Appointment -> Res.string.field_on
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { picking = Picker.Date }) { Text(stringResource(dateLabel, dateText(form.date, today))) }
                if (form.kind == LifeKind.Appointment) {
                    OutlinedButton(onClick = { picking = Picker.Time }) { Text(TimeLabel.format(form.time, twentyFourHour = LocalTwentyFourHour.current)) }
                }
            }
            if (form.kind == LifeKind.Warranty) {
                NumberField(form.months, { form = form.copy(months = it) }, Res.string.field_warranty_months)
            }
            if (form.kind == LifeKind.Medicine) {
                Text(stringResource(Res.string.field_doses), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Dose.entries.forEach { dose ->
                        val on = dose in form.doses
                        FilterChip(
                            selected = on,
                            onClick = { form = form.copy(doses = if (on) form.doses - dose else form.doses + dose) },
                            label = { Text("${stringResource(doseLabel(dose))} · ${TimeLabel.format(dose.time, twentyFourHour = LocalTwentyFourHour.current)}") },
                        )
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Res.string.food_before, Res.string.food_after).forEach { label ->
                        val text = stringResource(label)
                        FilterChip(selected = form.note == text, onClick = { form = form.copy(note = if (form.note == text) "" else text) }, label = { Text(text) })
                    }
                }
                NumberField(form.courseDays, { form = form.copy(courseDays = it) }, Res.string.field_course)
                NumberField(form.pills, { form = form.copy(pills = it) }, Res.string.field_pills)
            }
        }
    }
    when (picking) {
        Picker.Date, Picker.EndDate -> DateDialog(form.date, { form = form.copy(date = it); picking = null }, { picking = null })
        Picker.Time -> TimeDialog(form.time, { form = form.copy(time = it); picking = null }, { picking = null })
        null -> Unit
    }
}

private fun doseLabel(dose: Dose): StringResource = when (dose) {
    Dose.Morning -> Res.string.dose_morning
    Dose.Afternoon -> Res.string.dose_afternoon
    Dose.Night -> Res.string.dose_night
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, label: StringResource) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> onChange(text.filter(Char::isDigit).take(4)) },
        label = { Text(stringResource(label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}
