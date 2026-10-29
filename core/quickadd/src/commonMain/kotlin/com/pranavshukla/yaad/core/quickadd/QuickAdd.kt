package com.pranavshukla.yaad.core.quickadd

import com.pranavshukla.yaad.core.model.BuiltInCategory
import com.pranavshukla.yaad.core.model.Currency
import com.pranavshukla.yaad.core.model.Frequency
import com.pranavshukla.yaad.core.model.Money
import com.pranavshukla.yaad.core.model.MonthlyOn
import com.pranavshukla.yaad.core.model.RecurrenceRule
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.model.ReminderId
import com.pranavshukla.yaad.core.model.ReminderType
import com.pranavshukla.yaad.core.model.Schedule
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlin.time.Duration.Companion.days

/**
 * What a quick-add phrase was understood as. [date] and [time] are null when the phrase did not say; [toReminder]
 * fills them in (today or the next time that day is still ahead; all day when there is no time).
 */
data class QuickAdd(
    val title: String,
    val date: LocalDate?,
    val time: LocalTime?,
    val rule: RecurrenceRule?,
    val amount: Money?,
    val type: ReminderType,
) {
    fun toReminder(id: ReminderId, now: LocalDateTime): Reminder {
        val day = date ?: if (time != null && time <= now.time) now.date.plus(1, DateTimeUnit.DAY) else now.date
        val allDay = time == null
        return Reminder(
            id = id,
            title = title,
            schedule = Schedule(LocalDateTime(day, time ?: LocalTime(0, 0)), rule),
            type = type,
            categoryId = when (type) {
                ReminderType.Bill, ReminderType.Emi, ReminderType.CreditCard, ReminderType.Subscription,
                ReminderType.Investment -> BuiltInCategory.Money.id
                ReminderType.Medicine, ReminderType.Insurance -> BuiltInCategory.Health.id
                else -> null
            },
            allDay = allDay,
            amount = amount,
            earlyAlerts = when (type) {
                ReminderType.Document -> listOf(30.days, 7.days)
                ReminderType.Bill, ReminderType.Emi, ReminderType.CreditCard, ReminderType.Insurance -> listOf(3.days)
                ReminderType.Birthday -> listOf(1.days)
                else -> emptyList()
            },
        )
    }
}

/**
 * Reads phrases like "rent 15000 on 5th every month", "call mom tomorrow 7pm", "car insurance expires 12 Mar 2027"
 * or "kal subah 8 baje dawai". Words it recognises become the date, time, repeat rule, amount and type; the rest is
 * the title. Dates are written day first (12/03/2027 is 12 March).
 */
object QuickAddParser {
    private const val Weekday = "mon|monday|tue|tues|tuesday|wed|weds|wednesday|thu|thur|thurs|thursday|fri|friday|" +
        "sat|saturday|sun|sunday|somvar|mangalvar|budhvar|guruvar|shukravar|shanivar|ravivar"
    private const val Month = "jan|january|feb|february|mar|march|apr|april|may|jun|june|jul|july|aug|august|" +
        "sep|sept|september|oct|october|nov|november|dec|december"

    private class Text(val original: String) {
        val lower = original.lowercase()
        val used = BooleanArray(original.length)

        /** Finds [regex] in the unused text, marks the match used, and returns it. */
        fun take(regex: Regex): MatchResult? {
            var from = 0
            while (true) {
                val match = regex.find(lower, from) ?: return null
                if (match.range.none { used[it] }) {
                    match.range.forEach { used[it] = true }
                    return match
                }
                from = match.range.first + 1
            }
        }
    }

