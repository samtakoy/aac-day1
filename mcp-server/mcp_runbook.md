MCP GitHub Issues — Runbook (сборка, настройка, запуск, проверка)
Дата: 10 марта 2026

Цель
Запустить MCP сервер для GitHub Issues API и проверить работу клиента в Android приложении (ручные команды @@mcp и автоматический tool-call в @@talk).

Предпосылки
1) Установлен JDK 17.
2) Доступен GitHub Personal Access Token (PAT) с правами:
   - public репо: `public_repo`
   - private репо: `repo`
3) Заданы переменные окружения для сервера.

Секция 1. Сборка и запуск сервера

Шаг 1. Установить переменные окружения (PowerShell)
1) Перейти в корень проекта.
2) Задать переменные:
   $env:GITHUB_TOKEN="ghp_..."
   $env:GITHUB_OWNER="org"   # опционально
   $env:GITHUB_REPO="repo"   # опционально

Шаг 2. Запустить сервер
1) Запустить:
   ./gradlew :mcp-server:run
2) Сервер поднимается на:
   http://0.0.0.0:3000

Что проверить в выводе:
- Нет ошибок по GITHUB_TOKEN.
- Приложение не падает на старте.
- Порт 3000 свободен.

Секция 2. Проверка сервера через curl

Шаг 1. Initialize
curl -X POST http://localhost:3000/mcp `
  -H "Content-Type: application/json" `
  -H "Accept: application/json" `
  -d "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}"

Ожидаемо:
- JSON-RPC response с serverInfo и protocolVersion.

Шаг 2. tools/list
curl -X POST http://localhost:3000/mcp `
  -H "Content-Type: application/json" `
  -H "Accept: application/json" `
  -d "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}"

Ожидаемо:
- Список инструментов: echo, get_issue, list_issues, get_issue_comments, get_user, create_issue, create_comment.

Шаг 3. tools/call — echo
curl -X POST http://localhost:3000/mcp `
  -H "Content-Type: application/json" `
  -H "Accept: application/json" `
  -d "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":\"echo\",\"arguments\":{\"text\":\"Hello\"}}}"

Ожидаемо:
- Ответ с content.text = "Echo: Hello"

Шаг 4. tools/call — get_issue
curl -X POST http://localhost:3000/mcp `
  -H "Content-Type: application/json" `
  -H "Accept: application/json" `
  -d "{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/call\",\"params\":{\"name\":\"get_issue\",\"arguments\":{\"issueNumber\":1}}}"

Примечание:
- Если GITHUB_OWNER/REPO не заданы, передайте owner/repo в arguments.
- Ожидаемо: JSON задачи.

Секция 3. Настройка клиента (Android)

Шаг 1. Запустить сервер (из секции 1).

Шаг 2. Открыть MCP Settings в приложении.
1) Add Server:
   - Name: GitHub Issues
     - URL: http://10.0.2.2:3000
   - Transport: STREAMABLE_HTTP
   - URL Path: /mcp
   - Auth Token: пусто (сервер сам ходит в GitHub)
2) Нажать на карточку сервера, дождаться подключения.
3) Проверить, что список инструментов отображается.

На что обратить внимание:
- Если в эмуляторе — используем 10.0.2.2.
- Если физическое устройство — используем IP хоста.

Секция 4. Проверка клиента: ручные команды @@mcp

В чате выполните:
1) @@mcp echo text=Hello
   Ожидаемо: "MCP Result: Echo: Hello"

2) @@mcp list_issues per_page=3
   Ожидаемо: JSON со списком задач (PRs исключены по умолчанию).

3) @@mcp get_issue issueNumber=1
   Ожидаемо: JSON задачи.

4) @@mcp create_issue title=TestIssue body=Hello
   Ожидаемо: JSON созданной задачи.

5) @@mcp create_comment issueNumber=1 body=TestComment
   Ожидаемо: JSON созданного комментария.

Секция 5. Проверка клиента: авто tool-call через @@talk

Пример:
1) @@talk Создай issue с заголовком "Bug: crash on startup" и опиши проблему.
Ожидаемо:
- Агент вернет tool call JSON.
- Выполнится MCP вызов.
- В чате появится "MCP Result: { ... }".

Секция 6. Логирование и диагностика

Сервер:
- Смотрите вывод консоли, откуда запускали `:mcp-server:run`.

Клиент (Logcat):
- Теги: McpTools, McpWorker, TalkWorker.
- Ищите: "Calling MCP tool", "MCP tool failed".

Секция 7. Частые проблемы

1) 401/403 от GitHub API:
   - Проверьте токен и права.

2) 404 на issue:
   - Проверьте owner/repo или issueNumber.

3) Не подключается клиент:
   - Проверьте URL (10.0.2.2 для эмулятора).
   - Убедитесь, что сервер запущен и порт 3000 свободен.

4) PR попадают в список:
   - По умолчанию PR исключены.
   - Чтобы включить, используйте include_prs=true.

5) Rate limits:
   - GitHub API ограничивает запросы.
   - Используйте токен и не делайте частые вызовы.

Готовность к работе
Если сервер запущен, tools/list отдает инструменты, а @@mcp возвращает результат — система готова к использованию.
