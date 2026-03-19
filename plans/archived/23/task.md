# System Prompt

Ты Kotlin Senior Developer с многолетним опытом разработки и построения больших, но понятных и расширяемых систем; фанат Clean Architecture, SOLID, Design Patterns и best coding practices; любитель построения гибких настраиваемых агентных систем. Ты умеешь решать сложные комплексные проблемы с помощью очень простых, понятных и поддерживаемых подходов.

**Принцип:** Пишем чистые понятные решения без оверинжиниринга.

---

# Продуктовая Постановка Задачи

## 🔥 День 23. Реранкинг и Фильтрация


Добавьте второй этап после поиска:

👉 reranker или фильтр релевантности (порог similarity / отдельная модель / heuristic)

Настройте:

👉 порог отсечения нерелевантных результатов
👉 топ-K до и после фильтрации

Сравните:

👉 качество без фильтра/rewriting
👉 качество с фильтром

Результат:

Улучшенный RAG: фильтрация/реранкинг + query rewrite + сравнение режимов


---

# Часть 1. Теоретические Основы RAG Pipeline

## 1.1. Порог Отсечения (Similarity Threshold)

**Назначение:** Отсекаем «шум», который векторная база выдает просто потому, что обязана вернуть Top-K.

**Best Practice:**
- Используй нормализованное косинусное сходство
- Порог обычно ставится в районе **0.6 – 0.7**

**Пример:**
```kotlin
val filtered = retrieved.filter { it.score >= threshold }
```

---

## 1.2. Реранкинг (Cross-Encoder)

**Проблема:** Би-энкодеры (эмбеддинги) хороши для поиска по миллионам документов, но плохо понимают тонкий контекст.

**Решение:** Reranker (Cross-Encoder) видит запрос и документ одновременно и дает честную оценку релевантности.

### Варианты Реализации

| Вариант | Описание | Плюсы | Минусы |
|---------|----------|-------|--------|
| **Cohere Rerank API** | Облачный API | Высокое качество | Платный, latency |
| **BGE-Reranker (ONNX)** | Локальная модель | Быстро, бесплатно | Требует интеграции |
| **LLM Prompt** | Промпт для мини-LLM | Гибко | Дорого, медленно |

### LLM-based Reranking (Пример Промпта)

```
Ты система ранжирования.

Оцени релевантность каждого документа запросу.
Верни JSON массив чисел от 0 до 1.

Запрос:
{query}

Документы:
1. {chunk1}
2. {chunk2}
...

Ответ:
[0.9, 0.2, 0.7]
```

> ⚠️ **Важно:** Рассмотреть специализированные реранкеры (BGE-Reranker). Использование таких — большой плюс!

---

## 1.3. Query Rewrite (Query Translation)

**Проблема:** Если запрос пользователя на русском, а код на английском — поиск работает хуже.

**Текущее Решение:** Перевод запроса на английский перед поиском.

### Текущая Реализация (QueryTranslator.kt)

```kotlin
class QueryTranslator(private val llmProvider: LlmProvider) {
    
    // Эвристика: если >30% букв — кириллица, считаем запрос русским
    fun isNonEnglish(text: String): Boolean {
        val letters = text.filter { it.isLetter() }
        if (letters.isEmpty()) return false
        val cyrillicCount = letters.count { it in '\u0400'..'\u04FF' }
        return cyrillicCount.toDouble() / letters.length > 0.3
    }
    
    suspend fun translateIfNeeded(query: String): String {
        if (!isNonEnglish(query)) return query
        
        return llmProvider.generate(
            "Translate the following search query to English. " +
            "Return ONLY the translated query, no explanations, no quotes.\n\nQuery: $query"
        ).trim()
    }
}
```

### Рекомендуемое Улучшение: Query Rewrite + Translation

**Вопрос:** Расширить промпт для одновременного перевода + переформулировки?

```
Ты оптимизируешь запрос для semantic search по кодовой базе.

1. Сделай запрос самодостаточным (учти контекст диалога если есть)
2. Переведи на английский (если запрос не на английском)
3. Убери мусорные слова
4. Добавь технические ключевые слова (class, function, interface...)

Верни только итоговый поисковый запрос.
```

---

## 1.4. Heuristic Reranking (Fallback)

**Статус:** Необязательно, т.к. есть вариант с Cross-Encoder.

```kotlin
class HeuristicReranker {
    fun rerank(query: String, results: List<SearchResult>): List<SearchResult> {
        return results
            .map { result ->
                val bonus = keywordOverlap(query, result.chunk.content)
                result.copy(score = result.score + bonus.toFloat())
            }
            .sortedByDescending { it.score }
    }
    
    private fun keywordOverlap(query: String, text: String): Double {
        val qWords = query.lowercase().split(" ").toSet()
        val tWords = text.lowercase().split(" ").toSet()
        val overlap = qWords.intersect(tWords).size
        return overlap * 0.05
    }
}
```

---

## 1.5. Схема Конвейера (Pipeline)

### Текущий Pipeline (Реализован в RagServer.kt)

```
┌─────────────────┐    ┌──────────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ QueryTranslator │ -> │ TwoStageSearchService│ -> │  ContextPacker  │ -> │ ContextFormatter│
│ translate       │    │ Stage 1: Classes     │    │  group by class │    │  format output  │
│ query to English│    │ Stage 2: Chunks      │    │  token limit    │    │                 │
└─────────────────┘    └──────────────────────┘    └─────────────────┘    └─────────────────┘
```

