package com.pranavshukla.yaad.core.scheduling

import com.pranavshukla.yaad.core.model.BuiltInCategory
import com.pranavshukla.yaad.core.model.Currency
import com.pranavshukla.yaad.core.model.Money
import com.pranavshukla.yaad.core.model.ReminderId
import com.pranavshukla.yaad.core.model.ReminderType
import com.pranavshukla.yaad.core.recurrence.occurrences
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days

class MoneyTemplatesTest {
    private val zone = TimeZone.of("Asia/Kolkata")
    private fun rupees(text: String) = Money.parse(text, Currency.INR)
    private fun dates(r: com.pranavshukla.yaad.core.model.Reminder, n: Int) =
        r.schedule.occurrences(zone).take(n).map { it.toLocalDateTime(zone).date }.toList()

    @Test
    fun aBimonthlyElectricityBill() {
        val bill = MoneyTemplates.bill(ReminderId("b"), "Electricity", LocalDate(2026, 11, 20), BillCycle.Bimonthly, rupees("2400"), autoPay = false)
        assertEquals(ReminderType.Bill, bill.type)
        assertEquals(BuiltInCategory.Money.id, bill.categoryId)
        assertEquals(rupees("2400"), bill.amount)
        assertEquals(listOf(LocalDate(2026, 11, 20), LocalDate(2027, 1, 20), LocalDate(2027, 3, 20)), dates(bill, 3))
        assertEquals(listOf(3.days), bill.earlyAlerts)
        assertEquals(true, bill.allDay)
    }

    @Test
    fun rentOnThe31stFallsOnTheLastDayOfShortMonths() {
        val rent = MoneyTemplates.bill(ReminderId("r"), "Rent", LocalDate(2027, 1, 31), BillCycle.Monthly, rupees("15000"), autoPay = true)
        assertEquals(listOf(LocalDate(2027, 1, 31), LocalDate(2027, 2, 28), LocalDate(2027, 3, 31)), dates(rent, 3))
        assertEquals(true, rent.autoPay)
    }

    @Test
    fun aYearlySubscriptionWithAFreeTrial() {
        val (renewal, trial) = MoneyTemplates.subscription(
            ReminderId("s"), "Music", LocalDate(2026, 12, 1), BillCycle.Yearly, rupees("1189"), trialEnds = LocalDate(2026, 11, 30),
        )
        assertEquals(ReminderType.Subscription, renewal.type)
        assertEquals(listOf(LocalDate(2026, 12, 1), LocalDate(2027, 12, 1)), dates(renewal, 2))
        assertEquals(listOf(LocalDate(2026, 11, 30)), dates(trial!!, 5))
        assertEquals(listOf(1.days), trial.earlyAlerts)
    }

    @Test
    fun aCreditCardIsDueMonthlyWithTwoWarnings() {
        val card = MoneyTemplates.creditCard(ReminderId("c"), "HDFC card", LocalDate(2026, 11, 7))
        assertEquals(ReminderType.CreditCard, card.type)
        assertEquals(listOf(5.days, 1.days), card.earlyAlerts)
        assertEquals(listOf(LocalDate(2026, 11, 7), LocalDate(2026, 12, 7)), dates(card, 2))
    }

    @Test
    fun aHalfYearlyInsurancePremium() {
        val policy = MoneyTemplates.insurance(ReminderId("i"), "Health cover", LocalDate(2027, 2, 15), BillCycle.HalfYearly, rupees("12500"))
        assertEquals(ReminderType.Insurance, policy.type)
        assertEquals(BuiltInCategory.Health.id, policy.categoryId)
        assertEquals(listOf(30.days, 7.days), policy.earlyAlerts)
        assertEquals(listOf(LocalDate(2027, 2, 15), LocalDate(2027, 8, 15)), dates(policy, 2))
    }

    @Test
    fun investments() {
        val sip = MoneyTemplates.sip(ReminderId("sip"), "Index fund SIP", LocalDate(2026, 11, 10), rupees("5000"))
        assertEquals(listOf(LocalDate(2026, 11, 10), LocalDate(2026, 12, 10)), dates(sip, 2))
        val fd = MoneyTemplates.maturity(ReminderId("fd"), "FD matures", LocalDate(2028, 5, 4))
        assertEquals(listOf(LocalDate(2028, 5, 4)), dates(fd, 3))
        val ppf = MoneyTemplates.ppf(ReminderId("ppf"), rupees("150000"), firstYear = 2027)
        // Before the financial year ends on 31 March, with a week's warning.
        assertEquals(listOf(LocalDate(2027, 3, 25), LocalDate(2028, 3, 25)), dates(ppf, 2))
        assertEquals(listOf(7.days), ppf.earlyAlerts)
        assertEquals(ReminderType.Investment, ppf.type)
    }
}
