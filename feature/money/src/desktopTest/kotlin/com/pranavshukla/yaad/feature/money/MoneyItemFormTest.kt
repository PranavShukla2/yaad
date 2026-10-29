package com.pranavshukla.yaad.feature.money

import com.pranavshukla.yaad.core.model.Currency
import com.pranavshukla.yaad.core.model.Money
import com.pranavshukla.yaad.core.model.ReminderId
import com.pranavshukla.yaad.core.model.ReminderType
import com.pranavshukla.yaad.core.scheduling.BillCycle
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MoneyItemFormTest {
    private val date = LocalDate(2026, 11, 20)
    private val id = ReminderId("x")

    @Test
    fun aBillWithAnAmount() {
        val form = MoneyItemForm(MoneyKind.Bill, name = "Electricity", amount = "2,400", date = date, cycle = BillCycle.Bimonthly, autoPay = true)
        val bill = form.toReminders(id, Currency.INR)!!.single()
        assertEquals(ReminderType.Bill, bill.type)
        assertEquals(Money.parse("2400", Currency.INR), bill.amount)
        assertEquals(true, bill.autoPay)
    }

    @Test
    fun aSubscriptionWithATrialMakesTwoReminders() {
        val form = MoneyItemForm(MoneyKind.Subscription, name = "Music", amount = "119", date = date, trialEnds = LocalDate(2026, 11, 19))
        assertEquals(2, form.toReminders(id, Currency.INR)!!.size)
    }

    @Test
    fun cardsAndMaturitiesNeedNoAmount() {
        assertEquals(ReminderType.CreditCard, MoneyItemForm(MoneyKind.CreditCard, name = "Card", date = date).toReminders(id, Currency.INR)!!.single().type)
        assertEquals(null, MoneyItemForm(MoneyKind.Maturity, name = "FD", date = date).toReminders(id, Currency.INR)!!.single().amount)
    }

    @Test
    fun aPpfDepositUsesTheYearOfTheDate() {
        val ppf = MoneyItemForm(MoneyKind.Ppf, name = "PPF", amount = "150000", date = date).toReminders(id, Currency.INR)!!.single()
        assertEquals(LocalDate(2027, 3, 25), ppf.schedule.start.date, "the next 25 March")
    }

    @Test
    fun incompleteFormsMakeNothing() {
        assertNull(MoneyItemForm(MoneyKind.Bill, name = " ", amount = "100", date = date).toReminders(id, Currency.INR))
        assertNull(MoneyItemForm(MoneyKind.Insurance, name = "Cover", amount = "", date = date).toReminders(id, Currency.INR))
        assertNull(MoneyItemForm(MoneyKind.Sip, name = "SIP", amount = "five", date = date).toReminders(id, Currency.INR))
    }
}
