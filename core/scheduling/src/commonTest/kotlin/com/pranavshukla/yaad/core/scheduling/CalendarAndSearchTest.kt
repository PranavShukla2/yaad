package com.pranavshukla.yaad.core.scheduling

import com.pranavshukla.yaad.core.model.BuiltInCategory
import com.pranavshukla.yaad.core.model.Currency
import com.pranavshukla.yaad.core.model.Frequency
import com.pranavshukla.yaad.core.model.Money
import com.pranavshukla.yaad.core.model.MonthlyOn
import com.pranavshukla.yaad.core.model.Occurrence
import com.pranavshukla.yaad.core.model.OccurrenceStatus
import com.pranavshukla.yaad.core.model.RecurrenceRule
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.model.ReminderId
import com.pranavshukla.yaad.core.model.ReminderType
import com.pranavshukla.yaad.core.model.Schedule
import com.pranavshukla.yaad.core.recurrence.occurrencesBetween
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class CalendarAndSearchTest {
    private val zone = TimeZone.of("Asia/Kolkata")
    private val now = Instant.parse("2026-10-28T10:00:00Z")
    private val gym = Reminder(ReminderId("gym"), "Gym", Schedule(LocalDateTime(2026, 10, 5, 7, 0), RecurrenceRule(Frequency.Weekly(1, setOf(kotlinx.datetime.DayOfWeek.MONDAY, kotlinx.datetime.DayOfWeek.THURSDAY)))), categoryId = BuiltInCategory.Health.id)
    private val rent = MoneyTemplates.bill(ReminderId("rent"), "Rent", LocalDate(2026, 1, 31), BillCycle.Monthly, Money.parse("15000", Currency.INR), autoPay = false)
    private val call = Reminder(ReminderId("call"), "Call the bank", Schedule(LocalDateTime(2026, 11, 3, 11, 0)), notes = "Ask about the FD")
    private val all = listOf(gym, rent, call)

    @Test
    fun theMonthMatchesTheRecurrenceEngine() {
        val month = Agenda.month(all, emptyList(), 2026, 11, now, zone)
        val from = LocalDate(2026, 11, 1).atStartOfDayIn(zone)
        val until = LocalDate(2026, 12, 1).atStartOfDayIn(zone)
        for (reminder in all) {
            val expected = reminder.schedule.occurrencesBetween(from, until, zone)
            assertEquals(expected, month.values.flatten().filter { it.reminder.id == reminder.id }.map { it.due }, reminder.title)
        }
        assertEquals(LocalDate(2026, 11, 30), month.keys.first { date -> month.getValue(date).any { it.reminder.id == rent.id } }, "the 31st falls on the 30th")
        assertEquals(9, month.values.sumOf { list -> list.count { it.reminder.id == gym.id } })
    }

    @Test
    fun aMonthCarriesWhatWasDone() {
        val due = Instant.parse("2026-11-03T05:30:00Z")
        val month = Agenda.month(all, listOf(Occurrence(call.id, due, OccurrenceStatus.Done(now))), 2026, 11, now, zone)
        assertEquals(true, month.getValue(LocalDate(2026, 11, 3)).first { it.reminder.id == call.id }.isHandled)
        assertEquals(due.toLocalDateTime(zone).date, LocalDate(2026, 11, 3))
    }

    @Test
    fun searchByTextCategoryTypeAndAmount() {
        assertEquals(listOf("call"), ReminderSearch.filter(all, SearchFilter(text = "fd")).map { it.id.value }, "notes count")
        assertEquals(listOf("gym"), ReminderSearch.filter(all, SearchFilter(text = "GYM")).map { it.id.value })
        assertEquals(listOf("gym"), ReminderSearch.filter(all, SearchFilter(categoryId = BuiltInCategory.Health.id)).map { it.id.value })
        assertEquals(listOf("rent"), ReminderSearch.filter(all, SearchFilter(type = ReminderType.Bill)).map { it.id.value })
        assertEquals(listOf("rent"), ReminderSearch.filter(all, SearchFilter(minAmount = Money.parse("10000", Currency.INR))).map { it.id.value })
        assertEquals(emptyList(), ReminderSearch.filter(all, SearchFilter(maxAmount = Money.parse("100", Currency.INR))).map { it.id.value })
        assertEquals(listOf("call", "gym", "rent"), ReminderSearch.filter(all, SearchFilter()).map { it.id.value }, "everything, by title")
        val archived = gym.copy(archived = true)
        assertEquals(listOf("gym"), ReminderSearch.filter(listOf(archived, rent), SearchFilter(status = SearchStatus.Archived)).map { it.id.value })
        assertEquals(listOf("rent"), ReminderSearch.filter(listOf(archived, rent), SearchFilter()).map { it.id.value })
    }
}
