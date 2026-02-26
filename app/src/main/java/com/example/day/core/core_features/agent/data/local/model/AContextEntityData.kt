package com.example.day.core.core_features.agent.data.local.model

import com.example.day.core.core_features.agent.domain.model.AContext.Companion.NO_SUMMARY_LIMIT
import kotlinx.serialization.Serializable

/**
 * Data layer representation of summarization state for JSON serialization.
 * Groups all summarization-related fields together.
 */
@Serializable
data class SummarizationStateEntityData(
    val strategy: String = "none",    // none или summarization
    val msgLimit: Int = NO_SUMMARY_LIMIT,
    val extraLimit: Int = 8,
    val summary: String? = null
)

/**
 * Data layer representation of AContext for JSON serialization.
 * Mirrors [com.example.day.core.core_features.agent.domain.model.AContext]
 */
@Serializable
data class AContextEntityData(
    val agentName: String,
    val systemPrompt: String,
    val messages: List<AContextMessageEntityData>,  // Use List instead of ImmutableList for serialization
    // Состояние стратегии сжатия (сгруппировано в отдельную сущность)
    val summarizationState: SummarizationStateEntityData = SummarizationStateEntityData()
)

/**
 * Data layer representation of AContextMessage for JSON serialization.
 * Mirrors [com.example.day.core.core_features.agent.domain.model.AContextMessage]
 */
@Serializable
data class AContextMessageEntityData(
    val role: String,  // Role enum serialized as String
    val content: String,
    val orderNumber: Long
)
