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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography

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
 * @param isMarkdownEnabled Рендерить текст как Markdown (для Bot/Info)
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
    isMarkdownEnabled: Boolean = false,
    textSize: TextUnit = 14.sp,
    horizontalPadding: Dp = 12.dp,
    verticalPadding: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
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
            is BubbleType.User -> {
                Text(
                    text = text,
                    color = textColor,
                    fontSize = textSize,
                    maxLines = Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis
                )
            }

            is BubbleType.Bot -> {
                if (isMarkdownEnabled) {
                    ChatBubbleMarkdown(text = text, textColor = textColor)
                } else {
                    Text(
                        text = text,
                        color = textColor,
                        fontSize = textSize,
                        maxLines = Int.MAX_VALUE,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            is BubbleType.Info, is BubbleType.Buttons, is BubbleType.Title -> {
                if (isMarkdownEnabled && isExpanded) {
                    ChatBubbleMarkdown(text = text, textColor = textColor)
                } else {
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
        // Раскрытый текст — weight(1f) прямо на AnimatedVisibility в RowScope
        AnimatedVisibility(
            visible = isExpanded,
            modifier = Modifier.weight(1f, fill = false),
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
                overflow = TextOverflow.Ellipsis
            )
        }

        // Свёрнутый текст с иконкой — weight(1f) прямо в RowScope
        if (!isExpanded) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text,
                    color = textColor,
                    fontSize = textSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                IconExpandMore(tint = textColor.copy(alpha = 0.4f))
            }
        }

        // Иконка сворачивания — прямой дочерний элемент Row, всегда виден
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

/**
 * Markdown в чат-пузырьке с уменьшенными заголовками и тёмным фоном кода.
 *
 * Дефолты библиотеки — displayLarge/displayMedium/displaySmall — слишком крупные для чата.
 * Переопределяем через markdownTypography() до title/body размеров.
 */
@Composable
private fun ChatBubbleMarkdown(
    text: String,
    textColor: Color,
) {
    CompositionLocalProvider(LocalContentColor provides textColor) {
        Markdown(
            content = text,
            colors = markdownColor(
                codeBackground = lerp(MaterialTheme.colorScheme.surfaceContainerHighest, Color.Black, 0.1f),
            ),
            typography = markdownTypography(
                h1 = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                h2 = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                h3 = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                h4 = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                h5 = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                h6 = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
