package com.pranavshukla.yaad.feature.money

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import com.pranavshukla.yaad.core.model.Currency
import com.pranavshukla.yaad.core.model.ReminderId
import com.pranavshukla.yaad.feature.loans.LoanDetail
import com.pranavshukla.yaad.feature.loans.LoanEditor
import com.pranavshukla.yaad.feature.loans.LoanForm
import com.pranavshukla.yaad.feature.loans.LoansModel
import com.pranavshukla.yaad.feature.money.resources.Res
import com.pranavshukla.yaad.feature.money.resources.select_item
import com.pranavshukla.yaad.feature.reminders.EditorState
import com.pranavshukla.yaad.feature.reminders.ReminderDetail
import com.pranavshukla.yaad.feature.reminders.ReminderEditor
import com.pranavshukla.yaad.feature.reminders.RemindersModel
import com.pranavshukla.yaad.ui.designsystem.adaptive.ListDetailLayout
import com.pranavshukla.yaad.ui.designsystem.adaptive.WidthClass
import com.pranavshukla.yaad.ui.designsystem.components.EmptyState
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

/** What shows beside the Money list (iPad) or instead of it (phone). */
sealed interface MoneyPaneState {
    data object None : MoneyPaneState
    data class Loan(val id: String) : MoneyPaneState
    data class LoanEdit(val form: LoanForm) : MoneyPaneState
    data class Item(val id: ReminderId) : MoneyPaneState
    data class ItemEdit(val state: EditorState) : MoneyPaneState
    data class NewItem(val form: MoneyItemForm) : MoneyPaneState
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MoneyPane(
    reminders: RemindersModel,
    loans: LoansModel,
    pane: MoneyPaneState,
    onPane: (MoneyPaneState) -> Unit,
    emiTitle: (String) -> String,
    modifier: Modifier = Modifier,
    currency: Currency = Currency.INR,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val remindersState by reminders.state.collectAsState()
    val loansState by loans.state.collectAsState()
    val today = remindersState.now.toLocalDateTime(remindersState.zone).date
    BoxWithConstraints(modifier) {
        val twoPane = WidthClass.of(maxWidth).isTwoPane
        val back: () -> Unit = {
            onPane(
                when (pane) {
                    is MoneyPaneState.LoanEdit -> pane.form.base?.let { MoneyPaneState.Loan(it.id) } ?: MoneyPaneState.None
                    is MoneyPaneState.ItemEdit -> MoneyPaneState.Item(pane.state.id)
                    else -> MoneyPaneState.None
                },
            )
        }
        BackHandler(enabled = !twoPane && pane != MoneyPaneState.None) { back() }
        ListDetailLayout(
            showDetail = pane != MoneyPaneState.None,
            list = {
                MoneyHome(
                    remindersState, loansState,
                    onOpenLoan = { onPane(MoneyPaneState.Loan(it.loan.id)) },
                    onOpenItem = { onPane(MoneyPaneState.Item(it.id)) },
                    currency = currency,
                    contentPadding = contentPadding,
                )
            },
            detail = {
                val none = @Composable { EmptyState(stringResource(Res.string.select_item), "") }
                val backButton = if (twoPane) null else back
                when (pane) {
                    MoneyPaneState.None -> none()
                    is MoneyPaneState.Loan -> loansState.loan(pane.id)?.let { view ->
                        LoanDetail(
                            view = view,
                            onEdit = { onPane(MoneyPaneState.LoanEdit(LoanForm.from(view.loan))) },
                            onDelete = { loans.delete(view.loan); onPane(MoneyPaneState.None) },
                            onSave = { loans.save(it, emiTitle(it.lender)) },
                            onTogglePaid = { loans.markPaid(view, it) },
                            onBack = backButton,
                        )
                    } ?: none()
                    is MoneyPaneState.LoanEdit -> LoanEditor(
                        initial = pane.form,
                        currency = currency,
                        onSave = { loan -> loans.save(loan, emiTitle(loan.lender)); onPane(MoneyPaneState.Loan(loan.id)) },
                        onCancel = back,
                    )
                    is MoneyPaneState.Item -> remindersState.reminder(pane.id)?.let { reminder ->
                        ReminderDetail(
                            reminder = reminder,
                            state = remindersState,
                            onEdit = { onPane(MoneyPaneState.ItemEdit(EditorState.from(reminder))) },
                            onDelete = { reminders.delete(reminder.id); onPane(MoneyPaneState.None) },
                            onAct = reminders::act,
                            onToggleChecklist = { reminders.toggleChecklist(reminder, it) },
                            onBack = backButton,
                        )
                    } ?: none()
                    is MoneyPaneState.ItemEdit -> ReminderEditor(
                        initial = pane.state,
                        today = today,
                        onSave = { reminders.save(it.toReminder()); onPane(MoneyPaneState.Item(it.id)) },
                        onCancel = back,
                    )
                    is MoneyPaneState.NewItem -> MoneyItemEditor(
                        initial = pane.form,
                        today = today,
                        currency = currency,
                        onSave = { form ->
                            val id = reminders.newId()
                            form.toReminders(id, currency)?.forEach(reminders::save)
                            onPane(MoneyPaneState.Item(id))
                        },
                        onCancel = back,
                    )
                }
            },
        )
    }
}
