package com.example.day.core.core_features.llm.domain.model

import kotlinx.collections.immutable.ImmutableList

sealed interface ModelResult {

    data class Success(
        val id: String,
        val model: String,
        val choices: ImmutableList<Choice>
    ) : ModelResult {
        data class Choice(
            val message: Message,
            val finishReason: String?
        )

        data class Message(
            val role: String,
            val content: String,
            val reasoning: String?
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