package com.pranavshukla.yaad.data

import com.pranavshukla.yaad.core.model.AppSettings
import com.pranavshukla.yaad.core.model.Currency
import com.pranavshukla.yaad.core.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsCodecTest {
    private val custom = AppSettings(
        allDayTime = LocalTime(8, 30), quietStart = LocalTime(22, 0), quietEnd = LocalTime(7, 0), twentyFourHour = true,
        currency = Currency.USD, theme = ThemeMode.Dark, firstDayOfWeek = DayOfWeek.SUNDAY, hideAmounts = true, appLock = true,
    )

    @Test
    fun everySettingRoundTrips() {
        assertEquals(custom, SettingsCodec.decode(SettingsCodec.encode(custom)))
        assertEquals(AppSettings(), SettingsCodec.decode(SettingsCodec.encode(AppSettings())))
    }

    @Test
    fun missingOrBadValuesKeepTheirDefaults() {
        assertEquals(AppSettings(), SettingsCodec.decode(emptyMap()))
        assertEquals(AppSettings(), SettingsCodec.decode(mapOf("allDayTime" to "9 o'clock", "currency" to "XYZ", "theme" to "Neon", "appLock" to "maybe")))
    }

    @Test
    fun theStoreKeepsSettings() = runTest {
        val store = PreferenceStore(testDriver(), StandardTestDispatcher(testScheduler))
        assertEquals(AppSettings(), store.settings().first())
        store.saveSettings(custom)
        assertEquals(custom, store.settings().first())
    }
}
