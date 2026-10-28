package com.pranavshukla.yaad.feature.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pranavshukla.yaad.core.model.OccurrenceStatus
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.scheduling.Agenda
import com.pranavshukla.yaad.core.scheduling.AgendaItem
import com.pranavshukla.yaad.core.scheduling.AlertAction
import com.pranavshukla.yaad.feature.reminders.resources.Res
import com.pranavshukla.yaad.feature.reminders.resources.*
import com.pranavshukla.yaad.ui.designsystem.icons.YaadIcons
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

/** One reminder: when it is next due, what to do about it, its checklist and its history. */
@Composable
fun ReminderDetail(
    reminder: Reminder,
    state: RemindersState,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAct: (AgendaItem, AlertAction) -> Unit,
    onToggleChecklist: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val today = state.now.toLocalDateTime(state.zone).date
    val history = state.historyOf(reminder.id)
    val next = Agenda.nextDue(reminder, state.history, state.now, state.zone)?.let { due ->
        val status = history.firstOrNull { it.due.toEpochMilliseconds() == due.toEpochMilliseconds() }?.status
        AgendaItem(reminder, due, status ?: OccurrenceStatus.Pending, due.toLocalDateTime(state.zone).date)
    }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack) { Icon(YaadIcons.Back, stringResource(Res.string.back)) }
            }
            Box(Modifier.weight(1f))
            IconButton(onClick = onEdit) { Icon(YaadIcons.Edit, stringResource(Res.string.edit)) }
            IconButton(onClick = { confirmDelete = true }) { Icon(YaadIcons.Delete, stringResource(Res.string.delete)) }
        }
        Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(reminder.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            Text(
                next?.let { stringResource(Res.string.next_due, dueText(it.due, reminder.allDay, state.now, state.zone)) }
                    ?: stringResource(Res.string.no_more),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(ruleText(reminder.schedule.rule, today), style = MaterialTheme.typography.bodyMedium)
            if (reminder.earlyAlerts.isNotEmpty()) {
                Text(reminder.earlyAlerts.map { beforeText(it) }.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
            }
            reminder.nagInterval?.let {
                Text(stringResource(Res.string.nag_every, it.inWholeMinutes.toInt()), style = MaterialTheme.typography.bodyMedium)
            }
            if (reminder.notes.isNotBlank()) {
                Text(reminder.notes, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
            }
            if (next != null && !next.isHandled) ActionRow(next, onAct)
        }

        if (reminder.checklist.isNotEmpty()) {
            DetailSection(stringResource(Res.string.checklist))
            reminder.checklist.forEachIndexed { index, item ->
                Row(
                    Modifier.fillMaxWidth()
                        .toggleable(item.done, role = Role.Checkbox, onValueChange = { onToggleChecklist(index) })
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = item.done, onCheckedChange = null)
                    Text(item.text, Modifier.padding(start = 8.dp))
                }
            }
        }

        DetailSection(stringResource(Res.string.history))
        if (history.isEmpty()) {
            Text(stringResource(Res.string.no_history), Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        history.forEach { occurrence ->
            val due = dueText(occurrence.due, reminder.allDay, state.now, state.zone)
            val text = when (occurrence.status) {
                is OccurrenceStatus.Done -> stringResource(Res.string.status_done_on, due)
                is OccurrenceStatus.Skipped -> stringResource(Res.string.status_skipped_on, due)
                is OccurrenceStatus.Snoozed -> stringResource(Res.string.status_snoozed_on, due)
                OccurrenceStatus.Pending -> due
            }
            Text(text, Modifier.padding(horizontal = 24.dp, vertical = 4.dp), style = MaterialTheme.typography.bodyMedium)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(Res.string.delete_title)) },
            text = { Text(stringResource(Res.string.delete_message)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) { Text(stringResource(Res.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(Res.string.cancel)) } },
        )
    }
}

@Composable
private fun ActionRow(item: AgendaItem, onAct: (AgendaItem, AlertAction) -> Unit) {
    var snoozeMenu by remember { mutableStateOf(false) }
    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { onAct(item, AlertAction.Done) }) { Text(stringResource(Res.string.action_done)) }
        Box {
            OutlinedButton(onClick = { snoozeMenu = true }) { Text(stringResource(Res.string.action_snooze)) }
            DropdownMenu(expanded = snoozeMenu, onDismissRequest = { snoozeMenu = false }) {
                listOf(
                    AlertAction.Snooze10Minutes to Res.string.snooze_10,
                    AlertAction.Snooze1Hour to Res.string.snooze_60,
                    AlertAction.SnoozeTomorrow to Res.string.snooze_tomorrow,
                ).forEach { (action, label) ->
                    DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = { snoozeMenu = false; onAct(item, action) })
                }
            }
        }
        TextButton(onClick = { onAct(item, AlertAction.Skip) }) { Text(stringResource(Res.string.action_skip)) }
    }
}

@Composable
private fun DetailSection(title: String) {
    HorizontalDivider(Modifier.padding(top = 16.dp))
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp).semantics { heading() },
    )
}