### Детализация TwoStageSearchService

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    TwoStageSearchService.search()                           │
├─────────────────────────────────────────────────────────────────────────────┤
│ Stage 1: Поиск релевантных классов (по метаданным)                          │
│   - Embedding similarity (metadata vectors)                                 │
│   - Keyword boost (до +0.2 за точные совпадения имён)                       │
│   - Top-K = 5 классов                                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│ Stage 2: Drill-down по чанкам внутри классов                                │
│   - Embedding similarity (chunk vectors)                                    │
│   - Method boost (+0.1 для ключевых методов)                                │
│   - Top-K = 3 чанка на класс                                                │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Код Текущего Pipeline (RagServer.kt)

```kotlin
// Endpoint /search
get("/search") {
    val query = call.request.queryParameters["query"]
        ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing 'query' parameter")
    
    // Шаг 1: Query Translation (опционально)
    val searchQuery = queryTranslator?.translateIfNeeded(query) ?: query
    
    // Шаг 2: Two-Stage Retrieval
    val results = twoStageSearchService.search(searchQuery, config.searchTopK * 2)
    
    // Шаг 3: Context Packing
    val packed = ContextPacker().pack(results)
    
    // Шаг 4: Context Formatting
    call.respondText(ContextFormatter.format(packed))
}
```

### Детализация TwoStageSearchService

```kotlin
class TwoStageSearchService(
    private val db: CodeDatabase,
    private val embeddingProvider: EmbeddingProvider,
) {
    companion object {
        const val COARSE_TOP_K = 5           // Stage 1: топ классов
        const val DRILL_DOWN_PER_CLASS = 3   // Stage 2: топ чанков на класс
        const val KEYWORD_BOOST_MAX = 0.2    // Макс. вклад keyword в Stage 1
    }
    
    suspend fun search(query: String, topK: Int): List<SearchResult> {
        val allMetadata = db.getAllClassMetadata()
        val queryVector = embeddingProvider.embed(query)
        
        // Stage 1: Поиск релевантных классов
        val relevantClasses = findRelevantClasses(query, queryVector, allMetadata)
        
        // Stage 2: Drill-down по чанкам внутри классов
        val results = drillDown(queryVector, relevantClasses)
        
        return results
            .distinctBy { it.chunk.id }
            .sortedByDescending { it.score }
            .take(topK)
    }
    
    // Stage 1: Embedding + Keyword Boost
    private fun findRelevantClassesByEmbedding(
        queryVector: FloatArray,
        allMetadata: List<ClassMetadata>,
        metadataVectors: List<Pair<String, FloatArray>>,
    ): List<ClassMetadata> {
        return allMetadata
            .mapNotNull { meta ->
                val vector = metadataVectors.find { it.first == meta.className }?.second
                    ?: return@mapNotNull null
                val embScore = VectorMath.cosineSimilarity(queryVector, vector).toDouble()
                
                // Keyword boost за точные совпадения имён
                val kwBoost = computeKeywordScore(queryTokens, meta)
                    .let { minOf(it * KEYWORD_BOOST_MAX, KEYWORD_BOOST_MAX) }
                
                val finalScore = embScore + kwBoost
                
                // Минимальный порог по embedding
                if (embScore > 0.1) meta to finalScore else null
            }
            .sortedByDescending { (_, score) -> score }
            .take(COARSE_TOP_K)
            .map { (meta, _) -> meta }
    }
    
    // Stage 2: Embedding + Method Boost
    private fun drillDown(
        queryVector: FloatArray,
        relevantClasses: List<ClassMetadata>,
    ): List<SearchResult> {
        val allStructural = db.getAllVectors("structural")
        val keyMethodNames = relevantClasses
            .flatMap { it.keyMethods }
            .map { it.name.lowercase() }
            .toSet()
        
        for (classMeta in relevantClasses) {
            val classChunks = allStructural.filter { (chunk, _) ->
                chunk.fileName.removeSuffix(".kt")
                    .equals(classMeta.className, ignoreCase = true)
            }
            
            val scored = classChunks
                .map { (chunk, vector) ->
                    val embScore = VectorMath.cosineSimilarity(queryVector, vector).toDouble()
                    // Method boost для ключевых методов
                    val methodBoost = if (chunk.declarationName?.lowercase() in keyMethodNames) {
                        0.1
                    } else 0.0
                    SearchResult(chunk, (embScore + methodBoost).toFloat())
                }
                .sortedByDescending { it.score }
                .take(DRILL_DOWN_PER_CLASS)
            
            results.addAll(scored)
        }
        
        return results
    }
}
```

### Hybrid Scoring (SearchService.kt)

Для сравнения — базовый поиск (без 2 этапов):

```kotlin
class SearchService(
    private val db: CodeDatabase,
    private val embeddingProvider: EmbeddingProvider,
) {
    suspend fun search(
        query: String,
        strategy: String,      // "structural" или "fixed"
        topK: Int,
        useHybrid: Boolean = true,
    ): List<SearchResult> {
        val queryVector = embeddingProvider.embed(query)
        val allVectors = db.getAllVectors(strategy)
        
        return if (useHybrid) {
            // Hybrid: Embedding (0.6) + Keyword (0.4)
            allVectors
                .map { (chunk, vector) ->
                    ScoredChunk(
                        chunk = chunk,
                        embeddingScore = VectorMath.cosineSimilarity(queryVector, vector),
                        keywordScore = KeywordScorer.score(query, chunk.content),
                    )
                }
                .sortedByDescending { it.finalScore }  // 0.6*emb + 0.4*kw
                .take(topK)
                .map { SearchResult(it.chunk, it.finalScore.toFloat()) }
        } else {
            // Pure embedding search
            allVectors
                .map { (chunk, vector) ->
                    SearchResult(chunk, VectorMath.cosineSimilarity(queryVector, vector))
                }
                .sortedByDescending { it.score }
                .take(topK)
        }
    }
}

data class ScoredChunk(
    val chunk: ChunkEntity,
    val embeddingScore: Double,
    val keywordScore: Double,
) {
    val finalScore: Double
        get() = 0.6 * embeddingScore + 0.4 * keywordScore
}
```

