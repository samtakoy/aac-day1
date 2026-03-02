package com.example.day.core.core_features.chat.data.local.model.joins

import androidx.room.Embedded
import androidx.room.Relation
import com.example.day.core.core_features.chat.data.local.model.ChatEntity
import com.example.day.core.core_features.chat.data.local.model.ChatGroupEntity
import com.example.day.core.core_features.chat.data.local.model.ChatSettingsEntity

// TODO это удалить, settings унести в llm и запрашиват при необходимости
internal data class ChatWithGroupAndSettings(
    @Embedded val chat: ChatEntity,
    @Relation(
        entity = ChatGroupEntity::class,
        parentColumn = "chat_group_id",
        entityColumn = "id"
    )
    val groupWithType: ChatGroupWithType,
    @Relation(
        entity = ChatSettingsEntity::class,
        parentColumn = "id",
        entityColumn = "chat_id"
    )
    val settings: ChatSettingsEntity?
)
