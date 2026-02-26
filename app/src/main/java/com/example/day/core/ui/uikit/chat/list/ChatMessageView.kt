package com.example.day.core.ui.uikit.chat.list

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.day.core.ui.uikit.chat.ChatUiColors
import com.example.day.core.ui.uikit.chat.LocalChatColors
import com.example.day.core.ui.uikit.chat.list.model.ChatMessageUiModel
import com.example.day.core.ui.uikit.chat.list.model.ChatMessageUiType
import com.example.day.core.ui.uikit.chat.list.model.UiMessageStatus

/**
 * Composable for displaying a single chat message
 * - If userType == User: avatar on right, text on left
 * - If userType == Bot: avatar on left, text on right
 * - If userType == Info: no avatar, text with expand/collapse
 */
@Composable
fun ChatMessageView(
    item: ChatMessageUiModel,
    modifier: Modifier = Modifier,
    colors: ChatUiColors = LocalChatColors.current,
    onInfoMessageExpand: (id: Long, isExpanded: Boolean) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current

    // Copy function
    val copyToClipboard: () -> Unit = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("message", item.text)
        clipboard.setPrimaryClip(clip)
    }

    when (item.userType) {
        ChatMessageUiType.User -> {
            val bubbleColor = colors.userBubble
            val textColor = colors.userText
            val avatarBackground = colors.userAvatarBackground
            val avatarUrl = item.avatarUrl.orEmpty()
            val placeholderText = "U"
            val statusText = when (item.status) {
                UiMessageStatus.Sending -> "..."
                UiMessageStatus.Delivered -> "✓"
                UiMessageStatus.Viewed -> "✓✓"
            }
            val statusColor = when (item.status) {
                UiMessageStatus.Sending -> colors.sendingStatus
                UiMessageStatus.Delivered -> colors.deliveredStatus
                UiMessageStatus.Viewed -> colors.viewedStatus
            }

            UserMessageRow(
                modifier = modifier,
                text = item.text,
                bubbleColor = bubbleColor,
                textColor = textColor,
                statusText = statusText,
                statusColor = statusColor,
                avatarUrl = avatarUrl,
                placeholderText = placeholderText,
                avatarBackground = avatarBackground,
                onCopyClick = copyToClipboard
            )
        }

        ChatMessageUiType.Bot -> {
            val bubbleColor = colors.botBubble
            val textColor = colors.botText
            val avatarBackground = colors.botAvatarBackground
            val avatarUrl = item.avatarUrl.orEmpty()
            val placeholderText = "B"

            BotMessageRow(
                modifier = modifier,
                text = item.text,
                bubbleColor = bubbleColor,
                textColor = textColor,
                avatarUrl = avatarUrl,
                placeholderText = placeholderText,
                avatarBackground = avatarBackground,
                onCopyClick = copyToClipboard
            )
        }

        ChatMessageUiType.Info -> {
            val bubbleColor = colors.infoBubble
            val textColor = colors.infoText
            val avatarBackground = colors.infoAvatarBackground
            val avatarUrl = item.avatarUrl.orEmpty()
            val placeholderText = "I"

            InfoMessageRow(
                modifier = modifier,
                text = item.text,
                bubbleColor = bubbleColor,
                textColor = textColor,
                avatarUrl = avatarUrl,
                placeholderText = placeholderText,
                avatarBackground = avatarBackground,
                onCopyClick = copyToClipboard,
                isExpanded = item.isExpanded,
                onExpandChange = { isExpanded ->
                    onInfoMessageExpand(item.id, isExpanded)
                }
            )
        }
    }
}

/**
 * User message row: avatar on RIGHT, copy icon on LEFT of bubble
 */
