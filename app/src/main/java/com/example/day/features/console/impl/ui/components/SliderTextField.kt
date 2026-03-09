package com.example.day.features.console.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.day.core.ui.uikit.chat.ChatColorScheme
import com.example.day.core.ui.uikit.chat.LocalChatColorScheme
import com.example.day.core.ui.uikit.components.HelpIconButton

/**
 * A composable that combines a text field with a slider for numeric input.
 * Supports nullable values - empty field means null.
 *
 * @param label The label to display above the field
 * @param value The current value (null = empty field)
 * @param onValueChange Callback when value changes
 * @param min Minimum value for slider
 * @param max Maximum value for slider
 * @param modifier Modifier for the composable
 * @param colors Chat color scheme
 * @param decimalPlaces Number of decimal places for display (0 for integers)
 */
@Composable
fun SliderTextField(
    label: String,
    value: Double?,
    onValueChange: (Double?) -> Unit,
    min: Double,
    max: Double,
    modifier: Modifier = Modifier,
    colors: ChatColorScheme = LocalChatColorScheme.current,
    decimalPlaces: Int = 2,
    helpDescription: String? = null
) {
    var textValue by remember(value) {
        mutableStateOf(value?.let { formatDouble(it, decimalPlaces) } ?: "")
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.inputBackground)
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                BasicTextField(
                    value = textValue,
                    onValueChange = { newText ->
                        textValue = newText
                        val parsed = newText.toDoubleOrNull()
                        onValueChange(parsed)
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
                                    text = "null",
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

            // Slider - Material3 Slider uses Float
            Slider(
                value = (value ?: min).toFloat(),
                onValueChange = { newValue ->
                    val doubleValue = newValue.toDouble()
                    textValue = formatDouble(doubleValue, decimalPlaces)
                    onValueChange(doubleValue)
                },
                modifier = Modifier.weight(1.5f),
                valueRange = min.toFloat()..max.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = colors.sendButtonEnabled,
                    activeTrackColor = colors.sendButtonEnabled,
                    inactiveTrackColor = colors.inputBackground
                )
            )
        }
    }
}

/**
 * Overload for integer values
 */
@Composable
fun SliderTextField(
    label: String,
    value: Int?,
    onValueChange: (Int?) -> Unit,
    min: Int,
    max: Int,
    modifier: Modifier = Modifier,
    colors: ChatColorScheme = LocalChatColorScheme.current,
    helpDescription: String? = null
) {
    var textValue by remember(value) {
        mutableStateOf(value?.toString() ?: "")
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.inputBackground)
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                BasicTextField(
                    value = textValue,
                    onValueChange = { newText ->
                        textValue = newText.filter { it.isDigit() || it == '-' }
                        val parsed = textValue.toIntOrNull()
                        onValueChange(parsed)
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
                                    text = "null",
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

            // Slider
            Slider(
                value = (value ?: min).toFloat(),
                onValueChange = { newValue ->
                    val intValue = newValue.toInt()
                    textValue = intValue.toString()
                    onValueChange(intValue)
                },
                modifier = Modifier.weight(1.5f),
                valueRange = min.toFloat()..max.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = colors.sendButtonEnabled,
                    activeTrackColor = colors.sendButtonEnabled,
                    inactiveTrackColor = colors.inputBackground
                )
            )
        }
    }
}

private fun formatDouble(value: Double, decimalPlaces: Int): String {
    return if (decimalPlaces == 0) {
        value.toInt().toString()
    } else {
        String.format("%.${decimalPlaces}f", value)
    }
}
