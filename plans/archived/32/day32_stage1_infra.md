# Этап 1: GitHub Actions + MCP Server GitHub API tools

## Общее описание

Подготовка инфраструктуры для PR-уведомлений и инструментов для работы с Pull Request-ами через GitHub API.

**Что получим:**
- GitHub Actions workflow, который при открытии/обновлении PR отправляет JSON-сообщение в Telegram
- 4 новых MCP инструмента для работы с PR (get_pr_info, get_pr_diff, get_pr_file_diff, add_pr_review_comment)
- Расширение get_git_file_list с опциональным фильтром по паттерну

**Критерии успеха:**
- При открытии нового PR в GitHub репозитории приходит Telegram-сообщение в формате `{"event":"pr_opened","repo":"owner/repo","pr_number":42,"title":"..."}`
- MCP Inspector (или curl) успешно вызывает `get_pr_info` и возвращает JSON с данными PR
- `get_pr_file_diff` возвращает patch конкретного файла
- `add_pr_review_comment` создаёт комментарий в PR на GitHub
- `get_git_file_list` с параметром `pattern="*.kt"` возвращает только Kotlin-файлы

---

## Задача 1.1: GitHub Actions Workflow

### Файл для создания

**`.github/workflows/pr_review_notify.yml`** (в корне проекта)

### Описание

Workflow запускается на события `opened` и `synchronize` для pull_request. Единственный шаг — curl запрос к Telegram Bot API.

### Детали реализации

**Триггер:**
- `on: pull_request: types: [opened, synchronize]`

**Секреты репозитория (нужно добавить в GitHub Settings → Secrets):**
- `TELEGRAM_BOT_TOKEN` — токен бота (формат: `123456:ABC-DEF...`)
- `TELEGRAM_CHAT_ID` — chat_id Telegram-чата/группы куда отправлять (может быть отрицательным для групп, например `-100123456789`)

**Переменные из контекста GitHub Actions:**
- `${{ github.repository }}` → `"owner/repo"`
- `${{ github.event.pull_request.number }}` → номер PR (Int)
- `${{ github.event.pull_request.title }}` → заголовок PR

**Команда отправки:**
```
curl -s -X POST \
  "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendMessage" \
  -H "Content-Type: application/json" \
  -d "{\"chat_id\":\"${TELEGRAM_CHAT_ID}\",\"text\":\"{\\\"event\\\":\\\"pr_opened\\\",\\\"repo\\\":\\\"${REPO}\\\",\\\"pr_number\\\":${PR_NUMBER},\\\"title\\\":\\\"${PR_TITLE}\\\"}\",\"parse_mode\":\"\"}"
```

Безопаснее использовать env-переменные для экранирования. Примерная структура:
```yaml
jobs:
  notify:
    runs-on: ubuntu-latest
    steps:
      - name: Notify Telegram about PR
        env:
          TELEGRAM_BOT_TOKEN: ${{ secrets.TELEGRAM_BOT_TOKEN }}
          TELEGRAM_CHAT_ID: ${{ secrets.TELEGRAM_CHAT_ID }}
          PR_NUMBER: ${{ github.event.pull_request.number }}
          PR_TITLE: ${{ github.event.pull_request.title }}
          REPO: ${{ github.repository }}
        run: |
          MESSAGE="{\"event\":\"pr_opened\",\"repo\":\"$REPO\",\"pr_number\":$PR_NUMBER,\"title\":\"$PR_TITLE\"}"
          curl -s -X POST "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendMessage" \
            -H "Content-Type: application/json" \
            -d "{\"chat_id\":\"${TELEGRAM_CHAT_ID}\",\"text\":\"${MESSAGE}\"}"
```

### Важно

Кавычки в `PR_TITLE` могут сломать JSON. Для демо-версии допустимо, но в проде нужно экранировать. На этапе тестирования использовать PR-тайтлы без специальных символов.

---

## Задача 1.2: MCP Server — новые GitHub PR инструменты

### Файлы для изменения

**`mcp-server/src/main/kotlin/com/example/day/mcpserver/tools/McpTools.kt`**

Посмотреть структуру существующих инструментов в этом файле. GitHub токен уже используется в существующих инструментах (`get_issue`, `list_issues` и др.) — взять тот же механизм получения токена.

### Как получается GitHub token в mcp-server

В `McpTools.kt` токен читается из переменной окружения:
```kotlin
val githubToken = System.getenv("GITHUB_TOKEN") ?: ""
```
Запуск сервера требует:
```powershell
$env:GITHUB_TOKEN="ghp_..."
$env:GITHUB_OWNER="org"   # опционально — если нужен дефолтный owner
$env:GITHUB_REPO="repo"   # опционально — если нужен дефолтный repo
```
Для новых инструментов использовать тот же `githubToken` — он уже доступен в том же файле.

### Инструмент: `get_pr_info`

