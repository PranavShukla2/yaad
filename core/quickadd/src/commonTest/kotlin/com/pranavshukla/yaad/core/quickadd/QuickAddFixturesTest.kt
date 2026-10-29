package com.pranavshukla.yaad.core.quickadd

import com.pranavshukla.yaad.core.model.Currency
import com.pranavshukla.yaad.core.model.Frequency
import com.pranavshukla.yaad.core.model.Money
import com.pranavshukla.yaad.core.model.MonthlyOn
import com.pranavshukla.yaad.core.model.RecurrenceRule
import com.pranavshukla.yaad.core.model.ReminderType
import com.pranavshukla.yaad.core.model.ReminderType.Appointment
import com.pranavshukla.yaad.core.model.ReminderType.Bill
import com.pranavshukla.yaad.core.model.ReminderType.Birthday
import com.pranavshukla.yaad.core.model.ReminderType.CreditCard
import com.pranavshukla.yaad.core.model.ReminderType.Document
import com.pranavshukla.yaad.core.model.ReminderType.Emi
import com.pranavshukla.yaad.core.model.ReminderType.General
import com.pranavshukla.yaad.core.model.ReminderType.Insurance
import com.pranavshukla.yaad.core.model.ReminderType.Investment
import com.pranavshukla.yaad.core.model.ReminderType.Medicine
import com.pranavshukla.yaad.core.model.ReminderType.Subscription
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.isoDayNumber
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Over 200 phrases with what they mean, read on Wednesday 28 October 2026 at 15:30. */
class QuickAddFixturesTest {
    private val now = LocalDateTime(2026, 10, 28, 15, 30)

    private data class Case(
        val phrase: String,
        val title: String,
        val date: String? = null,
        val time: String? = null,
        val rule: String = "-",
        val amount: String? = null,
        val type: ReminderType = General,
    )

    /** Rules in short: "daily", "daily/3", "weekly:MO,TH", "monthly:5", "monthly/2:20", "monthly:last", "yearly". */
    private fun RecurrenceRule?.short(): String {
        val f = this?.frequency ?: return "-"
        val every = if (f.interval == 1) "" else "/${f.interval}"
        return when (f) {
            is Frequency.Minutely -> "minutely$every"
            is Frequency.Hourly -> "hourly$every"
            is Frequency.Daily -> "daily$every"
            is Frequency.Weekly -> "weekly$every" + if (f.days.isEmpty()) "" else ":" + f.days.sortedBy { it.isoDayNumber }.joinToString(",") { it.name.take(2) }
            is Frequency.Monthly -> "monthly$every:" + when (val on = f.on) {
                is MonthlyOn.Day -> on.day.toString()
                MonthlyOn.LastDay -> "last"
                is MonthlyOn.NthWeekday -> "${on.ordinal}${on.weekday.name.take(2)}"
            }
            is Frequency.Yearly -> "yearly$every"
        }
    }

