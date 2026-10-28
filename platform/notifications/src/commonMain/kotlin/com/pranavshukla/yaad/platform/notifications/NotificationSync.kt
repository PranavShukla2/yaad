package com.pranavshukla.yaad.platform.notifications

import com.pranavshukla.yaad.core.model.Occurrence
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.scheduling.Alert
import com.pranavshukla.yaad.core.scheduling.AlertPlanner
import com.pranavshukla.yaad.core.scheduling.AlertSync
import com.pranavshukla.yaad.core.scheduling.PlanSettings
import com.pranavshukla.yaad.core.scheduling.RollingWindow
import com.pranavshukla.yaad.core.scheduling.Window
import kotlinx.datetime.TimeZone
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/** The platform's scheduled notifications, addressed by [Alert.id]. */
interface PlatformAlerts {
    fun scheduledIds(): Set<String>

    /** [reminder] is the alert's reminder, for platforms that build the notification ahead of time (iOS). */
    fun schedule(alert: Alert, reminder: Reminder)
    fun cancel(id: String)

    /** Shows "open Yaad to keep reminders coming" at [at], or removes that notice when null. */
    fun setRefillNotice(at: Instant?) {}
}

/**
 * Brings the platform's notifications in line with the plan, adding only what is missing. A platform with a
 * [capacity] (iOS: 64) gets the earliest alerts that fit and a refill notice (see [RollingWindow]).
 */
class NotificationSync(
    private val platform: PlatformAlerts,
    private val settings: PlanSettings = PlanSettings(),
    private val capacity: Int? = null,
) {
    fun sync(reminders: List<Reminder>, history: List<Occurrence>, now: Instant, zone: TimeZone) {
        val plan = AlertPlanner.plan(reminders, history, now, zone, settings)
        val window = capacity?.let { RollingWindow.fit(plan, it) } ?: Window(plan, null)
        val byId = reminders.associateBy { it.id }
        val diff = AlertSync.diff(platform.scheduledIds(), window.alerts)
        diff.toCancel.forEach(platform::cancel)
        diff.toSchedule.forEach { alert -> platform.schedule(alert, byId.getValue(alert.reminderId)) }
        platform.setRefillNotice(window.refillAt)
    }

    /**
     * When the system may have dropped alarms (reboot) or kept ones the app no longer expects (app update): cancel
     * everything on record, then schedule the whole plan again.
     *
     * @return the alerts that should have fired since [lastSync] (at most a day back) and could not, for example
     * because the phone was off; the caller shows them now.
     */
    fun resyncFromScratch(
        reminders: List<Reminder>,
        history: List<Occurrence>,
        now: Instant,
        zone: TimeZone,
        lastSync: Instant? = null,
    ): List<Alert> {
        platform.scheduledIds().forEach(platform::cancel)
        sync(reminders, history, now, zone)
        val since = maxOf(lastSync ?: return emptyList(), now - CatchUpWindow)
        return AlertPlanner.missedBetween(reminders, history, since, now, zone, settings)
    }

    private companion object {
        val CatchUpWindow = 24.hours
    }
}
