package com.example.day.features.user_settings.impl.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FactEditDialog(
    isNewFact: Boolean,
    factKey: String,
    factCategory: String,
    factValue: String,
    error: String?,
    onKeyChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNewFact) "Добавить факт" else "Редактировать факт") },
        text = {
            Column {
                OutlinedTextField(
                    value = factKey,
                    onValueChange = onKeyChange,
                    label = { Text("Ключ (например: primary_language)") },
                    isError = error != null && factKey.isBlank(),
                    singleLine = true,
                    enabled = isNewFact
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = factCategory,
                    onValueChange = onCategoryChange,
                    label = { Text("Категория (например: skills)") },
                    singleLine = true,
                    placeholder = { Text("general") }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = factValue,
                    onValueChange = onValueChange,
                    label = { Text("Значение") },
                    isError = error != null && factValue.isBlank(),
                    minLines = 2,
                    maxLines = 4
                )
                if (error != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = factKey.isNotBlank() && factValue.isNotBlank()
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
