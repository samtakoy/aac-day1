package com.example.day.core.core_features.agent.domain.strategy.impl

import com.example.day.core.core_features.agent.domain.AgentContextRepository
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.AContextParams
import com.example.day.core.core_features.agent.domain.model.AContextState
import com.example.day.core.core_features.agent.domain.model.AgentConfig
import com.example.day.core.core_features.agent.domain.model.addAssistantMessage
import com.example.day.core.core_features.agent.domain.model.addUserMessage
import com.example.day.core.core_features.agent.domain.strategy.ContextSnapshot
import com.example.day.core.core_features.agent.domain.strategy.ContextStrategy
import com.example.day.core.core_features.agent.domain.strategy.ContextStrategyConstants
import com.example.day.core.core_features.agent.domain.strategy.ContextStrategyResult
import com.example.day.core.core_features.agent.domain.utils.SummarizationPrompt
import com.example.day.core.core_features.chat.domain.model.ChatSettings
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.llm.domain.model.getContent
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

/**
 * Context strategy that summarizes old messages via LLM when msgLimit is exceeded.
 *
 * - process(): prepends summary + recent messages as context for LLM
 * - afterResponse(): saves new messages, triggers compression when threshold reached
 */
class ContextSummaryStrategy(
    private val llmRequestUseCase: LlmRequestUseCase
) : ContextStrategy {

    override suspend fun process(
        agent: AgentConfig,
        store: AgentContextRepository
    ): ContextSnapshot {
        val state = store.getContextState(agent.id) as? AContextState.Summary
            ?: AContextState.Summary("", persistentListOf())
        // НЕ добавляем userPrompt — это сделает LlmRequestUseCase.exec()
        return ContextSnapshot(
            messages = state.messages.mutate { list ->
                if (state.summary.isNotBlank()) {
                    list.add(
                        0,
                        AContextMessage(
                            role = AContextMessage.Role.ASSISTANT,
                            content = "Previous conversation summary: ${state.summary}"
                        )
                    )
                }
            }
        )
    }

    override suspend fun afterResponse(
        agent: AgentConfig,
        response: String,
        store: AgentContextRepository,
        fullContext: ContextSnapshot
    ): ContextStrategyResult {
        val state = store.getContextState(agent.id) as? AContextState.Summary
            ?: AContextState.Summary("", persistentListOf())
        val params = store.getContextParams(agent.id) as? AContextParams.Summarization

        // fullContext содержит полную историю: initialHistory + prompt + assistant/tool messages
        val messagesToSave = fullContext.messages.toPersistentList()

        // Check if compression is needed
        val threshold = if (params != null) params.msgLimit + params.extraLimit else Int.MAX_VALUE
        val needsCompression = params != null && messagesToSave.size > threshold

        if (!needsCompression) {
            store.saveContextState(agent.id, state.copy(messages = messagesToSave))
            return ContextStrategyResult(null)
        }

        // Compress: summarize old messages, keep recent ones
        val toCompressCount = (messagesToSave.size - params!!.msgLimit).coerceAtLeast(0)
        val toCompress = messagesToSave.take(toCompressCount)
        val remaining = messagesToSave.drop(toCompressCount)
        val prevSummary = state.summary.takeIf { it.isNotBlank() }

        val prompt = SummarizationPrompt.buildSummarizationPrompt(prevSummary, toCompress)
        val newSummary = llmRequestUseCase.exec(
            modelSettings = agent.modelSettings,
            systemPrompt = null,
            messages = emptyList(),
            prompt = AContextMessage(AContextMessage.Role.USER, prompt),
            tools = null
        ).getOrThrow().getContent()

        store.saveContextState(
            agent.id,
            state.copy(
                summary = newSummary,
                messages = remaining.toPersistentList()
            )
        )

        return ContextStrategyResult(
            reportMessage = "Сжато $toCompressCount сообщений. Summary: ${prevSummary?.length ?: 0} → ${newSummary.length} символов"
        )
    }

    override suspend fun getInfoReport(
        agent: AgentConfig,
        store: AgentContextRepository
    ): String {
        val params = store.getContextParams(agent.id) as? AContextParams.Summarization
        val state = store.getContextState(agent.id) as? AContextState.Summary
        return buildString {
            appendLine("Стратегия: summarization")
            appendLine("Лимит сообщений (msg): ${params?.msgLimit ?: "не задан"}")
            appendLine("Буфер сжатия (extra): ${params?.extraLimit ?: "не задан"}")
            appendLine("Сообщений в контексте: ${state?.messages?.size ?: 0}")
            append("Длина summary: ${state?.summary?.length ?: 0} символов")
        }
    }

    override suspend fun getFullContextReport(
        agent: AgentConfig,
        store: AgentContextRepository
    ): String {
        val state = store.getContextState(agent.id) as? AContextState.Summary
        return buildString {
            appendLine("=== Агент: ${agent.systemName} ===")
            appendLine("Системный промпт: ${agent.systemPrompt}")
            if (!state?.summary.isNullOrBlank()) {
                appendLine()
                appendLine("--- Summary ---")
                appendLine(state!!.summary)
            }
            if (!state?.messages.isNullOrEmpty()) {
                appendLine()
                appendLine("--- Сообщения (${state!!.messages.size}) ---")
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
        val msgLimit = params[ContextStrategyConstants.PARAM_MSG_LIMIT]?.toIntOrNull() ?: return "Требуется параметр --${ContextStrategyConstants.PARAM_MSG_LIMIT}"
        val extraLimit = params[ContextStrategyConstants.PARAM_EXTRA_LIMIT]?.toIntOrNull() ?: return "Требуется параметр --${ContextStrategyConstants.PARAM_EXTRA_LIMIT}"
        store.saveContextParams(agent.id, AContextParams.Summarization(msgLimit, extraLimit))
        return "Настройки сохранены: msg=$msgLimit, extra=$extraLimit"
    }
}
