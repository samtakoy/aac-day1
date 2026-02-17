package com.example.day.core.core_features.chat.data.local.mapper

import com.example.day.core.core_features.chat.data.local.model.ChatDbConst
import com.example.day.core.core_features.chat.data.local.model.MessageEntity
import com.example.day.core.core_features.chat.domain.model.ChatMessage
import com.example.day.core.core_features.chat.domain.model.ChatMessageStatus
import com.example.day.core.core_features.chat.domain.model.User

internal fun MessageEntity.toDomain(user: User): ChatMessage = ChatMessage(
    id = id,
    chatId = chatId,
    timestamp = timestamp,
    user = user,
    text = text,
    status = when (status) {
        ChatDbConst.MESSAGE_STATUS_SENDING -> ChatMessageStatus.Sending
        ChatDbConst.MESSAGE_STATUS_DELIVERED -> ChatMessageStatus.Delivered
        ChatDbConst.MESSAGE_STATUS_VIEWED -> ChatMessageStatus.Viewed
        else -> ChatMessageStatus.Sending
    }
)

internal fun ChatMessage.toEntity(userId: Long): MessageEntity = MessageEntity(
    id = id,
    chatId = chatId,
    userId = userId,
    timestamp = timestamp,
    text = text,
    status = when (status) {
        ChatMessageStatus.Sending -> ChatDbConst.MESSAGE_STATUS_SENDING
        ChatMessageStatus.Delivered -> ChatDbConst.MESSAGE_STATUS_DELIVERED
        ChatMessageStatus.Viewed -> ChatDbConst.MESSAGE_STATUS_VIEWED
    }
)
