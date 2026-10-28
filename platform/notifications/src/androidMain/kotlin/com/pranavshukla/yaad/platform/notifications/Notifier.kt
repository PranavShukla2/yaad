package com.pranavshukla.yaad.platform.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.text.format.DateUtils
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.pranavshukla.yaad.core.model.BuiltInCategory
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.scheduling.AlertKind
import kotlin.time.Instant

/**
 * Shows reminders as notifications: one channel per built-in category, so each can have its own sound, and one
 * notification per occurrence (tagged with its key), so early, due and nag alerts replace each other.
 */
internal class Notifier(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    fun createChannels() {
        val system = context.getSystemService(NotificationManager::class.java)
        val channels = listOf(General to R.string.channel_general) + BuiltInCategory.entries.map { channelId(it.id) to nameOf(it) }
        system.createNotificationChannels(
            channels.map { (id, name) ->
                NotificationChannel(id, context.getString(name), NotificationManager.IMPORTANCE_HIGH)
            },
        )
    }

    fun canPost(): Boolean = manager.areNotificationsEnabled() &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun show(reminder: Reminder, due: Instant, kind: AlertKind, key: String) {
        if (!canPost()) return
        val whenText = DateUtils.formatDateTime(
            context,
            due.toEpochMilliseconds(),
            if (reminder.allDay) DateUtils.FORMAT_SHOW_DATE else DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE,
        )
        val text = context.getString(
            when (kind) {
                AlertKind.Early -> R.string.due_at
                AlertKind.Due -> R.string.due_now
                AlertKind.Nag -> R.string.still_due
                AlertKind.Snoozed -> R.string.snoozed
            },
            whenText,
        )
        val notification = NotificationCompat.Builder(context, channelFor(reminder))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(reminder.title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setWhen(due.toEpochMilliseconds())
            .setShowWhen(true)
            .setAutoCancel(true)
            .setOnlyAlertOnce(kind == AlertKind.Nag)
            .setContentIntent(openApp())
            .addAction(0, context.getString(R.string.action_done), action(AlertAction.Done, reminder, due, key))
            .addAction(0, context.getString(R.string.action_snooze), action(AlertAction.Snooze10Minutes, reminder, due, key))
            .addAction(0, context.getString(R.string.action_skip), action(AlertAction.Skip, reminder, due, key))
            .build()
        @Suppress("MissingPermission") // checked by canPost()
        manager.notify(key, NotificationId, notification)
    }

    fun cancel(key: String) = manager.cancel(key, NotificationId)

    private fun channelFor(reminder: Reminder): String =
        BuiltInCategory.entries.firstOrNull { it.id == reminder.categoryId }?.let { channelId(it.id) } ?: General

    private fun openApp(): PendingIntent? = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
        PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun action(action: AlertAction, reminder: Reminder, due: Instant, key: String): PendingIntent {
        val intent = Intent(context, ActionReceiver::class.java)
            .setData(Uri.parse("yaad://action/${action.name}/" + Uri.encode(key)))
            .putExtra(AndroidAlerts.ExtraAction, action.name)
            .putExtra(AndroidAlerts.ExtraReminderId, reminder.id.value)
            .putExtra(AndroidAlerts.ExtraDue, due.toEpochMilliseconds())
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun nameOf(category: BuiltInCategory) = when (category) {
        BuiltInCategory.Money -> R.string.channel_money
        BuiltInCategory.Health -> R.string.channel_health
        BuiltInCategory.Personal -> R.string.channel_personal
        BuiltInCategory.Work -> R.string.channel_work
        BuiltInCategory.Home -> R.string.channel_home
        BuiltInCategory.Vehicle -> R.string.channel_vehicle
    }

    private companion object {
        const val General = "yaad.general"
        const val NotificationId = 1

        fun channelId(category: String) = "yaad.$category"
    }
}
