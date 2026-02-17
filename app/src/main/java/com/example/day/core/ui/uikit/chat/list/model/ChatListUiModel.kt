package com.example.day.core.ui.uikit.chat.list.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList

/**
 * Immutable UI model for chat list content
 */
@Immutable
data class ChatListUiModel(
    val messages: PersistentList<ChatMessageUiModel>
)
