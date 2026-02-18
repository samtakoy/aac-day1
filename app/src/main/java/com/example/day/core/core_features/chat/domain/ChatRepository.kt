package com.example.day.core.core_features.chat.domain

import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.model.ChatMessage
import com.example.day.core.core_features.chat.domain.model.ChatMessageStatus
import com.example.day.core.core_features.chat.domain.model.User
import com.example.day.core.core_features.chat.domain.model.UserType
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun createChat(title: String): Long
    fun getChatListAsFlow(): Flow<List<Chat>>
    suspend fun getChatById(chatId: Long): Chat?
    suspend fun addMessage(
        chatId: Long,
        timestamp: Long,
        userType: UserType,
        text: String,
        status: ChatMessageStatus
    ): Long
    suspend fun removeMessage(messageId: Long)
    suspend fun changeMessageStatus(messageId: Long, status: ChatMessageStatus)
    fun getChatMessagesAsFlow(chatId: Long): Flow<List<ChatMessage>>
    suspend fun getChatMessages(chatId: Long, status: ChatMessageStatus): List<ChatMessage>
    suspend fun getOrCreateDefaultUsers(): Pair<User, User>
    suspend fun clearChat(chatId: Long)
    suspend fun clearChatNotViewedMessages(chatId: Long)
    suspend fun dropChat(chatId: Long)
}
