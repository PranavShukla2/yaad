package com.pranavshukla.yaad.ui.designsystem.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pranavshukla.yaad.ui.designsystem.YaadTheme
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ListDetailLayoutTest {
    @Composable
    private fun Sized(width: Dp, showDetail: Boolean) {
        YaadTheme(dynamicColor = false) {
            Box(Modifier.requiredSize(width, 800.dp)) {
                ListDetailLayout(
                    showDetail = showDetail,
                    list = { Text("List pane") },
                    detail = { Text("Detail pane") },
                )
            }
        }
    }

    @Test
    fun phoneShowsTheListAlone() = runComposeUiTest {
        setContent { Sized(400.dp, showDetail = false) }
        onNodeWithText("List pane").assertIsDisplayed()
        onNodeWithText("Detail pane").assertDoesNotExist()
    }

    @Test
    fun phoneShowsTheDetailAloneWhenOneIsOpen() = runComposeUiTest {
        setContent { Sized(400.dp, showDetail = true) }
        onNodeWithText("Detail pane").assertIsDisplayed()
        onNodeWithText("List pane").assertDoesNotExist()
    }

    @Test
    fun tabletShowsBothPanes() = runComposeUiTest {
        setContent { Sized(1000.dp, showDetail = false) }
        onNodeWithText("List pane").assertIsDisplayed()
        onNodeWithText("Detail pane").assertIsDisplayed()
    }
}
