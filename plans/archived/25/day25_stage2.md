# Этап 2: TaskState + AgentServer endpoint

## Что решает этот этап
Реализует хранение и обновление TaskState:
- На стороне rag-server: новый endpoint `POST /task-state/update`, который принимает текущий TaskState и последние сообщения, обновляет его через Ollama и возвращает обновлённый JSON + краткое резюме последнего ответа.
- На стороне Android: `TaskStateRepository` — сохраняет/читает TaskState в `AgentMemoryRepository`. `TaskStateUpdater` — вызывает endpoint перед каждым RAG-запросом.

Этот этап реализует Вариант B для Short History: LLM для TaskState возвращает `last_response_summary` — это резюме последнего ответа ассистента, которое будет сохранено как часть сжатой истории в Этапе 3.

## Что получим в итоге
- `POST rag-server/task-state/update` работает и тестируется вручную
- В памяти агента (category="task_state") хранится актуальный TaskState
- Логи показывают обновление TaskState при каждом запросе

## Зависимости
- Требует завершения Этапа 1 (нужен `RagWorker` и агентная инфраструктура)
- Этап 3 зависит от этого этапа

---

## TaskState JSON структура (финальная)

```json
{
  "current_focus": { "file": "", "class": "", "method": "" },
  "tech_stack": "Kotlin, Android, ...",
  "intent": "general|debugging|architecture|implementation",
  "context_switched": false,
  "confirmed_decisions": [],
  "open_questions": []
}
```

---

## Пошаговый план реализации

### Часть А. rag-server — новый endpoint

**ВАЖНО:** Весь код для этого endpoint кладётся в отдельную папку
`rag-server/src/main/kotlin/com/example/day/ragserver/agent_context/`.
Это кандидат на переезд в отдельный AgentServer. Логика endpoint не должна
проникать в другие части rag-server — только регистрация маршрута в `RagServer.kt`.

---

#### А.1. Модели запроса/ответа

**Файл:** `rag-server/.../agent_context/TaskStateModels.kt`

```kotlin
package com.example.day.ragserver.agent_context

import kotlinx.serialization.Serializable

/**
 * Запрос на обновление TaskState.
 * TODO: candidate for migration to a dedicated AgentServer module.
 */
@Serializable
data class TaskStateUpdateRequest(
    val currentState: String,   // JSON строка текущего TaskState (или "{}" если первый раз)
    val lastMessages: List<MessageDto>, // последние 4 сообщения из истории
)

@Serializable
data class MessageDto(
    val role: String,   // "user" | "assistant"
    val content: String,
)

/**
 * Ответ: обновлённый TaskState + краткое резюме последнего ответа ассистента.
 * last_response_summary используется как Short History для QueryOptimizer (Variant B).
 */
@Serializable
data class TaskStateUpdateResponse(
    val updatedState: String,          // JSON строка обновлённого TaskState
    val lastResponseSummary: String,   // 1-2 предложения: суть последнего ответа ассистента
)
```

---

#### А.2. Сервис обновления TaskState

**Файл:** `rag-server/.../agent_context/TaskStateUpdaterService.kt`

