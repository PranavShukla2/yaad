package com.pranavshukla.yaad.ui.designsystem

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.datetime.DayOfWeek

/** The user's 24-hour clock choice, for every time shown on screen. */
val LocalTwentyFourHour = staticCompositionLocalOf { false }

/** The first column of calendar grids. */
val LocalFirstDayOfWeek = staticCompositionLocalOf { DayOfWeek.MONDAY }
