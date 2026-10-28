package com.pranavshukla.yaad.ui.designsystem.adaptive

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Material's window width classes. Phones are [Compact]; iPads and tablets get two panes from 600 dp. */
enum class WidthClass {
    Compact,
    Medium,
    Expanded,
    ;

    val isTwoPane: Boolean get() = this != Compact

    companion object {
        fun of(width: Dp): WidthClass = when {
            width < 600.dp -> Compact
            width < 840.dp -> Medium
            else -> Expanded
        }
    }
}