@Composable
private fun UserMessageRow(
    text: String,
    bubbleColor: Color,
    textColor: Color,
    statusText: String,
    statusColor: Color,
    avatarUrl: String,
    placeholderText: String,
    avatarBackground: Color,
    onCopyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = LocalContentColor.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Top
    ) {
        // Copy icon on LEFT (attached to bubble, opposite side from avatar)
        Box(
            modifier = Modifier
                .padding(end = 4.dp, top = 4.dp)
                .clickable { onCopyClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy",
                modifier = Modifier.size(14.dp),
                tint = contentColor.copy(alpha = 0.6f)
            )
        }

        // Message bubble in center
        Column(
            modifier = Modifier.weight(1f, fill = false),
            horizontalAlignment = Alignment.End
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = bubbleColor,
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 4.dp,
                            bottomEnd = 16.dp,
                            bottomStart = 16.dp
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = text,
                    color = textColor,
                    fontSize = 14.sp
                )
            }
            // Status below message
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }

        // Avatar on right
        ChatAvatar(
            url = avatarUrl,
            placeholderText = placeholderText,
            backgroundColor = avatarBackground,
            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
        )
    }
}

/**
 * Bot message row: avatar on LEFT, copy icon on RIGHT of bubble
 */
@Composable
private fun BotMessageRow(
    text: String,
    bubbleColor: Color,
    textColor: Color,
    avatarUrl: String,
    placeholderText: String,
    avatarBackground: Color,
    onCopyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = LocalContentColor.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        // Avatar on left
        ChatAvatar(
            url = avatarUrl,
            placeholderText = placeholderText,
            backgroundColor = avatarBackground,
            modifier = Modifier.padding(top = 4.dp)
        )

        // Message bubble on right, with copy icon attached to its right side
        Row(
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(start = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = bubbleColor,
                        shape = RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 16.dp,
                            bottomEnd = 16.dp,
                            bottomStart = 16.dp
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = text,
                    color = textColor,
                    fontSize = 14.sp
                )
            }
        }
        // Copy icon attached to right side of bubble
        Box(
            modifier = Modifier
                .padding(start = 4.dp, top = 4.dp)
                .clickable { onCopyClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy",
                modifier = Modifier.size(14.dp),
                tint = contentColor.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Info message row: avatar on LEFT, text with expand/collapse icon on RIGHT
 * Default: single line with truncation, ^V icon to expand/collapse
 * 
 * Collapsed state: no avatar, no expand icon, smaller text and padding (compact)
 * Expanded state: avatar with spring animation, expand/collapse icon with animation
 */
@Composable
private fun InfoMessageRow(
    text: String,
    bubbleColor: Color,
    textColor: Color,
    avatarUrl: String,
    placeholderText: String,
    avatarBackground: Color,
    onCopyClick: () -> Unit,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    onExpandChange: (Boolean) -> Unit = {}
) {
    val contentColor = LocalContentColor.current
    val colors = LocalChatColors.current
    
    // Animation values for avatar
    // 1. Анимация аватара (мягко и быстро)
    val avatarAlpha by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "avatarAlpha"
    )

    val avatarScale by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "avatarScale"
    )
    
    // Determine text size and padding based on expanded state
    val textSize = if (isExpanded) colors.infoExpandedTextSize else colors.infoCollapsedTextSize
    val horizontalPadding = if (isExpanded) colors.infoExpandedPadding else colors.infoCollapsedPadding
    val verticalPadding = if (isExpanded) 8.dp else 6.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        // Avatar on left - only visible when expanded, with animation
        if (avatarAlpha > 0f) {
            ChatAvatar(
                url = avatarUrl,
                placeholderText = placeholderText,
                backgroundColor = avatarBackground,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = avatarScale
                        scaleY = avatarScale
                        alpha = avatarAlpha
                    }
                    .padding(top = 4.dp)
            )
        }

        // Message bubble with expand/collapse inside
        Box(
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(start = 8.dp)
                .background(
                    color = bubbleColor,
                    shape = RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 16.dp,
                        bottomEnd = 16.dp,
                        bottomStart = 16.dp
                    )
                )
                .padding(
                    start = horizontalPadding,
                    end = 4.dp,
                    top = verticalPadding,
                    bottom = verticalPadding
                )
        ) {
            Row(
                verticalAlignment = Alignment.Top
            ) {
                // Text with animated visibility
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(
                        animationSpec = spring(
                            dampingRatio = 0.7f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(animationSpec = androidx.compose.animation.core.tween(200)),
                    exit = shrinkVertically(
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = androidx.compose.animation.core.tween(150))
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
                
                // Collapsed state - show single line with truncation, clickable to expand
                if (!isExpanded) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clickable { onExpandChange(true) }
                    ) {
                        Text(
                            text = text,
                            color = textColor,
                            fontSize = textSize,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        // Small expand icon in collapsed state
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = "Expand",
                            modifier = Modifier
                                .size(16.dp)
                                .padding(start = 2.dp),
                            tint = textColor.copy(alpha = 0.4f)
                        )
                    }
                }
                
                // Expand/Collapse icon - only visible when expanded
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = scaleIn(
                        animationSpec = spring(
                            dampingRatio = 0.6f,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeIn(animationSpec = androidx.compose.animation.core.tween(200)),
                    exit = scaleOut(
                        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = androidx.compose.animation.core.tween(150))
                ) {
                    Box(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .clickable { onExpandChange(!isExpanded) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExpandLess,
                            contentDescription = "Collapse",
                            modifier = Modifier.size(18.dp),
                            tint = textColor.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // Copy icon on right side of bubble
        Box(
            modifier = Modifier
                .padding(start = 4.dp, top = 4.dp)
                .clickable { onCopyClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy",
                modifier = Modifier.size(14.dp),
                tint = contentColor.copy(alpha = 0.6f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatMessageViewPreview() {
    Column(
        // modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User message - short
        ChatMessageView(
            item = ChatMessageUiModel(
                id = 1,
                text = "Привет w rwer wrw r we!",
                userType = ChatMessageUiType.User,
                status = UiMessageStatus.Delivered,
                avatarUrl = "https://free-png.ru/wp-content/uploads/2021/07/free-png.ru-30.png"
            )
        )
        
        // User message - long
        ChatMessageView(
            item = ChatMessageUiModel(
                id = 2,
                text = "Это длинное сообщение от пользователя, которое демонстрирует, как выглядит сообщение с большим количеством текста и как оно переносится на новые строки.",
                userType = ChatMessageUiType.User,
                status = UiMessageStatus.Viewed,
                avatarUrl = "https://free-png.ru/wp-content/uploads/2021/07/free-png.ru-30.png"
            )
        )
        
        // Bot message - short
        ChatMessageView(
            item = ChatMessageUiModel(
                id = 3,
                text = "Привет! Я бот.",
                userType = ChatMessageUiType.Bot,
                status = UiMessageStatus.Delivered,
                avatarUrl = "https://cdnstatic.rg.ru/uploads/images/2023/02/17/bender_7b5.jpg"
            )
        )
        
        // Bot message - long
        ChatMessageView(
            item = ChatMessageUiModel(
                id = 4,
                text = "Это длинное сообщение от бота, которое показывает, как выглядит сообщение бота с большим количеством текста. Здесь может быть много информации и она будет красиво отображаться.",
                userType = ChatMessageUiType.Bot,
                status = UiMessageStatus.Delivered,
                avatarUrl = "https://cdnstatic.rg.ru/uploads/images/2023/02/17/bender_7b5.jpg"
            )
        )

        // Info message - short
        ChatMessageView(
            item = ChatMessageUiModel(
                id = 5,
                text = "Короткое информационное сообщение",
                userType = ChatMessageUiType.Info,
                status = UiMessageStatus.Delivered
            )
        )

        // Info message - long (collapsed)
        ChatMessageView(
            item = ChatMessageUiModel(
                id = 6,
                text = "Это длинное информационное сообщение, которое по умолчанию отображается в одну строку. При нажатии на иконку раскрытия текст будет показан полностью.",
                userType = ChatMessageUiType.Info,
                status = UiMessageStatus.Delivered,
                isExpanded = false
            )
        )
        
        // Info message - long (expanded)
        ChatMessageView(
            item = ChatMessageUiModel(
                id = 7,
                text = "Это длинное информационное сообщение в развернутом состоянии. Здесь видна аватарка с анимацией появления, иконка сворачивания, а также увеличенный размер текста и отступов.",
                userType = ChatMessageUiType.Info,
                status = UiMessageStatus.Delivered,
                isExpanded = true
            )
        )
    }
}
