package com.example.day.core.ui.uikit.chat.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Типы пузырьков сообщений с различными формами
 */
sealed class BubbleType {
    object User : BubbleType()
    object Bot : BubbleType()
    object Info : BubbleType()
    object Buttons : BubbleType()
    object Title : BubbleType()

    /**
     * Получение формы пузырька на основе типа
     */
    fun toRoundedCornerShape(): RoundedCornerShape = when (this) {
        is User -> RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 4.dp,
            bottomEnd = 16.dp,
            bottomStart = 16.dp
        )
        is Bot -> RoundedCornerShape(
            topStart = 4.dp,
            topEnd = 16.dp,
            bottomEnd = 16.dp,
            bottomStart = 16.dp
        )
        is Info -> RoundedCornerShape(16.dp)
        is Buttons -> RoundedCornerShape(12.dp)
        is Title -> RoundedCornerShape(24.dp)
    }
}

/**
 * Универсальный компонент пузырька сообщений
 *
 * @param text Текст сообщения
 * @param bubbleType Тип пузырька (определяет форму)
 * @param textColor Цвет текста
 * @param bubbleColor Цвет фона пузырька
 * @param isExpanded Состояние раскрытия (для Info/Buttons/Title типов)
 * @param onExpandChange Callback изменения состояния раскрытия
 * @param textSize Размер текста (для Info/Buttons/Title)
 * @param horizontalPadding Горизонтальный отступ
 * @param verticalPadding Вертикальный отступ
 * @param modifier Модификатор
 */
@Composable
fun ChatMessageBubble(
    text: String,
    bubbleType: BubbleType,
    textColor: Color,
    bubbleColor: Color,
    isExpanded: Boolean = false,
    onExpandChange: ((Boolean) -> Unit)? = null,
    textSize: TextUnit = 14.sp,
    horizontalPadding: Dp = 12.dp,
    verticalPadding: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    val contentColor = LocalContentColor.current

    Box(
        modifier = modifier
            .background(
                color = bubbleColor,
                shape = bubbleType.toRoundedCornerShape()
            )
            .then(
                if (bubbleType is BubbleType.Info || bubbleType is BubbleType.Buttons || bubbleType is BubbleType.Title) {
                    Modifier.clickable { onExpandChange?.invoke(!isExpanded) }
                } else {
                    Modifier
                }
            )
            .padding(
                start = horizontalPadding,
                end = if (bubbleType is BubbleType.Info || bubbleType is BubbleType.Buttons || bubbleType is BubbleType.Title) 4.dp else horizontalPadding,
                top = verticalPadding,
                bottom = verticalPadding
            )
    ) {
        when (bubbleType) {
            is BubbleType.User, is BubbleType.Bot -> {
                // Простой текст для User/Bot сообщений
                Text(
                    text = text,
                    color = textColor,
                    fontSize = textSize,
                    maxLines = Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis
                )
            }

            is BubbleType.Info, is BubbleType.Buttons, is BubbleType.Title -> {
                // Текст с expand/collapse для Info/Buttons/Title
                ExpandableMessageContent(
                    text = text,
                    textColor = textColor,
                    isExpanded = isExpanded,
                    onExpandChange = onExpandChange,
                    textSize = textSize
                )
            }
        }
    }
}

/**
 * Компонент раскрывающегося контента для Info/Buttons сообщений
 */
@Composable
private fun ExpandableMessageContent(
    text: String,
    textColor: Color,
    isExpanded: Boolean,
    onExpandChange: ((Boolean) -> Unit)?,
    textSize: TextUnit
) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        // Раскрытый текст с анимацией
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = 0.7f,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = tween(200)),
            exit = shrinkVertically(
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(150))
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = textSize,
                maxLines = Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }

        // Свёрнутый текст с иконкой раскрытия
        if (!isExpanded) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f, fill = false)
            ) {
                Text(
                    text = text,
                    color = textColor,
                    fontSize = textSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                IconExpandMore(
                    tint = textColor.copy(alpha = 0.4f)
                )
            }
        }

        // Иконка сворачивания (видна только в раскрытом состоянии)
        AnimatedVisibility(
            visible = isExpanded,
            enter = scaleIn(
                animationSpec = spring(
                    dampingRatio = 0.6f,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeIn(animationSpec = tween(200)),
            exit = scaleOut(
                animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(150))
        ) {
            IconExpandLess(
                tint = textColor.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Иконка раскрытия (ExpandMore)
 */
@Composable
private fun IconExpandMore(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(start = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Icon(
            imageVector = Icons.Default.ExpandMore,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = tint
        )
    }
}

/**
 * Иконка сворачивания (ExpandLess)
 */
@Composable
private fun IconExpandLess(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(start = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Icon(
            imageVector = Icons.Default.ExpandLess,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = tint
        )
    }
}