    private val handWritten = listOf(
        Case("rent 15000 on 5th every month", "Rent", "2026-11-05", rule = "monthly:5", amount = "15000", type = Bill),
        Case("call mom tomorrow 7pm", "Call mom", "2026-10-29", "19:00"),
        Case("car insurance expires 12 Mar 2027", "Car insurance expires", "2027-03-12", type = Insurance),
        Case("kal subah 8 baje dawai", "Dawai", "2026-10-29", "08:00", type = Medicine),
        Case("pay electricity bill ₹2,400 on friday", "Pay electricity bill", "2026-10-30", amount = "2400", type = Bill),
        Case("gym every mon and thu at 6:30 am", "Gym", "2026-10-29", "06:30", "weekly:MO,TH"),
        Case("remind me to water plants every day at 7", "Water plants", null, "07:00", "daily"),
        Case("sip 5k on 10th every month", "SIP", "2026-11-10", rule = "monthly:10", amount = "5000", type = Investment),
        Case("1.5 lakh fd matures 4/5/2028", "FD matures", "2028-05-04", amount = "150000", type = Investment),
        Case("netflix 649 every month on 2nd", "Netflix", "2026-11-02", rule = "monthly:2", amount = "649", type = Subscription),
        Case("doctor appointment on 3 nov at 5:30 pm", "Doctor appointment", "2026-11-03", "17:30", type = Appointment),
        Case("passport renewal 15/08/2027", "Passport renewal", "2027-08-15", type = Document),
        Case("asha birthday 14 feb", "Asha birthday", "2027-02-14", type = Birthday),
        Case("emi 8678 on 7th every month", "EMI", "2026-11-07", rule = "monthly:7", amount = "8678", type = Emi),
        Case("credit card due on 20th", "Credit card", "2026-11-20", type = CreditCard),
        Case("meeting in 2 hours", "Meeting", "2026-10-28", "17:30", type = Appointment),
        Case("call back in 30 minutes", "Call back", "2026-10-28", "16:00"),
        Case("renew licence in 3 days", "Renew licence", "2026-10-31", type = Document),
        Case("submit form next monday", "Submit form", "2026-11-02"),
        Case("team call this friday 11am", "Team call", "2026-10-30", "11:00"),
        Case("pay maintenance 3,500 on the last day of every month", "Pay maintenance", "2026-10-31", rule = "monthly:last", amount = "3500", type = Bill),
        Case("water bill every 2 months on 20th", "Water bill", "2026-11-20", rule = "monthly/2:20", type = Bill),
        Case("take vitamin d every morning", "Take vitamin d", null, "09:00", "daily"),
        Case("har roz raat 9 baje dawai", "Dawai", null, "21:00", "daily", type = Medicine),
        Case("shaam 6 baje doodh lana", "Doodh lana", null, "18:00"),
        Case("parso dentist", "Dentist", "2026-10-30", type = Appointment),
        Case("aaj raat 10 baje movie", "Movie", "2026-10-28", "22:00"),
        Case("lic premium 12,500 every year on 15 feb", "LIC premium", "2027-02-15", rule = "yearly", amount = "12500", type = Insurance),
        Case("ppf deposit 1,50,000 every year on 25 march", "PPF deposit", "2027-03-25", rule = "yearly", amount = "150000", type = Investment),
        Case("gym every weekday at 7 am", "Gym", "2026-10-28", "07:00", "weekly:MO,TU,WE,TH,FR"),
        Case("clean house every weekend", "Clean house", "2026-10-31", rule = "weekly:SA,SU"),
        Case("backup phone every sunday", "Backup phone", "2026-11-01", rule = "weekly:SU"),
        Case("standup every day at 10:00", "Standup", null, "10:00", "daily"),
        Case("drink water every 2 hours", "Drink water", rule = "hourly/2"),
        Case("stretch every 45 minutes", "Stretch", rule = "minutely/45"),
        Case("pay rent rs 15000 on 1st", "Pay rent", "2026-11-01", amount = "15000", type = Bill),
        Case("call Ravi at 4", "Call Ravi", null, "16:00"),
        Case("book tickets on 12/12", "Book tickets", "2026-12-12"),
        Case("pay school fees by 10 nov", "Pay school fees", "2026-11-10", type = Bill),
        Case("visa expires on 31 jan 2028", "Visa expires", "2028-01-31", type = Document),
        Case("puc renewal 30-11-2026", "PUC renewal", "2026-11-30", type = Document),
        Case("recharge phone 299 tomorrow", "Recharge phone", "2026-10-29", amount = "299", type = Bill),
        Case("wifi bill 999 every month", "Wifi bill", "2026-10-28", rule = "monthly:28", amount = "999", type = Bill),
        Case("anniversary 5 dec", "Anniversary", "2026-12-05", type = Birthday),
        Case("mom bday 2 jan", "Mom bday", "2027-01-02", type = Birthday),
        Case("dentist tomorrow at noon", "Dentist", "2026-10-29", "12:00", type = Appointment),
        Case("interview on monday at 10:30 am", "Interview", "2026-11-02", "10:30", type = Appointment),
        Case("take medicine tonight", "Take medicine", "2026-10-28", "21:00", type = Medicine),
        Case("call dad on sunday evening", "Call dad", "2026-11-01", "18:00"),
        Case("pay credit card bill ₹12,345.50 on 18th", "Pay credit card bill", "2026-11-18", amount = "12345.50", type = CreditCard),
        Case("home loan emi 21,237 on 5th every month", "Home loan EMI", "2026-11-05", rule = "monthly:5", amount = "21237", type = Emi),
        Case("buy groceries", "Buy groceries"),
        Case("remind me about the parcel tomorrow morning", "The parcel", "2026-10-29", "09:00"),
        Case("renew car insurance on 14 march 2027 ₹18,000", "Renew car insurance", "2027-03-14", amount = "18000", type = Insurance),
        Case("pay gas cylinder 1100 on 3rd", "Pay gas cylinder", "2026-11-03", amount = "1100", type = Bill),
        Case("flight at 6:05 am on 15 nov", "Flight", "2026-11-15", "06:05"),
        Case("end of month salary check", "Salary check", "2026-10-31"),
        Case("next week dentist", "Dentist", "2026-11-04", type = Appointment),
        Case("next month review", "Review", "2026-11-28"),
        Case("pay 2,000 to maid every month on 1st", "Pay maid", "2026-11-01", rule = "monthly:1", amount = "2000"),
        Case("budhvar ko yoga", "Yoga", "2026-10-28"),
        Case("har somvar mandir", "Mandir", "2026-11-02", rule = "weekly:MO"),
        Case("shukravar shaam 7 baje party", "Party", "2026-10-30", "19:00"),
        Case("kal 5 baje meeting", "Meeting", "2026-10-29", "17:00", type = Appointment),
        Case("aaj dopahar 2 baje bank", "Bank", "2026-10-28", "14:00"),
        Case("every 3 days water cactus", "Water cactus", rule = "daily/3"),
        Case("every other week deep clean", "Deep clean", rule = "weekly/2"),
        Case("pay property tax 8,450 on 30th june", "Pay property tax", "2027-06-30", amount = "8450"),
        Case("₹500 to Priya tomorrow", "Priya", "2026-10-29", amount = "500"),
        Case("pay rent 15,000 on 31st every month", "Pay rent", "2026-10-31", rule = "monthly:31", amount = "15000", type = Bill),
        Case("spotify 119 on 9th", "Spotify", "2026-11-09", amount = "119", type = Subscription),
        Case("insulin every night", "Insulin", null, "21:00", "daily", type = Medicine),
    )

