package com.example.day.core.core_features.memory.domain.provider.base

import com.example.day.core.core_features.memory.domain.provider.AgentRulesMemoryProvider
import com.example.day.core.core_features.memory.domain.provider.CompositeMemoryProvider
import com.example.day.core.core_features.memory.domain.provider.EmptyMemoryProvider
import com.example.day.core.core_features.memory.domain.provider.ToolCallHelperMemoryProvider
import com.example.day.core.core_features.memory.domain.provider.UserProfileMemoryProvider
import javax.inject.Inject

class MemoryProviderFactory @Inject constructor(
    private val userProfileMemoryProvider: UserProfileMemoryProvider,
    private val agentRulesMemoryProvider: AgentRulesMemoryProvider,
    private val toolCallHelperMemoryProvider: ToolCallHelperMemoryProvider
) {
    fun create(
        memoryTypes: List<MemoryType>,
        agentId: Long? = null
    ): MemoryProvider {
        return if (memoryTypes.isNotEmpty()) {
            return CompositeMemoryProvider(
                memoryTypes.mapNotNull { createProviderByType(it, agentId) }
                    + toolCallHelperMemoryProvider
            )
        } else {
            toolCallHelperMemoryProvider
        }
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
        }
    }
}