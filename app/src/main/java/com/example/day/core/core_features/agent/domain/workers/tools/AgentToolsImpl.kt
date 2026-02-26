package com.example.day.core.core_features.agent.domain.workers.tools

import com.example.day.core.core_features.agent.domain.model.AContext
import com.example.day.core.core_features.agent.domain.model.AContext.Companion.DEFAULT_EXTRA_LIMIT
import com.example.day.core.core_features.agent.domain.model.AContext.Companion.NO_SUMMARY_LIMIT
import com.example.day.core.core_features.agent.domain.model.Agent
import com.example.day.core.core_features.agent.domain.model.ContextParameters
import com.example.day.core.core_features.agent.domain.model.summarization.SummarizationEnabledState
import com.example.day.core.core_features.agent.domain.model.summarization.SummarizationState
import com.example.day.core.core_features.agent.domain.model.toModelRequestMessages
import com.example.day.core.core_features.agent.domain.usecase.ClearAgentContextUseCase
import com.example.day.core.core_features.agent.domain.usecase.GetAgentContextUseCase
import com.example.day.core.core_features.agent.domain.usecase.GetOrCreateAgentUseCase
import com.example.day.core.core_features.agent.domain.usecase.SaveAgentContextUseCase
import com.example.day.core.core_features.agent.domain.workers.handler.ContextCompressionHandler
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelSettings
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
    private val clearAgentContextUseCase: ClearAgentContextUseCase,
    private val contextCompressionHandler: ContextCompressionHandler
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
        return getAgentContextUseCase.invoke(agentId) ?: createDefaultContext()
    }

    override suspend fun saveContext(agentId: Long, context: AContext) {
        saveAgentContextUseCase.invoke(agentId, context)
    }

    override suspend fun clearAgentContext(agentId: Long) {
        clearAgentContextUseCase.invoke(agentId)
    }
    
    // ==================== Context Compression Methods ====================
    
    override suspend fun saveContextParameters(
        agentId: Long,
        msgLimit: Int,
        extraLimit: Int,
        strategy: String
    ) {
        val context = getContext(agentId)
        
        val newState = when (strategy) {
            "summarization" -> SummarizationState.enabled(msgLimit, extraLimit)
            else -> SummarizationState.disabled()
        }
        
        val updatedContext = context.copy(summarizationState = newState)
        saveContext(agentId, updatedContext)
    }
    
    override suspend fun getFullContext(context: AContext): List<ModelRequest.Message> {
        return buildList {
            // Если есть summary - добавить как первое сообщение
            val summary = context.summarizationState.retrieveSummary()
            if (!summary.isNullOrBlank()) {
                add(
                    ModelRequest.Message(
                        role = ModelRequest.Role.Assistant,
                        content = "Previous conversation summary: $summary"
                    )
                )
            }
            addAll(context.messages.toModelRequestMessages())
        }
    }
    
    override suspend fun processContextOptimization(
        context: AContext,
        modelSettings: ModelSettings
    ): AContext {
        return if (!context.summarizationState.shouldCompress(context.messages.size)) {
            context
        } else {
            // Делегируем обработку в ContextCompressionHandler (TODO не понятно зачем, но делегируем)
            contextCompressionHandler.processOptimization(
                context = context,
                modelSettings = modelSettings
            )
        }
    }
    
    override suspend fun shouldCompress(agentId: Long): Boolean {
        val context = getContext(agentId)
        return context.summarizationState.shouldCompress(context.messages.size)
    }
    
    private fun createDefaultContext(): AContext {
        return AContext(
            agentName = "",
            systemPrompt = "",
            messages = persistentListOf(),
            summarizationState = SummarizationState.disabled()
        )
    }
}
