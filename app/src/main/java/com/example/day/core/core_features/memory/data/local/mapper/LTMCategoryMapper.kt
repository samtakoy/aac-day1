package com.example.day.core.core_features.memory.data.local.mapper

import com.example.day.core.core_features.memory.data.local.model.LTMCategoryEntity
import com.example.day.core.core_features.memory.domain.model.LTMCategory

/**
 * Mapper between LTMCategoryEntity and LTMCategory domain model.
 */
internal object LTMCategoryMapper {

    fun toDomain(entity: LTMCategoryEntity): LTMCategory {
        return LTMCategory(
            id = entity.id,
            title = entity.title
        )
    }

    fun toEntity(domain: LTMCategory): LTMCategoryEntity {
        return LTMCategoryEntity(
            id = domain.id,
            title = domain.title.lowercase()
        )
    }

    fun toDomainList(entities: List<LTMCategoryEntity>): List<LTMCategory> {
        return entities.map { toDomain(it) }
    }
}
