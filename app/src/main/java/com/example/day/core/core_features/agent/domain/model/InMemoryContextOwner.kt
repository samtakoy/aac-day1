package com.example.day.core.core_features.agent.domain.model

import kotlinx.collections.immutable.persistentListOf

/**
 * In-memory implementation of [AContextOwner] for storing agent context in memory.
 * 
 * This is useful for MVP or testing purposes. For production, consider using
 *
 * Note: Context is stored in memory and will be lost on app restart.
 */
internal class InMemoryContextOwner : AContextOwner {

    private val contexts = mutableMapOf<Long, AContext>()

    override suspend fun getContext(agentId: Long): AContext {
        return contexts.getOrPut(agentId) {
            AContext(
                agentName = "",
                systemPrompt = "",
                messages = persistentListOf()
            )
        }
    }

    override suspend fun saveContext(agentId: Long, context: AContext) {
        contexts[agentId] = context
    }

    override suspend fun clearAgentContext(agentId: Long) {
        contexts.remove(agentId)
    }

    /**
     * Clear context for a specific agent
     */
    fun clearContext(agentId: Long) {
        contexts.remove(agentId)
    }

    /**
     * Clear all contexts
     */
    fun clearAll() {
        contexts.clear()
    }
}
