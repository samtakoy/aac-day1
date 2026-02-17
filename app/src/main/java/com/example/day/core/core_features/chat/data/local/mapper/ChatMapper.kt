package com.example.day.core.core_features.chat.data.local.mapper

import com.example.day.core.core_features.chat.data.local.model.ChatEntity
import com.example.day.core.core_features.chat.domain.model.Chat

internal fun ChatEntity.toDomain(): Chat = Chat(
    id = id,
    title = title
)

internal fun Chat.toEntity(): ChatEntity = ChatEntity(
    id = id,
    title = title
)
