package com.example.day.core.ui.uikit.chat.list.model

/**
 * Immutable UI model for a single chat message
 */
data class ChatMessageUiModel(
    val id: Long,
    val text: String,
    val isUser: Boolean,
    val status: UiMessageStatus,
    // TODO
    // val avatarUrl: String
)
