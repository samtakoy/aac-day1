package com.example.day.core.ui.uikit.dialogs.group

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.day.core.core_features.chat.domain.model.ChatType

@Composable
fun GroupEditDialog(
    state: GroupEditDialogState,
    onTitleChange: (String) -> Unit,
    onTypeChange: (ChatType) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(state.title) }
    var selectedType by remember { mutableStateOf(state.selectedType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onTitleChange(title)
                onTypeChange(selectedType)
                onConfirm()
            }) {
                Text(text = if (state.isCreateMode) "Создать" else "Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Отмена")
            }
        },
        title = { Text(text = if (state.isCreateMode) "Создать группу" else "Редактировать группу") },
        text = {
            Column(modifier = Modifier.padding(horizontal = 0.dp)) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название группы") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Тип чата",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )

                Column {
                    enumValues<ChatType>().forEach { type ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedType = type }
                                .padding(vertical = 4.dp),
                            ) {
                            RadioButton(
                                selected = selectedType == type,
                                onClick = { selectedType = type }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = type.title)
                        }
                        Divider()
                    }
                }
            }
        }
    )
}
