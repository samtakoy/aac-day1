package com.example.day.core.core_features.memory.data.local.dao.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.day.core.core_features.memory.data.local.model.LTMCategoryEntity
import com.example.day.core.core_features.memory.data.local.model.LongTermMemoryEntity

internal data class LongTermMemoryWithCategoryRelation(
    @Embedded
    val memory: LongTermMemoryEntity,

    @Relation(
        parentColumn = "category_id", // Поле в LongTermMemoryEntity
        entityColumn = "id"           // Поле в LTMCategoryEntity
    )
    val category: LTMCategoryEntity? // TODO проверить - зачем 0 так не надо
)