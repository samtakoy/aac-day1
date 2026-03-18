# День 23 — Реранкинг и Фильтрация: План Реализации

## Контекст и Цель

**Продуктовая задача:** Улучшенный RAG с фильтрацией, реранкингом, query rewrite и возможностью сравнения режимов.

**Ключевые требования:**
- Query rewrite — переформулировка + перевод запроса перед поиском
- Настраиваемый pipeline (включение/выключение шагов через параметры)
- Порог отсечения нерелевантных результатов (threshold filter)
- Реранкинг после поиска (heuristic + LLM)
- Топ-K до и после фильтрации (видимые метрики)
- Сравнение качества разных стратегий в (полу)автоматическом режиме
- Автоматизированное тестирование через `@@talk(rag --gentest)`
- Отчёт в файл по каждой конфигурации pipeline

**Принципы реализации:**
- Чистый понятный код без оверинжиниринга
- Существующие наработки сохраняем, новые добавляем
- Не плодим лишних обёрток — только необходимые
- Каждый шаг pipeline — самостоятельный класс, понятный нейминг

---

## Текущее Состояние Кода (до реализации)

### RAG Server (`rag-server/src/main/kotlin/com/example/day/ragserver/`)

**Существующий pipeline в `RagServer.kt`** (жёстко закодирован):
```
QueryTranslator (опционально) → TwoStageSearchService → ContextPacker → ContextFormatter
```

**Ключевые существующие компоненты:**
| Класс | Путь | Статус |
|-------|------|--------|
| `TwoStageSearchService` | `search/TwoStageSearchService.kt` | ✅ Используется в `/search` |
| `SearchService` | `search/SearchService.kt` | ⚠️ Реализован, НЕ используется (hybrid scoring) |
| `QueryTranslator` | `search/QueryTranslator.kt` | ✅ Опционально через env `TRANSLATE_QUERIES` |
| `ContextPacker` | `search/context/ContextPacker.kt` | ✅ Используется |
| `ContextFormatter` | `search/context/ContextFormatter.kt` | ✅ Используется |
| `SearchResult` | `db/ChunkEntity.kt` | `score: Float` — только один score |
| `RagConfig` | `config/RagConfig.kt` | Только env variables, нет query params |

**`TwoStageSearchService`** — двухэтапный поиск:
- Stage 1: embedding + keyword boost по метаданным классов → top COARSE_TOP_K=5 классов
- Stage 2: embedding по чанкам внутри классов + method boost → top DRILL_DOWN_PER_CLASS=3 чанка на класс
- Fallback на стандартный поиск если нет метаданных

**`SearchService`** — гибридный поиск:
- Hybrid: 0.6 * embeddingScore + 0.4 * keywordScore
- Pure embedding (useHybrid=false)
- Принимает `strategy: String` — "structural" или "fixed"

### Android (`app/src/main/java/com/example/day/`)

**`RagCommandHandler.kt`** — обрабатывает `@@talk(rag ...)`:
- `--on` / `--off` — включение/выключение RAG
- `--state` — статус
- `--url <url>` — настройка URL сервера
- Использует `AgentRepository` + `AgentMemoryRepository`

**`AutoRagMemoryProvider.kt`** — обогащает prompt RAG-контекстом:
- Вызывает `RagSearchRepository.search(prompt, serverUrl)`
- Добавляет результат к промпту перед отправкой в LLM

**`RagSearchRepository`** / **`RagSearchRepositoryImpl`** — Ktor client к `/search` endpoint:
- `RagSearchRepositoryImpl` использует `httpClient.get("$serverUrl/search") { parameter("query", query) }`
- Нужно добавить `evaluate()` метод для POST на `/evaluate`

---

## Архитектурные Решения

### Pipeline: единый `results: List<SearchResult>`, обновляемый каждым шагом

**Отклонено:** несколько полей `retrieved`, `filtered`, `reranked` — дублирование и неясность "что передавать дальше".