**Название:** `"get_pr_info"`
**Описание:** "Получить информацию о Pull Request: заголовок, описание, статус, автор, список изменённых файлов и SHA коммита. Используй для получения обзора PR перед ревью."

**Параметры (inputSchema):**
- `pr_number` (integer, required) — номер PR
- `repo` (string, required) — репозиторий в формате "owner/repo"

**Реализация:**
1. GET `https://api.github.com/repos/{repo}/pulls/{pr_number}` с заголовком `Authorization: Bearer {token}`, `Accept: application/vnd.github+json`
2. GET `https://api.github.com/repos/{repo}/pulls/{pr_number}/files` — тот же заголовок
3. Собрать результат в JSON:
```json
{
  "number": 42,
  "title": "Fix something",
  "description": "PR body text",
  "state": "open",
  "author": "username",
  "head_sha": "abc123",
  "files": [
    {"path": "app/src/.../Foo.kt", "status": "modified", "additions": 10, "deletions": 3}
  ]
}
```
4. Вернуть как `CallToolResult(content = listOf(TextContent(text = jsonString)))`

**Обработка ошибок:** если запрос вернул не 2xx — вернуть текст ошибки в `CallToolResult`.

---

### Инструмент: `get_pr_diff`

**Название:** `"get_pr_diff"`
**Описание:** "Получить полный diff Pull Request. Возвращает текст в формате unified diff."

**Параметры:**
- `pr_number` (integer, required)
- `repo` (string, required)

**Реализация:**
1. GET `https://api.github.com/repos/{repo}/pulls/{pr_number}`
2. Заголовок: `Accept: application/vnd.github.v3.diff` (вместо обычного json)
3. Вернуть тело ответа как есть (это текст diff-а)

---

### Инструмент: `get_pr_file_diff`

**Название:** `"get_pr_file_diff"`
**Описание:** "Получить diff конкретного файла из Pull Request. Используй для детального анализа изменений в одном файле."

**Параметры:**
- `pr_number` (integer, required)
- `repo` (string, required)
- `file_path` (string, required) — путь к файлу, например "app/src/main/java/com/example/Foo.kt"

**Реализация:**
1. GET `https://api.github.com/repos/{repo}/pulls/{pr_number}/files` (JSON, обычный Accept)
2. Найти объект в массиве, где `"filename" == file_path`
3. Вернуть поле `"patch"` этого объекта (это diff для данного файла)
4. Если файл не найден — вернуть сообщение об ошибке

---

### Инструмент: `add_pr_review_comment`

**Название:** `"add_pr_review_comment"`
**Описание:** "Добавить review-комментарий к конкретной строке файла в Pull Request на GitHub. Используй когда нашёл конкретную проблему в коде."

**Параметры:**
- `pr_number` (integer, required)
- `repo` (string, required)
- `file_path` (string, required) — путь к файлу
- `body` (string, required) — текст комментария
- `line` (integer, required) — номер строки в файле (в новой версии файла)
- `commit_id` (string, required) — SHA коммита (head_sha из get_pr_info)

**Реализация:**
1. POST `https://api.github.com/repos/{repo}/pulls/{pr_number}/comments`
2. Заголовки: `Authorization: Bearer {token}`, `Accept: application/vnd.github+json`, `Content-Type: application/json`
3. Тело:
```json
{
  "body": "комментарий",
  "commit_id": "head_sha",
  "path": "path/to/File.kt",
  "line": 42,
  "side": "RIGHT"
}
```
4. Вернуть: `{"id": 12345, "status": "created"}` или текст ошибки

**Примечание:** `"side": "RIGHT"` означает комментарий к новой версии файла (после изменений). `line` — это номер строки в новой версии файла. Если строка не входит в diff этого PR — GitHub вернёт ошибку 422. Агент должен использовать строки из patch (из `get_pr_file_diff`).

---

### Задача 1.3: Расширение `get_git_file_list`

**Файл:** `mcp-server/src/main/kotlin/com/example/day/mcpserver/tools/McpTools.kt`

Найти существующую реализацию `get_git_file_list`. Добавить опциональный параметр `pattern`.

**Изменение inputSchema** — добавить:
- `pattern` (string, optional) — glob-паттерн для фильтрации, например "*.kt", "app/src/**/*.kt"

**Изменение логики:**
- Если `pattern` передан и не пустой: выполнить `git ls-files "{pattern}"`
- Если `pattern` не передан: выполнить `git ls-files` (поведение без изменений, обратная совместимость сохранена)

---

## Что потребуется для тестирования этапа

1. Запущенный `mcp-server` локально
2. MCP Inspector или curl для вызова инструментов
3. Telegram-бот и чат для проверки отправки сообщений
4. GitHub репозиторий с добавленными секретами `TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID`
5. Создать тестовый PR → убедиться что в Telegram пришло сообщение в нужном JSON-формате
