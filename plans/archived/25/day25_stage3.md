# Этап 3: RagContextMemoryProvider + Short History + DebugInfo

## Что решает этот этап
Вводит `RagContextMemoryProvider` — центральный оркестратор памяти для RAG-чата.
Он заменяет прямое использование `AutoRagMemoryProvider` в `RagWorker` и добавляет:
1. Обновление TaskState перед RAG-запросом (через `TaskStateRepository` из Этапа 2)
2. Short History — сжатая история диалога, хранится в памяти агента (Variant B: из `last_response_summary`)
3. Команда `debuginfo` в чате — выводит текущий TaskState + short history как info-сообщение

## Что получим в итоге
- `RagContextMemoryProvider` оркестрирует: TaskState → AutoRag → enriched prompt
- Short history накапливается в памяти агента (category="short_history")
- `debuginfo` в чате → info-сообщение с полным состоянием

## Зависимости
- Требует Этап 1 (RagWorker, RagTalkDelegate)
- Требует Этап 2 (TaskStateRepository, TaskStateUpdateResult)
- Этап 4 зависит от этого этапа (получает task_state + short_history из памяти агента)

---

## Пошаговый план реализации

### Шаг 1. Создать ShortHistoryRepository

Хранит сжатую историю диалога (вопрос пользователя + `last_response_summary` ассистента).
Используется в Этапе 4 для передачи в QueryOptimizer.

**Файл:** `app/.../memory/domain/provider/rag/ShortHistoryRepository.kt`

```kotlin
package com.example.day.core.core_features.memory.domain.provider.rag

interface ShortHistoryRepository {
    /** Добавить запись в short history. Хранит последние MAX_ENTRIES записей. */
    suspend fun append(agentId: Long, userMessage: String, assistantSummary: String)
    /** Получить текущую историю в виде строки "USER: ...\nASSISTANT: ..." */
    suspend fun getAsText(agentId: Long): String
    /** Получить raw список записей */
    suspend fun getEntries(agentId: Long): List<ShortHistoryEntry>
}

data class ShortHistoryEntry(
    val userMessage: String,
    val assistantSummary: String,
)
```

**Файл:** `app/.../memory/data/repository/ShortHistoryRepositoryImpl.kt`

```kotlin
class ShortHistoryRepositoryImpl @Inject constructor(
    private val agentMemoryRepository: AgentMemoryRepository,
    private val json: Json,
) : ShortHistoryRepository {

    companion object {
        const val CATEGORY = "short_history"
        const val KEY = "entries"
        const val MAX_ENTRIES = 6   // хранить 6 последних пар вопрос/ответ
    }

    override suspend fun append(agentId: Long, userMessage: String, assistantSummary: String) {
        val current = getEntries(agentId).toMutableList()
        current.add(ShortHistoryEntry(userMessage, assistantSummary))
        if (current.size > MAX_ENTRIES) {
            current.removeAt(0)
        }
        val serialized = json.encodeToString(
            ListSerializer(ShortHistoryEntryDto.serializer()),
            current.map { ShortHistoryEntryDto(it.userMessage, it.assistantSummary) }
        )
        agentMemoryRepository.upsertFact(agentId, KEY, CATEGORY, serialized)
    }

    override suspend fun getAsText(agentId: Long): String {
        return getEntries(agentId).joinToString("\n") { entry ->
            "USER: ${entry.userMessage}\nASSISTANT: ${entry.assistantSummary}"
        }
    }

    override suspend fun getEntries(agentId: Long): List<ShortHistoryEntry> {
        val raw = agentMemoryRepository.getFact(agentId, KEY, CATEGORY)?.fact
            ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<ShortHistoryEntryDto>>(raw)
                .map { ShortHistoryEntry(it.userMessage, it.assistantSummary) }
        }.getOrDefault(emptyList())
    }

    @Serializable
    private data class ShortHistoryEntryDto(
        val userMessage: String,
        val assistantSummary: String,
    )
}
```

---

### Шаг 2. Создать RagContextMemoryProvider

Центральный провайдер для RAG-чата. Реализует `MemoryProvider`.
Оркестрирует: TaskState update → Short History → AutoRag enrichment.

**Файл:** `app/.../memory/domain/provider/RagContextMemoryProvider.kt`

