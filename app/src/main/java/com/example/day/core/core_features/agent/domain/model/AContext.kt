package com.example.day.core.core_features.agent.domain.model

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap

data class AContext(
    val params: AContextParams,
    val data: AContextState,
)

sealed interface AContextState {
    data class Summary(
        val summary: String,
        val messages: PersistentList<AContextMessage>
    ) : AContextState

    data class SlidingWindow(
        val messages: PersistentList<AContextMessage>
    ) : AContextState

    data class Full(
        val messages: PersistentList<AContextMessage>
    ) : AContextState

    /**
     * Sticky Facts state - хранит факты (key-value) + окно последних сообщений.
     */
    data class StickyFacts(
        val facts: Map<String, String>,
        val messages: PersistentList<AContextMessage>
    ) : AContextState

    /**
     * Branching state - хранит Map веток и ID текущей активной ветки.
     * @param branches Map: branchId -> список сообщений ветки
     * @param currentBranchId ID активной ветки
     * @param defaultBranchId ID ветки по умолчанию (основной ветки)
     */
    data class Branching(
        val branches: PersistentMap<String, PersistentList<AContextMessage>>,
        val currentBranchId: String,
        val defaultBranchId: String
    ) : AContextState

    data object Empty : AContextState
}

sealed interface AContextParams {
    data class Summarization(
        val msgLimit: Int, val extraLimit: Int
    ) : AContextParams

    data class SlidingWindow(
        val windowSize: Int
    ) : AContextParams

    /**
     * Sticky Facts параметры - хранит факты (key-value) + окно последних сообщений.
     * @param windowSize количество последних сообщений в окне
     * @param maxFacts максимальное количество фактов для хранения
     */
    data class StickyFacts(
        val windowSize: Int,
        val maxFacts: Int = 20
    ) : AContextParams

    /**
     * Branching параметры - параметры стратегии ветвления.
     * @param defaultBranchId имя основной ветки по умолчанию
     */
    data class Branching(
        val defaultBranchId: String = "main"
    ) : AContextParams

    data object Full : AContextParams

    data object Empty : AContextParams
}