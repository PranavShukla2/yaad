package com.pranavshukla.yaad.feature.reminders

import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.model.ReminderId
import com.pranavshukla.yaad.core.model.ReminderType
import com.pranavshukla.yaad.core.scheduling.LifeTemplates
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** The life-admin templates offered by the add button, besides a plain reminder. */
enum class LifeKind { Birthday, Anniversary, Medicine, Document, Warranty, Appointment }

/** Usual dose times; the editor can change the reminders afterwards. */
enum class Dose(val time: LocalTime) { Morning(LocalTime(8, 0)), Afternoon(LocalTime(14, 0)), Night(LocalTime(21, 0)) }

/** The quick form for a life template; [toReminders] is null until it is complete. */
data class LifeForm(
    val kind: LifeKind,
    val name: String = "",
    val date: LocalDate,
    val time: LocalTime = LocalTime(10, 0),
    val doses: Set<Dose> = setOf(Dose.Morning),
    val courseDays: String = "",
    val note: String = "",
    val pills: String = "",
    val months: String = "12",
) {
    fun toReminders(id: ReminderId): List<Reminder>? {
        if (name.isBlank()) return null
        val title = name.trim()
        return when (kind) {
            LifeKind.Birthday -> listOf(LifeTemplates.birthday(id, title, date))
            LifeKind.Anniversary -> listOf(LifeTemplates.birthday(id, title, date))
            LifeKind.Document -> listOf(LifeTemplates.document(id, title, date))
            LifeKind.Warranty -> listOf(LifeTemplates.warranty(id, title, date, months.toIntOrNull()?.takeIf { it > 0 } ?: return null))
            LifeKind.Appointment -> listOf(LifeTemplates.appointment(id, title, kotlinx.datetime.LocalDateTime(date, time)))
            LifeKind.Medicine -> {
                if (doses.isEmpty()) return null
                val course = if (courseDays.isBlank()) null else courseDays.toIntOrNull()?.takeIf { it > 0 } ?: return null
                val pillCount = if (pills.isBlank()) null else pills.toIntOrNull()?.takeIf { it > 0 } ?: return null
                LifeTemplates.medicine(id, title, doses.map { it.time }, date, course, note.trim()) +
                    listOfNotNull(pillCount?.let { LifeTemplates.refill(ReminderId("${id.value}-refill"), title, it, doses.size, date) })
            }
        }
    }
}

/** "Turns 31" for a birthday, or the years for an anniversary; null for other reminders. */
fun yearsOn(reminder: Reminder, date: LocalDate): Int? =
    if (reminder.type == ReminderType.Birthday) LifeTemplates.yearsOn(reminder, date).takeIf { it > 0 } else null
