package com.example.day.core.ui.uikit.chat.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.day.core.ui.uikit.chat.ChatUiColors
import com.example.day.core.ui.uikit.chat.LocalChatColors
import com.example.day.core.ui.uikit.chat.list.model.ChatMessageUiModel
import com.example.day.core.ui.uikit.chat.list.model.UiMessageStatus

/**
 * Composable for displaying a single chat message
 * - If isUser: avatar on right, text on left
 * - If bot: avatar on left, text on right
 */
@Composable
fun ChatMessageView(
    item: ChatMessageUiModel,
    modifier: Modifier = Modifier,
    colors: ChatUiColors = LocalChatColors.current
) {
    val bubbleColor = if (item.isUser) colors.userBubble else colors.botBubble
    val textColor = if (item.isUser) colors.userText else colors.botText

    // Status indicator
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

    if (item.isUser) {
        // User message: avatar on RIGHT, text on LEFT
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            // Message bubble on left
            Column(
                modifier = Modifier.weight(1f),
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
                        text = item.text,
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
                // url = item.avatarUrl,
                url = "https://free-png.ru/wp-content/uploads/2021/07/free-png.ru-30.png",
                placeholderText = "U",
                backgroundColor = colors.userAvatarBackground,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }
    } else {
        // Bot message: avatar on LEFT, text on RIGHT
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            // Avatar on left
            ChatAvatar(
                // url = item.avatarUrl,
                url = "https://cdnstatic.rg.ru/uploads/images/2023/02/17/bender_7b5.jpg",
                placeholderText = "B",
                backgroundColor = colors.botAvatarBackground,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Message bubble on right
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
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
                        text = item.text,
                        color = textColor,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
