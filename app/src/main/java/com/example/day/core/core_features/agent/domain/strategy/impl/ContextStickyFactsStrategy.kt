package com.example.day.core.core_features.agent.domain.strategy.impl

import com.example.day.core.core_features.agent.domain.AgentContextRepository
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.AContextParams
import com.example.day.core.core_features.agent.domain.model.AContextState
import com.example.day.core.core_features.agent.domain.model.AgentConfig
import com.example.day.core.core_features.agent.domain.model.Role
import com.example.day.core.core_features.agent.domain.model.addAssistantMessage
import com.example.day.core.core_features.agent.domain.model.addUserMessage
import com.example.day.core.core_features.agent.domain.strategy.ContextSnapshot
import com.example.day.core.core_features.agent.domain.strategy.ContextStrategy
import com.example.day.core.core_features.agent.domain.strategy.ContextStrategyConstants
import com.example.day.core.core_features.agent.domain.strategy.ContextStrategyResult
import com.example.day.core.core_features.chat.domain.model.ChatSettings
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.llm.domain.model.getContent
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

/**
 * Context strategy that maintains "sticky facts" (key-value memory) + sliding window of recent messages.
 * 
 * - process(): returns facts block + recent messages as context for LLM
 * - afterResponse(): extracts/updates facts via LLM after each response, saves messages
 */
