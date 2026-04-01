# Этап 1: CrmServer — детальный план реализации

## Контекст этапа

CrmServer — отдельный Gradle-модуль (Ktor + Exposed + SQLite + MCP SDK).
Паттерн: `mcp-server` (структура MCP-сервера) + `rag-server` (БД через Exposed + SQLite).

После этапа: работающий MCP-сервер с CRM БД, подключаемый в настройках MCP приложения.
Этот сервер используется в Этапе 4 (Support State Handlers).

---

## Что получим (критерии успеха)

- [ ] Gradle-модуль `crm-server` собирается без ошибок
- [ ] При запуске создаётся SQLite-БД с таблицами `crm_users` и `crm_tickets`
- [ ] MCP-сервер стартует на порту (по умолчанию 3002, конфигурируется через env)
- [ ] Все 5 MCP-инструментов зарегистрированы и вызываются без ошибок
- [ ] Базовый сценарий работает: создать пользователя → создать тикет → получить тикеты

---

## Шаг 1. Регистрация модуля в проекте

**Файл:** `settings.gradle.kts` (корень проекта)

Добавить строку по образцу существующих модулей:
```
include(":crm-server")
```

---

## Шаг 2. `crm-server/build.gradle.kts`

По образцу `rag-server/build.gradle.kts`. Убрать всё специфичное для RAG (embedding, tree-sitter, AST).

**Нужные зависимости:**
- `io.modelcontextprotocol:kotlin-sdk` — MCP SDK (та же версия что в rag-server/mcp-server)
- `io.ktor:ktor-server-netty` + `ktor-server-core` — HTTP сервер
- `io.ktor:ktor-server-content-negotiation` + `ktor-serialization-kotlinx-json`
- `org.jetbrains.exposed:exposed-core` + `exposed-dao` + `exposed-jdbc` — ORM (та же версия что в rag-server)
- `org.xerial:sqlite-jdbc` — SQLite драйвер (та же версия)
- `org.jetbrains.kotlinx:kotlinx-serialization-json`
- `org.jetbrains.kotlinx:kotlinx-coroutines-core`
- `org.slf4j:slf4j-simple` — логи

**Main class:** `com.example.day.crmserver.CrmServerKt`

Версии брать из `rag-server/build.gradle.kts` — они уже проверены.

---

## Шаг 3. Структура файлов

```
crm-server/
├── build.gradle.kts
└── src/main/kotlin/com/example/day/crmserver/
    ├── CrmServer.kt            ← точка входа (main)
    ├── config/
    │   └── CrmConfig.kt        ← конфигурация из env-переменных
    ├── db/
    │   ├── CrmDatabase.kt      ← подключение + SchemaUtils
    │   ├── CrmUsersTable.kt    ← Exposed Table object
    │   └── CrmTicketsTable.kt  ← Exposed Table object
    └── tools/
        ├── CrmToolNames.kt     ← константы имён инструментов
        └── CrmTools.kt         ← регистрация MCP-инструментов
```

---

## Шаг 4. `CrmConfig.kt`

Читает конфигурацию из переменных окружения.

**Поля:**
- `dbPath: String` — путь к SQLite-файлу. Env: `CRM_DB_PATH`, default: `"crm.db"`
- `serverPort: Int` — порт HTTP-сервера. Env: `CRM_SERVER_PORT`, default: `3002`

**Реализация:** object или data class с companion, читает через `System.getenv()` с дефолтами.

---

## Шаг 5. `CrmUsersTable.kt`

Exposed `Table` object. Имя таблицы: `"crm_users"`.

**Колонки:**
- `id` — `long("id").autoIncrement()` (PrimaryKey)
- `chatId` — `long("chat_id").uniqueIndex()` — идентификатор пользователя в Telegram/боте
- `name` — `varchar("name", 255)`

---

## Шаг 6. `CrmTicketsTable.kt`

Exposed `Table` object. Имя таблицы: `"crm_tickets"`.

