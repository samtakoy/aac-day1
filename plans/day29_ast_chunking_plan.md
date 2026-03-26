# Plan: AST-Based Chunking для RAG Server

## 1. Описание проблемы

### Что наблюдается
При поиске через `/search` возвращаются чанки, обрезанные посередине кода:
```
// File: GraphAIAgent.kt
d: String? = null,
        clock: Clock = Clock.System,
        installFeatures: FeatureContext.() -> Unit = {}
    ) : this(
```
```
// File: AIAgentServiceBuilderAPI.kt
nfigured for the AI agent service.
     * @return The instance of AIAgentServiceBuilder...
```

Блоки начинаются посередине KDoc, посередине сигнатуры конструктора, посередине import-блока.
LLM-реранкер получает бессмысленные фрагменты → ставит score=0.00 → `postRerankThreshold` убивает все результаты → пустой ответ.

### Корневые причины

**1. regex-сплит не понимает вложенность**
`StructuralStrategy` режет по `(?=\nfun |class |...)`. Не видит depth — `fun` в лямбде,
`companion object` внутри класса, `class` в строке — всё триггерит сплит.

**2. Sub-split через FixedSizeStrategy**
Блок > 2000 символов → `FixedSizeStrategy(2000, 400).split(block)` → нарезка каждые 1600
символов посередине строки. `strategy="structural"` сохраняется, содержимое — мусор.

**3. extractDeclarationName ненадёжен**
Если чанк начался с KDoc или посередине — первое совпадение случайно.
В логах: `decl=for`, `decl=serves`, `decl=maintains` — слова из KDoc, не имена классов.

**4. Одна стратегия на все типы файлов**
`StructuralStrategy` (Kotlin regex) применяется к `.kt`, `.kts` и `.md`.
Для `.md` regex ничего не найдёт → весь файл как один блок → sub-split по символам.

### Масштаб
- `structural=1367` vs `fixed=2613` — structural вдвое меньше (должно быть наоборот)
- `metadataVectors=494` из `metadata=1171` — 677 классов невидимы для Stage 1
- LLM reranker выдаёт все 0.00 при каждом втором запросе

---

## 2. Три подхода к решению

Исследованы на реальном `rag-server/build.gradle.kts` (fat jar с `duplicatesStrategy = EXCLUDE`).

### Подход А: `ktreesitter` — настоящий AST, максимальное качество
- Риск: ВЫСОКИЙ. Kotlin grammar нет в Maven Central, нужен git submodule. Совместимость с fat jar требует проверки.
- Время: 2-3 дня.

### Подход Б: `kotlin-compiler-embeddable` (PSI) — 100% точность для Kotlin
- Риск: НИЗКИЙ. Стабильный JVM API, нет нативных зависимостей, +50MB к fat jar.
- Время: 1 день.

### Подход В: `BraceDepthStrategy` — без зависимостей, решает корневые причины
- Риск: ОЧЕНЬ НИЗКИЙ. Посимвольный разбор с depth-tracking. ~95% точность.
- Время: 0.5 дня.

### Рекомендуемый гибридный план
1. **Шаг 1**: `BraceDepthStrategy` — быстро устраняет 90% проблем, нет рисков
2. **Шаг 2**: PSI для `.kt` — подменяем BraceDepth, получаем 100% + KDoc + contextualizedText
3. **Шаг 3**: `ktreesitter` — только если нужен multi-language (Java, Python, Go)

> Детальное сравнение всех трёх подходов: `plans/day29_ast_chunking_research.md` §5-6

---

## 3. Принципы решения (из cAST paper + supermemory/code-chunk)
1. Чанк = синтаксически валидная единица (никогда не резать посередине выражения)
2. split-then-merge алгоритм — рекурсивный спуск + жадное слияние соседних нод
3. `contextualizedText` в content чанка — scope chain + imports + сигнатуры соседей prepend'ятся
4. Размер = non-whitespace символы
5. **Стратегия per file type** — разные языки и форматы нарезаются по-разному

### Архитектура: `LanguageAwareChunker`

```
IndexingService
    └─► LanguageAwareChunker.chunkFile(file)
            ├─ .kt / .kts  → BraceDepthStrategy (Шаг 1) → AstChunkingStrategy/PSI (Шаг 2)
            ├─ .java        → BraceDepthStrategy (Шаг 1) → AstChunkingStrategy(JavaLanguage) (Шаг 3)
            ├─ .md          → MarkdownChunkingStrategy
            └─ остальные    → FixedSizeStrategy (fallback)
```

---

## 4. Зависимости по шагам

### Шаг 1: BraceDepthStrategy — нет новых зависимостей
`build.gradle.kts` не меняется.

### Шаг 2: PSI для .kt (опционально)
```kotlin
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.1.0") // ~50MB
}
```
Fat jar совместим: нет нативных артефактов.

