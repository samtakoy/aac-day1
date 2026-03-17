package com.example.day.core.core_features.memory.domain.provider.base

import com.example.day.core.core_features.memory.domain.provider.AgentRulesMemoryProvider
import com.example.day.core.core_features.memory.domain.provider.AgentSystemPromptMemoryProvider
import com.example.day.core.core_features.memory.domain.provider.AgentToolsMemoryProvider
import com.example.day.core.core_features.memory.domain.provider.AutoRagMemoryProvider
import com.example.day.core.core_features.memory.domain.provider.CompositeMemoryProvider
import com.example.day.core.core_features.memory.domain.provider.EmptyMemoryProvider
import com.example.day.core.core_features.memory.domain.provider.ToolCallHelperMemoryProvider
import com.example.day.core.core_features.memory.domain.provider.UserProfileMemoryProvider
import javax.inject.Inject

class MemoryProviderFactory @Inject constructor(
    private val userProfileMemoryProvider: UserProfileMemoryProvider,
    private val agentRulesMemoryProvider: AgentRulesMemoryProvider,
    private val toolCallHelperMemoryProvider: ToolCallHelperMemoryProvider,
    private val agentSystemPromptMemoryProvider: AgentSystemPromptMemoryProvider,
    private val agentToolsMemoryProvider: AgentToolsMemoryProvider,
    private val autoRagMemoryProvider: AutoRagMemoryProvider
) {
    fun create(
        memoryTypes: List<MemoryType>,
        agentId: Long? = null
    ): MemoryProvider {
        val providers = mutableListOf<MemoryProvider>()
        
        // Добавляем memory providers по типам
        if (memoryTypes.isNotEmpty()) {
            providers.addAll(
                memoryTypes.mapNotNull { createProviderByType(it, agentId) }
            )
        }
        
        // Всегда добавляем agent-specific providers если есть agentId
        agentId?.let {
            providers.add(agentSystemPromptMemoryProvider.apply { bindAgentId(it) })
            providers.add(agentToolsMemoryProvider.apply { bindAgentId(it) })
        }
        
        // Всегда добавляем tool call helper
        providers.add(toolCallHelperMemoryProvider)
        
        return CompositeMemoryProvider(providers)
    }

    private fun createProviderByType(type: MemoryType, agentId: Long?): MemoryProvider? {
        return when (type) {
            MemoryType.UserProfile -> userProfileMemoryProvider
            MemoryType.Chat -> null
            MemoryType.ChatGroup -> null
            MemoryType.AgentRules -> {
                agentId?.let {
                    agentRulesMemoryProvider.bindAgentId(it)
                    agentRulesMemoryProvider
                }
            }
            MemoryType.AutoRag -> {
                agentId?.let {
                    autoRagMemoryProvider.bindAgentId(it)
                    autoRagMemoryProvider
                }
            }
        }
    }
}