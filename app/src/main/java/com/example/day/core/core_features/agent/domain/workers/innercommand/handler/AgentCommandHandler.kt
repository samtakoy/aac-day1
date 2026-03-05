package com.example.day.core.core_features.agent.domain.workers.innercommand.handler

import com.example.day.core.core_features.agent.domain.AIAgentFactory
import com.example.day.core.core_features.agent.domain.repository.AgentMemoryRepository
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.memory.domain.provider.AgentRulesMemoryProvider
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Handles all @@talk(agent ...) commands.
 *
 * Supported sub-commands:
 *   --addrule "текст"     добавить новое правило диалога
 *   --listrules           вывести список всех правил
 *   --clearrules          удалить все правила диалога
 */
class AgentCommandHandler @Inject constructor(
    private val aiAgentFactory: AIAgentFactory,
    private val agentMemoryRepository: AgentMemoryRepository
) : CommandHandler {

    override val commandName = "agent"

    override suspend fun handle(params: List<Pair<String, String?>>, chat: Chat): CommandResult {
        val paramsMap = params.toMap()
        return when {
            "addrule" in paramsMap -> handleAddRule(paramsMap["addrule"], chat)
            "listrules" in paramsMap -> handleListRules(chat)
            "clearrules" in paramsMap -> handleClearRules(chat)
            else -> CommandResult.Error(
                "Неизвестная команда agent.\n" +
                "Доступные: --addrule \"текст\" | --listrules | --clearrules"
            )
        }
    }

    private suspend fun handleAddRule(ruleText: String?, chat: Chat): CommandResult {
        if (ruleText.isNullOrBlank()) {
            return CommandResult.Error("Укажите правило: @@talk(agent --addrule \"текст правила\")")
        }

        val agent = aiAgentFactory.getOrCreate(
            AGENT_NAME,
            chat.id,
            chat.settings.systemPromt,
            defaultModel = { chat.settings.model }
        )

        // Получаем текущие правила
        val currentRules = getCurrentRules(agent.config.id)

        // Добавляем новое правило
        val updatedRules = currentRules + ruleText

        // Сохраняем обновлённый список правил как JSON массив
        val jsonRules = Json.encodeToString(updatedRules)
        agentMemoryRepository.upsertFact(
            agentId = agent.config.id,
            memoryKey = AgentRulesMemoryProvider.MEMORY_KEY,
            category = AgentRulesMemoryProvider.CATEGORY,
            fact = jsonRules
        )

        return CommandResult.Success("Правило добавлено. Всего правил: ${updatedRules.size}")
    }

    private suspend fun handleListRules(chat: Chat): CommandResult {
        val agent = aiAgentFactory.getOrCreate(
            AGENT_NAME,
            chat.id,
            chat.settings.systemPromt,
            defaultModel = { chat.settings.model }
        )

        val rules = getCurrentRules(agent.config.id)

        if (rules.isEmpty()) {
            return CommandResult.Success("Правила диалога отсутствуют.\nДобавьте правило: @@talk(agent --addrule \"текст\")")
        }

        val text = buildString {
            appendLine("Правила диалога (${rules.size}):")
            rules.forEachIndexed { index, rule ->
                appendLine("${index + 1}. $rule")
            }
        }.trim()

        return CommandResult.Success(text)
    }

    private suspend fun handleClearRules(chat: Chat): CommandResult {
        val agent = aiAgentFactory.getOrCreate(
            AGENT_NAME,
            chat.id,
            chat.settings.systemPromt,
            defaultModel = { chat.settings.model }
        )

        agentMemoryRepository.deleteFact(
            agentId = agent.config.id,
            memoryKey = AgentRulesMemoryProvider.MEMORY_KEY,
            category = AgentRulesMemoryProvider.CATEGORY
        )

        return CommandResult.Success("Все правила диалога удалены")
    }

    private suspend fun getCurrentRules(agentId: Long): List<String> {
        val fact = agentMemoryRepository.getFactByKey(agentId, AgentRulesMemoryProvider.MEMORY_KEY)
            ?: return emptyList()

        if (fact.category != AgentRulesMemoryProvider.CATEGORY) {
            return emptyList()
        }

        return try {
            Json.decodeFromString(ListSerializer(String.serializer()), fact.fact)
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val AGENT_NAME = "talk_agent"
    }
}
