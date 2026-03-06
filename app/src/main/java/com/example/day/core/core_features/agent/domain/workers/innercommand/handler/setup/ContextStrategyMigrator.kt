package com.example.day.core.core_features.agent.domain.workers.innercommand.handler.setup

import com.example.day.core.core_features.agent.domain.AgentContextRepository
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.AContextParams
import com.example.day.core.core_features.agent.domain.model.AContextState
import com.example.day.core.core_features.agent.domain.strategy.CtxStrategyType
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
import javax.inject.Inject

/**
 * Handles extraction and migration of context messages between strategies.
 */
class ContextStrategyMigrator @Inject constructor(
    private val contextRepository: AgentContextRepository
) {

    /**
     * Extracts messages from current strategy state regardless of type.
     */
    suspend fun extractMessages(agentId: Long): List<AContextMessage> {
        val state = contextRepository.getContextState(agentId)
        return when (state) {
            is AContextState.Summary -> state.messages
            is AContextState.SlidingWindow -> state.messages
            is AContextState.StickyFacts -> state.messages
            is AContextState.Branching -> state.branches[state.currentBranchId] ?: emptyList()
            is AContextState.Full -> state.messages
            AContextState.Empty, null -> emptyList()
        }
    }

    /**
     * Creates initial state for a target strategy with migrated messages.
     */
    fun createInitialState(
        targetStrategy: CtxStrategyType,
        messages: List<AContextMessage>,
        params: AContextParams
    ): AContextState = when (targetStrategy) {
        CtxStrategyType.SUMMARIZATION -> {
            require(params is AContextParams.Summarization)
            AContextState.Summary(summary = "", messages = messages.toPersistentList())
        }
        CtxStrategyType.SLIDING_WINDOW -> {
            require(params is AContextParams.SlidingWindow)
            val windowMessages = messages.takeLast(params.windowSize)
            AContextState.SlidingWindow(messages = windowMessages.toPersistentList())
        }
        CtxStrategyType.STICKY_FACTS -> {
            require(params is AContextParams.StickyFacts)
            val windowMessages = messages.takeLast(params.windowSize)
            AContextState.StickyFacts(facts = emptyMap(), messages = windowMessages.toPersistentList())
        }
        CtxStrategyType.BRANCHING -> {
            require(params is AContextParams.Branching)
            AContextState.Branching(
                branches = persistentMapOf(params.defaultBranchId to messages.toPersistentList()),
                currentBranchId = params.defaultBranchId,
                defaultBranchId = params.defaultBranchId
            )
        }
        CtxStrategyType.FULL_CONTEXT -> AContextState.Full(messages = messages.toPersistentList())
        CtxStrategyType.EMPTY -> AContextState.Empty
    }
}
