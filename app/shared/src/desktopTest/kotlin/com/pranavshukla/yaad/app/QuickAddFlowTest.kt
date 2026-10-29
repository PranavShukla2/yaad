package com.pranavshukla.yaad.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.pranavshukla.yaad.feature.reminders.QuickAddFieldTag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.toLocalDateTime
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

/** Phase 9: quick add, then find the reminder in search and on the calendar. */
@OptIn(ExperimentalTestApi::class)
class QuickAddFlowTest {
    private fun ComposeUiTest.waitFor(text: String) =
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty() }

    private fun ComposeUiTest.save(name: String) {
        waitForIdle()
        val file = File("build/screenshots/$name.png").apply { parentFile.mkdirs() }
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", file)
    }

    private fun flow(device: String, width: Int) = runComposeUiTest {
        val app = TestApp()
        setContent { Box(Modifier.requiredSize(width.dp, 760.dp)) { App(app.reminders, app.loans, app.preferences) } }

        onNodeWithTag(QuickAddFieldTag).performTextInput("pay rent 15000 on 5th every month")
        waitFor("Understood as")
        waitFor("Every month on day 5")
        waitFor("₹15,000.00")
        save("$device-quick-add")
        onNodeWithText("Add").performClick()

        val saved = runBlocking { app.store.reminders().first { it.isNotEmpty() }.single() }
        assertEquals("Pay rent", saved.title)

        onNodeWithText("Upcoming").performClick()
        onNodeWithText("Search").performClick()
        onNode(hasSetTextAction()).performTextInput("rent")
        waitFor("Pay rent")
        save("$device-search")

        onNodeWithText("Calendar").performClick()
        val fifth = saved.schedule.start.date
        val today = kotlin.time.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
        if (fifth.month != today.month) onNode(hasContentDescription("Next month")).performClick()
        onNode(hasContentDescription("${fifth.day} ", substring = true) and hasContentDescription("1 items", substring = true)).performClick()
        waitFor("Pay rent")
        save("$device-calendar")
    }

    @Test
    fun onAPhone() = flow("phone", 400)

    @Test
    fun onAnIPad() = flow("ipad", 1000)
}
