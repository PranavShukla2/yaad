package com.pranavshukla.yaad.android

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.pranavshukla.yaad.data.DatabaseFactory
import com.pranavshukla.yaad.data.LoanStore
import com.pranavshukla.yaad.feature.loans.LoansModel
import com.pranavshukla.yaad.feature.reminders.RemindersModel
import com.pranavshukla.yaad.data.ReminderStore
import com.pranavshukla.yaad.platform.notifications.AndroidNotifications
import com.pranavshukla.yaad.platform.notifications.YaadHost

/** Opens the database once per process and keeps alarms in line with it. */
class YaadApplication : Application(), YaadHost {
    val store by lazy { ReminderStore(driver) }

    override val notifications by lazy { AndroidNotifications(this, store) }

    private val driver by lazy { DatabaseFactory(this).driver() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** The screens' state; every change re-plans the alarms. */
    val reminders by lazy { RemindersModel(store, scope, onChanged = { notifications.requestSync() }) }
    val loans by lazy { LoansModel(LoanStore(driver), store, scope, onChanged = { notifications.requestSync() }) }

    override fun onCreate() {
        super.onCreate()
        // The system may have dropped alarms while the process was gone (for example, when the exact-alarm
        // permission was revoked), so schedule everything again; re-adding an alarm replaces it.
        notifications.requestSync(fromScratch = true)
    }
}
