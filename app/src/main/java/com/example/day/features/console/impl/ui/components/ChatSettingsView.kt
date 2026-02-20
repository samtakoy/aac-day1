package com.example.day.features.console.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.tooling.preview.Preview
import com.example.day.features.console.impl.domain.model.ChatSettings
import com.example.day.features.console.impl.domain.model.ModelSettings
import com.example.day.core.ui.uikit.chat.ChatUiColors
import com.example.day.core.ui.uikit.chat.DarkChatUiColors
import com.example.day.core.ui.uikit.chat.LocalChatColors
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.Serializable

/**
 * Chat settings view for configuring LLM parameters
 */
@Composable
fun ChatSettingsView(
    state: ChatSettingsUiModel,
    onSubmit: (ChatSettings) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ChatUiColors = LocalChatColors.current
) {
    val settings = state.settingsState
    val model = state.settingsState.model
    var systemPrompt by remember(settings.systemPromt) { mutableStateOf(settings.systemPromt) }
    var stopWord by remember(model.stopSequence) { mutableStateOf(model.stopSequence.firstOrNull().orEmpty()) }
    var temperature by remember(model.temperature) { mutableStateOf(model.temperature.toString()) }
    var reasoningEffort by remember(model.reasoningEffort) { mutableStateOf(model.reasoningEffort.toString()) }
    var jsonFormat by remember(model.jsonFormat) { mutableStateOf(model.jsonFormat) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
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

        // Stop Word field (smaller text field)
        SettingsTextField(
            label = "Stop Word",
            value = stopWord,
            onValueChange = { stopWord = it },
            placeholder = "Enter stop word",
            modifier = Modifier.fillMaxWidth(),
            colors = colors
        )

        // Max Tokens field (small numeric field)
        SettingsTextField(
            label = "Temperature",
            value = temperature,
            onValueChange = { newValue ->
                temperature = newValue
            },
            placeholder = "e.g., 1000",
            modifier = Modifier.fillMaxWidth(),
            keyboardType = KeyboardType.Number,
            colors = colors
        )

        //
        SettingsTextField(
            label = "Reasoning effort",
            value = reasoningEffort,
            onValueChange = { reasoningEffort = it },
            placeholder = "xhigh|high|medium|low|minimal|none",
            modifier = Modifier.fillMaxWidth(),
            colors = colors
        )

        // JSON Format checkbox
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.inputBackground)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Checkbox(
                checked = jsonFormat,
                onCheckedChange = { jsonFormat = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = colors.sendButtonEnabled,
                    uncheckedColor = colors.inputPlaceholder
                )
            )
            Text(
                text = "Response in JSON format",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.botText,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                    val temperature = temperature.toDoubleOrNull() ?: settings.model.temperature
                    onSubmit(
                        settings.copy(
                            systemPromt = systemPrompt,
                            model = ModelSettings(
                                name = state.settingsState.model.name,
                                stopSequence = buildList {
                                    if (stopWord.isNotBlank()) add(stopWord)
                                }.toImmutableList(),
                                jsonFormat = jsonFormat,
                                temperature = temperature,
                                reasoningEffort = reasoningEffort
                            )
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

/**
 * Reusable text field for settings
 */
@Composable
private fun SettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    maxLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    colors: ChatUiColors = LocalChatColors.current
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.botText
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.inputBackground)
                .then(
                    if (minLines > 1) {
                        Modifier.padding(12.dp)
                    } else {
                        Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                    }
                )
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = colors.inputText
                ),
                cursorBrush = SolidColor(colors.sendButtonEnabled),
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = ImeAction.Next
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.inputPlaceholder
                            )
                        }
                        innerTextField()
                    }
                },
                minLines = minLines,
                maxLines = maxLines
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatSettingsViewPreview() {
    val testSettings = ChatSettings(
        chatId = 1L,
        systemPromt = "You are a ¡ assistant.",
        model = ModelSettings(
            name = "name",
            stopSequence = listOf("END").toImmutableList(),
            maxTokens = 1000,
            jsonFormat = false
        )
    )

    Dialog(onDismissRequest = {}) {
        ChatSettingsView(
            state = ChatSettingsUiModel("title", testSettings),
            onSubmit = { settings ->
                println("Submit: $settings")
            },
            onCancel = {
                println("Cancel clicked")
            },
            colors = DarkChatUiColors
        )
    }
}
