package com.pranavshukla.yaad.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AnnualRateTest {
    @Test
    fun ratesAreReadExactly() {
        assertEquals(AnnualRate(8500), AnnualRate.percent("8.5"))
        assertEquals(AnnualRate(8375), AnnualRate.percent("8.375"))
        assertEquals(AnnualRate(12000), AnnualRate.percent("12"))
        assertEquals(AnnualRate(0), AnnualRate.percent("0"))
    }

    @Test
    fun badRatesAreRejected() {
        for (text in listOf("", "8.5%", "-1", "8.3755", "abc", "101")) {
            assertFailsWith<IllegalArgumentException>(text) { AnnualRate.percent(text) }
        }
    }
}
