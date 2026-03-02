package com.example.day.core.core_features.agent.domain.model

import com.example.day.core.core_features.agent.domain.strategy.CtxStrategyType
import com.example.day.core.core_features.llm.domain.model.ModelSettings

/**
 * Domain model representing an Agent settings in the system.
 * 
 * @property id Unique identifier of the agent
 * @property systemName System name used internally
 * @property title Display title of the agent
 * @property chatUserId Reference to UserEntity that represents this agent in chats (avatar)
 * @property isCommon If true (1), agent can be used in any chat without explicit binding
 */
data class AgentConfig(
    val id: Long = 0,
    val systemName: String,
    val title: String,
    val chatUserId: Long,
    val isCommon: Boolean,
    /** Настройки модели llm  (json строка из ModelSettingsEntity) */
    val modelSettings: ModelSettings,
    /** Системный промпт агента (по умолчанию пустой) */
    val systemPrompt: String,
    /** тип стратегии по работе с контекстом - enum class Full, Summarization, SlidingWindow */
    val contextStrategyType: CtxStrategyType,
)
