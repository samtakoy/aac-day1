package com.example.day.core.ui.uikit.chat.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Типы кнопок сообщений
 */
enum class ButtonType {
    Primary,    // зелёная (Да, ОК, Сохранить, Confirm)
    Secondary,  // серая/синяя (обычные действия)
    Cancel      // красная (Отмена, Нет, Cancel)
}

/**
 * Утилита для определения типа кнопки по заголовку
 */
object ButtonTypeClassifier {
    /**
     * Определяет тип кнопки на основе заголовка
     * 
     * @param title Заголовок кнопки
     * @return Тип кнопки
     */
    fun fromTitle(title: String): ButtonType {
        val lowerTitle = title.lowercase()
        return when {
            isCancelButton(lowerTitle) -> ButtonType.Cancel
            isPrimaryButton(lowerTitle) -> ButtonType.Primary
            else -> ButtonType.Secondary
        }
    }
    
    /**
     * Проверка на кнопку отмены
     */
    private fun isCancelButton(title: String): Boolean {
        return title.containsAny(
            "отмена", "нет", "cancel", "no", "отменить"
        )
    }
    
    /**
     * Проверка на основную кнопку
     */
    private fun isPrimaryButton(title: String): Boolean {
        return title.containsAny(
            "да", "ok", "подтвердить", "confirm", "accept", "yes", "сохранить", "save"
        )
    }
    
    /**
     * Проверка наличия любой из подстрок
     */
    private fun String.containsAny(vararg substrings: String): Boolean {
        return substrings.any { contains(it, ignoreCase = true) }
    }
}

/**
 * Компонент кнопки сообщения
 * 
 * @param title Заголовок кнопки
 * @param type Тип кнопки (определяет цвета)
 * @param isPressed Состоятие нажатия
 * @param onClick Callback клика
 * @param enabled Доступность
 * @param primaryBackground Цвет фона основной кнопки
 * @param primaryText Цвет текста основной кнопки
 * @param secondaryBackground Цвет фона вторичной кнопки
 * @param secondaryText Цвет текста вторичной кнопки
 * @param cancelBackground Цвет фона кнопки отмены
 * @param cancelText Цвет текста кнопки отмены
 * @param modifier Модификатор
 */
@Composable
fun MessageButton(
    title: String,
    type: ButtonType,
    isPressed: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    primaryBackground: Color,
    primaryText: Color,
    secondaryBackground: Color,
    secondaryText: Color,
    cancelBackground: Color,
    cancelText: Color,
    modifier: Modifier = Modifier
) {
    // Выбор цветов на основе типа кнопки
    val (backgroundColor, textColor) = when (type) {
        ButtonType.Primary -> primaryBackground to primaryText
        ButtonType.Secondary -> secondaryBackground to secondaryText
        ButtonType.Cancel -> cancelBackground to cancelText
    }
    
    // Цвет фона с учётом состояния нажатия
    val buttonBackground = if (isPressed) {
        backgroundColor.copy(alpha = 0.6f)
    } else {
        backgroundColor
    }
    
    Surface(
        modifier = modifier
            .clickable(
                enabled = enabled && !isPressed,
                onClick = onClick
            )
            .background(
                color = buttonBackground,
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(6.dp),
        color = Color.Transparent
    ) {
        Text(
            text = title,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = if (isPressed) FontWeight.Bold else FontWeight.Medium
        )
    }
}
