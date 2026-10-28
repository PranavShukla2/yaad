package com.pranavshukla.yaad.ui.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Yaad's own palette: a calm teal with a warm amber for money. Used where dynamic colour is unavailable. */
internal val YaadLightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF0F6A73),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA4EEF5),
    onPrimaryContainer = Color(0xFF002023),
    secondary = Color(0xFF4A6366),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8EB),
    onSecondaryContainer = Color(0xFF051F22),
    tertiary = Color(0xFF8A5100),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCBE),
    onTertiaryContainer = Color(0xFF2C1600),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF5FAFA),
    onBackground = Color(0xFF171D1D),
    surface = Color(0xFFF5FAFA),
    onSurface = Color(0xFF171D1D),
    surfaceVariant = Color(0xFFDAE4E5),
    onSurfaceVariant = Color(0xFF3F4849),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF5F5),
    surfaceContainer = Color(0xFFE9EFEF),
    surfaceContainerHigh = Color(0xFFE3E9E9),
    surfaceContainerHighest = Color(0xFFDEE3E4),
    outline = Color(0xFF6F7979),
    outlineVariant = Color(0xFFBEC8C9),
)

internal val YaadDarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF82D3DC),
    onPrimary = Color(0xFF00363B),
    primaryContainer = Color(0xFF004F56),
    onPrimaryContainer = Color(0xFFA4EEF5),
    secondary = Color(0xFFB1CBCE),
    onSecondary = Color(0xFF1B3437),
    secondaryContainer = Color(0xFF324B4E),
    onSecondaryContainer = Color(0xFFCCE8EB),
    tertiary = Color(0xFFFFB870),
    onTertiary = Color(0xFF4A2800),
    tertiaryContainer = Color(0xFF693C00),
    onTertiaryContainer = Color(0xFFFFDCBE),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0E1415),
    onBackground = Color(0xFFDEE3E4),
    surface = Color(0xFF0E1415),
    onSurface = Color(0xFFDEE3E4),
    surfaceVariant = Color(0xFF3F4849),
    onSurfaceVariant = Color(0xFFBEC8C9),
    surfaceContainerLowest = Color(0xFF090F10),
    surfaceContainerLow = Color(0xFF171D1D),
    surfaceContainer = Color(0xFF1B2121),
    surfaceContainerHigh = Color(0xFF252B2C),
    surfaceContainerHighest = Color(0xFF303637),
    outline = Color(0xFF899393),
    outlineVariant = Color(0xFF3F4849),
)

/**
 * Colours for an item's state. Overdue and due-today follow the colour scheme (error and tertiary), so they also work
 * with dynamic colour; "done" has no Material role, so it is a fixed green tuned for each theme.
 */
@Immutable
data class StatusColors(
    val overdue: Color,
    val dueToday: Color,
    val upcoming: Color,
    val done: Color,
)

internal fun statusColors(scheme: ColorScheme, dark: Boolean) = StatusColors(
    overdue = scheme.error,
    dueToday = scheme.tertiary,
    upcoming = scheme.primary,
    done = if (dark) Color(0xFF8BD88F) else Color(0xFF2E6B30),
)

internal val LocalStatusColors = staticCompositionLocalOf { statusColors(YaadLightColors, dark = false) }
