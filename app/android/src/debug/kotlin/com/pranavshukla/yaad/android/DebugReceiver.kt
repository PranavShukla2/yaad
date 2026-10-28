package com.pranavshukla.yaad.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.pranavshukla.yaad.core.model.Frequency
import com.pranavshukla.yaad.core.model.RecurrenceRule
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.model.ReminderId
import com.pranavshukla.yaad.core.model.Schedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * adb shell am broadcast -n com.pranavshukla.yaad/.android.DebugReceiver -a add --es title Test --ei inSeconds 90
 *     [--ei everyMinutes 5] [--ei nagMinutes 2] [--ei earlySeconds 60]
 * adb shell am broadcast -n com.pranavshukla.yaad/.android.DebugReceiver -a status   (logs ids and statuses only)
 * adb shell am broadcast -n com.pranavshukla.yaad/.android.DebugReceiver -a clear
 */
class DebugReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as YaadApplication
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    "add" -> add(app, intent)
                    "clear" -> app.store.reminders().first().forEach { app.store.delete(it.id) }
                }
                app.notifications.sync()
                val reminders = app.store.reminders().first()
                val history = app.store.allOccurrences().first()
                Log.i(Tag, "reminders=${reminders.map { it.id.value }} history=${history.map { "${it.reminderId.value}@${it.due}:${it.status::class.simpleName}" }}")
                Log.i(Tag, "scheduled=${context.getSharedPreferences("yaad.alerts", Context.MODE_PRIVATE).getStringSet("ids", emptySet())}")
            } finally {
                pending.finish()
            }
        }
    }

    private fun add(app: YaadApplication, intent: Intent) {
        val start = (Clock.System.now() + intent.getIntExtra("inSeconds", 60).seconds)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        val every = intent.getIntExtra("everyMinutes", 0)
        val nag = intent.getIntExtra("nagMinutes", 0)
        val early = intent.getIntExtra("earlySeconds", 0)
        app.store.save(
            Reminder(
                id = ReminderId(UUID.randomUUID().toString()),
                title = intent.getStringExtra("title") ?: "Test reminder",
                schedule = Schedule(start, if (every > 0) RecurrenceRule(Frequency.Minutely(every)) else null),
                nagInterval = if (nag > 0) nag.minutes else null,
                earlyAlerts = if (early > 0) listOf(early.seconds) else emptyList(),
            ),
        )
    }

    private companion object {
        const val Tag = "YaadDebug"
    }
}
