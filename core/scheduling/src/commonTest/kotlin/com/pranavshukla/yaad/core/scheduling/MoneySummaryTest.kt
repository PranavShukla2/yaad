package com.pranavshukla.yaad.core.scheduling

import com.pranavshukla.yaad.core.model.BuiltInCategory
import com.pranavshukla.yaad.core.model.Currency
import com.pranavshukla.yaad.core.model.Money
import com.pranavshukla.yaad.core.model.Occurrence
import com.pranavshukla.yaad.core.model.OccurrenceStatus
import com.pranavshukla.yaad.core.model.ReminderId
import com.pranavshukla.yaad.core.model.ReminderType
import com.pranavshukla.yaad.core.recurrence.occurrences
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * November 2026, by hand:
 * rent 15,000 (paid 15,000) + EMI 8,678.23 (paid 8,700.00: a little extra) + electricity 2,400 (pending)
 * + streaming 649 (pending) + gym 1,500 (skipped: not counted) + card (no amount: not counted) = 26,727.23 due;
 * paid 23,700.00; pending 3,049.00; EMI outgo 8,678.23.
 */
class MoneySummaryTest {
    private val zone = TimeZone.of("Asia/Kolkata")
    private fun rupees(text: String) = Money.parse(text, Currency.INR)
    private val now = Instant.parse("2026-11-15T10:00:00Z")

    private val rent = MoneyTemplates.bill(ReminderId("rent"), "Rent", LocalDate(2026, 10, 5), BillCycle.Monthly, rupees("15000"), autoPay = false)
    private val emi = MoneyTemplates.sip(ReminderId("emi"), "HDFC EMI", LocalDate(2026, 9, 7), rupees("8678.23")).copy(type = ReminderType.Emi)
    private val power = MoneyTemplates.bill(ReminderId("power"), "Electricity", LocalDate(2026, 11, 20), BillCycle.Bimonthly, rupees("2400"), autoPay = false)
    private val stream = MoneyTemplates.subscription(ReminderId("tv"), "Streaming", LocalDate(2026, 11, 28), BillCycle.Monthly, rupees("649")).first.copy(categoryId = "fun")
    private val gym = MoneyTemplates.bill(ReminderId("gym"), "Gym", LocalDate(2026, 11, 1), BillCycle.Monthly, rupees("1500"), autoPay = true)
    private val card = MoneyTemplates.creditCard(ReminderId("card"), "Card", LocalDate(2026, 11, 7))
    private val all = listOf(rent, emi, power, stream, gym, card)

    private fun dueIn(r: com.pranavshukla.yaad.core.model.Reminder, month: Int) =
        r.schedule.occurrences(zone).first { it.toLocalDateTime(zone).date.month.ordinal + 1 == month && it.toLocalDateTime(zone).date.year == 2026 }

    private val history = listOf(
        Occurrence(rent.id, dueIn(rent, 11), OccurrenceStatus.Done(now)),
        Occurrence(emi.id, dueIn(emi, 11), OccurrenceStatus.Done(now), paidAmount = rupees("8700")),
        Occurrence(gym.id, dueIn(gym, 11), OccurrenceStatus.Skipped(now)),
        Occurrence(rent.id, dueIn(rent, 10), OccurrenceStatus.Done(now)),
    )

    @Test
    fun theMonthsTotals() {
        val month = MoneySummary.month(all, history, 2026, 11, zone, Currency.INR)
        assertEquals(rupees("26727.23"), month.due)
        assertEquals(rupees("23700"), month.paid)
        assertEquals(rupees("3049"), month.pending)
        assertEquals(rupees("8678.23"), month.emiOutgo)
        assertEquals(5, month.items.size, "the card without an amount is listed too; the skipped gym is not")
    }

    @Test
    fun totalsByCategory() {
        val month = MoneySummary.month(all, history, 2026, 11, zone, Currency.INR)
        assertEquals(mapOf<String?, Money>(BuiltInCategory.Money.id to rupees("26078.23"), "fun" to rupees("649")), month.byCategory)
    }

    @Test
    fun theLargestUpcomingPayments() {
        val upcoming = MoneySummary.upcomingLargest(all, history, now, zone, days = 30, limit = 3)
        assertEquals(listOf("Rent", "HDFC EMI", "Electricity"), upcoming.map { it.reminder.title })
        assertEquals(LocalDate(2026, 12, 5), upcoming.first().date)
    }

    @Test
    fun otherCurrenciesAreLeftOut() {
        val dollars = stream.copy(id = ReminderId("usd"), amount = Money(999, Currency.USD))
        val month = MoneySummary.month(all + dollars, history, 2026, 11, zone, Currency.INR)
        assertEquals(rupees("26727.23"), month.due)
    }
}
