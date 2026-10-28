package com.pranavshukla.yaad.ui.designsystem.adaptive

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag

/** A top-level destination: Today, Upcoming, Money, Settings. */
data class NavItem(val label: String, val icon: ImageVector)

const val NavigationBarTag = "navigation-bar"
const val NavigationRailTag = "navigation-rail"

/**
 * The app's frame: a bottom navigation bar on phones and a navigation rail from 600 dp (iPad, tablets, landscape),
 * where the [floatingAction] moves to the top of the rail.
 */
@Composable
fun YaadNavigationScaffold(
    items: List<NavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    floatingAction: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        if (WidthClass.of(maxWidth).isTwoPane) {
            Row(Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.testTag(NavigationRailTag),
                    header = floatingAction?.let { { it() } },
                ) {
                    items.forEachIndexed { index, item ->
                        NavigationRailItem(
                            selected = index == selectedIndex,
                            onClick = { onSelect(index) },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(item.label) },
                        )
                    }
                }
                Scaffold(modifier = Modifier.weight(1f), content = content)
            }
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar(Modifier.testTag(NavigationBarTag)) {
                        items.forEachIndexed { index, item ->
                            NavigationBarItem(
                                selected = index == selectedIndex,
                                onClick = { onSelect(index) },
                                icon = { Icon(item.icon, contentDescription = null) },
                                label = { Text(item.label) },
                            )
                        }
                    }
                },
                floatingActionButton = { floatingAction?.invoke() },
                content = content,
            )
        }
    }
}
