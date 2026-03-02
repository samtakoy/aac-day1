package com.example.day.core.core_features.llm.data.local.model

import kotlinx.serialization.Serializable

@Serializable
internal data class ModelSettingsEntity(
    val name: String,
    val stopSequence: List<String> = emptyList(),
    val maxTokens: Int? = null,
    val maxCompletionTokens: Int? = null,
    val jsonFormat: Boolean = false,
    val temperature: Double? = null,
    val topP: Double? = null,
    val topK: Int? = null,
    val presencePenalty: Double? = null,
    val frequencyPenalty: Double? = null,
    val seed: Int? = null,
    val reasoningEffort: String? = null
)