package com.example.day.core.core_features.llm.domain.utils

import com.example.day.core.core_features.llm.domain.model.ModelResult

/**
 * Data class for model consumption statistics with all numeric non-null fields from ModelResult.Success.
 * Note: The name is intentionally spelled with typo as specified by user.
 *
 * @param Итоговая сумма, которую списал OpenRouter за весь запрос. Это «чек», который вы оплачиваете
 * @param upstreamInferenceCost Стоимость, которую выставил провайдер модели (например, OpenAI, Anthropic или Google). Обычно она совпадает с итоговой стоимостью.
 *  Обычно это сумма upstreamInferencePromptCost + upstreamInferenceCompletionsCost
 * @param upstreamInferencePromptCost Сколько вы заплатили только за входящие данные (ваш текст вопроса, контекст, системные инструкции).
 * @param upstreamInferenceCompletionsCost Сколько вы заплатили за ответ нейросети (генерацию текста).
 */
data class ModelConsuption(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val cost: Double,
    val upstreamInferencePromptCost: Double,
    val upstreamInferenceCompletionsCost: Double,
    val upstreamInferenceCost: Double
) {
    companion object {
        /**
         * Returns a ModelConsuption with all zero values.
         */
        fun empty(): ModelConsuption = ModelConsuption(
            promptTokens = 0,
            completionTokens = 0,
            totalTokens = 0,
            cost = 0.0,
            upstreamInferencePromptCost = 0.0,
            upstreamInferenceCompletionsCost = 0.0,
            upstreamInferenceCost = 0.0
        )
    }

    /**
     * Operator function for combining two ModelConsuption objects.
     * Adds all numeric fields together.
     */
    operator fun plus(other: ModelConsuption): ModelConsuption = ModelConsuption(
        promptTokens = this.promptTokens + other.promptTokens,
        completionTokens = this.completionTokens + other.completionTokens,
        totalTokens = this.totalTokens + other.totalTokens,
        cost = this.cost + other.cost,
        upstreamInferencePromptCost = this.upstreamInferencePromptCost + other.upstreamInferencePromptCost,
        upstreamInferenceCompletionsCost = this.upstreamInferenceCompletionsCost + other.upstreamInferenceCompletionsCost,
        upstreamInferenceCost = this.upstreamInferenceCost + other.upstreamInferenceCost
    )
}

/**
 * Extension function to convert ModelResult.Success to ModelConsuption.
 * Extracts all numeric fields from Usage and CostDetails, using 0 for null values.
 */
fun ModelResult.Success.toModelConsumption(): ModelConsuption {
    val usage = this.usage
    val costDetails = usage?.costDetails

    return ModelConsuption(
        promptTokens = usage?.promptTokens ?: 0,
        completionTokens = usage?.completionTokens ?: 0,
        totalTokens = usage?.totalTokens ?: 0,
        cost = usage?.cost ?: 0.0,
        upstreamInferencePromptCost = costDetails?.upstreamInferencePromptCost ?: 0.0,
        upstreamInferenceCompletionsCost = costDetails?.upstreamInferenceCompletionsCost ?: 0.0,
        upstreamInferenceCost = costDetails?.upstreamInferenceCost ?: 0.0
    )
}
