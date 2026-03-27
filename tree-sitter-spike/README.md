# tree-sitter-spike

Исследовательский модуль (spike). Не используется в production. Цель — проверить гипотезы
перед реализацией AST-чанкинга в `rag-server`.

---

## Зачем

`rag-server` использует `StructuralStrategy` для нарезки Kotlin-файлов на чанки.
Стратегия основана на regex и не понимает вложенность — `fun` внутри лямбды,
`companion object` внутри класса — всё триггерит сплит не в том месте.
Большие блоки sub-split'ятся через `FixedSizeStrategy` посередине строки.
Результат: LLM-реранкер получает бессмысленные фрагменты → score=0.00 → пустой ответ.

Решение — AST-based chunking через `ktreesitter` (официальные Kotlin Multiplatform
биндинги к tree-sitter). Перед реализацией нужно было убедиться что:

1. Библиотека резолвится из Maven Central
2. Fat jar (`duplicatesStrategy = EXCLUDE`) совместим с нативными JNI либами
3. Kotlin grammar можно подключить без сложного инструментария
4. API работает как ожидается (парсинг, обход дерева, source text)

---

## Что сделано

### 1. Проверка core library

Добавлена зависимость `io.github.tree-sitter:ktreesitter:0.24.1`.
Собран fat jar по той же схеме что в `rag-server` (`duplicatesStrategy = EXCLUDE`).

**Результат:** нативные либы лежат в `lib/macos/aarch64/libktreesitter.dylib` —
уникальные пути, конфликтов нет. JNI загружается, `Parser` инстанциируется.

### 2. Поиск языковых артефактов

Проверено: `io.github.tree-sitter:ktreesitter-lang-java:0.24.1` — **не существует**.
Языковые грамматики не публикуются как отдельные Maven артефакты.

**Вывод:** нужен `ktreesitter-plugin` + C-исходники грамматики.

### 3. ktreesitter-plugin

Плагин `io.github.tree-sitter.ktreesitter-plugin:0.24.1` резолвится из Gradle Plugin Portal.
Требует CMake для компиляции C-исходников грамматики.

**Важное открытие:** плагин генерирует KMP-код (`actual object`, `actual fun`).
В JVM-only проекте это не компилируется. Решение — ручная обёртка `KotlinLanguage.kt`
по шаблону из плагина, без `actual/expect`.

### 4. Kotlin grammar (fwcd/tree-sitter-kotlin)

Клонированы исходники: `tree-sitter-kotlin/src/parser.c` + `scanner.c`.
В `bindings/c/` лежат только шаблонные `.h.in` файлы — нужно раскрыть вручную.
Создан `tree-sitter-kotlin.h` из шаблона.

CMake скомпилировал `libkotlin.dylib` успешно.

### 5. Полный end-to-end тест

`Spike.kt` парсит реальный Kotlin-код с классом, методами и `companion object`.

---

## Результаты

```
=== ktreesitter spike: Kotlin grammar ===

[1] Loading Kotlin grammar native library...
    OK: Language loaded, symbolCount=375, fieldCount=4

[2] Parsing Kotlin source...
    Root node type: source_file
    Has errors: false
    Child count: 3

[3] Top-level declarations (companion object должен отсутствовать):
    [top] type=package_header  lines=1..1
    [top] type=class_declaration  lines=3..11
    [top] type=function_declaration  lines=13..13

[4] Inside class body (companion object MUST be here):
    — companion object внутри класса, не на top-level ✅

[5] Source text extraction:
    class source length: 191
    first 60 chars: class GraphAIAgent(val name: String) {     fun runSession(in
```

| Проверка | Результат |
|---|---|
| `ktreesitter:0.24.1` из Maven Central | ✅ |
| Fat jar + `duplicatesStrategy = EXCLUDE` | ✅ нативники не конфликтуют |
| JNI загружается в рантайме | ✅ |
| `ktreesitter-plugin` из Gradle Plugin Portal | ✅ |
| Kotlin grammar C-исходники (fwcd) | ✅ |
| CMake компиляция → `libkotlin.dylib` | ✅ |
| Парсинг Kotlin, корректное AST | ✅ |
| Companion object внутри класса, не top-level | ✅ ключевая проверка |
| `node.text()`, `node.children`, field access | ✅ |
| rag-server не сломан | ✅ |

---

## Зафиксированные особенности API

Важно учесть при реализации `AstChunkingStrategy` в rag-server:

| Аспект | Факт |
|---|---|
| `Point.row` / `Point.column` | `UInt` в Kotlin, нужен `.toInt()` для арифметики |
| `node.text()` | возвращает `CharSequence?`, не `String` |
| `node.children` | `List<Node>`, удобнее чем `node.child(Int)` |
| `node.childByFieldName("name")` | поля зависят от типа ноды — нужна верификация |
| `node.type` | строка: `"class_declaration"`, `"function_declaration"`, `"object_declaration"`, ... |
| Плагин генерирует KMP-код | для JVM-only нужна ручная обёртка без `actual/expect` |
| `tree-sitter-kotlin.h` | не входит в репо fwcd, нужно создать из шаблона `.h.in` |

---

## Структура модуля

```
tree-sitter-spike/
├── build.gradle.kts              — зависимости, plugin, CMake задачи, fat jar
├── tree-sitter-kotlin/           — клон fwcd/tree-sitter-kotlin (grammar C-исходники)
│   ├── src/
│   │   ├── parser.c              — сгенерированный парсер (не редактировать)
│   │   └── scanner.c             — сканер (не редактировать)
│   └── bindings/c/
│       └── tree-sitter-kotlin.h  — создан вручную из шаблона .h.in
└── src/main/kotlin/com/example/spike/
    ├── Spike.kt                  — end-to-end тест
    └── grammar/
        └── KotlinLanguage.kt     — JVM-обёртка для grammar (без KMP actual/expect)
```

---

## Что нужно дальше

### В rag-server (реализация AstChunkingStrategy)

**Сборка:**
- Добавить `ktreesitter:0.24.1` в `rag-server/build.gradle.kts`
- Настроить CMake задачи для компиляции Kotlin grammar (по аналогии с spike)
- Скопировать `KotlinLanguage.kt` как основу для JVM-обёртки
- Включить `libkotlin.dylib` в fat jar как ресурс

**Новые классы:**
- `AstChunkingStrategy` — split-then-merge алгоритм, параметризован `Language`
- `MarkdownChunkingStrategy` — нарезка по заголовкам `#`/`##`/`###`
- `LanguageAwareChunker` — router: `.kt/.kts` → AST, `.md` → Markdown, остальное → Fixed

**Изменения существующих классов:**
- `ChunkEntity` — добавить `parentScope: String?`
- `CodeDatabase` — добавить колонку `parent_scope` (createMissingTablesAndColumns справится)
- `IndexingService` — заменить два strategy на `LanguageAwareChunker`
- `FileScanner` — добавить `.txt` в `INCLUDED_EXTENSIONS`
- `TwoStageSearchService.drillDown()` — добавить матчинг по `parentScope`

**Верификация node types в tree-sitter-kotlin:**
Нужно распечатать полное AST для реального файла из кодовой базы и подтвердить
точные имена: `class_declaration`, `function_declaration`, `object_declaration`,
`companion_object`, `property_declaration`, `import_list`, `package_header`.

**Верификация field names:**
`childByFieldName("name")` — работает не для всех типов нод.
Нужно проверить какие field names доступны для каждого объявления.
