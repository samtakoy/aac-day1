package com.example.day.core.core_features.memory.domain.provider

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.repository.AgentMemoryRepository
import com.example.day.core.core_features.memory.domain.provider.base.MemoryProvider
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Memory provider that delivers agent's strict dialogue rules to the LLM context.
 * Rules are stored as a JSON array of strings in agent memory with:
 * - memoryKey = "settings"
 * - category = "rules"
 *
 * If rules exist, returns a system message with the format:
 * ```
 * Строгие правила диалога:
 * [правило 1]
 * [правило 2]
 * ...
 * ```
 */
class AgentRulesMemoryProvider @Inject constructor(
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

        // Извлекаем правила только из категории "rules"
        if (fact.category != CATEGORY) {
            return emptyList()
        }

        val rules = parseRules(fact.fact)
        if (rules.isEmpty()) {
            return emptyList()
        }

        val content = buildString {
            appendLine("Строго соблюдать правила диалога:")
            rules.forEach { rule ->
                appendLine(rule)
            }
        }

        return listOf(AContextMessage(role = AContextMessage.Role.SYSTEM, content = content.trim()))
    }

    /**
     * Parse JSON array of rules from stored fact.
     */
    private fun parseRules(json: String): List<String> {
        return try {
            Json.decodeFromString(ListSerializer(String.serializer()), json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        const val MEMORY_KEY = "settings"
        const val CATEGORY = "rules"
    }
}
