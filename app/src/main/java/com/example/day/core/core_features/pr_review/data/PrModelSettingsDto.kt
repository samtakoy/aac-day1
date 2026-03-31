package com.example.day.core.core_features.pr_review.data

import com.example.day.core.core_features.llm.domain.model.ModelSettings
import kotlinx.serialization.Serializable

@Serializable
internal data class PrModelSettingsDto(
    val name: String,
    val maxTokens: Int? = null,
    val maxCompletionTokens: Int? = null,
    val temperature: Double? = null,
    val topP: Double? = null,
    val topK: Int? = null,
    val presencePenalty: Double? = null,
    val frequencyPenalty: Double? = null,
    val seed: Int? = null,
    val reasoningEffort: String? = null,
    val isLocal: Boolean = false
)

internal fun ModelSettings.toDto() = PrModelSettingsDto(
    name = name,
    maxTokens = maxTokens,
    maxCompletionTokens = maxCompletionTokens,
    temperature = temperature,
    topP = topP,
    topK = topK,
    presencePenalty = presencePenalty,
    frequencyPenalty = frequencyPenalty,
    seed = seed,
    reasoningEffort = reasoningEffort,
    isLocal = isLocal
)

internal fun PrModelSettingsDto.toDomain() = ModelSettings(
    name = name,
    maxTokens = maxTokens,
    maxCompletionTokens = maxCompletionTokens,
    temperature = temperature,
    topP = topP,
    topK = topK,
    presencePenalty = presencePenalty,
    frequencyPenalty = frequencyPenalty,
    seed = seed,
    reasoningEffort = reasoningEffort,
    isLocal = isLocal
)
