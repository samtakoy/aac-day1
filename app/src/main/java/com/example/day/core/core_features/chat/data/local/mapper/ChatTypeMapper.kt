package com.example.day.core.core_features.chat.data.local.mapper

import com.example.day.core.core_features.chat.data.local.model.ChatTypeEntity
import com.example.day.core.core_features.chat.domain.model.ChatType
import javax.inject.Inject

internal class ChatTypeMapper @Inject constructor() {
    fun toDomain(entity: ChatTypeEntity): ChatType? {
        return ChatType.fromDbType(entity.type)
    }
    
    fun toEntity(type: ChatType): ChatTypeEntity {
        return ChatTypeEntity(type = type.dbType)
    }
}
