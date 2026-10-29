package com.pranavshukla.yaad.feature.loans

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import com.pranavshukla.yaad.core.model.Currency
import com.pranavshukla.yaad.feature.loans.resources.Res
import com.pranavshukla.yaad.feature.loans.resources.emi_title
import com.pranavshukla.yaad.feature.loans.resources.select_loan
import com.pranavshukla.yaad.ui.designsystem.adaptive.ListDetailLayout
import com.pranavshukla.yaad.ui.designsystem.adaptive.WidthClass
import com.pranavshukla.yaad.ui.designsystem.components.EmptyState
import org.jetbrains.compose.resources.stringResource

/** What shows beside the loan list (iPad) or instead of it (phone). */
sealed interface LoanPane {
    data object None : LoanPane
    data class Detail(val id: String) : LoanPane
    data class Editor(val form: LoanForm) : LoanPane
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LoansPane(
    model: LoansModel,
    pane: LoanPane,
    onPane: (LoanPane) -> Unit,
    modifier: Modifier = Modifier,
    currency: Currency = Currency.INR,
    contentPadding: PaddingValues = PaddingValues(16.dp()),
) {
    val state by model.state.collectAsState()
    val titleFor = rememberEmiTitle()
    BoxWithConstraints(modifier) {
        val twoPane = WidthClass.of(maxWidth).isTwoPane
        BackHandler(enabled = !twoPane && pane != LoanPane.None) {
            onPane(if (pane is LoanPane.Editor && pane.form.base != null) LoanPane.Detail(pane.form.id) else LoanPane.None)
        }
        ListDetailLayout(
            showDetail = pane != LoanPane.None,
            list = { LoansList(state, onOpen = { onPane(LoanPane.Detail(it.loan.id)) }, contentPadding = contentPadding) },
            detail = {
                when (pane) {
                    LoanPane.None -> EmptyState(stringResource(Res.string.select_loan), "")
                    is LoanPane.Detail -> state.loan(pane.id)?.let { view ->
                        LoanDetail(
                            view = view,
                            onEdit = { onPane(LoanPane.Editor(LoanForm.from(view.loan))) },
                            onDelete = { model.delete(view.loan); onPane(LoanPane.None) },
                            onSave = { model.save(it, titleFor(it.lender)) },
                            onTogglePaid = { model.markPaid(view, it) },
                            onBack = if (twoPane) null else ({ onPane(LoanPane.None) }),
                        )
                    } ?: EmptyState(stringResource(Res.string.select_loan), "")
                    is LoanPane.Editor -> {
                        LoanEditor(
                            initial = pane.form,
                            currency = currency,
                            onSave = { loan ->
                                model.save(loan, titleFor(loan.lender))
                                onPane(LoanPane.Detail(loan.id))
                            },
                            onCancel = { onPane(if (pane.form.base != null) LoanPane.Detail(pane.form.id) else LoanPane.None) },
                        )
                    }
                }
            },
        )
    }
}

/** Names a loan's EMI reminder ("HDFC EMI", localised) from the lender as saved. */
@Composable
fun rememberEmiTitle(): (String) -> String {
    val template = stringResource(Res.string.emi_title, LenderPlaceholder)
    return { lender -> template.replace(LenderPlaceholder, lender) }
}

private const val LenderPlaceholder = "{lender}"

private fun Int.dp() = androidx.compose.ui.unit.Dp(toFloat())
