package com.pranavshukla.yaad.ui.designsystem.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pranavshukla.yaad.ui.designsystem.YaadTheme
import com.pranavshukla.yaad.ui.designsystem.icons.YaadIcons
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class YaadNavigationScaffoldTest {
    private val items = listOf(NavItem("Today", YaadIcons.Today), NavItem("Upcoming", YaadIcons.Upcoming))

    @Composable
    private fun Sized(width: Dp) {
        var selected by remember { mutableIntStateOf(0) }
        YaadTheme(dynamicColor = false) {
            Box(Modifier.requiredSize(width, 800.dp)) {
                YaadNavigationScaffold(items, selected, onSelect = { selected = it }) {
                    Text("Showing ${items[selected].label}")
                }
            }
        }
    }

    @Test
    fun phoneUsesABottomBar() = runComposeUiTest {
        setContent { Sized(400.dp) }
        onNodeWithTag(NavigationBarTag).assertExists()
        onNodeWithTag(NavigationRailTag).assertDoesNotExist()
    }

    @Test
    fun tabletUsesARail() = runComposeUiTest {
        setContent { Sized(1000.dp) }
        onNodeWithTag(NavigationRailTag).assertExists()
        onNodeWithTag(NavigationBarTag).assertDoesNotExist()
    }

    @Test
    fun selectingAnItemSwitchesTheContent() = runComposeUiTest {
        setContent { Sized(400.dp) }
        onNodeWithText("Upcoming").performClick()
        onNodeWithText("Upcoming").assertIsSelected()
        onNodeWithText("Showing Upcoming").assertExists()
    }
}
