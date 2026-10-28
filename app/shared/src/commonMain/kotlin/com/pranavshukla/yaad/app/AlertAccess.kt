package com.pranavshukla.yaad.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pranavshukla.yaad.app.resources.Res
import com.pranavshukla.yaad.app.resources.access_allow
import com.pranavshukla.yaad.app.resources.access_exact_denied
import com.pranavshukla.yaad.app.resources.access_notifications_off
import com.pranavshukla.yaad.app.resources.access_turn_on
import org.jetbrains.compose.resources.stringResource

/** What the platform lets Yaad do. Anything missing is explained on screen, never silently degraded. */
data class AlertAccess(val notifications: Boolean = true, val exactAlarms: Boolean = true)

@Composable
internal fun AlertAccessBanner(access: AlertAccess, onFix: () -> Unit, modifier: Modifier = Modifier) {
    val (message, action) = when {
        !access.notifications -> Res.string.access_notifications_off to Res.string.access_turn_on
        !access.exactAlarms -> Res.string.access_exact_denied to Res.string.access_allow
        else -> return
    }
    Card(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Column(Modifier.padding(start = 16.dp, top = 16.dp, end = 8.dp, bottom = 4.dp)) {
            Text(stringResource(message), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onFix, modifier = Modifier.align(Alignment.End)) { Text(stringResource(action)) }
        }
    }
}
