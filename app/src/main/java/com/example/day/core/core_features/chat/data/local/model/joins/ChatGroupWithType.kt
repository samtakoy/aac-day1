package com.example.day.core.core_features.chat.data.local.model.joins

import androidx.room.Embedded
import androidx.room.Relation
import com.example.day.core.core_features.chat.data.local.model.ChatGroupEntity
import com.example.day.core.core_features.chat.data.local.model.ChatTypeEntity

// Вспомогательная модель для группы и её типа
internal data class ChatGroupWithType(
    @Embedded val group: ChatGroupEntity,
    @Relation(
        parentColumn = "type_id",
        entityColumn = "id"
    )
    val type: ChatTypeEntity
)

