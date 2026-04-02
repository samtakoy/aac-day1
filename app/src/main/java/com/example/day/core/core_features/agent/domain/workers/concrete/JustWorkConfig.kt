package com.example.day.core.core_features.agent.domain.workers.concrete

import com.example.day.core.core_features.agent.domain.model.AContext
import com.example.day.core.core_features.llm.domain.model.ModelSettings
import com.example.day.core.core_features.memory.domain.provider.base.MemoryType

/**
 * Configuration for creating an agent via JustWorkWorker.
 * Contains all settings needed to initialize an agent with specific behavior.
 *
 * @param agentName System name for agent identification
 * @param chatId Chat ID for notifications and context binding
 * @param systemPrompt System prompt to configure agent behavior
 * @param allowedTools List of allowed MCP tool names (empty = all tools allowed)
 * @param defaultModel Factory for default model settings
 * @param defaultContext Factory for default context
 * @param memoryTypes Явный список MemoryType для агента. null — не менять то, что есть в БД.
 *                    Пустой список — убрать все опциональные типы (нет ToolCallHelper и т.д.)
 */
data class JustWorkConfig(
    val agentName: String,
    val chatId: Long,
    val systemPrompt: String,
    val allowedTools: List<String>,
    val defaultModel: () -> ModelSettings,
    val defaultContext: () -> AContext,
    val recreateAgent: Boolean,
    val memoryTypes: List<MemoryType>? = null,
)