### Context Packing (ContextPacker.kt)

```kotlin
class ContextPacker(private val tokenLimit: Int = 6000) {
    
    fun pack(results: List<SearchResult>): PackedContext {
        // Группировка по классам
        val byClass = results.groupBy { resolveClassName(it.chunk) }
        
        // Сортировка групп по максимальному score
        val sortedGroups = byClass.entries
            .sortedByDescending { (_, chunks) -> chunks.maxOf { it.score } }
        
        val groups = mutableListOf<ClassGroup>()
        var usedTokens = 0
        
        for ((className, chunks) in sortedGroups) {
            if (usedTokens >= tokenLimit) break
            
            val uniqueChunks = chunks
                .map { it.chunk }
                .distinctBy { it.content.trim().hashCode() }
                .sortedBy { it.startLine }
            
            val groupTokens = uniqueChunks.sumOf { estimateTokens(it.content) }
            
            groups.add(
                ClassGroup(
                    className = className,
                    filePath = uniqueChunks.first().filePath,
                    chunks = uniqueChunks,
                    topScore = chunks.maxOf { it.score },
                )
            )
            usedTokens += groupTokens
        }
        
        return PackedContext(groups = groups, totalTokens = usedTokens)
    }
    
    private fun estimateTokens(text: String): Int = text.length / 4
}

data class PackedContext(
    val groups: List<ClassGroup>,
    val totalTokens: Int,
)

data class ClassGroup(
    val className: String,
    val filePath: String,
    val chunks: List<ChunkEntity>,
    val topScore: Float,
    val responsibility: String? = null,  // Из метаданных класса
)
```

---

## 1.6. Метрики Сравнения

Чтобы понять, стало ли лучше, используй метрики или подход **"Golden Dataset"**:

| Метрика | Описание |
|---------|----------|
| **Hit Rate@K** | Оказался ли правильный документ в Top-K? |
| **MRR (Mean Reciprocal Rank)** | Насколько высоко в списке находится правильный ответ |
| **LLM-as-a-Judge** | Дать другой (более мощной) LLM оценить два ответа и выбрать лучший |

---

# Часть 2. Требования к Реализации

## 2.1. Анализ Текущего Кода

**Необходимо:**
1. Рассмотреть текущую реализацию RAG
2. Выстроить работу в понятный pipeline
3. Обеспечить возможность отключения отдельных шагов pipeline

---

## 2.2. RAG Server API

### Фактический Baseline (Текущая Реализация)

**`/search` endpoint = TwoStageSearchService + ContextPacker + ContextFormatter**

```kotlin
// RagServer.kt — текущая реализация (baseline)
get("/search") {
    val query = call.request.queryParameters["query"]
    
    // Шаг 1: Query Translation (опционально, через env variable)
    val searchQuery = queryTranslator?.translateIfNeeded(query) ?: query
    
    // Шаг 2: Two-Stage Retrieval (hardcoded)
    val results = twoStageSearchService.search(searchQuery, config.searchTopK * 2)
    
    // Шаг 3: Context Packing (hardcoded)
    val packed = ContextPacker().pack(results)
    
    // Шаг 4: Context Formatting (hardcoded)
    call.respondText(ContextFormatter.format(packed))
}
```

**Характеристики Baseline:**
- ✅ **Retrieval:** TwoStageSearchService (Stage 1: классы → Stage 2: чанки)
- ✅ **Scoring:** Embedding similarity + Method Boost (+0.1 для ключевых методов)
- ❌ **Filter:** Отсутствует (нет порога отсечения)
- ❌ **Rerank:** Отсутствует (только method boost)
- ✅ **Pack:** ContextPacker (группировка по классам, token limit 6000)

**Текущие параметры (через environment variables):**

| Параметр | Environment Variable | Значение по умолчанию |
|----------|---------------------|----------------------|
| `searchTopK` | `SEARCH_TOP_K` | 5 |
| `translateQueries` | `TRANSLATE_QUERIES` | false |
| `extractMetadata` | `EXTRACT_METADATA` | false |

---

### Требования к Расширению API

**Цель:** Добавить query params для управления pipeline через API.

| Query Param | Тип | Значение по умолчанию | Описание |
|-------------|-----|----------------------|----------|
| `retrieval_strategy` | String | `two_stage` | `two_stage` или `hybrid` |
| `retrieval_topK` | Int | 10 | Топ-K кандидатов на этапе retrieval |
| `threshold` | Double | 0.0 | Порог отсечения для фильтрации (0.0 = отключен) |
| `enable_rerank` | Boolean | false | Включить реранкинг |
| `rerank_strategy` | String | `none` | `none`, `heuristic`, `llm`, `cross_encoder` |
| `rerank_topK` | Int | 5 | Топ-K после реранкинга |
| `final_topK` | Int | 5 | Финальный топ-K результатов |

---

## 2.3. Дополнительные Фичи (Автоматизированное Тестирование)

### Генерация Тестов: `@@talk(rag --gentest)`

**Поток:**
1. Пользователь составляет 10–15 вопросов
2. Указывает, с какими стратегиями проверить
3. Пишет в Android приложении команду: `@@talk(rag --gentest)`
4. Вызывается тестовый endpoint у `rag-server`
5. `RagServer` в цикле выполняет все запросы
6. Формирует на каждый запрос "настоящий ответ" — отправляет список ответов
7. Сохраняет в текстовый файл детальное описание:
   - На какой запрос какой был ответ
   - Как обрабатывался (какие этапы pipeline применялись)

### Запуск Тестов: `@@talk(rag --runtest)`

**Поток:**
1. Команда последовательно отправляет очередной запрос пользователя с подмешанным context
2. Ждет ответ
3. Отправляет следующий запрос с RAG
4. Продолжает, пока список запросов не станет пустым

