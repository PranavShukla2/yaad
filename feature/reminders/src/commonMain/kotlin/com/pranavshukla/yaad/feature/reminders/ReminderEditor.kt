package com.pranavshukla.yaad.feature.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pranavshukla.yaad.core.model.BuiltInCategory
import com.pranavshukla.yaad.core.model.ChecklistItem
import com.pranavshukla.yaad.core.model.Priority
import com.pranavshukla.yaad.core.scheduling.TimeLabel
import com.pranavshukla.yaad.feature.reminders.resources.Res
import com.pranavshukla.yaad.feature.reminders.resources.*
import com.pranavshukla.yaad.ui.designsystem.icons.YaadIcons
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

const val EditorTitleTag = "editor-title"
const val EditorSaveTag = "editor-save"

/** Adds or edits a reminder. [initial] comes from `EditorState.new` or `EditorState.from`. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderEditor(
    initial: EditorState,
    today: LocalDate,
    onSave: (EditorState) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var state by remember(initial.id) { mutableStateOf(initial) }
    var picking by remember { mutableStateOf<Picker?>(null) }

    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancel) { Icon(YaadIcons.Close, stringResource(Res.string.cancel)) }
            Text(
                stringResource(if (initial.base == null) Res.string.new_reminder else Res.string.edit_reminder),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            Button(
                onClick = { onSave(state) },
                enabled = state.isValid,
                modifier = Modifier.padding(end = 8.dp).testTag(EditorSaveTag),
            ) { Text(stringResource(Res.string.save)) }
        }
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = { state = state.copy(title = it) },
                label = { Text(stringResource(Res.string.field_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag(EditorTitleTag),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { picking = Picker.Date }) { Text(dayText(state.date, today)) }
                if (!state.allDay) {
                    OutlinedButton(onClick = { picking = Picker.Time }) { Text(TimeLabel.format(state.time, twentyFourHour = false)) }
                }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.field_all_day), Modifier.padding(end = 8.dp))
                    Switch(checked = state.allDay, onCheckedChange = { state = state.copy(allDay = it) })
                }
            }

            Label(Res.string.field_repeat)
            Chips(RepeatKind.entries, state.repeat, { state = state.copy(repeat = it) }) { kindLabel(it) }
            if (state.repeat != RepeatKind.None) RepeatDetails(state, today, { state = it }, { picking = it })

            Label(Res.string.field_early)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EarlyOptions.forEach { (offset, label) ->
                    val on = offset in state.earlyAlerts
                    FilterChip(
                        selected = on,
                        onClick = { state = state.copy(earlyAlerts = if (on) state.earlyAlerts - offset else state.earlyAlerts + offset) },
                        label = { Text(stringResource(label)) },
                    )
                }
            }

            Label(Res.string.field_nag)
            Chips(listOf<Int?>(null, 5, 10, 15, 30, 60), state.nagMinutes, { state = state.copy(nagMinutes = it) }) {
                if (it == null) stringResource(Res.string.off) else stringResource(Res.string.minutes_short, it)
            }

            Label(Res.string.field_category)
            Chips(listOf<BuiltInCategory?>(null) + BuiltInCategory.entries, BuiltInCategory.entries.firstOrNull { it.id == state.categoryId }, {
                state = state.copy(categoryId = it?.id)
            }) { if (it == null) stringResource(Res.string.category_none) else categoryLabel(it) }

            Label(Res.string.field_priority)
            Chips(Priority.entries, state.priority, { state = state.copy(priority = it) }) {
                stringResource(
                    when (it) {
                        Priority.Low -> Res.string.priority_low
                        Priority.Normal -> Res.string.priority_normal
                        Priority.High -> Res.string.priority_high
                    },
                )
            }

            Label(Res.string.field_checklist)
            state.checklist.forEachIndexed { index, item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = item.text,
                        onValueChange = { text -> state = state.copy(checklist = state.checklist.mapIndexed { i, c -> if (i == index) c.copy(text = text) else c }) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { state = state.copy(checklist = state.checklist.filterIndexed { i, _ -> i != index }) }) {
                        Icon(YaadIcons.Delete, stringResource(Res.string.remove_item, item.text))
                    }
                }
            }
            TextButton(onClick = { state = state.copy(checklist = state.checklist + ChecklistItem("")) }) {
                Text(stringResource(Res.string.add_item))
            }

            OutlinedTextField(
                value = state.notes,
                onValueChange = { state = state.copy(notes = it) },
                label = { Text(stringResource(Res.string.field_notes)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    when (picking) {
        Picker.Date -> DateDialog(state.date, { state = state.withDate(it); picking = null }, { picking = null })
        Picker.EndDate -> DateDialog(state.endDate ?: state.date, { state = state.copy(endDate = it); picking = null }, { picking = null })
        Picker.Time -> TimeDialog(state.time, { state = state.copy(time = it); picking = null }, { picking = null })
        null -> Unit
    }
}

internal enum class Picker { Date, EndDate, Time }

private val EarlyOptions = listOf(
    15.minutes to Res.string.early_15m,
    1.hours to Res.string.early_1h,
    1.days to Res.string.early_1d,
    3.days to Res.string.early_3d,
    7.days to Res.string.early_1w,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RepeatDetails(state: EditorState, today: LocalDate, onChange: (EditorState) -> Unit, onPick: (Picker) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(Res.string.field_every), Modifier.padding(end = 8.dp))
        OutlinedTextField(
            value = if (state.interval > 0) state.interval.toString() else "",
            onValueChange = { onChange(state.copy(interval = it.filter(Char::isDigit).take(3).toIntOrNull() ?: 0)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(96.dp),
        )
    }
    if (state.repeat == RepeatKind.Weekly) {
        Label(Res.string.field_on_days)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DayOfWeek.entries.forEach { day ->
                val on = day in state.weekdays.ifEmpty { setOf(state.date.dayOfWeek) }
                FilterChip(
                    selected = on,
                    onClick = {
                        val current = state.weekdays.ifEmpty { setOf(state.date.dayOfWeek) }
                        val next = if (on) current - day else current + day
                        if (next.isNotEmpty()) onChange(state.copy(weekdays = next))
                    },
                    label = { Text(weekdayName(day, short = true)) },
                )
            }
        }
    }
    if (state.repeat == RepeatKind.Monthly) {
        val ordinal = if (state.nthOrdinal == -1) stringResource(Res.string.ordinal_last) else stringArrayOrdinal(state.nthOrdinal)
        Chips(MonthlyMode.entries, state.monthlyMode, { onChange(state.copy(monthlyMode = it)) }) {
            when (it) {
                MonthlyMode.Day -> stringResource(Res.string.monthly_day, state.monthDay)
                MonthlyMode.NthWeekday -> stringResource(Res.string.monthly_nth, ordinal, weekdayName(state.nthWeekday))
                MonthlyMode.LastDay -> stringResource(Res.string.monthly_last_day)
            }
        }
    }
    Label(Res.string.field_ends)
    Chips(EndKind.entries, state.end, { onChange(state.copy(end = it, endDate = state.endDate ?: state.date)) }) {
        stringResource(
            when (it) {
                EndKind.Never -> Res.string.end_never
                EndKind.OnDate -> Res.string.end_on_date
                EndKind.AfterCount -> Res.string.end_after
            },
        )
    }
    when (state.end) {
        EndKind.OnDate -> OutlinedButton(onClick = { onPick(Picker.EndDate) }) { Text(dateText(state.endDate ?: state.date, today)) }
        EndKind.AfterCount -> OutlinedTextField(
            value = if (state.endCount > 0) state.endCount.toString() else "",
            onValueChange = { onChange(state.copy(endCount = it.filter(Char::isDigit).take(4).toIntOrNull() ?: 0)) },
            label = { Text(stringResource(Res.string.field_times)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(160.dp),
        )
        EndKind.Never -> Unit
    }
}

@Composable
private fun stringArrayOrdinal(ordinal: Int): String = stringArrayResource(Res.array.ordinals)[ordinal - 1]

@Composable
private fun kindLabel(kind: RepeatKind): String = stringResource(
    when (kind) {
        RepeatKind.None -> Res.string.kind_none
        RepeatKind.Minutes -> Res.string.kind_minutes
        RepeatKind.Hours -> Res.string.kind_hours
        RepeatKind.Daily -> Res.string.kind_daily
        RepeatKind.Weekly -> Res.string.kind_weekly
        RepeatKind.Monthly -> Res.string.kind_monthly
        RepeatKind.Yearly -> Res.string.kind_yearly
    },
)

@Composable
internal fun categoryLabel(category: BuiltInCategory): String = stringResource(
    when (category) {
        BuiltInCategory.Money -> Res.string.category_money
        BuiltInCategory.Health -> Res.string.category_health
        BuiltInCategory.Personal -> Res.string.category_personal
        BuiltInCategory.Work -> Res.string.category_work
        BuiltInCategory.Home -> Res.string.category_home
        BuiltInCategory.Vehicle -> Res.string.category_vehicle
    },
)

@Composable
private fun Label(text: StringResource) {
    Text(stringResource(text), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

/** Single-choice chips. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Chips(options: List<T>, selected: T, onSelect: (T) -> Unit, label: @Composable (T) -> String) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(label(option)) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDialog(initial: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    // The Material date picker works in UTC milliseconds at midnight.
    val picker = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                picker.selectedDateMillis?.let { onPick(Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date) } ?: onDismiss()
            }) { Text(stringResource(Res.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    ) { DatePicker(picker) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(initial: LocalTime, onPick: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val picker = rememberTimePickerState(initial.hour, initial.minute, is24Hour = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onPick(LocalTime(picker.hour, picker.minute)) }) { Text(stringResource(Res.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
        text = { TimePicker(picker) },
    )
}
