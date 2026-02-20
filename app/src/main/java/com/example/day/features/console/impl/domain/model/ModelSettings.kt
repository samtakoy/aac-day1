package com.example.day.features.console.impl.domain.model

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
) {

}