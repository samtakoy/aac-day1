package com.example.day.core.ui.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.day.core.ui.uikit.chat.ChatColorScheme
import com.example.day.core.ui.uikit.chat.LocalChatColorScheme

/**
 * Круглая кнопка с вопросительным знаком для отображения справки по параметрам
 */
@Composable
fun HelpIconButton(
    description: String,
    modifier: Modifier = Modifier,
    colors: ChatColorScheme = LocalChatColorScheme.current
) {
    var showDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(colors.inputBackground)
            .clickable { showDialog = true },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "?",
            style = MaterialTheme.typography.labelSmall,
            color = colors.botText
        )
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                color = colors.background
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Справка",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.botText
                    )
                    Spacer(modifier = Modifier.padding(8.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.botText
                    )
                    Spacer(modifier = Modifier.padding(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "OK",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.sendButtonEnabled,
                            modifier = Modifier.clickable { showDialog = false }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Универсальное текстовое поле для настроек
 */
@Composable
fun SettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    maxLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    helpDescription: String? = null,
    colors: ChatColorScheme = LocalChatColorScheme.current
) {
    Column(
        modifier = modifier,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.botText
            )
            if (helpDescription != null) {
                Spacer(modifier = Modifier.width(8.dp))
                HelpIconButton(
                    description = helpDescription,
                    colors = colors
                )
            }
        }

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

/**
 * Текстовое поле для ввода целочисленных nullable значений
 */
@Composable
fun NullableIntField(
    label: String,
    value: Int?,
    onValueChange: (Int?) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    helpDescription: String? = null,
    colors: ChatColorScheme = LocalChatColorScheme.current
) {
    var textValue by remember(value) { mutableStateOf(value?.toString() ?: "") }

    Column(
        modifier = modifier,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.botText
            )
            if (helpDescription != null) {
                Spacer(modifier = Modifier.width(8.dp))
                HelpIconButton(
                    description = helpDescription,
                    colors = colors
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.inputBackground)
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = textValue,
                onValueChange = { newText ->
                    textValue = newText.filter { it.isDigit() }
                    onValueChange(textValue.toIntOrNull())
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = colors.inputText
                ),
                cursorBrush = SolidColor(colors.sendButtonEnabled),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (textValue.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.inputPlaceholder
                            )
                        }
                        innerTextField()
                    }
                },
                singleLine = true
            )
        }
    }
}

/**
 * Описания параметров модели на русском языке (из документации OpenRouter)
 */
object ModelParameterDescriptions {
    const val TEMPERATURE = "Температура генерации. Влияет на степень случайности ответов. Высокие значения (ближе к 2) делают ответы более креативными и разнообразными, низкие (ближе к 0) — более предсказуемыми и сфокусированными. Рекомендуется изменять либо temperature, либо top_p, но не оба."

    const val TOP_P = "Ядерная выборка (Nucleus Sampling). Определяет минимальную совокупную вероятность токенов, из которой выбирается следующий токен. Например, top_p=0.9 означает, что учитываются только токены, суммарная вероятность которых составляет 90%. Рекомендуется изменять либо top_p, либо temperature, но не оба."

    const val TOP_K = "Выбор из k наиболее вероятных токенов. Ограничивает выбор следующего токена k наиболее вероятными вариантами. При top_k=1 всегда выбирается наиболее вероятный токен."

    const val MAX_TOKENS = "Максимальное количество токенов для генерации в каждом ответе. Ограничивает длину ответа модели. Один токен примерно равен 4 символам на английском или 1-2 символам на русском."

    const val MAX_COMPLETION_TOKENS = "Максимальное количество токенов, выделяемых именно для генерации ответа. Полезно, когда нужно ограничить длину ответа независимо от длины промпта."

    const val PRESENCE_PENALTY = "Штраф за присутствие. Положительные значения уменьшают вероятность повторения слов, которые уже встречались в тексте. Значения от -2 до 2."

    const val FREQUENCY_PENALTY = "Штраф за частоту. Положительные значения снижают вероятность часто используемых слов, делая ответ более разнообразным. Значения от -2 до 2."

    const val SEED = "Зерно случайности для воспроизводимости результатов. При одинаковом seed и одинаковом промпте модель должна вернуть одинаковый ответ. Поддерживается не всеми моделями."

    const val REASONING_EFFORT = "Уровень усилий модели для рассуждений. Влияет на баланс между скоростью ответа и глубиной анализа. Доступные значения: xhigh, high, medium, low, minimal, none."

    const val STOP_SEQUENCE = "Стоп-последовательности. При обнаружении этих последовательностей в сгенерированном тексте генерация останавливается."

    const val JSON_FORMAT = "Формат JSON. При включении модель возвращает ответ в формате JSON. Может потребоваться указать схему в системном промпте."

    const val MODEL_NAME = "Имя модели для использования. Укажите модель в формате 'provider/model-name', например 'openai/gpt-4'."
}
