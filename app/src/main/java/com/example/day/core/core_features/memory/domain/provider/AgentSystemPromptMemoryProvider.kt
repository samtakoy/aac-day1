package com.example.day.core.core_features.memory.domain.provider

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.repository.AgentMemoryRepository
import com.example.day.core.core_features.memory.domain.provider.base.MemoryProvider
import javax.inject.Inject

/**
 * Memory provider that delivers agent's system prompt to the LLM context.
 * System prompt is stored as plain text in agent memory with:
 * - memoryKey = "settings"
 * - category = "systemPrompt"
 *
 * If system prompt exists, returns it as a SYSTEM message.
 * Otherwise returns empty list.
 */
class AgentSystemPromptMemoryProvider @Inject constructor(
    private val agentMemoryRepository: AgentMemoryRepository
) : MemoryProvider {

    private var agentId: Long? = null

    /**
     * Bind agent ID to this provider.
     * Must be called before getMemoryContext().
     */
    fun bindAgentId(agentId: Long) {
        this.agentId = agentId
    }

    override suspend fun getMemoryContext(): List<AContextMessage> {
        val agentId = agentId ?: return emptyList()

        val fact = agentMemoryRepository.getFact(agentId, MEMORY_KEY, CATEGORY)
            ?: return emptyList()

        // Извлекаем системный промпт только из категории "systemPrompt"
        if (fact.category != CATEGORY) {
            return emptyList()
        }

        val systemPrompt = fact.fact.trim()
        if (systemPrompt.isBlank()) {
            return emptyList()
        }

        return listOf(AContextMessage(role = AContextMessage.Role.SYSTEM, content = systemPrompt))
    }

    companion object {
        const val MEMORY_KEY = "settings"
        const val CATEGORY = "systemPrompt"
    }
}
