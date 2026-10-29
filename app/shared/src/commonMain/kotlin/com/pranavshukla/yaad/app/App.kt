package com.pranavshukla.yaad.app

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.toLocalDateTime
import com.pranavshukla.yaad.app.resources.Res
import com.pranavshukla.yaad.app.resources.action_add
import com.pranavshukla.yaad.app.resources.empty_money_message
import com.pranavshukla.yaad.app.resources.empty_money_title
import com.pranavshukla.yaad.app.resources.empty_settings_message
import com.pranavshukla.yaad.app.resources.empty_settings_title
import com.pranavshukla.yaad.app.resources.empty_today_message
import com.pranavshukla.yaad.app.resources.empty_today_title
import com.pranavshukla.yaad.app.resources.empty_upcoming_message
import com.pranavshukla.yaad.app.resources.empty_upcoming_title
import com.pranavshukla.yaad.app.resources.tab_money
import com.pranavshukla.yaad.app.resources.tab_settings
import com.pranavshukla.yaad.app.resources.tab_today
import com.pranavshukla.yaad.app.resources.tab_upcoming
import com.pranavshukla.yaad.feature.loans.LoanForm
import com.pranavshukla.yaad.feature.loans.LoansModel
import com.pranavshukla.yaad.feature.loans.rememberEmiTitle
import com.pranavshukla.yaad.feature.money.MoneyChoice
import com.pranavshukla.yaad.feature.money.MoneyChooser
import com.pranavshukla.yaad.feature.money.MoneyItemForm
import com.pranavshukla.yaad.feature.money.MoneyPane
import com.pranavshukla.yaad.feature.money.MoneyPaneState
import com.pranavshukla.yaad.feature.reminders.AgendaTab
import com.pranavshukla.yaad.feature.reminders.EditorState
import com.pranavshukla.yaad.feature.reminders.Pane
import com.pranavshukla.yaad.feature.reminders.RemindersModel
import com.pranavshukla.yaad.feature.reminders.RemindersPane
import com.pranavshukla.yaad.ui.designsystem.YaadTheme
import com.pranavshukla.yaad.ui.designsystem.adaptive.WidthClass
import com.pranavshukla.yaad.ui.designsystem.adaptive.NavItem
import com.pranavshukla.yaad.ui.designsystem.adaptive.YaadNavigationScaffold
import com.pranavshukla.yaad.ui.designsystem.components.EmptyState
import com.pranavshukla.yaad.ui.designsystem.icons.YaadIcons
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private enum class Tab(val label: StringResource, val emptyTitle: StringResource, val emptyMessage: StringResource) {
    Today(Res.string.tab_today, Res.string.empty_today_title, Res.string.empty_today_message),
    Upcoming(Res.string.tab_upcoming, Res.string.empty_upcoming_title, Res.string.empty_upcoming_message),
    Money(Res.string.tab_money, Res.string.empty_money_title, Res.string.empty_money_message),
    Settings(Res.string.tab_settings, Res.string.empty_settings_title, Res.string.empty_settings_message),
}

/**
 * The Yaad app, shared by Android, iOS and desktop. [alertAccess] is what the platform currently allows; when
 * something is missing, Today explains it and [onFixAlertAccess] takes the user to fix it.
 */
@Composable
fun App(
    reminders: RemindersModel,
    loans: LoansModel,
    alertAccess: AlertAccess = AlertAccess(),
    onFixAlertAccess: () -> Unit = {},
) {
    YaadTheme {
        var selected by rememberSaveable { mutableIntStateOf(0) }
        var pane by remember { mutableStateOf<Pane>(Pane.None) }
        var moneyPane by remember { mutableStateOf<MoneyPaneState>(MoneyPaneState.None) }
        var choosing by remember { mutableStateOf(false) }
        val emiTitle = rememberEmiTitle()
        val tabs = Tab.entries
        val icons = listOf(YaadIcons.Today, YaadIcons.Upcoming, YaadIcons.Money, YaadIcons.Settings)
        BoxWithConstraints {
            val twoPane = WidthClass.of(maxWidth).isTwoPane
            YaadNavigationScaffold(
                items = tabs.mapIndexed { i, tab -> NavItem(stringResource(tab.label), icons[i]) },
                selectedIndex = selected,
                onSelect = { selected = it; pane = Pane.None; moneyPane = MoneyPaneState.None },
                floatingAction = if (twoPane || (pane == Pane.None && moneyPane == MoneyPaneState.None)) {
                    {
                        FloatingActionButton(onClick = {
                            if (tabs[selected] == Tab.Money) {
                                choosing = true
                            } else {
                                val state = reminders.state.value
                                if (tabs[selected] != Tab.Upcoming) selected = Tab.Today.ordinal
                                pane = Pane.Editor(EditorState.new(reminders.newId(), state.now, state.zone))
                            }
                        }) {
                            Icon(YaadIcons.Add, contentDescription = stringResource(Res.string.action_add))
                        }
                    }
                } else {
                    null
                },
            ) { padding ->
                if (choosing) {
                    MoneyChooser(
                        onChoose = { choice ->
                            choosing = false
                            val today = reminders.state.value.let { s -> s.now.toLocalDateTime(s.zone).date }
                            moneyPane = when (choice) {
                                MoneyChoice.Loan -> MoneyPaneState.LoanEdit(LoanForm(id = loans.newId(), firstEmiDate = today))
                                is MoneyChoice.Item -> MoneyPaneState.NewItem(MoneyItemForm(choice.kind, date = today))
                            }
                        },
                        onDismiss = { choosing = false },
                    )
                }
                when (val tab = tabs[selected]) {
                    Tab.Today, Tab.Upcoming -> Column(Modifier.padding(top = padding.calculateTopPadding())) {
                        if (tab == Tab.Today && pane == Pane.None) AlertAccessBanner(alertAccess, onFixAlertAccess)
                        RemindersPane(
                            tab = if (tab == Tab.Today) AgendaTab.Today else AgendaTab.Upcoming,
                            model = reminders,
                            pane = pane,
                            onPane = { pane = it },
                            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding()),
                        )
                    }
                    Tab.Money -> MoneyPane(
                        reminders = reminders,
                        loans = loans,
                        pane = moneyPane,
                        onPane = { moneyPane = it },
                        emiTitle = emiTitle,
                        modifier = Modifier.padding(top = padding.calculateTopPadding()),
                        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = padding.calculateBottomPadding() + 88.dp),
                    )
                    Tab.Settings -> EmptyState(
                        title = stringResource(tab.emptyTitle),
                        message = stringResource(tab.emptyMessage),
                        modifier = Modifier.padding(padding),
                    )
                }
            }
        }
    }
}
