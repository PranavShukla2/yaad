package com.pranavshukla.yaad.data

import com.pranavshukla.yaad.core.model.AppSettings
import com.pranavshukla.yaad.core.model.Currency
import com.pranavshukla.yaad.core.model.ThemeMode
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

/** [AppSettings] as the key-value text in the settings table; a missing or unreadable value keeps its default. */
object SettingsCodec {
    private val currencies = listOf(Currency.INR, Currency.USD, Currency.EUR, Currency.GBP, Currency.AED, Currency.JPY)

    fun encode(settings: AppSettings): Map<String, String> = buildMap {
        put("allDayTime", settings.allDayTime.toString())
        put("quietStart", settings.quietStart?.toString().orEmpty())
        put("quietEnd", settings.quietEnd?.toString().orEmpty())
        put("twentyFourHour", settings.twentyFourHour.toString())
        put("currency", settings.currency.code)
        put("theme", settings.theme.name)
        put("firstDayOfWeek", settings.firstDayOfWeek.name)
        put("hideAmounts", settings.hideAmounts.toString())
        put("appLock", settings.appLock.toString())
    }

    fun decode(values: Map<String, String>): AppSettings {
        val d = AppSettings()
        fun time(key: String) = values[key]?.takeIf { it.isNotEmpty() }?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
        fun flag(key: String, default: Boolean) = values[key]?.toBooleanStrictOrNull() ?: default
        return AppSettings(
            allDayTime = time("allDayTime") ?: d.allDayTime,
            quietStart = time("quietStart"),
            quietEnd = time("quietEnd"),
            twentyFourHour = flag("twentyFourHour", d.twentyFourHour),
            currency = currencies.firstOrNull { it.code == values["currency"] } ?: d.currency,
            theme = ThemeMode.entries.firstOrNull { it.name == values["theme"] } ?: d.theme,
            firstDayOfWeek = DayOfWeek.entries.firstOrNull { it.name == values["firstDayOfWeek"] } ?: d.firstDayOfWeek,
            hideAmounts = flag("hideAmounts", d.hideAmounts),
            appLock = flag("appLock", d.appLock),
        )
    }
}
