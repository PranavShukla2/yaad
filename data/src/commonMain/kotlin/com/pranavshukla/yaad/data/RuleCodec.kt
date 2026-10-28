package com.pranavshukla.yaad.data

import com.pranavshukla.yaad.core.model.Frequency
import com.pranavshukla.yaad.core.model.MonthlyOn
import com.pranavshukla.yaad.core.model.RecurrenceEnd
import com.pranavshukla.yaad.core.model.RecurrenceRule
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/**
 * Stores a [RecurrenceRule] as text in the style of an iCalendar RRULE, e.g. `FREQ=MONTHLY;INTERVAL=1;BYMONTHDAY=31`.
 * The meaning is Yaad's (day 31 falls on the last day of short months), not RFC 5545's. The text is part of the
 * database and backup formats: change it only with a migration.
 */
object RuleCodec {
    private val dayCodes = mapOf(
        DayOfWeek.MONDAY to "MO", DayOfWeek.TUESDAY to "TU", DayOfWeek.WEDNESDAY to "WE", DayOfWeek.THURSDAY to "TH",
        DayOfWeek.FRIDAY to "FR", DayOfWeek.SATURDAY to "SA", DayOfWeek.SUNDAY to "SU",
    )
    private val daysByCode = dayCodes.entries.associate { (day, code) -> code to day }

    fun encode(rule: RecurrenceRule): String = buildList {
        val f = rule.frequency
        add(
            "FREQ=" + when (f) {
                is Frequency.Minutely -> "MINUTELY"
                is Frequency.Hourly -> "HOURLY"
                is Frequency.Daily -> "DAILY"
                is Frequency.Weekly -> "WEEKLY"
                is Frequency.Monthly -> "MONTHLY"
                is Frequency.Yearly -> "YEARLY"
            },
        )
        add("INTERVAL=${f.interval}")
        if (f is Frequency.Weekly && f.days.isNotEmpty()) {
            add("BYDAY=" + f.days.sortedBy { it.ordinal }.joinToString(",") { dayCodes.getValue(it) })
        }
        if (f is Frequency.Monthly) {
            add(
                when (val on = f.on) {
                    is MonthlyOn.Day -> "BYMONTHDAY=${on.day}"
                    MonthlyOn.LastDay -> "BYMONTHDAY=-1"
                    is MonthlyOn.NthWeekday -> "BYDAY=${on.ordinal}${dayCodes.getValue(on.weekday)}"
                },
            )
        }
        when (val end = rule.end) {
            RecurrenceEnd.Never -> Unit
            is RecurrenceEnd.AfterCount -> add("COUNT=${end.count}")
            is RecurrenceEnd.OnDate -> add("UNTIL=${end.date}")
        }
    }.joinToString(";")

    /** @throws IllegalArgumentException if [text] is not a rule this codec wrote. */
    fun decode(text: String): RecurrenceRule {
        val parts = text.split(';').associate { part ->
            val (key, value) = part.split('=', limit = 2).also { require(it.size == 2) { "Bad rule part '$part'" } }
            key to value
        }
        val interval = parts["INTERVAL"]?.let { number(it) } ?: 1
        val frequency = when (val freq = parts["FREQ"]) {
            "MINUTELY" -> Frequency.Minutely(interval)
            "HOURLY" -> Frequency.Hourly(interval)
            "DAILY" -> Frequency.Daily(interval)
            "WEEKLY" -> Frequency.Weekly(interval, parts["BYDAY"]?.split(',')?.map(::day)?.toSet().orEmpty())
            "MONTHLY" -> Frequency.Monthly(interval, monthlyOn(parts))
            "YEARLY" -> Frequency.Yearly(interval)
            else -> throw IllegalArgumentException("Unknown frequency '$freq' in '$text'")
        }
        require(interval >= 1) { "Interval must be at least 1 in '$text'" }
        val end = when {
            "COUNT" in parts -> RecurrenceEnd.AfterCount(number(parts.getValue("COUNT")))
            "UNTIL" in parts -> RecurrenceEnd.OnDate(LocalDate.parse(parts.getValue("UNTIL")))
            else -> RecurrenceEnd.Never
        }
        return RecurrenceRule(frequency, end)
    }

    private fun monthlyOn(parts: Map<String, String>): MonthlyOn {
        parts["BYMONTHDAY"]?.let { return if (it == "-1") MonthlyOn.LastDay else MonthlyOn.Day(number(it)) }
        val byDay = requireNotNull(parts["BYDAY"]) { "A monthly rule needs BYMONTHDAY or BYDAY" }
        return MonthlyOn.NthWeekday(number(byDay.dropLast(2)), day(byDay.takeLast(2)))
    }

    private fun day(code: String): DayOfWeek = requireNotNull(daysByCode[code]) { "Unknown weekday '$code'" }

    private fun number(text: String): Int = requireNotNull(text.toIntOrNull()) { "Not a number: '$text'" }
}
