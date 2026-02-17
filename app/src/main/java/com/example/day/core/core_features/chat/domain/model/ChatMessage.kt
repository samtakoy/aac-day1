package com.example.day.core.core_features.chat.domain.model

data class ChatMessage(
    val id: Long,
    val chatId: Long,
    val timestamp: Long,
    val user: User,
    val text: String,
    val status: ChatMessageStatus
)
