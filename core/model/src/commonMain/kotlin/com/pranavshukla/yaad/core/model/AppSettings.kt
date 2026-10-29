package com.pranavshukla.yaad.core.model

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

enum class ThemeMode { System, Light, Dark }

/**
 * The user's choices in Settings.
 *
 * @property allDayTime when all-day items (bills, birthdays, expiries) alert on their day.
 * @property quietStart and [quietEnd]: alerts wait during these hours (except high priority); null for none.
 * @property hideAmounts keep amounts out of notifications, which show on the lock screen.
 * @property appLock ask for the device's biometrics or PIN when Yaad opens.
 */
data class AppSettings(
    val allDayTime: LocalTime = LocalTime(9, 0),
    val quietStart: LocalTime? = null,
    val quietEnd: LocalTime? = null,
    val twentyFourHour: Boolean = false,
    val currency: Currency = Currency.INR,
    val theme: ThemeMode = ThemeMode.System,
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val hideAmounts: Boolean = false,
    val appLock: Boolean = false,
)