**Принято:** одно поле `results`, история через `PipelineMetrics` (countAfterRetrieval, countAfterFilter, etc.).

### PipelineFactory: не нужен

Сборка pipeline через fun `buildPipeline(config)` прямо в `RagServer.kt`. Для 6 шагов отдельный Factory-класс — преждевременная абстракция.

### StageScores на SearchResult: не трогаем

`SearchResult` остаётся `(chunk, score: Float)`. История scoring — в `PipelineMetrics`, не в моделях данных.

### PipelineMetrics: immutable data class с явным copy

Все поля `val`, обновление через `ctx.copy(metrics = ctx.metrics.copy(...))`. Семантически корректно для data class.

### finalTopK: отдельный TopKStep перед упаковкой

`ContextPacker` работает через `tokenLimit`, не через topK. Явный `TopKStep(finalTopK)` перед `ContextPackingStep` — single responsibility, ContextPacker не знает о topK.

### rerankTopK: убран, используется только finalTopK

Реранкер получает **все** filtered результаты, переранжирует их, затем `TopKStep(finalTopK)` отсекает финальный список. Обрезать ДО реранка — бессмысленно (потеряем чанки которые реранкер должен поднять).

**Метрика в отчёте:** `Retrieved: 15 → After filter: 9 → After rerank: 9 → Final: 5`

### QueryTranslator → QueryOptimizer: расширенный промпт

Переименование + новый промпт для полноценного query rewrite (не только перевод). Оптимизирует запросы на любом языке — и русские (translate + rewrite), и английские (rewrite + keyword expansion).

### LlmReranker: выделенный rerankerLlmProvider в main()

В `RagServer.kt` всегда создаётся `rerankerLlmProvider` (отдельно от llmProvider для метаданных). Cheap операция — просто конфигурация HTTP клиента. Отдельный env `RERANKER_LLM_MODEL` (defaults to `LLM_MODEL`).

### TRANSLATE_QUERIES env: глобальный разрешитель

`TRANSLATE_QUERIES=true` (env) → `queryOptimizer` создаётся и доступен.
`enable_query_optimize=true` (query param) → шаг добавляется в pipeline.
Если `enable_query_optimize=true` но `queryOptimizer == null` (env=false) → шаг пропускается + println в консоль.

### RagSearchRepository.evaluate(): в основном плане (Этап 6)

Нужен уже для `--gentest`, не только для деferred `--runtest`. Добавляем `evaluate()` в интерфейс и реализацию в рамках Этапа 6.

---

## Структура Новых Файлов

### RAG Server — новые файлы:
```
pipeline/
  PipelineContext.kt       — query + results + packed + metrics
  PipelineStep.kt          — interface { val name: String; suspend fun process(ctx): ctx }
  PipelineExecutor.kt      — sequential execution with timing per step
  PipelineConfig.kt        — config + RetrievalStrategy + RerankStrategy + PipelinePreset enum
  steps/
    QueryOptimizeStep.kt   — обёртка QueryOptimizer (бывший TranslationStep)
    RetrievalStep.kt       — switch TWO_STAGE/HYBRID по config
    ThresholdFilterStep.kt — filter { score >= threshold }
    RerankStep.kt          — делегирует Reranker
    TopKStep.kt            — results.take(topK)
    ContextPackingStep.kt  — обёртка ContextPacker (без topK — только токен-лимит)
search/rerank/
  Reranker.kt              — interface
  HeuristicReranker.kt     — keyword overlap bonus
  LlmReranker.kt           — OllamaLlmProvider + regex парсинг "Chunk N: X.XX"
evaluation/
  EvaluationService.kt     — запуск вопросов через pipeline, сохранение MD-отчётов
```

### RAG Server — переименовываемые файлы:
```
search/QueryTranslator.kt  →  search/QueryOptimizer.kt  (новый промпт, тот же механизм)
```

