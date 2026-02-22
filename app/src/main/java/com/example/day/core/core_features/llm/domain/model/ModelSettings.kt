package com.example.day.core.core_features.llm.domain.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

data class ModelSettings(
    val name: String,
    // TODO можно просто List
    val stopSequence: ImmutableList<String> = emptyList<String>().toImmutableList(),
    val maxTokens: Int = 0,
    val jsonFormat: Boolean = false,
    val temperature: Double? = null,
    val reasoningEffort: String? = null
)