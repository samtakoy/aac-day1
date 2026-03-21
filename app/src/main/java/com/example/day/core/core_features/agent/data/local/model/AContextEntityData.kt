package com.example.day.core.core_features.agent.data.local.model

import kotlinx.serialization.Serializable

/** TODO настройки хранить отдельно - чтобы не гонять каждый раз список сообщений да еще и через мапперы */
@Serializable
sealed interface AContextEntityData {
    /** состояние стратегии саммаризации контекста */
    @Serializable
    data class Summarization(
        val summary: String,
        val messages: List<AContextMessageEntityData>
    ) : AContextEntityData
    /** состояние стратегии скользящего окна контекста */
    @Serializable
    data class SlidingWindow(
        val messages: List<AContextMessageEntityData>
    ) : AContextEntityData
    /** состояние стратегии "храним все" контекста */
    @Serializable
    data class Full(
        val messages: List<AContextMessageEntityData>
    ) : AContextEntityData
    /** состояние стратегии Sticky Facts - хранит факты (key-value) + окно сообщений */
    @Serializable
    data class StickyFacts(
        val facts: Map<String, String>,
        val messages: List<AContextMessageEntityData>
    ) : AContextEntityData

    /** состояние стратегии ветвления - хранит Map веток и ID текущей ветки */
    @Serializable
    data class Branching(
        val branches: Map<String, List<AContextMessageEntityData>>,
        val currentBranchId: String,
        val defaultBranchId: String
    ) : AContextEntityData

    @Serializable
    data object Empty : AContextEntityData
}

@Serializable
sealed interface AContextEntitySettings {
    /** состояние стратегии саммаризации контекста */
    @Serializable
    data class Summarization(
        val msgLimit: Int,
        val extraLimit: Int,
    ) : AContextEntitySettings
    /** состояние стратегии скользящего окна контекста */
    @Serializable
    data class SlidingWindow(
        val windowSize: Int,
    ) : AContextEntitySettings
    /** состояние стратегии "храним все" контекста */
    @Serializable
    data object Full : AContextEntitySettings
    /** параметры стратегии Sticky Facts */
    @Serializable
    data class StickyFacts(
        val windowSize: Int,
        val maxFacts: Int
    ) : AContextEntitySettings

    /** параметры стратегии ветвления */
    @Serializable
    data class Branching(
        val defaultBranchId: String
    ) : AContextEntitySettings

    @Serializable
    data object Empty : AContextEntitySettings
}

/**
 * Data layer representation of AContextMessage for JSON serialization.
 * Mirrors [com.example.day.core.core_features.agent.domain.model.AContextMessage]
 */
@Serializable
data class AContextMessageEntityData(
    val role: String,  // Role enum serialized as String
    val content: String?,
    val toolCallId: String? = null,
    val toolCalls: List<ToolCallRefEntityData>? = null
)

/**
 * Data layer representation of tool call reference.
 * Mirrors [com.example.day.core.core_features.agent.domain.model.AContextMessage.ToolCallRef]
 */
@Serializable
data class ToolCallRefEntityData(
    val id: String,
    val type: String,
    val functionName: String,
    val arguments: String
)
