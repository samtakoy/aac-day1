package com.example.day.features.console.impl.domain.agents

import com.example.day.core.core_features.agent.domain.AgentRepository
import com.example.day.core.core_features.agent.domain.model.AContext
import com.example.day.core.core_features.agent.domain.model.Agent
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.model.ChatMessageStatus
import com.example.day.core.core_features.chat.domain.model.UserType
import com.example.day.core.core_features.chat.domain.usecase.AddChatMessageUseCase
import com.example.day.core.core_features.chat.domain.usecase.CreateChatUseCase
import com.example.day.core.core_features.chat.domain.usecase.GetOrCreateChatUseCase
import kotlinx.collections.immutable.persistentListOf
import javax.inject.Inject

/**
 * Implementation of [WorkerTools] using [AgentRepository] for agent management
 * and persistent context storage in the database.
 */
internal class WorkerToolsImpl @Inject constructor(
    private val createChatUseCase: CreateChatUseCase,
    private val getOrCreateChatUseCase: GetOrCreateChatUseCase,
    private val addChatMessageUseCase: AddChatMessageUseCase,
    private val agentRepository: AgentRepository
) : WorkerTools {

    override suspend fun createChat(chatTitle: String, groupId: Long): Long {
        return createChatUseCase.invoke(chatTitle, groupId)
    }

    override suspend fun getOrCreateChat(chatTitle: String, groupId: Long): Chat {
        return getOrCreateChatUseCase.invoke(chatTitle, groupId)
    }

    override suspend fun addBotMessage(chatId: Long, message: String) {
        addChatMessageUseCase.invoke(
            chatId,
            System.currentTimeMillis(),
            UserType.Bot,
            message,
            ChatMessageStatus.Viewed
        )
    }

    override suspend fun getOrCreateAgent(
        systemName: String,
        chatId: Long,
        isCommonAgent: Boolean
    ): Agent {
        return agentRepository.getOrCreateAgent(
            systemName = systemName,
            isCommon = isCommonAgent,
            chatId = chatId
        )
    }

    override suspend fun getContext(agentId: Long): AContext {
        return agentRepository.getAgentContext(agentId) ?: AContext(
            agentName = "",
            systemPrompt = "",
            messages = persistentListOf()
        )
    }

    override suspend fun saveContext(agentId: Long, context: AContext) {
        agentRepository.saveAgentContext(agentId, context)
    }
}
