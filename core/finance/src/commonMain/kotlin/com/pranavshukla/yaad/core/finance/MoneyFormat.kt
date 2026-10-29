package com.pranavshukla.yaad.core.finance

import com.pranavshukla.yaad.core.model.Money

/** Indian grouping puts commas after the first three digits and then every two: 1,23,45,678. */
enum class Grouping { Indian, International }

object MoneyFormat {
    private val symbols = mapOf("INR" to "₹", "USD" to "$", "EUR" to "€", "GBP" to "£", "JPY" to "¥")

    /** "₹1,23,456.00"; with [showMinor] false, rounded half up to whole units ("₹8,678"). */
    fun format(money: Money, grouping: Grouping = Grouping.Indian, showMinor: Boolean = true): String {
        val digits = money.currency.minorDigits
        val perMajor = money.currency.minorPerMajor
        val absolute = if (money.minorUnits < 0) -money.minorUnits else money.minorUnits
        val (major, minor) = if (showMinor || digits == 0) {
            absolute / perMajor to absolute % perMajor
        } else {
            divideHalfUp(absolute, perMajor) to 0L
        }
        val symbol = symbols[money.currency.code] ?: "${money.currency.code} "
        val number = group(major.toString(), grouping) +
            if (showMinor && digits > 0) "." + minor.toString().padStart(digits, '0') else ""
        return (if (money.minorUnits < 0) "−" else "") + symbol + number
    }

    private fun group(digits: String, grouping: Grouping): String {
        if (digits.length <= 3) return digits
        val head = digits.dropLast(3)
        val tail = digits.takeLast(3)
        val size = if (grouping == Grouping.Indian) 2 else 3
        return head.reversed().chunked(size).joinToString(",").reversed() + "," + tail
    }
}
