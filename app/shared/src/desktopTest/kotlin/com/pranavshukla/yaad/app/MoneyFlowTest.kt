package com.pranavshukla.yaad.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.pranavshukla.yaad.core.model.ReminderType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

/** Phase 7: add a bill from its template, see it on this month's dashboard, and pay it. */
@OptIn(ExperimentalTestApi::class)
class MoneyFlowTest {
    private fun ComposeUiTest.waitFor(text: String) =
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty() }

    private fun ComposeUiTest.save(name: String) {
        waitForIdle()
        val file = File("build/screenshots/$name.png").apply { parentFile.mkdirs() }
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", file)
    }

    private fun flow(device: String, width: Int) = runComposeUiTest {
        val app = TestApp()
        setContent { Box(Modifier.requiredSize(width.dp, 760.dp)) { App(app.reminders, app.loans) } }

        onNodeWithText("Money").performClick()
        onNodeWithContentDescription("Add reminder").performClick()
        onNodeWithText("Bill (rent, electricity, phone…)").performClick()
        onAllNodes(hasSetTextAction())[0].performTextInput("Rent")
        onAllNodes(hasSetTextAction())[1].performTextInput("15000")
        onNodeWithText("Save").performClick()

        waitFor("Next: Today")
        val bill = runBlocking { app.store.reminders().first { it.isNotEmpty() }.single() }
        assertEquals(ReminderType.Bill, bill.type)
        save("$device-money-item")

        onNodeWithText("Done").performClick()
        if (width < 600) onNodeWithContentDescription("Back").performClick()
        waitFor("Paid")
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("₹15,000.00").fetchSemanticsNodes().size >= 2 }
        onAllNodesWithText("₹15,000.00").onFirst().assertExists()
        save("$device-money")
    }

    @Test
    fun onAPhone() = flow("phone", 400)

    @Test
    fun onAnIPad() = flow("ipad", 1000)
}
