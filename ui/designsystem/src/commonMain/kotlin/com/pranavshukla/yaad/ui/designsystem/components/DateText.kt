package com.pranavshukla.yaad.ui.designsystem.components

import androidx.compose.runtime.Composable
import com.pranavshukla.yaad.ui.designsystem.resources.Res
import com.pranavshukla.yaad.ui.designsystem.resources.date_day_month
import com.pranavshukla.yaad.ui.designsystem.resources.date_day_month_year
import com.pranavshukla.yaad.ui.designsystem.resources.months_short
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

/** "4 Nov", or "4 Nov 2027" outside [today]'s year. */
@Composable
fun dateText(date: LocalDate, today: LocalDate): String {
    val month = stringArrayResource(Res.array.months_short)[date.month.number - 1]
    return if (date.year == today.year) {
        stringResource(Res.string.date_day_month, date.day, month)
    } else {
        stringResource(Res.string.date_day_month_year, date.day, month, date.year)
    }
}
