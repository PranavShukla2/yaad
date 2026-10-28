package com.pranavshukla.yaad.ui.designsystem.adaptive

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WidthClassTest {
    @Test
    fun phonesAreCompact() {
        assertEquals(WidthClass.Compact, WidthClass.of(360.dp))
        assertEquals(WidthClass.Compact, WidthClass.of(599.dp))
    }

    @Test
    fun sixHundredDpStartsTheTwoPaneLayout() {
        assertEquals(WidthClass.Medium, WidthClass.of(600.dp))
        assertEquals(WidthClass.Medium, WidthClass.of(839.dp))
        assertFalse(WidthClass.Compact.isTwoPane)
        assertTrue(WidthClass.Medium.isTwoPane)
    }

    @Test
    fun iPadLandscapeIsExpanded() {
        assertEquals(WidthClass.Expanded, WidthClass.of(840.dp))
        assertEquals(WidthClass.Expanded, WidthClass.of(1366.dp))
        assertTrue(WidthClass.Expanded.isTwoPane)
    }
}