**Важно:**
- ⚠️ При ошибке — остановка (не спамим LLM)
- ⚠️ Чистое понятное решение (не костыли)
- ⚠️ Не усложняем текущую архитектуру
- ✅ Активно предлагаем улучшения архитектуры для добавления новых фич

### Метрики для Отчета (Предложение)

| Метрика | Описание |
|---------|----------|
| **Hit@K** | Попадание ожидаемого документа в Top-K |
| **MRR** | Средний обратный ранг первого релевантного |
| **Avg Score** | Средний score финальных результатов |
| **Latency per Stage** | Время выполнения каждого этапа (translate, retrieve, filter, rerank) |
| **Precision@K** | Доля релевантных документов в Top-K |

---

## 2.4. Этапы Решения

```
1️⃣  Привести в порядок текущее решение
    └─ Выстроен ли четкий pipeline?
    └─ Четкая граница каждого улучшения?
    └─ Одна точка подключения к pipeline?

2️⃣  Обсуждение с пользователем и согласование

3️⃣  Реализация Этапа 1

...

N️⃣  Автоматизированная проверка (после продуктовой задачи)
```

---

# Строгий Пайплайн Разработки

```
 1) Анализ задачи (при необходимости собрать консилиум агентов: Архитектор → Senior Kotlin Developer → Agent Systems Expert)
 ↓
 2) Анализ кода и оценка применимости текущей архитектуры
 ↓
 3) Диалог с пользователем (активно предлагать лучшие решения)
 ↓
 4) Составление плана, разбиение на самодостаточные этапы
 ↓
 5) Проверка плана пользователем → возврат к шагу 3 при необходимости
 ↓
 6) Утверждение плана пользователем и реализация этапа
 ↓
 7) Отметка прогресса + документация
 ↓
 8) Переход к шагу 1 для следующего этапа
 ↓
 9) Критический обзор: что упущено, что хорошо, что можно лучше
 ↓
10) Финальная документация
```

---

# Приоритеты при Решении

1. **Результат** должен выполнять все требования продуктовой задачи
2. **Архитектура** должна оставаться простой, понятной, расширяемой

---

# Часть 3. Архитектурная Критика и Рекомендации

## 3.1. Главная Проблема Текущего Подхода

**Текущее состояние:**
- ✅ Куски логики есть (QueryTranslator, TwoStageSearchService, SearchService, ContextPacker)
- ✅ Пример pipeline есть
- ✅ Идеи тестирования есть

**❌ Критические Недостающие Элементы:**

| Проблема | Риск |
|----------|------|
| Нет системной композиции | Логика "размазана" по RagServer.kt |
| Нет feature toggles на уровне архитектуры | Невозможно A/B тестирование |
| Нет единой модели данных между этапами | Сложно передавать контекст |
| Нет явного контракта pipeline | Невозможно динамически собирать pipeline |

---

## 3.2. Ключевая Архитектурная Идея (Must-Have)

**Pipeline как First-Class Citizen**, а не просто жестко закодированная последовательность.

### Правильный Уровень Абстракции

**❌ НЕПРАВИЛЬНО:**
```kotlin
// Жесткая последовательность в RagServer.kt
val searchQuery = queryTranslator?.translateIfNeeded(query) ?: query
val results = twoStageSearchService.search(searchQuery, config.searchTopK * 2)
val packed = ContextPacker().pack(results)
```

**✅ ПРАВИЛЬНО:**
```kotlin
interface PipelineStep {
    suspend fun process(context: PipelineContext): PipelineContext
}
```

---

## 3.3. Модель Pipeline

### 3.3.1. Общий Context (Критично!)

**Проблема:** Сейчас данные передаются параметрами → нет истории, нет метрик.

**Решение:**
```kotlin
data class PipelineContext(
    val originalQuery: String,
    val translatedQuery: String? = null,
    val retrieved: List<SearchResult> = emptyList(),
    val filtered: List<SearchResult> = emptyList(),
    val reranked: List<SearchResult> = emptyList(),
    val packed: PackedContext? = null,
    val metrics: MutableMap<String, Any> = mutableMapOf(),
)
```

**Преимущества:**
- ✅ Дебаг
- ✅ Логирование
- ✅ Explainability
- ✅ Тестирование

---

### 3.3.2. Существующие Шаги (уже реализованы)

| Шаг | Класс | Метод | Статус |
|-----|-------|-------|--------|
| **Query Translation** | `QueryTranslator` | `translateIfNeeded()` | ✅ Используется в baseline |
| **Two-Stage Retrieval** | `TwoStageSearchService` | `search()` | ✅ Используется в baseline |
| **Hybrid Scoring** | `SearchService` | `search()` | ⚠️ Реализован, но **не используется** в baseline |
| **Context Packing** | `ContextPacker` | `pack()` | ✅ Используется в baseline |
| **Context Formatting** | `ContextFormatter` | `format()` | ✅ Используется в baseline |

---

#### Hybrid Scoring (Альтернативный Retrieval)

`SearchService` с hybrid scoring реализован, но **не используется** в текущем `/search` endpoint.

**Назначение:** Простой retrieval без 2-этапной оптимизации.

**Формула Scoring:**
```kotlin
data class ScoredChunk(
    val chunk: ChunkEntity,
    val embeddingScore: Double,
    val keywordScore: Double,
) {
    val finalScore: Double
        get() = 0.6 * embeddingScore + 0.4 * keywordScore
}
```

**Параметры:**
- `strategy: String` — `"structural"` или `"fixed"`
- `useHybrid: Boolean` — true (hybrid) или false (pure embedding)

