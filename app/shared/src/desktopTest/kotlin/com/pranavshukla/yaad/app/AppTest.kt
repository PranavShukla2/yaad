package com.pranavshukla.yaad.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AppTest {
    @Test
    fun opensOnTodayAndSwitchesTabs() = runComposeUiTest {
        setContent { TestApp().let { App(it.reminders, it.loans) } }
        onNodeWithText("Nothing due today").assertExists()
        onNodeWithContentDescription("Add reminder").assertExists()

        onNodeWithText("Money").performClick()
        onNodeWithText("No loans yet").assertExists()
    }
}
