package com.pranavshukla.yaad.core.finance

import com.pranavshukla.yaad.core.model.Currency
import com.pranavshukla.yaad.core.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals

class MoneyFormatTest {
    @Test
    fun indianGrouping() {
        assertEquals("₹1,23,45,678.90", MoneyFormat.format(Money(1_234_567_890, Currency.INR)))
        assertEquals("₹8,678.23", MoneyFormat.format(Money(867_823, Currency.INR)))
        assertEquals("₹999.00", MoneyFormat.format(Money(99_900, Currency.INR)))
        assertEquals("₹0.05", MoneyFormat.format(Money(5, Currency.INR)))
        assertEquals("−₹1,00,000.00", MoneyFormat.format(Money(-10_000_000, Currency.INR)))
    }

    @Test
    fun internationalGroupingAndOtherCurrencies() {
        assertEquals("$1,234,567.89", MoneyFormat.format(Money(123_456_789, Currency.USD), Grouping.International))
        assertEquals("€12.50", MoneyFormat.format(Money(1_250, Currency.EUR), Grouping.International))
        assertEquals("¥1,500", MoneyFormat.format(Money(1_500, Currency.JPY), Grouping.International))
        assertEquals("AED 99.00", MoneyFormat.format(Money(9_900, Currency.AED), Grouping.International))
    }

    @Test
    fun wholeRupeesWhenAsked() {
        assertEquals("₹8,678", MoneyFormat.format(Money(867_823, Currency.INR), showMinor = false))
        assertEquals("₹8,679", MoneyFormat.format(Money(867_850, Currency.INR), showMinor = false))
    }

    @Test
    fun parsingAmountsTypedByPeople() {
        assertEquals(Money(1_500_000, Currency.INR), Money.parse("15000", Currency.INR))
        assertEquals(Money(1_500_050, Currency.INR), Money.parse("15,000.5", Currency.INR))
        assertEquals(Money(867_823, Currency.INR), Money.parse("8678.23", Currency.INR))
    }
}
