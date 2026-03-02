package com.example.day.core.core_features.agent.domain.workers.tools

import com.example.day.core.core_features.agent.domain.model.AContext
import com.example.day.core.core_features.agent.domain.model.AgentConfig
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelSettings

/**
 * Interface for agent operations that workers use.
 * Provides methods for agent lifecycle management and context handling.
 * 
 * Part of the agent domain layer - uses Use Cases for Clean Architecture.
 */
interface AgentTools {
    
    /**
     * Get or create an agent for use.
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
    ): AgentConfig
    
    /**
     * Get context for an agent.
     * @param agentId the agent ID
     * @return AContext if found
     */
    suspend fun getContext(agentId: Long): AContext
    
    /**
     * Save context for an agent.
     * @param agentId the agent ID
     * @param context the context to save
     */
    suspend fun saveContext(agentId: Long, context: AContext)

    /**
     * Clear agent context from database.
     * @param agentId the agent ID
     */
    suspend fun clearAgentContext(agentId: Long)
    
    // ==================== Context Compression Methods ====================
    
    /**
     * Сохранить параметры контекста.
     * @param agentId ID агента
     * @param msgLimit сколько пар хранить как есть
     * @param extraLimit сколько пар накопить для сжатия
     * @param strategy стратегия сжатия
     */
    suspend fun saveContextParameters(
        agentId: Long,
        msgLimit: Int,
        extraLimit: Int,
        strategy: String
    )
    
    /**
     * Получить полный контекст для LLM запроса.
     * Включает summary (если есть) + последние asis пар сообщений.
     * 
     * @param agentId ID агента
     * @return список сообщений для LLM (с summary в начале)
     */
    suspend fun getFullContext(context: AContext): List<ModelRequest.Message>
    
    /**
     * Обработать оптимизацию контекста.
     * Проверяет лимиты и при необходимости выполняет сжатие.
     * 
     * @param agentId ID агента
     * @param modelSettings настройки модели для LLM
     * @return результат оптимизации с информацией о сжатии
     */
    suspend fun processContextOptimization(
        context: AContext,
        modelSettings: ModelSettings
    ): AContext
    
    /**
     * Проверить, нужно ли сжатие контекста.
     * Используется для уведомления пользователя до начала сжатия.
     * 
     * @param agentId ID агента
     * @return true если сжатие требуется
     */
    suspend fun shouldCompress(agentId: Long): Boolean
}
