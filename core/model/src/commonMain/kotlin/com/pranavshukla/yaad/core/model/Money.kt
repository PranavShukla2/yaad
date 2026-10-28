package com.pranavshukla.yaad.core.model

/** A currency and how many minor units it has (2 for paise and cents, 0 for yen). */
data class Currency(val code: String, val minorDigits: Int) {
    init {
        require(minorDigits in 0..3) { "Minor digits must be 0–3, was $minorDigits" }
    }

    /** 10 ^ [minorDigits]: minor units in one major unit. */
    val minorPerMajor: Long get() = pow10(minorDigits)

    companion object {
        val INR = Currency("INR", 2)
        val USD = Currency("USD", 2)
        val EUR = Currency("EUR", 2)
        val GBP = Currency("GBP", 2)
        val AED = Currency("AED", 2)
        val JPY = Currency("JPY", 0)
    }
}

/**
 * An exact amount in [currency]'s minor units (paise for rupees). Never a floating-point number: arithmetic is on
 * [Long] and fails with [ArithmeticException] rather than overflowing. Amounts in different currencies are never
 * combined.
 */
data class Money(val minorUnits: Long, val currency: Currency) : Comparable<Money> {
    val isZero: Boolean get() = minorUnits == 0L
    val isNegative: Boolean get() = minorUnits < 0L

    operator fun plus(other: Money): Money = Money(addExact(minorUnits, sameCurrency(other)), currency)

    operator fun minus(other: Money): Money = Money(subtractExact(minorUnits, sameCurrency(other)), currency)

    operator fun times(count: Int): Money = Money(multiplyExact(minorUnits, count.toLong()), currency)

    operator fun unaryMinus(): Money = Money(subtractExact(0, minorUnits), currency)

    override fun compareTo(other: Money): Int = minorUnits.compareTo(sameCurrency(other))

    private fun sameCurrency(other: Money): Long {
        require(other.currency == currency) { "Cannot combine ${currency.code} with ${other.currency.code}" }
        return other.minorUnits
    }

    companion object {
        fun zero(currency: Currency) = Money(0, currency)

        /**
         * An amount as people type it: "15000", "15,000.5", "-8678.23". Commas and spaces are ignored; at most the
         * currency's minor digits after the point.
         */
        fun parse(text: String, currency: Currency): Money {
            val clean = text.trim().replace(",", "").replace(" ", "")
            val negative = clean.startsWith("-")
            val digits = clean.removePrefix("-")
            val parts = digits.split('.')
            require(parts.size in 1..2 && parts[0].isNotEmpty() && parts.all { p -> p.all(Char::isDigit) }) { "Not an amount: '$text'" }
            val fraction = parts.getOrElse(1) { "" }
            require(fraction.length <= currency.minorDigits) { "Too many decimals for ${currency.code}: '$text'" }
            val minor = addExact(
                multiplyExact(parts[0].toLong(), currency.minorPerMajor),
                fraction.padEnd(currency.minorDigits, '0').ifEmpty { "0" }.toLong(),
            )
            return Money(if (negative) -minor else minor, currency)
        }

        /** [major] whole units and [minor] minor units, e.g. `of(8_678, 23, INR)` is ₹8,678.23. */
        fun of(major: Long, minor: Int, currency: Currency): Money {
            require(minor >= 0 && minor < currency.minorPerMajor) {
                "Minor part must be below ${currency.minorPerMajor}, was $minor"
            }
            val sign = if (major < 0) -1L else 1L
            return Money(addExact(multiplyExact(major, currency.minorPerMajor), sign * minor), currency)
        }
    }
}

private fun pow10(exponent: Int): Long {
    var result = 1L
    repeat(exponent) { result *= 10 }
    return result
}

internal fun addExact(a: Long, b: Long): Long {
    val result = a + b
    if ((a xor result) and (b xor result) < 0) throw ArithmeticException("Amount overflow: $a + $b")
    return result
}

internal fun subtractExact(a: Long, b: Long): Long {
    val result = a - b
    if ((a xor b) and (a xor result) < 0) throw ArithmeticException("Amount overflow: $a - $b")
    return result
}

internal fun multiplyExact(a: Long, b: Long): Long {
    val result = a * b
    if (a != 0L && (result / a != b || (a == -1L && b == Long.MIN_VALUE))) {
        throw ArithmeticException("Amount overflow: $a × $b")
    }
    return result
}
