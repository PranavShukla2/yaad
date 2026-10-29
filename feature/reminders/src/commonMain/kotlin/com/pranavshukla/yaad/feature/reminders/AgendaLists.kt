package com.pranavshukla.yaad.feature.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pranavshukla.yaad.core.scheduling.Agenda
import com.pranavshukla.yaad.core.scheduling.AgendaItem
import com.pranavshukla.yaad.feature.reminders.resources.Res
import com.pranavshukla.yaad.feature.reminders.resources.action_undo
import com.pranavshukla.yaad.feature.reminders.resources.empty_today_message
import com.pranavshukla.yaad.feature.reminders.resources.empty_today_title
import com.pranavshukla.yaad.feature.reminders.resources.empty_upcoming_message
import com.pranavshukla.yaad.feature.reminders.resources.empty_upcoming_title
import com.pranavshukla.yaad.feature.reminders.resources.mark_done
import com.pranavshukla.yaad.feature.reminders.resources.section_overdue
import com.pranavshukla.yaad.feature.reminders.resources.section_today
import com.pranavshukla.yaad.feature.reminders.resources.turns
import com.pranavshukla.yaad.ui.designsystem.components.DueStatus
import com.pranavshukla.yaad.ui.designsystem.components.EmptyState
import com.pranavshukla.yaad.ui.designsystem.components.ItemCard
import com.pranavshukla.yaad.ui.designsystem.components.SectionHeader
import com.pranavshukla.yaad.ui.designsystem.icons.YaadIcons
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

/** Overdue first, then the rest of today. */
@Composable
fun TodayList(
    state: RemindersState,
    onOpen: (AgendaItem) -> Unit,
    onToggleDone: (AgendaItem) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
) {
    val agenda = state.agenda
    if (agenda.overdue.isEmpty() && agenda.today.isEmpty()) {
        EmptyState(stringResource(Res.string.empty_today_title), stringResource(Res.string.empty_today_message), modifier)
        return
    }
    val overdueTitle = stringResource(Res.string.section_overdue)
    val todayTitle = stringResource(Res.string.section_today)
    LazyColumn(modifier, contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        section(overdueTitle, agenda.overdue, state, onOpen, onToggleDone)
        section(todayTitle, agenda.today, state, onOpen, onToggleDone)
    }
}

/** The next 30 days, one section per day. */
@Composable
fun UpcomingList(
    state: RemindersState,
    onOpen: (AgendaItem) -> Unit,
    onToggleDone: (AgendaItem) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
) {
    if (state.agenda.upcoming.isEmpty()) {
        EmptyState(stringResource(Res.string.empty_upcoming_title), stringResource(Res.string.empty_upcoming_message), modifier)
        return
    }
    val today = state.now.toLocalDateTime(state.zone).date
    val titles = state.agenda.upcoming.keys.associateWith { dayText(it, today) }
    LazyColumn(modifier, contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.agenda.upcoming.forEach { (date, items) -> section(titles.getValue(date), items, state, onOpen, onToggleDone) }
    }
}

private fun LazyListScope.section(
    title: String,
    items: List<AgendaItem>,
    state: RemindersState,
    onOpen: (AgendaItem) -> Unit,
    onToggleDone: (AgendaItem) -> Unit,
) {
    if (items.isEmpty()) return
    item(key = "header-$title") { SectionHeader(title, trailing = items.size.toString()) }
    items(items, key = { "${it.reminder.id.value}@${it.due}" }) { item ->
        AgendaRow(item, state, onOpen = { onOpen(item) }, onToggleDone = { onToggleDone(item) })
    }
}

@Composable
internal fun AgendaRow(item: AgendaItem, state: RemindersState, onOpen: () -> Unit, onToggleDone: () -> Unit) {
    val today = state.now.toLocalDateTime(state.zone).date
    val status = when {
        item.isHandled -> DueStatus.Done
        Agenda.isOverdue(item.reminder, item.due, state.now, state.zone) -> DueStatus.Overdue
        item.date == today -> DueStatus.DueToday
        else -> DueStatus.Upcoming
    }
    ItemCard(
        title = item.reminder.title,
        dueText = dueText(item.due, item.reminder.allDay, state.now, state.zone),
        status = status,
        onClick = onOpen,
        supportingText = yearsOn(item.reminder, item.date)?.let { stringResource(Res.string.turns, it) }
            ?: item.reminder.schedule.rule?.let { ruleText(it, today) },
        trailing = {
            IconButton(onClick = onToggleDone) {
                Icon(
                    YaadIcons.Check,
                    contentDescription = if (item.isHandled) {
                        stringResource(Res.string.action_undo)
                    } else {
                        stringResource(Res.string.mark_done, item.reminder.title)
                    },
                    tint = if (item.isHandled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                )
            }
        },
    )
}
