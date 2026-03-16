package com.example.day.core.core_features.mcp.data.local.inmemory

import android.util.Log
import com.example.day.core.core_features.agent.domain.AgentRepository
import com.example.day.core.core_features.agent.domain.strategy.AContextDefaultFactory
import com.example.day.core.core_features.agent.domain.workers.concrete.JustWorkConfig
import com.example.day.core.core_features.agent.domain.workers.concrete.JustWorkWorker
import com.example.day.core.core_features.llm.domain.model.ModelSettings
import com.example.day.core.core_features.mcp.domain.McpToolNames
import com.example.day.core.core_features.mcp.domain.model.McpToolCallContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
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

        Log.d("ktor", "[$TOOL_NAME] call started: request='$fileRequestMessage', chatId=${context?.chatId}, agentId=${context?.agentId}")

        val chatId = context?.chatId
            ?: resolveChatId(context?.agentId)
            ?: return Result.failure(IllegalStateException("chatId is required in context"))

        Log.d("ktor", "[$TOOL_NAME] creating agent '$AGENT_NAME' for chatId=$chatId")

        val config = JustWorkConfig(
            agentName = AGENT_NAME,
            chatId = chatId,
            systemPrompt = buildSystemPrompt(),
            allowedTools = listOf(
                McpToolNames.GET_GIT_FILE_LIST,
                McpToolNames.GET_FILE_ANALYSIS
            ),
            defaultModel = { ModelSettings.default().copy(jsonFormat = true) },
            defaultContext = { AContextDefaultFactory.createFull() },
            recreateAgent = true
        )

        return justWorkWorker.doWork(
            config = config,
            userPrompt = "Мне нужен результат анализа файла. $fileRequestMessage"
        ).mapCatching { responseText ->
            Log.d("ktor", "[$TOOL_NAME] agent raw response: $responseText")
            // Агент возвращает JSON {"message":"...","content":"..."} — пропускаем как есть.
            // Если ответ невалидный JSON — оборачиваем в наш формат как запасной вариант.
            runCatching {
                Json.parseToJsonElement(responseText.trim()).jsonObject
                responseText.trim()
            }.getOrElse { e ->
                Log.d("ktor", "[$TOOL_NAME] agent response is not valid JSON, wrapping: ${e.message}")
                buildJsonObject {
                    put("message", JsonPrimitive("Результат получен"))
                    put("content", JsonPrimitive(responseText))
                }.toString()
            }.also { result ->
                Log.d("ktor", "[$TOOL_NAME] final result: $result")
            }
        }.onFailure { e ->
            Log.d("ktor", "[$TOOL_NAME] doWork error: ${e.message}")
        }
    }

    private suspend fun resolveChatId(agentId: Long?): Long? {
        agentId ?: return null
        return agentRepository.getChatsForAgent(agentId).first().firstOrNull()?.id
    }

    private fun buildSystemPrompt(): String =
        """
== ЗАДАЧА ==
По описанию пользователя найти файл в git, запросить его анализ и результат выдать пользователю.
Если точного совпадения не найдено - покажи пользователю наиболее подходящие имена.

== АЛГОРИТМ РАБОТЫ ==
Действуй строго последовательно и прямолинейно:
1. Получи список файлов с помощью одного доступных инструментов с git
2. Найди в списке файл (включая полный путь) наиболее подходящий под описание пользователя.
4. Если точное совпадение не найдено - выдай в content список наиболее подходящих файлов, свой комментарий в message и закончи работу.
5. Если файл идентифицирован в списке, то используй инструменты и получени анализ по файлу
6. Скажи пользователю полное имя файла в поле message и текст полученного анализа в поле content 

== ФОРМАТ ОТВЕТА ==
Весь ответ — это ОДИН валидный JSON-объект, без markdown, без пояснений до/после.
Ты должен отвечать строго ТОЛЬКО в формате JSON:
{
    "message": "твое сообщение, описывающее результат работы",
    "content": "Анализ файла, либо список имен наиболее подходящих файлов"
}
Текст message - формальный и сухой.

Пример 1:
{
    "message": "Вот полный анализ файла [имя файла] по вашему запросу",
    "content": "[Текст анализа файла]"
}

Пример 2:
{
    "message": "Подходящего по имени файла не найдено, вот несколько похожих вариантов",
    "content": "[список полных имен файлов через запятую]"
}

Пример 3:
{
    "message": "Не найдено подходящего инструмента для выполнения вашего запроса",
    "content": "null"
}
"""
}