    fun parse(phrase: String, now: LocalDateTime, currency: Currency = Currency.INR): QuickAdd {
        val text = Text(phrase.replace(' ', ' '))
        val today = now.date
        var date: LocalDate? = null
        var time: LocalTime? = null
        var period: Period? = null

        // Amounts with a currency, so their digits are not read as dates or times.
        var amount = text.take(Regex("""(?:₹|\brs\.?|\binr)\s*(\d[\d,]*(?:\.\d{1,2})?)\s*(k|lakhs?|lacs?|l|cr|crores?)?\b"""))
            ?.let { money(it.groupValues[1], it.groupValues[2], currency) }

        if (amount == null) {
            text.take(Regex("""\b(\d+(?:\.\d+)?)\s*(k|lakhs?|lacs?|cr|crores?)\b"""))?.let { amount = money(it.groupValues[1], it.groupValues[2], currency) }
        }

        // Repeat rules.
        var rule: RecurrenceRule? = null
        var monthlyLastDay = false
        text.take(Regex("""\b(?:on\s+)?(?:the\s+)?last\s+day\s+of\s+(?:every|each|the)\s+month\b"""))?.let {
            monthlyLastDay = true
        }
        text.take(Regex("""\b(?:every|har)\s+(\d+)\s+(minutes?|mins?|hours?|hrs?|days?|weeks?|months?|years?)\b"""))?.let {
            rule = RecurrenceRule(frequency(it.groupValues[2], it.groupValues[1].toInt()))
        }
        if (rule == null) {
            text.take(Regex("""\bevery\s+other\s+(day|week|month|year)\b"""))?.let { rule = RecurrenceRule(frequency(it.groupValues[1], 2)) }
        }
        if (rule == null) {
            text.take(Regex("""\b(?:every|each|har)\s+weekdays?\b"""))?.let {
                rule = RecurrenceRule(Frequency.Weekly(1, DayOfWeek.entries.take(5).toSet()))
            }
        }
        if (rule == null) {
            text.take(Regex("""\b(?:every|each|har)\s+weekends?\b"""))?.let {
                rule = RecurrenceRule(Frequency.Weekly(1, setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)))
            }
        }
        if (rule == null) {
            text.take(Regex("""\b(?:every|each|har)\s+((?:$Weekday)(?:\s*(?:,|and|&|aur)\s*(?:$Weekday))*)\b"""))?.let { match ->
                val days = Regex(Weekday).findAll(match.groupValues[1]).map { weekday(it.value) }.toSet()
                rule = RecurrenceRule(Frequency.Weekly(1, days))
            }
        }
        if (rule == null) {
            text.take(
                Regex("""\b(every\s*day|daily|everyday|har\s+din|(?:har\s+)?roz(?:ana)?|every\s+(?:week|month|year|hour)|weekly|monthly|yearly|annually|hourly|har\s+(?:hafte|hafta|mahine|mahina|saal))\b"""),
            )?.let {
                rule = RecurrenceRule(
                    when {
                        it.value.contains("hour") -> Frequency.Hourly(1)
                        it.value.contains("week") || it.value.contains("haft") -> Frequency.Weekly()
                        it.value.contains("month") || it.value.contains("mahin") -> Frequency.Monthly(1, MonthlyOn.Day(1))
                        it.value.contains("year") || it.value.contains("annual") || it.value.contains("saal") -> Frequency.Yearly()
                        else -> Frequency.Daily()
                    },
                )
            }
        }
        if (rule == null) {
            // "every morning", "har shaam": daily at that time of day.
            text.take(Regex("""\b(?:every|each|har)\s+(morning|subah|afternoon|dopahar|evening|shaam|sham|night|raat)\b"""))?.let {
                rule = RecurrenceRule(Frequency.Daily())
                period = periodOf(it.groupValues[1])
            }
        }
        if (monthlyLastDay && rule == null) rule = RecurrenceRule(Frequency.Monthly(1, MonthlyOn.LastDay))

        // Moments relative to now: "in 2 hours", "in 3 days".
        text.take(Regex("""\b(?:in|after)\s+(\d+|an?|one|two|three)\s+(minutes?|mins?|hours?|hrs?|days?|weeks?|months?)\b"""))?.let {
            val n = number(it.groupValues[1])
            val unit = it.groupValues[2]
            when {
                unit.startsWith("min") -> LocalDateTime(now.date, now.time).plusMinutes(n).let { at -> date = at.date; time = at.time }
                unit.startsWith("h") -> LocalDateTime(now.date, now.time).plusMinutes(n * 60).let { at -> date = at.date; time = at.time }
                unit.startsWith("d") -> date = today.plus(n, DateTimeUnit.DAY)
                unit.startsWith("w") -> date = today.plus(n * 7, DateTimeUnit.DAY)
                else -> date = today.plus(n, DateTimeUnit.MONTH)
            }
        }

