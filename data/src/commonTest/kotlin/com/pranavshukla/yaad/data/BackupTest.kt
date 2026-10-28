package com.pranavshukla.yaad.data

import com.pranavshukla.yaad.core.model.Category
import com.pranavshukla.yaad.core.model.ChecklistItem
import com.pranavshukla.yaad.core.model.Currency
import com.pranavshukla.yaad.core.model.Frequency
import com.pranavshukla.yaad.core.model.Money
import com.pranavshukla.yaad.core.model.MonthlyOn
import com.pranavshukla.yaad.core.model.Occurrence
import com.pranavshukla.yaad.core.model.OccurrenceStatus
import com.pranavshukla.yaad.core.model.RecurrenceEnd
import com.pranavshukla.yaad.core.model.RecurrenceRule
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.model.ReminderId
import com.pranavshukla.yaad.core.model.ReminderType
import com.pranavshukla.yaad.core.model.Schedule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class BackupTest {
    private val now = Instant.parse("2026-10-28T10:00:00Z")
    private val clock = object : Clock {
        override fun now() = now
    }

    private val emi = Reminder(
        id = ReminderId("emi-1"),
        title = "Home loan EMI",
        type = ReminderType.Emi,
        schedule = Schedule(
            LocalDateTime(2026, 11, 5, 9, 0),
            RecurrenceRule(Frequency.Monthly(on = MonthlyOn.Day(5)), RecurrenceEnd.AfterCount(240)),
        ),
        earlyAlerts = listOf(3.days),
        amount = Money(867_823, Currency.INR),
        checklist = listOf(ChecklistItem("Pay", done = true)),
    )
    private val birthday = Reminder(
        ReminderId("bday-1"),
        "Asha's birthday",
        Schedule(LocalDateTime(2027, 2, 28, 8, 0), RecurrenceRule(Frequency.Yearly())),
        type = ReminderType.Birthday,
    )
    private val paid = Occurrence(emi.id, Instant.parse("2026-11-05T03:30:00Z"), OccurrenceStatus.Done(now), emi.amount)

    private class Device(val reminders: ReminderStore, val preferences: PreferenceStore, val backup: Backup)

    private fun device(dispatcher: TestDispatcher): Device {
        val driver = testDriver()
        return Device(ReminderStore(driver, clock, dispatcher), PreferenceStore(driver, dispatcher), Backup(driver, clock))
    }

    private fun Device.fill() {
        reminders.save(emi)
        reminders.save(birthday)
        reminders.record(paid)
        preferences.saveCategory(Category("gym", "Gym", 0xFF0F6A73))
        preferences.putSetting("currency", "INR")
    }

    @Test
    fun exportThenImportOnANewDeviceRestoresEverything() = runTest {
        val old = device(StandardTestDispatcher(testScheduler)).apply { fill() }
        val file = old.backup.export()

        val new = device(StandardTestDispatcher(testScheduler))
        new.backup.import(file)

        assertEquals(old.reminders.reminders().first(), new.reminders.reminders().first())
        assertEquals(listOf(paid), new.reminders.allOccurrences().first())
        assertEquals(listOf(Category("gym", "Gym", 0xFF0F6A73)), new.preferences.categories().first())
        assertEquals("INR", new.preferences.setting("currency"))
        assertEquals(file, new.backup.export(), "a second export is identical")
    }

    @Test
    fun importReplacesWhatWasThere() = runTest {
        val source = device(StandardTestDispatcher(testScheduler)).apply { reminders.save(birthday) }
        val target = device(StandardTestDispatcher(testScheduler)).apply { fill() }
        target.backup.import(source.backup.export())
        assertEquals(listOf(birthday), target.reminders.reminders().first())
        assertEquals(emptyList(), target.preferences.categories().first())
    }

    @Test
    fun aBadFileChangesNothing() = runTest {
        val target = device(StandardTestDispatcher(testScheduler)).apply { fill() }
        val good = device(StandardTestDispatcher(testScheduler)).apply { reminders.save(birthday) }.backup.export()
        val bad = listOf(
            "not json",
            """{"format":"something-else","version":1}""",
            good.replace("\"version\":1", "\"version\":99"),
            good.replace("FREQ=YEARLY", "FREQ=SOMETIMES"),
            good.replace("\"Birthday\"", "\"Party\""),
            good.replace(
                "\"occurrences\":[]",
                "\"occurrences\":[{\"reminderId\":\"ghost\",\"dueAt\":0,\"status\":\"done\",\"statusAt\":0," +
                    "\"paidMinor\":null,\"paidCurrency\":null,\"paidMinorDigits\":null,\"note\":\"\"}]",
            ),
        )
        for (file in bad) {
            assertFailsWith<BackupException>(file.take(60)) { target.backup.import(file) }
            assertEquals(listOf(emi, birthday), target.reminders.reminders().first())
        }
    }

    @Test
    fun theFileSaysWhatItIs() = runTest {
        val file = device(StandardTestDispatcher(testScheduler)).apply { fill() }.backup.export()
        assertTrue(file.startsWith("""{"format":"yaad-backup","version":1,"exportedAt":"2026-10-28T10:00:00Z""""), file)
    }
}
