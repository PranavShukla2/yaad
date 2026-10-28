package com.pranavshukla.yaad.platform.notifications

import com.pranavshukla.yaad.core.scheduling.AlertAction
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pranavshukla.yaad.core.model.ReminderId
import com.pranavshukla.yaad.core.scheduling.AlertKind
import kotlin.time.Instant

/** An alert's alarm went off. */
class AlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = AndroidAlerts.alertId(intent) ?: return
        val reminderId = intent.getStringExtra(AndroidAlerts.ExtraReminderId) ?: return
        val due = Instant.fromEpochMilliseconds(intent.getLongExtra(AndroidAlerts.ExtraDue, 0))
        val kind = intent.getStringExtra(AndroidAlerts.ExtraKind)?.let { runCatching { AlertKind.valueOf(it) }.getOrNull() } ?: return
        runAsync(context) { it.onAlert(id, ReminderId(reminderId), due, kind) }
    }
}

/** Done, Snooze or Skip tapped on a notification, without opening the app. */
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getStringExtra(AndroidAlerts.ExtraAction)?.let { runCatching { AlertAction.valueOf(it) }.getOrNull() } ?: return
        val reminderId = intent.getStringExtra(AndroidAlerts.ExtraReminderId) ?: return
        val due = Instant.fromEpochMilliseconds(intent.getLongExtra(AndroidAlerts.ExtraDue, 0))
        runAsync(context) { it.onAction(action, ReminderId(reminderId), due) }
    }
}

/**
 * Re-plans after anything that can drop or shift alarms: a reboot or app update (alarms are gone), a clock or time
 * zone change (times move), the exact-alarm permission changing, and the twice-daily refresh.
 */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val fromScratch = intent.action in setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )
        runAsync(context) { it.sync(fromScratch) }
    }

    companion object {
        const val ActionRefresh = "com.pranavshukla.yaad.REFRESH_ALERTS"
    }
}
