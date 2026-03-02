package com.example.day.core.core_features.agent.domain.workers.tools

import com.example.day.core.core_features.agent.domain.model.AContext
import com.example.day.core.core_features.agent.domain.model.AContextParams
import com.example.day.core.core_features.agent.domain.model.AContextState
import com.example.day.core.core_features.agent.domain.model.AgentConfig
import com.example.day.core.core_features.agent.domain.model.toModelRequestMessages
import com.example.day.core.core_features.agent.domain.usecase.ClearAgentContextUseCase
import com.example.day.core.core_features.agent.domain.usecase.GetAgentContextUseCase
import com.example.day.core.core_features.agent.domain.usecase.GetOrCreateAgentUseCase
import com.example.day.core.core_features.agent.domain.usecase.SaveAgentContextUseCase
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelSettings
import kotlinx.collections.immutable.persistentListOf
import javax.inject.Inject

/**
 * Implementation of [AgentTools] using Use Cases for agent management
 * and persistent context storage.
 *
 * Follows Clean Architecture - uses Use Cases rather than direct repository access.
 *
 * Note: context compression methods (saveContextParameters, getFullContext,
 * processContextOptimization, shouldCompress) are kept for API compatibility
 * but are no longer used by TalkWorker — that logic has moved to ContextStrategy subclasses.
 */
internal class AgentToolsImpl @Inject constructor(
    private val getOrCreateAgentUseCase: GetOrCreateAgentUseCase,
    private val getAgentContextUseCase: GetAgentContextUseCase,
    private val saveAgentContextUseCase: SaveAgentContextUseCase,
    private val clearAgentContextUseCase: ClearAgentContextUseCase,
) : AgentTools {

    override suspend fun getOrCreateAgent(
        systemName: String,
        chatId: Long,
        isCommonAgent: Boolean
    ): AgentConfig {
        TODO("не используем")
        /*
        return getOrCreateAgentUseCase.invoke(
            systemName = systemName,
            isCommon = isCommonAgent,
            chatId = chatId
        )*/
    }

    override suspend fun getContext(agentId: Long): AContext {
        return getAgentContextUseCase.invoke(agentId) ?: createDefaultContext()
    }

    override suspend fun saveContext(agentId: Long, context: AContext) {
        saveAgentContextUseCase.invoke(agentId, context)
    }

    override suspend fun clearAgentContext(agentId: Long) {
        clearAgentContextUseCase.invoke(agentId)
    }

    // ==================== Context Compression Methods (legacy, not used by TalkWorker) ====================

    override suspend fun saveContextParameters(
        agentId: Long,
        msgLimit: Int,
        extraLimit: Int,
        strategy: String
    ) {
        // No-op: context parameters are now managed by ContextStrategy via AgentContextRepository
    }

    override suspend fun getFullContext(context: AContext): List<ModelRequest.Message> {
        return when (val state = context.data) {
            is AContextState.Summary -> buildList {
                if (state.summary.isNotBlank()) {
                    add(
                        ModelRequest.Message(
                            role = ModelRequest.Role.Assistant,
                            content = "Previous conversation summary: ${state.summary}"
                        )
                    )
                }
                addAll(state.messages.toModelRequestMessages())
            }
            is AContextState.Full -> state.messages.toModelRequestMessages()
            is AContextState.SlidingWindow -> state.messages.toModelRequestMessages()
            is AContextState.StickyFacts -> state.messages.toModelRequestMessages()
            is AContextState.Branching -> state.branches[state.currentBranchId]?.toModelRequestMessages() ?: emptyList()
            AContextState.Empty -> emptyList()
        }
    }

    override suspend fun processContextOptimization(
        context: AContext,
        modelSettings: ModelSettings
    ): AContext {
        // No-op: compression is now handled by ContextSummaryStrategy.afterResponse()
        return context
    }

    override suspend fun shouldCompress(agentId: Long): Boolean {
        // No-op: compression check is now handled by ContextSummaryStrategy
        return false
    }

    private fun createDefaultContext(): AContext {
        return AContext(
            params = AContextParams.Full,
            data = AContextState.Full(persistentListOf())
        )
    }
}
