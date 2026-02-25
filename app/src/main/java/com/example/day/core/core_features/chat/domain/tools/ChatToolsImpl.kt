package com.example.day.core.core_features.chat.domain.tools

import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.model.ChatMessageStatus
import com.example.day.core.core_features.chat.domain.model.UserType
import com.example.day.core.core_features.chat.domain.usecase.AddChatMessageUseCase
import com.example.day.core.core_features.chat.domain.usecase.CreateChatUseCase
import com.example.day.core.core_features.chat.domain.usecase.GetOrCreateChatUseCase
import javax.inject.Inject

/**
 * Implementation of [ChatTools] using Use Cases for chat management.
 * 
 * Follows Clean Architecture - uses Use Cases rather than direct repository access.
 */
internal class ChatToolsImpl @Inject constructor(
    private val createChatUseCase: CreateChatUseCase,
    private val getOrCreateChatUseCase: GetOrCreateChatUseCase,
    private val addChatMessageUseCase: AddChatMessageUseCase
) : ChatTools {

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

    override suspend fun addInfoMessage(chatId: Long, message: String) {
        addChatMessageUseCase.invoke(
            chatId,
            System.currentTimeMillis(),
            UserType.Info,
            message,
            ChatMessageStatus.Viewed
        )
    }
}