**Колонки:**
- `id` — `long("id").autoIncrement()` (PrimaryKey)
- `chatId` — `long("chat_id")` — ссылка на пользователя (не FK, для простоты)
- `status` — `varchar("status", 50)` — `"open"` | `"closed"` | `"operator"`
- `title` — `varchar("title", 500)`
- `description` — `text("description")`
- `result` — `text("result").default("")`

---

## Шаг 7. `CrmDatabase.kt`

**Метод `connect(dbPath: String)`:**
1. `Database.connect("jdbc:sqlite:$dbPath", driver = "org.sqlite.JDBC")`
2. `transaction { SchemaUtils.createMissingTablesAndColumns(CrmUsersTable, CrmTicketsTable) }`

Точная копия паттерна из `CodeDatabase.kt` в rag-server.

**CRUD-методы (все выполняются в `transaction { }`):**

`getUserByChatId(chatId: Long): UserRow?`
- SELECT из `CrmUsersTable` где `chatId = chatId`
- Возвращает data class `UserRow(id, chatId, name)` или null

`createUser(chatId: Long, name: String): UserRow`
- INSERT в `CrmUsersTable`
- Возвращает созданную запись (с id из `insertedKey`)

`getTicketsByChatId(chatId: Long): List<TicketRow>`
- SELECT из `CrmTicketsTable` где `chatId = chatId`
- Возвращает список `TicketRow(id, chatId, status, title, description, result)`

`createTicket(chatId: Long, title: String, description: String): TicketRow`
- INSERT в `CrmTicketsTable` со статусом `"open"`
- Возвращает созданную запись

`updateTicketStatus(ticketId: Long, status: String, result: String = "")`
- UPDATE `CrmTicketsTable` SET `status = status`, `result = result` WHERE `id = ticketId`

**Data classes** (в том же файле или отдельно, не Exposed entity):
```
UserRow(id: Long, chatId: Long, name: String)
TicketRow(id: Long, chatId: Long, status: String, title: String, description: String, result: String)
```

Оба — `@Serializable` для JSON-сериализации в MCP-ответах.

---

## Шаг 8. `CrmToolNames.kt`

Константы имён инструментов — строковый object:

```
GET_CRM_USER_BY_CHAT   = "get_crm_user_by_chat"
CREATE_CRM_USER        = "create_crm_user"
GET_CRM_USER_TICKETS   = "get_crm_user_tickets"
CREATE_CRM_TICKET      = "create_crm_ticket"
UPDATE_CRM_TICKET      = "update_crm_ticket"
```

---

## Шаг 9. `CrmTools.kt`

Функция `registerCrmTools(server: McpServer, db: CrmDatabase)`.

Регистрирует 5 инструментов через `server.addTool(name, description, inputSchema) { request -> ... }`.

Паттерн регистрации — точно как в `McpTools.kt` из mcp-server (каждый инструмент — отдельная private fun).

### Инструмент 1: `get_crm_user_by_chat`

**Описание:** "Get CRM user by Telegram chat ID. Returns user object or null if not found."

**Input schema:** `{ chatId: Long (required) }`

**Логика:**
1. Распарсить `chatId` из `request.arguments`
2. `db.getUserByChatId(chatId)`
3. Вернуть JSON пользователя или `{"found": false}`

**Возврат:** `CallToolResult` с `TextContent(json)`

### Инструмент 2: `create_crm_user`

**Описание:** "Create a new CRM user. Use when user is not found by get_crm_user_by_chat."

**Input schema:** `{ chatId: Long (required), userName: String (required) }`

**Логика:**
1. Распарсить `chatId`, `userName`
2. Проверить: `db.getUserByChatId(chatId)` — если уже существует, вернуть существующего (не ошибку)
3. Иначе `db.createUser(chatId, userName)`
4. Вернуть JSON пользователя

### Инструмент 3: `get_crm_user_tickets`

**Описание:** "Get all tickets for a user by chat ID. Returns list of tickets with id, status, title, description."

**Input schema:** `{ chatId: Long (required) }`