### Шаг 3: ktreesitter (опционально, для multi-language)
```kotlin
plugins {
    id("io.github.tree-sitter.ktreesitter-plugin")  // генерирует language bindings
}
dependencies {
    implementation("io.github.tree-sitter:ktreesitter:0.24.1")
    // grammars через ktreesitter-plugin из исходников:
    // - fwcd/tree-sitter-kotlin  (kotlin, kts) — нет в Maven Central
    // - tree-sitter/tree-sitter-java — уже в репо ktreesitter
}
```
⚠️ Нужна проверка совместимости с `duplicatesStrategy = DuplicatesStrategy.EXCLUDE`.

---

## 4. Структура результирующего чанка

### Code-файлы (.kt, .java, ...)
```
// File: GraphAIAgent.kt
// Package: ai.koog.agents.core.agent
// Scope: GraphAIAgent
// Imports: FeatureContext, ToolRegistry, Clock
// Siblings: startSession(), stopSession(), handleTool()

fun runSession(
    input: I,
    clock: Clock = Clock.System,
): O {
    ...
}
```

### Markdown (.md)
```
// File: FEATURES.md
// Section: Installing Features > Using FeatureMessageProcessor

## Using FeatureMessageProcessor

Install a feature with FeatureMessageProcessor:
...
```

### Fallback (.txt, .json, .yaml, ...)
```
// File: config.yaml
<sliding window content, 1000 chars, 200 overlap>
```

---

## 5. План реализации (гибридный, по шагам)

### Шаг 1: `BraceDepthStrategy` — заменяет `StructuralStrategy`
**Файл**: `indexing/ChunkingStrategy.kt` (новый класс рядом с `FixedSizeStrategy`)

Алгоритм (посимвольный разбор с depth tracking):
```kotlin
class BraceDepthStrategy(
    val targetChunkSize: Int = 1500,
    val maxChunkSize: Int = 3000,
) : ChunkingStrategy {
    override val strategyName = "structural"

    // Keyword на depth=0 → начало объявления
    // { → depth++, } → depth-- → depth==0 означает конец объявления
    // Строки, символы, комментарии (// и /* */) — корректно пропускаются
    private fun extractDeclarations(source: String): List<DeclarationSpan> { ... }

    // KDoc и аннотации перед объявлением включаются через поиск назад
    private fun findDeclStart(source: String, bracePos: Int): Int { ... }
}

data class DeclarationSpan(val startOffset: Int, val endOffset: Int, val name: String)
```

Что исправляет:
- `fun` в лямбде при `depth > 0` — не триггерит новый чанк ✅
- `companion object` внутри класса — не отрывается ✅
- Большой блок — не sub-split через FixedSize, остаётся целым чанком ✅
- `declarationName` — из keyword match в начале объявления, не из середины ✅

### Шаг 2 (опционально): PSI для `.kt`
**Файл**: `indexing/PsiChunkingStrategy.kt` — реализует тот же интерфейс `ChunkingStrategy`

```kotlin
// Инициализация (один раз, lazy)
val ktFile: KtFile = KtPsiFactory(kotlinEnv.project).createFile(sourceCode)

// Рекурсивный спуск: ktFile.declarations → KtClass → declarations (методы)
// KDoc: decl.docComment?.text — гарантированно привязан
// Imports: ktFile.importDirectives — для contextualizedText
// Позиции: decl.startOffset, decl.endOffset — точные байты
```

Добавить в `LanguageAwareChunker`: для `.kt` использовать `PsiChunkingStrategy`, для остального — `BraceDepthStrategy`.

### Шаг 3: `MarkdownChunkingStrategy`
**Файл**: `indexing/MarkdownChunkingStrategy.kt`

Нарезка по заголовкам `#`, `##`, `###`:
- Каждый заголовок с телом → один чанк
- Заголовок чанка = полный breadcrumb: `# Features > ## Installing > ### Using MessageProcessor`
- Если секция > maxChunkSize → split по параграфам (`\n\n`)
- `declarationName` = текст заголовка

### Шаг 4: `LanguageAwareChunker`
**Файл**: `indexing/LanguageAwareChunker.kt`

```kotlin
class LanguageAwareChunker(
    private val targetChunkSize: Int = 1500,
    private val maxChunkSize: Int = 3000,
) {
    private val kotlinStrategy = AstChunkingStrategy(KotlinLanguage(), targetChunkSize, maxChunkSize)
    private val javaStrategy   = AstChunkingStrategy(JavaLanguage(), targetChunkSize, maxChunkSize)
    private val mdStrategy     = MarkdownChunkingStrategy(maxChunkSize)
    private val fixedStrategy  = FixedSizeStrategy(chunkSize = 1000, overlap = 200)

    fun chunk(file: File): List<ChunkEntity> = when (file.extension.lowercase()) {
        "kt", "kts"        -> kotlinStrategy.split(file.readText(), file.path, file.name)
        "java"             -> javaStrategy.split(file.readText(), file.path, file.name)
        "md"               -> mdStrategy.split(file.readText(), file.path, file.name)
        else               -> fixedStrategy.split(file.readText(), file.path, file.name)
    }
}
```

