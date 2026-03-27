# AST-based Chunking: ktreesitter Integration Plan

## Context

`StructuralStrategy` в rag-server разбивает Kotlin-файлы по regex (`fun|class|interface|...`).
Regex не понимает вложенность: `companion object` внутри класса триггерит лишний сплит.
Большие блоки sub-split'ятся через `FixedSizeStrategy` посередине строки.
Результат: LLM-реранкер получает бессмысленные фрагменты → `score=0.00` → пустой ответ.

Спайк в `tree-sitter-spike/` подтвердил: ktreesitter 0.24.1 + Kotlin grammar (fwcd) корректно
строят AST, companion object оказывается внутри класса, а не на top-level. Все API-нюансы задокументированы.

**Цель плана**: заменить `StructuralStrategy` на AST-based chunking без поломки текущей сборки.
Scope: `.kt`/`.kts` → AST, `.md` → заголовки, `.txt` → Fixed.

---

## Архитектурные решения

| Вопрос | Решение |
|---|---|
| Как подключить ktreesitter к rag-server (JVM-only)? | Новый модуль `:rag-grammar` (KMP + jvm()) — изоляция, rag-server не меняет тип |
| Как получить C-исходники грамматики? | Vendor: `parser.c` + `scanner.c` из fwcd/tree-sitter-kotlin лежат в `rag-grammar/grammar/kotlin/src/` |
| Совместимость с существующим DB индексом? | `strategyName = "structural"` сохраняется — FORCE_REINDEX пересобирает с лучшими чанками |
| Миграция схемы БД? | `SchemaUtils.createMissingTablesAndColumns` добавляет новые колонки автоматически |

---

## Файлы проекта: изменяемые

| Файл | Что изменится |
|---|---|
| `rag-server/src/main/kotlin/com/example/day/ragserver/indexing/ChunkingStrategy.kt` | Добавить `AstChunkingStrategy`, `MarkdownChunkingStrategy`, `LanguageAwareChunker`; `StructuralStrategy` удалить |
| `rag-server/src/main/kotlin/com/example/day/ragserver/indexing/IndexingService.kt` | `listOf(FixedSizeStrategy(), StructuralStrategy())` → `LanguageAwareChunker` |
| `rag-server/src/main/kotlin/com/example/day/ragserver/indexing/FileScanner.kt` | `INCLUDED_EXTENSIONS`: добавить `"txt"` |
| `rag-server/src/main/kotlin/com/example/day/ragserver/db/ChunkEntity.kt` | Добавить поле `parentScope: String?` |
| `rag-server/src/main/kotlin/com/example/day/ragserver/db/CodeDatabase.kt` | `CodeChunksTable` + `parent_scope`; обновить `saveChunk()` и `getAllVectors()` |
| `rag-server/src/main/kotlin/com/example/day/ragserver/search/TwoStageSearchService.kt` | `drillDown()`: добавить матчинг по `parentScope` |
| `rag-server/build.gradle.kts` | Добавить `implementation(project(":rag-grammar"))` |
| `settings.gradle.kts` | Добавить `include(":rag-grammar")` |

## Новые файлы/модули

| Путь | Что это |
|---|---|
| `rag-grammar/` | Новый KMP модуль: grammar + native lib |
| `rag-grammar/build.gradle.kts` | KMP + ktreesitter-plugin + CMake tasks |
| `rag-grammar/grammar/kotlin/src/parser.c` | Vendored из fwcd/tree-sitter-kotlin (pre-generated, стабильный) |
| `rag-grammar/grammar/kotlin/src/scanner.c` | Vendored из fwcd/tree-sitter-kotlin (hand-written scanner) |
| `rag-grammar/src/commonMain/kotlin/.../TreeSitterKotlin.kt` | Генерируется плагином (expect object) |
| `rag-grammar/src/jvmMain/kotlin/.../TreeSitterKotlin.kt` | Генерируется плагином (actual object с JNI) |

**Добавление нового языка (напр. Python)**:
1. Скопировать `parser.c` + `scanner.c` из `tree-sitter/tree-sitter-python` в `rag-grammar/grammar/python/src/`
2. Добавить `grammar {}` блок (или отдельный подмодуль — зависит от поддержки нескольких грамматик плагином, **нужно проверить**)
3. Добавить `PythonChunkingStrategy` + роутинг `.py` в `LanguageAwareChunker`

---

## Stage 1: Модуль `:rag-grammar`