### RAG Server — изменяемые файлы:
```
RagServer.kt               — buildPipeline(), buildReranker(), /search с params, /evaluate endpoint,
                             rerankerLlmProvider как val в main scope
config/RagConfig.kt        — добавить rerankerLlmModel: String
```

### Android — новые файлы:
```
.../memory/domain/provider/rag/TestQueries.kt
```

### Android — изменяемые файлы:
```
.../memory/domain/provider/rag/RagSearchRepository.kt      — добавить evaluate()
.../memory/data/repository/RagSearchRepositoryImpl.kt      — реализовать evaluate()
.../workers/innercommand/handler/RagCommandHandler.kt      — добавить --gentest
```

---

## Этап 1: Pipeline Core

**Файлы:** `pipeline/PipelineContext.kt`, `pipeline/PipelineStep.kt`, `pipeline/PipelineExecutor.kt`, `pipeline/PipelineConfig.kt`

### PipelineContext.kt
```kotlin
data class PipelineContext(
    val originalQuery: String,
    val query: String = originalQuery,    // обновляется QueryOptimizeStep
    val results: List<SearchResult> = emptyList(),
    val packed: PackedContext? = null,    // заполняется ContextPackingStep
    val metrics: PipelineMetrics = PipelineMetrics(),
)

data class PipelineMetrics(
    val timings: Map<String, Long> = emptyMap(),       // step name → ms
    val countAfterRetrieval: Int = 0,
    val countAfterFilter: Int = 0,
    val countAfterRerank: Int = 0,
)
```

### PipelineStep.kt
```kotlin
interface PipelineStep {
    val name: String
    suspend fun process(ctx: PipelineContext): PipelineContext
}
```

### PipelineExecutor.kt
```kotlin
class PipelineExecutor(private val steps: List<PipelineStep>) {
    suspend fun execute(query: String): PipelineContext {
        var ctx = PipelineContext(originalQuery = query)
        for (step in steps) {
            val t = System.currentTimeMillis()
            ctx = step.process(ctx)
            val elapsed = System.currentTimeMillis() - t
            ctx = ctx.copy(metrics = ctx.metrics.copy(
                timings = ctx.metrics.timings + (step.name to elapsed)
            ))
        }
        return ctx
    }
}
```

### PipelineConfig.kt
```kotlin
data class PipelineConfig(
    val enableQueryOptimize: Boolean = false,           // query rewrite + translate
    val retrievalStrategy: RetrievalStrategy = RetrievalStrategy.TWO_STAGE,
    val chunkingStrategy: String = "structural",        // "structural"|"fixed", только для HYBRID
    val retrievalTopK: Int = 10,                        // top-K ДО фильтрации
    val threshold: Double = 0.0,                        // 0.0 = фильтр выключен
    val rerankStrategy: RerankStrategy = RerankStrategy.NONE,
    val finalTopK: Int = 5,                             // top-K ПОСЛЕ реранка → в LLM
)

enum class RetrievalStrategy { TWO_STAGE, HYBRID }
enum class RerankStrategy { NONE, HEURISTIC, LLM }

enum class PipelinePreset(val config: PipelineConfig) {
    BASELINE(PipelineConfig(
        retrievalTopK = 10,
        finalTopK = 5,
    )),
    FILTERED(PipelineConfig(
        retrievalTopK = 15,
        threshold = 0.65,
        finalTopK = 5,
        // Retrieved: 15 → After filter: N → Final: 5
    )),
    RERANKED_HEURISTIC(PipelineConfig(
        retrievalTopK = 15,
        threshold = 0.5,
        rerankStrategy = RerankStrategy.HEURISTIC,
        finalTopK = 5,
        // реранкер получает все после фильтра, TopKStep берёт top-5
    )),
    RERANKED_LLM(PipelineConfig(
        retrievalTopK = 15,
        threshold = 0.5,
        rerankStrategy = RerankStrategy.LLM,
        finalTopK = 5,
    ));

    companion object {
        fun fromString(s: String) =
            entries.firstOrNull { it.name.equals(s, ignoreCase = true) } ?: BASELINE
        val all get() = entries.toList()
    }
}
```