### Шаг 5: Обновление `ChunkEntity`
**Файл**: `db/ChunkEntity.kt`
```kotlin
data class ChunkEntity(
    val id: Long = -1L,
    val content: String,          // contextualizedText (scope + imports + siblings + code)
    val filePath: String,
    val fileName: String,
    val packageName: String = "",
    val declarationName: String? = null,
    val parentScope: String? = null,   // ← новое: "GraphAIAgent<I, O> : AIAgentBase<I, O>"
    val startLine: Int = 0,
    val strategy: String,
    val chunkOrder: Int,
    val indexedAt: String,
)
```

### Шаг 6: Обновление БД
**Файл**: `db/CodeDatabase.kt`
- Добавить колонку `parent_scope TEXT` в `CodeChunksTable`
- БД пересоздаётся при `FORCE_REINDEX=true` — миграция не нужна

### Шаг 7: Обновление `IndexingService`
**Файл**: `indexing/IndexingService.kt`
```kotlin
// До:
val strategies = listOf(FixedSizeStrategy(), StructuralStrategy())
for (strategy in strategies) { indexStrategy(strategy, files, config.forceReindex) }

// После:
val chunker = LanguageAwareChunker()
indexAllFiles(chunker, files, config.forceReindex)
```
`StructuralStrategy` — удалить. `FixedSizeStrategy` — оставить (используется внутри `LanguageAwareChunker`).

### Шаг 8: Обновление `FileScanner`
**Файл**: `indexing/FileScanner.kt`
```kotlin
// До:
private val INCLUDED_EXTENSIONS = setOf("kt", "kts", "md")

// После:
private val INCLUDED_EXTENSIONS = setOf("kt", "kts", "java", "md", "txt", "yaml", "toml", "json")
```

### Шаг 9: Обновление `TwoStageSearchService`
**Файл**: `search/TwoStageSearchService.kt`
Добавить матчинг по `parentScope` в `drillDown`:
```kotlin
val classChunks = allStructural.filter { (chunk, _) ->
    chunk.fileName.removeSuffix(".kt").equals(classMeta.className, ignoreCase = true) ||
    chunk.declarationName?.equals(classMeta.className, ignoreCase = true) == true ||
    chunk.parentScope?.contains(classMeta.className) == true  // ← новое
}
```

### Шаг 10: Переиндексация
```bash
FORCE_REINDEX=true EXTRACT_METADATA=true ./gradlew run
```

---

## 6. Тестирование

### Unit: `AstChunkingStrategy`
```kotlin
// Тест 1: простая функция → 1 чанк, начинается с fun
// Тест 2: класс с 3 методами, умещается → 1 чанк с классом целиком
// Тест 3: большой класс → методы как отдельные чанки, каждый с parentScope
// Тест 4: companion object → не отрывается от класса
// Тест 5: вложенный класс → inner class внутри outer
// Тест 6: contextualizedText содержит scope chain и imports
// Тест 7: GraphAIAgent.kt — реальный файл, все чанки начинаются с объявления
```

### Unit: `MarkdownChunkingStrategy`
```kotlin
// Тест 1: три секции → 3 чанка
// Тест 2: большая секция → split по параграфам
// Тест 3: breadcrumb заголовок сохраняется в каждом чанке
// Тест 4: FEATURES.md из реального кодебейса
```

### Интеграционный тест
- До: некоторые чанки начинаются с `d: String?`, `decl=for`
- После: все structural chunks начинаются с объявления, все md chunks с заголовка

### Smoke test pipeline
```bash
curl -X POST localhost:3001/search \
  -d '{"query":"какие основные возможности агента", "preset":"reranked_llm"}'
# Ожидание: results > 0, все chunks начинаются с объявления или заголовка
```

---

## 7. Что НЕ меняем

- `PipelineConfig`, `PipelineExecutor`, `RetrievalStep` — без изменений
- `ChunkEntity.formatHeader()` — без изменений
- `FixedSizeStrategy` — остаётся как fallback внутри `LanguageAwareChunker`
- `IndexStats` — без изменений (поля structural/fixed остаются)

---

## 8. Ожидаемый результат

| Метрика | До | После |
|---------|-----|-------|
| Чанки начинающиеся с объявления | ~60% | ~100% |
| `decl=for/serves/maintains` | часто | никогда |
| LLM reranker score=0.00 для всех | при каждом втором запросе | не должно быть |
| structural chunks в БД | 1367 | ~2500+ |
| exactMatch в drillDown | низкий | высокий |
| `.md` файлы нарезаны | как code (плохо) | по заголовкам |
| embedding quality | низкое (нет контекста) | выше (contextualizedText) |

---

## 9. Риски

| Риск | Митигация |
|------|-----------|
| `ktreesitter-plugin` — сложная настройка grammars | Начать с Java grammar (есть в репо), Kotlin добавить вторым шагом |
| fat jar несовместим с KMP артефактом | Проверить в первом же коммите, до написания алгоритма |
| Kotlin grammar node types отличаются от предполагаемых | Написать тест-утилиту: напечатать AST для простого `.kt` файла |
| Большой метод (> maxChunkSize) — всё равно один чанк | Допустимо. Не sub-split посимвольно — это главный инвариант |
| После переиндексации metadata vectors устарели | `EXTRACT_METADATA=true` при переиндексации обязательно |
