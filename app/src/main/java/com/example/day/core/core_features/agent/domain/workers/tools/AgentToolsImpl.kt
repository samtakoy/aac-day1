package com.example.day.core.core_features.agent.domain.workers.tools

import com.example.day.core.core_features.agent.domain.model.AContext
import com.example.day.core.core_features.agent.domain.model.Agent
import com.example.day.core.core_features.agent.domain.usecase.ClearAgentContextUseCase
import com.example.day.core.core_features.agent.domain.usecase.GetAgentContextUseCase
import com.example.day.core.core_features.agent.domain.usecase.GetOrCreateAgentUseCase
import com.example.day.core.core_features.agent.domain.usecase.SaveAgentContextUseCase
import kotlinx.collections.immutable.persistentListOf
import javax.inject.Inject

/**
 * Implementation of [AgentTools] using Use Cases for agent management
 * and persistent context storage.
 * 
 * Follows Clean Architecture - uses Use Cases rather than direct repository access.
 */
internal class AgentToolsImpl @Inject constructor(
    private val getOrCreateAgentUseCase: GetOrCreateAgentUseCase,
    private val getAgentContextUseCase: GetAgentContextUseCase,
    private val saveAgentContextUseCase: SaveAgentContextUseCase,
    private val clearAgentContextUseCase: ClearAgentContextUseCase
) : AgentTools {

    override suspend fun getOrCreateAgent(
        systemName: String,
        chatId: Long,
        isCommonAgent: Boolean
    ): Agent {
        return getOrCreateAgentUseCase.invoke(
            systemName = systemName,
            isCommon = isCommonAgent,
            chatId = chatId
        )
    }

    override suspend fun getContext(agentId: Long): AContext {
        return getAgentContextUseCase.invoke(agentId) ?: AContext(
            agentName = "",
            systemPrompt = "",
            messages = persistentListOf()
        )
    }

    override suspend fun saveContext(agentId: Long, context: AContext) {
        saveAgentContextUseCase.invoke(agentId, context)
    }

    override suspend fun clearAgentContext(agentId: Long) {
        clearAgentContextUseCase.invoke(agentId)
    }
}