---

## Этап 2: Pipeline Steps

### QueryOptimizeStep.kt (переименован из TranslationStep, новый промпт)
```kotlin
class QueryOptimizeStep(private val optimizer: QueryOptimizer) : PipelineStep {
    override val name = "query_optimize"
    override suspend fun process(ctx: PipelineContext): PipelineContext {
        val optimized = optimizer.optimize(ctx.query)
        return ctx.copy(query = optimized)
    }
}
```

**QueryOptimizer.kt** (переименован из QueryTranslator, новый промпт):
```kotlin
class QueryOptimizer(private val llmProvider: LlmProvider) {

    suspend fun optimize(query: String): String {
        val optimized = llmProvider.generate("""
            You optimize search queries for semantic search over a Kotlin codebase.

            1. Make the query self-contained and specific
            2. Translate to English if not already in English
            3. Remove filler words
            4. Add relevant technical keywords (class, function, interface, repository, etc.)

            Return ONLY the optimized query, no explanations.

            Query: $query
        """.trimIndent())
        val result = optimized.trim().lines().firstOrNull { it.isNotBlank() } ?: optimized.trim()
        println("[QueryOptimizer] '$query' → '$result'")
        return result
    }
}
```

**Примечание:** убрана проверка `isNonEnglish` — оптимизируем всегда (английские запросы тоже выигрывают от добавления технических ключевых слов).

### RetrievalStep.kt
- По `config.retrievalStrategy`:
  - `TWO_STAGE` → `TwoStageSearchService.search(ctx.query, config.retrievalTopK)`
  - `HYBRID` → `SearchService.search(ctx.query, config.chunkingStrategy, config.retrievalTopK, useHybrid=true)`
- `ctx.copy(results = results, metrics = ctx.metrics.copy(countAfterRetrieval = results.size))`

### ThresholdFilterStep.kt
```kotlin
class ThresholdFilterStep(private val threshold: Double) : PipelineStep {
    override val name = "filter"
    override suspend fun process(ctx: PipelineContext): PipelineContext {
        val filtered = ctx.results.filter { it.score >= threshold }
        return ctx.copy(
            results = filtered,
            metrics = ctx.metrics.copy(countAfterFilter = filtered.size),
        )
    }
}
```

### RerankStep.kt
```kotlin
class RerankStep(private val reranker: Reranker) : PipelineStep {
    override val name = "rerank"
    override suspend fun process(ctx: PipelineContext): PipelineContext {
        val reranked = reranker.rerank(ctx.query, ctx.results)  // получает ВСЕ filtered
        return ctx.copy(
            results = reranked,
            metrics = ctx.metrics.copy(countAfterRerank = reranked.size),
        )
    }
}
```

### TopKStep.kt
```kotlin
class TopKStep(private val topK: Int) : PipelineStep {
    override val name = "top_k"
    override suspend fun process(ctx: PipelineContext): PipelineContext =
        ctx.copy(results = ctx.results.take(topK))
}
```

### ContextPackingStep.kt
```kotlin
class ContextPackingStep(private val packer: ContextPacker) : PipelineStep {
    override val name = "pack"
    override suspend fun process(ctx: PipelineContext): PipelineContext =
        ctx.copy(packed = packer.pack(ctx.results))  // topK уже применён TopKStep
}
```

