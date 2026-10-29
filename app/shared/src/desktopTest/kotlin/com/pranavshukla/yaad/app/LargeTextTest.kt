package com.pranavshukla.yaad.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.pranavshukla.yaad.core.model.Money
import com.pranavshukla.yaad.core.model.Currency
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.model.Schedule
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.time.Clock

/** Phase 10: the main screens still work with text at 200%, as set in the system's accessibility settings. */
@OptIn(ExperimentalTestApi::class)
class LargeTextTest {
    @Test
    fun twiceTheTextSize() = runComposeUiTest {
        val app = TestApp()
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        app.reminders.save(Reminder(app.reminders.newId(), "Pay the electricity bill", Schedule(LocalDateTime(today, LocalTime(23, 30))), amount = Money(240_000, Currency.INR)))
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                Box(Modifier.requiredSize(400.dp, 760.dp)) { App(app.reminders, app.loans, app.preferences) }
            }
        }
        fun save(name: String) = File("build/screenshots/phone-200-$name.png").apply { parentFile.mkdirs() }
            .let { ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", it) }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Pay the electricity bill")).fetchSemanticsNodes().isNotEmpty() }
        save("today")
        onNodeWithText("Pay the electricity bill").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("₹2,400.00")).fetchSemanticsNodes().isNotEmpty() }
        save("detail")
        onNodeWithText("Settings").performClick()
        waitForIdle()
        save("settings")
    }
}
