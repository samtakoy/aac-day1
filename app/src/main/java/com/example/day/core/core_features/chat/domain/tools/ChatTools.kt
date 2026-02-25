package com.example.day.core.core_features.chat.domain.tools

import com.example.day.core.core_features.chat.domain.model.Chat

/**
 * Interface for chat operations that workers and other components use.
 * Provides methods for chat and message management.
 * 
 * Part of the chat domain layer - uses Use Cases for Clean Architecture.
 */
interface ChatTools {
    
    /**
     * Create a new chat.
     * 
     * @param chatTitle the title of the chat
     * @param groupId the chat group identifier
     * @return the ID of the created chat
     */
    suspend fun createChat(chatTitle: String, groupId: Long): Long
    
    /**
     * Get or create a chat.
     * 
     * @param chatTitle the title of the chat
     * @param groupId the chat group identifier
     * @return Chat instance
     */
    suspend fun getOrCreateChat(chatTitle: String, groupId: Long): Chat
    
    /**
     * Add a bot message to the chat.
     * 
     * @param chatId the chat identifier
     * @param message the message content
     */
    suspend fun addBotMessage(chatId: Long, message: String)
    
    /**
     * Add an info message to the chat.
     * 
     * @param chatId the chat identifier
     * @param message the message content
     */
    suspend fun addInfoMessage(chatId: Long, message: String)
}
