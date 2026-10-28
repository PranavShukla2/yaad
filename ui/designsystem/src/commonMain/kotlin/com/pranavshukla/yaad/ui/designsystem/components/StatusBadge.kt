package com.pranavshukla.yaad.ui.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pranavshukla.yaad.ui.designsystem.YaadTheme
import com.pranavshukla.yaad.ui.designsystem.resources.Res
import com.pranavshukla.yaad.ui.designsystem.resources.status_done
import com.pranavshukla.yaad.ui.designsystem.resources.status_due_today
import com.pranavshukla.yaad.ui.designsystem.resources.status_overdue
import com.pranavshukla.yaad.ui.designsystem.resources.status_upcoming
import org.jetbrains.compose.resources.stringResource

/** Where an item stands. Screens get it from `core:scheduling`; the design system only draws it. */
enum class DueStatus { Overdue, DueToday, Upcoming, Done }

@Composable
fun DueStatus.label(): String = stringResource(
    when (this) {
        DueStatus.Overdue -> Res.string.status_overdue
        DueStatus.DueToday -> Res.string.status_due_today
        DueStatus.Upcoming -> Res.string.status_upcoming
        DueStatus.Done -> Res.string.status_done
    },
)

@Composable
fun DueStatus.color(): Color = with(YaadTheme.statusColors) {
    when (this@color) {
        DueStatus.Overdue -> overdue
        DueStatus.DueToday -> dueToday
        DueStatus.Upcoming -> upcoming
        DueStatus.Done -> done
    }
}

/** A small tinted label such as "Overdue". Status is spelled out, never shown by colour alone. */
@Composable
fun StatusBadge(status: DueStatus, modifier: Modifier = Modifier) {
    val color = status.color()
    Text(
        text = status.label(),
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
