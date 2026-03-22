package com.example.day.ragserver.agent_context

import com.example.day.ragserver.indexing.LlmProvider
import com.example.day.ragserver.logging.SessionLogger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Обновляет TaskState сессии через Ollama LLM.
 *
 * Принимает текущий TaskState (JSON) + последние сообщения диалога.
 * Возвращает обновлённый TaskState (JSON строка) + краткое резюме последнего ответа.
 *
 * TODO: Кандидат на переезд в отдельный AgentServer.
 *   Сейчас использует LlmProvider из rag-server (Ollama).
 *   При выносе: перенести в agent-server, оставить только регистрацию маршрута здесь.
 */
class TaskStateUpdaterService(
    private val llmProvider: LlmProvider,
    private val sessionLogger: SessionLogger? = null,
) {
    companion object {
        // Максимальная длина ответа ассистента, передаваемого в промпт task-state.
        // Полный ответ (до 8000+ символов) перегружает контекст модели — она перестаёт
        // заполнять last_response_summary и возвращает пустую строку.
        // Для задачи "написать 1-2 предложения о чём ответил ассистент" достаточно начала ответа.
        private const val MAX_ASSISTANT_CONTENT_FOR_SUMMARY = 600
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    suspend fun update(request: TaskStateUpdateRequest): TaskStateUpdateResponse {
        val historyText = request.lastMessages.joinToString("\n") { msg ->
            val content = if (msg.role.equals("assistant", ignoreCase = true) && msg.content.length > MAX_ASSISTANT_CONTENT_FOR_SUMMARY) {
                "${msg.content.take(MAX_ASSISTANT_CONTENT_FOR_SUMMARY)}… (truncated)"
            } else msg.content
            "${msg.role.uppercase()}: $content"
        }
        val historyTextForLog = request.lastMessages.joinToString("\n") { msg ->
            val content = msg.content
            if (msg.role.equals("assistant", ignoreCase = true) && content.length > 150) {
                "ASSISTANT: ${content.take(150)}... (${content.length} chars)"
            } else {
                "${msg.role.uppercase()}: $content"
            }
        }

        println("[rag][ktor][task-state][${llmProvider.modelName}] updating, messages=${request.lastMessages.size}")

        val prompt = buildPrompt(request.currentState, historyText)
        val rawResponse = llmProvider.generate(prompt)

        return parseResponse(rawResponse).also { response ->
            println("[rag][ktor][task-state][${llmProvider.modelName}] done, summary='${response.lastResponseSummary.take(60)}'")

            // Парсим итоговый state для лога (best-effort)
            val parsedState = runCatching {
                json.decodeFromString<LlmResponseDto>(
                    rawResponse.substring(
                        rawResponse.indexOf('{').takeIf { it >= 0 } ?: 0,
                        (rawResponse.lastIndexOf('}') + 1).takeIf { it > 0 } ?: rawResponse.length
                    )
                ).updatedState
            }.getOrNull()

            sessionLogger?.logTaskStateUpdate(
                historyForLog = historyTextForLog,
                rawResponse = rawResponse,
                intent = parsedState?.intent ?: "?",
                focusClass = parsedState?.currentFocus?.className ?: "?",
                switched = parsedState?.contextSwitched ?: false,
                summary = response.lastResponseSummary,
                model = llmProvider.modelName,
            )
        }
    }

    private fun buildPrompt(currentState: String, historyText: String): String = """
        ### ROLE
        You are a State Manager for a RAG code assistant.
        Maintain an accurate TaskState (JSON) based on recent messages.
        Also produce a short summary of the last assistant response.

        ### TASK STATE STRUCTURE
        {
          "current_focus": { "file": "", "class": "", "method": "" },
          "tech_stack": "",
          "intent": "general|debugging|architecture|implementation",
          "context_switched": false,
          "confirmed_decisions": [],
          "open_questions": []
        }

        ### UPDATE RULES
        1. CONTEXT SWITCH: Set context_switched=true if ANY of these:
           - User mentions a new file/class not related to current_focus
           - User switches to a completely different topic/concept (e.g. from "configuration" to "message history")
           - The new question has no semantic overlap with the current_focus file/class
           Set context_switched=false only if the discussion clearly continues the same topic.
        2. FOCUS UPDATE: Enrich current_focus with file, class, method if mentioned.
        3. INTENT: From the latest user message:
           - debugging → mentions bugs/errors/crashes
           - architecture → structure/design/pattern/module
           - implementation → adding feature/writing code
           - general → otherwise
        4. confirmed_decisions: Add ONLY explicit decisions ("we'll use X", "decided to Y"). No duplicates.
        5. open_questions: Questions still awaiting answers. Remove when answered.
        6. tech_stack: Do not change unless explicitly mentioned.
        7. Do NOT clear fields without reason.

        ### LAST_RESPONSE_SUMMARY RULES
        - Write 1-2 sentences summarizing what the ASSISTANT said last.
        - Be specific: mention class/file/method names if relevant.
        - Leave empty string "" if there is no assistant message in LAST_MESSAGES.

        ### OUTPUT FORMAT
        Return ONLY valid JSON, no explanation, no markdown fences:
        {
          "updated_state": {
            "current_focus": { "file": "", "class": "", "method": "" },
            "tech_stack": "",
            "intent": "general",
            "context_switched": false,
            "confirmed_decisions": [],
            "open_questions": []
          },
          "last_response_summary": ""
        }

        ### CURRENT_STATE
        $currentState

        ### LAST_MESSAGES
        $historyText
    """.trimIndent()

    /**
     * Парсит ответ LLM через kotlinx.serialization.
     * Модель может добавить текст вокруг JSON — берём подстроку от первого { до последнего }.
     */
    private fun parseResponse(raw: String): TaskStateUpdateResponse {
        val jsonStart = raw.indexOf('{')
        val jsonEnd = raw.lastIndexOf('}')

        if (jsonStart == -1 || jsonEnd <= jsonStart) {
            println("[rag][ktor][task-state] WARNING: no JSON found in LLM response, returning defaults")
            return defaultResponse()
        }

        return runCatching {
            val dto = json.decodeFromString<LlmResponseDto>(raw.substring(jsonStart, jsonEnd + 1))
            TaskStateUpdateResponse(
                updatedState = json.encodeToString(TaskStateDto.serializer(), dto.updatedState),
                lastResponseSummary = dto.lastResponseSummary,
            )
        }.getOrElse { e ->
            println("[rag][ktor][task-state] ERROR: failed to parse LLM response: ${e.message}")
            defaultResponse()
        }
    }

    private fun defaultResponse() = TaskStateUpdateResponse(updatedState = "{}", lastResponseSummary = "")
}

// --- Internal DTOs for LLM response deserialization ---

@Serializable
private data class LlmResponseDto(
    @SerialName("updated_state") val updatedState: TaskStateDto,
    @SerialName("last_response_summary") val lastResponseSummary: String = "",
)

@Serializable
data class TaskStateDto(
    @SerialName("current_focus") val currentFocus: FocusDto = FocusDto(),
    @SerialName("tech_stack") val techStack: String = "",
    @SerialName("intent") val intent: String = "general",
    @SerialName("context_switched") val contextSwitched: Boolean = false,
    @SerialName("confirmed_decisions") val confirmedDecisions: List<String> = emptyList(),
    @SerialName("open_questions") val openQuestions: List<String> = emptyList(),
)

@Serializable
data class FocusDto(
    @SerialName("file") val file: String = "",
    @SerialName("class") val className: String = "",
    @SerialName("method") val method: String = "",
)
