package com.example.day.core.core_features.chat.data.local.mapper

import com.example.day.core.core_features.chat.data.local.model.ChatDbConst
import com.example.day.core.core_features.chat.data.local.model.MessageEntity
import com.example.day.core.core_features.chat.domain.model.ChatMessage
import com.example.day.core.core_features.chat.domain.model.ChatMessageStatus
import javax.inject.Inject

internal class MessageMapper @Inject constructor() {
    
    fun toDomain(entity: MessageEntity, user: com.example.day.core.core_features.chat.domain.model.User): ChatMessage = ChatMessage(
        id = entity.id,
        chatId = entity.chatId,
        timestamp = entity.timestamp,
        user = user,
        text = entity.text,
        status = fromDbStatus(entity.status)
    )

    fun toEntity(message: ChatMessage, userId: Long): MessageEntity = MessageEntity(
        id = message.id,
        chatId = message.chatId,
        userId = userId,
        timestamp = message.timestamp,
        text = message.text,
        status = toDbStatus(message.status)
    )
    
    fun toDbStatus(status: ChatMessageStatus): Int = when (status) {
        ChatMessageStatus.Sending -> ChatDbConst.MESSAGE_STATUS_SENDING
        ChatMessageStatus.Delivered -> ChatDbConst.MESSAGE_STATUS_DELIVERED
        ChatMessageStatus.Viewed -> ChatDbConst.MESSAGE_STATUS_VIEWED
    }
    
    fun fromDbStatus(status: Int): ChatMessageStatus = when (status) {
        ChatDbConst.MESSAGE_STATUS_SENDING -> ChatMessageStatus.Sending
        ChatDbConst.MESSAGE_STATUS_DELIVERED -> ChatMessageStatus.Delivered
        ChatDbConst.MESSAGE_STATUS_VIEWED -> ChatMessageStatus.Viewed
        else -> ChatMessageStatus.Sending
    }
}
