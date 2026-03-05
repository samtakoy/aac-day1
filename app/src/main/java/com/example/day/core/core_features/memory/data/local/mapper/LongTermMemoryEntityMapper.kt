package com.example.day.core.core_features.memory.data.local.mapper

import com.example.day.core.core_features.memory.data.local.dao.relation.LongTermMemoryWithCategoryRelation
import com.example.day.core.core_features.memory.data.local.model.LongTermMemoryEntity
import com.example.day.core.core_features.memory.domain.model.LongTermMemoryFact

/**
 * Mapper between LongTermMemoryEntity and LongTermMemory domain model.
 */
internal object LongTermMemoryEntityMapper {

    fun toDomain(entity: LongTermMemoryWithCategoryRelation): LongTermMemoryFact {
        return LongTermMemoryFact(
            memoryKey = entity.memory.memoryKey,
            ltmGroupId = entity.memory.ltmGroupId,
            categoryId = entity.memory.categoryId,
            category = entity.category?.title ?: "general",
            fact = entity.memory.fact,
            updatedAt = entity.memory.updatedAt
        )
    }

    fun toDomainList(entities: List<LongTermMemoryWithCategoryRelation>): List<LongTermMemoryFact> {
        return entities.map { toDomain(it) }
    }
}
