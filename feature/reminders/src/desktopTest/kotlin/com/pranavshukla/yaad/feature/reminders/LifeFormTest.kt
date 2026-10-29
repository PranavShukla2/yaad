package com.pranavshukla.yaad.feature.reminders

import com.pranavshukla.yaad.core.model.ReminderId
import com.pranavshukla.yaad.core.model.ReminderType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LifeFormTest {
    private val id = ReminderId("x")
    private val date = LocalDate(2026, 10, 28)

    @Test
    fun aMedicineWithARefill() {
        val form = LifeForm(LifeKind.Medicine, name = "Metformin", date = date, doses = setOf(Dose.Morning, Dose.Night), courseDays = "", pills = "30", note = "After food")
        val reminders = form.toReminders(id)!!
        assertEquals(3, reminders.size, "two doses and a refill")
        assertEquals(listOf(LocalTime(8, 0), LocalTime(21, 0)), reminders.take(2).map { it.schedule.start.time })
        assertEquals(LocalDate(2026, 11, 9), reminders[2].schedule.start.date)
    }

    @Test
    fun otherKinds() {
        assertEquals(ReminderType.Birthday, LifeForm(LifeKind.Birthday, name = "Asha", date = LocalDate(1996, 2, 29)).toReminders(id)!!.single().type)
        assertEquals(ReminderType.Document, LifeForm(LifeKind.Document, name = "Passport", date = date).toReminders(id)!!.single().type)
        assertEquals(LocalDate(2028, 10, 28), LifeForm(LifeKind.Warranty, name = "TV", date = date, months = "24").toReminders(id)!!.single().schedule.start.date)
        assertEquals(LocalTime(17, 30), LifeForm(LifeKind.Appointment, name = "Dentist", date = date, time = LocalTime(17, 30)).toReminders(id)!!.single().schedule.start.time)
    }

    @Test
    fun incompleteFormsMakeNothing() {
        assertNull(LifeForm(LifeKind.Document, name = "", date = date).toReminders(id))
        assertNull(LifeForm(LifeKind.Medicine, name = "X", date = date, doses = emptySet()).toReminders(id))
        assertNull(LifeForm(LifeKind.Warranty, name = "TV", date = date, months = "").toReminders(id))
        assertNull(LifeForm(LifeKind.Medicine, name = "X", date = date, courseDays = "abc").toReminders(id))
    }
}
