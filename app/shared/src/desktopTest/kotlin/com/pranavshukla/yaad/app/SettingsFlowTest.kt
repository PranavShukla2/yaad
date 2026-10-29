package com.pranavshukla.yaad.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.model.Schedule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

/** Phase 10: a setting changes what the screens show, and is kept. */
@OptIn(ExperimentalTestApi::class)
class SettingsFlowTest {
    @Test
    fun theTwentyFourHourClock() = runComposeUiTest {
        val app = TestApp()
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        app.reminders.save(Reminder(app.reminders.newId(), "Late call", Schedule(LocalDateTime(today, LocalTime(23, 59)))))
        setContent { Box(Modifier.requiredSize(400.dp, 760.dp)) { App(app.reminders, app.loans, app.preferences) } }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("11:59 PM", substring = true)).fetchSemanticsNodes().isNotEmpty() }

        onNodeWithText("Settings").performClick()
        onNodeWithText("24-hour clock").performClick()
        waitUntil(timeoutMillis = 5_000) { runBlocking { app.preferences.settings().first().twentyFourHour } }
        onNodeWithText("Today").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("23:59", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(true, runBlocking { app.preferences.settings().first().twentyFourHour })
    }
}