        // Absolute dates, day first.
        if (date == null) {
            text.take(Regex("""\b(?:on\s+)?(\d{1,2})(?:st|nd|rd|th)?\s*(?:of\s+)?($Month)\b\.?(?:\s*,?\s*(\d{4}))?"""))?.let {
                date = dayMonth(it.groupValues[1].toInt(), month(it.groupValues[2]), it.groupValues[3].toIntOrNull(), today)
            }
        }
        if (date == null) {
            text.take(Regex("""\b(?:on\s+)?($Month)\.?\s+(\d{1,2})(?:st|nd|rd|th)?\b(?:\s*,?\s*(\d{4}))?"""))?.let {
                date = dayMonth(it.groupValues[2].toInt(), month(it.groupValues[1]), it.groupValues[3].toIntOrNull(), today)
            }
        }
        if (date == null) {
            text.take(Regex("""\b(?:on\s+)?(\d{1,2})[/-](\d{1,2})(?:[/-](\d{2,4}))?\b"""))?.let {
                val year = it.groupValues[3].toIntOrNull()?.let { y -> if (y < 100) 2000 + y else y }
                date = dayMonth(it.groupValues[1].toInt(), it.groupValues[2].toInt(), year, today)
            }
        }
        var dayOfMonth: Int? = null
        if (date == null) {
            text.take(Regex("""\b(?:on\s+)?(?:the\s+)?(\d{1,2})(?:st|nd|rd|th)\b(?:\s+of\s+(?:every|each|the)\s+month)?"""))?.let {
                val day = it.groupValues[1].toInt()
                if (day in 1..31) {
                    dayOfMonth = day
                    date = nextDayOfMonth(day, today)
                }
            }
        }
        if (date == null) {
            text.take(Regex("""\b(day\s+after\s+tomorrow|parso|parson|tomorrow|tmrw|tmr|kal|today|aaj|tonight|end\s+of\s+(?:the\s+)?month|month\s+end|next\s+week|next\s+month)\b"""))?.let {
                date = when (it.value) {
                    "today", "aaj" -> today
                    "tonight" -> today.also { period = Period.Night }
                    "tomorrow", "tmrw", "tmr", "kal" -> today.plus(1, DateTimeUnit.DAY)
                    "next week" -> today.plus(7, DateTimeUnit.DAY)
                    "next month" -> today.plus(1, DateTimeUnit.MONTH)
                    else -> if (it.value.startsWith("end") || it.value.startsWith("month")) {
                        LocalDate(today.year, today.month.number, 1).plus(1, DateTimeUnit.MONTH).plus(-1, DateTimeUnit.DAY)
                    } else {
                        today.plus(2, DateTimeUnit.DAY)
                    }
                }
            }
        }
        if (date == null && (rule?.frequency !is Frequency.Weekly)) {
            text.take(Regex("""\b(?:on\s+)?(?:(next|this|coming)\s+)?($Weekday)\b"""))?.let {
                val target = weekday(it.groupValues[2])
                var ahead = (target.isoDayNumber - today.dayOfWeek.isoDayNumber + 7) % 7
                if (it.groupValues[1] == "next" && ahead == 0) ahead = 7
                date = today.plus(ahead, DateTimeUnit.DAY)
            }
        }

        // Times.
        if (time == null) {
            text.take(Regex("""\b(?:at\s+)?(\d{1,2})(?:[:.](\d{2}))?\s*(am|pm|a\.m\.|p\.m\.)(?=\W|$)"""))?.let {
                val hour = it.groupValues[1].toInt() % 12 + if (it.groupValues[3].startsWith("p")) 12 else 0
                time = LocalTime(hour, it.groupValues[2].toIntOrNull() ?: 0)
            }
        }
        if (time == null) {
            text.take(Regex("""\b(?:at\s+)?([01]?\d|2[0-3]):([0-5]\d)\b"""))?.let {
                time = LocalTime(it.groupValues[1].toInt(), it.groupValues[2].toInt())
            }
        }
        text.take(Regex("""\b(morning|subah|afternoon|dopahar|evening|shaam|sham|night|raat|tonight|noon|midday|midnight)\b"""))?.let {
            period = periodOf(it.value)
        }
        if (time == null) {
            text.take(Regex("""\b(?:at\s+)?(\d{1,2})(?:[:.](\d{2}))?\s*(?:baje|bje|o'?clock)\b|\bat\s+(\d{1,2})(?:[:.](\d{2}))?\b"""))?.let {
                val hour = (it.groupValues[1].ifEmpty { it.groupValues[3] }).toInt()
                val minute = (it.groupValues[2].ifEmpty { it.groupValues[4] }).toIntOrNull() ?: 0
                if (hour in 0..23) time = LocalTime(withPeriod(hour, period), minute)
            }
        }
        if (time == null) time = period?.default

        // Bare amounts: "15000", "15k", "1.5 lakh", "2,500".
        if (amount == null) {
            text.take(Regex("""(?<![\d/.:-])\b(\d{1,3}(?:,\d{2,3})+|\d{3,})(?:\.(\d{1,2}))?\b(?![/:-])"""))?.let {
                amount = money(it.value, "", currency)
            }
        }

