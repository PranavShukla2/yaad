package com.pranavshukla.yaad.platform.notifications

import com.pranavshukla.yaad.core.model.Occurrence
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.scheduling.Alert
import com.pranavshukla.yaad.core.scheduling.AlertPlanner
import com.pranavshukla.yaad.core.scheduling.AlertSync
import com.pranavshukla.yaad.core.scheduling.PlanSettings
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

/** The platform's scheduled notifications, addressed by [Alert.id]. */
interface PlatformAlerts {
    fun scheduledIds(): Set<String>
    fun schedule(alert: Alert)
    fun cancel(id: String)
}

/** Brings the platform's notifications in line with the plan, adding only what is missing. */
class NotificationSync(private val platform: PlatformAlerts, private val settings: PlanSettings = PlanSettings()) {
    fun sync(reminders: List<Reminder>, history: List<Occurrence>, now: Instant, zone: TimeZone) {
        val plan = AlertPlanner.plan(reminders, history, now, zone, settings)
        val diff = AlertSync.diff(platform.scheduledIds(), plan)
        diff.toCancel.forEach(platform::cancel)
        diff.toSchedule.forEach(platform::schedule)
    }

    /**
     * When the system may have dropped alarms (reboot) or kept ones the app no longer expects (app update): cancel
     * everything on record, then schedule the whole plan again.
     */
    fun resyncFromScratch(reminders: List<Reminder>, history: List<Occurrence>, now: Instant, zone: TimeZone) {
        platform.scheduledIds().forEach(platform::cancel)
        sync(reminders, history, now, zone)
    }
}
