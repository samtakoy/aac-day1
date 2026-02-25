package com.example.day.core.ui.uikit.chat.list.model

/**
 * Immutable UI model for a single chat message
 */
data class ChatMessageUiModel(
    val id: Long,
    val text: String,
    val userType: ChatMessageUiType,
    val status: UiMessageStatus,
    val avatarUrl: String? = null
)
