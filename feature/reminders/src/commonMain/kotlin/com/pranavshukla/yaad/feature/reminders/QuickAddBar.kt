package com.pranavshukla.yaad.feature.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pranavshukla.yaad.core.finance.MoneyFormat
import com.pranavshukla.yaad.core.quickadd.QuickAddParser
import com.pranavshukla.yaad.core.scheduling.TimeLabel
import com.pranavshukla.yaad.feature.reminders.resources.Res
import com.pranavshukla.yaad.feature.reminders.resources.*
import com.pranavshukla.yaad.ui.designsystem.LocalTwentyFourHour
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

const val QuickAddFieldTag = "quick-add"

/**
 * Type a reminder in a line; Yaad shows how it understood it before adding it. "Edit" opens the full editor with
 * everything filled in.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickAddBar(model: RemindersModel, state: RemindersState, onEdit: (EditorState) -> Unit, modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf("") }
    val now = state.now.toLocalDateTime(state.zone)
    val parsed = if (text.isBlank()) null else QuickAddParser.parse(text, now)
    val add: () -> Unit = {
        if (parsed != null && parsed.title.isNotBlank()) {
            model.save(parsed.toReminder(model.newId(), now))
            text = ""
        }
    }
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(stringResource(Res.string.quick_add_hint), maxLines = 2, overflow = TextOverflow.Ellipsis) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { add() }),
            modifier = Modifier.fillMaxWidth().testTag(QuickAddFieldTag),
        )
        if (parsed != null && parsed.title.isNotBlank()) {
            Text(stringResource(Res.string.understood), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AssistChip(onClick = {}, label = { Text(parsed.title) })
                AssistChip(onClick = {}, label = { Text(parsed.date?.let { dayText(it, now.date) } ?: stringResource(Res.string.no_date)) })
                parsed.time?.let { AssistChip(onClick = {}, label = { Text(TimeLabel.format(it, twentyFourHour = LocalTwentyFourHour.current)) }) }
                parsed.rule?.let { AssistChip(onClick = {}, label = { Text(ruleText(it, now.date)) }) }
                parsed.amount?.let { AssistChip(onClick = {}, label = { Text(MoneyFormat.format(it)) }) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = add) { Text(stringResource(Res.string.quick_add)) }
                TextButton(onClick = {
                    onEdit(EditorState.from(parsed.toReminder(model.newId(), now)).copy(base = null))
                    text = ""
                }) { Text(stringResource(Res.string.quick_edit)) }
            }
        }
    }
}
