package com.example.day.core.core_features.agent.domain.workers.innercommand.handler

import com.example.day.core.core_features.agent.domain.AgentRepository
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.memory.domain.provider.base.MemoryType
import javax.inject.Inject

/**
 * Handles @@talk(agent_memory ...) commands for managing agent memory types.
 *
 * Supported sub-commands:
 *   --add MemoryType    add a memory type to talk_agent (by name or dbName)
 *   --remove MemoryType remove a memory type from talk_agent
 *   --list              list all active memory types for talk_agent
 */
class AgentMemoryCommandHandler @Inject constructor(
    private val agentRepository: AgentRepository
) : CommandHandler {

    override val commandName = "agent_memory"

    override suspend fun handle(params: List<Pair<String, String?>>, chat: Chat): CommandResult {
        val paramsMap = params.toMap()
        val agentConfig = agentRepository.getOrCreateAgent(
            systemName = AGENT_NAME,
            chatId = chat.id,
            systemPromt = chat.settings.systemPromt,
            model = chat.settings.model
        )
        return when {
            "add" in paramsMap    -> handleAdd(agentConfig.id, paramsMap["add"])
            "remove" in paramsMap -> handleRemove(agentConfig.id, paramsMap["remove"])
            "list" in paramsMap   -> handleList(agentConfig.id)
            else -> CommandResult.Error(
                "Неизвестная команда agent_memory. Доступные: --add MemoryType | --remove MemoryType | --list\n" +
                "Доступные типы: ${MemoryType.entries.joinToString(", ") { it.name }}"
            )
        }
    }

    private suspend fun handleAdd(agentId: Long, typeName: String?): CommandResult {
        val memoryType = resolveMemoryType(typeName)
            ?: return CommandResult.Error(
                "Неизвестный MemoryType: '$typeName'.\n" +
                "Доступные: ${MemoryType.entries.joinToString(", ") { "${it.name} (${it.dbName})" }}"
            )
        agentRepository.addMemoryType(agentId, memoryType)
        return CommandResult.Success("Тип памяти [${memoryType.name}] добавлен агенту")
    }

    private suspend fun handleRemove(agentId: Long, typeName: String?): CommandResult {
        val memoryType = resolveMemoryType(typeName)
            ?: return CommandResult.Error(
                "Неизвестный MemoryType: '$typeName'.\n" +
                "Доступные: ${MemoryType.entries.joinToString(", ") { "${it.name} (${it.dbName})" }}"
            )
        agentRepository.removeMemoryType(agentId, memoryType)
        return CommandResult.Success("Тип памяти [${memoryType.name}] удалён у агента")
    }

    private suspend fun handleList(agentId: Long): CommandResult {
        val types = agentRepository.getMemoryTypes(agentId)
        val text = if (types.isEmpty()) {
            "Типы памяти не настроены.\nДоступные: ${MemoryType.entries.joinToString(", ") { it.name }}"
        } else {
            buildString {
                appendLine("Активные типы памяти агента (${types.size}):")
                types.forEach { appendLine("  • ${it.name} (${it.dbName})") }
            }.trim()
        }
        return CommandResult.Success(text)
    }

    /** Resolves MemoryType by its enum name or dbName (case-insensitive). */
    private fun resolveMemoryType(input: String?): MemoryType? {
        if (input.isNullOrBlank()) return null
        return MemoryType.entries.find { type ->
            type.name.equals(input, ignoreCase = true) ||
            type.dbName.equals(input, ignoreCase = true)
        }
    }

    companion object {
        private const val AGENT_NAME = "talk_agent"
    }
}
