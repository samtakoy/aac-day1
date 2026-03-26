# rag-grammar

Gradle-модуль, который компилирует нативные grammar-библиотеки tree-sitter и предоставляет JVM-обёртки для использования в `rag-server`.

---

## Что это и зачем

`rag-server` использует [ktreesitter](https://github.com/tree-sitter/kotlin-tree-sitter) для AST-парсинга исходных файлов. Это позволяет нарезать код на семантически корректные чанки (функции, классы, объекты) — в отличие от regex-подхода, который не понимает вложенность.

Нативные grammar-библиотеки tree-sitter написаны на C и требуют компиляции под целевую платформу. `rag-grammar` инкапсулирует весь этот процесс:

1. **Vendored исходники грамматики** — `parser.c` + `scanner.c` лежат в репо, не нужен git submodule при сборке.
2. **CMake build** — компилирует нативную `.dylib`/`.so`/`.dll` как часть Gradle-задачи.
3. **JVM wrapper** — `KotlinLanguage` загружает нативную либу (из classpath или temp-файла) и передаёт `language()` в ktreesitter `Parser`.

---

## Структура модуля

```
rag-grammar/
├── build.gradle.kts                     — конфигурация: ktreesitter-plugin, CMake tasks
├── grammar/
│   └── kotlin/
│       ├── src/
│       │   ├── parser.c                 — vendored из fwcd/tree-sitter-kotlin
│       │   ├── scanner.c                — vendored из fwcd/tree-sitter-kotlin
│       │   └── tree_sitter/             — заголовки tree-sitter runtime
│       │       ├── parser.h
│       │       ├── array.h
│       │       └── alloc.h
│       └── bindings/
│           └── c/
│               └── tree-sitter-kotlin.h — C binding header (нужен CMake)
└── src/
    └── main/kotlin/com/example/day/raggrammar/
        └── KotlinLanguage.kt            — JVM wrapper (вручную, без KMP expect/actual)
```

---

## Как работает сборка

```
generateGrammarFiles (ktreesitter-plugin)
        │
        ▼  генерирует build/generated/CMakeLists.txt
cmakeConfigure
        │
        ▼  cmake <generated-dir> -DCMAKE_BUILD_TYPE=Release
cmakeBuild
        │
        ▼  cmake --build . → libkotlin.dylib / libkotlin.so / kotlin.dll
copyNativeLib
        │
        ▼  копирует в build/resources/main/lib/{os}/{arch}/
processResources  ← dylib попадает в classpath jar
compileKotlin     ← KotlinLanguage.kt компилируется
jar               ← готов к использованию как :rag-grammar dependency
```

Нативная библиотека пакуется внутрь jar в `lib/{os}/{arch}/`. При первом обращении к `KotlinLanguage.language()` она распаковывается во временный файл и загружается через JNI.

---

## Публичный API

Единственный публичный класс:

```
KotlinLanguage.language(): Any
```

Возвращает opaque объект `Long` (указатель на C-структуру `TSLanguage`), который передаётся в `io.github.treesitter.ktreesitter.Language`:

```kotlin
val language = Language(KotlinLanguage.language())
val parser = Parser(language)
val tree = parser.parse(sourceCode)
```

---

## Требования для сборки

- **JDK 17+**
- **cmake** — должен быть доступен в `PATH`
- **C компилятор** — `clang` (macOS), `gcc` (Linux), MSVC (Windows)

На macOS:
```bash
# Проверить
cmake --version
clang --version

# Установить если отсутствует
xcode-select --install
brew install cmake
```

---

## Сборка

```bash
# Только rag-grammar jar (с нативной либой внутри)
./gradlew :rag-grammar:jar

# Вместе с rag-server (транзитивно)
./gradlew :rag-server:build
```

После сборки:
- `rag-grammar/build/libs/rag-grammar-1.0.0.jar` — jar с `KotlinLanguage.class` и `lib/macos/aarch64/libkotlin.dylib`
- `rag-grammar/build/cmake-build/libkotlin.dylib` — нативная lib

---

## Текущие grammars

| Язык | Источник | Файлы |
|------|----------|-------|
| Kotlin | [fwcd/tree-sitter-kotlin](https://github.com/fwcd/tree-sitter-kotlin) | `grammar/kotlin/src/parser.c`, `scanner.c` |

---

## Добавление нового языка (пример: Python)

### Шаг 1. Получить исходники грамматики

```bash
# Клонировать официальный репо
git clone https://github.com/tree-sitter/tree-sitter-python /tmp/ts-python

# Скопировать необходимые файлы
mkdir -p rag-grammar/grammar/python/src
cp /tmp/ts-python/src/parser.c  rag-grammar/grammar/python/src/
cp /tmp/ts-python/src/scanner.c rag-grammar/grammar/python/src/  # если есть

# Скопировать tree-sitter headers (те же что у kotlin — общие для всех grammars)
cp -r /tmp/ts-python/src/tree_sitter/ rag-grammar/grammar/python/src/

# Скопировать C binding header
mkdir -p rag-grammar/grammar/python/bindings/c
cp /tmp/ts-python/bindings/c/tree-sitter-python.h rag-grammar/grammar/python/bindings/c/
```

> Если в репо нет `bindings/c/` — создать заголовок вручную по образцу `tree-sitter-kotlin.h`:
> ```c
> #ifndef TREE_SITTER_PYTHON_H_
> #define TREE_SITTER_PYTHON_H_
> typedef struct TSLanguage TSLanguage;
> #ifdef __cplusplus
> extern "C" {
> #endif
> const TSLanguage *tree_sitter_python(void);
> #ifdef __cplusplus
> }
> #endif
> #endif
> ```

### Шаг 2. Решить: один модуль или отдельный

**Вопрос** — поддерживает ли `ktreesitter-plugin` несколько блоков `grammar {}` в одном модуле?

- **Если да** (нужно проверить): добавить второй блок в `rag-grammar/build.gradle.kts`.
- **Если нет**: создать отдельный модуль `:rag-grammar-python` с аналогичным `build.gradle.kts`.

Рекомендуется сначала проверить через отдельный модуль — изолированно, без риска сломать Kotlin grammar.

### Шаг 3. Создать JVM wrapper

```
rag-grammar/src/main/kotlin/com/example/day/raggrammar/PythonLanguage.kt
```

По аналогии с `KotlinLanguage.kt`:
- `LIB_NAME = "python"`
- `private external fun tree_sitter_python(): Long`
- Путь к нативной либе: `/lib/{os}/{arch}/libpython.{ext}`

### Шаг 4. Добавить `PythonChunkingStrategy` в `rag-server`

В `ChunkingStrategy.kt`:
```kotlin
class PythonChunkingStrategy(maxChunkSize: Int = 2000) : ChunkingStrategy {
    companion object {
        fun create(maxChunkSize: Int = 2000): PythonChunkingStrategy {
            val language = Language(PythonLanguage.language())
            // ...
        }
    }
    // Интересные node types для Python:
    // "function_definition", "class_definition", "decorated_definition"
}
```

### Шаг 5. Добавить роутинг в `LanguageAwareChunker`

```kotlin
".py" && useAst -> pythonStrategy.split(content, filePath, fileName)
```

### Шаг 6. Добавить `.py` в `FileScanner.INCLUDED_EXTENSIONS`

```kotlin
private val INCLUDED_EXTENSIONS = setOf("kt", "kts", "md", "txt", "py")
```

---

## Обновление grammars

Grammars обновляются редко (грамматика языка меняется медленно). При необходимости:

```bash
# Пример обновления Kotlin grammar
git clone https://github.com/fwcd/tree-sitter-kotlin /tmp/ts-kotlin-new
cp /tmp/ts-kotlin-new/src/parser.c  rag-grammar/grammar/kotlin/src/
cp /tmp/ts-kotlin-new/src/scanner.c rag-grammar/grammar/kotlin/src/

# Пересобрать
./gradlew :rag-grammar:jar --rerun-tasks
```

> `parser.c` — автогенерируется из `grammar.js` и стабилен. `scanner.c` — написан вручную, меняется редко.
> Нет нужды обновлять при каждом релизе grammar-репо.

---

## Troubleshooting

### `UnsatisfiedLinkError: no kotlin in java.library.path`

Нативная либа не найдена. `KotlinLanguage` пытается извлечь её из classpath. Убедитесь что `rag-grammar:jar` был собран и находится в classpath.

```bash
# Проверить что dylib есть в jar
jar tf rag-grammar/build/libs/rag-grammar-1.0.0.jar | grep ".dylib\|.so\|.dll"
# Ожидание: lib/macos/aarch64/libkotlin.dylib
```

### `tree-sitter-kotlin.h file not found` при сборке

В `rag-grammar/grammar/kotlin/bindings/c/` должен быть заголовочный файл. CMakeLists ищет его по пути `../../grammar/kotlin/bindings/c`.

### `cmake: command not found`

```bash
brew install cmake    # macOS
apt install cmake     # Ubuntu/Debian
```
