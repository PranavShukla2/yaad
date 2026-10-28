package com.pranavshukla.yaad.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MoneyTest {
    private fun inr(paise: Long) = Money(paise, Currency.INR)

    @Test
    fun addsAndSubtractsInMinorUnits() {
        assertEquals(inr(867_823), inr(708_333) + inr(159_490))
        assertEquals(inr(-1), inr(10) - inr(11))
        assertEquals(inr(-5), -inr(5))
    }

    @Test
    fun multipliesByWholeCounts() {
        // The plan's worked example: 240 EMIs of ₹8,678.23 come to ₹20,82,775.20.
        assertEquals(inr(208_277_520), inr(867_823) * 240)
        assertEquals(inr(0), inr(15_000_00) * 0)
    }

    @Test
    fun buildsFromMajorAndMinorUnits() {
        assertEquals(inr(867_823), Money.of(8_678, 23, Currency.INR))
        assertEquals(Money(1_500, Currency.JPY), Money.of(1_500, 0, Currency.JPY))
        assertFailsWith<IllegalArgumentException> { Money.of(1, 100, Currency.INR) }
    }

    @Test
    fun refusesToMixCurrencies() {
        assertFailsWith<IllegalArgumentException> { inr(1) + Money(1, Currency.USD) }
        assertFailsWith<IllegalArgumentException> { inr(1) < Money(1, Currency.USD) }
    }

    @Test
    fun overflowFailsInsteadOfWrapping() {
        assertFailsWith<ArithmeticException> { inr(Long.MAX_VALUE) + inr(1) }
        assertFailsWith<ArithmeticException> { inr(Long.MIN_VALUE) - inr(1) }
        assertFailsWith<ArithmeticException> { inr(Long.MAX_VALUE / 2 + 1) * 2 }
        assertFailsWith<ArithmeticException> { -inr(Long.MIN_VALUE) }
    }

    @Test
    fun comparesWithinACurrency() {
        assertTrue(inr(100) < inr(101))
        assertTrue(Money.zero(Currency.INR).isZero)
        assertTrue(inr(-1).isNegative)
    }
}
