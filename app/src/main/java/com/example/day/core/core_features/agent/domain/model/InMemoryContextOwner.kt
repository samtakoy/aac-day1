package com.example.day.core.core_features.agent.domain.model

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * In-memory реализация [AContextOwner] для хранения контекста агента в памяти.
 * 
 * Для MVP (без ограничений памяти, без привязки к чату)
 */
internal class InMemoryContextOwner : AContextOwner {

    private val contexts = mutableMapOf<String, AContext>()

    override fun getContext(agentName: String): AContext {
        return contexts.getOrPut(agentName) {
            AContext(
                agentName = agentName,
                systemPrompt = "",
                messages = persistentListOf()
            )
        }
    }

    override fun saveContext(context: AContext) {
        contexts[context.agentName] = context
    }

    /**
     * Очистить контекст конкретного агента
     */
    fun clearContext(agentName: String) {
        contexts.remove(agentName)
    }

    /**
     * Очистить все контексты
     */
    fun clearAll() {
        contexts.clear()
    }
}