class ContextStickyFactsStrategy(
    private val llmRequestUseCase: LlmRequestUseCase
) : ContextStrategy {

    companion object {
        private const val DEFAULT_WINDOW = 5
        private const val DEFAULT_MAX_FACTS = 20
    }

    override suspend fun process(
        chat: ChatSettings,
        agent: AgentConfig,
        userPrompt: String?,
        store: AgentContextRepository
    ): ContextSnapshot {
        val state = store.getContextState(agent.id) as? AContextState.StickyFacts
            ?: AContextState.StickyFacts(facts = emptyMap(), messages = persistentListOf())
        
        val params = store.getContextParams(agent.id) as? AContextParams.StickyFacts
        val windowSize = params?.windowSize ?: DEFAULT_WINDOW

        // 1. Form facts block
        val factsBlock = if (state.facts.isNotEmpty()) {
            buildString {
                appendLine("CORE CONTEXT (FACTS):")
                state.facts.entries.forEach { (key, value) ->
                    appendLine("- $key: $value")
                }
            }
        } else {
            "No prior facts recorded."
        }

        // 2. Get last N messages (sliding window)
        val window = state.messages.takeLast(windowSize).toPersistentList()

        // 3. Build final context: facts first (as system), then messages
        val messages = persistentListOf<AContextMessage>(
            AContextMessage(role = Role.SYSTEM, content = factsBlock)
        ).addAll(window).addUserMessage(userPrompt)

        return ContextSnapshot(messages = messages)
    }

    override suspend fun afterResponse(
        chat: ChatSettings,
        agent: AgentConfig,
        userPrompt: String,
        response: String,
        store: AgentContextRepository
    ): ContextStrategyResult {
        val state = store.getContextState(agent.id) as? AContextState.StickyFacts
            ?: AContextState.StickyFacts(facts = emptyMap(), messages = persistentListOf())
        
        val params = store.getContextParams(agent.id) as? AContextParams.StickyFacts
        val windowSize = params?.windowSize ?: DEFAULT_WINDOW
        val maxFacts = params?.maxFacts ?: DEFAULT_MAX_FACTS

        // 1. Update message history (for next window)
        val updatedHistory = state.messages
            .addUserMessage(userPrompt)
            .addAssistantMessage(response)
            .takeLast(windowSize)
            .toPersistentList()

        // 2. Extract new facts via LLM based on last exchange
        val extractionPrompt = buildFactsExtractionPrompt(
            currentFacts = state.facts,
            messages = updatedHistory,
            maxFacts = maxFacts
        )

        val rawFacts = llmRequestUseCase.exec(
            modelSettings = chat.model,
            systemPrompt = "You are a factual memory processor. Extract and maintain key facts from conversations.",
            messages = emptyList(),
            promptText = extractionPrompt
        ).getOrNull()?.getContent() ?: ""

        val newFactsMap = parseFacts(rawFacts)

        // 3. Save updated facts and trimmed history
        store.saveContextState(
            agent.id,
            state.copy(
                facts = newFactsMap,
                messages = updatedHistory
            )
        )

        return ContextStrategyResult(
            reportMessage = "Memory updated. Facts: ${newFactsMap.size}, History: ${updatedHistory.size}"
        )
    }

    override suspend fun getInfoReport(
        agent: AgentConfig,
        store: AgentContextRepository
    ): String {
        val params = store.getContextParams(agent.id) as? AContextParams.StickyFacts
        val state = store.getContextState(agent.id) as? AContextState.StickyFacts
        val windowSize = params?.windowSize ?: DEFAULT_WINDOW
        val maxFacts = params?.maxFacts ?: DEFAULT_MAX_FACTS
        
        return buildString {
            appendLine("Стратегия: sticky facts")
            appendLine("Размер окна: $windowSize")
            appendLine("Максимум фактов: $maxFacts")
            append("Фактов в памяти: ${state?.facts?.size ?: 0}")
        }
    }

    override suspend fun getFullContextReport(
        agent: AgentConfig,
        store: AgentContextRepository
    ): String {
        val state = store.getContextState(agent.id) as? AContextState.StickyFacts
        return buildString {
            appendLine("=== Агент: ${agent.systemName} ===")
            appendLine("Системный промпт: ${agent.systemPrompt}")
            
            if (!state?.facts.isNullOrEmpty()) {
                appendLine()
                appendLine("--- FACTS (${state!!.facts.size}) ---")
                state.facts.forEach { (key, value) ->
                    appendLine("- $key: $value")
                }
            }
            
            if (!state?.messages.isNullOrEmpty()) {
                appendLine()
                appendLine("--- Сообщения в окне (${state!!.messages.size}) ---")
                state.messages.forEach { msg ->
                    appendLine("[${msg.role}] ${msg.content}")
                }
            }
        }
    }

    override suspend fun updateParams(
        agent: AgentConfig,
        params: Map<String, String>,
        store: AgentContextRepository
    ): String {
        val windowSize = params[ContextStrategyConstants.PARAM_MSG_LIMIT]?.toIntOrNull() ?: DEFAULT_WINDOW
        val maxFacts = params[ContextStrategyConstants.PARAM_MAX_FACTS]?.toIntOrNull() ?: DEFAULT_MAX_FACTS
        
        store.saveContextParams(
            agent.id,
            AContextParams.StickyFacts(windowSize = windowSize, maxFacts = maxFacts)
        )
        
        return "Настройки обновлены: окно=$windowSize, макс. фактов=$maxFacts"
    }

    /**
     * Build prompt for LLM to extract/update facts
     */
    private fun buildFactsExtractionPrompt(
        currentFacts: Map<String, String>,
        messages: List<AContextMessage>,
        maxFacts: Int
    ): String {
        return buildString {
            appendLine("Review this conversation exchange and update the fact map.")
            appendLine()
            
            if (currentFacts.isNotEmpty()) {
                appendLine("Current facts:")
                currentFacts.forEach { (key, value) ->
                    appendLine("- $key: $value")
                }
                appendLine()
            }
            
            appendLine("Recent messages:")
            messages.forEach { msg ->
                appendLine("[${msg.role.name}]: ${msg.content}")
            }
            appendLine()
            
            appendLine("Instructions:")
            appendLine("1. Keep existing facts that are still relevant")
            appendLine("2. Update facts that have changed")
            appendLine("3. Add new important facts discovered in the conversation")
            appendLine("4. Remove facts that are no longer relevant")
            appendLine("5. Maximum $maxFacts facts total")
            appendLine("6. Focus on: goals, constraints, preferences, agreements, decisions, important details")
            appendLine()
            
            appendLine("Return ONLY the updated fact map in format:")
            appendLine("Key: Value")
        }
    }

    /**
     * Parse LLM response into facts map
     */
    private fun parseFacts(raw: String): Map<String, String> {
        return raw.lines()
            .filter { it.contains(":") }
            .associate { line ->
                val parts = line.split(":", limit = 2)
                if (parts.size == 2) {
                    parts[0].trim() to parts[1].trim()
                } else {
                    "" to ""
                }
            }
            .filter { it.key.isNotBlank() && it.value.isNotBlank() }
    }
}
