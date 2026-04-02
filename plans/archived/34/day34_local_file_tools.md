# День 34. Локальные файловые инструменты MCP

## Контекст и мотивация

Текущие инструменты MCP работают через GitHub API (remote). Цель — добавить инструменты
для чтения и записи **локальных** файлов проекта, что позволит агенту непосредственно
изменять исходный код на устройстве.

MCP-сервер уже запущен на том же устройстве и уже знает `projectPath` — путь к проекту.

---

## Текущее состояние инструментов (диагностика)

| Инструмент | Источник | Проблема |
|---|---|---|
| `get_current_git_branch` | **локальный git** | порядок |
| `get_git_file_list` | **GitHub API** | название вводит в заблуждение |
| `get_file_content` | **GitHub API** | не видит незапушенные ветки |

Несоответствие: `get_current_git_branch` возвращает локальную ветку, а `get_file_content(branch=...)` 
требует что ветка есть на remote. Если ветка не запушена — разъезжается.

---

## Что добавляем

### 1. `read_local_file`

Читает файл из `projectPath` по относительному пути.

```kotlin
// параметры
file_path: String  // обязательный, напр. "/app/src/main/.../Foo.kt"

// реализация
val file = File(projectPath, filePath.trimStart('/'))
file.readText(Charsets.UTF_8)
```

**Защита от path traversal:**
```kotlin
val resolved = file.canonicalPath
val root = File(projectPath).canonicalPath
require(resolved.startsWith(root)) { "Access denied: path outside project" }
```

---

### 2. `write_local_file`

Создаёт или перезаписывает файл. Автоматически создаёт промежуточные директории.

```kotlin
// параметры
file_path: String   // обязательный
content: String     // обязательный — полное новое содержимое файла

// реализация
val file = File(projectPath, filePath.trimStart('/'))
file.parentFile.mkdirs()
file.writeText(content, Charsets.UTF_8)
```

---

### 3. `list_local_files`

Обходит `projectPath` локально (без GitHub API). Поддерживает glob-паттерн.
Всегда актуален — отражает что есть на диске, включая незапушенные изменения.

```kotlin
// параметры
pattern: String?  // опциональный glob, напр. "**/*.kt"

// реализация
File(projectPath).walk()
    .filter { it.isFile }
    .map { "/" + it.relativeTo(File(projectPath)).path.replace('\\', '/') }
    .filter { pattern == null || globToRegex(pattern).matches(it.trimStart('/')) }
    .toList()
```

---

## Место в коде

**Файл:** `mcp-server/src/main/kotlin/com/example/day/mcpserver/tools/McpTools.kt`

### Новые константы в `GitHubToolNames`

```kotlin
// Day 34: Local file tools
const val READ_LOCAL_FILE = "read_local_file"
const val WRITE_LOCAL_FILE = "write_local_file"
const val LIST_LOCAL_FILES = "list_local_files"
```

### Регистрация в `registerMcpTools()`

```kotlin
// Day 34: Local file tools
registerReadLocalFile(server, projectPath)
registerWriteLocalFile(server, projectPath)
registerListLocalFiles(server, projectPath)
```

---

## Соображения по безопасности

- Все три инструмента ограничены `projectPath` — проверка через `canonicalPath`
- `write_local_file` перезаписывает файл целиком; нет patch/diff-режима (можно добавить позже)
- Не добавляем инструмент удаления файлов (избыточно, деструктивно)

---

## Почему НЕ переименовываем существующие инструменты

`get_git_file_list` и `get_file_content` оставляем как есть — у них своя ниша (сравнение 
веток на GitHub, PR-ревью). Добавляем параллельный локальный набор с явным префиксом `local`.

LLM сможет сам выбирать нужный инструмент по описанию:
- нужен файл из другой ветки → `get_file_content(branch=...)`
- нужен текущий файл на диске → `read_local_file`
- нужно изменить файл → `write_local_file`

---

## Критерии успеха

- [ ] `read_local_file` возвращает содержимое файла с диска
- [ ] `write_local_file` создаёт/перезаписывает файл, создаёт директории
- [ ] `list_local_files` возвращает актуальный список с диска (glob работает)
- [ ] Path traversal за пределы `projectPath` возвращает ошибку
- [ ] Компилируется без ошибок, существующие инструменты не затронуты
