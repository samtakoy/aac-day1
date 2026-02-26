package com.example.day.core.core_features.agent.domain.workers.handler

import com.example.day.core.core_features.agent.domain.model.AContext
import com.example.day.core.core_features.agent.domain.model.summarization.SummarizationEnabledState
import com.example.day.core.core_features.agent.domain.strategy.CompressionStrategy
import com.example.day.core.core_features.llm.domain.model.ModelSettings
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

/**
 *
 * Обработчик сжатия контекста.
 * 
 * Отвечает за:
 * - Проверку необходимости сжатия
 * - Выполнение сжатия через стратегию
 * - Обновление контекста
 * 
 * Использует паттерн Strategy для различных алгоритмов сжатия.
 */
class ContextCompressionHandler @Inject constructor(
    private val compressionStrategy: CompressionStrategy,
) {
    /**
     * Обработать оптимизацию контекста.
     * Проверяет лимиты и при необходимости выполняет сжатие.
     * 
     * @param context текущий контекст
     * @param modelSettings настройки модели для LLM
     * @return пара: обновлённый контекст и результат оптимизации
     */
    suspend fun processOptimization(
        context: AContext,
        modelSettings: ModelSettings
    ): AContext {

        // Стратегия сама решит, глядя в context.summarizationState,
        // нужно ли ей работать или вернуть context "как есть".
        val result = compressionStrategy.compress(context, modelSettings)

        // Если сжатия не было, просто возвращаем старый контекст
        return if (!result.isCompressed) {
            return context
        } else {
            context.copy(
                summarizationState = result.newState,
                messages = result.remainingMessages.toImmutableList()
            )
        }
    }
}
