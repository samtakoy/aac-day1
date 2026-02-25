package com.example.day.core.core_features.agent.domain.workers.tools

import com.example.day.core.core_features.agent.domain.model.AContext
import com.example.day.core.core_features.agent.domain.model.Agent

/**
 * Interface for agent operations that workers use.
 * Provides methods for agent lifecycle management and context handling.
 * 
 * Part of the agent domain layer - uses Use Cases for Clean Architecture.
 */
interface AgentTools {
    
    /**
     * Get or create an agent for use.
     * 
     * @param systemName system name of the agent
     * @param chatId current chat identifier
     * @param isCommonAgent if true, agent is not bound to specific chat
     * @return Agent instance
     */
    suspend fun getOrCreateAgent(
        systemName: String,
        chatId: Long,
        isCommonAgent: Boolean
    ): Agent
    
    /**
     * Get context for an agent.
     * @param agentId the agent ID
     * @return AContext if found
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