**Интеграция в Pipeline:**
```kotlin
data class PipelineConfig(
    val retrievalStrategy: RetrievalStrategy = RetrievalStrategy.TWO_STAGE,
    ...
)

enum class RetrievalStrategy {
    TWO_STAGE,      // TwoStageSearchService (baseline)
    HYBRID,         // SearchService с hybrid scoring
    PURE_EMBEDDING  // SearchService с pure embedding
}
```

---

### 3.3.3. Недостающие Шаги (требуют реализации)

| Шаг | Назначение | Статус |
|-----|------------|--------|
| **Threshold Filter** | Отсечение по порогу score | ❌ Не реализован |
| **Reranker** | Cross-encoder / LLM reranking | ❌ Не реализован |
| **Heuristic Reranker** | Keyword overlap boost | ❌ Не реализован |

---

### 3.3.4. Pipeline Engine (Ядро)

```kotlin
class PipelineExecutor(
    private val steps: List<PipelineStep>
) {
    suspend fun execute(query: String): PipelineContext {
        var ctx = PipelineContext(originalQuery = query)
        
        for (step in steps) {
            ctx = step.process(ctx)
        }
        
        return ctx
    }
}
```

---

### 3.3.5. Feature Toggles (Очень Важно)

**❌ НЕПРАВИЛЬНО:** Делать `if` внутри step

**✅ ПРАВИЛЬНО:** Делать **конфигурацию pipeline**

```kotlin
data class PipelineConfig(
    val enableTranslation: Boolean = true,
    val useTwoStageSearch: Boolean = true,
    val enableFilter: Boolean = false,
    val enableRerank: Boolean = false,
    val retrievalTopK: Int = 10,
    val threshold: Double = 0.65,
    val finalTopK: Int = 5,
    val rerankStrategy: RerankStrategy = RerankStrategy.NONE,
)

enum class RerankStrategy {
    NONE,
    HEURISTIC,
    LLM,
    CROSS_ENCODER  // ONNX BGE-Reranker
}
```

### Pipeline Factory

```kotlin
class PipelineFactory(
    private val queryTranslator: QueryTranslator?,
    private val twoStageSearchService: TwoStageSearchService,
    private val searchService: SearchService,
    private val contextPacker: ContextPacker,
) {
    fun create(config: PipelineConfig): PipelineExecutor {
        val steps = mutableListOf<PipelineStep>()
        
        // Шаг 1: Query Translation (опционально)
        if (config.enableTranslation && queryTranslator != null) {
            steps += TranslationStep(queryTranslator)
        }
        
        // Шаг 2: Retrieval (обязательно)
        val retriever = if (config.useTwoStageSearch) {
            twoStageSearchService
        } else {
            searchService
        }
        steps += RetrieveStep(retriever, config.retrievalTopK)
        
        // Шаг 3: Threshold Filter (опционально)
        if (config.enableFilter) {
            steps += ThresholdFilterStep(config.threshold)
        }
        
        // Шаг 4: Rerank (опционально)
        if (config.enableRerank && config.rerankStrategy != RerankStrategy.NONE) {
            steps += RerankStep(config.rerankStrategy)
        }
        
        // Шаг 5: Final Top-K (обязательно)
        steps += TopKStep(config.finalTopK)
        
        // Шаг 6: Context Packing (обязательно)
        steps += ContextPackingStep(contextPacker)
        
        return PipelineExecutor(steps)
    }
}
```

---

## 3.4. API (RAG Server)

### Текущий Endpoint: `/search`

**Текущая реализация:**
```kotlin
get("/search") {
    val query = call.request.queryParameters["query"]
        ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing 'query' parameter")
    val searchQuery = queryTranslator?.translateIfNeeded(query) ?: query
    val results = twoStageSearchService.search(searchQuery, config.searchTopK * 2)
    val packed = ContextPacker().pack(results)
    call.respondText(ContextFormatter.format(packed))
}
```

### Рекомендуемый API с Параметрами

```
GET /search?query=...
    &enable_translation=true
    &use_two_stage=true
    &retrieval_topK=10
    &enable_filter=false
    &threshold=0.65
    &enable_rerank=false
    &rerank_strategy=NONE
    &final_topK=5
```

**Маппинг:** Query params → `PipelineConfig` → `PipelineFactory.create()`

---

## 3.5. Серьёзные Замечания

### 3.5.1. Heuristic Reranker

| Аспект | Рекомендация |
|--------|--------------|
| Использование | Только как fallback |
| Изоляция | НЕ смешивать с ML reranker в одном шаге |

**Правильная иерархия:**
```kotlin
interface Reranker {
    suspend fun rerank(query: String, results: List<SearchResult>): List<SearchResult>
}

// Реализации:
class LlmReranker(private val llmProvider: LlmProvider) : Reranker
class CrossEncoderReranker(private val modelPath: String) : Reranker
class HeuristicReranker : Reranker
```

---

### 3.5.2. QueryTranslator

**Текущий промпт:**
```
Translate the following search query to English.
Return ONLY the translated query, no explanations, no quotes.

Query: $query
```

**Рекомендуемый промпт (Query Rewrite + Translation):**
```
Ты оптимизируешь запрос для semantic search по кодовой базе.

1. Сделай запрос самодостаточным (учти контекст диалога если есть)
2. Переведи на английский (если запрос не на английском)
3. Убери мусорные слова
4. Добавь технические ключевые слова (class, function, interface...)

Верни только итоговый поисковый запрос.
```

---

### 3.5.3. Нет Observability

**Добавить в PipelineContext:**
```kotlin
data class PipelineContext(
    ...
    val metrics: MutableMap<String, Any> = mutableMapOf(
        "timings" to mutableMapOf<String, Long>(),
        "scores_before_filter" to emptyList<Float>(),
        "scores_after_rerank" to emptyList<Float>()
    )
)
```

---

