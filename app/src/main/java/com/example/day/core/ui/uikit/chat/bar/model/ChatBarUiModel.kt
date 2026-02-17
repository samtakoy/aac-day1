package com.example.day.core.ui.uikit.chat.bar.model

/**
 * UI model for chat input bar
 */
data class ChatBarUiModel(
    val inputInitialValue: String,
    val buttonType: ChatSendButtonType = ChatSendButtonType.ArrowDisabled
)
