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
import com.example.day.core.ui.uikit.chat.ChatUiColors
import com.example.day.core.ui.uikit.chat.LocalChatColors
import com.example.day.core.ui.uikit.chat.list.model.ChatListUiEvent
import com.example.day.core.ui.uikit.chat.list.model.ChatListUiModel

/**
 * Composable for displaying chat message list
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatListView(
    model: ChatListUiModel,
    onEvent: (ChatListUiEvent) -> Unit,
    onInfoMessageExpand: (id: Long, isExpanded: Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    colors: ChatUiColors = LocalChatColors.current
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
                color = colors.inputPlaceholder
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
                    // TODO зачем Column?
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
                            onInfoMessageExpand = onInfoMessageExpand
                        )
                    }
                }
            }
        }
    }
}