**Цель**: изолированный KMP модуль, который компилирует Kotlin grammar и предоставляет
`TreeSitterKotlin.language(): Any` для использования в rag-server.

### Структура модуля (vendor подход)

```
rag-grammar/
├── build.gradle.kts
└── grammar/
    └── kotlin/
        └── src/
            ├── parser.c    ← vendored из fwcd/tree-sitter-kotlin (pre-generated, стабильный)
            └── scanner.c   ← vendored из fwcd/tree-sitter-kotlin (hand-written scanner)
```

Источник: `https://github.com/fwcd/tree-sitter-kotlin` → `src/parser.c`, `src/scanner.c`.
Обновление грамматики = ручная замена двух файлов (делается редко).

### `rag-grammar/build.gradle.kts`

**Плагины:**
- `kotlin("multiplatform")` с таргетом `jvm()`
- `io.github.tree-sitter.ktreesitter-plugin` версии `0.24.1`

**Блок `grammar {}` (официальный DSL плагина):**
- `grammarName = "kotlin"`
- `baseDir = file("grammar/kotlin")` — указывает на vendored исходники
- `files = arrayOf(baseDir.get().resolve("src/parser.c"), baseDir.get().resolve("src/scanner.c"))`
- `packageName = "com.example.day.raggrammar"`
- `className = "TreeSitterKotlin"`
- `libraryName = "ktreesitter-kotlin"` — дефолт плагина (`"ktreesitter-$grammarName"`)

**Gradle-задачи в порядке зависимостей:**

1. `generateGrammarFiles` (из плагина)
   - Inputs: `grammar/kotlin/src/` (vendored, всегда присутствуют)
   - Генерирует: `build/generated/CMakeLists.txt` + Kotlin `expect/actual` исходники

2. `cmakeConfigure`
   - Тип: `Exec`; зависит от: `generateGrammarFiles`
   - Рабочая папка: `build/cmake-build/` (создаётся в `doFirst`)
   - Команда: `cmake <path-to-generated-dir> -DCMAKE_BUILD_TYPE=Release`

3. `cmakeBuild`
   - Тип: `Exec`; зависит от: `cmakeConfigure`
   - Команда: `cmake --build . --config Release`
   - Outputs: `libktreesitter-kotlin.dylib` / `.so` / `.dll`

4. `copyNativeLib`
   - Тип: `Copy`; зависит от: `cmakeBuild`
   - OS/arch detection: `os.name` + `os.arch` → `macos/aarch64`, `linux/x64`, etc.
   - Назначение: `build/resources/jvm/lib/{os}/{arch}/`

5. `processResources` — зависит от `copyNativeLib`
6. `compileKotlin` — зависит от `generateGrammarFiles`

**Зависимости:** `commonMain.dependencies`: `io.github.tree-sitter:ktreesitter:0.24.1`

**`settings.gradle.kts`**: добавить `include(":rag-grammar")`

**Открытый вопрос**: поддерживает ли плагин несколько `grammar {}` блоков в одном модуле?
Если нет — каждый новый язык = отдельный подмодуль (`:rag-grammar-kotlin`, `:rag-grammar-python`).

### Что генерирует плагин

Два Kotlin-файла:
- `commonMain`: `expect object TreeSitterKotlin { fun language(): Any }`
- `jvmMain`: `actual object TreeSitterKotlin` с:
  - `init { System.loadLibrary("kotlin") }` + fallback из classpath
  - `@JvmStatic private external fun tree_sitter_kotlin(): Long`

Аналог ручной `KotlinLanguage.kt` из спайка, но генерируется автоматически.

### Критерий готовности Stage 1

```
./gradlew :rag-grammar:jvmJar
```
Jar содержит `TreeSitterKotlin.class` + `lib/macos/aarch64/libktreesitter-kotlin.dylib`.
```
./gradlew :rag-server:build
```
rag-server собирается без ошибок (зависимость `:rag-grammar` резолвится).

---

## Stage 2: `ChunkEntity` + `CodeDatabase` — поле `parentScope`

**Цель**: добавить `parentScope: String?` в модель чанка и схему БД.
Stage 2 независим от Stage 1, можно выполнять параллельно.

### `ChunkEntity.kt` — новое поле

- Имя: `parentScope`
- Тип: `String?`
- Значение по умолчанию: `null`
- Позиция: после `declarationName`, перед `startLine`
- Семантика: имя enclosing-декларации (напр. `"GraphAIAgent"` для метода внутри класса)
- `formatHeader()` не меняется — parentScope не включается в заголовок

