package com.example.day.core.ui.uikit.chat.list.model

/**
 * Sealed interface for chat list UI events
 */
sealed interface ChatListUiEvent {
    /**
     * User long-pressed on a message item
     */
    data class ItemLongClick(val message: ChatMessageUiModel) : ChatListUiEvent
}
