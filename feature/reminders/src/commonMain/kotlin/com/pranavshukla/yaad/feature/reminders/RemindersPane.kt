package com.pranavshukla.yaad.feature.reminders

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.pranavshukla.yaad.core.model.ReminderId
import com.pranavshukla.yaad.core.scheduling.AgendaItem
import com.pranavshukla.yaad.core.scheduling.AlertAction
import com.pranavshukla.yaad.feature.reminders.resources.Res
import com.pranavshukla.yaad.feature.reminders.resources.select_reminder
import com.pranavshukla.yaad.ui.designsystem.adaptive.ListDetailLayout
import com.pranavshukla.yaad.ui.designsystem.adaptive.WidthClass
import com.pranavshukla.yaad.ui.designsystem.components.EmptyState
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

enum class AgendaTab { Today, Upcoming }

/** What shows beside the list (iPad) or instead of it (phone). */
sealed interface Pane {
    data object None : Pane
    data class Detail(val id: ReminderId) : Pane
    data class Editor(val state: EditorState) : Pane
    data class Life(val form: LifeForm) : Pane
}

/** A list of reminders with their detail and editor, laid out for the window width. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RemindersPane(
    tab: AgendaTab,
    model: RemindersModel,
    pane: Pane,
    onPane: (Pane) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by model.state.collectAsState()
    BoxWithConstraints(modifier) {
        val twoPane = WidthClass.of(maxWidth).isTwoPane
        BackHandler(enabled = !twoPane && pane != Pane.None) {
            onPane(if (pane is Pane.Editor && pane.state.base != null) Pane.Detail(pane.state.id) else Pane.None)
        }
        ListDetailLayout(
            showDetail = pane != Pane.None,
            list = {
                val open = { item: AgendaItem -> onPane(Pane.Detail(item.reminder.id)) }
                val toggle = { item: AgendaItem -> if (item.isHandled) model.undo(item) else model.act(item, AlertAction.Done) }
                when (tab) {
                    AgendaTab.Today -> TodayList(state, open, toggle, contentPadding = contentPadding.plus16())
                    AgendaTab.Upcoming -> UpcomingList(state, open, toggle, contentPadding = contentPadding.plus16())
                }
            },
            detail = {
                when (pane) {
                    Pane.None -> EmptyState(stringResource(Res.string.select_reminder), "")
                    is Pane.Detail -> {
                        val reminder = state.reminder(pane.id)
                        if (reminder == null) {
                            EmptyState(stringResource(Res.string.select_reminder), "")
                        } else {
                            ReminderDetail(
                                reminder = reminder,
                                state = state,
                                onEdit = { onPane(Pane.Editor(EditorState.from(reminder))) },
                                onDelete = { model.delete(reminder.id); onPane(Pane.None) },
                                onAct = model::act,
                                onToggleChecklist = { model.toggleChecklist(reminder, it) },
                                onBack = if (twoPane) null else ({ onPane(Pane.None) }),
                            )
                        }
                    }
                    is Pane.Life -> LifeEditor(
                        initial = pane.form,
                        today = state.now.toLocalDateTime(state.zone).date,
                        onSave = { form ->
                            val id = model.newId()
                            form.toReminders(id)?.forEach(model::save)
                            onPane(Pane.Detail(id))
                        },
                        onCancel = { onPane(Pane.None) },
                    )
                    is Pane.Editor -> ReminderEditor(
                        initial = pane.state,
                        today = state.now.toLocalDateTime(state.zone).date,
                        onSave = { edited ->
                            model.save(edited.toReminder())
                            onPane(Pane.Detail(edited.id))
                        },
                        onCancel = { onPane(if (pane.state.base != null) Pane.Detail(pane.state.id) else Pane.None) },
                    )
                }
            },
        )
    }
}

/** The scaffold's padding plus the lists' own 16 dp margin, and room at the bottom for the add button. */
@Composable
private fun PaddingValues.plus16(): PaddingValues {
    val direction = LocalLayoutDirection.current
    return PaddingValues(
        start = calculateStartPadding(direction) + 16.dp,
        top = calculateTopPadding() + 16.dp,
        end = calculateEndPadding(direction) + 16.dp,
        bottom = calculateBottomPadding() + 88.dp,
    )
}
