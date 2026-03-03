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
     * Uses memoryKey + ltmGroupId for UPSERT - if key exists, updates; otherwise inserts.
     *
     * @param ltmGroupId LTM Group ID for memory isolation
     * @param memoryKey Unique identifier (e.g., "primary_language", "experience_level")
     * @param categoryId Category ID from LTMCategory
     * @param fact The fact text to store
     */
    suspend fun upsertFact(ltmGroupId: Long, memoryKey: String, categoryId: Long, fact: String)

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
     * Delete all facts with the given key within an LTM group (across all categories).
     *
     * @param ltmGroupId LTM Group ID for memory isolation
     */
    suspend fun deleteFact(ltmGroupId: Long, memoryKey: String)

    /**
     * Delete a specific fact by key + category within an LTM group.
     *
     * @param ltmGroupId LTM Group ID for memory isolation
     * @param categoryId Category ID of the fact to delete
     */
    suspend fun deleteFact(ltmGroupId: Long, memoryKey: String, categoryId: Long)

    /**
     * Clear all facts for a specific LTM group.
     *
     * @param ltmGroupId LTM Group ID for memory isolation
     */
    suspend fun clearFactsByGroup(ltmGroupId: Long)

}
