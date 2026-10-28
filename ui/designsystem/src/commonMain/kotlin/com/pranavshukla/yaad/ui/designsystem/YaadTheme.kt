package com.pranavshukla.yaad.ui.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * The Yaad theme. On Android 12 and later [dynamicColor] takes the colours from the wallpaper; elsewhere, and when
 * it is off, Yaad's own teal palette is used.
 */
@Composable
fun YaadTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val scheme = (if (dynamicColor) platformDynamicColorScheme(darkTheme) else null)
        ?: if (darkTheme) YaadDarkColors else YaadLightColors
    CompositionLocalProvider(LocalStatusColors provides statusColors(scheme, darkTheme)) {
        MaterialTheme(colorScheme = scheme, typography = yaadTypography(), content = content)
    }
}

/** Yaad-specific theme values that Material 3 has no slot for. */
object YaadTheme {
    val statusColors: StatusColors
        @Composable @ReadOnlyComposable get() = LocalStatusColors.current
}

/** The wallpaper-based colour scheme, or null where the platform has none. */
@Composable
internal expect fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme?
