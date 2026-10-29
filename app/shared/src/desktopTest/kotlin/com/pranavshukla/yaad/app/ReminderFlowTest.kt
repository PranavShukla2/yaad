package com.pranavshukla.yaad.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.scheduling.Alert
import com.pranavshukla.yaad.core.scheduling.PlanSettings
import com.pranavshukla.yaad.data.ReminderStore
import com.pranavshukla.yaad.feature.reminders.EditorSaveTag
import com.pranavshukla.yaad.feature.reminders.EditorTitleTag
import com.pranavshukla.yaad.platform.notifications.NotificationSync
import com.pranavshukla.yaad.platform.notifications.PlatformAlerts
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

/** Stands in for AlarmManager / UNUserNotificationCenter. */
private class RecordingPlatform : PlatformAlerts {
    val pending = mutableMapOf<String, Alert>()
    override fun scheduledIds() = pending.keys.toSet()
    override fun schedule(alert: Alert, reminder: Reminder) { pending[alert.id] = alert }
    override fun cancel(id: String) { pending.remove(id) }
}

/** The acceptance flow of Phase 5: create a reminder, see its notification planned, mark it done. */
@OptIn(ExperimentalTestApi::class)
class ReminderFlowTest {
    private val platform = RecordingPlatform()
    private val sync = NotificationSync(platform, PlanSettings())

    /** What the platforms do after every change: re-plan from the database. */
    private fun replan(store: ReminderStore) = runBlocking {
        val reminders = store.reminders().first()
        val history = store.allOccurrences().first()
        synchronized(platform) { sync.sync(reminders, history, Clock.System.now(), TimeZone.currentSystemDefault()) }
    }

    private fun ComposeUiTest.waitFor(text: String) =
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty() }

    private fun flow(width: Int) = runComposeUiTest {
        val app = TestApp(::replan)
        setContent { Box(Modifier.requiredSize(width.dp, 760.dp)) { App(app.reminders, app.loans, app.preferences) } }

        onNodeWithContentDescription("Add reminder").performClick()
        onNode(hasText("Reminder")).performClick()
        onNodeWithTag(EditorTitleTag).performTextInput("Pay electricity bill")
        onNodeWithTag(EditorSaveTag).performClick()

        waitFor("Pay electricity bill")
        waitUntil(timeoutMillis = 5_000) { platform.pending.isNotEmpty() }
        assertEquals(1, platform.pending.size, "one notification planned at its due time")

        if (width < 600) onNodeWithContentDescription("Back").performClick()
        onNode(hasContentDescription("Mark Pay electricity bill done")).performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasContentDescription("Undo")).fetchSemanticsNodes().isNotEmpty() }
        waitUntil(timeoutMillis = 5_000) { platform.pending.isEmpty() }
        assertTrue(platform.pending.isEmpty(), "done cancels the notification")
    }

    @Test
    fun onAPhone() = flow(width = 400)

    @Test
    fun onAnIPad() = flow(width = 1000)
}
