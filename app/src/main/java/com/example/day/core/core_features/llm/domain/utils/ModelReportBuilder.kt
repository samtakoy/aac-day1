package com.example.day.core.core_features.llm.domain.utils

import com.example.day.core.core_features.llm.domain.model.ModelResult
import javax.inject.Inject

/**
 * Builder for reports about model query results.
 */
internal class ModelReportBuilder @Inject constructor() {

    /**
     * Builds a report about model query results.
     */
    fun build(
        modelName: String,
        durationSeconds: Double,
        modelResult: ModelResult.Success
    ): String = buildString {
        appendLine("📊 Отчет по модели: $modelName")
        appendLine("---")

        // Add response time
        appendLine("⏱️ Время ответа: ${String.format("%.2f", durationSeconds)} сек.")

        // Add token usage information
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