```kotlin
package com.example.day.core.core_features.memory.domain.provider

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.repository.AgentContextRepository
import com.example.day.core.core_features.agent.domain.repository.AgentMemoryRepository
import com.example.day.core.core_features.memory.domain.provider.base.MemoryProvider
import com.example.day.core.core_features.memory.domain.provider.rag.ShortHistoryRepository
import com.example.day.core.core_features.memory.domain.provider.rag.TaskStateRepository

/**
 * Центральный memory provider для RAG-чата.
 *
 * Оркестрирует в appendUserPrompt():
 * 1. Читает short history из памяти
 * 2. Обновляет TaskState через rag-server (Ollama)
 * 3. Вызывает AutoRagMemoryProvider для обогащения промпта RAG-контекстом
 *
 * В postProcess() (после ответа LLM):
 * 4. Сохраняет discuss_later entry в short history (userMessage + lastResponseSummary из TaskState update)
 *
 * Паттерн хранения (AgentMemoryRepository):
 * - TaskState: category="task_state", key="current"
 * - Short History: category="short_history", key="entries"
 * - RAG URL: category="rag_server_url", key="settings"
 */
class RagContextMemoryProvider(
    private val taskStateRepository: TaskStateRepository,
    private val shortHistoryRepository: ShortHistoryRepository,
    private val autoRagMemoryProvider: AutoRagMemoryProvider,
    private val agentContextRepository: AgentContextRepository,
) : MemoryProvider {

    private var agentId: Long? = null
    private var serverUrl: String = AutoRagMemoryProvider.DEFAULT_URL

    // Хранит lastResponseSummary между appendUserPrompt() и postProcess()
    private var pendingUserMessage: String = ""
    private var pendingLastResponseSummary: String = ""

    fun bindAgentId(agentId: Long, serverUrl: String = AutoRagMemoryProvider.DEFAULT_URL) {
        this.agentId = agentId
        this.serverUrl = serverUrl
        autoRagMemoryProvider.bindAgentId(agentId)
    }

    override suspend fun getMemoryContext(): List<AContextMessage> = emptyList()

    override suspend fun appendUserPrompt(prompt: AContextMessage): AContextMessage {
        val agentId = agentId ?: return prompt

        // 1. Получить последние сообщения из контекста агента (для TaskState update)
        val recentMessages = getRecentMessages(agentId)

        // 2. Обновить TaskState + получить резюме последнего ответа (Variant B)
        val taskStateResult = runCatching {
            taskStateRepository.update(
                agentId = agentId,
                lastMessages = recentMessages,
                serverUrl = serverUrl,
            )
        }.getOrNull()

        // Сохранить для postProcess()
        pendingUserMessage = prompt.content
        pendingLastResponseSummary = taskStateResult?.lastResponseSummary ?: ""

        // 3. AutoRag enrichment (обогащение промпта RAG-контекстом)
        return autoRagMemoryProvider.appendUserPrompt(prompt)
    }

    /**
     * Вызывается после успешного ответа LLM.
     * Сохраняет запись в short history.
     */
    suspend fun postProcess() {
        val agentId = agentId ?: return
        if (pendingUserMessage.isBlank()) return

        shortHistoryRepository.append(
            agentId = agentId,
            userMessage = pendingUserMessage,
            assistantSummary = pendingLastResponseSummary,
        )

        // Сброс
        pendingUserMessage = ""
        pendingLastResponseSummary = ""
    }

    /**
     * Для команды debuginfo — возвращает текущее состояние.
     */
    suspend fun getDebugInfo(): String {
        val agentId = agentId ?: return "agentId не установлен"
        val taskState = taskStateRepository.getCurrent(agentId)
        val shortHistory = shortHistoryRepository.getAsText(agentId)
        return buildString {
            appendLine("=== TaskState ===")
            appendLine("focus: ${taskState.currentFocus.file} / ${taskState.currentFocus.`class`} / ${taskState.currentFocus.method}")
            appendLine("intent: ${taskState.intent}")
            appendLine("context_switched: ${taskState.contextSwitched}")
            appendLine("tech_stack: ${taskState.techStack}")
            if (taskState.confirmedDecisions.isNotEmpty()) {
                appendLine("decisions: ${taskState.confirmedDecisions.joinToString("; ")}")
            }
            if (taskState.openQuestions.isNotEmpty()) {
                appendLine("open: ${taskState.openQuestions.joinToString("; ")}")
            }
            appendLine()
            appendLine("=== Short History (${shortHistoryRepository.getEntries(agentId).size} записей) ===")
            appendLine(shortHistory.ifBlank { "(пусто)" })
        }
    }

    private suspend fun getRecentMessages(agentId: Long): List<AContextMessage> {
        // Читаем последние 4 сообщения из контекста агента
        val contextState = agentContextRepository.getContextState(agentId)
        return when (contextState) {
            is com.example.day.core.core_features.agent.domain.model.AContextState.Summary ->
                contextState.messages.takeLast(4)
            else -> emptyList()
        }
    }
}
```