    /** Every title with every way of saying a day and a time. */
    private val combinations: List<Case> = buildList {
        val titles = listOf("Call mom", "Submit report", "Water plants", "Book tickets", "Fix the tap")
        val days = listOf(
            "today" to "2026-10-28", "tomorrow" to "2026-10-29", "tmrw" to "2026-10-29", "kal" to "2026-10-29",
            "day after tomorrow" to "2026-10-30", "parso" to "2026-10-30", "on friday" to "2026-10-30",
            "next monday" to "2026-11-02", "on 12 nov" to "2026-11-12", "on 5th" to "2026-11-05",
        )
        val times = listOf("" to null, "at 7pm" to "19:00", "9:30 am" to "09:30")
        for (title in titles) for ((day, date) in days) for ((time, at) in times) {
            add(Case("${title.lowercase()} $day $time".trim(), title, date, at))
        }
    }

    @Test
    fun overTwoHundredPhrases() {
        val cases = handWritten + combinations
        assertTrue(cases.size >= 200, "${cases.size} phrases")
        val failures = cases.mapNotNull { case ->
            val result = QuickAddParser.parse(case.phrase, now)
            val actual = Case(
                case.phrase, result.title, result.date?.toString(), result.time?.toString(), result.rule.short(),
                result.amount?.let { amountText(it) }, result.type,
            )
            if (actual == case) null else "  ${case.phrase}\n    expected $case\n    actual   $actual"
        }
        assertEquals("", failures.joinToString("\n"), "${failures.size} of ${cases.size} phrases misread")
    }

    private fun amountText(money: Money): String {
        val minor = money.minorUnits % 100
        return "${money.minorUnits / 100}" + if (minor == 0L) "" else "." + minor.toString().padStart(2, '0')
    }

    @Test
    fun theUnderstoodReminder() {
        val reminder = QuickAddParser.parse("rent 15000 on 5th every month", now).toReminder(com.pranavshukla.yaad.core.model.ReminderId("r"), now)
        assertEquals(true, reminder.allDay)
        assertEquals(Money(1_500_000, Currency.INR), reminder.amount)
        val timed = QuickAddParser.parse("call mom at 9 am", now).toReminder(com.pranavshukla.yaad.core.model.ReminderId("c"), now)
        assertEquals(LocalDateTime(2026, 10, 29, 9, 0), timed.schedule.start, "9 am has passed today, so tomorrow")
    }
}
