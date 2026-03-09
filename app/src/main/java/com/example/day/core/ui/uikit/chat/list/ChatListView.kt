package com.example.day.core.ui.uikit.chat.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.day.core.ui.uikit.chat.ChatColorScheme
import com.example.day.core.ui.uikit.chat.LocalChatColorScheme
import com.example.day.core.ui.uikit.chat.list.model.ChatListUiEvent
import com.example.day.core.ui.uikit.chat.list.model.ChatListUiModel

/**
 * Composable для отображения списка сообщений чата
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatListView(
    model: ChatListUiModel,
    onEvent: (ChatListUiEvent) -> Unit,
    onInfoMessageExpand: (id: Long, isExpanded: Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    colors: ChatColorScheme = LocalChatColorScheme.current
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        if (model.messages.isEmpty()) {
            // Empty state
            Text(
                text = "No messages yet",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                color = colors.contentColor.copy(alpha = 0.6f)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp)
            ) {
                items(
                    items = model.messages,
                    key = { it.id }
                ) { message ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { },
                                onLongClick = { onEvent(ChatListUiEvent.ItemLongClick(message)) }
                            )
                    ) {
                        ChatMessageView(
                            item = message,
                            colors = colors,
                            onInfoMessageExpand = onInfoMessageExpand,
                            onMessageButtonClick = { messageId, actionId ->
                                onEvent(ChatListUiEvent.ChatButtonClick(messageId, actionId))
                            }
                        )
                    }
                }
            }
        }
    }
}
