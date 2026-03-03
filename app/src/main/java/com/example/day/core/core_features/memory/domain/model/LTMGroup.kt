package com.example.day.core.core_features.memory.domain.model

/**
 * TODO в таком виде это точно не будет использовано - разве что сюда будет добавлен список фактов
 *
 * Domain model representing an LTM (Long-Term Memory) group.
 * Serves as a container for grouping long-term memory facts.
 * Can be linked to ChatGroup, UserProfile, or other entities.
 *
 * @property id Unique identifier
 */
data class LTMGroup(
    val id: Long
)
