package com.pranavshukla.yaad.feature.money

import com.pranavshukla.yaad.core.model.Currency
import com.pranavshukla.yaad.core.model.Money
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.model.ReminderId
import com.pranavshukla.yaad.core.scheduling.BillCycle
import com.pranavshukla.yaad.core.scheduling.MoneyTemplates
import kotlinx.datetime.LocalDate

/** The money templates offered by the add button, besides loans. */
enum class MoneyKind(val needsAmount: Boolean, val hasCycle: Boolean) {
    Bill(needsAmount = false, hasCycle = true),
    Subscription(needsAmount = true, hasCycle = true),
    CreditCard(needsAmount = false, hasCycle = false),
    Insurance(needsAmount = true, hasCycle = true),
    Sip(needsAmount = true, hasCycle = false),
    Maturity(needsAmount = false, hasCycle = false),
    Ppf(needsAmount = true, hasCycle = false),
}

/** The quick form for a money template; [toReminders] is null until it is complete. */
data class MoneyItemForm(
    val kind: MoneyKind,
    val name: String = "",
    val amount: String = "",
    val date: LocalDate,
    val cycle: BillCycle = BillCycle.Monthly,
    val autoPay: Boolean = false,
    val trialEnds: LocalDate? = null,
) {
    fun toReminders(id: ReminderId, currency: Currency): List<Reminder>? {
        if (name.isBlank()) return null
        val money = if (amount.isBlank()) null else runCatching { Money.parse(amount, currency) }.getOrNull() ?: return null
        if (kind.needsAmount && money == null) return null
        val title = name.trim()
        return when (kind) {
            MoneyKind.Bill -> listOf(MoneyTemplates.bill(id, title, date, cycle, money, autoPay))
            MoneyKind.Subscription -> MoneyTemplates.subscription(id, title, date, cycle, money!!, trialEnds)
                .let { (renewal, trial) -> listOfNotNull(renewal.copy(autoPay = autoPay), trial) }
            MoneyKind.CreditCard -> listOf(MoneyTemplates.creditCard(id, title, date).copy(autoPay = autoPay))
            MoneyKind.Insurance -> listOf(MoneyTemplates.insurance(id, title, date, cycle, money!!).copy(autoPay = autoPay))
            MoneyKind.Sip -> listOf(MoneyTemplates.sip(id, title, date, money!!).copy(autoPay = autoPay))
            MoneyKind.Maturity -> listOf(MoneyTemplates.maturity(id, title, date))
            MoneyKind.Ppf -> {
                val year = if (date <= LocalDate(date.year, 3, 25)) date.year else date.year + 1
                listOf(MoneyTemplates.ppf(id, money!!, year, title))
            }
        }
    }
}
