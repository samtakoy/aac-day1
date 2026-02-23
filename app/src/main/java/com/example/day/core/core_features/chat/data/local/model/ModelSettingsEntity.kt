package com.example.day.core.core_features.chat.data.local.model

import kotlinx.serialization.Serializable

@Serializable
internal data class ModelSettingsEntity(
    val name: String,
    val stopSequence: List<String> = emptyList(),
    val maxTokens: Int = 0,
    val jsonFormat: Boolean = false,
    val temperature: Double? = null,
    val reasoningEffort: String? = null
)
