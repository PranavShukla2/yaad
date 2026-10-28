package com.pranavshukla.yaad.android

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.pranavshukla.yaad.data.DatabaseFactory
import com.pranavshukla.yaad.feature.reminders.RemindersModel
import com.pranavshukla.yaad.data.ReminderStore
import com.pranavshukla.yaad.platform.notifications.AndroidNotifications
import com.pranavshukla.yaad.platform.notifications.YaadHost

/** Opens the database once per process and keeps alarms in line with it. */
class YaadApplication : Application(), YaadHost {
    val store by lazy { ReminderStore(DatabaseFactory(this).driver()) }

    override val notifications by lazy { AndroidNotifications(this, store) }

    /** The reminder screens' state; every change re-plans the alarms. */
    val reminders by lazy {
        RemindersModel(store, CoroutineScope(SupervisorJob() + Dispatchers.Main), onChanged = { notifications.requestSync() })
    }

    override fun onCreate() {
        super.onCreate()
        // The system may have dropped alarms while the process was gone (for example, when the exact-alarm
        // permission was revoked), so schedule everything again; re-adding an alarm replaces it.
        notifications.requestSync(fromScratch = true)
    }
}
