package com.example.day.core.core_features.agent.domain.repository

import com.example.day.core.core_features.memory.domain.model.LongTermMemoryFact
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing agent-specific long-term memory facts.
 * Provides a high-level abstraction over LTMGroup and LongTermMemory repositories.
 * Uniqueness of facts is defined by (agentId + memoryKey + category).
 */
interface AgentMemoryRepository {

    /**
     * Get all facts for an agent.
     *
     * @param agentId Agent ID
     * @return List of facts
     */
    suspend fun getFacts(agentId: Long): List<LongTermMemoryFact>

    /**
     * Get all facts for an agent as a Flow for reactive updates.
     *
     * @param agentId Agent ID
     * @return Flow of list of facts
     */
    fun getFactsAsFlow(agentId: Long): Flow<List<LongTermMemoryFact>>

    /**
     * Get a specific fact by its key within an agent.
     *
     * @param agentId Agent ID
     * @param memoryKey Unique identifier within category
     * @return Fact or null if not found
     */
    suspend fun getFact(agentId: Long, memoryKey: String, category: String): LongTermMemoryFact?

    /**
     * Save or update a fact for an agent.
     * Uniqueness is enforced by (agentId + memoryKey + category).
     *
     * @param agentId Agent ID
     * @param memoryKey Unique identifier within category
     * @param category Category name
     * @param fact The fact text to store
     */
    suspend fun upsertFact(agentId: Long, memoryKey: String, category: String, fact: String)

    /**
     * Delete a fact by its unique id.
     *
     * @param id Fact id
     */
    suspend fun deleteFact(id: Long)

    /**
     * Delete a fact by composite key (agentId + memoryKey + category).
     *
     * @param agentId Agent ID
     * @param memoryKey Unique identifier within category
     * @param category Category name
     */
    suspend fun deleteFact(agentId: Long, memoryKey: String, category: String)

    suspend fun deleteFacts(agentId: Long, memoryKey: String)

    /**
     * Clear all facts for an agent.
     *
     * @param agentId Agent ID
     */
    suspend fun clearAllFacts(agentId: Long)
}
