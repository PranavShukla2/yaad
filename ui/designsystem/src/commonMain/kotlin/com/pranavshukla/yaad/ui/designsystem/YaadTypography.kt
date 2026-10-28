package com.pranavshukla.yaad.ui.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle

/** Tabular figures: digits share one width, so amounts and times line up in columns. */
private const val TabularFigures = "tnum"

private fun TextStyle.tabular() = copy(fontFeatureSettings = TabularFigures)

/** Material 3's type scale with tabular figures on the styles that show amounts, dates and counts. */
internal fun yaadTypography(): Typography {
    val base = Typography()
    return base.copy(
        displaySmall = base.displaySmall.tabular(),
        headlineLarge = base.headlineLarge.tabular(),
        headlineMedium = base.headlineMedium.tabular(),
        headlineSmall = base.headlineSmall.tabular(),
        titleLarge = base.titleLarge.tabular(),
        titleMedium = base.titleMedium.tabular(),
        bodyLarge = base.bodyLarge.tabular(),
        bodyMedium = base.bodyMedium.tabular(),
        labelLarge = base.labelLarge.tabular(),
        labelMedium = base.labelMedium.tabular(),
    )
}
