package com.pranavshukla.yaad.platform.notifications

import com.pranavshukla.yaad.core.model.AppSettings
import com.pranavshukla.yaad.core.model.Currency
import com.pranavshukla.yaad.core.model.Money
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.model.ReminderId
import com.pranavshukla.yaad.core.model.Schedule
import com.pranavshukla.yaad.core.scheduling.PlanSettings
import com.pranavshukla.yaad.core.scheduling.QuietHours
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsEffectsTest {
    private val rent = Reminder(ReminderId("r"), "Rent", Schedule(LocalDateTime(2026, 11, 5, 0, 0)), amount = Money(1_500_000, Currency.INR))

    @Test
    fun amountsShowUnlessHidden() {
        assertEquals("Due now, 5 Nov · ₹15,000.00", withAmount("Due now, 5 Nov", rent, hideAmounts = false))
        assertEquals("Due now, 5 Nov", withAmount("Due now, 5 Nov", rent, hideAmounts = true))
        assertEquals("Due now", withAmount("Due now", rent.copy(amount = null), hideAmounts = false))
    }

    @Test
    fun settingsShapeThePlan() {
        val base = PlanSettings(limit = 400)
        val plan = AppSettings(allDayTime = LocalTime(8, 0), quietStart = LocalTime(22, 0), quietEnd = LocalTime(7, 0)).planSettings(base)
        assertEquals(LocalTime(8, 0), plan.allDayTime)
        assertEquals(QuietHours(LocalTime(22, 0), LocalTime(7, 0)), plan.quietHours)
        assertEquals(400, plan.limit)
        assertEquals(null, AppSettings().planSettings(base).quietHours)
    }
}
