package com.pranavshukla.yaad.core.scheduling

import com.pranavshukla.yaad.core.model.BuiltInCategory
import com.pranavshukla.yaad.core.model.Occurrence
import com.pranavshukla.yaad.core.model.OccurrenceStatus
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.model.ReminderId
import com.pranavshukla.yaad.core.model.ReminderType
import com.pranavshukla.yaad.core.recurrence.occurrences
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class LifeTemplatesTest {
    private val zone = TimeZone.of("Asia/Kolkata")
    private fun local(r: Reminder, n: Int) = r.schedule.occurrences(zone).take(n).map { it.toLocalDateTime(zone) }.toList()

    @Test
    fun aPassportExpiryWarnsThreeMonthsAhead() {
        val passport = LifeTemplates.document(ReminderId("p"), "Passport", LocalDate(2027, 3, 12))
        assertEquals(ReminderType.Document, passport.type)
        assertEquals(listOf(90.days, 30.days, 7.days, 1.days), passport.earlyAlerts)
        assertEquals(listOf(LocalDate(2027, 3, 12)), local(passport, 3).map { it.date })
    }

    @Test
    fun aWarrantyExpiresAfterItsPeriod() {
        val tv = LifeTemplates.warranty(ReminderId("w"), "Television", purchased = LocalDate(2025, 1, 31), months = 13)
        assertEquals(ReminderType.Warranty, tv.type)
        assertEquals(BuiltInCategory.Home.id, tv.categoryId)
        assertEquals(listOf(LocalDate(2026, 2, 28)), local(tv, 2).map { it.date })
    }

    @Test
    fun birthdaysRepeatYearlyAndKnowTheAge() {
        val asha = LifeTemplates.birthday(ReminderId("b"), "Asha", LocalDate(1996, 2, 29))
        assertEquals(ReminderType.Birthday, asha.type)
        val y2027 = asha.schedule.occurrences(zone).first { it.toLocalDateTime(zone).date.year == 2027 }
        assertEquals(LocalDate(2027, 2, 28), y2027.toLocalDateTime(zone).date)
        assertEquals(31, LifeTemplates.yearsOn(asha, LocalDate(2027, 2, 28)))
        assertEquals(listOf(1.days), asha.earlyAlerts)
    }

    @Test
    fun aMedicineTwiceADayForFiveDays() {
        val doses = LifeTemplates.medicine(
            ReminderId("m"), "Amoxicillin", listOf(LocalTime(8, 0), LocalTime(20, 0)), LocalDate(2026, 10, 28),
            courseDays = 5, note = "After food",
        )
        assertEquals(2, doses.size)
        assertTrue(doses.all { it.type == ReminderType.Medicine && it.nagInterval == 10.minutes && it.notes == "After food" })
        assertEquals(5, doses[0].schedule.occurrences(zone).count())
        assertEquals(LocalDateTime(2026, 11, 1, 20, 0), local(doses[1], 10).last())
    }

    @Test
    fun aRefillIsRemindedThreeDaysBeforeThePillsRunOut() {
        // 30 pills at 2 a day from 28 October last 15 days, to 11 November; remind on 9 November.
        val refill = LifeTemplates.refill(ReminderId("r"), "Metformin", pills = 30, perDay = 2, from = LocalDate(2026, 10, 28))
        assertEquals(listOf(LocalDate(2026, 11, 9)), local(refill, 2).map { it.date })
    }

    @Test
    fun anAppointmentWarnsADayAndAnHourAhead() {
        val visit = LifeTemplates.appointment(ReminderId("a"), "Dentist", LocalDateTime(2026, 11, 3, 17, 30))
        assertEquals(ReminderType.Appointment, visit.type)
        assertEquals(listOf(1.days, 1.hours), visit.earlyAlerts)
    }

    @Test
    fun medicineNaggingStopsOnDone() {
        val dose = LifeTemplates.medicine(ReminderId("m"), "Insulin", listOf(LocalTime(15, 0)), LocalDate(2026, 10, 28), courseDays = null)[0]
        val now = Instant.parse("2026-10-28T10:00:00Z") // 15:30 IST, half an hour after the dose
        val nagging = AlertPlanner.plan(listOf(dose), emptyList(), now, zone, PlanSettings(horizon = 2.hours))
        assertTrue(nagging.isNotEmpty() && nagging.all { it.kind == AlertKind.Nag })
        val taken = Occurrence(dose.id, Instant.parse("2026-10-28T09:30:00Z"), OccurrenceStatus.Done(now))
        assertEquals(emptyList(), AlertPlanner.plan(listOf(dose), listOf(taken), now, zone, PlanSettings(horizon = 2.hours)))
    }
}
