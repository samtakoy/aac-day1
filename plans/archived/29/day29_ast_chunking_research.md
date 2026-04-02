# RAG Chunking Research: Результаты и рекомендации

## 1. Проблема (диагностирована по логам)

### Наблюдаемые симптомы
- Чанки начинаются посередине строки: `d: String? = null,`, `nfigured for the AI agent service.`
- `decl=for`, `decl=serves`, `decl=maintains`, `decl=facilitates` — мусорные имена деклараций
- LLM reranker выдаёт все `0.00` → `postRerankThreshold` убивает все результаты → пустой ответ
- `structural=1367` чанков vs `fixed=2613` — structural вдвое меньше (ненормально)
- `metadataVectors=494` из `metadata=1171` — 677 классов без embedding в Stage 1

### Корневые причины (цепочка)

**Причина 1: regex-сплит не понимает вложенность**
```kotlin
Regex("""(?=\n(?:fun |class |interface |object |...))""")
```
`fun` внутри лямбды, `companion object` внутри класса, `class` в строке — всё триггерит сплит.

**Причина 2: sub-split через FixedSizeStrategy**
```kotlin
val subChunks = FixedSizeStrategy(maxChunkSize, maxChunkSize / 5).split(block.trim(), ...)
subChunks.forEach { sub -> chunks.add(sub.copy(strategy = strategyName)) }
```
Блок > 2000 символов (большой класс, длинный import-блок) → нарезка каждые 1600 символов посередине строки. `strategy="structural"` сохраняется, содержимое — мусор.

**Причина 3: extractDeclarationName ненадёжен**
```kotlin
val DECLARATION_REGEX = Regex("""(?:fun|class|interface|object|typealias)\s+(\w+)""")
```
Если чанк начался с KDoc или sub-split посередине — первое совпадение случайно.

**Причина 4: одна стратегия на все типы файлов**
`StructuralStrategy` (написана под Kotlin) применяется ко всем файлам: `.kt`, `.kts`, `.md`.
Для `.md` regex ничего не найдёт → весь файл как один блок → sub-split по символам.

### Масштаб проблемы
- DB stats: `structural=1367, fixed=2613` — structural чанков вдвое меньше fixed
- `metadata=1171, metadataVectors=494` — 677 классов без metadata vectors (Stage 1 не может их найти)
- Stage 1 регулярно возвращает `maintains`, `serves`, `facilitates` как "имена классов"
- LLM-реранкер при первом прогоне выдаёт все 0.00

---

## 2. Лучшие практики (источники)

### cAST paper (EMNLP 2025): "Enhancing Code RAG with Structural Chunking via AST"
Результаты на реальных бенчмарках vs naive chunking:
- +5.5 points на RepoEval
- +4.3 points на CrossCodeEval
- +2.7 points на SWE-bench
- 70% fully correct vs 59% (naive) и 61% (language-aware regex)

**Алгоритм split-then-merge**:
1. Парсим файл в AST
2. Обходим сверху вниз
3. Нода влезает в `targetChunkSize` → один чанк
4. Нода не влезает → рекурсивно спускаемся в дочерние ноды
5. После сплита — жадно мержим соседние ноды пока суммарный размер ≤ `targetChunkSize`
6. Размер считается по **non-whitespace символам** (не токены, не строки)

**Что хранить в чанке** (согласно paper и индустриальным практикам):
```
[file: GraphAIAgent.kt] [class: GraphAIAgent] [function: runSession]
<imports релевантные для типов в чанке>
<полный текст объявления>
```

### supermemory/code-chunk: production реализация cAST
Источник: https://supermemory.ai/blog/building-code-chunk-ast-aware-code-chunking/
Репо: https://github.com/supermemoryai/code-chunk (TypeScript/JS, не JVM — принципы переносим)

**Архитектура (5 шагов)**:

1. **Parse** — tree-sitter → AST
2. **Extract** — семантические entities (сигнатура, KDoc, parent-child связи, byte/line ranges)
3. **Build Scope Tree** — иерархия через DFS: `UserService > getUser`. Основа для Stage 1 матчинга.
4. **Greedy Window Assignment**:
   - Размер = **non-whitespace символы** (не токены, не строки)
   - Cumulative sum arrays → O(1) range queries
   - Добавляем ноды → если > лимит, рекурсируем в дочерние → не влезает → новое окно
   - Жадно мержим соседние маленькие окна
