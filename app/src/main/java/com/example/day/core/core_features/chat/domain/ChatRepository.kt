package com.example.day.core.core_features.chat.domain

import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.model.ChatGroup
import com.example.day.core.core_features.chat.domain.model.ChatMessage
import com.example.day.core.core_features.chat.domain.model.ChatMessageStatus
import com.example.day.core.core_features.chat.domain.model.ChatSettings
import com.example.day.core.core_features.chat.domain.model.ChatType
import com.example.day.core.core_features.chat.domain.model.User
import com.example.day.core.core_features.chat.domain.model.UserType
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    // Existing chat methods
    suspend fun createChat(title: String, chatGroupId: Long): Long
    suspend fun getChatById(chatId: Long): Chat?
    fun getChatByIdAsFlow(chatId: Long): Flow<Chat?>
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
    
    // New methods for groups
    fun getAllChatGroups(): Flow<List<ChatGroup>>
    suspend fun getChatGroupWithMaxId(): ChatGroup?
    suspend fun createChatGroup(title: String, chatType: ChatType, colorIndex: Int): Long
    suspend fun updateChatGroup(group: ChatGroup)
    suspend fun deleteChatGroup(groupId: Long)
    
    // New methods for types
    suspend fun getAllChatTypes(): List<ChatType>
    suspend fun ensureChatTypesExist()
    
    // New methods for chats by group
    fun getChatsByGroup(groupId: Long): Flow<List<Chat>>
    suspend fun getChatsCountInGroup(groupId: Long): Int
    
    // ChatSettings methods
    suspend fun getChatSettings(chatId: Long): ChatSettings?
    suspend fun updateChatSettings(settings: ChatSettings)

    // Get or create chat by title and group
    suspend fun getOrCreateChat(title: String, chatGroupId: Long): Chat
}
