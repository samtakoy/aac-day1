package com.example.day.core.core_features.chat.data.local.mapper

import com.example.day.core.core_features.chat.data.local.model.ChatEntity
import com.example.day.core.core_features.chat.data.local.model.joins.ChatWithGroup
import com.example.day.core.core_features.chat.domain.model.Chat
import javax.inject.Inject

internal class ChatMapper @Inject constructor(
    private val groupMapper: ChatGroupMapper
) {
    fun toDomain(entity: ChatWithGroup): Chat {
        return Chat(
            id = entity.chat.id,
            title = entity.chat.title,
            chatGroup = groupMapper.toDomain(entity.groupWithType)
        )
    }

    fun toEntity(model: Chat): ChatEntity = ChatEntity(
        id = model.id,
        title = model.title,
        chatGroupId = model.chatGroup.id
    )
}
