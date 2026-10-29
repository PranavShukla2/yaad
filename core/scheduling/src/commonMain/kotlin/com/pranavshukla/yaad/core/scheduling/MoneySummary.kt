package com.pranavshukla.yaad.core.scheduling

import com.pranavshukla.yaad.core.model.Currency
import com.pranavshukla.yaad.core.model.Money
import com.pranavshukla.yaad.core.model.Occurrence
import com.pranavshukla.yaad.core.model.OccurrenceStatus
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.model.ReminderType
import com.pranavshukla.yaad.core.recurrence.occurrencesBetween
import com.pranavshukla.yaad.core.recurrence.zoneOr
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** One money occurrence: its amount (null if entered when paying) and whether it is paid. */
data class MoneyItem(val reminder: Reminder, val due: Instant, val date: LocalDate, val amount: Money?, val paid: Boolean)

/**
 * A month's money: what is due (skipped items excluded), what was paid (the amount paid when one was recorded) and
 * what is still pending, the EMI outgo and the total per category. Only amounts in one currency are added up.
 */
data class MoneyMonth(
    val items: List<MoneyItem>,
    val due: Money,
    val paid: Money,
    val pending: Money,
    val emiOutgo: Money,
    val byCategory: Map<String?, Money>,
)

object MoneySummary {
    fun month(reminders: List<Reminder>, history: List<Occurrence>, year: Int, month: Int, deviceZone: TimeZone, currency: Currency): MoneyMonth {
        val first = LocalDate(year, month, 1)
        val items = itemsBetween(reminders, history, first, first.plus(1, DateTimeUnit.MONTH), deviceZone, currency)
        val zero = Money.zero(currency)
        val index = HistoryIndex(history)
        fun sum(list: List<MoneyItem>, amount: (MoneyItem) -> Money?) = list.mapNotNull(amount).fold(zero, Money::plus)
        val paidAmount = { item: MoneyItem ->
            index.record(item.reminder, item.due, item.reminder.schedule.zoneOr(deviceZone))?.paidAmount?.takeIf { it.currency == currency } ?: item.amount
        }
        return MoneyMonth(
            items = items,
            due = sum(items) { it.amount },
            paid = sum(items.filter { it.paid }, paidAmount),
            pending = sum(items.filter { !it.paid }) { it.amount },
            emiOutgo = sum(items.filter { it.reminder.type == ReminderType.Emi }) { it.amount },
            byCategory = items.filter { it.amount != null }.groupBy { it.reminder.categoryId }
                .mapValues { (_, list) -> sum(list) { it.amount } },
        )
    }

    /** Unpaid payments in the next [days] days, largest first. */
    fun upcomingLargest(
        reminders: List<Reminder>,
        history: List<Occurrence>,
        now: Instant,
        deviceZone: TimeZone,
        days: Int = 30,
        limit: Int = 5,
        currency: Currency = Currency.INR,
    ): List<MoneyItem> {
        val today = now.toLocalDateTime(deviceZone).date
        return itemsBetween(reminders, history, today, today.plus(days, DateTimeUnit.DAY), deviceZone, currency)
            .filter { !it.paid && it.amount != null && it.due >= now - 1.days }
            .sortedWith(compareByDescending<MoneyItem> { it.amount!!.minorUnits }.thenBy { it.due })
            .take(limit)
    }

    /** Money occurrences with a date in [from, until), skipped ones left out. */
    private fun itemsBetween(
        reminders: List<Reminder>,
        history: List<Occurrence>,
        from: LocalDate,
        until: LocalDate,
        deviceZone: TimeZone,
        currency: Currency,
    ): List<MoneyItem> {
        val index = HistoryIndex(history)
        return reminders.asSequence()
            .filter { !it.archived && (it.amount?.currency ?: currency) == currency && it.isMoney() }
            .flatMap { reminder ->
                val zone = reminder.schedule.zoneOr(deviceZone)
                reminder.schedule.occurrencesBetween(from.atStartOfDayIn(zone), until.atStartOfDayIn(zone), zone).map { due ->
                    val status = index.record(reminder, due, zone)?.status
                    status to MoneyItem(reminder, due, due.toLocalDateTime(zone).date, reminder.amount, status is OccurrenceStatus.Done)
                }
            }
            .filter { (status, _) -> status !is OccurrenceStatus.Skipped }
            .map { it.second }
            .sortedBy { it.due }
            .toList()
    }

    private fun Reminder.isMoney() = amount != null || type in MoneyTypes

    private val MoneyTypes = setOf(
        ReminderType.Bill, ReminderType.Emi, ReminderType.CreditCard, ReminderType.Subscription, ReminderType.Insurance,
        ReminderType.Investment,
    )
}
