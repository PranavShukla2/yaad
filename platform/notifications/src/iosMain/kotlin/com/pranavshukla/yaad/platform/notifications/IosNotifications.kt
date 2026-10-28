package com.pranavshukla.yaad.platform.notifications

import com.pranavshukla.yaad.core.model.ReminderId
import com.pranavshukla.yaad.core.scheduling.Alert
import com.pranavshukla.yaad.core.scheduling.AlertKind
import com.pranavshukla.yaad.core.scheduling.PlanSettings
import com.pranavshukla.yaad.data.ReminderStore
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSSystemTimeZoneDidChangeNotification
import platform.UIKit.UIApplicationSignificantTimeChangeNotification
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionProvisional
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationAction
import platform.UserNotifications.UNNotificationActionOptionNone
import platform.UserNotifications.UNNotificationCategory
import platform.UserNotifications.UNNotificationCategoryOptionNone
import platform.UserNotifications.UNNotificationPresentationOptionBanner
import platform.UserNotifications.UNNotificationPresentationOptionList
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Keeps iOS's pending notifications in line with the database: the next 64 alerts (a rolling window, refilled when
 * the app opens, on background refresh and after every change), Done / Snooze / Skip actions, and re-planning when
 * the clock or time zone changes.
 */
class IosNotifications(private val store: ReminderStore, private val clock: Clock = Clock.System) {
    private val center = UNUserNotificationCenter.currentNotificationCenter()
    private val alerts = IosAlerts()
    private val settings = PlanSettings()
    private val sync = NotificationSync(alerts, settings, capacity = 64)
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val delegate = Delegate() // the center holds its delegate weakly
    private val authorized = MutableStateFlow(true)

    /** Whether notifications are allowed (full or provisional); the app explains it when they are not. */
    val notificationsAllowed: StateFlow<Boolean> get() = authorized

    /** Call once at launch, before the app finishes launching, so actions tapped while it was closed arrive. */
    fun start(provisional: Boolean = false) {
        center.delegate = delegate
        center.setNotificationCategories(setOf(category()))
        var options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        if (provisional) options = options or UNAuthorizationOptionProvisional
        center.requestAuthorizationWithOptions(options) { _, _ -> refreshAuthorization() }
        for (name in listOf(UIApplicationSignificantTimeChangeNotification, NSSystemTimeZoneDidChangeNotification)) {
            NSNotificationCenter.defaultCenter.addObserverForName(name, null, null) { _ -> requestSync() }
        }
        requestSync()
    }

    fun refreshAuthorization() {
        center.getNotificationSettingsWithCompletionHandler { settings ->
            val status = settings?.authorizationStatus
            authorized.value = status == UNAuthorizationStatusAuthorized || status == UNAuthorizationStatusProvisional ||
                status == UNAuthorizationStatusEphemeral
        }
    }

    fun requestSync(): Job = scope.launch { sync() }

    /** For background refresh: re-plans, then calls [done]. */
    fun refresh(done: () -> Unit) {
        scope.launch {
            try {
                sync()
            } finally {
                done()
            }
        }
    }

    suspend fun sync() = mutex.withLock {
        val reminders = store.reminders().first()
        val history = store.allOccurrences().first()
        sync.sync(reminders, history, clock.now(), TimeZone.currentSystemDefault())
    }

    /**
     * Handles a notification action ([actionIdentifier] is an [AlertAction] name) for the notification carrying
     * [userInfo], then calls [done]. Other identifiers (opening the notification) only call [done].
     */
    fun handle(actionIdentifier: String, userInfo: Map<Any?, *>, done: () -> Unit) {
        val action = runCatching { AlertAction.valueOf(actionIdentifier) }.getOrNull()
        val reminderId = userInfo[IosAlerts.KeyReminderId] as? String
        val due = (userInfo[IosAlerts.KeyDue] as? String)?.toLongOrNull()
        if (action == null || reminderId == null || due == null) {
            done()
            return
        }
        scope.launch {
            try {
                onAction(action, ReminderId(reminderId), Instant.fromEpochMilliseconds(due))
            } finally {
                done()
            }
        }
    }

    private suspend fun onAction(action: AlertAction, reminderId: ReminderId, due: Instant) {
        store.record(action.toOccurrence(reminderId, due, clock.now(), TimeZone.currentSystemDefault(), settings.allDayTime))
        center.removeDeliveredNotificationsWithIdentifiers(deliveredIdsFor(Alert(reminderId, due, due, AlertKind.Due)))
        sync()
    }

    /** The delivered notifications of one occurrence, so handling it clears all of them. */
    private suspend fun deliveredIdsFor(alert: Alert): List<String> {
        val delivered = MutableStateFlow<List<String>?>(null)
        center.getDeliveredNotificationsWithCompletionHandler { list ->
            delivered.value = list.orEmpty().filterIsInstance<UNNotification>()
                .filter { it.request.content.threadIdentifier == alert.occurrenceKey }
                .map { it.request.identifier }
        }
        return delivered.first { it != null }!!
    }

    private fun category(): UNNotificationCategory {
        fun action(action: AlertAction, key: String, english: String) =
            UNNotificationAction.actionWithIdentifier(action.name, IosAlerts.text(key, english), UNNotificationActionOptionNone)
        return UNNotificationCategory.categoryWithIdentifier(
            IosAlerts.Category,
            actions = listOf(
                action(AlertAction.Done, "action_done", "Done"),
                action(AlertAction.Snooze10Minutes, "action_snooze", "Snooze 10 min"),
                action(AlertAction.Skip, "action_skip", "Skip"),
            ),
            intentIdentifiers = emptyList<Any>(),
            options = UNNotificationCategoryOptionNone,
        )
    }

    private inner class Delegate : NSObject(), UNUserNotificationCenterDelegateProtocol {
        /** A notification action was tapped, with or without opening the app. */
        @ObjCSignatureOverride
        override fun userNotificationCenter(
            center: UNUserNotificationCenter,
            didReceiveNotificationResponse: UNNotificationResponse,
            withCompletionHandler: () -> Unit,
        ) {
            handle(
                didReceiveNotificationResponse.actionIdentifier,
                didReceiveNotificationResponse.notification.request.content.userInfo,
                withCompletionHandler,
            )
        }

        /** Show reminders as banners even while Yaad is open. */
        @ObjCSignatureOverride
        override fun userNotificationCenter(
            center: UNUserNotificationCenter,
            willPresentNotification: UNNotification,
            withCompletionHandler: (UNNotificationPresentationOptions) -> Unit,
        ) {
            withCompletionHandler(
                UNNotificationPresentationOptionBanner or UNNotificationPresentationOptionList or
                    UNNotificationPresentationOptionSound,
            )
        }
    }
}
