package com.example.day.core.core_features.agent.data

import com.example.day.core.core_features.agent.data.local.dao.AgentDao
import com.example.day.core.core_features.agent.data.local.dao.AgentContextMemoryDao
import com.example.day.core.core_features.agent.data.local.dao.AgentToChatDao
import com.example.day.core.core_features.agent.data.local.mapper.AgentContextMapper
import com.example.day.core.core_features.agent.data.local.mapper.AgentMapper
import com.example.day.core.core_features.agent.data.local.model.AgentEntity
import com.example.day.core.core_features.agent.data.local.model.AgentToChatEntity
import com.example.day.core.core_features.agent.domain.AgentRepository
import com.example.day.core.core_features.agent.domain.model.Agent
import com.example.day.core.core_features.agent.domain.model.AContext
import com.example.day.core.core_features.chat.domain.ChatRepository
import com.example.day.core.core_features.chat.domain.model.Chat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of AgentRepository for managing agents, their chat bindings, and context.
 */
internal class AgentRepositoryImpl @Inject constructor(
    private val agentDao: AgentDao,
    private val agentToChatDao: AgentToChatDao,
    private val agentContextMemoryDao: AgentContextMemoryDao,
    // TODO use chat dao
    private val chatRepository: ChatRepository,
    private val agentMapper: AgentMapper,
    private val agentContextMapper: AgentContextMapper
) : AgentRepository {
    
    // ==================== Agent CRUD ====================
    
    override suspend fun createAgent(
        systemName: String,
        title: String,
        chatUserId: Long,
        isCommon: Boolean
    ): Long {
        val entity = AgentEntity(
            systemName = systemName,
            title = title,
            chatUserId = chatUserId,
            isCommon = if (isCommon) AgentMapper.IS_COMMON_TRUE else AgentMapper.IS_COMMON_FALSE
        )
        return agentDao.insert(entity)
    }
    
    override suspend fun updateAgent(agent: Agent) {
        agentDao.update(agentMapper.toEntity(agent))
    }
    
    override suspend fun deleteAgent(agentId: Long) {
        agentDao.deleteById(agentId)
    }
    
    override suspend fun getAgentById(agentId: Long): Agent? {
        return agentDao.getById(agentId)?.let(agentMapper::toDomain)
    }
    
    override fun getAgentByIdAsFlow(agentId: Long): Flow<Agent?> {
        return agentDao.getByIdAsFlow(agentId).map { it?.let(agentMapper::toDomain) }
    }
    
    override fun getAllAgents(): Flow<List<Agent>> {
        return agentDao.getAll().map { entities ->
            entities.map(agentMapper::toDomain)
        }
    }
    
    override fun getCommonAgents(): Flow<List<Agent>> {
        return agentDao.getCommonAgents().map { entities ->
            entities.map(agentMapper::toDomain)
        }
    }
    
    override suspend fun getAgentByChatUserId(chatUserId: Long): Agent? {
        return agentDao.getByChatUserId(chatUserId)?.let(agentMapper::toDomain)
    }
    
    // ==================== Agent-Chat Binding ====================
    
    // TODO(code-advice): Using ChatRepository here adds overhead. Consider adding a batch fetch method to ChatDao
    // and using it directly for better performance instead of individual getChatById calls.
    override suspend fun bindAgentToChat(agentId: Long, chatId: Long) {
        val binding = AgentToChatEntity(agentId = agentId, chatId = chatId)
        agentToChatDao.insert(binding)
    }
    
    override suspend fun unbindAgentFromChat(agentId: Long, chatId: Long) {
        agentToChatDao.deleteByAgentAndChat(agentId, chatId)
    }
    
    // NOTE(code-advice): This uses map { } with suspend functions inside Flow, which can block.
    // Consider using flatMapConcat or adding a batch DAO method for production use.
    override fun getChatsForAgent(agentId: Long): Flow<List<Chat>> {
        return agentToChatDao.getChatsForAgent(agentId).map { chatIds ->
            chatIds.mapNotNull { chatId ->
                chatRepository.getChatById(chatId)
            }
        }
    }
    
    // NOTE(code-advice): This uses map { } with suspend functions inside Flow, which can block.
    // Consider using flatMapConcat or adding a batch DAO method for production use.
    override fun getAgentsForChat(chatId: Long): Flow<List<Agent>> {
        return agentToChatDao.getAgentsForChat(chatId).map { agentIds ->
            agentIds.mapNotNull { agentId ->
                agentDao.getById(agentId)?.let(agentMapper::toDomain)
            }
        }
    }
    
    override suspend fun isAgentBoundToChat(agentId: Long, chatId: Long): Boolean {
        return agentToChatDao.isAgentBoundToChat(agentId, chatId)
    }
    
    override suspend fun canAgentBeUsedInChat(agentId: Long, chatId: Long): Boolean {
        val agent = agentDao.getById(agentId) ?: return false
        
        // If agent is common, it can be used in any chat
        if (agent.isCommon == AgentMapper.IS_COMMON_TRUE) {
            return true
        }
        
        // Otherwise, check if agent is bound to this chat
        return agentToChatDao.isAgentBoundToChat(agentId, chatId)
    }
    
    // ==================== Agent Context ====================
    
    override suspend fun saveAgentContext(agentId: Long, context: AContext) {
        val entity = agentContextMapper.toEntity(agentId, context)
        agentContextMemoryDao.insertOrUpdate(entity)
    }
    
    override suspend fun getAgentContext(agentId: Long): AContext? {
        return agentContextMemoryDao.getContext(agentId)?.let(agentContextMapper::toDomain)
    }
    
    override suspend fun clearAgentContext(agentId: Long) {
        agentContextMemoryDao.delete(agentId)
    }
}