```kotlin
package com.example.day.ragserver.agent_context

import com.example.day.ragserver.indexing.LlmProvider

/**
 * Обновляет TaskState через Ollama LLM.
 * TODO: candidate for migration to a dedicated AgentServer module.
 *
 * Prompting strategy:
 * - Принимает currentState (JSON) + lastMessages
 * - Возвращает ТОЛЬКО JSON с полями: updated_state (объект) + last_response_summary (строка)
 */
class TaskStateUpdaterService(private val llmProvider: LlmProvider) {

    suspend fun update(request: TaskStateUpdateRequest): TaskStateUpdateResponse {
        val historyText = request.lastMessages.joinToString("\n") { msg ->
            "${msg.role.uppercase()}: ${msg.content}"
        }

        val prompt = buildPrompt(request.currentState, historyText)
        val rawResponse = llmProvider.generate(prompt)

        return parseResponse(rawResponse)
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
        1. CONTEXT SWITCH: If the user started discussing a discuss_later file/class — set context_switched=true and update current_focus.
        2. FOCUS UPDATE: If the discussion continues — enrich current_focus (file, class, method).
        3. INTENT: Determine from the latest user message: debugging → mentions bugs/errors; architecture → structure/design; implementation → feature/code writing; otherwise general.
        4. confirmed_decisions: Add ONLY if there is an explicit decision (e.g. "we'll use JWT"). Do not duplicate.
        5. open_questions: Questions that still await answers. Remove when answered.
        6. tech_stack: Do not change unless explicitly mentioned.
        7. Do NOT clear fields without reason.

        ### LAST_RESPONSE_SUMMARY
        Write 1-2 sentences summarizing what the ASSISTANT said last (last assistant message).
        Be specific: mention class/file names if relevant. Leave empty if no assistant message.

        ### OUTPUT FORMAT
        Return ONLY valid JSON, no explanation, no markdown:
        {
          "updated_state": { ...full task state object... },
          "last_response_summary": "..."
        }

        ### CURRENT_STATE
        $currentState

        ### LAST_MESSAGES
        $historyText
    """.trimIndent()

    private fun parseResponse(raw: String): TaskStateUpdateResponse {
        // Извлекаем JSON из ответа (модель может добавить лишний текст)
        val jsonStart = raw.indexOf('{')
        val jsonEnd = raw.lastIndexOf('}')
        if (jsonStart == -1 || jsonEnd == -1) {
            println("[TaskStateUpdaterService] WARNING: Could not parse LLM response, returning defaults")
            return TaskStateUpdateResponse(
                updatedState = "{}",
                lastResponseSummary = ""
            )
        }

        return try {
            val json = raw.substring(jsonStart, jsonEnd + 1)
            // Парсим вручную — избегаем сложной десериализации вложенных объектов
            val updatedStateMatch = Regex("\"updated_state\"\\s*:\\s*(\\{[^}]+\\})", RegexOption.DOT_MATCHES_ALL).find(json)
            val summaryMatch = Regex("\"last_response_summary\"\\s*:\\s*\"([^\"]*)\"").find(json)

            TaskStateUpdateResponse(
                updatedState = updatedStateMatch?.groupValues?.get(1) ?: "{}",
                lastResponseSummary = summaryMatch?.groupValues?.get(1) ?: ""
            )
        } catch (e: Exception) {
            println("[TaskStateUpdaterService] ERROR parsing response: ${e.message}")
            TaskStateUpdateResponse(updatedState = "{}", lastResponseSummary = "")
        }
    }
}
```

**Примечание:** Простой regex-парсинг достаточен для MVP. Если модель возвращает стабильный JSON — можно заменить на `kotlinx.serialization` позже.

---

#### А.3. Регистрация endpoint в RagServer.kt

**Файл:** `rag-server/.../RagServer.kt`

В блоке `routing { ... }` добавить:

```kotlin
// TODO: This endpoint is a candidate for migration to a dedicated AgentServer.
// All implementation lives in agent_context/ package to minimize coupling with rag-server.
post("/task-state/update") {
    val request = runCatching { call.receive<TaskStateUpdateRequest>() }.getOrElse {
        return@post call.respond(HttpStatusCode.BadRequest, "Invalid request body")
    }
    val response = taskStateUpdaterService.update(request)
    call.respond(response)
}
```

Перед `embeddedServer(...)` создать сервис:

```kotlin
// TODO: candidate for AgentServer migration
val taskStateUpdaterService = TaskStateUpdaterService(
    llmProvider = OllamaLlmProvider(
        baseUrl = config.ollamaBaseUrl,
        model = config.translateLlmModel,  // используем ту же модель что для QueryOptimizer
        httpClient = httpClient
    )
)
```

---

### Часть Б. Android — TaskState repository и updater

---

#### Б.1. Domain модель TaskState

**Файл:** `app/.../memory/domain/provider/rag/TaskState.kt`

```kotlin
package com.example.day.core.core_features.memory.domain.provider.rag

import kotlinx.serialization.Serializable

@Serializable
data class TaskState(
    val currentFocus: CurrentFocus = CurrentFocus(),
    val techStack: String = "",
    val intent: String = "general",
    val contextSwitched: Boolean = false,
    val confirmedDecisions: List<String> = emptyList(),
    val openQuestions: List<String> = emptyList(),
) {
    @Serializable
    data class CurrentFocus(
        val file: String = "",
        val `class`: String = "",
        val method: String = "",
    )

    companion object {
        val EMPTY = TaskState()
    }
}
```

---

#### Б.2. TaskStateRepository интерфейс

**Файл:** `app/.../memory/domain/provider/rag/TaskStateRepository.kt`

