package com.example.day.core.core_features.agent.domain.strategy

import com.example.day.core.core_features.agent.domain.model.AContext
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.summarization.SummarizationState
import com.example.day.core.core_features.llm.domain.model.ModelSettings

/**
 * Интерфейс стратегии сжатия контекста.
 * 
 * Позволяет расширять систему новыми стратегиями в будущем:
 * - SummarizationStrategy (текущая) - суммаризация через LLM
 * - KeywordExtractionStrategy - извлечение ключевых слов
 * - FirstMessageStrategy - сохранение только первого сообщения
 * - HybridStrategy - комбинация подходов
 */
interface CompressionStrategy {
    
    /**
     * Уникальное имя стратегии
     */
    val strategyName: String

    suspend fun compress(
        context: AContext,
        modelSettings: ModelSettings
    ): Result

    data class Result(
        val isCompressed: Boolean,
        val newState: SummarizationState, // Базовый интерфейс/класс ваших состояний
        val remainingMessages: List<AContextMessage> = emptyList(),
        val compressedCount: Int = 0,
        val previousSummaryLength: Int = 0,
        val newSummaryLength: Int = 0
    )
}