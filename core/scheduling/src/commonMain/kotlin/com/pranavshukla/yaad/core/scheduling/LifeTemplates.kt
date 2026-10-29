package com.pranavshukla.yaad.core.scheduling

import com.pranavshukla.yaad.core.model.BuiltInCategory
import com.pranavshukla.yaad.core.model.Frequency
import com.pranavshukla.yaad.core.model.RecurrenceEnd
import com.pranavshukla.yaad.core.model.RecurrenceRule
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.model.ReminderId
import com.pranavshukla.yaad.core.model.ReminderType
import com.pranavshukla.yaad.core.model.Schedule
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/** Templates for life admin: documents, warranties, birthdays, medicines and appointments. */
object LifeTemplates {
    fun document(id: ReminderId, name: String, expires: LocalDate): Reminder =
        allDay(id, name, ReminderType.Document, expires, null, listOf(90.days, 30.days, 7.days, 1.days), BuiltInCategory.Personal)

    fun warranty(id: ReminderId, product: String, purchased: LocalDate, months: Int): Reminder =
        allDay(id, product, ReminderType.Warranty, purchased.plus(months, DateTimeUnit.MONTH), null, listOf(30.days, 7.days), BuiltInCategory.Home)

    /**
     * Every year on [born]: the start stays the real date, so [yearsOn] gives the age (or years married for an
     * anniversary). 29 February falls on 28 February in common years.
     */
    fun birthday(id: ReminderId, name: String, born: LocalDate, type: ReminderType = ReminderType.Birthday): Reminder =
        allDay(id, name, type, born, RecurrenceRule(Frequency.Yearly()), listOf(1.days), BuiltInCategory.Personal)

    /** How many years old a birthday (or anniversary) is on [date]. */
    fun yearsOn(reminder: Reminder, date: LocalDate): Int = date.year - reminder.schedule.start.date.year

    /**
     * One reminder per dose time, every day for [courseDays] days (null: no end), nagging every 10 minutes until it
     * is marked taken.
     */
    fun medicine(id: ReminderId, name: String, times: List<LocalTime>, from: LocalDate, courseDays: Int?, note: String = ""): List<Reminder> =
        times.sorted().mapIndexed { i, time ->
            Reminder(
                id = if (i == 0) id else ReminderId("${id.value}-$i"),
                title = name,
                schedule = Schedule(
                    LocalDateTime(from, time),
                    RecurrenceRule(Frequency.Daily(), courseDays?.let { RecurrenceEnd.AfterCount(it) } ?: RecurrenceEnd.Never),
                ),
                type = ReminderType.Medicine,
                notes = note,
                categoryId = BuiltInCategory.Health.id,
                nagInterval = 10.minutes,
            )
        }

    /** Three days before [pills] taken [perDay] a day from [from] run out. */
    fun refill(id: ReminderId, name: String, pills: Int, perDay: Int, from: LocalDate): Reminder {
        val runsOut = from.plus(pills / perDay, DateTimeUnit.DAY)
        return allDay(id, name, ReminderType.Medicine, runsOut.plus(-3, DateTimeUnit.DAY), null, emptyList(), BuiltInCategory.Health)
    }

    fun appointment(id: ReminderId, title: String, at: LocalDateTime): Reminder = Reminder(
        id = id,
        title = title,
        schedule = Schedule(at),
        type = ReminderType.Appointment,
        categoryId = BuiltInCategory.Personal.id,
        earlyAlerts = listOf(1.days, 1.hours),
    )

    private fun allDay(
        id: ReminderId,
        name: String,
        type: ReminderType,
        date: LocalDate,
        rule: RecurrenceRule?,
        early: List<kotlin.time.Duration>,
        category: BuiltInCategory,
    ) = Reminder(
        id = id,
        title = name,
        schedule = Schedule(LocalDateTime(date, LocalTime(0, 0)), rule),
        type = type,
        categoryId = category.id,
        allDay = true,
        earlyAlerts = early,
    )
}
