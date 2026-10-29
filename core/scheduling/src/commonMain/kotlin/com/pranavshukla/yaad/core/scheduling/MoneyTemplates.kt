package com.pranavshukla.yaad.core.scheduling

import com.pranavshukla.yaad.core.model.BuiltInCategory
import com.pranavshukla.yaad.core.model.Frequency
import com.pranavshukla.yaad.core.model.Money
import com.pranavshukla.yaad.core.model.MonthlyOn
import com.pranavshukla.yaad.core.model.RecurrenceRule
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.model.ReminderId
import com.pranavshukla.yaad.core.model.ReminderType
import com.pranavshukla.yaad.core.model.Schedule
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/** How often a bill, premium or subscription comes round. */
enum class BillCycle(val months: Int) { Monthly(1), Bimonthly(2), Quarterly(3), HalfYearly(6), Yearly(12) }

/**
 * One-tap templates for money items. Each is an all-day reminder on its due date with sensible early alerts; the
 * user can change anything afterwards in the editor.
 */
object MoneyTemplates {
    fun bill(id: ReminderId, name: String, firstDue: LocalDate, cycle: BillCycle, amount: Money?, autoPay: Boolean): Reminder =
        item(id, name, ReminderType.Bill, firstDue, cycle, amount, listOf(3.days)).copy(autoPay = autoPay)

    /** The renewal, and a one-off reminder the day before a free trial ends, if there is one. */
    fun subscription(
        id: ReminderId,
        name: String,
        renewal: LocalDate,
        cycle: BillCycle,
        price: Money,
        trialEnds: LocalDate? = null,
    ): Pair<Reminder, Reminder?> {
        val renews = item(id, name, ReminderType.Subscription, renewal, cycle, price, listOf(3.days))
        val trial = trialEnds?.let {
            item(ReminderId("${id.value}-trial"), name, ReminderType.Subscription, it, null, null, listOf(1.days))
        }
        return renews to trial
    }

    /** The payment due date each month; the amount changes every statement, so it is entered when paying. */
    fun creditCard(id: ReminderId, name: String, firstDue: LocalDate): Reminder =
        item(id, name, ReminderType.CreditCard, firstDue, BillCycle.Monthly, null, listOf(5.days, 1.days))

    fun insurance(id: ReminderId, name: String, renewal: LocalDate, cycle: BillCycle, premium: Money): Reminder =
        item(id, name, ReminderType.Insurance, renewal, cycle, premium, listOf(30.days, 7.days), BuiltInCategory.Health.id)

    /** A monthly SIP or recurring deposit. */
    fun sip(id: ReminderId, name: String, firstDate: LocalDate, amount: Money): Reminder =
        item(id, name, ReminderType.Investment, firstDate, BillCycle.Monthly, amount, listOf(1.days))

    /** A one-off date such as a fixed deposit maturing. */
    fun maturity(id: ReminderId, name: String, date: LocalDate): Reminder =
        item(id, name, ReminderType.Investment, date, null, null, listOf(7.days))

    /** The yearly PPF deposit, due before the financial year ends on 31 March. */
    fun ppf(id: ReminderId, amount: Money, firstYear: Int, name: String = "PPF"): Reminder =
        item(id, name, ReminderType.Investment, LocalDate(firstYear, 3, 25), BillCycle.Yearly, amount, listOf(7.days))

    private fun item(
        id: ReminderId,
        name: String,
        type: ReminderType,
        first: LocalDate,
        cycle: BillCycle?,
        amount: Money?,
        early: List<Duration>,
        category: String = BuiltInCategory.Money.id,
    ): Reminder {
        val rule = when (cycle) {
            null -> null
            BillCycle.Yearly -> RecurrenceRule(Frequency.Yearly())
            else -> RecurrenceRule(Frequency.Monthly(cycle.months, MonthlyOn.Day(first.day)))
        }
        return Reminder(
            id = id,
            title = name,
            schedule = Schedule(LocalDateTime(first, LocalTime(0, 0)), rule),
            type = type,
            categoryId = category,
            allDay = true,
            earlyAlerts = early,
            amount = amount,
        )
    }
}
