package com.example.day.core.core_features.llm.domain.model

import kotlinx.collections.immutable.ImmutableList

sealed interface ModelResult {

    data class Success(
        val id: String,
        val model: String,
        val choices: ImmutableList<Choice>,
        val usage: Usage? = null
    ) : ModelResult {
        data class Choice(
            val message: Message,
            val finishReason: String?
        )

        data class Message(
            val role: String,
            val content: String?,
            val reasoning: String?,
            val toolCalls: ImmutableList<ToolCall>? = null
        )

        data class ToolCall(
            val id: String,
            val type: String,
            val function: FunctionCall
        )

        data class FunctionCall(
            val name: String,
            val arguments: String
        )

        /**
         * Информация об использовании токенов и стоимости
         *
         * @property promptTokens Количество токенов в запросе
         * @property completionTokens Количество токенов в ответе
         * @property totalTokens Общее количество использованных токенов
         * @property cost Общая стоимость в USD
         * @property costDetails Детализация стоимости
         */
        data class Usage(
            val promptTokens: Int?,
            val completionTokens: Int?,
            val totalTokens: Int?,
            val cost: Double?,
            val costDetails: CostDetails?
        )

        /**
         * Детализация стоимости от OpenRouter
         *
         * @property upstreamInferencePromptCost Стоимость входных данных
         * @property upstreamInferenceCompletionsCost Стоимость сгенерированного ответа
         * @property upstreamInferenceCost Общая стоимость вывода
         */
        data class CostDetails(
            val upstreamInferencePromptCost: Double?,
            val upstreamInferenceCompletionsCost: Double?,
            val upstreamInferenceCost: Double?
        )
    }

    data class Error(
        val message: String,
        val code: Int? = null,
        // val metadata: String? = null,
        val param: String? = null,
        val type: String? = null
    ) : ModelResult

    data class RuntimeError(
        val message: String
    ) : ModelResult
}