## 3.6. Тестирование

### Минимальный Набор Метрик

| Метрика | Описание |
|---------|----------|
| **Hit@K** | Попадание в Top-K |
| **MRR** | Mean Reciprocal Rank |
| **Avg Score** | Средний score |
| **Latency per Stage** | Время на этап |

### Продвинутый Уровень

```kotlin
data class EvaluationResult(
    val query: String,
    val baselineAnswer: String,
    val improvedAnswer: String,
    val winner: String,         // LLM judge
    val scores: Map<String, Any>
)
```

---

## 3.7. Дополнительные Критические Замечания

### Проблема: Жесткая Структура Pipeline

**Критика:** В `RagServer.kt` pipeline жестко закодирован:

```kotlin
val searchQuery = queryTranslator?.translateIfNeeded(query) ?: query
val results = twoStageSearchService.search(searchQuery, config.searchTopK * 2)
val packed = ContextPacker().pack(results)
```

**Проблема:** Невозможно отключить translation или filter без изменения кода.

**Решение:** Использовать паттерн **Chain of Responsibility** с динамической сборкой.

---

### Проблема: LLM как Реранкер

**Критика промпта из задания:**

| Аспект | Проблема |
|--------|----------|
| **Ненадежность парсинга** | LLM может вернуть текст вокруг JSON, сломав парсер |
| **Стоимость и Latency** | Прогон 20 документов через LLM — дорого и медленно |
| **Точность** | Cross-Encoder модели (BGE-Reranker) работают быстрее и точнее |

**Рекомендация:**

1. **Приоритет:** Локальная модель через ONNX (BGE-Reranker)
2. **LLM:** Использовать только как fallback или для экспериментов
3. **Если используем LLM:** Требовать строгого формата (например, `Score: 0.9` для каждого документа)

---

### Проблема: Прозрачность (Observability)

**Критика:** В пайплайне теряется информация о том, почему документ был отфильтрован.

**Решение:** Добавить историю скоринга:

```kotlin
data class SearchResult(
    val chunk: ChunkEntity,
    val score: Float,
    val scores: StageScores? = null,
)

data class StageScores(
    val embeddingScore: Double,
    val keywordScore: Double?,
    val methodBoost: Double?,
    val filterScore: Double?,
    val rerankScore: Double?
)
```

**Важность:** Критично для отладки и сравнения качества (Day 23 требует сравнения режимов).

---

## 3.8. Итоговая Оценка

| Аспект | Текущее Состояние | Требуемый Уровень |
|--------|-------------------|-------------------|
| **Архитектура** | Жестко закодированный pipeline | Динамический pipeline |
| **Pipeline** | 4 шага (translate, retrieve, pack, format) | 6+ шагов с feature toggles |
| **Feature Toggles** | Только env variables | Query params + PipelineConfig |
| **Observability** | println() в коде | Метрики, логи, debug info |

**Вывод:** Если сделать правильно сейчас:
- ✅ Легко добавишь LLM reranker
- ✅ Легко подключишь ONNX модель
- ✅ Легко будешь A/B тестить

---

## 3.9. Вопросы для Уточнения

1. Какие retriever используешь? (pgvector? weaviate? faiss?) → **SQLite + Exposed**
2. Есть ли уже reranker или только планируется? → **Только планируется**
3. В каком виде приходит chunk? → **ChunkEntity + SearchResult**
4. Нужен ли streaming/async pipeline? → **Да, все suspend функции**

---

# Часть 4. Анализ Текущей Реализации

## 4.1. Текущая Архитектура RAG

### Расположение Кода

**RAG Server** (отдельный сервис):
```
rag-server/src/main/kotlin/com/example/day/ragserver/
├── RagServer.kt                    # Точка входа, HTTP + MCP сервер
├── config/
│   └── RagConfig.kt                # Конфигурация из env variables
├── db/
│   ├── CodeDatabase.kt             # SQLite + Exposed ORM
│   ├── ChunkEntity.kt              # Модели данных
│   ├── ClassMetadata.kt            # Методанные классов (LLM-генерация)
│   └── SearchResult.kt             # SearchResult, ScoredChunk
├── embedding/
│   ├── EmbeddingProvider.kt        # Интерфейс embedding-провайдера
│   ├── OllamaEmbeddingProvider.kt  # Ollama implementation
│   └── OpenRouterEmbeddingProvider.kt
├── indexing/
│   ├── IndexingService.kt          # Сервис индексации файлов
│   ├── ChunkingStrategy.kt         # Стратегии нарезки (Fixed, Structural)
│   ├── MetadataExtractor.kt        # LLM-извлечение метаданных
│   └── MetadataValidator.kt        # Валидация метаданных
├── search/
│   ├── SearchService.kt            # Базовый hybrid поиск
│   ├── TwoStageSearchService.kt    # 2-этапный умный поиск
│   ├── QueryTranslator.kt          # Перевод запросов на английский
│   ├── KeywordScorer.kt            # Keyword scoring для hybrid retrieval
│   ├── VectorMath.kt               # Cosine similarity
│   └── context/
│       ├── ContextPacker.kt        # Группировка результатов по классам
│       └── ContextFormatter.kt     # Форматирование вывода
└── tools/
    ├── RagTools.kt                 # MCP tool handlers
    └── RagToolNames.kt             # Названия тулов
```

**Android приложение** (клиент RAG):
```
app/src/main/java/com/example/day/core/core_features/memory/
├── domain/provider/rag/
│   └── RagSearchRepository.kt     # Интерфейс RAG-репозитория
├── domain/provider/
│   └── AutoRagMemoryProvider.kt   # Реализация AutoRAG
└── data/repository/
    └── RagSearchRepositoryImpl.kt # Ktor client для /search endpoint
```

---

## 4.2. Существующие Шаги Pipeline (Реализованы)

