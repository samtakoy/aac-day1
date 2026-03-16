package com.example.day.core.core_features.mcp.data.local.inmemory

import android.util.Log
import com.example.day.core.core_features.mcp.domain.McpLocalConstants
import com.example.day.core.core_features.mcp.domain.McpToolNames
import com.example.day.core.core_features.mcp.domain.model.McpConnectionState
import com.example.day.core.core_features.mcp.domain.model.McpToolCallContext
import com.example.day.core.core_features.mcp.domain.model.TransportType
import com.example.day.core.core_features.mcp.domain.repository.FileAnalysisRepository
import com.example.day.core.core_features.mcp.domain.repository.McpRepository
import com.example.day.core.core_features.mcp.domain.tools.McpTools
import dagger.Lazy
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
 * Локальный MCP-инструмент для получения анализа файла с кешированием.
 *
 * Пайплайн:
 * 1. Проверяет кеш (FileAnalysisRepository)
 * 2. Если нет — скачивает содержимое через get_file_content (remote McpServer)
 * 3. Анализирует через analyze_code_content (local McpServer)
 * 4. Сохраняет результат в кеш
 */
internal class GetFileAnalysisTool @Inject constructor(
    private val fileAnalysisRepository: FileAnalysisRepository,
    private val mcpRepository: McpRepository,
    private val mcpTools: Lazy<McpTools>,  // Lazy для разрыва цикла Dagger
    private val json: Json
) : LocalMcpTool {

    companion object {
        const val TOOL_NAME = McpToolNames.GET_FILE_ANALYSIS
        const val FILE_CONTENT_TTL_MINUTES = 180L
    }

    override val name: String = TOOL_NAME

    override val description: String =
        "Получает анализ файла по его точному полному пути (например /app/src/main/java/com/example/File.kt). " +
            "ВАЖНО: требует точный полный путь — не имя файла. " +
            "Если известно только имя файла, сначала вызови get_git_file_list чтобы найти полный путь."

    override val inputSchema: JsonObject = buildJsonObject {
        put("type", JsonPrimitive("object"))
        put("properties", buildJsonObject {
            put("file_full_path", buildJsonObject {
                put("type", JsonPrimitive("string"))
                put("description", JsonPrimitive("Точный полный путь к файлу (например /app/src/main/java/com/example/File.kt). Не имя файла — полный путь."))
            })
        })
        put("required", JsonArray(listOf(JsonPrimitive("file_full_path"))))
    }

    override suspend fun call(
        arguments: JsonObject,
        context: McpToolCallContext?
    ): Result<String> = runCatching {
        val filePath = arguments["file_full_path"]?.jsonPrimitive?.content
            ?: error("file_full_path is required")

        Log.d("ktor", "[$TOOL_NAME] call started: filePath='$filePath', chatId=${context?.chatId}")

        // 1. Проверяем кеш
        val cached = fileAnalysisRepository.getAnalysis(filePath)
        if (cached != null) {
            Log.d("ktor", "[$TOOL_NAME] cache hit for '$filePath'")
            return@runCatching buildJsonObject {
                put("message", JsonPrimitive("Анализ файла $filePath получен из кеша"))
                put("content", JsonPrimitive(cached.content))
            }.toString()
        }

        Log.d("ktor", "[$TOOL_NAME] cache miss, downloading content for '$filePath'")

        // 2. Скачиваем содержимое файла через remote MCP tool get_file_content
        val fileContent = downloadFileContent(filePath, context)
        Log.d("ktor", "[$TOOL_NAME] downloaded content for '$filePath', length=${fileContent.length}")

        // 3. Анализируем через local MCP tool analyze_code_content
        Log.d("ktor", "[$TOOL_NAME] calling analyze_code_content for '$filePath'")
        val analysisResult = analyzeContent(fileContent, context)
        Log.d("ktor", "[$TOOL_NAME] analysis done for '$filePath', resultLength=${analysisResult.length}")

        // 4. Сохраняем в кеш
        fileAnalysisRepository.saveAnalysis(filePath, analysisResult)

        buildJsonObject {
            put("message", JsonPrimitive("Анализ файла $filePath выполнен успешно"))
            put("content", JsonPrimitive(analysisResult))
        }.toString()
    }.onFailure { e ->
        val filePath = arguments["file_full_path"]?.jsonPrimitive?.content ?: ""
        Log.d("ktor", "[$TOOL_NAME] error for '$filePath': ${e.message}")
    }

    private suspend fun downloadFileContent(filePath: String, context: McpToolCallContext?): String {
        val serverId = findServerWithTool(McpToolNames.GET_FILE_CONTENT)
            ?: error("No MCP server found with tool '${McpToolNames.GET_FILE_CONTENT}'. Make sure McpServer is connected.")

        val resultJson = mcpTools.get().callTool(
            serverId = serverId,
            toolName = McpToolNames.GET_FILE_CONTENT,
            arguments = buildJsonObject { put("file_path", JsonPrimitive(filePath)) },
            context = context
        ).getOrElse { e -> error("Failed to download file content: ${e.message}") }

        // Парсим ответ: {"status":"ok","content":"...","error":"..."}
        val responseObj = runCatching {
            json.parseToJsonElement(resultJson).jsonObject
        }.getOrNull()

        return responseObj?.get("content")?.jsonPrimitive?.content
            ?: resultJson  // fallback: если ответ — plain text
    }

    private suspend fun analyzeContent(content: String, context: McpToolCallContext?): String {
        val resultJson = mcpTools.get().callTool(
            serverId = McpLocalConstants.LOCAL_SERVER_ID,
            toolName = McpToolNames.ANALYZE_CODE_CONTENT,
            arguments = buildJsonObject { put("content", JsonPrimitive(content)) },
            context = context
        ).getOrElse { e -> error("Failed to analyze content: ${e.message}") }

        // Парсим ответ: {"message":"...","content":"..."}
        val responseObj = runCatching {
            json.parseToJsonElement(resultJson).jsonObject
        }.getOrNull()

        return responseObj?.get("content")?.jsonPrimitive?.content
            ?: resultJson  // fallback
    }

    /**
     * Находит сервер, у которого зарегистрирован инструмент с указанным именем.
     * Пропускает локальный сервер (LOCAL transport).
     */
    private suspend fun findServerWithTool(toolName: String): String? {
        val servers = mcpRepository.getServers().first()
        for (server in servers) {
            if (server.transportType == TransportType.LOCAL) continue
            if (!server.isEnabled) continue

            val tools = when (val state = mcpRepository.getConnectionState(server.id).first()) {
                is McpConnectionState.Connected -> state.tools
                else -> {
                    when (val connected = mcpRepository.connect(server.id)) {
                        is McpConnectionState.Connected -> connected.tools
                        else -> emptyList()
                    }
                }
            }
            if (tools.any { it.name == toolName }) return server.id
        }
        return null
    }
}
