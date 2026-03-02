package com.example.day.core.core_features.agent.domain

import com.example.day.core.core_features.agent.domain.model.AContext
import com.example.day.core.core_features.agent.domain.model.AContextParams
import com.example.day.core.core_features.agent.domain.model.AContextState

interface AgentContextRepository {

    /**
     * Save agent context (conversation history)
     */
    suspend fun saveContext(agentId: Long, context: AContext)

    /**
     * Get agent context
     */
    suspend fun getContext(agentId: Long): AContext?

    /**
     * Save agent context (conversation history)
     */
    suspend fun saveContextParams(agentId: Long, params: AContextParams)

    /**
     * Get agent context
     */
    suspend fun getContextParams(agentId: Long): AContextParams?

    /**
     * Save agent context (conversation history)
     */
    suspend fun saveContextState(agentId: Long, context: AContextState)

    /**
     * Get agent context
     */
    suspend fun getContextState(agentId: Long): AContextState?

    /**
     * Clear agent context
     */
    suspend fun clearAgentContext(agentId: Long)
}
