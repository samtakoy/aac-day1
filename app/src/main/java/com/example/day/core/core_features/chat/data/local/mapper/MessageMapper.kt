package com.example.day.core.core_features.chat.data.local.mapper

import com.example.day.core.core_features.chat.data.local.model.ButtonDataModel
import com.example.day.core.core_features.chat.data.local.model.ButtonsDataModel
import com.example.day.core.core_features.chat.data.local.model.ChatDbConst
import com.example.day.core.core_features.chat.data.local.model.MessageEntity
import com.example.day.core.core_features.chat.domain.model.ChatMessage
import com.example.day.core.core_features.chat.domain.model.ChatMessageStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

internal class MessageMapper @Inject constructor() {

    fun toDomain(entity: MessageEntity, user: com.example.day.core.core_features.chat.domain.model.User): ChatMessage = ChatMessage(
        id = entity.id,
        chatId = entity.chatId,
        timestamp = entity.timestamp,
        user = user,
        text = entity.text,
        status = fromDbStatus(entity.status),
        type = fromDbType(entity.type),
        buttons = entity.buttons?.let { Json.decodeFromString<ButtonsDataModel>(it) }?.let { toDomainButtons(it) }
    )

    fun toEntity(message: ChatMessage, userId: Long): MessageEntity = MessageEntity(
        id = message.id,
        chatId = message.chatId,
        userId = userId,
        timestamp = message.timestamp,
        text = message.text,
        status = toDbStatus(message.status),
        type = toDbType(message.type),
        buttons = message.buttons?.let { Json.encodeToString(toDataButtons(it)) }
    )

    fun buttonsToJson(buttons: ChatMessage.Buttons): String = Json.encodeToString(toDataButtons(buttons))

    private fun toDomainButtons(data: ButtonsDataModel) = ChatMessage.Buttons(
        list = data.list.map { ChatMessage.Button(it.actionId, it.title, it.description, it.replyMessage, it.isEnabled, it.isPressed) },
        isEnabled = data.isEnabled
    )

    private fun toDataButtons(domain: ChatMessage.Buttons) = ButtonsDataModel(
        list = domain.list.map { ButtonDataModel(it.actionId, it.title, it.description, it.replyMessage, it.isEnabled, it.isPressed) },
        isEnabled = domain.isEnabled
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

    fun toDbType(type: ChatMessage.Type): Int = when (type) {
        ChatMessage.Type.User -> 0
        ChatMessage.Type.Bot -> 1
        ChatMessage.Type.Info -> 2
        ChatMessage.Type.Buttons -> 3
        ChatMessage.Type.Title -> 4
    }

    fun fromDbType(type: Int): ChatMessage.Type = when (type) {
        0 -> ChatMessage.Type.User
        1 -> ChatMessage.Type.Bot
        2 -> ChatMessage.Type.Info
        3 -> ChatMessage.Type.Buttons
        4 -> ChatMessage.Type.Title
        else -> ChatMessage.Type.User
    }
}