---

### Шаг 3. Создать RagContextMemoryProviderFactory

По аналогии с `TaskStateMemoryProviderFactory` из TaskWorker.

**Файл:** `app/.../memory/domain/provider/RagContextMemoryProviderFactory.kt`

```kotlin
class RagContextMemoryProviderFactory @Inject constructor(
    private val taskStateRepository: TaskStateRepository,
    private val shortHistoryRepository: ShortHistoryRepository,
    private val autoRagMemoryProvider: AutoRagMemoryProvider,
    private val agentMemoryRepository: AgentMemoryRepository,
    private val agentContextRepository: AgentContextRepository,
) {
    fun create(agentId: Long, serverUrl: String): RagContextMemoryProvider {
        return RagContextMemoryProvider(
            taskStateRepository = taskStateRepository,
            shortHistoryRepository = shortHistoryRepository,
            autoRagMemoryProvider = autoRagMemoryProvider,
            agentContextRepository = agentContextRepository,
        ).also { it.bindAgentId(agentId, serverUrl) }
    }
}
```

---

### Шаг 4. Обновить RagWorker — использовать RagContextMemoryProvider

**Файл:** `app/.../agent/domain/workers/concrete/RagWorker.kt`

Изменения по сравнению с Этапом 1:
1. Добавить `ragContextMemoryProviderFactory: RagContextMemoryProviderFactory` в конструктор
2. Заменить `memoryProviderFactory.create(listOf(MemoryType.AutoRag), agentId)` на:
   ```kotlin
   val ragContextProvider = ragContextMemoryProviderFactory.create(agentId, serverUrl)
   val memoryProvider = CompositeMemoryProvider(listOf(ragContextProvider, baseMemoryProvider))
   ```
3. После `result.fold(onSuccess = ...)` добавить:
   ```kotlin
   if (result.isSuccess) {
       ragContextProvider.postProcess()
   }
   ```
4. `serverUrl` читается из `AgentMemoryRepository` (как в `AutoRagMemoryProvider`):
   ```kotlin
   val serverUrl = agentMemoryRepository.getFact(agentId, AutoRagMemoryProvider.MEMORY_KEY, AutoRagMemoryProvider.CATEGORY_URL)?.fact
       ?: AutoRagMemoryProvider.DEFAULT_URL
   ```

---

### Шаг 5. Добавить команду debuginfo в RagTalkDelegate

**Файл:** `app/.../features/console/impl/ui/delegates/RagTalkDelegate.kt`

В `tryAddUserMessage()` перед вызовом `ragWorker.doWork()`:

```kotlin
if (inputText.trim().lowercase() == "debuginfo") {
    val info = ragWorker.getDebugInfo()  // RagWorker делегирует к ragContextProvider
    chatTools.addInfoMessage(chat.id, info)
    return
}
```

Добавить в `RagWorker`:
```kotlin
suspend fun getDebugInfo(): String {
    // ragContextProvider доступен как lateinit var или через отдельный механизм
    // Простое решение: хранить последний созданный ragContextProvider
    return currentRagContextProvider?.getDebugInfo() ?: "RAG provider не инициализирован"
}
```

Или передавать `agentId` напрямую и читать из репозиториев — на усмотрение реализации.

---

### Шаг 6. DI биндинги

В `MemoryCoreFeatureModule`:
```kotlin
@Binds
fun bindShortHistoryRepository(impl: ShortHistoryRepositoryImpl): ShortHistoryRepository
```

`RagContextMemoryProviderFactory` — injectable через `@Inject constructor`, биндинг не нужен.

---

## Что проверить после реализации

1. Написать 5-6 сообщений в RAG-чате на тему одного класса
2. Написать `debuginfo` — появилось info-сообщение с TaskState и short history
3. Проверить что `current_focus.class` заполнен правильно
4. Написать о другом классе — `context_switched: true`
5. Short history содержит резюме предыдущих ответов

---

## Риски и нюансы

- `postProcess()` вызывается только при успешном ответе. При ошибке LLM — short history не обновляется. Это корректное поведение.
- `pendingUserMessage` / `pendingLastResponseSummary` — не thread-safe. Для RAG-чата это нормально (один диалог за раз).
- `getRecentMessages()` читает из `AContextState.Summary`. Если стратегия другая — вернёт пустой список (не упадёт).
- `RagContextMemoryProvider` — это не `@Inject` класс, создаётся через фабрику. Это важно: фабрика нужна для передачи `agentId` в момент создания.
