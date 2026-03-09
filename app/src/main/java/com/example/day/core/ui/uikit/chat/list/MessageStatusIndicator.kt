package com.example.day.core.ui.uikit.chat.list

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.day.core.ui.uikit.chat.list.model.UiMessageStatus

/**
 * Компонент индикатора статуса сообщения
 * 
 * @param status Статус сообщения (Sending, Delivered, Viewed)
 * @param sendingColor Цвет статуса "отправка"
 * @param deliveredColor Цвет статуса "доставлено"
 * @param viewedColor Цвет статуса "прочитано"
 * @param modifier Модификатор
 */
@Composable
fun MessageStatusIndicator(
    status: UiMessageStatus,
    sendingColor: Color,
    deliveredColor: Color,
    viewedColor: Color,
    modifier: Modifier = Modifier
) {
    val statusText = when (status) {
        UiMessageStatus.Sending -> "..."
        UiMessageStatus.Delivered -> "✓"
        UiMessageStatus.Viewed -> "✓✓"
    }
    
    val statusColor = when (status) {
        UiMessageStatus.Sending -> sendingColor
        UiMessageStatus.Delivered -> deliveredColor
        UiMessageStatus.Viewed -> viewedColor
    }
    
    Text(
        text = statusText,
        color = statusColor,
        fontSize = 10.sp,
        modifier = modifier.padding(start = 4.dp, top = 2.dp)
    )
}

/**
 * Компонент индикатора статуса "отправка" с анимацией
 * 
 * @param color Цвет индикатора
 * @param modifier Модификатор
 */
@Composable
fun SendingStatusIndicator(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sendingStatus")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sendingAlpha"
    )
    
    Text(
        text = "...",
        color = color.copy(alpha = alpha),
        fontSize = 10.sp,
        modifier = modifier.padding(start = 4.dp, top = 2.dp)
    )
}
