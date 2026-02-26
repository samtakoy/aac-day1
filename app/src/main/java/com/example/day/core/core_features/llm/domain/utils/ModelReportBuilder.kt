package com.example.day.core.core_features.llm.domain.utils

import javax.inject.Inject

/**
 * Builder for reports about model query results.
 */
class ModelReportBuilder @Inject constructor() {

    /**
     * Builds a report about model query results.
     */
    fun build(
        modelName: String,
        durationSeconds: Double,
        consumption: ModelConsuption
    ): String = buildString {
        appendLine("📊 Отчет по модели: $modelName")
        appendLine("---")

        // Add response time
        appendLine("⏱️ Время ответа: ${String.format("%.2f", durationSeconds)} сек.")
        appendLine()
        append(buildConsuption(consumption))
    }

    /** Расход токенов и стоимость */
    fun buildConsuption(consumption: ModelConsuption): String = buildString {
        // Add token usage information
        appendLine("📝 Токены:   ${consumption.totalTokens}")
        appendLine("  - Prompt/Completion: ${consumption.promptTokens} + ${consumption.completionTokens}")
        // appendLine("  - Prompt токены: ${modelConsumption.promptTokens}")
        // appendLine("  - Completion токены: ${modelConsumption.completionTokens}")
        // appendLine("  - Всего токенов: ${consumption.totalTokens}")
        appendLine("💰 Стоимость:  ${String.format("%.6f$", consumption.cost)}")
        appendCostDetails(consumption)
    }

    /** Сколько взял провайдер */
    private fun StringBuilder.appendCostDetails(consumption: ModelConsuption) {
        val promptCost = String.format("%.6f$", consumption.upstreamInferencePromptCost)
        val completionCost = String.format("%.6f$", consumption.upstreamInferenceCompletionsCost)
        val totalCost = String.format("%.6f$", consumption.upstreamInferenceCost)
        // appendLine("  - Upstream inference cost:")
        appendLine("  - Провайдер: $totalCost")
        appendLine("  - Prompt/Completion провайдера:  $promptCost / $completionCost")
    }
}
