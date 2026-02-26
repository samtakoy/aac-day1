package com.example.day.core.core_features.agent.domain.strategy

import com.example.day.core.core_features.agent.domain.model.AContext
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.summarization.SummarizationEnabledState
import com.example.day.core.core_features.agent.domain.utils.SummarizationPrompt
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.llm.domain.model.ModelSettings
import com.example.day.core.core_features.llm.domain.model.getContent
import javax.inject.Inject

/**
 * Реализация стратегии сжатия через суммаризацию.
 * 
 * Использует LLM для создания summary на основе предыдущего summary
 * и новых сообщений, которые нужно сжать.
 */
class SummarizationUseCase @Inject constructor(
    private val llmRequestUseCase: LlmRequestUseCase,
) : CompressionStrategy {
    
    override val strategyName: String = "summarization"

    override suspend fun compress(
        context: AContext,
        modelSettings: ModelSettings
    ): CompressionStrategy.Result {
        val state = context.summarizationState

        // Если сжатие вообще отключено в настройках контекста — выходим
        if (state !is SummarizationEnabledState) {
            return CompressionStrategy.Result(isCompressed = false, newState = state)
        }

        val messages = context.messages
        val messagesToCompressCount = (messages.size - state.msgLimit).coerceAtLeast(0)

        // Если лимит не превышен — ничего не делаем
        if (messagesToCompressCount <= 0) {
            return CompressionStrategy.Result(isCompressed = false, newState = state)
        }

        val messagesToCompress = messages.take(messagesToCompressCount)
        val remainingMessages = messages.drop(messagesToCompressCount)
        val previousSummary = state.retrieveSummary()

        val prompt = SummarizationPrompt.buildSummarizationPrompt(
            previousSummary = previousSummary,
            messagesToSummarize = messagesToCompress
        )

        val newSummary = llmRequestUseCase.exec(
            modelSettings = modelSettings,
            systemPrompt = null,
            messages = emptyList(),
            promptText = prompt
        ).getOrThrow().getContent()

        // 3. Формируем результат с обновленным состоянием
        return CompressionStrategy.Result(
            isCompressed = true,
            newState = state.withSummary(newSummary),
            remainingMessages = remainingMessages,
            compressedCount = messagesToCompressCount,
            previousSummaryLength = previousSummary?.length ?: 0,
            newSummaryLength = newSummary.length
        )
    }
}
