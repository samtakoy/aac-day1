День 17: MCP Сервер — Детальный пошаговый план (объединенное решение)
Дата: 10 марта 2026
Статус: Готов к реализации
Часть: 1 из 2 (Сервер)

Цель
Создать отдельный JVM/Kotlin MCP сервер для интеграции с GitHub Issues API, используя MCP Kotlin SDK. Сервер должен поддерживать три транспорта (Streamable HTTP, SSE, HTTP fallback) и предоставить набор инструментов для работы с задачами (echo + 4-5 инструментов GitHub Issues). Должен запускаться как отдельный процесс и поддерживать Docker.

Требования (обязательные)
1) MCP Kotlin SDK (server) как основа для обработки MCP запросов.
2) Эндпоинты:
   - POST /mcp (Streamable HTTP, основной)
   - GET /mcp (SSE)
   - POST /message (HTTP fallback)
3) Регистрация инструментов (tools) с описанием параметров и результатом.
4) Интеграция с GitHub Issues API.
5) Docker контейнеризация и удобный локальный запуск.

Список инструментов (MCP Tools)
Минимально:
1) echo
2) get_issue
3) list_issues
4) get_issue_comments
5) get_user
Дополнительно (опционально, если нужно расширение):
6) create_issue
7) update_issue
8) search_issues

Архитектура
1) mcp-server (отдельный JVM модуль)
2) MCP Kotlin SDK server + Ktor HTTP server
3) HTTP client для GitHub Issues API (Ktor client)
4) ToolRegistry для регистрации инструментов
5) Транспортные обработчики:
   - Streamable HTTP: POST /mcp
   - SSE: GET /mcp
   - HTTP fallback: POST /message

Фаза 0: Подготовка (30-45 минут)
0.1. Получить GitHub Personal Access Token:
   - Settings → Developer settings → Personal access tokens.
   - Сохранить в переменную окружения.
0.2. Определить repo:
   - owner/repo (например, orgName/projectName).
0.3. Проверить API доступ:
   - curl GET https://api.github.com/user с Bearer токеном.
   - Ожидаем успешный ответ 200.
0.4. Определить транспортный контракт:
   - /mcp поддерживает Accept: application/json и Accept: text/event-stream.
   - /message принимает JSON-RPC и возвращает JSON (fallback).
0.5. Проверить выбранный MCP SDK:
   - Используем https://github.com/modelcontextprotocol/kotlin-sdk (server).

Фаза 1: Создание модуля mcp-server (30 минут)
1.1. Добавить модуль:
   - settings.gradle.kts: include(":mcp-server")
1.2. Создать структуру:
   - mcp-server/src/main/kotlin/com/example/day/mcpserver/
   - transport/, tools/
1.3. Добавить build.gradle.kts:
   - kotlin("jvm")
   - application
   - MCP SDK server
   - ktor-server (netty или cio)
   - ktor-client (cio/okhttp) для GitHub Issues API
   - kotlinx-serialization

Фаза 2: Базовый сервер (1 час)
2.1. Создать McpServer.kt (main):
   - Загрузить переменные окружения:
     GITHUB_TOKEN
     GITHUB_OWNER (опционально, если не передаем в аргументах)
     GITHUB_REPO (опционально, если не передаем в аргументах)
   - Создать Ktor HTTP client с defaultRequest:
     baseUrl https://api.github.com
     Authorization: Bearer ...
     Accept: application/vnd.github+json
     X-GitHub-Api-Version: 2022-11-28
   - Создать MCP Server (SDK) с serverInfo и capabilities.
2.2. Зарегистрировать инструменты:
   - EchoTools
   - GitHubIssuesTools (get_issue, list_issues, get_issue_comments, get_user)
2.3. Поднять embedded Ktor server:
   - host: 0.0.0.0
   - port: 3000
2.4. Подключить маршруты:
   - POST /mcp → Streamable HTTP handler
   - GET /mcp → SSE handler
   - POST /message → HTTP fallback handler