### buildPipeline() в RagServer.kt
```kotlin
private fun buildPipeline(config: PipelineConfig): PipelineExecutor = PipelineExecutor(buildList {
    if (config.enableQueryOptimize) {
        if (queryOptimizer != null) {
            add(QueryOptimizeStep(queryOptimizer))
        } else {
            println("[Pipeline] Query optimization requested but TRANSLATE_QUERIES=false — skipping QueryOptimizeStep")
        }
    }
    add(RetrievalStep(twoStageSearchService, searchService, config))
    if (config.threshold > 0.0)
        add(ThresholdFilterStep(config.threshold))
    if (config.rerankStrategy != RerankStrategy.NONE)
        add(RerankStep(buildReranker(config.rerankStrategy)))
    add(TopKStep(config.finalTopK))
    add(ContextPackingStep(ContextPacker()))
})

private fun buildReranker(strategy: RerankStrategy): Reranker = when (strategy) {
    RerankStrategy.HEURISTIC -> HeuristicReranker()
    RerankStrategy.LLM       -> LlmReranker(rerankerLlmProvider)   // всегда доступен
    RerankStrategy.NONE      -> error("unreachable")
}
```

**rerankerLlmProvider в main() RagServer.kt:**
```kotlin
// Всегда создаём — cheap, просто конфигурация HTTP клиента
// Отдельный env RERANKER_LLM_MODEL (defaults to LLM_MODEL)
val rerankerLlmProvider = OllamaLlmProvider(
    baseUrl = config.ollamaBaseUrl,
    model = config.rerankerLlmModel,
    httpClient = httpClient,
)
```

---

## Этап 3: Rerankers

### Reranker.kt
```kotlin
interface Reranker {
    suspend fun rerank(query: String, results: List<SearchResult>): List<SearchResult>
}
```

### HeuristicReranker.kt
- Для каждого результата: `bonus = queryWords.intersect(chunkWords).size * 0.05`
- `newScore = result.score + bonus`
- Сортировка по убыванию, возвращает все (TopKStep отсекает)

### LlmReranker.kt

Промпт (структурирован для надёжного парсинга):
```
Rate the relevance of each code chunk to the search query.
For each chunk output ONLY a line in format: "Chunk N: X.XX" (score 0.00 to 1.00)

Query: {query}

Chunk 1:
{content, max ~300 chars}

Chunk 2:
{content}

Scores:
```

Парсинг: `Regex("Chunk (\\d+): ([\\d.]+)")` — устойчив к любому лишнему тексту LLM.
Возвращает SearchResult с обновлёнными score из LLM, сортированные по убыванию.

---

## Этап 4: Обновление /search API

### Новый /search endpoint с поддержкой параметров:
```
GET /search?query=...
    &preset=baseline            // если задан — берёт preset как базу
    &retrieval_strategy=hybrid  // two_stage | hybrid (переопределяет preset)
    &chunking_strategy=fixed    // structural | fixed (только для hybrid)
    &retrieval_topK=15
    &threshold=0.65
    &rerank_strategy=heuristic  // none | heuristic | llm
    &final_topK=5
    &enable_query_optimize=true
```

**Логика:** `preset` → `PipelineConfig` → индивидуальные параметры переопределяют поверх.

**Ответ:** текстовый формат через `ContextFormatter.format(ctx.packed!!)` с debug-заголовком:
```
Pipeline: FILTERED | Retrieved: 15 → Filtered: 9 → Final: 5
Timings: query_optimize=1.2s, retrieve=0.31s, filter=0ms, top_k=0ms, pack=0ms
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
...
```

### RagConfig.kt — добавить:
```kotlin
val rerankerLlmModel: String,   // env RERANKER_LLM_MODEL, defaults to llmModel
```

---

## Этап 5: Evaluation Service + /evaluate Endpoint

### EvaluationService.kt
- Принимает `questions: List<String>` + `presets: List<PipelinePreset>`
- Для каждого пресета: прогоняет все вопросы через `buildPipeline(preset.config).execute(question)`
- Сохраняет MD-отчёт: `reports/eval_{PRESET}_{yyyy-MM-dd_HH-mm}.md`
- Возвращает `EvaluationSummary(savedReports, summaryText, items)`

`items: List<EvalItem>` нужны для будущего `--runtest`:
```kotlin
data class EvalItem(val question: String, val optimizedQuery: String?, val ragContext: String)
data class EvaluationSummary(val savedReports: List<String>, val summary: String, val items: List<EvalItem>)
```

