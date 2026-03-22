package com.example.day.core.ui.uikit.chat.list.model

/**
 * Sealed interface for chat list UI events
 */
sealed interface ChatListUiEvent {
    /**
     * User long-pressed on a message item
     */
    data class ItemLongClick(val message: ChatMessageUiModel) : ChatListUiEvent

    /**
     * User clicked on a button in a message
     */
    data class ChatButtonClick(val messageId: Long, val actionId: String) : ChatListUiEvent

    /**
     * User toggled the Markdown rendering for a message
     */
    data class MarkdownToggle(val messageId: Long, val isEnabled: Boolean) : ChatListUiEvent
}
