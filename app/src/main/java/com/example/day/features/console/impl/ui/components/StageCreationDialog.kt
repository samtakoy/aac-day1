package com.example.day.features.console.impl.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.day.features.console.impl.ui.viewmodel.ConsoleViewModel

/**
 * Dialog for confirming stage creation in PLANNER groups.
 * Shows when agent suggests creating a new stage (CREATE_STAGE pattern detected).
 * 
 * @param state nullable StageCreationSuggestion - null means don't show dialog
 */
@Composable
internal fun StageCreationDialog(
    state: ConsoleViewModel.StageCreationSuggestion?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Don't show anything if state is null
    if (state == null) return
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Создание этапа проекта")
        },
        text = {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Агент предлагает создать новый этап:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.stageTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (state.workingSummary.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Контекст:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = state.workingSummary.take(200) + if (state.workingSummary.length > 200) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Создать этап")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
