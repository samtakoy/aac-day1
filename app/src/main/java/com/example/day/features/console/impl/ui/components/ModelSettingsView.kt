package com.example.day.features.console.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.day.core.core_features.llm.domain.model.ModelSettings
import com.example.day.core.ui.uikit.chat.ChatColorScheme
import com.example.day.core.ui.uikit.chat.LocalChatColorScheme
import com.example.day.core.ui.uikit.components.ModelParameterDescriptions
import com.example.day.core.ui.uikit.components.NullableIntField
import com.example.day.core.ui.uikit.components.SettingsTextField
import kotlinx.collections.immutable.toImmutableList

/**
 * A composable for configuring ModelSettings parameters.
 * Provides UI for all model parameters including sliders for numeric values.
 *
 * @param modelSettings The current model settings
 * @param onModelSettingsChange Callback when settings change
 * @param modifier Modifier for the composable
 * @param colors Chat color scheme
 */
@Composable
fun ModelSettingsView(
    modelSettings: ModelSettings,
    onModelSettingsChange: (ModelSettings) -> Unit,
    modifier: Modifier = Modifier,
    colors: ChatColorScheme = LocalChatColorScheme.current
) {
    var modelName by remember(modelSettings.name) { mutableStateOf(modelSettings.name) }
    var stopWord by remember(modelSettings.stopSequence) { 
        mutableStateOf(modelSettings.stopSequence.firstOrNull().orEmpty()) 
    }
    var jsonFormat by remember(modelSettings.jsonFormat) { mutableStateOf(modelSettings.jsonFormat) }
    var isLocal by remember(modelSettings.isLocal) { mutableStateOf(modelSettings.isLocal) }
    var reasoningEffort by remember(modelSettings.reasoningEffort) { 
        mutableStateOf(modelSettings.reasoningEffort.orEmpty()) 
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Model Name field
        SettingsTextField(
            label = "Model Name",
            value = modelName,
            onValueChange = { newName ->
                modelName = newName
                onModelSettingsChange(modelSettings.copy(name = newName))
            },
            placeholder = "e.g., openai/gpt-4",
            modifier = Modifier.fillMaxWidth(),
            colors = colors,
            helpDescription = ModelParameterDescriptions.MODEL_NAME
        )

        // Temperature slider (0-2)
        SliderTextField(
            label = "Temperature",
            value = modelSettings.temperature,
            onValueChange = { newTemp ->
                onModelSettingsChange(modelSettings.copy(temperature = newTemp))
            },
            min = 0.0,
            max = 2.0,
            modifier = Modifier.fillMaxWidth(),
            colors = colors,
            decimalPlaces = 2,
            helpDescription = ModelParameterDescriptions.TEMPERATURE
        )

        // Top P slider (0-1)
        SliderTextField(
            label = "Top P",
            value = modelSettings.topP,
            onValueChange = { newTopP ->
                onModelSettingsChange(modelSettings.copy(topP = newTopP))
            },
            min = 0.0,
            max = 1.0,
            modifier = Modifier.fillMaxWidth(),
            colors = colors,
            decimalPlaces = 2,
            helpDescription = ModelParameterDescriptions.TOP_P
        )

        // Top K slider (1-100)
        SliderTextField(
            label = "Top K",
            value = modelSettings.topK,
            onValueChange = { newTopK ->
                onModelSettingsChange(modelSettings.copy(topK = newTopK))
            },
            min = 1,
            max = 100,
            modifier = Modifier.fillMaxWidth(),
            colors = colors,
            helpDescription = ModelParameterDescriptions.TOP_K
        )

        // Max Tokens field
        NullableIntField(
            label = "Max Tokens",
            value = modelSettings.maxTokens,
            onValueChange = { newMaxTokens ->
                onModelSettingsChange(modelSettings.copy(maxTokens = newMaxTokens))
            },
            placeholder = "null",
            modifier = Modifier.fillMaxWidth(),
            colors = colors,
            helpDescription = ModelParameterDescriptions.MAX_TOKENS
        )

        // Max Completion Tokens field
        NullableIntField(
            label = "Max Completion Tokens",
            value = modelSettings.maxCompletionTokens,
            onValueChange = { newMaxCompletionTokens ->
                onModelSettingsChange(modelSettings.copy(maxCompletionTokens = newMaxCompletionTokens))
            },
            placeholder = "null",
            modifier = Modifier.fillMaxWidth(),
            colors = colors,
            helpDescription = ModelParameterDescriptions.MAX_COMPLETION_TOKENS
        )

        // Presence Penalty slider (-2 to 2)
        SliderTextField(
            label = "Presence Penalty",
            value = modelSettings.presencePenalty,
            onValueChange = { newPenalty ->
                onModelSettingsChange(modelSettings.copy(presencePenalty = newPenalty))
            },
            min = -2.0,
            max = 2.0,
            modifier = Modifier.fillMaxWidth(),
            colors = colors,
            decimalPlaces = 2,
            helpDescription = ModelParameterDescriptions.PRESENCE_PENALTY
        )

        // Frequency Penalty slider (-2 to 2)
        SliderTextField(
            label = "Frequency Penalty",
            value = modelSettings.frequencyPenalty,
            onValueChange = { newPenalty ->
                onModelSettingsChange(modelSettings.copy(frequencyPenalty = newPenalty))
            },
            min = -2.0,
            max = 2.0,
            modifier = Modifier.fillMaxWidth(),
            colors = colors,
            decimalPlaces = 2,
            helpDescription = ModelParameterDescriptions.FREQUENCY_PENALTY
        )

        // Seed field
        NullableIntField(
            label = "Seed",
            value = modelSettings.seed,
            onValueChange = { newSeed ->
                onModelSettingsChange(modelSettings.copy(seed = newSeed))
            },
            placeholder = "null",
            modifier = Modifier.fillMaxWidth(),
            colors = colors,
            helpDescription = ModelParameterDescriptions.SEED
        )

        // Stop Word field
        SettingsTextField(
            label = "Stop Word",
            value = stopWord,
            onValueChange = { newStopWord ->
                stopWord = newStopWord
                onModelSettingsChange(
                    modelSettings.copy(
                        stopSequence = buildList {
                            if (newStopWord.isNotBlank()) add(newStopWord)
                        }.toImmutableList()
                    )
                )
            },
            placeholder = "Enter stop word",
            modifier = Modifier.fillMaxWidth(),
            colors = colors,
            helpDescription = ModelParameterDescriptions.STOP_SEQUENCE
        )

        // Reasoning Effort field
        SettingsTextField(
            label = "Reasoning Effort",
            value = reasoningEffort,
            onValueChange = { newReasoning ->
                reasoningEffort = newReasoning
                onModelSettingsChange(
                    modelSettings.copy(
                        reasoningEffort = newReasoning.ifBlank { null }
                    )
                )
            },
            placeholder = "xhigh|high|medium|low|minimal|none",
            modifier = Modifier.fillMaxWidth(),
            colors = colors,
            helpDescription = ModelParameterDescriptions.REASONING_EFFORT
        )

        // Local LLM (Ollama) checkbox
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
                checked = isLocal,
                onCheckedChange = { newIsLocal ->
                    isLocal = newIsLocal
                    onModelSettingsChange(modelSettings.copy(isLocal = newIsLocal))
                },
                colors = CheckboxDefaults.colors(
                    checkedColor = colors.sendButtonEnabled,
                    uncheckedColor = colors.inputPlaceholder
                )
            )
            Text(
                text = "Локальная LLM (Ollama)",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.botText,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

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
                onCheckedChange = { newJsonFormat ->
                    jsonFormat = newJsonFormat
                    onModelSettingsChange(modelSettings.copy(jsonFormat = newJsonFormat))
                },
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
    }
 }