**Логика:**
1. Распарсить `chatId`
2. `db.getTicketsByChatId(chatId)`
3. Вернуть JSON-массив тикетов

### Инструмент 4: `create_crm_ticket`

**Описание:** "Create a new support ticket for user. Status is set to 'open' automatically."

**Input schema:** `{ chatId: Long (required), title: String (required), description: String (required) }`

**Логика:**
1. Распарсить `chatId`, `title`, `description`
2. `db.createTicket(chatId, title, description)`
3. Вернуть JSON созданного тикета

### Инструмент 5: `update_crm_ticket`

**Описание:** "Update ticket status. status can be: 'open', 'closed', 'operator'. Use result to summarize resolution."

**Input schema:** `{ ticketId: Long (required), status: String (required), result: String (optional, default "") }`

**Логика:**
1. Распарсить `ticketId`, `status`, опционально `result`
2. Валидировать `status` ∈ {"open", "closed", "operator"} — если нет, вернуть ошибку
3. `db.updateTicketStatus(ticketId, status, result)`
4. Вернуть `{"success": true, "ticketId": ticketId, "status": status}`

**Обработка ошибок во всех инструментах:** try-catch, при ошибке возвращать `CallToolResult(isError = true, content = [TextContent("Error: ${e.message}")])`

---

## Шаг 10. `CrmServer.kt` — точка входа

По образцу `McpServer.kt` (72 строки).

**Последовательность в `main()`:**

1. Создать `CrmConfig` (читает env)
2. Создать `CrmDatabase`, вызвать `db.connect(config.dbPath)` — таблицы создадутся автоматически
3. Создать MCP Server с capabilities:
   ```
   McpServer(serverInfo = ..., capabilities = ServerCapabilities(tools = ...))
   ```
4. Вызвать `registerCrmTools(server, db)`
5. Стартовать Ktor embeddedServer на `config.serverPort`:
   - Установить `mcpStreamableHttp(server)` на роут `/mcp`
   - Опционально: GET `/health` → `{"status": "ok"}` для проверки

---

## Шаг 11. Подключение к приложению

После запуска CrmServer нужно добавить его в настройки MCP в приложении.

В приложении MCP-серверы конфигурируются через UI (MCP Settings). После запуска CrmServer на `localhost:3002`:
- URL: `http://localhost:3002/mcp`
- Имя: `crm` (или любое удобное)

Агент получит доступ к инструментам `get_crm_user_by_chat`, `create_crm_user`, `get_crm_user_tickets`, `create_crm_ticket`, `update_crm_ticket` наравне с `search_codebase` из RagServer.

---

## Порядок реализации (checklist)

1. [ ] `settings.gradle.kts` — добавить `include(":crm-server")`
2. [ ] `crm-server/build.gradle.kts` — скопировать из rag-server, убрать лишнее, поправить main class
3. [ ] `CrmConfig.kt` — env-переменные
4. [ ] `CrmUsersTable.kt` — Exposed Table
5. [ ] `CrmTicketsTable.kt` — Exposed Table
6. [ ] `CrmDatabase.kt` — connect() + CRUD-методы + UserRow/TicketRow data classes
7. [ ] `CrmToolNames.kt` — константы
8. [ ] `CrmTools.kt` — registerCrmTools() с 5 инструментами
9. [ ] `CrmServer.kt` — main()
10. [ ] Собрать модуль, убедиться что нет ошибок компиляции
11. [ ] Запустить, проверить что БД создаётся и `/health` отвечает
12. [ ] Подключить в MCP-настройках приложения, проверить что инструменты видны агенту

---

## Зависимости и ограничения

- Версии библиотек (exposed, ktor, mcp-sdk) — брать строго из `rag-server/build.gradle.kts`
- SQLite работает в однопоточном режиме Exposed — `transaction { }` блокирует. Для Support-чата это приемлемо
- `update_crm_ticket` используется только Support-агентом (не другими агентами)
- Dockerfile для этапа не нужен