### `CodeDatabase.kt` — изменения

**`CodeChunksTable`**: новая колонка
- Имя в SQL: `parent_scope`
- Exposed: `varchar("parent_scope", 255).nullable()`
- Позиция: после `declarationName`

`SchemaUtils.createMissingTablesAndColumns` в `connect()` добавит колонку автоматически.
Migration scripts не нужны.

**`saveChunk(entity: ChunkEntity, embedding: FloatArray): Long`**:
- В `CodeChunksTable.insert {}`: добавить `it[parentScope] = entity.parentScope`

**`getAllVectors(strategy: String): List<Pair<ChunkEntity, FloatArray>>`**:
- В маппинге `ChunkEntity(...)`: добавить `parentScope = row[CodeChunksTable.parentScope]`

### Критерий готовности Stage 2

Проект компилируется. `ChunkEntity` с новым полем доступен. БД стартует без ошибок.

---

## Stage 3: `AstChunkingStrategy`

**Цель**: заменить regex-based `StructuralStrategy` на AST-based разбивку Kotlin-файлов.
**Файл**: `rag-server/.../indexing/ChunkingStrategy.kt` — добавить рядом с `FixedSizeStrategy`.

### Алгоритм split-then-merge

**Проход 1 — Split** (рекурсивный обход AST):

Интересные типы нод (из спайка — верифицированные имена):
- `class_declaration`
- `function_declaration`
- `object_declaration`
- `companion_object`
- `property_declaration`
- `typealias_declaration`
- `interface_declaration`

Для каждой такой ноды создаётся кандидат-чанк:
- `text` = `node.text().toString()`
- `startLine` = `node.startPoint.row.toInt() + 1`
- `declarationName` = `node.childByFieldName("name")?.text()?.toString()`
- `parentScope` = имя enclosing-класса/объекта, если нода вложена; иначе `null`

Определение вложенности:
- Нода считается вложенной, если `node.parent?.type` — `class_body` или `object_declaration`
- `parentScope` = `node.parent?.parent?.childByFieldName("name")?.text()?.toString()`

**Проход 2 — Merge** (жадное слияние):

- Параметр `maxChunkSize: Int = 2000` (символы)
- Пока следующий кандидат влезает в лимит — объединяем текст
- `declarationName` берётся от первого кандидата в группе
- `parentScope` сохраняется (не меняется при слиянии)

**Особые случаи:**
- Файл целиком < `maxChunkSize` → один чанк, без обхода AST
- Одна нода > `maxChunkSize` → sub-split через `FixedSizeStrategy`, с сохранением `declarationName` и `parentScope`

### `AstChunkingStrategy` — описание класса

Реализует `ChunkingStrategy`:
- `override val strategyName = "structural"` — совместимость с DB
- `override fun split(content: String, filePath: String, fileName: String): List<ChunkEntity>`
- Конструктор: принимает `language: Language` (из `io.github.treesitter.ktreesitter`), `maxChunkSize: Int = 2000`
- `Parser(language)` создаётся один раз в init

Companion object:
- `fun create(maxChunkSize: Int = 2000): AstChunkingStrategy`
- Внутри: `Language(TreeSitterKotlin.language())` — единственное место с импортом из `:rag-grammar`

### API-нюансы (верифицированы в спайке)

| Аспект | Факт |
|---|---|
| `node.startPoint.row` | `UInt` → нужен `.toInt()` для арифметики |
| `node.text()` | возвращает `CharSequence?` → `.toString()` для строк |
| `node.children` | `List<Node>` — использовать вместо `node.child(Int)` |
| `node.childByFieldName("name")` | работает для `class_declaration`, `function_declaration`, `object_declaration` |
| `node.type` | строковые константы: `"class_declaration"`, `"function_declaration"`, `"companion_object"` и др. |
| `Parser` | не потокобезопасен — создавать по одному на поток или синхронизировать |

### Критерий готовности Stage 3

Запустить rag-server с `FORCE_REINDEX=true`.
Лог: `IndexingService: 'structural' done — N chunks saved`, N > 0.
Через `/search` — запрос про конкретный класс → чанк начинается с объявления (`class`/`fun`), не с середины строки.

---

## Stage 4: `MarkdownChunkingStrategy` + `LanguageAwareChunker`

**Цель**: стратегия для Markdown и роутер по типу файла.
**Файл**: `rag-server/.../indexing/ChunkingStrategy.kt` — добавить рядом с `AstChunkingStrategy`.

