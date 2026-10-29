package com.pranavshukla.yaad.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import java.io.File
import java.util.Locale
import javax.imageio.ImageIO
import kotlin.test.Test

/** Phase 10: with the device in Hindi, Yaad is in Hindi. */
@OptIn(ExperimentalTestApi::class)
class HindiTest {
    @Test
    fun theAppSpeaksHindi() {
        val before = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("hi-IN"))
        try {
            runComposeUiTest {
                val app = TestApp()
                setContent { Box(Modifier.requiredSize(400.dp, 760.dp)) { App(app.reminders, app.loans, app.preferences) } }
                waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("आज कुछ बाकी नहीं")).fetchSemanticsNodes().isNotEmpty() }
                File("build/screenshots/phone-hindi-today.png").apply { parentFile.mkdirs() }
                    .let { ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", it) }
                onNodeWithText("सेटिंग").performClick()
                waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("शांत घंटे")).fetchSemanticsNodes().isNotEmpty() }
                File("build/screenshots/phone-hindi-settings.png")
                    .let { ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", it) }
            }
        } finally {
            Locale.setDefault(before)
        }
    }
}
