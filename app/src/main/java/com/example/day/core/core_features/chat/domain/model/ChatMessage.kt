package com.example.day.core.core_features.chat.domain.model

data class ChatMessage(
    val id: Long,
    val chatId: Long,
    val timestamp: Long,
    val user: User,
    val text: String,
    val status: ChatMessageStatus,
    val type: Type,
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
        val replyMessage: String,
        val isEnabled: Boolean = true,
        val isPressed: Boolean = false
    )

    enum class Type {
        User,
        Bot,
        Info,
        Buttons,
        Title
    }
}
