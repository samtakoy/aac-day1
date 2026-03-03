package com.example.day.core.core_features.chat.domain.repository

import com.example.day.core.core_features.chat.domain.model.LongTermMemory
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for long-term memory operations.
 * Manages user facts and preferences that persist across sessions.
 * Memory is isolated per ChatGroup.
 */
interface LongTermMemoryRepository {

    /**
     * Save or update a fact in long-term memory.
     * Uses memoryKey + groupId for UPSERT - if key exists, updates; otherwise inserts.
     *
     * @param groupId ChatGroup ID for memory isolation
     * @param memoryKey Unique identifier (e.g., "primary_language", "experience_level")
     * @param category Category: "skills", "preferences", "experience", "personal"
     * @param fact The fact text to store
     */
    suspend fun upsertFact(groupId: Long, memoryKey: String, category: String, fact: String)

    /**
     * Get facts for a specific group as a one-time operation.
     * Used when building system prompts for LLM.
     *
     * @param groupId ChatGroup ID for memory isolation
     */
    suspend fun getFactsByGroup(groupId: Long): List<LongTermMemory>

    /**
     * Get facts for a specific group as a Flow for reactive UI updates.
     *
     * @param groupId ChatGroup ID for memory isolation
     */
    fun getFactsByGroupFlow(groupId: Long): Flow<List<LongTermMemory>>

    /**
     * Get a specific fact by its key within a group.
     * Returns null if not found.
     *
     * @param groupId ChatGroup ID for memory isolation
     */
    suspend fun getFactByKey(groupId: Long, memoryKey: String): LongTermMemory?

    /**
     * Delete a fact by its key within a group.
     *
     * @param groupId ChatGroup ID for memory isolation
     */
    suspend fun deleteFact(groupId: Long, memoryKey: String)

    /**
     * Clear all facts for a specific group.
     *
     * @param groupId ChatGroup ID for memory isolation
     */
    suspend fun clearFactsByGroup(groupId: Long)
}
