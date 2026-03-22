package com.example.day.core.ui.uikit.chat.list.model

/**
 * Immutable UI model for a single chat message
 */
data class ChatMessageUiModel(
    val id: Long,
    val text: String,
    val userType: ChatMessageUiType,
    val status: UiMessageStatus,
    val avatarUrl: String? = null,
    val isExpanded: Boolean = false,
    val isMarkdownEnabled: Boolean = false,
    val buttons: Buttons? = null
) {
    data class Buttons(
        val list: List<Button>,
        val isEnabled: Boolean
    )

    data class Button(
        val actionId: String,
        val title: String,
        val description: String,
        val isPressed: Boolean
    )
}
