package com.pranavshukla.yaad.ui.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * One reminder, bill or EMI in a list. Every value arrives already formatted: due text and amounts come from tested
 * functions in `core:*`, never from the UI.
 */
@Composable
fun ItemCard(
    title: String,
    dueText: String,
    status: DueStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    amountText: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The text reads as one item; a trailing button stays separately focusable.
            Column(
                modifier = Modifier.weight(1f).semantics(mergeDescendants = true) {},
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (supportingText != null) {
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(status)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = dueText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (amountText != null) {
                Spacer(Modifier.width(12.dp))
                AmountText(amountText, style = MaterialTheme.typography.titleMedium)
            }
            if (trailing != null) {
                Spacer(Modifier.width(4.dp))
                trailing()
            }
        }
    }
}
