package com.example.day.core.core_features.mcp.data.local.inmemory

import com.example.day.core.core_features.agent.domain.AgentRepository
import com.example.day.core.core_features.agent.domain.strategy.AContextDefaultFactory
import com.example.day.core.core_features.agent.domain.workers.concrete.JustWorkConfig
import com.example.day.core.core_features.agent.domain.workers.concrete.JustWorkWorker
import com.example.day.core.core_features.llm.domain.model.ModelSettings
import com.example.day.core.core_features.mcp.domain.McpToolNames
import com.example.day.core.core_features.mcp.domain.model.McpToolCallContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

/**
 * Локальный MCP-инструмент — точка входа пайплайна исследования файла.
 *
 * Создаёт агента "git_file_investigator", которому доступны:
 * - get_git_file_list (McpServer) — получение списка файлов
 * - get_file_analysis (local) — анализ файла по пути
 *
 * Агент сам выбирает подходящий файл по описанию и возвращает анализ.
 */
internal class InvestigateGitFileTool @Inject constructor(
    private val justWorkWorker: JustWorkWorker,
    private val agentRepository: AgentRepository
) : LocalMcpTool {

    companion object {
        const val TOOL_NAME = McpToolNames.INVESTIGATE_GIT_FILE
        const val AGENT_NAME = "git_file_investigator"
    }

    override val name: String = TOOL_NAME

    override val description: String =
        "Исследует git файл по описанию: находит подходящий файл в репозитории и возвращает его анализ. " +
            "Параметр file_request_message — описание нужного файла."

    override val inputSchema: JsonObject = buildJsonObject {
        put("type", JsonPrimitive("object"))
        put("properties", buildJsonObject {
            put("file_request_message", buildJsonObject {
                put("type", JsonPrimitive("string"))
                put("description", JsonPrimitive(
                    "Описание файла для поиска (например: 'файл содержащий Tool но не MyTool')"
                ))
            })
        })
        put("required", JsonArray(listOf(JsonPrimitive("file_request_message"))))
    }

    override suspend fun call(
        arguments: JsonObject,
        context: McpToolCallContext?
    ): Result<String> {
        val fileRequestMessage = arguments["file_request_message"]?.jsonPrimitive?.content
            ?: return Result.failure(IllegalArgumentException("file_request_message is required"))

        val chatId = context?.chatId
            ?: resolveChatId(context?.agentId)
            ?: return Result.failure(IllegalStateException("chatId is required in context"))

        val config = JustWorkConfig(
            agentName = AGENT_NAME,
            chatId = chatId,
            systemPrompt = buildSystemPrompt(),
            allowedTools = listOf(
                McpToolNames.GET_GIT_FILE_LIST,
                McpToolNames.GET_FILE_ANALYSIS
            ),
            defaultModel = { ModelSettings.default() },
            defaultContext = { AContextDefaultFactory.createFull() }
        )

        return justWorkWorker.doWork(
            config = config,
            userPrompt = "Мне нужен результат анализа файла. $fileRequestMessage"
        ).mapCatching { responseText ->
            buildJsonObject {
                put("content", JsonPrimitive(responseText))
            }.toString()
        }.recoverCatching { e ->
            buildJsonObject {
                put("content", JsonPrimitive(null as String?))
                put("error", JsonPrimitive(e.message ?: "Unknown error"))
            }.toString()
        }
    }

    private suspend fun resolveChatId(agentId: Long?): Long? {
        agentId ?: return null
        return agentRepository.getChatsForAgent(agentId).first().firstOrNull()?.id
    }

    private fun buildSystemPrompt(): String =
        """Тебе доступны инструменты:
- get_git_file_list для получения списка файлов
- get_file_analysis для получения анализа по файлу

Действуй строго последовательно и прямолинейно:
1. Получи список файлов с помощью get_git_file_list
2. Найди в списке файл (включая полный путь) наиболее подходящий под описание пользователя
3. Используй get_file_analysis для получения анализа по файлу
4. Скажи пользователю полное имя файла и текст полученного анализа"""
}
