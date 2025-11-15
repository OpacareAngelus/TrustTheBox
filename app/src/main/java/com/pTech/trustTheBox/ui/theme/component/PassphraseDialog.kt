package com.pTech.trustTheBox.ui.theme.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.stringResource
import com.pTech.trustTheBox.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassphraseDialog(currentValue: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(currentValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.passphrase_dialog_title), style = MaterialTheme.typography.headlineLarge) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                placeholder = { Text(stringResource(R.string.passphrase_dialog_placeholder), style = MaterialTheme.typography.bodyMedium) },
                supportingText = { Text(stringResource(R.string.passphrase_dialog_supporting), style = MaterialTheme.typography.bodyMedium) }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(text) },
                enabled = text.trim().isNotBlank()
            ) { Text(stringResource(R.string.passphrase_dialog_save), style = MaterialTheme.typography.labelLarge) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.passphrase_dialog_cancel), style = MaterialTheme.typography.labelLarge) }
        }
    )
}