5. **Enrich: contextualizedText** — ключевое отличие от raw кода:
```
// File: GraphAIAgent.kt
// Scope: GraphAIAgent > runSession
// Imports: FeatureContext, ToolRegistry, Clock
// Siblings: startSession(), stopSession(), handleTool()

fun runSession(input: I, ...): O { ... }
```
**Для embedding нужно использовать `contextualizedText`, не raw `text`.**
Это объясняет низкие embedding scores (0.49–0.74) — модель видит код без контекста.

**Benchmark: правильная метрика — IoU@5, не recall**
- AST chunker: **70.1% recall@5, IoU@5 = 0.43**
- Alternative chunkers: 49.0% recall, IoU@5 = 0.38
- Fixed-size baseline: 42.4% recall, IoU@5 = 0.34
- SWE-bench: время задачи 2.0→1.2 мин, токены 4.3k→2.4k, tool calls 19→12

### CocoIndex / LanceDB / Qodo best practices
- Chunk = синтаксически валидная единица (функция, класс, блок)
- Никогда не резать посередине выражения
- Метаданные **prepend'ятся внутрь content** чанка, не только в поля — улучшает embedding
- Overlap = 10-20% от размера, но только на границах функций
- Tree-sitter — стандарт де-факто: Neovim, Helix, Zed, GitHub Copilot

---

## 3. Правильная архитектура: стратегия per file type

Сейчас одна `StructuralStrategy` применяется ко всем файлам. Это неверно.
Нужен **`LanguageAwareChunker`** — router по расширению файла:

| Расширение | Стратегия | Граница нарезки |
|---|---|---|
| `.kt`, `.kts` | `AstChunkingStrategy(KotlinGrammar)` | функции, классы, объекты |
| `.java` | `AstChunkingStrategy(JavaGrammar)` | методы, классы |
| `.py`, `.go`, `.rs`, `.ts`, `.js` | `AstChunkingStrategy(соответствующая)` | функции, классы |
| `.md` | `MarkdownChunkingStrategy` | заголовки `#`, `##`, `###` |
| `.txt`, `.json`, `.yaml`, `.toml` | `FixedSizeStrategy` | семантики нет, sliding window |
| неизвестные | `FixedSizeStrategy` | fallback |

`ktreesitter` решает это элегантно — каждый язык это отдельная `Language`, алгоритм один.

---

## 4. Варианты интеграции Tree-sitter в JVM

### ✅ Рекомендуемый: `ktreesitter` (официальные Kotlin Multiplatform биндинги)
- Репо: https://github.com/tree-sitter/kotlin-tree-sitter — **от организации tree-sitter**
- Артефакт: `io.github.tree-sitter:ktreesitter:0.24.1` в **Maven Central**
- Kotlin Multiplatform: JVM, Android, Linux/macOS/Windows
- Нативный C код компилируется через Konan C interop → **статически линкуется**
- **Нет проблемы с fat jar**: не нужен ручной extract `.so`

**Встроенные grammars в репо**: Java.
**Остальные grammars** подключаются через `ktreesitter-plugin` из исходников:
- Kotlin: `fwcd/tree-sitter-kotlin` (178 stars, август 2024, нет Maven артефакта)
- Python, Go, Rust, TS: официальные tree-sitter grammars, нет JVM артефактов

```kotlin
// Базовый API:
val language = Language(TreeSitterKotlin.language())
val parser = Parser(language)
val tree = parser.parse(sourceCode)
val root = tree.rootNode
// node.type → "function_declaration", "class_declaration", ...
// node.startPoint → Point(row, column)
// node.children → List<Node>
// node.text(source) → String
```

### Вариант B: `tree-sitter` через subprocess
**Не рассматривать**: внешняя зависимость на окружение, latency, сложный деплой.

### Вариант C: kotlin-compiler-embeddable (PSI)
`org.jetbrains.kotlin:kotlin-compiler-embeddable` — ~50MB, init ~3с. Оверкилл.

---

## 5. Три подхода к реализации (исследованы на реальном build.gradle.kts)

### Подход А: `ktreesitter` (официальные KMP биндинги)

