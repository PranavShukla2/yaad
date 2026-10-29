package com.pranavshukla.yaad.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import com.pranavshukla.yaad.feature.reminders.LifeChooser
import com.pranavshukla.yaad.feature.reminders.LifeForm
import com.pranavshukla.yaad.feature.reminders.Pane
import com.pranavshukla.yaad.feature.reminders.RemindersModel
import com.pranavshukla.yaad.feature.reminders.RemindersPane
import com.pranavshukla.yaad.core.model.AppSettings
import com.pranavshukla.yaad.core.model.ThemeMode
import com.pranavshukla.yaad.core.scheduling.TimeLabel
import com.pranavshukla.yaad.data.PreferenceStore
import com.pranavshukla.yaad.feature.settings.SettingsCapabilities
import com.pranavshukla.yaad.feature.settings.SettingsScreen
import com.pranavshukla.yaad.ui.designsystem.LocalFirstDayOfWeek
import com.pranavshukla.yaad.ui.designsystem.LocalTwentyFourHour
import com.pranavshukla.yaad.ui.designsystem.YaadTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    preferences: PreferenceStore,
    alertAccess: AlertAccess = AlertAccess(),
    onFixAlertAccess: () -> Unit = {},
    capabilities: SettingsCapabilities = SettingsCapabilities(),
    onSettingsChanged: () -> Unit = {},
    onExport: () -> Unit = {},
    onRestore: () -> Unit = {},
) {
    val settings by remember(preferences) { preferences.settings() }.collectAsState(AppSettings())
    val scope = rememberCoroutineScope()
    val dark = when (settings.theme) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    YaadTheme(darkTheme = dark) {
      CompositionLocalProvider(LocalTwentyFourHour provides settings.twentyFourHour, LocalFirstDayOfWeek provides settings.firstDayOfWeek) {
        var selected by rememberSaveable { mutableIntStateOf(0) }
        var pane by remember { mutableStateOf<Pane>(Pane.None) }
        var moneyPane by remember { mutableStateOf<MoneyPaneState>(MoneyPaneState.None) }
        var choosing by remember { mutableStateOf(false) }
        var choosingLife by remember { mutableStateOf(false) }
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
                                if (tabs[selected] != Tab.Upcoming) selected = Tab.Today.ordinal
                                choosingLife = true
                            }
                        }) {
                            Icon(YaadIcons.Add, contentDescription = stringResource(Res.string.action_add))
                        }
                    }
                } else {
                    null
                },
            ) { padding ->
                if (choosingLife) {
                    val state = reminders.state.value
                    LifeChooser(
                        onReminder = {
                            choosingLife = false
                            pane = Pane.Editor(EditorState.new(reminders.newId(), state.now, state.zone))
                        },
                        onLife = { kind ->
                            choosingLife = false
                            pane = Pane.Life(LifeForm(kind, date = state.now.toLocalDateTime(state.zone).date))
                        },
                        onDismiss = { choosingLife = false },
                    )
                }
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
                        currency = settings.currency,
                        modifier = Modifier.padding(top = padding.calculateTopPadding()),
                        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = padding.calculateBottomPadding() + 88.dp),
                    )
                    Tab.Settings -> SettingsScreen(
                        settings = settings,
                        onChange = { changed ->
                            scope.launch {
                                withContext(Dispatchers.Default) { preferences.saveSettings(changed) }
                                onSettingsChanged()
                            }
                        },
                        capabilities = capabilities,
                        onExport = onExport,
                        onRestore = onRestore,
                        formatTime = { TimeLabel.format(it, settings.twentyFourHour) },
                        contentPadding = padding,
                    )
                }
            }
        }
      }
    }
}
