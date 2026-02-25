package com.example.day.core.core_features.agent.domain.model

/**
 * Interface for database-backed context owner.
 * Provides methods for storing and retrieving agent context from the database.
 * 
 * This interface extends [AContextOwner] and provides concrete implementation
 * for persisting agent context across app restarts.
 */
interface DbContextOwner {
    
    /**
     * Get agent context from database.
     * @param agentId the agent ID
     * @return AContext if found, null otherwise
     */
    suspend fun getAgentContext(agentId: Long): AContext?
    
    /**
     * Save agent context to database.
     * @param agentId the agent ID
     * @param context the context to save
     */
    suspend fun saveAgentContext(agentId: Long, context: AContext)
    
    /**
     * Clear agent context from database.
     * @param agentId the agent ID
     */
    suspend fun clearAgentContext(agentId: Long)
}
