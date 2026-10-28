package com.pranavshukla.yaad.feature.reminders

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.pranavshukla.yaad.core.model.Frequency
import com.pranavshukla.yaad.core.model.MonthlyOn
import com.pranavshukla.yaad.core.model.RecurrenceEnd
import com.pranavshukla.yaad.core.model.RecurrenceRule
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class LabelsTest {
    private val today = LocalDate(2026, 10, 28)
    private val zone = TimeZone.of("Asia/Kolkata")
    private val now = Instant.parse("2026-10-28T10:00:00Z")

    @Test
    fun rulesReadNaturally() = runComposeUiTest {
        setContent {
            Column {
                Text(ruleText(null, today))
                Text(ruleText(RecurrenceRule(Frequency.Weekly(2, setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY)), RecurrenceEnd.AfterCount(10)), today))
                Text(ruleText(RecurrenceRule(Frequency.Monthly(on = MonthlyOn.NthWeekday(MonthlyOn.NthWeekday.LAST, DayOfWeek.FRIDAY))), today))
                Text(ruleText(RecurrenceRule(Frequency.Monthly(on = MonthlyOn.Day(5)), RecurrenceEnd.OnDate(LocalDate(2027, 3, 5))), today))
                Text(ruleText(RecurrenceRule(Frequency.Minutely(90)), today))
            }
        }
        onNodeWithText("Does not repeat").assertExists()
        onNodeWithText("Every 2 weeks on Mon, Fri, 10 times").assertExists()
        onNodeWithText("Every month on the last Friday").assertExists()
        onNodeWithText("Every month on day 5, until 5 Mar 2027").assertExists()
        onNodeWithText("Every 90 min").assertExists()
    }

    @Test
    fun dueTimesAreRelativeToToday() = runComposeUiTest {
        setContent {
            Column {
                Text(dueText(Instant.parse("2026-10-28T13:30:00Z"), allDay = false, now = now, zone = zone))
                Text(dueText(Instant.parse("2026-10-30T03:30:00Z"), allDay = false, now = now, zone = zone))
                Text(dueText(Instant.parse("2026-11-04T18:30:00Z"), allDay = true, now = now, zone = zone))
            }
        }
        onNodeWithText("Today, 7:00 PM").assertExists()
        onNodeWithText("Friday, 9:00 AM").assertExists()
        onNodeWithText("5 Nov").assertExists()
    }

    @Test
    fun earlyAlerts() = runComposeUiTest {
        setContent {
            Column {
                Text(beforeText(1.days))
                Text(beforeText(3.days))
                Text(beforeText(2.hours))
                Text(beforeText(15.minutes))
            }
        }
        listOf("1 day before", "3 days before", "2 h before", "15 min before").forEach { onNodeWithText(it).assertExists() }
    }
}
