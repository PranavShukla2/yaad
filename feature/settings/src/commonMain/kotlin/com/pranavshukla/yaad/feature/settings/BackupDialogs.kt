package com.pranavshukla.yaad.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.pranavshukla.yaad.feature.settings.resources.Res
import com.pranavshukla.yaad.feature.settings.resources.*
import org.jetbrains.compose.resources.stringResource

/** Asks for a backup password: twice when making one ([confirm]), once when restoring. */
@Composable
fun BackupPasswordDialog(confirm: Boolean, onDone: (String) -> Unit, onDismiss: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var again by remember { mutableStateOf("") }
    val valid = if (confirm) password.length >= 8 && password == again else password.isNotEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (confirm) Res.string.backup_password_title else Res.string.restore_password_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(if (confirm) Res.string.backup_password_note else Res.string.restore_warning),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    password, { password = it }, label = { Text(stringResource(Res.string.password)) }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                if (confirm) {
                    OutlinedTextField(
                        again, { again = it }, label = { Text(stringResource(Res.string.password_again)) }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                    )
                    Text(stringResource(Res.string.password_rules), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onDone(password) }, enabled = valid) { Text(stringResource(Res.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}

/** A short message after a backup or restore. */
@Composable
fun BackupMessage(text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.ok)) } },
    )
}

@Composable
fun backupMessages(): BackupMessages = BackupMessages(
    saved = stringResource(Res.string.backup_saved),
    restored = stringResource(Res.string.restored),
    failed = stringResource(Res.string.restore_failed, "%s"),
)

data class BackupMessages(val saved: String, val restored: String, val failed: String)