### Формат отчёта (`reports/eval_FILTERED_2024-03-18_14-30.md`):
```markdown
# Evaluation: FILTERED | 2024-03-18 14:30
Config: retrievalTopK=15, threshold=0.65, rerank=NONE, finalTopK=5

---

## Query 1: "Как работает ContextPacker?"
**Optimized:** "How does ContextPacker work? class grouping token limit packing"
**Metrics:** Retrieved: 15 → After filter: 9 → Final: 5
**Top score:** 0.84 | Avg score: 0.71
**Timings:** query_optimize=1.2s, retrieve=0.31s, filter=0ms, top_k=0ms, pack=0ms

### RAG Context:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] ContextPacker | score: 0.841
...

---

## Summary
| Метрика | Значение |
|---------|---------|
| Всего вопросов | 10 |
| Avg top score | 0.74 |
| Avg retrieved | 15 |
| Avg after filter | 8.3 |
| Avg final | 5.0 |
| Avg total latency | 1.8s |
```

### /evaluate endpoint (POST):
```json
// Request:
{
  "questions": ["Как работает ContextPacker?", "Где хранятся эмбеддинги?"],
  "presets": ["baseline", "filtered", "reranked_heuristic", "reranked_llm"]
}

// Response:
{
  "savedReports": ["reports/eval_BASELINE_2024-03-18_14-30.md", ...],
  "summary": "4 пресета × 10 вопросов\nBASELINE avg: 0.71 | FILTERED avg: 0.68 | ..."
}
```

Поле `presets` принимает `"all"` как строку — разворачивается в `PipelinePreset.all`.

---

## Этап 6: Android

### RagSearchRepository.kt — добавить evaluate():
```kotlin
interface RagSearchRepository {
    suspend fun search(query: String, serverUrl: String): Result<String>
    suspend fun evaluate(questions: List<String>, presets: List<String>, serverUrl: String): Result<EvaluateResponse>
}

data class EvaluateResponse(
    val savedReports: List<String>,
    val summary: String,
)
```

### RagSearchRepositoryImpl.kt — реализовать:
```kotlin
override suspend fun evaluate(questions: List<String>, presets: List<String>, serverUrl: String): Result<EvaluateResponse> =
    runCatching {
        httpClient.post("${serverUrl.trimEnd('/')}/evaluate") {
            contentType(ContentType.Application.Json)
            setBody(EvaluateRequest(questions = questions, presets = presets))
        }.body<EvaluateResponse>()
    }
```

### TestQueries.kt (новый файл):
```
app/src/main/java/com/example/day/core/core_features/memory/domain/provider/rag/TestQueries.kt
```
```kotlin
object TestQueries {
    val list = listOf(
        "Как работает двухэтапный поиск?",
        "Где хранятся эмбеддинги?",
        "Как работает ContextPacker?",
        "Что делает QueryOptimizer?",
        "Как индексируются файлы?",
        "Что такое ChunkingStrategy?",
        "Как настроить RAG сервер?",
        "Где хранится база данных?",
        "Как работает keyword scoring?",
        "Что такое metadata extraction?",
        // добавить специфичные для проекта вопросы перед запуском тестов
    )
}
```

### RagCommandHandler.kt — добавить `--gentest`:
```kotlin
"gentest" in paramsMap -> handleGentest(agentConfig.id, paramsMap["gentest"])
```

**handleGentest flow:**
1. Читает URL сервера из `AgentMemoryRepository`
2. Определяет пресеты: `paramsMap["gentest"]?.split(",") ?: listOf("all")`
3. `ragSearchRepository.evaluate(TestQueries.list, presets, serverUrl)`
4. Возвращает `CommandResult.Success(response.summary + "\n\nФайлы сохранены:\n" + response.savedReports.joinToString("\n"))`

**Варианты вызова:**
- `@@talk(rag --gentest)` — все пресеты
- `@@talk(rag --gentest baseline)` — один пресет
- `@@talk(rag --gentest filtered,reranked_llm)` — несколько через запятую

