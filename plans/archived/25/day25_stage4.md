# Этап 4: RagServer /search расширение + QueryOptimizer с TaskState

## Что решает этот этап
Расширяет цепочку RAG-поиска так, чтобы QueryOptimizer знал контекст задачи:
1. **RagServer:** `/search` уже принимает `?preset=` — нужно добавить `?task_state=` и `?history=`
2. **QueryOptimizer:** расширить промпт — использовать task_state и history для умного rewrite запроса
3. **Android:** `RagSearchRepository.search()` и `AutoRagMemoryProvider` расширяются для передачи этих параметров

После этого этапа QueryOptimizer будет формировать семантически точные поисковые запросы с учётом:
- что сейчас обсуждается (current_focus)
- какой intent у пользователя (debugging/architecture/implementation)
- переключился ли контекст (context_switched)
- краткой истории диалога

## Что получим в итоге
- `GET /search?query=...&preset=reranked_llm&task_state=...&history=...`
- QueryOptimizer формирует улучшенный поисковый запрос с учётом контекста задачи
- Качество RAG-поиска улучшается при длинных диалогах

## Зависимости
- Требует Этапа 1 (инфраструктура)
- Требует Этапа 3 (данные TaskState и Short History доступны в памяти агента)

---

## Пошаговый план реализации

### Часть А. rag-server — расширение QueryOptimizer

---

#### А.1. Расширить QueryOptimizer

**Файл:** `rag-server/.../search/QueryOptimizer.kt`

Добавить перегрузку с контекстом задачи:

```kotlin
class QueryOptimizer(private val llmProvider: LlmProvider) {

    /** Базовая оптимизация без контекста (обратная совместимость). */
    suspend fun optimize(query: String): String =
        optimizeWithContext(query, taskState = null, history = null)

    /**
     * Оптимизация с учётом контекста задачи.
     * @param taskState JSON строка TaskState (может быть null или "{}")
     * @param history краткая история диалога (пары USER/ASSISTANT, может быть null)
     */
    suspend fun optimizeWithContext(
        query: String,
        taskState: String?,
        history: String?,
    ): String {
        val hasContext = !taskState.isNullOrBlank() && taskState != "{}" ||
                        !history.isNullOrBlank()

        val prompt = if (hasContext) {
            buildContextAwarePrompt(query, taskState, history)
        } else {
            buildBasePrompt(query)
        }

        val response = llmProvider.generate(prompt)
        val result = response.trim().lines().firstOrNull { it.isNotBlank() } ?: response.trim()
        println("[QueryOptimizer] '$query' → '$result' (context: $hasContext)")
        return result
    }

    private fun buildBasePrompt(query: String): String = """
        You optimize search queries for semantic search over a Kotlin codebase.

        1. Make the query self-contained and specific
        2. Translate to English if not already in English
        3. Remove filler words
        4. Add relevant technical keywords (class, function, interface, repository, use case, etc.)

        Return ONLY the optimized query, no explanations, no quotes.

        Query: $query
    """.trimIndent()

    private fun buildContextAwarePrompt(
        query: String,
        taskState: String?,
        history: String?,
    ): String = """
        Your goal: rewrite the user's query for semantic search over a Kotlin codebase,
        using task context and conversation history for better precision.

        PRIORITY RULES:
        1. If the query explicitly names a class/file/method → use it as-is (ignore active focus)
        2. If the query is abstract ("how does it work?", "show me") → use current_focus from TaskState
        3. If context_switched=true → DO NOT use the old focus
        4. Always translate to English
        5. Always add technical keywords (class, function, interface, repository, etc.)

        Return ONLY the optimized query. No explanation, no quotes.

        ---
        TASK STATE:
        ${taskState?.takeIf { it.isNotBlank() && it != "{}" } ?: "(no context yet)"}

        ---
        RECENT HISTORY (summary):
        ${history?.takeIf { it.isNotBlank() } ?: "(no history yet)"}

        ---
        USER QUERY: $query
    """.trimIndent()
}
```

---

#### А.2. Расширить GET /search в RagServer.kt

**Файл:** `rag-server/.../RagServer.kt`

В обработчике `get("/search")` добавить чтение параметров и передачу в QueryOptimizeStep:

```kotlin
get("/search") {
    val query = call.request.queryParameters["query"]
        ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing 'query' parameter")

    // Новые параметры для QueryOptimizer с контекстом задачи
    val taskState = call.request.queryParameters["task_state"]
    val history = call.request.queryParameters["history"]

    val pipelineConfig = parsePipelineConfig(call.request)
    val pipeline = buildPipeline(pipelineConfig, taskState = taskState, history = history)
    val ctx = pipeline.execute(query)
    // ... остальное без изменений
}
```

Изменить `buildPipeline()` для передачи контекста:

```kotlin
fun buildPipeline(
    pipelineConfig: PipelineConfig,
    taskState: String? = null,
    history: String? = null,
): PipelineExecutor = PipelineExecutor(buildList {
    if (pipelineConfig.enableQueryOptimize) {
        if (queryOptimizer != null) {
            add(QueryOptimizeStep(queryOptimizer, taskState = taskState, history = history))
        } else {
            println("[Pipeline] enable_query_optimize=true but TRANSLATE_QUERIES=false — skipping")
        }
    }
    // ... остальные шаги без изменений
})
```

---

#### А.3. Расширить QueryOptimizeStep

**Файл:** `rag-server/.../pipeline/steps/QueryOptimizeStep.kt`