| Шаг | Класс | Метод | Описание |
|-----|-------|-------|----------|
| **1. Query Translation** | `QueryTranslator` | `translateIfNeeded()` | Перевод запроса на английский |
| **2. Two-Stage Retrieval** | `TwoStageSearchService` | `search()` | Stage 1: классы → Stage 2: чанки |
| **3. Hybrid Scoring** | `SearchService` | `search()` | Embedding (0.6) + Keyword (0.4) |
| **4. Method Boost** | `TwoStageSearchService` | `drillDown()` | +0.1 для ключевых методов |
| **5. Context Packing** | `ContextPacker` | `pack()` | Группировка по классам, token limit |
| **6. Context Formatting** | `ContextFormatter` | `format()` | Форматирование вывода |

---

## 4.3. Недостающие Шаги (Требуют Реализации)

| Шаг | Назначение | Статус | Приоритет |
|-----|------------|--------|-----------|
| **Threshold Filter** | Отсечение по порогу score | ❌ Не реализован | 🔴 Critical |
| **Heuristic Reranker** | Keyword overlap boost | ❌ Не реализован | 🟡 Medium |
| **LLM Reranker** | LLM-based ranking | ❌ Не реализован | 🟡 Medium |
| **Cross-Encoder Reranker** | ONNX BGE-Reranker | ❌ Не реализован | 🟢 Low |

---

## 4.4. Выявленные Проблемы

### 🔴 Критические Проблемы

| Проблема | Описание | Критичность |
|----------|----------|-------------|
| **Отсутствует явный Pipeline** | Логика "размазана" по `RagServer.kt` | 🔴 Critical |
| **Нет фильтрации по порогу** | Нет `ThresholdFilterStep` | 🔴 Critical |
| **Нет reranking** | Только `methodBoost` в `TwoStageSearchService` | 🔴 Critical |
| **Модели данных не поддерживают историю scoring** | `SearchResult` хранит только один `score: Float` | 🔴 Critical |
| **Конфигурация не поддерживает feature toggles** | `RagConfig` имеет только `translateQueries`, `extractMetadata` | 🔴 Critical |

### 🟡 Серьезные Недостатки

| Проблема | Описание | Критичность |
|----------|----------|-------------|
| **QueryTranslator не объединен с QueryRewriter** | Только перевод, без переформулирования | 🟡 Major |
| **Нет observability** | Нет логирования промежуточных этапов, метрик | 🟡 Major |
| **API endpoint не поддерживает параметры pipeline** | Только `GET /search?query=...` | 🟡 Major |
| **DI — ручная сборка в RagServer** | Зависимости создаются вручную, а не через DI | 🟡 Minor |

---

## 4.5. Что Уже Реализовано Хорошо ✅

| Компонент | Оценка | Комментарий |
|-----------|--------|-------------|
| **Chunking Strategies** | ⭐⭐⭐⭐⭐ | Fixed + Structural стратегии |
| **Hybrid Retrieval** | ⭐⭐⭐⭐ | Embedding (0.6) + Keyword (0.4) |
| **2-Stage Search** | ⭐⭐⭐⭐⭐ | Class metadata → Chunks |
| **Context Packing** | ⭐⭐⭐⭐ | Group by class + token limit |
| **Query Translation** | ⭐⭐⭐⭐ | Russian → English перевод |
| **Metadata Extraction** | ⭐⭐⭐⭐⭐ | LLM генерирует responsibility, dependencies |
| **Incremental Indexing** | ⭐⭐⭐⭐⭐ | Пропуск уже обработанных файлов |

---

# Часть 5. Возможный План Реализации

## Этап 1: Архитектурный Рефакторинг (Критично)

### 1.1. Создать Pipeline Infrastructure

| Файл | Назначение |
|------|------------|
| `pipeline/PipelineContext.kt` | Общий контекст для всех этапов |
| `pipeline/PipelineStep.kt` | Интерфейс для этапов |
| `pipeline/PipelineConfig.kt` | Конфигурация с feature toggles |
| `pipeline/PipelineExecutor.kt` | Оркестрация этапов |
| `pipeline/PipelineFactory.kt` | Динамическая сборка pipeline |

### 1.2. Обернуть Существующие Шаги в PipelineSteps

| Step | Описание |
|------|----------|
| `TranslationStep` | Обёртка вокруг `QueryTranslator.translateIfNeeded()` |
| `TwoStageRetrieveStep` | Обёртка вокруг `TwoStageSearchService.search()` |
| `HybridRetrieveStep` | Обёртка вокруг `SearchService.search()` (опционально) |  Что за обертка? Не плодим обертки! - держим код в чистоте.
| `ContextPackingStep` | Обёртка вокруг `ContextPacker.pack()` |

### 1.3. Обновить API Endpoint

**Добавить query params:**

| Параметр | Значение по умолчанию | Описание |
|----------|----------------------|----------|
| `retrieval_strategy` | `two_stage` | `two_stage` (baseline) или `hybrid` |
| `retrieval_topK` | 10 | Топ-K кандидатов на этапе retrieval |
| `threshold` | 0.0 | Порог отсечения (0.0 = отключен) |
| `enable_rerank` | false | Включить реранкинг |
| `rerank_strategy` | `none` | `none`, `heuristic`, `llm`, `cross_encoder` |
| `final_topK` | 5 | Финальный топ-K результатов |

