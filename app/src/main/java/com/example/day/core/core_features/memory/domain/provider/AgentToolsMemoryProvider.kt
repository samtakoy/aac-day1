package com.example.day.core.core_features.memory.domain.provider

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.repository.AgentMemoryRepository
import com.example.day.core.core_features.memory.domain.provider.base.MemoryProvider
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Memory provider that delivers agent's allowed MCP tools to the LLM context.
 * Tools are stored as a JSON array of strings in agent memory with:
 * - memoryKey = "settings"
 * - category = "tools"
 *
 * If tools exist, returns a system message with the format:
 * ```
 * Доступные инструменты:
 * - tool_name_1
 * - tool_name_2
 * ...
 * ```
 *
 * If no tools are configured, returns empty list (all tools are allowed).
 *
     * TODO удалить класс - непонятное назначение
     */
    class AgentToolsMemoryProvider @Inject constructor(
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

        // Извлекаем tools только из категории "tools"
        if (fact.category != CATEGORY) {
            return emptyList()
        }

        val tools = parseTools(fact.fact)
        if (tools.isEmpty()) {
            return emptyList()
        }

        val content = buildString {
            appendLine("Доступные инструменты:")
            tools.forEach { tool ->
                appendLine("- $tool")
            }
        }

        return listOf(AContextMessage(role = AContextMessage.Role.SYSTEM, content = content.trim()))
    }

    /**
     * Parse JSON array of tools from stored fact.
     */
    private fun parseTools(json: String): List<String> {
        return try {
            Json.decodeFromString(ListSerializer(String.serializer()), json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        const val MEMORY_KEY = "settings"
        const val CATEGORY = "tools"
    }
}