        // A month rule falls on the date's day ("on 5th every month"); a year rule on its date.
        val ruleNow = rule
        if (ruleNow != null && ruleNow.frequency is Frequency.Monthly && !monthlyLastDay) {
            val day = dayOfMonth ?: date?.day ?: today.day
            rule = RecurrenceRule(Frequency.Monthly(ruleNow.frequency.interval, MonthlyOn.Day(day)))
            if (date == null) date = nextDayOfMonth(day, today)
        }
        if (monthlyLastDay && date == null) {
            date = LocalDate(today.year, today.month.number, 1).plus(1, DateTimeUnit.MONTH).plus(-1, DateTimeUnit.DAY)
        }
        val weekly = rule?.frequency as? Frequency.Weekly
        if (weekly != null && date == null && weekly.days.isNotEmpty()) {
            date = (0..6).map { today.plus(it, DateTimeUnit.DAY) }.first { it.dayOfWeek in weekly.days }
        }

        val title = title(text)
        return QuickAdd(title, date, time, rule, amount, type(text.lower))
    }

    private enum class Period(val default: LocalTime) {
        Morning(LocalTime(9, 0)), Afternoon(LocalTime(14, 0)), Evening(LocalTime(18, 0)), Night(LocalTime(21, 0)),
        Noon(LocalTime(12, 0)), Midnight(LocalTime(0, 0)),
    }

    private fun periodOf(word: String): Period = when (word) {
        "morning", "subah" -> Period.Morning
        "afternoon", "dopahar" -> Period.Afternoon
        "evening", "shaam", "sham" -> Period.Evening
        "night", "raat", "tonight" -> Period.Night
        "noon", "midday" -> Period.Noon
        else -> Period.Midnight
    }

    /** "8 baje" in the evening is 20:00; without a word for the time of day, 1 to 6 o'clock means afternoon. */
    private fun withPeriod(hour: Int, period: Period?): Int = when {
        hour >= 13 || hour == 0 -> hour
        period == Period.Morning -> hour % 12
        period == Period.Afternoon || period == Period.Evening || period == Period.Night -> hour % 12 + 12
        hour in 1..6 -> hour + 12
        else -> hour
    }

    private fun frequency(unit: String, n: Int): Frequency = when {
        unit.startsWith("min") -> Frequency.Minutely(n)
        unit.startsWith("h") -> Frequency.Hourly(n)
        unit.startsWith("d") -> Frequency.Daily(n)
        unit.startsWith("w") -> Frequency.Weekly(n)
        unit.startsWith("m") -> Frequency.Monthly(n, MonthlyOn.Day(1))
        else -> Frequency.Yearly(n)
    }

    private fun weekday(word: String): DayOfWeek = when {
        word.startsWith("mon") || word == "somvar" -> DayOfWeek.MONDAY
        word.startsWith("tue") || word == "mangalvar" -> DayOfWeek.TUESDAY
        word.startsWith("wed") || word == "budhvar" -> DayOfWeek.WEDNESDAY
        word.startsWith("thu") || word == "guruvar" -> DayOfWeek.THURSDAY
        word.startsWith("fri") || word == "shukravar" -> DayOfWeek.FRIDAY
        word.startsWith("sat") || word == "shanivar" -> DayOfWeek.SATURDAY
        else -> DayOfWeek.SUNDAY
    }

    private fun month(word: String): Int =
        listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec").indexOf(word.take(3)) + 1

    private fun number(word: String): Int = when (word) {
        "a", "an", "one" -> 1
        "two" -> 2
        "three" -> 3
        else -> word.toInt()
    }

    /** The date with [day] and [month], in [year] or else the next one from [today]; the last day if [day] is too big. */
    private fun dayMonth(day: Int, month: Int, year: Int?, today: LocalDate): LocalDate? {
        if (month !in 1..12 || day !in 1..31) return null
        fun on(y: Int) = LocalDate(y, month, minOf(day, monthLength(y, month)))
        if (year != null) return on(year)
        val thisYear = on(today.year)
        return if (thisYear >= today) thisYear else on(today.year + 1)
    }

    private fun nextDayOfMonth(day: Int, today: LocalDate): LocalDate {
        val thisMonth = LocalDate(today.year, today.month.number, minOf(day, monthLength(today.year, today.month.number)))
        if (thisMonth >= today) return thisMonth
        val next = LocalDate(today.year, today.month.number, 1).plus(1, DateTimeUnit.MONTH)
        return LocalDate(next.year, next.month.number, minOf(day, monthLength(next.year, next.month.number)))
    }

    private fun monthLength(year: Int, month: Int): Int =
        LocalDate(year, month, 1).plus(1, DateTimeUnit.MONTH).plus(-1, DateTimeUnit.DAY).day

    private fun money(digits: String, scale: String, currency: Currency): Money? {
        val base = runCatching { Money.parse(digits, currency) }.getOrNull() ?: return null
        val factor = when {
            scale == "k" -> 1_000
            scale.startsWith("l") -> 100_000
            scale.startsWith("cr") -> 10_000_000
            else -> 1
        }
        // "1.5 lakh": the minor units of 1.5, scaled; exact because the factor is a whole number.
        return base * factor
    }

    private fun LocalDateTime.plusMinutes(minutes: Int): LocalDateTime {
        val total = time.hour * 60 + time.minute + minutes
        val days = total / (24 * 60)
        val rest = total % (24 * 60)
        return LocalDateTime(date.plus(days, DateTimeUnit.DAY), LocalTime(rest / 60, rest % 60))
    }

    private val TypeWords = listOf(
        ReminderType.Emi to Regex("""\bemi\b"""),
        ReminderType.CreditCard to Regex("""\b(credit\s+card|card\s+(?:bill|due|payment))\b"""),
        ReminderType.Insurance to Regex("""\b(insurance|premium|policy)\b"""),
        ReminderType.Document to Regex("""\b(expires?|expiry|renew(?:al)?|passport|licen[cs]e|puc|visa)\b"""),
        ReminderType.Subscription to Regex("""\b(subscription|netflix|spotify|prime|hotstar|youtube\s+premium)\b"""),
        ReminderType.Bill to Regex("""\b(rent|bill|electricity|bijli|water\s+bill|gas\s+(?:bill|cylinder)|wifi|broadband|recharge|maintenance|fees?)\b"""),
        ReminderType.Investment to Regex("""\b(sip|ppf|fd|rd|mutual\s+fund)\b"""),
        ReminderType.Birthday to Regex("""\b(birthday|bday|b'day|janamdin|anniversary)\b"""),
        ReminderType.Medicine to Regex("""\b(medicine|pills?|tablets?|dawai|dawa|dose|insulin)\b"""),
        ReminderType.Appointment to Regex("""\b(appointment|doctor|dentist|meeting|interview)\b"""),
    )

    private fun type(lower: String): ReminderType = TypeWords.firstOrNull { (_, regex) -> regex.containsMatchIn(lower) }?.first ?: ReminderType.General

    private val Acronyms = setOf("emi", "sip", "ppf", "fd", "rd", "puc", "lic", "atm", "pan", "gst", "itr", "ac", "tv")

    private val Fillers = setOf("on", "at", "by", "in", "every", "the", "of", "due", "is", "for", "from", "till", "until", "ko", "pe", "par", "se")
    private val Lead = Regex("""^(?:please\s+)?(?:remind\s+me\s+(?:to\s+|about\s+)?|remember\s+to\s+|yaad\s+dilana\s+)""", RegexOption.IGNORE_CASE)

    /** The words left over, without the little words that only joined them to a date or time. */
    private fun title(text: Text): String {
        // Runs of words between recognised parts, and whether each run follows a recognised part.
        val runs = mutableListOf<Pair<MutableList<String>, Boolean>>(mutableListOf<String>() to false)
        var word = StringBuilder()
        fun endWord() {
            if (word.isNotEmpty()) runs.last().first += word.toString()
            word = StringBuilder()
        }
        text.original.forEachIndexed { i, c ->
            when {
                text.used[i] -> {
                    endWord()
                    if (runs.last().first.isNotEmpty() || !runs.last().second) runs += mutableListOf<String>() to true
                }
                c.isWhitespace() || c == ',' -> endWord()
                else -> word.append(c)
            }
        }
        endWord()
        val words = runs.filter { it.first.isNotEmpty() }.flatMap { (run, afterPart) ->
            val trimmed = run.dropLastWhile { it.lowercase() in Fillers }
            if (afterPart) trimmed.dropWhile { it.lowercase() in Fillers || it.lowercase() == "to" } else trimmed
        }
        val joined = Lead.replace(words.joinToString(" "), "").trim().trimEnd('.', ',', '-', ':')
        return joined.split(' ').joinToString(" ") { if (it.lowercase() in Acronyms) it.uppercase() else it }
            .replaceFirstChar { it.uppercaseChar() }
    }
}
