package com.example.day.core.core_features.agent.domain.model

/**
 * Interface for managing agent context.
 * Defines the contract for storing and retrieving conversation context.
 */
interface AContextOwner {
    /**
     * Get context for an agent.
     * @param agentId the agent ID
     * @return AContext if found, null otherwise
     */
    suspend fun getContext(agentId: Long): AContext
    
    /**
     * Save context for an agent.
     * @param agentId the agent ID
     * @param context the context to save
     */
    suspend fun saveContext(agentId: Long, context: AContext)

    /**
     * Clear agent context from database.
     * @param agentId the agent ID
     */
    suspend fun clearAgentContext(agentId: Long)
}