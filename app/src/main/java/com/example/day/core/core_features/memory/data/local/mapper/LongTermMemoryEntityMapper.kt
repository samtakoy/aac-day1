package com.example.day.core.core_features.memory.data.local.mapper

import com.example.day.core.core_features.memory.data.local.model.LongTermMemoryEntity
import com.example.day.core.core_features.memory.domain.model.LongTermMemoryFact

/**
 * Mapper between LongTermMemoryEntity and LongTermMemory domain model.
 */
internal object LongTermMemoryEntityMapper {

    fun toDomain(entity: LongTermMemoryEntity): LongTermMemoryFact {
        return LongTermMemoryFact(
            id = entity.id,
            memoryKey = entity.memoryKey,
            ltmGroupId = entity.ltmGroupId,
            category = entity.category,
            fact = entity.fact,
            updatedAt = entity.updatedAt
        )
    }

    fun toDomainList(entities: List<LongTermMemoryEntity>): List<LongTermMemoryFact> {
        return entities.map { toDomain(it) }
    }
}
