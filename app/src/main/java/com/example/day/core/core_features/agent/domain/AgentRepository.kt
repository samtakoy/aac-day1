package com.example.day.core.core_features.agent.domain

import com.example.day.core.core_features.agent.domain.model.Agent
import com.example.day.core.core_features.agent.domain.model.AContext
import com.example.day.core.core_features.chat.domain.model.Chat
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Agent operations.
 * Defines the contract for managing agents, their chat bindings, and context.
 */
interface AgentRepository {
    
    // ==================== Agent CRUD ====================
    
    /**
     * Create a new agent
     * @return id of created agent
     */
    suspend fun createAgent(
        systemName: String,
        title: String,
        chatUserId: Long,
        isCommon: Boolean
    ): Long
    
    /**
     * Update an existing agent
     */
    suspend fun updateAgent(agent: Agent)
    
    /**
     * Delete an agent by id
     */
    suspend fun deleteAgent(agentId: Long)
    
    /**
     * Get agent by id
     */
    suspend fun getAgentById(agentId: Long): Agent?
    
    /**
     * Get agent by id as Flow
     */
    fun getAgentByIdAsFlow(agentId: Long): Flow<Agent?>
    
    /**
     * Get all agents
     */
    fun getAllAgents(): Flow<List<Agent>>
    
    /**
     * Get all common agents (isCommon = true)
     */
    fun getCommonAgents(): Flow<List<Agent>>
    
    /**
     * Get agent by chatUserId (the UserEntity that represents this agent)
     */
    suspend fun getAgentByChatUserId(chatUserId: Long): Agent?
    
    // ==================== Agent-Chat Binding ====================
    
    /**
     * Bind agent to a specific chat
     */
    suspend fun bindAgentToChat(agentId: Long, chatId: Long)
    
    /**
     * Unbind agent from a specific chat
     */
    suspend fun unbindAgentFromChat(agentId: Long, chatId: Long)
    
    /**
     * Get all chats that an agent is bound to
     */
    fun getChatsForAgent(agentId: Long): Flow<List<Chat>>
    
    /**
     * Get all agents bound to a specific chat
     */
    fun getAgentsForChat(chatId: Long): Flow<List<Agent>>
    
    /**
     * Check if agent is bound to a specific chat
     */
    suspend fun isAgentBoundToChat(agentId: Long, chatId: Long): Boolean
    
    /**
     * Check if agent can be used in a specific chat
     * (returns true if isCommon = true OR agent is bound to this chat)
     */
    suspend fun canAgentBeUsedInChat(agentId: Long, chatId: Long): Boolean
    
    // ==================== Agent Factory ====================
    
    /**
     * Get or create agent by systemName and isCommon flag.
     * 
     * New Logic (as of implementation):
     * 
     * If isCommon = true:
     *   1. Find agent by systemName only (common agents)
     *   2. If not found - create new common agent
     * 
     * If isCommon = false:
     *   1. Find agent by systemName + chatId (chat-specific)
     *   2. If not found - create new agent and bind to chatId
     * 
     * @param systemName system name of the agent
     * @param isCommon if true, agent can be used in any chat without binding
     * @param chatId chat id to bind agent to (if isCommon = false)
     * @return existing or newly created Agent
     */
    suspend fun getOrCreateAgent(
        systemName: String,
        isCommon: Boolean,
        chatId: Long
    ): Agent
    
    // ==================== Agent Context ====================
    
    /**
     * Save agent context (conversation history)
     */
    suspend fun saveAgentContext(agentId: Long, context: AContext)
    
    /**
     * Get agent context
     */
    suspend fun getAgentContext(agentId: Long): AContext?
    
    /**
     * Clear agent context
     */
    suspend fun clearAgentContext(agentId: Long)
}
