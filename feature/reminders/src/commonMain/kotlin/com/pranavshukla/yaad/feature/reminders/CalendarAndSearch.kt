package com.pranavshukla.yaad.feature.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pranavshukla.yaad.core.model.BuiltInCategory
import com.pranavshukla.yaad.core.scheduling.Agenda
import com.pranavshukla.yaad.core.scheduling.AgendaItem
import com.pranavshukla.yaad.core.scheduling.ReminderSearch
import com.pranavshukla.yaad.core.scheduling.SearchFilter
import com.pranavshukla.yaad.core.scheduling.SearchStatus
import com.pranavshukla.yaad.feature.reminders.resources.Res
import com.pranavshukla.yaad.feature.reminders.resources.*
import com.pranavshukla.yaad.ui.designsystem.components.DueStatus
import com.pranavshukla.yaad.ui.designsystem.components.ItemCard
import com.pranavshukla.yaad.ui.designsystem.components.dateText
import com.pranavshukla.yaad.ui.designsystem.icons.YaadIcons
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

enum class UpcomingView { List, Calendar, Search }

/** Upcoming: the next 30 days as a list, a month calendar, or a search over every reminder. */
@Composable
fun UpcomingScreen(
    state: RemindersState,
    onOpen: (AgendaItem) -> Unit,
    onOpenReminder: (com.pranavshukla.yaad.core.model.Reminder) -> Unit,
    onToggleDone: (AgendaItem) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
) {
    var view by rememberSaveable { mutableStateOf(UpcomingView.List) }
    Column(modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
            UpcomingView.entries.forEachIndexed { i, option ->
                SegmentedButton(
                    selected = view == option,
                    onClick = { view = option },
                    shape = SegmentedButtonDefaults.itemShape(i, UpcomingView.entries.size),
                ) {
                    Text(stringResource(when (option) {
                        UpcomingView.List -> Res.string.view_list
                        UpcomingView.Calendar -> Res.string.view_calendar
                        UpcomingView.Search -> Res.string.view_search
                    }))
                }
            }
        }
        when (view) {
            UpcomingView.List -> UpcomingList(state, onOpen, onToggleDone, contentPadding = contentPadding)
            UpcomingView.Calendar -> CalendarView(state, onOpen, onToggleDone, contentPadding)
            UpcomingView.Search -> SearchView(state, onOpenReminder, contentPadding)
        }
    }
}

@Composable
private fun CalendarView(state: RemindersState, onOpen: (AgendaItem) -> Unit, onToggleDone: (AgendaItem) -> Unit, contentPadding: PaddingValues) {
    val today = state.now.toLocalDateTime(state.zone).date
    var month by remember { mutableStateOf(LocalDate(today.year, today.month.number, 1)) }
    var selected by remember { mutableStateOf(today) }
    val days = remember(state, month) { Agenda.month(state.reminders, state.history, month.year, month.month.number, state.now, state.zone) }
    val monthName = stringArrayResource(Res.array.months_long)[month.month.number - 1]
    LazyColumn(contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { month = month.plus(-1, DateTimeUnit.MONTH) }) { Icon(YaadIcons.Back, stringResource(Res.string.previous_month)) }
                Text("$monthName ${month.year}", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f).semantics { heading() })
                IconButton(onClick = { month = month.plus(1, DateTimeUnit.MONTH) }) {
                    Icon(YaadIcons.Back, stringResource(Res.string.next_month), Modifier.rotate(180f))
                }
            }
        }
        item {
            Row {
                DayOfWeek.entries.forEach {
                    Text(weekdayName(it, short = true), Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        item {
            val lead = month.dayOfWeek.isoDayNumber - 1
            val length = month.plus(1, DateTimeUnit.MONTH).plus(-1, DateTimeUnit.DAY).day
            Column {
                for (week in 0 until (lead + length + 6) / 7) {
                    Row {
                        for (column in 0..6) {
                            val day = week * 7 + column - lead + 1
                            Box(Modifier.weight(1f).aspectRatio(1f)) {
                                if (day in 1..length) {
                                    val date = LocalDate(month.year, month.month.number, day)
                                    DayCell(date, days[date].orEmpty().size, date == selected, date == today, monthName) { selected = date }
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Text(dayText(selected, today), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
        val items = days[selected].orEmpty()
        if (items.isEmpty()) item { Text(stringResource(Res.string.nothing_this_day), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(items, key = { "${it.reminder.id.value}@${it.due}" }) { item -> AgendaRow(item, state, onOpen = { onOpen(item) }, onToggleDone = { onToggleDone(item) }) }
    }
}

@Composable
private fun DayCell(date: LocalDate, count: Int, selected: Boolean, today: Boolean, monthName: String, onClick: () -> Unit) {
    val description = stringResource(Res.string.items_on, "${date.day} $monthName", count)
    Column(
        Modifier.fillMaxSize().padding(2.dp).clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description; this.selected = selected },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "${date.day}",
            style = MaterialTheme.typography.bodyMedium,
            color = if (today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        Box(Modifier.size(6.dp).clip(CircleShape).background(if (count > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surface.copy(alpha = 0f)))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchView(state: RemindersState, onOpen: (com.pranavshukla.yaad.core.model.Reminder) -> Unit, contentPadding: PaddingValues) {
    var filter by remember { mutableStateOf(SearchFilter()) }
    val today = state.now.toLocalDateTime(state.zone).date
    val results = remember(state.reminders, filter) { ReminderSearch.filter(state.reminders, filter) }
    LazyColumn(contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            OutlinedTextField(
                value = filter.text, onValueChange = { filter = filter.copy(text = it) },
                placeholder = { Text(stringResource(Res.string.search_hint)) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BuiltInCategory.entries.forEach { category ->
                    val on = filter.categoryId == category.id
                    FilterChip(selected = on, onClick = { filter = filter.copy(categoryId = if (on) null else category.id) }, label = { Text(categoryLabel(category)) })
                }
                val archived = filter.status == SearchStatus.Archived
                FilterChip(
                    selected = archived,
                    onClick = { filter = filter.copy(status = if (archived) SearchStatus.Active else SearchStatus.Archived) },
                    label = { Text(stringResource(Res.string.show_archived)) },
                )
            }
        }
        if (results.isEmpty()) item { Text(stringResource(Res.string.no_results), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(results, key = { it.id.value }) { reminder ->
            val next = Agenda.nextDue(reminder, state.history, state.now, state.zone)
            ItemCard(
                title = reminder.title,
                dueText = next?.let { dueText(it, reminder.allDay, state.now, state.zone) } ?: "",
                status = when {
                    next == null -> DueStatus.Done
                    Agenda.isOverdue(reminder, next, state.now, state.zone) -> DueStatus.Overdue
                    next.toLocalDateTime(state.zone).date == today -> DueStatus.DueToday
                    else -> DueStatus.Upcoming
                },
                onClick = { onOpen(reminder) },
                supportingText = reminder.schedule.rule?.let { ruleText(it, today) } ?: reminder.notes.takeIf { it.isNotBlank() },
                modifier = Modifier.heightIn(min = 72.dp),
            )
        }
    }
}