```kotlin
class QueryOptimizeStep(
    private val optimizer: QueryOptimizer,
    private val taskState: String? = null,
    private val history: String? = null,
) : PipelineStep {
    override suspend fun execute(ctx: PipelineContext): PipelineContext {
        val optimized = optimizer.optimizeWithContext(
            query = ctx.currentQuery,
            taskState = taskState,
            history = history,
        )
        return ctx.copy(
            currentQuery = optimized,
            metrics = ctx.metrics.copy(optimizedQuery = optimized)
        )
    }
}
```

---

### Часть Б. Android — передача TaskState и history в /search

---

#### Б.1. Расширить RagSearchRepository

**Файл:** `app/.../memory/domain/provider/rag/RagSearchRepository.kt`

Добавить параметры в `search()`:

```kotlin
interface RagSearchRepository {
    /**
     * @param taskStateJson JSON строка TaskState (опционально, для QueryOptimizer)
     * @param shortHistory сжатая история диалога (опционально, для QueryOptimizer)
     * @param preset pipeline preset, по умолчанию "reranked_llm"
     */
    suspend fun search(
        query: String,
        serverUrl: String,
        taskStateJson: String? = null,
        shortHistory: String? = null,
        preset: String = "reranked_llm",
    ): Result<String>
    // ... остальные методы без изменений
}
```

**Файл:** `app/.../memory/data/repository/RagSearchRepositoryImpl.kt`

```kotlin
override suspend fun search(
    query: String,
    serverUrl: String,
    taskStateJson: String?,
    shortHistory: String?,
    preset: String,
): Result<String> = runCatching {
    val response = httpClient.get("${serverUrl.trimEnd('/')}/search") {
        parameter("query", query)
        parameter("preset", preset)
        if (!taskStateJson.isNullOrBlank() && taskStateJson != "{}") {
            parameter("task_state", taskStateJson)
        }
        if (!shortHistory.isNullOrBlank()) {
            parameter("history", shortHistory)
        }
    }
    response.bodyAsText()
}
```

---

#### Б.2. Обновить AutoRagMemoryProvider

**Файл:** `app/.../memory/domain/provider/AutoRagMemoryProvider.kt`

Добавить возможность передачи контекстных параметров. Чтобы не ломать существующий код — добавить setter:

```kotlin
class AutoRagMemoryProvider @Inject constructor(
    private val agentMemoryRepository: AgentMemoryRepository,
    private val ragSearchRepository: RagSearchRepository
) : MemoryProvider {

    private var agentId: Long? = null
    private var taskStateJson: String? = null
    private var shortHistory: String? = null

    fun bindAgentId(agentId: Long) {
        this.agentId = agentId
    }

    /** Устанавливается из RagContextMemoryProvider перед вызовом appendUserPrompt() */
    fun setContext(taskStateJson: String?, shortHistory: String?) {
        this.taskStateJson = taskStateJson
        this.shortHistory = shortHistory
    }

    override suspend fun appendUserPrompt(prompt: AContextMessage): AContextMessage {
        val agentId = agentId ?: return prompt

        val serverUrl = agentMemoryRepository
            .getFact(agentId, MEMORY_KEY, CATEGORY_URL)?.fact
            ?: DEFAULT_URL

        val ragResult = ragSearchRepository.search(
            query = prompt.content,
            serverUrl = serverUrl,
            taskStateJson = taskStateJson,
            shortHistory = shortHistory,
            preset = "reranked_llm",
        ).getOrElse { return prompt }

        if (ragResult.isBlank()) return prompt

        return prompt.copy(
            content = buildString {
                append(prompt.content)
                append("\n\nКонтекстная информация по запросу:\n")
                append(ragResult)
            }
        )
    }
    // ... companion object без изменений
}
```

---

#### Б.3. Обновить RagContextMemoryProvider — передавать контекст в AutoRag

**Файл:** `app/.../memory/domain/provider/RagContextMemoryProvider.kt`

В `appendUserPrompt()` между шагом "обновить TaskState" и "AutoRag enrichment":

```kotlin
// Передать контекст в AutoRagMemoryProvider
val taskStateJson = runCatching {
    taskStateRepository.getCurrent(agentId)
}.getOrNull()?.let { json.encodeToString(TaskState.serializer(), it) }

val historyText = shortHistoryRepository.getAsText(agentId)

autoRagMemoryProvider.setContext(
    taskStateJson = taskStateJson,
    shortHistory = historyText.takeIf { it.isNotBlank() },
)

// AutoRag enrichment
return autoRagMemoryProvider.appendUserPrompt(prompt)
```

---

## Что проверить после реализации

1. Написать абстрактный запрос в RAG-чате ("как это работает?") после нескольких сообщений
2. В rag-server логах должно быть видно: `[QueryOptimizer] 'как это работает?' → 'AuthService login method implementation Kotlin'`
3. Проверить debug-header в ответе RAG: `Query: "..." → "..."` строчка изменилась
4. Проверить что `?preset=reranked_llm` передаётся (в логах сервера должно быть `LLM` в строке Pipeline)

---

## Риски и нюансы

- `task_state` передаётся как URL query parameter — если JSON длинный (>500 символов), могут быть проблемы с длиной URL. Решение: передавать только ключевые поля (`current_focus`, `intent`, `context_switched`), не весь JSON.
- `history` тоже может быть длинным — передавать максимум 3 последних записи.
- Изменение сигнатуры `RagSearchRepository.search()` нарушает обратную совместимость. Все существующие вызовы (в `RagCommandHandler`) нужно обновить — добавить параметры по умолчанию или обновить вызовы явно. Параметры со значениями по умолчанию позволяют не трогать старые вызовы.
- `buildPipeline()` в RagServer.kt сейчас вызывается из нескольких мест (search, evaluate). Добавить `taskState` и `history` только в путь `/search`. Для `/evaluate` — оставить старую сигнатуру без контекста.
