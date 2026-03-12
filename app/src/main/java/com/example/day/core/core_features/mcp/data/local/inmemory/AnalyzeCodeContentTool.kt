package com.example.day.core.core_features.mcp.data.local.inmemory

import com.example.day.core.core_features.agent.domain.AgentRepository
import com.example.day.core.core_features.agent.domain.strategy.AContextDefaultFactory
import com.example.day.core.core_features.agent.domain.workers.concrete.JustWorkConfig
import com.example.day.core.core_features.agent.domain.workers.concrete.JustWorkWorker
import com.example.day.core.core_features.llm.domain.model.ModelSettings
import com.example.day.core.core_features.mcp.domain.McpToolNames
import com.example.day.core.core_features.mcp.domain.model.McpToolCallContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

internal class AnalyzeCodeContentTool @Inject constructor(
    private val justWorkWorker: JustWorkWorker,
    private val agentRepository: AgentRepository
) : LocalMcpTool {

    companion object {
        const val TOOL_NAME = McpToolNames.ANALYZE_CODE_CONTENT
        const val AGENT_NAME = "content_analyzer"
    }

    override val name: String = TOOL_NAME

    override val description: String =
        "Исследует содержимое контента и возвращает обобщенный анализ: " +
            "краткое описание, что хорошо, что плохо, рекомендации."

    override val inputSchema: JsonObject = buildJsonObject {
        put("type", JsonPrimitive("object"))
        put("properties", buildJsonObject {
            put("content", buildJsonObject {
                put("type", JsonPrimitive("string"))
                put("description", JsonPrimitive("Содержимое файла или текст для анализа"))
            })
        })
        put("required", kotlinx.serialization.json.JsonArray(listOf(JsonPrimitive("content"))))
    }

    override suspend fun call(
        arguments: JsonObject,
        context: McpToolCallContext?
    ): Result<String> {
        val content = arguments["content"]?.jsonPrimitive?.content
            ?: return Result.failure(IllegalArgumentException("content is required"))

        val chatId = context?.chatId
            ?: resolveChatId(context?.agentId)
            ?: return Result.failure(IllegalStateException("chatId is required in context"))

        val config = JustWorkConfig(
            agentName = AGENT_NAME,
            chatId = chatId,
            systemPrompt = buildSystemPrompt(),
            allowedTools = emptyList(),
            defaultModel = { ModelSettings.default() },
            defaultContext = { AContextDefaultFactory.createFull() }
        )

        return justWorkWorker.doWork(
            config = config,
            userPrompt = "Проанализируй пожалуйста это:\n$content"
        ).mapCatching { responseText ->
            buildJsonObject {
                put("status", JsonPrimitive("ok"))
                put("analysis_result", JsonPrimitive(responseText))
            }.toString()
        }.recoverCatching { e ->
            buildJsonObject {
                put("status", JsonPrimitive("error"))
                put("error", JsonPrimitive(e.message ?: "Unknown error"))
            }.toString()
        }
    }

    private suspend fun resolveChatId(agentId: Long?): Long? {
        agentId ?: return null
        return agentRepository.getChatsForAgent(agentId).first().firstOrNull()?.id
    }

    private fun buildSystemPrompt(): String =
        """Ты Kotlin Senior Developer, с многолетним опытом разработки и построения больших, но понятных и расширяемых систем; фанат Clean Architecture, SOLID, Design Patterns и best coding practicles.
Твоя задача проанализировать текст, который тебе принес пользователь. Выдать какое-то резюме: короткое описание содержимого, что хорошо, что плохо, рекомендации."""
}
