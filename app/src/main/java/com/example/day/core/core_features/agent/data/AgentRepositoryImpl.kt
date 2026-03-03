package com.example.day.core.core_features.agent.data

import androidx.room.Transaction
import com.example.day.core.core_features.agent.data.local.dao.AgentDao
import com.example.day.core.core_features.agent.data.local.dao.AgentMemoryDao
import com.example.day.core.core_features.agent.data.local.dao.AgentToChatDao
import com.example.day.core.core_features.agent.data.local.mapper.AgentMapper
import com.example.day.core.core_features.agent.data.local.model.AgentEntity
import com.example.day.core.core_features.agent.data.local.model.AgentToChatEntity
import com.example.day.core.core_features.agent.data.local.model.AgentToMemoryTypeEntity
import com.example.day.core.core_features.agent.data.local.model.StrategyTypeEntity
import com.example.day.core.core_features.agent.domain.AgentContextRepository
import com.example.day.core.core_features.agent.domain.AgentRepository
import com.example.day.core.core_features.agent.domain.model.AContext
import com.example.day.core.core_features.agent.domain.model.AContextParams
import com.example.day.core.core_features.agent.domain.model.AContextState
import com.example.day.core.core_features.agent.domain.model.AgentConfig
import com.example.day.core.core_features.chat.domain.ChatRepository
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.model.ChatSettings
import com.example.day.core.core_features.llm.data.local.mapper.ModelSettingsMapper
import com.example.day.core.core_features.memory.domain.provider.base.MemoryType
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of AgentRepository for managing agents, their chat bindings, and context.
 */