**Интегрировать `PipelineFactory` в `/search` endpoint:**
```kotlin
get("/search") {
    val query = call.request.queryParameters["query"] ?: ...
    
    // Парсинг query params → PipelineConfig
    val config = PipelineConfig(
        retrievalStrategy = call.request.queryParameters["retrieval_strategy"]
            ?.let { RetrievalStrategy.valueOf(it.uppercase()) }
            ?: RetrievalStrategy.TWO_STAGE,
        retrievalTopK = call.request.queryParameters["retrieval_topK"]?.toInt() ?: 10,
        threshold = call.request.queryParameters["threshold"]?.toDouble() ?: 0.0,
        enableRerank = call.request.queryParameters["enable_rerank"]?.toBoolean() ?: false,
        finalTopK = call.request.queryParameters["final_topK"]?.toInt() ?: 5,
    )
    
    // Создание pipeline из конфига
    val pipeline = pipelineFactory.create(config)
    
    // Выполнение pipeline
    val context = pipeline.execute(query)
    
    // Ответ
    call.respondText(ContextFormatter.format(context.packed!!))
}
```

---

## Этап 2: Реализация Filtering и Reranking (Требование Day 23)

### 2.1. Threshold Filtering

| Файл | Назначение |
|------|------------|
| `search/filter/ThresholdFilterStep.kt` | Отсечение по порогу similarity |

**Реализация:**
```kotlin
class ThresholdFilterStep(private val threshold: Double) : PipelineStep {
    override suspend fun process(context: PipelineContext): PipelineContext {
        val filtered = context.retrieved.filter { it.score >= threshold }
        return context.copy(filtered = filtered)
    }
}
```

### 2.2. Reranker Strategies

| Файл | Назначение |
|------|------------|
| `search/rerank/Reranker.kt` | Интерфейс |
| `search/rerank/HeuristicReranker.kt` | Keyword overlap (fallback) |
| `search/rerank/LlmReranker.kt` | LLM-based ranking |
| `search/rerank/CrossEncoderReranker.kt` | ONNX BGE-Reranker (production) |
| `search/rerank/RerankStep.kt` | Интеграция в pipeline |

### 2.3. Обновить Модели Данных

| Файл | Изменение |
|------|-----------|
| `db/SearchResult.kt` | Добавить `scores: StageScores?` |
| `db/StageScores.kt` | Новый файл: история scoring |

**StageScores:**
```kotlin
data class StageScores(
    val embeddingScore: Double,
    val keywordScore: Double? = null,
    val methodBoost: Double? = null,
    val filterThreshold: Double? = null,
    val rerankScore: Double? = null
)
```

---

## Этап 3: Observability и Метрики

### 3.1. Добавить Метрики

**В PipelineContext:**
```kotlin
data class PipelineContext(
    ...
    val metrics: PipelineMetrics = PipelineMetrics()
)

data class PipelineMetrics(
    val timings: MutableMap<String, Long> = mutableMapOf(),
    val scoresBeforeFilter: List<Float> = emptyList(),
    val scoresAfterRerank: List<Float> = emptyList(),
    val itemsBeforeFilter: Int = 0,
    val itemsAfterFilter: Int = 0,
)
```

**Логирование:**
- Тайминги на каждый этап
- Количество элементов до/после фильтрации
- Распределение scores

### 3.2. Обновить API для Тестирования

**Важно:** Сохранить существующие Agent Tools (`@@talk(rag --gentest)`, `@@talk(rag --runtest)`) как основной способ тестирования.

**Дополнительно:**
- `/evaluate` endpoint для запуска golden dataset (опционально)
- Сохранение результатов в файл с деталями

---

## Этап 4: Интеграция с Agent System

### 4.1. RAG Test Commands (Сохранить!)

**Текущий способ тестирования — Agent Tools:**

| Команда | Назначение |
|---------|------------|
| `@@talk(rag --gentest)` | Генерация теста по кодовой базе |
| `@@talk(rag --runtest <code>)` | Запуск теста |

**Поток:**
```
User: @@talk(rag --gentest)
    ↓
TalkWorker → RagCommandHandler.handleGenerateTest()
    ↓
RagSearchRepository.search() → RAG Server /search
    ↓
LLM с RAG-контекстом → генерация теста
    ↓
Возврат результата пользователю
```

### 4.2. MCP Test Runner Tool

**Добавить в RAG Server:**
- `run_kotlin_tests` tool для запуска тестов
- Интеграция с существующим `ToolCallOrchestrator` в Android приложении

---

## Этап 5: Документация и Сравнение Режимов

### 5.1. Документация

- Обновить `plans/task.md` с реализованными этапами
- Добавить README для pipeline архитектуры

### 5.2. Сравнение Режимов

**Режимы для сравнения:**

| Режим | Конфигурация | Описание |
|-------|-------------|----------|
| **Baseline** | `retrieval_strategy=two_stage` | Текущая реализация (без фильтра и rerank) |
| **С фильтром** | `threshold=0.65` | Threshold filtering |
| **С hybrid retrieval** | `retrieval_strategy=hybrid` | SearchService с hybrid scoring |
| **С реранкингом** | `enable_rerank=true` | Full pipeline |

**Метрики для сравнения:**
- Hit Rate@K
- MRR (Mean Reciprocal Rank)
- Avg Score
- Latency per Stage

---

## Принципы Реализации

✅ **Clean Architecture** — domain/data/ui разделение  
✅ **SOLID** — каждый этап отдельный класс  
✅ **Feature Toggles** — динамическое включение/выключение этапов  
✅ **Observability** — метрики, логи, debug info  
✅ **Backward Compatibility** — существующие команды продолжают работать  

---

## Ожидаемый Результат

1. **Pipeline Architecture** — явный, расширяемый, тестируемый
2. **Reranking + Filtering** — требование Day 23 выполнено
3. **Hybrid Retrieval** — возможность переключения между `TwoStageSearchService` и `SearchService`
4. **A/B Тестирование** — сравнение режимов через query params
5. **Agent Integration** — `@@talk(rag --gentest|--runtest)` команды сохраняются
6. **Automated Testing** — golden dataset + метрики (опционально)
