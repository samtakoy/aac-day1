День 17: MCP Клиент — Детальный пошаговый план (объединенное решение)
Дата: 10 марта 2026
Статус: Готов к реализации
Часть: 2 из 2 (Клиент)

Цель
Интегрировать MCP клиент в Android приложение: поддержать Streamable HTTP транспорт, подключить MCP инструменты к агенту, обеспечить командный fallback @@mcp и отображение результатов в чате. Клиент должен уметь вызывать MCP инструменты и использовать результаты в ответах агента.

Предпосылки и зависимость
1) MCP сервер из части 1 должен быть доступен:
   - POST /mcp (Streamable HTTP)
   - GET /mcp (SSE)
   - POST /message (HTTP fallback)
2) В проекте уже есть MCP core feature:
   - McpRepository, McpTransport, McpSettings UI.
3) Dagger DI без Hilt, ViewModel Factory pattern.

Фаза 0: Анализ текущего клиента (30-45 минут)
0.1. Прочитать существующие файлы:
   - app/src/main/java/com/example/day/core/core_features/mcp/data/remote/McpTransport.kt
   - app/src/main/java/com/example/day/core/core_features/mcp/domain/repository/McpRepository.kt
   - app/src/main/java/com/example/day/features/console/impl/ui/viewmodel/ConsoleViewModelImpl.kt
   - app/src/main/java/com/example/day/core/core_features/agent/ (workers/tools)
0.2. Найти:
   - Где уже есть SSE и HTTP fallback.
   - Как происходит подключение MCP серверов.
   - Как обрабатываются @@ команды в чате.
0.3. Зафиксировать активный TransportType:
   - Нужно добавить STREAMABLE_HTTP, если его нет.

Фаза 1: Добавить Streamable HTTP в McpTransport (1 час)
1.1. Добавить TransportType.STREAMABLE_HTTP (если отсутствует).
1.2. Реализовать connectStreamableHttp(config):
   - POST {url}{urlPath} с Accept: application/json, text/event-stream.
   - Декодировать JSON или SSE.
1.3. Обновить connect(config):
   - when (transportType) → STREAMABLE_HTTP → connectStreamableHttp().
1.4. Если метод postStreamable уже есть:
   - Сделать доступным для использования McpToolsImpl (internal).
1.5. Добавить логирование:
   - URL, content-type ответа, размер тела.

Фаза 2: McpTools интерфейс (45 минут)
2.1. Создать интерфейс McpTools:
   - callTool(serverId, toolName, arguments)
   - listTools(serverId)
2.2. Реализовать McpToolsImpl:
   - Получить serverConfig из McpRepository.
   - Выполнить connect(serverId).
   - Сформировать JSON-RPC tools/call запрос.
   - Отправить через McpTransport.
   - Извлечь content/text из ответа.
2.3. Обработать ошибки:
   - Server not found.
   - Connection error.
   - Empty response.

Фаза 3: DI и wiring (30 минут)
3.1. McpCoreFeatureModule:
   - Binds McpToolsImpl → McpTools.
3.2. AppComponent:
   - Убедиться, что McpCoreFeatureModule включен.
3.3. ConsoleFeatureModule:
   - Передать McpTools в ConsoleViewModelFactory.

Фаза 4: Интеграция с агентом (1-1.5 часа)
4.1. Добавить системный промпт:
   - Описание MCP инструментов и параметров.
   - Формат tool call JSON:
     {"tool": "tool_name", "arguments": {...}}
4.2. Парсер tool call:
   - Распознавать JSON-ответ от LLM.
   - Валидировать наличие tool и arguments.
4.3. Выполнение tool call:
   - Выбрать активный MCP сервер (isEnabled или первый).
   - Выполнить mcpTools.callTool().
   - Вернуть результат в контекст разговора.
4.4. Безопасность:
   - Ограничить список разрешенных toolNames (whitelist).
   - Обрабатывать ошибки и таймауты.

Фаза 5: Команда @@mcp как fallback (45 минут)
5.1. Обработка команды:
   - @@mcp <tool_name> k=v k2=v2
5.2. Парсинг аргументов:
   - Разделение по пробелам.
   - key=value пары.
5.3. Вызов:
   - Выбрать сервер.
   - mcpTools.callTool().
5.4. Отображение результата:
   - В чате отдельное сообщение.
5.5. Ошибки:
   - Неверный формат команды.
   - Нет MCP серверов.

Фаза 6: UI сообщения (30-45 минут)
6.1. Tool call message:
   - Отдельный стиль сообщения (например, "🔧 tool_name").
6.2. Tool result message:
   - Отдельный стиль результата.
6.3. Отображение JSON:
   - Сокращение длинных ответов или expandable UI.

Фаза 7: Настройка сервера в приложении (30 минут)
7.1. Открыть MCP Settings.
7.2. Добавить сервер:
   - Name: GitHub Issues
   - URL: http://10.0.2.2:3000 (эмулятор)
   - Transport: STREAMABLE_HTTP
   - URL Path: /mcp
7.3. Подключиться:
   - Проверить список инструментов.

Фаза 8: Тестирование (1 час)
8.1. Проверить tools/list:
   - Список инструментов отображается в UI.
8.2. Проверить @@mcp echo:
   - @@mcp echo text=Hello
8.3. Проверить реальные вызовы:
   - @@mcp get_issue owner=org repo=project issueNumber=123
   - @@mcp list_issues owner=org repo=project state=open per_page=3
   - @@mcp get_user username=octocat
8.4. Проверить agent tool calling:
   - Попросить агента: "Покажи issue #123 в org/project".
   - Агент вызывает tool, возвращает результат.
8.5. Логи:
   - Включить фильтр Logcat MCP.

Критерии успеха
1) Streamable HTTP работает в McpTransport.
2) McpTools callTool успешно вызывает инструменты.
3) @@mcp команды работают.
4) Агент сам вызывает MCP инструмент и использует результат.
5) Результаты видны в чате.

Риски
1) Несоответствие Streamable HTTP формата ответа (JSON vs SSE).
2) Отсутствие MCP сервера или неправильный URL.
3) Ошибки парсинга JSON tool call от LLM.

Рекомендации по порядку внедрения
1) Сначала добавить Streamable HTTP.
2) Затем McpTools + DI.
3) Потом @@mcp команду.
4) Затем интеграцию с агентом и UI отображение tool calls.