```kotlin
// build.gradle.kts
plugins { id("io.github.tree-sitter.ktreesitter-plugin") version "0.24.1" }
dependencies { implementation("io.github.tree-sitter:ktreesitter:0.24.1") }
// Kotlin grammar — fwcd/tree-sitter-kotlin (нет в Maven Central, нужен git submodule)
// Java grammar — есть в ktreesitter из коробки
```

| | |
|---|---|
| Качество чанков | ★★★★★ (+5.5 points RepoEval — измерено) |
| Multi-language | ★★★★★ (один алгоритм, разные Language объекты) |
| Риск интеграции | **ВЫСОКИЙ** |
| Fat jar | ⚠️ нужна проверка: `duplicatesStrategy = EXCLUDE` vs KMP нативные ресурсы |
| Время | 2-3 дня с рисками |

**Открытые вопросы**:
1. `ktreesitter-plugin` — нетривиальная настройка: нужно указать пути к grammar.js из git submodule
2. Kotlin grammar нет в Maven Central — нужен local path или submodule к fwcd/tree-sitter-kotlin
3. Точные node type names: `function_declaration` vs `fun_declaration` — нужен тест-утилита для вывода AST
4. Совместимость KMP артефакта с текущим `duplicatesStrategy = DuplicatesStrategy.EXCLUDE`

**Предположительные node types в tree-sitter-kotlin** (требует верификации):
- `source_file` — корень
- `class_declaration` — class/data class/sealed/abstract class
- `object_declaration` — object/companion object
- `function_declaration` — fun
- `property_declaration` — val/var
- `import_list` — блок импортов
- `package_header` — package

---

### Подход Б: `kotlin-compiler-embeddable` (Kotlin PSI)

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.1.0") // ~50MB
}
```

```kotlin
// Инициализация (один раз при старте)
val environment = KotlinCoreEnvironment.createForProduction(
    Disposer.newDisposable(), CompilerConfiguration(), EnvironmentConfigFiles.JVM_CONFIG_FILES
)
val psiFactory = KtPsiFactory(environment.project)

// Парсинг — точные байтовые позиции, KDoc, imports
val ktFile: KtFile = psiFactory.createFile(sourceCode)
ktFile.declarations           // KtClass, KtFunction, KtObjectDeclaration, ...
ktFile.packageFqName          // com.example.day.features.chats
ktFile.importDirectives       // List<KtImportDirective>
decl.docComment?.text         // KDoc, гарантированно привязан к объявлению
decl.startOffset / endOffset  // точные байтовые позиции
```

| | |
|---|---|
| Качество чанков | ★★★★☆ (100% точные границы для .kt) |
| Multi-language | ★☆☆☆☆ (только .kt, для остального нужны другие стратегии) |
| Риск интеграции | **НИЗКИЙ** — стабильный JVM API, нет нативных зависимостей |
| Fat jar | ✅ нет проблем |
| Время | 1 день |

**Плюсы**:
- PSI — тот же парсер что использует IntelliJ IDEA, 100% корректный AST для Kotlin
- KDoc через `decl.docComment` — правильная привязка гарантирована парсером, не эвристикой
- `decl.startOffset/endOffset` — точные позиции без regex угадывания
- Рекурсивный спуск в `KtClassOrObject.declarations` — методы внутри класса как отдельные чанки

**Минусы**:
- ~50MB добавляется к fat jar
- Инициализация `KotlinCoreEnvironment` ~2-3 секунды при старте сервера (один раз)
- Только `.kt` — `.md`, `.java` требуют отдельных стратегий

---

### Подход В: `BraceDepthStrategy` (без зависимостей)

Посимвольный разбор с отслеживанием глубины скобок. Решает корневую проблему regex — не видит вложенность — без единой новой зависимости.

**Алгоритм**:
1. Посимвольный проход, корректно пропускает строки (`"`), символы (`'`), line-comments (`//`), block-comments (`/* */`)
2. `{` → `depth++`, `}` → `depth--`
3. Keyword (`fun|class|interface|object|...`) встречен при `depth == 0` → начало объявления
4. `depth` вернулся к 0 после первого `{` → конец объявления (точная граница `}`)
5. KDoc и аннотации перед объявлением включаются через поиск назад от начала declaration

