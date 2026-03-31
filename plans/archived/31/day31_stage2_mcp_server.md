# Stage 2: Доработки McpServer — get_current_git_branch

## Описание
Добавить в MCP-сервер инструмент, возвращающий текущую git-ветку проекта. Инструмент выполняет `git branch --show-current` через shell на машине, где запущен mcp-server, с поддержкой настройки пути к проекту через env-переменную.

## Файлы для изменения

### 1. `mcp-server/src/main/kotlin/com/example/day/mcpserver/McpServer.kt`

**Что меняется**:
- При старте сервера (в `fun main()`) читать env-переменную `GIT_PROJECT_PATH`: `System.getenv("GIT_PROJECT_PATH") ?: "."`
- Передавать `projectPath: String` в `registerMcpTools(server, api, projectPath)`

### 2. `mcp-server/src/main/kotlin/com/example/day/mcpserver/tools/McpTools.kt`

**Что меняется в `GitHubToolNames`**:
- Добавить константу: `GET_CURRENT_GIT_BRANCH = "get_current_git_branch"`

**Что меняется в `registerMcpTools(server, api)`**:
- Добавить параметр: `projectPath: String`
- Добавить вызов: `registerGetCurrentGitBranch(server, projectPath)`

**Новая функция `registerGetCurrentGitBranch(server, projectPath)`**:
- Имя инструмента: `GitHubToolNames.GET_CURRENT_GIT_BRANCH`
- Описание: `"Возвращает текущую ветку git-репозитория проекта"`
- Входные параметры: нет (пустая `inputSchema`)
- Логика:
  1. Создать `ProcessBuilder("git", "-C", projectPath, "branch", "--show-current").start()`
  2. Дождаться завершения: `process.waitFor(5, TimeUnit.SECONDS)` (импорт `java.util.concurrent.TimeUnit`)
  3. Если `waitFor` вернул `false` (timeout) — `process.destroyForcibly()`, вернуть `isError = true, content = "Timeout: git не ответил за 5 секунд"`
  4. Прочитать: `val branchName = process.inputStream.bufferedReader().readText().trim()`
  5. Если `process.exitValue() != 0` или `branchName.isBlank()` — прочитать `process.errorStream.bufferedReader().readText().trim()` и вернуть `isError = true, content = "Не удалось получить ветку: <stderr>"`
  6. Иначе — вернуть `CallToolResult(content = [TextContent(text = branchName)])`

**Обработка исключений**: весь блок обернуть в `try/catch(Exception)`, при ошибке вернуть `isError = true, content = e.message`

## Резюме
**Что получим**: MCP-инструмент `get_current_git_branch`, доступный ассистенту разработчика для ответа на вопрос "в какой ветке сейчас идёт работа".

**Критерии успеха**:
- McpServer компилируется и запускается
- Инструмент `get_current_git_branch` присутствует в списке MCP-инструментов
- Вызов инструмента возвращает имя текущей ветки git (например, `day31_project_docs`)
- При невалидном `GIT_PROJECT_PATH` возвращается понятное сообщение об ошибке, сервер не падает
