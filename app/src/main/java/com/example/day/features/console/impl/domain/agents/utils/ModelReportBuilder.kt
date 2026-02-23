package com.example.day.features.console.impl.domain.agents.utils

import com.example.day.core.core_features.llm.domain.model.ModelResult
import javax.inject.Inject

/**
 * Построитель отчетов о результатах запроса модели
 */
internal class ModelReportBuilder @Inject constructor() {

    /**
     * Формирует отчет о результатах запроса к модели
     */
    fun build(
        modelName: String,
        durationSeconds: Double,
        modelResult: ModelResult.Success
    ): String = buildString {
        appendLine("📊 Отчет по модели: $modelName")
        appendLine("---")

        // Добавляем время ответа
        appendLine("⏱️ Время ответа: ${String.format("%.2f", durationSeconds)} сек.")

        // Добавляем информацию об использовании токенов
        modelResult.usage?.let { usage ->
            appendLine("📝 Токены:")
            appendLine("  - Prompt токены: ${usage.promptTokens}")
            appendLine("  - Completion токены: ${usage.completionTokens}")
            appendLine("  - Всего токенов: ${usage.totalTokens}")
            if (usage.cost != null) {
                appendLine("  - Стоимость: ${String.format("%.6f$", usage.cost)}")
            }
            appendCostDetails(usage.costDetails)
        }
    }

    private fun StringBuilder.appendCostDetails(costDetails: ModelResult.Success.CostDetails?) {
        costDetails ?: return
        appendLine("  - Детали стоимости:")
        if (costDetails.upstreamInferencePromptCost != null) {
            appendLine(
                "    - Prompt: ${
                    String.format(
                        "%.6f$",
                        costDetails.upstreamInferencePromptCost
                    )
                }"
            )
        }
        if (costDetails.upstreamInferenceCompletionsCost != null) {
            appendLine(
                "    - Completion: ${
                    String.format(
                        "%.6f$",
                        costDetails.upstreamInferenceCompletionsCost
                    )
                }"
            )
        }
    }
}