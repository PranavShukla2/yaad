package com.pranavshukla.yaad.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class LockScreenTest {
    @Test
    fun aLockedAppShowsNothingButTheLock() = runComposeUiTest {
        val app = TestApp()
        var asked = 0
        setContent { App(app.reminders, app.loans, app.preferences, locked = true, onUnlock = { asked++ }) }
        onNodeWithText("Yaad is locked").assertExists()
        onNodeWithText("Today").assertDoesNotExist()
        onNodeWithText("Unlock").performClick()
        assertEquals(1, asked)
    }
}
