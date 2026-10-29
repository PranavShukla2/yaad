package com.pranavshukla.yaad.app

import com.pranavshukla.yaad.core.model.Frequency
import com.pranavshukla.yaad.core.model.RecurrenceRule
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.model.ReminderId
import com.pranavshukla.yaad.core.model.Schedule
import com.pranavshukla.yaad.data.DatabaseFactory
import com.pranavshukla.yaad.data.LoanStore
import com.pranavshukla.yaad.data.PreferenceStore
import com.pranavshukla.yaad.data.ReminderStore
import com.pranavshukla.yaad.feature.loans.LoansModel
import com.pranavshukla.yaad.feature.reminders.RemindersModel
import com.pranavshukla.yaad.platform.notifications.IosNotifications
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import platform.Foundation.NSUUID
import platform.Foundation.timeIntervalSince1970
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** The iOS app's one database and notification scheduler, called from Swift (app/ios/Yaad/YaadApp.swift). */
object YaadIos {
    private val driver by lazy { DatabaseFactory().driver() }
    internal val store by lazy { ReminderStore(driver) }
    internal val preferences by lazy { PreferenceStore(driver) }
    internal val backup by lazy { com.pranavshukla.yaad.data.Backup(driver) }
    val notifications by lazy { IosNotifications(store, preferences) }

    /** The reminder screens' state; every change re-plans the notifications. */
    internal val reminders by lazy { RemindersModel(store, MainScope(), onChanged = { notifications.requestSync() }) }
    internal val loans by lazy { LoansModel(LoanStore(driver), store, MainScope(), onChanged = { notifications.requestSync() }) }

    /** True while the app lock is on and Yaad has not been unlocked. */
    internal val locked = kotlinx.coroutines.flow.MutableStateFlow(false)
    private var lockOn = false

    /** Whether the device can verify its owner (Face ID, Touch ID or passcode). */
    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
    internal fun canLock(): Boolean =
        platform.LocalAuthentication.LAContext().canEvaluatePolicy(platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication, null)

    /** The app went to the background: lock it if the app lock is on. */
    fun onBackground() {
        if (lockOn) locked.value = true
    }

    internal fun unlock() {
        if (!canLock()) {
            locked.value = false
            return
        }
        platform.LocalAuthentication.LAContext().evaluatePolicy(
            platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication,
            localizedReason = "Unlock Yaad",
        ) { success, _ -> if (success) MainScope().launch { locked.value = false } }
    }

    /** At launch. [provisional] (debug only) skips the permission prompt, for automated simulator checks. */
    fun start(provisional: Boolean = false) {
        notifications.start(provisional)
        MainScope().launch {
            preferences.settings().collect { settings ->
                lockOn = settings.appLock
                if (!lockOn) locked.value = false
            }
        }
        lockOn = kotlinx.coroutines.runBlocking { preferences.settings().first().appLock }
        locked.value = lockOn
    }

    /** The app came to the foreground: refill the window and recheck permission. */
    fun onBecameActive() {
        notifications.refreshAuthorization()
        notifications.requestSync()
        MainScope().launch {
            lockOn = preferences.settings().first().appLock
            if (lockOn && locked.value) unlock()
        }
    }

    /** Background app refresh: refill the window, then call [done]. */
    fun refresh(done: () -> Unit) = notifications.refresh(done)

    /** Debug builds only: adds a test reminder due in [inSeconds]. */
    fun debugAdd(title: String, inSeconds: Int, nagMinutes: Int, everyMinutes: Int) {
        MainScope().launch {
            val start = (Clock.System.now() + inSeconds.seconds).toLocalDateTime(TimeZone.currentSystemDefault())
            store.save(
                Reminder(
                    id = ReminderId(NSUUID().UUIDString),
                    title = title,
                    schedule = Schedule(start, if (everyMinutes > 0) RecurrenceRule(Frequency.Minutely(everyMinutes)) else null),
                    nagInterval = if (nagMinutes > 0) nagMinutes.minutes else null,
                ),
            )
            notifications.sync()
        }
    }

    /** Debug builds only: acts on the latest delivered notification as if [action] had been tapped. */
    fun debugAct(action: String) {
        UNUserNotificationCenter.currentNotificationCenter().getDeliveredNotificationsWithCompletionHandler { delivered ->
            val latest = delivered.orEmpty().filterIsInstance<UNNotification>().maxByOrNull { it.date.timeIntervalSince1970 }
                ?: return@getDeliveredNotificationsWithCompletionHandler
            notifications.handle(action, latest.request.content.userInfo) { println("YAAD acted $action") }
        }
    }

    /** Debug builds only: prints ids, statuses and notification times (never titles) for simulator checks. */
    fun debugStatus() {
        MainScope().launch {
            notifications.sync()
            println("YAAD history=" + store.allOccurrences().first()
                .map { "${it.reminderId.value}@${it.due}:${it.status::class.simpleName}" })
            val center = UNUserNotificationCenter.currentNotificationCenter()
            center.getPendingNotificationRequestsWithCompletionHandler { pending ->
                val requests = pending.orEmpty().filterIsInstance<UNNotificationRequest>()
                println("YAAD pending=${requests.size} " + requests.map { it.identifier }.sorted())
                center.getDeliveredNotificationsWithCompletionHandler { delivered ->
                    println("YAAD delivered=" + delivered.orEmpty().filterIsInstance<UNNotification>()
                        .map { "${it.request.identifier} at ${it.date}" })
                }
            }
        }
    }
}
