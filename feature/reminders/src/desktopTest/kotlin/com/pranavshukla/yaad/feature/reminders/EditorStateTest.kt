package com.pranavshukla.yaad.feature.reminders

import com.pranavshukla.yaad.core.model.ChecklistItem
import com.pranavshukla.yaad.core.model.Currency
import com.pranavshukla.yaad.core.model.Frequency
import com.pranavshukla.yaad.core.model.Money
import com.pranavshukla.yaad.core.model.MonthlyOn
import com.pranavshukla.yaad.core.model.Priority
import com.pranavshukla.yaad.core.model.RecurrenceEnd
import com.pranavshukla.yaad.core.model.RecurrenceRule
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.model.ReminderId
import com.pranavshukla.yaad.core.model.ReminderType
import com.pranavshukla.yaad.core.model.Schedule
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class EditorStateTest {
    private val id = ReminderId("r1")
    private val zone = TimeZone.of("Asia/Kolkata")

    private fun roundTrip(reminder: Reminder) = assertEquals(reminder, EditorState.from(reminder).toReminder())

    @Test
    fun aNewReminderStartsAtTheNextWholeHourToday() {
        val state = EditorState.new(id, Instant.parse("2026-10-28T10:10:00Z"), zone) // 15:40 IST
        assertEquals(LocalDate(2026, 10, 28), state.date)
        assertEquals(LocalTime(16, 0), state.time)
        assertFalse(state.isValid, "a title is needed")
        assertTrue(state.copy(title = "Call mom").isValid)
    }

    @Test
    fun everyKindOfReminderSurvivesEditing() {
        val start = LocalDateTime(2026, 11, 5, 9, 0)
        roundTrip(Reminder(id, "Call", Schedule(start)))
        roundTrip(
            Reminder(
                id, "EMI", Schedule(start, RecurrenceRule(Frequency.Monthly(on = MonthlyOn.Day(5)), RecurrenceEnd.AfterCount(240)), "Asia/Kolkata"),
                type = ReminderType.Emi, notes = "HDFC", categoryId = "money", earlyAlerts = listOf(3.days, 1.hours),
                priority = Priority.High, nagInterval = 30.minutes, amount = Money(867_823, Currency.INR), autoPay = true,
                linkedLoanId = "loan-1", checklist = listOf(ChecklistItem("Pay", done = true)),
            ),
        )
        roundTrip(Reminder(id, "Gym", Schedule(start, RecurrenceRule(Frequency.Weekly(2, setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY))))))
        roundTrip(Reminder(id, "Rent", Schedule(start, RecurrenceRule(Frequency.Monthly(3, MonthlyOn.LastDay), RecurrenceEnd.OnDate(LocalDate(2027, 12, 31))))))
        roundTrip(Reminder(id, "Water", Schedule(start, RecurrenceRule(Frequency.Minutely(90)))))
        roundTrip(Reminder(id, "Passport", Schedule(LocalDateTime(2027, 3, 12, 0, 0), RecurrenceRule(Frequency.Yearly(10))), allDay = true))
    }

    @Test
    fun monthlyChoicesFollowTheDate() {
        // 26 November 2026 is the fourth Thursday; 31 December is the last day.
        val state = EditorState.new(id, Instant.parse("2026-10-28T10:00:00Z"), zone)
            .withDate(LocalDate(2026, 11, 26)).copy(title = "Bill", repeat = RepeatKind.Monthly)
        assertEquals(MonthlyOn.Day(26), state.copy(monthlyMode = MonthlyMode.Day).rule()!!.frequency.let { (it as Frequency.Monthly).on })
        assertEquals(MonthlyOn.NthWeekday(4, DayOfWeek.THURSDAY), (state.copy(monthlyMode = MonthlyMode.NthWeekday).rule()!!.frequency as Frequency.Monthly).on)
        val fifth = state.withDate(LocalDate(2026, 10, 29)).copy(monthlyMode = MonthlyMode.NthWeekday) // fifth Thursday
        assertEquals(MonthlyOn.NthWeekday(MonthlyOn.NthWeekday.LAST, DayOfWeek.THURSDAY), (fifth.rule()!!.frequency as Frequency.Monthly).on)
    }

    @Test
    fun weeklyWithNoDaysUsesTheDatesWeekday() {
        val state = EditorState.new(id, Instant.parse("2026-10-28T10:00:00Z"), zone).copy(title = "Gym", repeat = RepeatKind.Weekly)
        assertEquals(Frequency.Weekly(1, setOf(DayOfWeek.WEDNESDAY)), state.rule()!!.frequency)
    }

    @Test
    fun anEndBeforeTheStartIsInvalid() {
        val state = EditorState.new(id, Instant.parse("2026-10-28T10:00:00Z"), zone)
            .copy(title = "x", repeat = RepeatKind.Daily, end = EndKind.OnDate, endDate = LocalDate(2026, 10, 1))
        assertFalse(state.isValid)
        assertFalse(state.copy(end = EndKind.Never, interval = 0).isValid)
    }
}