```kotlin
package com.example.day.core.core_features.memory.domain.provider.rag

import com.example.day.core.core_features.agent.domain.model.AContextMessage

interface TaskStateRepository {
    /**
     * Обновляет TaskState через rag-server /task-state/update (Ollama).
     * @param agentId для хранения состояния в AgentMemoryRepository
     * @param lastMessages последние 4 сообщения из истории (user + assistant)
     * @param serverUrl базовый URL rag-server
     * @return обновлённый TaskState + резюме последнего ответа (Variant B: Short History)
     */
    suspend fun update(
        agentId: Long,
        lastMessages: List<AContextMessage>,
        serverUrl: String,
    ): TaskStateUpdateResult

    /**
     * Читает текущий TaskState из памяти агента.
     */
    suspend fun getCurrent(agentId: Long): TaskState
}

data class TaskStateUpdateResult(
    val updatedState: TaskState,
    val lastResponseSummary: String,
)
```

---

#### Б.3. TaskStateRepositoryImpl

**Файл:** `app/.../memory/data/repository/TaskStateRepositoryImpl.kt`

```kotlin
class TaskStateRepositoryImpl @Inject constructor(
    private val httpClient: HttpClient,
    private val agentMemoryRepository: AgentMemoryRepository,
    private val json: Json,
) : TaskStateRepository {

    companion object {
        const val CATEGORY = "task_state"
        const val KEY = "current"
    }

    override suspend fun update(
        agentId: Long,
        lastMessages: List<AContextMessage>,
        serverUrl: String,
    ): TaskStateUpdateResult {
        val currentJson = agentMemoryRepository.getFact(agentId, KEY, CATEGORY)?.fact ?: "{}"

        val request = TaskStateUpdateRequestDto(
            currentState = currentJson,
            lastMessages = lastMessages.map { MessageDto(role = it.role.name.lowercase(), content = it.content) }
        )

        val response = runCatching {
            httpClient.post("${serverUrl.trimEnd('/')}/task-state/update") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<TaskStateUpdateResponseDto>()
        }.getOrElse { e ->
            println("[TaskStateRepositoryImpl] WARNING: Failed to update TaskState: ${e.message}")
            return TaskStateUpdateResult(getCurrent(agentId), "")
        }

        // Сохранить обновлённый state в память агента
        agentMemoryRepository.upsertFact(agentId, KEY, CATEGORY, response.updatedState)

        val updatedState = runCatching {
            json.decodeFromString<TaskState>(response.updatedState)
        }.getOrDefault(TaskState.EMPTY)

        return TaskStateUpdateResult(
            updatedState = updatedState,
            lastResponseSummary = response.lastResponseSummary,
        )
    }

    override suspend fun getCurrent(agentId: Long): TaskState {
        val raw = agentMemoryRepository.getFact(agentId, KEY, CATEGORY)?.fact ?: return TaskState.EMPTY
        return runCatching { json.decodeFromString<TaskState>(raw) }.getOrDefault(TaskState.EMPTY)
    }

    @Serializable
    private data class TaskStateUpdateRequestDto(
        val currentState: String,
        val lastMessages: List<MessageDto>,
    )

    @Serializable
    private data class MessageDto(val role: String, val content: String)

    @Serializable
    private data class TaskStateUpdateResponseDto(
        val updatedState: String,
        val lastResponseSummary: String,
    )
}
```

---

#### Б.4. DI биндинг

В `MemoryCoreFeatureModule` (или в соответствующем data-модуле) добавить:
```kotlin
@Binds
fun bindTaskStateRepository(impl: TaskStateRepositoryImpl): TaskStateRepository
```

---

## Что проверить после реализации

1. Запустить rag-server, вызвать `POST /task-state/update` через curl:
```bash
curl -X POST http://localhost:3001/task-state/update \
  -H "Content-Type: application/json" \
  -d '{"currentState": "{}", "lastMessages": [{"role": "user", "content": "Расскажи про AuthService"}]}'
```
Ожидаемый ответ: JSON с `updatedState` и `lastResponseSummary`.

2. Написать в RAG-чате сообщение — в следующем этапе будет видна интеграция.

---

## Риски и нюансы

- `config.translateLlmModel` используется для TaskState LLM. Если модель не подходит — можно добавить отдельный env `TASK_STATE_LLM_MODEL`.
- Regex-парсинг ответа LLM нестабилен если модель возвращает нестандартный формат. Решение: дополнительная инструкция в промпте + fallback возвращает пустой state.
- `Json` в `TaskStateRepositoryImpl` нужно настроить с `ignoreUnknownKeys = true` — модель может возвращать дополнительные поля.
