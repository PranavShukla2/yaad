package com.pranavshukla.yaad.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.pranavshukla.yaad.ui.designsystem.YaadTheme
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
fun App(alertAccess: AlertAccess = AlertAccess(), onFixAlertAccess: () -> Unit = {}) {
    YaadTheme {
        var selected by rememberSaveable { mutableIntStateOf(0) }
        val tabs = Tab.entries
        val icons = listOf(YaadIcons.Today, YaadIcons.Upcoming, YaadIcons.Money, YaadIcons.Settings)
        YaadNavigationScaffold(
            items = tabs.mapIndexed { i, tab -> NavItem(stringResource(tab.label), icons[i]) },
            selectedIndex = selected,
            onSelect = { selected = it },
            floatingAction = {
                FloatingActionButton(onClick = { /* The editor arrives in Phase 5. */ }) {
                    Icon(YaadIcons.Add, contentDescription = stringResource(Res.string.action_add))
                }
            },
        ) { padding ->
            val tab = tabs[selected]
            Column(Modifier.padding(padding)) {
                if (tab == Tab.Today) AlertAccessBanner(alertAccess, onFixAlertAccess)
                EmptyState(title = stringResource(tab.emptyTitle), message = stringResource(tab.emptyMessage))
            }
        }
    }
}
