package com.example.day.core.core_features.memory.domain.provider

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.repository.AgentMemoryRepository
import com.example.day.core.core_features.memory.domain.provider.base.MemoryProvider
import com.example.day.core.core_features.memory.domain.provider.rag.RagSearchRepository
import javax.inject.Inject

/**
 * Memory provider that enriches the user's prompt with RAG-server search results.
 *
 * Activated when [com.example.day.core.core_features.memory.domain.provider.base.MemoryType.AutoRag]
 * is added to the agent's memory types via `@@talk(rag --on)`.
 *
 * Server URL is stored in AgentMemoryRepository (key=[MEMORY_KEY], category=[CATEGORY_URL]).
 * Falls back to [DEFAULT_URL] if not configured.
 *
 * On error (server unavailable, index not ready) silently returns the original prompt unchanged.
 *
 * Appended format:
 * ```
 * <original prompt>
 *
 * Контекстная информация по запросу:
 * <rag results>
 * ```
 */
class AutoRagMemoryProvider @Inject constructor(
    private val agentMemoryRepository: AgentMemoryRepository,
    private val ragSearchRepository: RagSearchRepository
) : MemoryProvider {

    private var agentId: Long? = null

    fun bindAgentId(agentId: Long) {
        this.agentId = agentId
    }

    override suspend fun getMemoryContext(): List<AContextMessage> = emptyList()

    override suspend fun appendUserPrompt(prompt: AContextMessage): AContextMessage {
        val agentId = agentId ?: return prompt

        val serverUrl = agentMemoryRepository
            .getFact(agentId, MEMORY_KEY, CATEGORY_URL)?.fact
            ?: DEFAULT_URL

        val ragResult = ragSearchRepository.search(prompt.content, serverUrl)
            .getOrElse { return prompt }

        if (ragResult.isBlank()) return prompt

        val enrichedContent = buildString {
            append(prompt.content)
            append("\n\nКонтекстная информация по запросу:\n")
            append(ragResult)
        }
        return prompt.copy(content = enrichedContent)
    }

    companion object {
        const val MEMORY_KEY = "settings"
        const val CATEGORY_URL = "rag_server_url"
        const val DEFAULT_URL = "http://10.0.2.2:3001"
    }
}
