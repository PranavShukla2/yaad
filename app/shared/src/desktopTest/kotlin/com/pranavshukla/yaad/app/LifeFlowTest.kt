package com.pranavshukla.yaad.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.model.ReminderType
import com.pranavshukla.yaad.core.scheduling.Alert
import com.pranavshukla.yaad.core.scheduling.AlertKind
import com.pranavshukla.yaad.core.scheduling.PlanSettings
import com.pranavshukla.yaad.data.ReminderStore
import com.pranavshukla.yaad.platform.notifications.NotificationSync
import com.pranavshukla.yaad.platform.notifications.PlatformAlerts
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import com.pranavshukla.yaad.core.scheduling.LifeTemplates
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

/** Phase 8: a medicine nags until it is marked taken; a birthday shows the age. */
@OptIn(ExperimentalTestApi::class)
class LifeFlowTest {
    private val pending = mutableMapOf<String, Alert>()
    private val sync = NotificationSync(
        object : PlatformAlerts {
            override fun scheduledIds() = pending.keys.toSet()
            override fun schedule(alert: Alert, reminder: Reminder) { pending[alert.id] = alert }
            override fun cancel(id: String) { pending.remove(id) }
        },
        PlanSettings(),
    )

    private fun replan(store: ReminderStore) = runBlocking {
        val reminders = store.reminders().first()
        val history = store.allOccurrences().first()
        synchronized(pending) { sync.sync(reminders, history, Clock.System.now(), TimeZone.currentSystemDefault()) }
    }

    private fun ComposeUiTest.waitFor(text: String) =
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun aMedicineNagsUntilTaken() = runComposeUiTest {
        val app = TestApp(::replan)
        setContent { Box(Modifier.requiredSize(1000.dp, 760.dp)) { App(app.reminders, app.loans) } }
        onNodeWithContentDescription("Add reminder").performClick()
        onNodeWithText("Medicine").performClick()
        onNode(hasSetTextAction() and hasText("Name")).performTextInput("Vitamin D")
        onNodeWithText("Save").performClick()

        waitFor("Next:")
        val dose = runBlocking { app.store.reminders().first { it.isNotEmpty() }.single() }
        assertEquals(ReminderType.Medicine, dose.type)
        // The first planned alert is for the dose shown as next: nagging if it is past, otherwise its due alert.
        waitUntil(timeoutMillis = 5_000) { synchronized(pending) { pending.values.any { it.reminderId == dose.id } } }
        val shown = synchronized(pending) { pending.values.filter { it.reminderId == dose.id }.minBy { it.occurrenceDue }.occurrenceDue }

        onNodeWithText("Done").performClick()
        waitUntil(timeoutMillis = 5_000) { synchronized(pending) { pending.values.none { it.occurrenceDue == shown } } }
        assertTrue(synchronized(pending) { pending.values.none { it.occurrenceDue == shown && it.kind == AlertKind.Nag } }, "taken: no more nagging")
    }

    @Test
    fun aBirthdayShowsTheAge() = runComposeUiTest {
        val app = TestApp()
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val soon = today.plus(5, DateTimeUnit.DAY)
        val born = LocalDate(soon.year - 30, soon.month, soon.day)
        app.reminders.save(LifeTemplates.birthday(app.reminders.newId(), "Asha", born))
        setContent { Box(Modifier.requiredSize(1000.dp, 760.dp)) { App(app.reminders, app.loans) } }
        onNodeWithText("Upcoming").performClick()
        waitFor("Turns 30")
    }
}
