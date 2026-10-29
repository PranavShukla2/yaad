package com.pranavshukla.yaad.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class AlertAccessBannerTest {
    @Test
    fun noBannerWhenEverythingIsAllowed() = runComposeUiTest {
        setContent { TestApp().let { app -> App(app.reminders, app.loans, app.preferences, alertAccess = AlertAccess()) } }
        onNodeWithText("Notifications are off", substring = true).assertDoesNotExist()
        onNodeWithText("a few minutes late", substring = true).assertDoesNotExist()
    }

    @Test
    fun notificationsOffAsksToTurnThemOn() = runComposeUiTest {
        var asked = 0
        setContent { TestApp().let { app -> App(app.reminders, app.loans, app.preferences, alertAccess = AlertAccess(notifications = false), onFixAlertAccess = { asked++ }) } }
        onNodeWithText("Notifications are off", substring = true).assertExists()
        onNodeWithText("Turn on").performClick()
        assertEquals(1, asked)
    }

    @Test
    fun noExactAlarmsSaysRemindersMayBeLate() = runComposeUiTest {
        var asked = 0
        setContent { TestApp().let { app -> App(app.reminders, app.loans, app.preferences, alertAccess = AlertAccess(exactAlarms = false), onFixAlertAccess = { asked++ }) } }
        onNodeWithText("a few minutes late", substring = true).assertExists()
        onNodeWithText("Allow").performClick()
        assertEquals(1, asked)
    }
}
