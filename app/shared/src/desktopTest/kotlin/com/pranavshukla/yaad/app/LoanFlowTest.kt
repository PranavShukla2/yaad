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
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.pranavshukla.yaad.core.model.ReminderType
import com.pranavshukla.yaad.feature.loans.LoanLenderTag
import com.pranavshukla.yaad.feature.loans.LoanPrincipalTag
import com.pranavshukla.yaad.feature.loans.LoanRateTag
import com.pranavshukla.yaad.feature.loans.LoanSaveTag
import com.pranavshukla.yaad.feature.loans.LoanTenureTag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

/** Phase 6: add a loan, see its EMI and schedule, get an EMI reminder, and mark an EMI paid. */
@OptIn(ExperimentalTestApi::class)
class LoanFlowTest {
    private fun ComposeUiTest.waitFor(text: String) =
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty() }

    private fun ComposeUiTest.type(tag: String, text: String) =
        onNode(hasSetTextAction() and (hasTestTag(tag) or androidx.compose.ui.test.hasAnyAncestor(hasTestTag(tag)))).performTextInput(text)

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
        type(LoanLenderTag, "HDFC")
        type(LoanPrincipalTag, "1000000")
        type(LoanRateTag, "8.5")
        type(LoanTenureTag, "20")
        waitFor("₹8,678.23")
        save("$device-loan-editor")
        onNodeWithTag(LoanSaveTag).performClick()

        waitFor("Outstanding")
        waitFor("₹8,678.23")
        save("$device-loan-detail")
        val reminder = runBlocking { app.store.reminders().first { it.isNotEmpty() }.single() }
        assertEquals(ReminderType.Emi, reminder.type)
        assertEquals("HDFC EMI", reminder.title)

        onNode(hasContentDescription("Mark EMI 1 paid")).performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasContentDescription("Mark EMI 1 unpaid")).fetchSemanticsNodes().isNotEmpty() }
        if (width < 600) onNodeWithContentDescription("Back").performClick()
        waitFor("1 of 240 EMIs paid")
        save("$device-loans")
    }

    @Test
    fun onAPhone() = flow("phone", 400)

    @Test
    fun onAnIPad() = flow("ipad", 1000)
}