Фаза 3: Транспорт Streamable HTTP (1 час)
3.1. Реализовать handler POST /mcp:
   - Читать body как строку (JSON-RPC).
   - Определить Accept header:
     - Если Accept: text/event-stream → SSE response.
     - Иначе → JSON response.
3.2. Обработать через MCP SDK:
   - server.handleRequest(requestBody).
3.3. Формат ответа:
   - SSE: "event: message\ndata: {json}\n\n"
   - JSON: raw JSON response.
3.4. Логирование:
   - Логировать входящий request (обрезка).
   - Логировать тип ответа (JSON/SSE).

Фаза 4: Транспорт SSE (1 час)
4.1. Реализовать handler GET /mcp:
   - Создать sessionId (UUID).
   - Создать Channel для входящих сообщений.
   - Сохранить sessionId → Channel в памяти.
4.2. Отдать SSE:
   - Первое событие: event "endpoint" data "/message?session={id}".
   - Слушать канал и отправлять event "message".
4.3. Реализовать функции:
   - sendToSession(sessionId, message): отправка сообщения в канал.
   - removeSession(sessionId): очистка (при disconnect).

Фаза 5: Транспорт HTTP fallback (45 минут)
5.1. Handler POST /message:
   - Если есть query param session:
     - Это сообщение для SSE сессии.
     - Передать через sendToSession.
   - Иначе:
     - Обычный JSON-RPC запрос.
     - server.handleRequest(requestBody) → JSON response.
5.2. Ошибки:
   - Если session не найден → 404.
   - Если обработка MCP не удалась → 500 с пояснением.

Фаза 6: Инструменты MCP (1.5-2 часа)
6.1. EchoTools:
   - inputSchema: text (string, required).
   - output: "Echo: <text>".
6.2. GitHubIssuesTools:
   - get_issue(owner, repo, issueNumber): GET /repos/{owner}/{repo}/issues/{issue_number}.
   - list_issues(owner, repo, state, labels, per_page): GET /repos/{owner}/{repo}/issues.
   - get_issue_comments(owner, repo, issueNumber): GET /repos/{owner}/{repo}/issues/{issue_number}/comments.
   - get_user(username): GET /users/{username}.
6.3. Описания и inputSchema:
   - Четко описать параметры.
   - required поля.
6.4. Результаты:
   - Возвращать TextContent с JSON строкой (или форматированным текстом).
   - В случае ошибки: CallToolResult(isError = true).

Фаза 7: Docker и локальный запуск (45 минут)
7.1. Dockerfile:
   - multi-stage: gradle build → jre runtime.
7.2. docker-compose.yml:
   - проброс порта 3000.
   - env для GitHub token и owner/repo.
   - healthcheck.
7.3. .env.example:
   - GITHUB_TOKEN
   - GITHUB_OWNER
   - GITHUB_REPO
7.4. .dockerignore:
   - исключить build, app, plans, meta.

Фаза 8: Тестирование и проверка (1 час)
8.1. Локальный запуск:
   - ./gradlew :mcp-server:run
8.2. Проверки:
   - POST /message tools/list.
   - POST /mcp tools/call echo.
   - GET /mcp (SSE).
8.3. Проверка инструментов:
   - get_issue с реальным issueNumber.
   - list_issues с owner/repo.
   - get_user с username.
8.4. Логи сервера:
   - Проверить, что запросы доходят и возвращают ответы.

Критерии успеха
1) Сервер запускается и держит три эндпоинта.
2) tools/list возвращает все зарегистрированные инструменты.
3) tools/call echo работает через /mcp и /message.
4) Инструменты GitHub Issues возвращают валидные данные.
5) Docker контейнер поднимается и отвечает на /mcp.

Риски
1) Ошибки токена GitHub.
2) Rate limits GitHub API.
3) Разные версии MCP SDK и спецификации.
4) Ограничения сетевого доступа из окружения.

Рекомендация по приоритетам
1) Поднять сервер + echo.
2) Добавить GitHub Issues tools.
3) Проверить end-to-end через curl.
4) Завести Docker.