### `MarkdownChunkingStrategy` — описание класса

Реализует `ChunkingStrategy`:
- `override val strategyName = "structural"`
- `override fun split(content: String, filePath: String, fileName: String): List<ChunkEntity>`
- Конструктор: `maxChunkSize: Int = 2000`

Алгоритм:
1. Разбить по заголовкам: regex `(?=\n#{1,3} )`
2. Каждый раздел = кандидат-чанк
3. `declarationName` = текст первой строки-заголовка (без `#` и пробела)
4. `parentScope` = `null`
5. Merge-логика: жадное слияние по `maxChunkSize`, аналогично AstChunkingStrategy
6. Если раздел > `maxChunkSize`: sub-split через `FixedSizeStrategy`

### `LanguageAwareChunker` — описание класса

Роутер-фасад, **не реализует** `ChunkingStrategy`.

Поля:
- `astStrategy: AstChunkingStrategy`
- `markdownStrategy: MarkdownChunkingStrategy`
- `fixedStrategy: FixedSizeStrategy`
- `strategyName: String = "structural"` — для совместимости с `indexStrategy()`

Метод:
- `fun split(content: String, filePath: String, fileName: String): List<ChunkEntity>`

Роутинг по расширению из `fileName`:
- `.kt`, `.kts` → `astStrategy`
- `.md` → `markdownStrategy`
- остальное → `fixedStrategy`

### `FileScanner.kt`

`INCLUDED_EXTENSIONS`: добавить `"txt"`.

### Критерий готовности Stage 4

`LanguageAwareChunker` компилируется. Ручная проверка в `main` или тестом:
- `.md`-контент → чанки по заголовкам
- `.kt`-контент → чанки по AST-нодам
- `.txt`-контент → fixed chunks

---

## Stage 5: Интеграция — IndexingService + TwoStageSearchService

**Цель**: подключить `LanguageAwareChunker` в pipeline и улучшить drillDown.

### `IndexingService.kt`

Текущее:
```
val strategies = listOf(FixedSizeStrategy(), StructuralStrategy())
for (strategy in strategies) { indexStrategy(strategy, files, config.forceReindex) }
```

Новое:
- Создать `LanguageAwareChunker` (в `indexAll` — не в конструкторе, чтобы не ломать DI)
- `AstChunkingStrategy.create()` создаётся внутри `LanguageAwareChunker`
- Два вызова `indexStrategy`: `FixedSizeStrategy()` (для "fixed" индекса) + `LanguageAwareChunker` (для "structural")
- `FixedSizeStrategy` остаётся: `IndexStats.fixedChunks` его считает
- `StructuralStrategy` класс удаляется из `ChunkingStrategy.kt`

Конструктор `IndexingService` не меняется.

### `TwoStageSearchService.kt`

Метод `drillDown()`: текущая фильтрация по `fileName` и `declarationName`.

Добавить третье условие:
- `chunk.parentScope?.contains(classMeta.className, ignoreCase = true) == true`

Позволяет найти методы с `parentScope = "GraphAIAgent"` при drillDown по классу `GraphAIAgent`.

### `rag-server/build.gradle.kts`

В `dependencies`: `implementation(project(":rag-grammar"))`

### Критерий готовности Stage 5 (end-to-end)

1. `./gradlew :rag-server:shadowJar` — собирается без ошибок
2. Запуск с `FORCE_REINDEX=true`
3. Лог: `IndexingService: 'structural' done — N chunks`, `IndexingService: 'fixed' done — M chunks`
4. `GET /search` с вопросом про конкретный класс → осмысленный чанк с кодом класса
5. `parentScope != null` для методов внутри классов (DB inspector или дополнительный лог)
6. LLM-реранкер: `score > 0` для релевантных чанков

---

## Последовательность выполнения

```
Stage 1 ──┐
          ├──► Stage 3 → Stage 4 → Stage 5
Stage 2 ──┘
```

Stage 1 и Stage 2 независимы — можно параллельно.
Stage 3 требует Stage 1 (нужен `TreeSitterKotlin`) и Stage 2 (нужен `parentScope` в `ChunkEntity`).
Каждый stage атомарен: после него проект компилируется и (если возможно) запускается.

---

## Открытые вопросы

1. Актуальный тег `fwcd/tree-sitter-kotlin` для download task — проверить на GitHub перед Stage 1.
2. После Stage 5: удалить `StructuralStrategy` или оставить как deprecated fallback?
