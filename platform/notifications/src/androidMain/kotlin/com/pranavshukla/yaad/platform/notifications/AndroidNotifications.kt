package com.pranavshukla.yaad.platform.notifications

import android.content.BroadcastReceiver
import android.content.Context
import com.pranavshukla.yaad.core.model.OccurrenceStatus
import com.pranavshukla.yaad.core.model.ReminderId
import com.pranavshukla.yaad.core.scheduling.Alert
import com.pranavshukla.yaad.core.scheduling.AlertKind
import com.pranavshukla.yaad.core.scheduling.PlanSettings
import com.pranavshukla.yaad.data.ReminderStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Instant

/** The application provides this, so the receivers can reach the one store and scheduler of the process. */
interface YaadHost {
    val notifications: AndroidNotifications
}

/**
 * Keeps Android's alarms and notifications in line with the database. Syncs run one at a time on a background
 * thread; every change to reminders or their history should be followed by [requestSync].
 */
class AndroidNotifications(
    context: Context,
    private val store: ReminderStore,
    private val clock: Clock = Clock.System,
) {
    private val settings = PlanSettings(limit = 400) // Android allows each app 500 alarms.
    private val alerts = AndroidAlerts(context)
    private val notifier = Notifier(context)
    private val sync = NotificationSync(alerts, settings)
    private val mutex = Mutex()
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        notifier.createChannels()
    }

    val exactAlarmsAllowed: Boolean get() = alerts.exactAllowed()
    val notificationsAllowed: Boolean get() = notifier.canPost()

    /** Re-plans in the background. [fromScratch] after the system may have dropped alarms (reboot, app start). */
    fun requestSync(fromScratch: Boolean = false): Job = scope.launch { sync(fromScratch) }

    suspend fun sync(fromScratch: Boolean = false) = mutex.withLock {
        val reminders = store.reminders().first()
        val history = store.allOccurrences().first()
        val zone = TimeZone.currentSystemDefault()
        val now = clock.now()
        if (fromScratch) {
            val missed = sync.resyncFromScratch(reminders, history, now, zone, alerts.lastSync())
            val byId = reminders.associateBy { it.id }
            missed.forEach { alert ->
                byId[alert.reminderId]?.let { notifier.show(it, alert.occurrenceDue, alert.kind, alert.occurrenceKey) }
            }
        } else {
            sync.sync(reminders, history, now, zone)
        }
        alerts.recordSync(now)
        alerts.scheduleRefresh()
    }

    /** An alarm went off: show it unless the occurrence was handled meanwhile, then plan what comes next. */
    internal suspend fun onAlert(alertId: String, reminderId: ReminderId, due: Instant, kind: AlertKind) {
        alerts.forget(alertId)
        val reminder = store.reminder(reminderId)
        val status = store.occurrences(reminderId).firstOrNull { it.due == due }?.status
        if (reminder != null && !reminder.archived && status !is OccurrenceStatus.Done && status !is OccurrenceStatus.Skipped) {
            notifier.show(reminder, due, kind, Alert(reminderId, due, due, kind).occurrenceKey)
        }
        sync()
    }

    /** Done, Snooze or Skip from a notification: record it, clear the notification, and re-plan. */
    internal suspend fun onAction(action: AlertAction, reminderId: ReminderId, due: Instant) {
        store.record(action.toOccurrence(reminderId, due, clock.now(), TimeZone.currentSystemDefault(), settings.allDayTime))
        notifier.cancel(Alert(reminderId, due, due, AlertKind.Due).occurrenceKey)
        sync()
    }
}

/** Runs [block] on the host's scope while keeping the receiver alive, as `goAsync` allows. */
internal fun BroadcastReceiver.runAsync(context: Context, block: suspend (AndroidNotifications) -> Unit) {
    val notifications = (context.applicationContext as YaadHost).notifications
    val pending = goAsync()
    notifications.scope.launch {
        try {
            block(notifications)
        } finally {
            pending.finish()
        }
    }
}