---

## Как реализовано требование "Топ-K до и после фильтрации"

Это требование продуктовой задачи. В нашей архитектуре реализовано через два явных среза:

```
RetrievalStep       → top retrievalTopK=15  ← первый топ-K срез (до фильтра)
ThresholdFilterStep → filter score >= 0.65  ← убирает нерелевантных по порогу
RerankStep          → переранжирует всё что осталось
TopKStep            → top finalTopK=5       ← второй топ-K срез (после фильтра)
ContextPackingStep  → упаковывает для LLM
```

**Пример (RERANKED_LLM):**
```
Retrieve top 15 → 15 штук
Filter ≥ 0.50  → 9 штук прошли порог
LLM rerank     → те же 9, переранжированы по релевантности
TopK top 5     → финальные 5 → в LLM
```

**Видимость в ответе `/search`:**
```
Pipeline: RERANKED_LLM | Retrieved: 15 → Filtered: 9 → Final: 5
```

**В отчёте evaluation:**
```
Metrics: Retrieved: 15 → After filter: 9 → Final: 5
```

| Требование | Реализация |
|-----------|-----------|
| top-K до фильтрации | `retrievalTopK` в `PipelineConfig` → `RetrievalStep` |
| top-K после фильтрации | `finalTopK` в `PipelineConfig` → `TopKStep` |
| видимость промежуточных значений | `PipelineMetrics` (countAfterRetrieval, countAfterFilter) |

---

## Что НЕ входит в текущий scope

- `@@talk(rag --runtest)` — LLM-цикл с RAG контекстами → вынесено в `plans/runtest_deferred.md`
- Cross-Encoder (ONNX BGE-Reranker) — сложная интеграция, не реализуем
- `/evaluate` с LLM-judge (оценка качества ответов) — в deferred

---

## Порядок Реализации

```
Этап 1  → Pipeline Core (4 файла)
Этап 2  → Steps + buildPipeline() (6 файлов + RagServer.kt + RagConfig.kt)
           Включает переименование QueryTranslator → QueryOptimizer
Этап 3  → Rerankers (3 файла)
Этап 4  → Обновление /search API (RagServer.kt)
Этап 5  → EvaluationService + /evaluate (1 файл + RagServer.kt)
Этап 6  → Android: TestQueries + RagSearchRepository + RagCommandHandler (4 файла)
```

**Зависимости:** 1→2→3 строго последовательны. 4 и 5 независимы после 3. 6 зависит от 5.

---

## Файлы: Итоговый Список

### Новые (rag-server):
```
pipeline/PipelineContext.kt
pipeline/PipelineStep.kt
pipeline/PipelineExecutor.kt
pipeline/PipelineConfig.kt
pipeline/steps/QueryOptimizeStep.kt
pipeline/steps/RetrievalStep.kt
pipeline/steps/ThresholdFilterStep.kt
pipeline/steps/RerankStep.kt
pipeline/steps/TopKStep.kt
pipeline/steps/ContextPackingStep.kt
search/rerank/Reranker.kt
search/rerank/HeuristicReranker.kt
search/rerank/LlmReranker.kt
evaluation/EvaluationService.kt
```

### Переименовываемые (rag-server):
```
search/QueryTranslator.kt  →  search/QueryOptimizer.kt
```

### Изменяемые (rag-server):
```
RagServer.kt        — buildPipeline(), buildReranker(), rerankerLlmProvider,
                      /search с params, /evaluate endpoint
config/RagConfig.kt — добавить rerankerLlmModel
```

### Новые (Android):
```
.../memory/domain/provider/rag/TestQueries.kt
```

### Изменяемые (Android):
```
.../memory/domain/provider/rag/RagSearchRepository.kt      — добавить evaluate()
.../memory/data/repository/RagSearchRepositoryImpl.kt      — реализовать evaluate()
.../workers/innercommand/handler/RagCommandHandler.kt      — добавить --gentest
```
