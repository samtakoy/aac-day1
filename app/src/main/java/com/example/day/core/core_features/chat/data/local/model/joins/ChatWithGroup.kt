package com.example.day.core.core_features.chat.data.local.model.joins

import androidx.room.Embedded
import androidx.room.Relation
import com.example.day.core.core_features.chat.data.local.model.ChatEntity
import com.example.day.core.core_features.chat.data.local.model.ChatGroupEntity

// Основная модель для загрузки чата со вложенной группой
internal data class ChatWithGroup(
    @Embedded val chat: ChatEntity,
    @Relation(
        entity = ChatGroupEntity::class,
        parentColumn = "chat_group_id",
        entityColumn = "id"
    )
    val groupWithType: ChatGroupWithType
)