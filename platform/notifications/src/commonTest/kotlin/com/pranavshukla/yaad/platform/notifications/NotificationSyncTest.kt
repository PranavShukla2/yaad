package com.pranavshukla.yaad.platform.notifications

import com.pranavshukla.yaad.core.model.Frequency
import com.pranavshukla.yaad.core.model.Occurrence
import com.pranavshukla.yaad.core.model.OccurrenceStatus
import com.pranavshukla.yaad.core.model.RecurrenceRule
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.model.ReminderId
import com.pranavshukla.yaad.core.model.Schedule
import com.pranavshukla.yaad.core.scheduling.Alert
import com.pranavshukla.yaad.core.scheduling.PlanSettings
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * Like Android: [alarms] is what the system holds (it survives app updates but not reboots); [ledger] is the app's
 * own record of what it scheduled.
 */
private class FakePlatform : PlatformAlerts {
    val alarms = mutableMapOf<String, Alert>()
    val ledger = mutableSetOf<String>()
    var scheduleCalls = 0

    override fun scheduledIds() = ledger.toSet()
    override fun schedule(alert: Alert) {
        scheduleCalls++
        alarms[alert.id] = alert
        ledger += alert.id
    }
    override fun cancel(id: String) {
        alarms.remove(id)
        ledger -= id
    }

    fun reboot() = alarms.clear()
}

class NotificationSyncTest {
    private val zone = TimeZone.of("Asia/Kolkata")
    private val now = Instant.parse("2026-10-28T10:00:00Z")
    private val daily = Reminder(
        ReminderId("walk"),
        "Walk",
        Schedule(LocalDateTime(2026, 10, 28, 18, 0), RecurrenceRule(Frequency.Daily())),
        earlyAlerts = listOf(1.hours),
    )
    private val platform = FakePlatform()
    private val sync = NotificationSync(platform, PlanSettings(horizon = 3.days))

    @Test
    fun schedulesThePlanOnce() {
        sync.sync(listOf(daily), emptyList(), now, zone)
        assertEquals(6, platform.alarms.size, "3 days of an early and a due alert")
        sync.sync(listOf(daily), emptyList(), now, zone)
        assertEquals(6, platform.scheduleCalls, "a second sync schedules nothing new")
    }

    @Test
    fun markingDoneCancelsThatOccurrencesAlerts() {
        sync.sync(listOf(daily), emptyList(), now, zone)
        val done = Occurrence(daily.id, Instant.parse("2026-10-28T12:30:00Z"), OccurrenceStatus.Done(now))
        sync.sync(listOf(daily), listOf(done), now, zone)
        assertEquals(4, platform.alarms.size)
        assertEquals(setOf(Instant.parse("2026-10-29T12:30:00Z"), Instant.parse("2026-10-30T12:30:00Z")),
            platform.alarms.values.map { it.occurrenceDue }.toSet())
    }

    @Test
    fun afterARebootEverythingIsScheduledAgain() {
        sync.sync(listOf(daily), emptyList(), now, zone)
        val before = platform.alarms.keys.toSet()
        platform.reboot()
        sync.resyncFromScratch(listOf(daily), emptyList(), now, zone)
        assertEquals(before, platform.alarms.keys)
    }

    @Test
    fun afterAnAppUpdateNoStaleAlarmIsLeftBehind() {
        // Found on the emulator: alarms survive an update, and forgetting the ledger first orphaned them.
        sync.sync(listOf(daily), emptyList(), now, zone)
        val done = Occurrence(daily.id, Instant.parse("2026-10-28T12:30:00Z"), OccurrenceStatus.Done(now))
        sync.resyncFromScratch(listOf(daily), listOf(done), now, zone)
        assertEquals(platform.ledger, platform.alarms.keys)
        assertEquals(4, platform.alarms.size)
    }

    @Test
    fun deletingAReminderCancelsItsAlerts() {
        sync.sync(listOf(daily), emptyList(), now, zone)
        sync.sync(emptyList(), emptyList(), now, zone)
        assertEquals(emptyMap(), platform.alarms)
    }
}
