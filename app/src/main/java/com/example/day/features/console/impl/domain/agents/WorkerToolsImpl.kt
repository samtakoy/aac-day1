package com.example.day.features.console.impl.domain.agents

import com.example.day.core.core_features.agent.domain.model.AContext
import com.example.day.core.core_features.agent.domain.model.AContextOwner
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.model.ChatMessageStatus
import com.example.day.core.core_features.chat.domain.model.UserType
import com.example.day.core.core_features.chat.domain.usecase.AddChatMessageUseCase
import com.example.day.core.core_features.chat.domain.usecase.CreateChatUseCase
import com.example.day.core.core_features.chat.domain.usecase.GetOrCreateChatUseCase
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Реализация [WorkerTools] с использованием [AContextOwner] для управления контекстом агента.
 */
internal class WorkerToolsImpl @Inject constructor(
    private val createChatUseCase: CreateChatUseCase,
    private val getOrCreateChatUseCase: GetOrCreateChatUseCase,
    private val addChatMessageUseCase: AddChatMessageUseCase,
    private val contextOwner: AContextOwner
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

    override fun getContext(agentName: String): AContext {
        return contextOwner.getContext(agentName)
    }

    override fun saveContext(context: AContext) {
        contextOwner.saveContext(context)
    }
}
