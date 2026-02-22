package com.example.day.core.core_features.chat.domain.model

data class ChatGroup(
    val id: Long,
    val title: String,
    val typeId: Long,
    val chatType: ChatType,
    val colorIndex: Int = 0
)
