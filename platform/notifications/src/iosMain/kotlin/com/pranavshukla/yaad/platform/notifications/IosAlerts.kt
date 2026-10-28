package com.pranavshukla.yaad.platform.notifications

import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.scheduling.Alert
import com.pranavshukla.yaad.core.scheduling.AlertKind
import platform.Foundation.NSBundle
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitSecond
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSTimeZone
import platform.Foundation.NSUserDefaults
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeZoneWithName
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.time.Instant

/**
 * Alerts as pending local notifications, one request per [Alert.id] (adding the same id again replaces it). iOS keeps
 * them across reboots and updates. The ids are kept in a small ledger, never reminder contents.
 */
internal class IosAlerts : PlatformAlerts {
    private val center = UNUserNotificationCenter.currentNotificationCenter()
    private val defaults = NSUserDefaults.standardUserDefaults

    @Suppress("UNCHECKED_CAST")
    override fun scheduledIds(): Set<String> = (defaults.stringArrayForKey(Ids) as List<String>?).orEmpty().toSet()

    override fun schedule(alert: Alert, reminder: Reminder) {
        val content = UNMutableNotificationContent().apply {
            setTitle(reminder.title)
            setBody(bodyFor(reminder, alert))
            setCategoryIdentifier(Category)
            setThreadIdentifier(alert.occurrenceKey)
            setSound(UNNotificationSound.defaultSound)
            setUserInfo(
                mapOf<Any?, Any?>(
                    KeyReminderId to reminder.id.value,
                    KeyDue to alert.occurrenceDue.toEpochMilliseconds().toString(),
                ),
            )
        }
        add(alert.id, content, alert.fireAt)
        saveIds(scheduledIds() + alert.id)
    }

    override fun cancel(id: String) {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(id))
        saveIds(scheduledIds() - id)
    }

    override fun setRefillNotice(at: Instant?) {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(RefillId))
        if (at == null) return
        val content = UNMutableNotificationContent().apply {
            setTitle(text("refill_title", "Open Yaad to keep reminders coming"))
            setBody(text("refill_body", "You have more reminders than iPhone and iPad can hold at once."))
        }
        add(RefillId, content, at)
    }

    /** Fires at [at] exactly: the date components are in UTC, so a change of the device's zone does not move it. */
    private fun add(id: String, content: UNMutableNotificationContent, at: Instant) {
        val utc = NSTimeZone.timeZoneWithName("UTC")!!
        val calendar = NSCalendar(calendarIdentifier = NSCalendarIdentifierGregorian).apply { timeZone = utc }
        val units = NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or NSCalendarUnitHour or
            NSCalendarUnitMinute or NSCalendarUnitSecond
        val components = calendar.components(units, fromDate = at.toNSDate()).apply { timeZone = utc }
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(components, repeats = false)
        center.addNotificationRequest(UNNotificationRequest.requestWithIdentifier(id, content, trigger), null)
    }

    private fun bodyFor(reminder: Reminder, alert: Alert): String {
        val formatter = NSDateFormatter().apply {
            dateStyle = NSDateFormatterMediumStyle
            timeStyle = if (reminder.allDay) NSDateFormatterNoStyle else NSDateFormatterShortStyle
        }
        val whenText = formatter.stringFromDate(alert.occurrenceDue.toNSDate())
        val template = when (alert.kind) {
            AlertKind.Early -> text("due_at", "Due %@")
            AlertKind.Due -> text("due_now", "Due now, %@")
            AlertKind.Nag -> text("still_due", "Still due, %@")
            AlertKind.Snoozed -> text("snoozed", "Snoozed, due %@")
        }
        return template.replace("%@", whenText)
    }

    private fun saveIds(ids: Set<String>) = defaults.setObject(ids.toList(), Ids)

    companion object {
        private const val Ids = "yaad.alerts.ids"
        private const val RefillId = "yaad.refill"
        const val Category = "yaad.reminder"
        const val KeyReminderId = "reminderId"
        const val KeyDue = "due"

        /** A string from the app's Localizable.strings, falling back to English. */
        fun text(key: String, english: String): String =
            NSBundle.mainBundle.localizedStringForKey(key, value = english, table = null)
    }
}

internal fun Instant.toNSDate(): NSDate = NSDate.dateWithTimeIntervalSince1970(toEpochMilliseconds() / 1000.0)
