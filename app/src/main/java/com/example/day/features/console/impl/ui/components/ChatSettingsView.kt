package com.example.day.features.console.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.day.core.core_features.chat.domain.model.ChatSettings
import com.example.day.core.ui.uikit.chat.ChatColorScheme
import com.example.day.core.ui.uikit.chat.LocalChatColorScheme
import com.example.day.core.ui.uikit.components.SettingsTextField

/**
 * Chat settings view for configuring LLM parameters.
 * Uses ModelSettingsView for model-specific settings.
 * Buttons are fixed at the bottom during scrolling.
 */
@Composable
fun ChatSettingsView(
    state: ChatSettingsUiModel,
    onSubmit: (chatTitle: String, ChatSettings) -> Unit,
    onCancel: () -> Unit,
    onHandlePrToggle: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    colors: ChatColorScheme = LocalChatColorScheme.current
) {
    val settings = state.settingsState
    var chatTitle by remember(state.chatTitle) { mutableStateOf(state.chatTitle) }
    var systemPrompt by remember(settings.systemPromt) { mutableStateOf(settings.systemPromt) }
    var currentModelSettings by remember(settings.model) { mutableStateOf(settings.model) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
    ) {
        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title
            Text(
                text = state.title,
                style = MaterialTheme.typography.titleLarge,
                color = colors.botText
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Chat Title field
            SettingsTextField(
                label = "Название чата",
                value = chatTitle,
                onValueChange = { chatTitle = it },
                placeholder = "Введите название чата...",
                modifier = Modifier.fillMaxWidth(),
                colors = colors
            )

            Spacer(modifier = Modifier.height(8.dp))

            // System Prompt field (larger text field)
            SettingsTextField(
                label = "System Prompt",
                value = systemPrompt,
                onValueChange = { systemPrompt = it },
                placeholder = "Enter system prompt...",
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                colors = colors
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Model Settings Section
            Text(
                text = "Model Settings",
                style = MaterialTheme.typography.titleMedium,
                color = colors.botText
            )

            ModelSettingsView(
                modelSettings = currentModelSettings,
                onModelSettingsChange = { newSettings ->
                    currentModelSettings = newSettings
                },
                modifier = Modifier.fillMaxWidth(),
                colors = colors
            )

            Spacer(modifier = Modifier.height(8.dp))

            // PR Monitoring toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Мониторинг PR",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Автоматическое ревью Pull Request-ов",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.handlePr,
                    onCheckedChange = onHandlePrToggle
                )
            }
        }

        // Fixed buttons at the bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.background)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start)
        ) {
            TextButton(
                onClick = onCancel,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colors.inputPlaceholder
                )
            ) {
                Text("Cancel")
            }

            Button(
                onClick = {
                    onSubmit(
                        chatTitle,
                        settings.copy(
                            systemPromt = systemPrompt,
                            model = currentModelSettings
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.sendButtonEnabled
                )
            ) {
                Text("OK")
            }
        }
    }
}
