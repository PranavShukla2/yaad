package com.pranavshukla.yaad.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import com.pranavshukla.yaad.app.resources.locked_message
import com.pranavshukla.yaad.app.resources.locked_title
import com.pranavshukla.yaad.app.resources.unlock
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
import com.pranavshukla.yaad.data.Backup
import com.pranavshukla.yaad.data.EncryptedBackup
import com.pranavshukla.yaad.data.PreferenceStore
import com.pranavshukla.yaad.feature.settings.BackupMessage
import com.pranavshukla.yaad.feature.settings.BackupPasswordDialog
import com.pranavshukla.yaad.feature.settings.SettingsCapabilities
import com.pranavshukla.yaad.feature.settings.backupMessages
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
    backup: Backup? = null,
    backupHost: BackupHost? = null,
    locked: Boolean = false,
    onUnlock: () -> Unit = {},
) {
    val settings by remember(preferences) { preferences.settings() }.collectAsState(AppSettings())
    val scope = rememberCoroutineScope()
    var backupStep by remember { mutableStateOf<BackupStep?>(null) }
    val dark = when (settings.theme) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    YaadTheme(darkTheme = dark) {
      CompositionLocalProvider(LocalTwentyFourHour provides settings.twentyFourHour, LocalFirstDayOfWeek provides settings.firstDayOfWeek) {
        if (locked) {
            LockScreen(onUnlock)
            return@CompositionLocalProvider
        }
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
                floatingAction = if (tabs[selected] != Tab.Settings && (twoPane || (pane == Pane.None && moneyPane == MoneyPaneState.None))) {
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
                backupStep?.let { step ->
                    val messages = backupMessages()
                    when (step) {
                        BackupStep.ExportPassword -> BackupPasswordDialog(confirm = true, onDismiss = { backupStep = null }, onDone = { password ->
                            backupStep = null
                            scope.launch { BackupFlow(backup!!, backupHost!!).export(password, reminders.state.value.now) }
                        })
                        is BackupStep.RestorePassword -> BackupPasswordDialog(confirm = false, onDismiss = { backupStep = null }, onDone = { password ->
                            backupStep = null
                            scope.launch {
                                val result = BackupFlow(backup!!, backupHost!!).restore(step.text, password)
                                backupStep = BackupStep.Message(result.exceptionOrNull()?.let { messages.failed.replace("%s", it.message ?: "") } ?: messages.restored)
                                if (result.isSuccess) onSettingsChanged()
                            }
                        })
                        is BackupStep.Message -> BackupMessage(step.text) { backupStep = null }
                    }
                }
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
                        capabilities = capabilities.copy(canBackUp = backup != null && backupHost != null),
                        onExport = { backupStep = BackupStep.ExportPassword },
                        onRestore = { backupHost?.open { text -> backupStep = BackupStep.RestorePassword(text) } },
                        formatTime = { TimeLabel.format(it, settings.twentyFourHour) },
                        contentPadding = padding,
                    )
                }
            }
        }
      }
    }
}

/** Shown instead of everything else while the app lock is on and Yaad has not been unlocked. */
@Composable
private fun LockScreen(onUnlock: () -> Unit) {
    androidx.compose.material3.Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp, androidx.compose.ui.Alignment.CenterVertically),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        ) {
            androidx.compose.material3.Text(stringResource(Res.string.locked_title), style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
            androidx.compose.material3.Text(stringResource(Res.string.locked_message))
            androidx.compose.material3.Button(onClick = onUnlock) { androidx.compose.material3.Text(stringResource(Res.string.unlock)) }
        }
    }
}

/** Saving and opening backup files, which only the platform can do (a save dialog, a share sheet, a file picker). */
interface BackupHost {
    fun save(fileName: String, content: String)

    /** Lets the user choose a backup file and calls [onOpened] with its text; nothing happens if they cancel. */
    fun open(onOpened: (String) -> Unit)
}

private sealed interface BackupStep {
    data object ExportPassword : BackupStep
    data class RestorePassword(val text: String) : BackupStep
    data class Message(val text: String) : BackupStep
}

/** Making and restoring encrypted backups, off the main thread; the platform saves and opens the file. */
class BackupFlow(private val backup: Backup, private val host: BackupHost) {
    suspend fun export(password: String, now: kotlin.time.Instant) {
        val sealed = withContext(Dispatchers.Default) { EncryptedBackup.seal(backup.export(), password) }
        host.save(backupFileName(now), sealed)
    }

    /** Replaces everything with the backup in [text]; a wrong password or damaged file changes nothing. */
    suspend fun restore(text: String, password: String): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching { backup.import(EncryptedBackup.open(text, password)) }
    }
}

/** "yaad-backup-2026-10-29.yaad". */
internal fun backupFileName(now: kotlin.time.Instant): String =
    "yaad-backup-${now.toString().take(10)}.yaad"