internal class AgentRepositoryImpl @Inject constructor(
    private val agentDao: AgentDao,
    private val agentToChatDao: AgentToChatDao,
    private val agentMemoryDao: AgentMemoryDao,
    private val agentContextRepository: AgentContextRepository,
    // TODO use chat dao
    private val chatRepository: ChatRepository,
    private val agentMapper: AgentMapper,
    private val modelSettingsMapper: ModelSettingsMapper,
) : AgentRepository {
    
    // ==================== Agent CRUD ====================
    
    override suspend fun createAgent(
        systemName: String,
        title: String,
        chatUserId: Long,
        isCommon: Boolean,
        chatSettings: ChatSettings
    ): Long {
        // Convert ChatSettings to entity values
        val modelSettingsJson = modelSettingsMapper.toJson(chatSettings.model)
        val systemPrompt = chatSettings.systemPromt
        
        val entity = AgentEntity(
            systemName = systemName,
            title = title,
            chatUserId = chatUserId,
            isCommon = if (isCommon) AgentMapper.IS_COMMON_TRUE else AgentMapper.IS_COMMON_FALSE,
            modelSettings = modelSettingsJson,
            systemPrompt = systemPrompt,
            contextStrategyType = StrategyTypeEntity.FULL_CONTEXT
        )
        val agentId = agentDao.insert(entity)
        
        // Создаем пустую запись контекста для агента
        createEmptyAgentContext(agentId)
        
        return agentId
    }
    
    override suspend fun createAgent(
        systemName: String,
        title: String,
        chatUserId: Long,
        isCommon: Boolean
    ): Long {
        // Legacy method - uses default values (empty modelSettings "{}")
        val entity = AgentEntity(
            systemName = systemName,
            title = title,
            chatUserId = chatUserId,
            isCommon = if (isCommon) AgentMapper.IS_COMMON_TRUE else AgentMapper.IS_COMMON_FALSE
        )
        val agentId = agentDao.insert(entity)
        
        // Создаем пустую запись контекста для агента
        createEmptyAgentContext(agentId)
        
        return agentId
    }
    
    private suspend fun createEmptyAgentContext(agentId: Long) {
        // Create proper context for FULL_CONTEXT strategy
        val context = AContext(
            params = AContextParams.Full,
            data = AContextState.Full(messages = persistentListOf())
        )
        agentContextRepository.saveContext(agentId, context)
    }
    
    override suspend fun updateAgent(agentConfig: AgentConfig) {
        agentDao.update(agentMapper.toEntity(agentConfig))
    }
    
    override suspend fun deleteAgent(agentId: Long) {
        agentDao.deleteById(agentId)
    }
    
    override suspend fun getAgentById(agentId: Long): AgentConfig? {
        return agentDao.getByIdWithMemories(agentId)?.let(agentMapper::toDomain)
    }
    
    override fun getAgentByIdAsFlow(agentId: Long): Flow<AgentConfig?> {
        return agentDao.getByIdAsFlow(agentId).map { it?.let(agentMapper::toDomain) }
    }
    
    override fun getAllAgents(): Flow<List<AgentConfig>> {
        return agentDao.getAll().map { entities ->
            entities.map(agentMapper::toDomain)
        }
    }
    
    override fun getCommonAgents(): Flow<List<AgentConfig>> {
        return agentDao.getCommonAgents().map { entities ->
            entities.map(agentMapper::toDomain)
        }
    }
    
    override suspend fun getAgentByChatUserId(chatUserId: Long): AgentConfig? {
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
    override fun getAgentsForChat(chatId: Long): Flow<List<AgentConfig>> {
        return agentToChatDao.getAgentsForChat(chatId).map { agentIds ->
            agentIds.mapNotNull { agentId ->
                agentDao.getByIdWithMemories(agentId)?.let(agentMapper::toDomain)
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
    
    // ==================== Agent Factory ====================
    
    override suspend fun getOrCreateAgent(
        systemName: String,
        isCommon: Boolean,
        chatSettings: ChatSettings
    ): AgentConfig {
        return if (isCommon) {
            // ЛОГИКА 1: Общие агенты (isCommon = true)
            getOrCreateCommonAgent(systemName)
        } else {
            // ЛОГИКА 2: Чат-специфичные агенты (isCommon = false)
            getOrCreateChatSpecificAgent(systemName, chatSettings.chatId, chatSettings)
        }
    }
    
    private suspend fun getOrCreateCommonAgent(systemName: String): AgentConfig {
        // 1. Искать только по systemName (isCommon = 1)
        val existingAgent = agentDao.getCommonAgentBySystemName(systemName)
        if (existingAgent != null) {
            return agentMapper.toDomain(existingAgent)
        }
        
        // 2. Создать нового общего агента (без ChatSettings)
        val botUser = chatRepository.getOrCreateDefaultUsers().second
        val newAgentId = createAgent(
            systemName = systemName,
            title = systemName,
            chatUserId = botUser.id,
            isCommon = true
        )
        
        return getAgentById(newAgentId) 
            ?: throw IllegalStateException("Failed to create common agent")
    }
    
    private suspend fun getOrCreateChatSpecificAgent(
        systemName: String, 
        chatId: Long,
        chatSettings: ChatSettings?
    ): AgentConfig {
        // 1. Искать агента по systemName + chatId
        val existingAgent = agentToChatDao.getAgentBySystemNameAndChatId(systemName, chatId)
        if (existingAgent != null) {
            return agentMapper.toDomain(existingAgent)
        }
        
        // 2. Создать нового чат-специфичного агента
        val botUser = chatRepository.getOrCreateDefaultUsers().second
        
        val newAgentId = if (chatSettings != null) {
            // Используем ChatSettings если передан
            createAgent(
                systemName = systemName,
                title = systemName,
                chatUserId = botUser.id,
                isCommon = false,
                chatSettings = chatSettings
            )
        } else {
            // TODO такого выбора не будет (и common агент - сейчас буддет работать некорректно)
            // Получаем ChatSettings из чата
            val settings = chatRepository.getChatSettings(chatId)
            createAgent(
                systemName = systemName,
                title = systemName,
                chatUserId = botUser.id,
                isCommon = false,
                chatSettings = settings ?: throw IllegalStateException("Chat settings not found for chat $chatId")
            )
        }
        
        // 3. Обязательно привязать к чату
        bindAgentToChat(newAgentId, chatId)
        
        return getAgentById(newAgentId) 
            ?: throw IllegalStateException("Failed to create chat-specific agent")
    }

    // ===== Agent Memory =====

    override suspend fun addMemoryType(agentId: Long, type: MemoryType) {
        val entity = AgentToMemoryTypeEntity(
            agentId = agentId,
            memoryType = type.dbName
        )
        agentMemoryDao.addMemoryTypeToAgent(entity)
    }

    override suspend fun removeMemoryType(agentId: Long, type: MemoryType) {
        agentMemoryDao.removeMemoryTypeFromAgent(agentId, type.dbName)
    }

    // Атомарное обновление всех типов (например, из экрана настроек)
    @Transaction
    override suspend fun updateAgentMemories(agentId: Long, types: List<MemoryType>) {
        agentMemoryDao.clearAgentMemories(agentId)
        types.forEach { type ->
            addMemoryType(agentId, type)
        }
    }
}
