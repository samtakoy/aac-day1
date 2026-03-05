package com.example.day.core.core_features.memory.domain.repository

import com.example.day.core.core_features.memory.domain.model.LongTermMemoryFact
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for long-term memory operations.
 * Manages user facts and preferences that persist across sessions.
 * Memory is isolated per LTMGroup, which can be linked to ChatGroup, UserProfile, etc.
 */
interface LongTermMemoryRepository {

    /**
     * Save or update a fact in long-term memory.
     * Uniqueness is enforced by (ltmGroupId + memoryKey + category) composite unique index.
     * If a fact with the same key exists, it will be updated; otherwise, a new one is inserted.
     *
     * @param ltmGroupId LTM Group ID for memory isolation
     * @param memoryKey Unique identifier within category (e.g., "primary_language", "experience_level")
     * @param category Category name (stored directly, no separate table)
     * @param fact The fact text to store
     */
    suspend fun upsertFact(ltmGroupId: Long, memoryKey: String, category: String, fact: String)

    /**
     * Get facts for a specific LTM group as a one-time operation.
     * Used when building system prompts for LLM.
     *
     * @param ltmGroupId LTM Group ID for memory isolation
     */
    suspend fun getFactsByGroup(ltmGroupId: Long): List<LongTermMemoryFact>

    /**
     * Get facts for a specific LTM group as a Flow for reactive UI updates.
     *
     * @param ltmGroupId LTM Group ID for memory isolation
     */
    fun getFactsByGroupFlow(ltmGroupId: Long): Flow<List<LongTermMemoryFact>>

    /**
     * Get a specific fact by its key within an LTM group.
     * Returns null if not found.
     *
     * @param ltmGroupId LTM Group ID for memory isolation
     */
    suspend fun getFactByKey(ltmGroupId: Long, memoryKey: String): LongTermMemoryFact?

    /**
     * Delete a fact by its unique id.
     *
     * @param id Fact id
     */
    suspend fun deleteFact(id: Long)

    /**
     * Delete a fact by composite key (ltmGroupId + memoryKey + category).
     * Uniqueness is defined by this combination.
     *
     * @param ltmGroupId LTM Group ID for memory isolation
     * @param memoryKey Unique identifier within category
     * @param category Category name
     */
    suspend fun deleteFact(ltmGroupId: Long, memoryKey: String, category: String)

    /**
     * Clear all facts for a specific LTM group.
     *
     * @param ltmGroupId LTM Group ID for memory isolation
     */
    suspend fun clearFactsByGroup(ltmGroupId: Long)

}
