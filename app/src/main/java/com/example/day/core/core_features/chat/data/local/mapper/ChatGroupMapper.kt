package com.example.day.core.core_features.chat.data.local.mapper

import com.example.day.core.core_features.chat.data.local.model.ChatGroupEntity
import com.example.day.core.core_features.chat.data.local.model.joins.ChatGroupWithType
import com.example.day.core.core_features.chat.domain.model.ChatGroup
import com.example.day.core.core_features.chat.domain.model.ChatType
import javax.inject.Inject

internal class ChatGroupMapper @Inject constructor() {
    fun toDomain(entity: ChatGroupWithType): ChatGroup {
        return ChatGroup(
            id = entity.group.id,
            title = entity.group.title,
            typeId = entity.group.typeId,
            chatType = ChatType.fromDbType(entity.type.type) ?: ChatType.SIMPLE_HISTORY,
            colorIndex = entity.group.colorIndex
        )
    }
    
    fun toEntity(domain: ChatGroup): ChatGroupEntity {
        return ChatGroupEntity(
            id = domain.id,
            title = domain.title,
            typeId = domain.typeId,
            colorIndex = domain.colorIndex
        )
    }
}
