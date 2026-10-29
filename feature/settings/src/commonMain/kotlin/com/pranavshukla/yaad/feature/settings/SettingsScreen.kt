package com.pranavshukla.yaad.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pranavshukla.yaad.core.model.AppSettings
import com.pranavshukla.yaad.core.model.Currency
import com.pranavshukla.yaad.core.model.ThemeMode
import com.pranavshukla.yaad.feature.settings.resources.Res
import com.pranavshukla.yaad.feature.settings.resources.*
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** What the platform can do here; the screen hides or explains anything it cannot. */
data class SettingsCapabilities(val canLock: Boolean = false, val canBackUp: Boolean = false)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onChange: (AppSettings) -> Unit,
    capabilities: SettingsCapabilities,
    onExport: () -> Unit,
    onRestore: () -> Unit,
    formatTime: (LocalTime) -> String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    var picking by remember { mutableStateOf<TimeField?>(null) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(contentPadding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(Res.string.settings), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })

        Section(Res.string.section_reminders)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.all_day_time), Modifier.weight(1f))
            OutlinedButton(onClick = { picking = TimeField.AllDay }) { Text(formatTime(settings.allDayTime)) }
        }
        val quiet = settings.quietStart != null && settings.quietEnd != null
        SwitchRow(Res.string.quiet_hours, Res.string.quiet_hours_note, quiet) { on ->
            onChange(if (on) settings.copy(quietStart = LocalTime(22, 0), quietEnd = LocalTime(7, 0)) else settings.copy(quietStart = null, quietEnd = null))
        }
        if (quiet) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { picking = TimeField.QuietStart }) { Text(stringResource(Res.string.quiet_from, formatTime(settings.quietStart!!))) }
                OutlinedButton(onClick = { picking = TimeField.QuietEnd }) { Text(stringResource(Res.string.quiet_until, formatTime(settings.quietEnd!!))) }
            }
        }
        SwitchRow(Res.string.twenty_four_hour, null, settings.twentyFourHour) { onChange(settings.copy(twentyFourHour = it)) }
        Text(stringResource(Res.string.week_starts))
        Chips(listOf(DayOfWeek.MONDAY to Res.string.monday, DayOfWeek.SUNDAY to Res.string.sunday), settings.firstDayOfWeek) { onChange(settings.copy(firstDayOfWeek = it)) }

        Section(Res.string.section_money)
        Text(stringResource(Res.string.currency))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Currency.INR, Currency.USD, Currency.EUR, Currency.GBP, Currency.AED, Currency.JPY).forEach { c ->
                FilterChip(selected = settings.currency == c, onClick = { onChange(settings.copy(currency = c)) }, label = { Text(c.code) })
            }
        }

        Section(Res.string.section_appearance)
        Chips(
            listOf(ThemeMode.System to Res.string.theme_system, ThemeMode.Light to Res.string.theme_light, ThemeMode.Dark to Res.string.theme_dark),
            settings.theme,
        ) { onChange(settings.copy(theme = it)) }

        Section(Res.string.section_privacy)
        SwitchRow(Res.string.hide_amounts, Res.string.hide_amounts_note, settings.hideAmounts) { onChange(settings.copy(hideAmounts = it)) }
        SwitchRow(
            Res.string.app_lock,
            if (capabilities.canLock) Res.string.app_lock_note else Res.string.app_lock_unavailable,
            settings.appLock,
            enabled = capabilities.canLock || settings.appLock,
        ) { onChange(settings.copy(appLock = it)) }

        if (capabilities.canBackUp) {
            Section(Res.string.section_backup)
            Text(stringResource(Res.string.backup_note), style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onExport) { Text(stringResource(Res.string.export_backup)) }
                OutlinedButton(onClick = onRestore) { Text(stringResource(Res.string.restore_backup)) }
            }
        }

        Section(Res.string.section_about)
        Text(stringResource(Res.string.about_text), style = MaterialTheme.typography.bodyMedium)
    }
    picking?.let { field ->
        val initial = when (field) {
            TimeField.AllDay -> settings.allDayTime
            TimeField.QuietStart -> settings.quietStart ?: LocalTime(22, 0)
            TimeField.QuietEnd -> settings.quietEnd ?: LocalTime(7, 0)
        }
        TimeDialog(initial, settings.twentyFourHour, onPick = { time ->
            onChange(
                when (field) {
                    TimeField.AllDay -> settings.copy(allDayTime = time)
                    TimeField.QuietStart -> settings.copy(quietStart = time)
                    TimeField.QuietEnd -> settings.copy(quietEnd = time)
                },
            )
            picking = null
        }, onDismiss = { picking = null })
    }
}

private enum class TimeField { AllDay, QuietStart, QuietEnd }

@Composable
private fun Section(title: StringResource) {
    HorizontalDivider(Modifier.padding(top = 8.dp))
    Text(stringResource(title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { heading() })
}

@Composable
private fun SwitchRow(title: StringResource, note: StringResource?, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(title))
            note?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Chips(options: List<Pair<T, StringResource>>, selected: T, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) -> FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(stringResource(label)) }) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(initial: LocalTime, twentyFourHour: Boolean, onPick: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val picker = rememberTimePickerState(initial.hour, initial.minute, is24Hour = twentyFourHour)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onPick(LocalTime(picker.hour, picker.minute)) }) { Text(stringResource(Res.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
        text = { TimePicker(picker) },
    )
}
