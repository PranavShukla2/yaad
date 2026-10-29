package com.pranavshukla.yaad.data

import com.pranavshukla.yaad.core.model.ChecklistItem
import com.pranavshukla.yaad.core.model.Currency
import com.pranavshukla.yaad.core.model.Frequency
import com.pranavshukla.yaad.core.model.Money
import com.pranavshukla.yaad.core.model.MonthlyOn
import com.pranavshukla.yaad.core.model.Occurrence
import com.pranavshukla.yaad.core.model.OccurrenceStatus
import com.pranavshukla.yaad.core.model.Priority
import com.pranavshukla.yaad.core.model.RecurrenceEnd
import com.pranavshukla.yaad.core.model.RecurrenceRule
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.model.ReminderId
import com.pranavshukla.yaad.core.model.ReminderType
import com.pranavshukla.yaad.core.model.Schedule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class ReminderStoreTest {
    private val now = Instant.parse("2026-10-28T10:00:00Z")
    private val clock = object : Clock {
        override fun now() = now
    }

    private fun store() = ReminderStore(testDriver(), clock, StandardTestDispatcher())

    private val emi = Reminder(
        id = ReminderId("emi-1"),
        title = "Home loan EMI",
        notes = "HDFC, account ending 4321",
        type = ReminderType.Emi,
        categoryId = "money",
        schedule = Schedule(
            LocalDateTime(2026, 11, 5, 9, 0),
            RecurrenceRule(Frequency.Monthly(on = MonthlyOn.Day(5)), RecurrenceEnd.AfterCount(240)),
            timeZoneId = "Asia/Kolkata",
        ),
        allDay = true,
        earlyAlerts = listOf(3.days, 1.hours),
        priority = Priority.High,
        nagInterval = 30.minutes,
        amount = Money(867_823, Currency.INR),
        autoPay = true,
        linkedLoanId = "loan-1",
        checklist = listOf(ChecklistItem("Check balance"), ChecklistItem("Pay", done = true)),
    )

    private val call = Reminder(ReminderId("call-1"), "Call mom", Schedule(LocalDateTime(2026, 10, 29, 19, 0)))

    @Test
    fun everyFieldRoundTrips() {
        val store = store()
        store.save(emi)
        store.save(call)
        assertEquals(emi, store.reminder(emi.id))
        assertEquals(call, store.reminder(call.id))
        assertNull(store.reminder(ReminderId("missing")))
    }

    @Test
    fun theListFollowsSavesAndDeletes() = runTest {
        val store = ReminderStore(testDriver(), clock, StandardTestDispatcher(testScheduler))
        store.save(emi)
        store.save(call)
        assertEquals(listOf(call, emi), store.reminders().first(), "ordered by first due date")
        store.delete(call.id)
        assertEquals(listOf(emi), store.reminders().first())
    }

    @Test
    fun savingAgainKeepsHistoryAndReplacesTheChecklist() {
        val store = store()
        store.save(emi)
        val paid = Occurrence(emi.id, Instant.parse("2026-11-05T03:30:00Z"), OccurrenceStatus.Done(now), emi.amount, dueLocal = LocalDateTime(2026, 11, 5, 9, 0))
        store.record(paid)
        val edited = emi.copy(title = "Home loan", checklist = listOf(ChecklistItem("Only this")))
        store.save(edited)
        assertEquals(edited, store.reminder(emi.id))
        assertEquals(listOf(paid), store.occurrences(emi.id))
    }

    @Test
    fun occurrencesAreRecordedReplacedAndCleared() {
        val store = store()
        store.save(call)
        val due = Instant.parse("2026-10-29T13:30:00Z")
        store.record(Occurrence(call.id, due, OccurrenceStatus.Snoozed(due + 10.minutes)))
        store.record(Occurrence(call.id, due, OccurrenceStatus.Skipped(now), note = "busy"))
        assertEquals(listOf(Occurrence(call.id, due, OccurrenceStatus.Skipped(now), note = "busy")), store.occurrences(call.id))
        store.record(Occurrence(call.id, due))
        assertEquals(emptyList(), store.occurrences(call.id))
    }

    @Test
    fun deletingAReminderDeletesItsHistory() = runTest {
        val store = ReminderStore(testDriver(), clock, StandardTestDispatcher(testScheduler))
        store.save(emi)
        store.record(Occurrence(emi.id, now, OccurrenceStatus.Done(now)))
        store.delete(emi.id)
        assertEquals(emptyList(), store.allOccurrences().first())
    }

    @Test
    fun anOccurrenceNeedsItsReminder() {
        val store = store()
        assertFails { store.record(Occurrence(ReminderId("ghost"), now, OccurrenceStatus.Done(now))) }
    }
}
