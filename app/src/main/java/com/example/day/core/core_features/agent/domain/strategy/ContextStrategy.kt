package com.example.day.core.core_features.agent.domain.strategy

import com.example.day.core.core_features.agent.domain.AgentContextRepository
import com.example.day.core.core_features.agent.domain.model.AContext
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.AgentConfig
import com.example.day.core.core_features.chat.domain.model.ChatSettings

/**
 * Abstract base class for context management strategies.
 * Each strategy transforms chat history into optimized context for LLM.
 */
interface ContextStrategy {
    /**
     * Process chat history and return optimized context.
     * @param userPrompt не обязан присутствовать, можно дернуть llm и без него
     */
    suspend fun process(
        chat: ChatSettings,
        agent: AgentConfig,
        userPrompt: String?,
        store: AgentContextRepository
    ): ContextSnapshot

    /**
     * Called after LLM response is received.
     * Saves messages to context and performs any post-processing (e.g. compression).
     * @return ContextStrategyResult with optional report message
     */
    suspend fun afterResponse(
        chat: ChatSettings,
        agent: AgentConfig,
        userPrompt: String,
        response: String,
        store: AgentContextRepository
    ): ContextStrategyResult

    /**
     * Return formatted info about current context parameters/state.
     */
    suspend fun getInfoReport(
        agent: AgentConfig,
        store: AgentContextRepository
    ): String = "No info available"

    /**
     * Return full context contents formatted for display.
     */
    suspend fun getFullContextReport(
        agent: AgentConfig,
        store: AgentContextRepository
    ): String = "No context available"

    /**
     * Update strategy parameters using a map of parameter names to values.
     * Each strategy extracts the parameters it needs by known parameter names.
     * @return confirmation message
     */
    suspend fun updateParams(
        agent: AgentConfig,
        params: Map<String, String>,
        store: AgentContextRepository
    ): String = "Strategy doesn't support parameter updates"
}

/**
 * Result of strategy processing - optimized context for LLM.
 */
data class ContextSnapshot(
    val messages: List<AContextMessage>,
    // TODO?
    val metadata: Map<String, Any> = emptyMap()  // Additional data like topic weights, etc.
)