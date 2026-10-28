package com.pranavshukla.yaad.data

import com.pranavshukla.yaad.core.model.Frequency
import com.pranavshukla.yaad.core.model.MonthlyOn
import com.pranavshukla.yaad.core.model.MonthlyOn.NthWeekday.Companion.LAST
import com.pranavshukla.yaad.core.model.RecurrenceEnd
import com.pranavshukla.yaad.core.model.RecurrenceRule
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RuleCodecTest {
    private fun roundTrip(rule: RecurrenceRule, text: String) {
        assertEquals(text, RuleCodec.encode(rule))
        assertEquals(rule, RuleCodec.decode(text))
    }

    @Test
    fun everyRuleHasAStableText() {
        roundTrip(RecurrenceRule(Frequency.Minutely(90)), "FREQ=MINUTELY;INTERVAL=90")
        roundTrip(RecurrenceRule(Frequency.Hourly(4)), "FREQ=HOURLY;INTERVAL=4")
        roundTrip(RecurrenceRule(Frequency.Daily()), "FREQ=DAILY;INTERVAL=1")
        roundTrip(
            RecurrenceRule(Frequency.Weekly(2, setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY))),
            "FREQ=WEEKLY;INTERVAL=2;BYDAY=MO,FR",
        )
        roundTrip(RecurrenceRule(Frequency.Weekly()), "FREQ=WEEKLY;INTERVAL=1")
        roundTrip(RecurrenceRule(Frequency.Monthly(on = MonthlyOn.Day(31))), "FREQ=MONTHLY;INTERVAL=1;BYMONTHDAY=31")
        roundTrip(RecurrenceRule(Frequency.Monthly(3, MonthlyOn.LastDay)), "FREQ=MONTHLY;INTERVAL=3;BYMONTHDAY=-1")
        roundTrip(
            RecurrenceRule(Frequency.Monthly(on = MonthlyOn.NthWeekday(2, DayOfWeek.TUESDAY))),
            "FREQ=MONTHLY;INTERVAL=1;BYDAY=2TU",
        )
        roundTrip(
            RecurrenceRule(Frequency.Monthly(on = MonthlyOn.NthWeekday(LAST, DayOfWeek.FRIDAY))),
            "FREQ=MONTHLY;INTERVAL=1;BYDAY=-1FR",
        )
        roundTrip(RecurrenceRule(Frequency.Yearly(2)), "FREQ=YEARLY;INTERVAL=2")
    }

    @Test
    fun endsAreKept() {
        roundTrip(
            RecurrenceRule(Frequency.Monthly(on = MonthlyOn.Day(5)), RecurrenceEnd.AfterCount(240)),
            "FREQ=MONTHLY;INTERVAL=1;BYMONTHDAY=5;COUNT=240",
        )
        roundTrip(
            RecurrenceRule(Frequency.Daily(), RecurrenceEnd.OnDate(LocalDate(2027, 3, 31))),
            "FREQ=DAILY;INTERVAL=1;UNTIL=2027-03-31",
        )
    }

    @Test
    fun badTextIsRejected() {
        for (text in listOf("", "FREQ=SECONDLY", "FREQ=DAILY;INTERVAL=x", "FREQ=MONTHLY", "FREQ=WEEKLY;BYDAY=XX",
            "FREQ=DAILY;COUNT=0", "FREQ=DAILY;UNTIL=2027-02-30", "INTERVAL=2")) {
            assertFailsWith<IllegalArgumentException>(text) { RuleCodec.decode(text) }
        }
    }
}
