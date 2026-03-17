package com.example.day.core.core_features.agent.domain.workers.innercommand.handler

import com.example.day.core.core_features.agent.domain.AgentRepository
import com.example.day.core.core_features.agent.domain.repository.AgentMemoryRepository
import com.example.day.core.core_features.agent.domain.strategy.AContextDefaultFactory
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.memory.domain.provider.AutoRagMemoryProvider
import com.example.day.core.core_features.memory.domain.provider.base.MemoryType
import javax.inject.Inject

/**
 * Handles @@talk(rag ...) commands for configuring RAG-based prompt enrichment.
 *
 * Supported sub-commands:
 *   --on           enable AutoRag for this agent (adds MemoryType.AutoRag)
 *   --off          disable AutoRag (removes MemoryType.AutoRag)
 *   --state        show current status and configured server URL
 *   --url <url>    set rag-server URL (stored in AgentMemoryRepository)
 */
class RagCommandHandler @Inject constructor(
    private val agentRepository: AgentRepository,
    private val agentMemoryRepository: AgentMemoryRepository
) : CommandHandler {

    override val commandName = "rag"

    override suspend fun handle(params: List<Pair<String, String?>>, chat: Chat): CommandResult {
        val paramsMap = params.toMap()
        val agentConfig = agentRepository.getOrCreateAgent(
            systemName = AGENT_NAME,
            chatId = chat.id,
            systemPrompt = chat.settings.systemPromt,
            defaultModel = { chat.settings.model },
            defaultContext = { AContextDefaultFactory.createFull() }
        )
        return when {
            "on"    in paramsMap -> handleOn(agentConfig.id)
            "off"   in paramsMap -> handleOff(agentConfig.id)
            "state" in paramsMap -> handleState(agentConfig.id)
            "url"   in paramsMap -> handleUrl(agentConfig.id, paramsMap["url"])
            else -> CommandResult.Error(
                "Неизвестная команда rag.\nДоступные: --on | --off | --state | --url <url>"
            )
        }
    }

    private suspend fun handleOn(agentId: Long): CommandResult {
        agentRepository.addMemoryType(agentId, MemoryType.AutoRag)
        val url = currentUrl(agentId)
        return CommandResult.Success("RAG включён. Сервер: $url")
    }

    private suspend fun handleOff(agentId: Long): CommandResult {
        agentRepository.removeMemoryType(agentId, MemoryType.AutoRag)
        return CommandResult.Success("RAG выключен")
    }

    private suspend fun handleState(agentId: Long): CommandResult {
        val types = agentRepository.getMemoryTypes(agentId)
        val isOn = MemoryType.AutoRag in types
        val url = currentUrl(agentId)
        return CommandResult.Success(
            "RAG: ${if (isOn) "включён ✓" else "выключен"}\nURL: $url"
        )
    }

    private suspend fun handleUrl(agentId: Long, url: String?): CommandResult {
        if (url.isNullOrBlank()) return CommandResult.Error("Укажите URL сервера")
        agentMemoryRepository.upsertFact(
            agentId,
            AutoRagMemoryProvider.MEMORY_KEY,
            AutoRagMemoryProvider.CATEGORY_URL,
            url
        )
        return CommandResult.Success("URL сохранён: $url")
    }

    private suspend fun currentUrl(agentId: Long): String =
        agentMemoryRepository
            .getFact(agentId, AutoRagMemoryProvider.MEMORY_KEY, AutoRagMemoryProvider.CATEGORY_URL)
            ?.fact ?: AutoRagMemoryProvider.DEFAULT_URL

    companion object {
        private const val AGENT_NAME = "talk_agent"
    }
}
