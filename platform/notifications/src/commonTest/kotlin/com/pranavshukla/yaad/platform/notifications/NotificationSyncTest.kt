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

/** Records what the platform was asked to do, like AlarmManager or UNUserNotificationCenter would. */
private class FakePlatform : PlatformAlerts {
    val pending = mutableMapOf<String, Alert>()
    var scheduleCalls = 0

    override fun scheduledIds() = pending.keys.toSet()
    override fun schedule(alert: Alert) {
        scheduleCalls++
        pending[alert.id] = alert
    }
    override fun cancel(id: String) {
        pending.remove(id)
    }
    override fun forgetAll() = pending.clear()
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
        assertEquals(6, platform.pending.size, "3 days of an early and a due alert")
        sync.sync(listOf(daily), emptyList(), now, zone)
        assertEquals(6, platform.scheduleCalls, "a second sync schedules nothing new")
    }

    @Test
    fun markingDoneCancelsThatOccurrencesAlerts() {
        sync.sync(listOf(daily), emptyList(), now, zone)
        val done = Occurrence(daily.id, Instant.parse("2026-10-28T12:30:00Z"), OccurrenceStatus.Done(now))
        sync.sync(listOf(daily), listOf(done), now, zone)
        assertEquals(4, platform.pending.size)
        assertEquals(setOf(Instant.parse("2026-10-29T12:30:00Z"), Instant.parse("2026-10-30T12:30:00Z")),
            platform.pending.values.map { it.occurrenceDue }.toSet())
    }

    @Test
    fun afterARebootEverythingIsScheduledAgainWithoutDuplicates() {
        sync.sync(listOf(daily), emptyList(), now, zone)
        val before = platform.pending.keys.toSet()
        sync.resyncFromScratch(listOf(daily), emptyList(), now, zone)
        assertEquals(before, platform.pending.keys)
    }

    @Test
    fun deletingAReminderCancelsItsAlerts() {
        sync.sync(listOf(daily), emptyList(), now, zone)
        sync.sync(emptyList(), emptyList(), now, zone)
        assertEquals(emptyMap(), platform.pending)
    }
}