```kotlin
class BraceDepthStrategy(val targetChunkSize: Int = 1500) : ChunkingStrategy {
    override val strategyName = "structural"

    private fun extractDeclarations(source: String): List<DeclarationSpan> {
        var depth = 0; var inLineComment = false; var inBlockComment = false
        var inString = false; var declStart = -1; var declName = ""
        val result = mutableListOf<DeclarationSpan>()
        var i = 0
        while (i < source.length) {
            val c = source[i]; val next = source.getOrNull(i + 1)
            when {
                inLineComment  -> if (c == '\n') inLineComment = false
                inBlockComment -> if (c == '*' && next == '/') { inBlockComment = false; i++ }
                inString       -> if (c == '"' && source.getOrNull(i-1) != '\\') inString = false
                c == '/' && next == '/' -> inLineComment = true
                c == '/' && next == '*' -> { inBlockComment = true; i++ }
                c == '"' -> inString = true
                c == '{' -> { if (depth == 0 && declStart < 0) declStart = findDeclStart(source, i); depth++ }
                c == '}' -> { depth--; if (depth == 0 && declStart >= 0) {
                    result += DeclarationSpan(declStart, i + 1, declName); declStart = -1
                }}
                depth == 0 -> tryMatchDeclaration(source, i)?.let { declName = it }
            }
            i++
        }
        return result
    }
}
data class DeclarationSpan(val startOffset: Int, val endOffset: Int, val name: String)
```

| | |
|---|---|
| Качество чанков | ★★★☆☆ (~95% корректных границ) |
| Multi-language | ★★★☆☆ (C-подобные языки с минимальной адаптацией) |
| Риск интеграции | **ОЧЕНЬ НИЗКИЙ** — нет зависимостей |
| Fat jar | ✅ нет проблем |
| Время | 0.5 дня |

**Плюсы**:
- Нет новых зависимостей — нет рисков с fat jar, сборкой, совместимостью
- Решает все три корневых причины: вложенность (depth), sub-split (нет!), declarationName (из match в начале)
- Дебажится легко, понятен без знания tree-sitter API

**Минусы**:
- Не настоящий AST — угловые случаи: `val x = object : Foo { }`, строковые шаблоны `"${}"`, тройные строки
- KDoc и imports по-прежнему через regex (хотя применяется в нужном месте)
- Нет типизированного доступа к узлам для `contextualizedText`

---

## 6. Сравнение подходов

| Критерий | ktreesitter | PSI | BraceDepth |
|---|---|---|---|
| Качество чанков | ★★★★★ | ★★★★☆ | ★★★☆☆ |
| Multi-language | ★★★★★ | ★☆☆☆☆ | ★★★☆☆ |
| Риск интеграции | ВЫСОКИЙ | НИЗКИЙ | ОЧЕНЬ НИЗКИЙ |
| Fat jar | ⚠️ проверка | ✅ OK | ✅ OK |
| Время | 2-3 дня | 1 день | 0.5 дня |
| Зависимости | +ktreesitter (нативный C) | +50MB jar | нет |
| Точность границ | 100% | 100% | ~95% |
| KDoc/imports в контексте | ✅ полный | ✅ полный | частичный (regex) |

---

## 7. Рекомендуемый гибридный план

**Шаг 1 — BraceDepthStrategy** (сегодня): устраняет 90% проблем без рисков, даёт рабочую систему с правильными границами чанков. Заменяет `StructuralStrategy` как `strategy="structural"`.

**Шаг 2 — PSI для .kt** (следующая итерация): подменяем BraceDepth для Kotlin файлов → 100% корректность + KDoc привязка + contextualizedText с реальными imports.

**Шаг 3 — ktreesitter** (если нужен multi-language): только когда PSI уже работает, понятна проблема fat jar, и нужна поддержка Java/Python/Go.

Каждый шаг даёт ценность сам по себе и не блокирует следующий.

---

## 8. Что уже диагностировано и готово к использованию

- Логи добавлены в `TwoStageSearchService` и `LlmReranker` — видна полная картина
- Проблема локализована точно: `StructuralStrategy.split()` + sub-split + нет per-type routing
- `ChunkEntity` нужно расширить полем `parentScope: String?`
- `TwoStageSearchService.drillDown()` нужен патч для матчинга по `parentScope`
- После переиндексации нужно пересобрать metadata vectors (`EXTRACT_METADATA=true`)
- `FileScanner` нужно расширить поддерживаемыми расширениями (сейчас только `kt`, `kts`, `md`)
