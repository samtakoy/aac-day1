package com.example.day.features.console.impl.domain.agents

import com.example.day.core.core_features.agent.domain.model.AContext
import com.example.day.core.core_features.agent.domain.model.AContextOwner
import com.example.day.core.core_features.agent.domain.model.Agent
import com.example.day.core.core_features.chat.domain.model.Chat

/**
 * Interface for agent tools to interact with the external world.
 * Extends [AContextOwner] for agent context management.
 */
interface WorkerTools : AContextOwner {
    
    /**
     * Get or create agent for use.
     * 
     * @param systemName system name of the agent
     * @param chatId current chat identifier
     * @param isCommonAgent if true, agent is not bound to specific chat
     * @return Agent instance
     */
    suspend fun getOrCreateAgent(
        systemName: String,
        chatId: Long,
        isCommonAgent: Boolean
    ): Agent
    
    suspend fun createChat(chatTitle: String, groupId: Long): Long
    suspend fun getOrCreateChat(chatTitle: String, groupId: Long): Chat
    suspend fun addBotMessage(chatId: Long, message: String)
}
