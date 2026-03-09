package com.example.day.core.ui.uikit.chat.list

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.day.core.ui.uikit.chat.list.model.ChatMessageUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Компонент группы кнопок сообщения
 * 
 * @param buttons Список кнопок
 * @param onButtonClick Callback клика кнопки
 * @param isEnabled Доступность группы кнопок
 * @param primaryBackground Цвет фона основной кнопки
 * @param primaryText Цвет текста основной кнопки
 * @param secondaryBackground Цвет фона вторичной кнопки
 * @param secondaryText Цвет текста вторичной кнопки
 * @param cancelBackground Цвет фона кнопки отмены
 * @param cancelText Цвет текста кнопки отмены
 * @param modifier Модификатор
 */
@Composable
fun ButtonGroup(
    buttons: ImmutableList<ChatMessageUiModel.Button>,
    onButtonClick: (String) -> Unit,
    isEnabled: Boolean,
    primaryBackground: Color,
    primaryText: Color,
    secondaryBackground: Color,
    secondaryText: Color,
    cancelBackground: Color,
    cancelText: Color,
    modifier: Modifier = Modifier
) {
    // Кэширование вычислений типов кнопок
    val buttonTypes = remember(buttons) {
        buttons.map { button ->
            ButtonTypeClassifier.fromTitle(button.title)
        }
    }
    
    // Alpha для disabled состояния
    val buttonAlpha = if (isEnabled) 1f else 0.5f
    
    Row(
        modifier = modifier
            .graphicsLayer { alpha = buttonAlpha }
    ) {
        buttons.forEachIndexed { index, button ->
            MessageButton(
                title = button.title,
                type = buttonTypes[index],
                isPressed = button.isPressed,
                onClick = { onButtonClick(button.actionId) },
                enabled = isEnabled,
                primaryBackground = primaryBackground,
                primaryText = primaryText,
                secondaryBackground = secondaryBackground,
                secondaryText = secondaryText,
                cancelBackground = cancelBackground,
                cancelText = cancelText,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

/**
 * Перегрузка для обычного List (конвертирует в ImmutableList)
 */
@Composable
fun ButtonGroup(
    buttons: List<ChatMessageUiModel.Button>,
    onButtonClick: (String) -> Unit,
    isEnabled: Boolean,
    primaryBackground: Color,
    primaryText: Color,
    secondaryBackground: Color,
    secondaryText: Color,
    cancelBackground: Color,
    cancelText: Color,
    modifier: Modifier = Modifier
) {
    ButtonGroup(
        buttons = buttons.toImmutableList(),
        onButtonClick = onButtonClick,
        isEnabled = isEnabled,
        primaryBackground = primaryBackground,
        primaryText = primaryText,
        secondaryBackground = secondaryBackground,
        secondaryText = secondaryText,
        cancelBackground = cancelBackground,
        cancelText = cancelText,
        modifier = modifier
    )
}
