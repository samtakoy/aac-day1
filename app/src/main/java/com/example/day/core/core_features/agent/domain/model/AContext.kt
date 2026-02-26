package com.example.day.core.core_features.agent.domain.model

import com.example.day.core.core_features.agent.domain.model.summarization.SummarizationState
import kotlinx.collections.immutable.ImmutableList

data class AContext(
    val agentName: String,
    val systemPrompt: String,
    val messages: ImmutableList<AContextMessage>,
    // Состояние стратегии сжатия (инкапсулирует и тип и параметры)
    val summarizationState: SummarizationState = SummarizationState.disabled()
) {
    companion object {
        const val NO_SUMMARY_LIMIT = Int.MAX_VALUE
        const val DEFAULT_MSG_LIMIT = NO_SUMMARY_LIMIT
        const val DEFAULT_EXTRA_LIMIT = 5
        const val DEFAULT_STRATEGY = "summarization"
    }
}