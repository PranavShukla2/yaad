package com.pranavshukla.yaad.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.pranavshukla.yaad.core.model.BuiltInCategory
import com.pranavshukla.yaad.core.model.ChecklistItem
import com.pranavshukla.yaad.core.model.Frequency
import com.pranavshukla.yaad.core.model.MonthlyOn
import com.pranavshukla.yaad.core.model.RecurrenceRule
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.model.Schedule
import com.pranavshukla.yaad.feature.reminders.RemindersModel
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/** Phone and iPad screenshots of the main screens, saved to app/shared/build/screenshots for review. */
@OptIn(ExperimentalTestApi::class)
class ScreenshotTest {
    private fun RemindersModel.seed() {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        fun at(days: Int, hour: Int) = LocalDateTime(today.plus(days, DateTimeUnit.DAY), LocalTime(hour, 0))
        listOf(
            Reminder(newId(), "Pay electricity bill", Schedule(at(-1, 10)), categoryId = BuiltInCategory.Money.id),
            Reminder(newId(), "Vitamin D", Schedule(at(-2, 9), RecurrenceRule(Frequency.Daily())), categoryId = BuiltInCategory.Health.id),
            Reminder(newId(), "Call mom", Schedule(at(0, 23)), checklist = listOf(ChecklistItem("Ask about Diwali plans"))),
            Reminder(
                newId(), "Home loan EMI", Schedule(at(3, 9), RecurrenceRule(Frequency.Monthly(on = MonthlyOn.Day(today.plus(3, DateTimeUnit.DAY).day)))),
                earlyAlerts = listOf(3.days, 1.hours), categoryId = BuiltInCategory.Money.id,
            ),
            Reminder(newId(), "Car service", Schedule(at(9, 11)), notes = "Ask for the 40,000 km check"),
        ).forEach(::save)
    }

    private fun ComposeUiTest.save(name: String) {
        waitForIdle()
        val file = File("build/screenshots/$name.png").apply { parentFile.mkdirs() }
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", file)
    }

    private fun screens(device: String, width: Int, height: Int) = runComposeUiTest {
        val app = TestApp().apply { reminders.seed() }
        setContent { Box(Modifier.requiredSize(width.dp, height.dp)) { App(app.reminders, app.loans, app.preferences) } }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Call mom")).fetchSemanticsNodes().isNotEmpty() }
        save("$device-today")
        onNodeWithText("Call mom").performClick()
        save("$device-detail")
        onNodeWithText("Upcoming").performClick()
        save("$device-upcoming")
    }

    @Test
    fun phone() = screens("phone", 400, 760)

    @Test
    fun iPad() = screens("ipad", 1000, 760)
}
