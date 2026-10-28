package com.pranavshukla.yaad.platform.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.scheduling.Alert
import kotlin.time.Instant

/**
 * Alerts as AlarmManager alarms, one per [Alert.id] (the id is the intent's data URI, so setting the same alert
 * again replaces it). The ids scheduled are kept in a small ledger; it holds ids only, never reminder contents.
 */
internal class AndroidAlerts(private val context: Context) : PlatformAlerts {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val ledger = context.getSharedPreferences("yaad.alerts", Context.MODE_PRIVATE)

    /** Exact alarms need the user's permission from Android 12; without it, alarms may be a few minutes late. */
    fun exactAllowed(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()

    override fun scheduledIds(): Set<String> = ledger.getStringSet(Ids, emptySet()).orEmpty().toSet()

    override fun schedule(alert: Alert, reminder: Reminder) {
        val intent = intentFor(alert.id)
            .putExtra(ExtraReminderId, alert.reminderId.value)
            .putExtra(ExtraDue, alert.occurrenceDue.toEpochMilliseconds())
            .putExtra(ExtraKind, alert.kind.name)
        val pending = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val at = alert.fireAt.toEpochMilliseconds()
        if (exactAllowed()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
        saveIds(scheduledIds() + alert.id)
    }

    override fun cancel(id: String) {
        PendingIntent.getBroadcast(context, 0, intentFor(id), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            ?.let { alarms.cancel(it); it.cancel() }
        forget(id)
    }

    /** When the alarms were last brought in line with the plan; alerts due after it may have been missed. */
    fun lastSync(): Instant? = ledger.getLong(LastSync, 0L).takeIf { it > 0 }?.let(Instant::fromEpochMilliseconds)

    fun recordSync(at: Instant) {
        ledger.edit().putLong(LastSync, at.toEpochMilliseconds()).commit()
    }

    /** An alarm went off: it is no longer pending. */
    fun forget(id: String) = saveIds(scheduledIds() - id)

    // Runs on a background thread, so commit: the ledger must be on disk before the process can be killed.
    private fun saveIds(ids: Set<String>) {
        ledger.edit().putStringSet(Ids, ids).commit()
    }

    /** Re-plans twice a day, so alerts beyond the planning horizon are added in time even if nothing else happens. */
    fun scheduleRefresh() {
        val intent = Intent(context, RescheduleReceiver::class.java).setAction(RescheduleReceiver.ActionRefresh)
        val pending = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarms.setInexactRepeating(
            AlarmManager.RTC,
            System.currentTimeMillis() + AlarmManager.INTERVAL_HALF_DAY,
            AlarmManager.INTERVAL_HALF_DAY,
            pending,
        )
    }

    private fun intentFor(id: String) =
        Intent(context, AlertReceiver::class.java).setData(Uri.parse("yaad://alert/" + Uri.encode(id)))

    companion object {
        private const val Ids = "ids"
        private const val LastSync = "lastSync"
        const val ExtraReminderId = "reminderId"
        const val ExtraDue = "due"
        const val ExtraKind = "kind"
        const val ExtraAction = "action"

        fun alertId(intent: Intent): String? = intent.data?.lastPathSegment
    }
}
