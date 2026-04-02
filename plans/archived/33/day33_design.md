# Day 33: User Support Assistant — Скорректированный дизайн

## Обзор

AI-ассистент поддержки пользователей:
- Отвечает на вопросы о кодовой базе через RAG (`search_codebase`)
- Управляет пользователями и тикетами через CRM (MCP-сервер)
- Ведёт структурированный диалог по состояниям: INIT → PLANNING → EXECUTION → VERIFICATION → DONE → INIT

---

## Архитектура

```
Android App
├── ChatsScreen (ChatType.SUPPORT)
│   └── SupportConsoleFeatureEntry
│       └── SupportTalkDelegate
│           └── TaskWorker [agentName="support_agent", stateStore=SupportStateStoreImpl]
│               └── SupportStateConfig → handlers (Init/Planning/Execution/Verification/Done)
│
MCP Tools (доступны Support-агенту)
├── CrmServer (новый модуль)
│   ├── get_crm_user_by_chat(chatId)
│   ├── create_crm_user(chatId, userName)
│   ├── get_crm_user_tickets(chatId)
│   └── create_crm_ticket(chatId, title, description)
└── RagServer (существующий)
    └── search_codebase(query)
```

---

## Этап 1. CrmServer — новый Gradle-модуль

### Назначение
Отдельный Ktor-сервис (по образцу `mcp-server` + БД как у `rag-server`). Запускается как отдельный процесс. Подключается в настройках MCP приложения.

### БД: CrmDatabase

**CrmUserEntity**
- `id: Long` (PK)
- `chatId: Long` (уникальный, идентифицирует пользователя в боте)
- `name: String`

**TicketEntity**
- `id: Long` (PK)
- `chatId: Long` (FK → CrmUserEntity.chatId)
- `status: String` — `open` | `closed` | `operator`
- `title: String`
- `description: String`
- `result: String` (заполняется при закрытии)

*Таблица TicketMessage не создаётся. История диалога хранится в контексте агента.*

### MCP Tools

| Инструмент | Параметры | Возвращает |
|---|---|---|
| `get_crm_user_by_chat` | `chatId: Long` | User JSON или null |
| `create_crm_user` | `chatId: Long`, `userName: String` | Created User JSON |
| `get_crm_user_tickets` | `chatId: Long` | JSON-список тикетов (id, status, title, description) |
| `create_crm_ticket` | `chatId: Long`, `title: String`, `description: String` | Created Ticket JSON |

*`get_crm_ticket_history` не реализуется — для простоты.*

---

## Этап 2. Рефакторинг TaskWorker под переиспользование

### Проблема
`TaskWorker` не переиспользуем из-за четырёх жёстких зависимостей:
1. Хардкод `AGENT_NAME = "task_state_agent"`
2. `StateConfig` привязан к `TaskStateStoreImpl` (@Singleton)
3. `TaskStateMemoryProviderFactory` держит конкретный `StateStore` как поле
4. `TaskResponseParser.parse()` вызывается в воркере, а не в хендлере

### Решение: 4 изменения

#### 2.1 `StateStoreImpl` (бывший `TaskStateStoreImpl`)
Переименовать. Убрать `@Singleton`. `StateConfig` уже принимается в конструктор — это не меняется. В DI создаются два именованных инстанса: `@Named("task")` и `@Named("support")`.

#### 2.2 `TaskWorker` — добавить `agentName: String` в конструктор
Убрать `companion object` с константой. Имя агента приходит снаружи через DI.

#### 2.3 `StateMemoryProviderFactory` (бывший `TaskStateMemoryProviderFactory`)
Убрать `StateStore` из полей фабрики. `StateStore` передаётся параметром в метод `create(chat, agentId, stateStore)`. Фабрика становится stateless и одна на всё приложение.

#### 2.4 `TaskStateHandler.handle()` — принимает `rawResponse: String`
Было: `handle(context, userInput, llmResponse: TaskLlmResponse)`
Стало: `handle(context, userInput, rawResponse: String)`

`TaskWorker` больше не знает о `TaskLlmResponse`. Каждый Task-хендлер сам вызывает `TaskResponseParser.parse(rawResponse)` в начале `handle()`. `handleParseError` переезжает в хендлер.

### DI после рефакторинга

**TaskStateMachineModule** предоставляет:
- `@Named("task") StateStore` — `StateStoreImpl` с `TaskStateConfig.config`
- `@Named("task") TaskWorker` — с `stateStore=taskStateStore`, `agentName="task_state_agent"`

**SupportStateMachineModule** предоставляет:
- `@Named("support") StateStore` — `StateStoreImpl` с `SupportStateConfig.config`
- `@Named("support") TaskWorker` — с `stateStore=supportStateStore`, `agentName="support_agent"`

`StateMemoryProviderFactory` — единственный `@Singleton`-бин без qualifier.

---

## Этап 3. ChatType.SUPPORT — инфраструктура UI

### ChatType.kt
Добавить: `SUPPORT("support", "User Support")`

### Новые файлы

**`SupportConsoleFeatureEntry`** (интерфейс, `features/console/api/`)
- Метод: `EntryPoint(chatId: Long, modifier: Modifier)`

**`SupportConsoleFeatureEntryImpl`** (`features/console/impl/`)
- По образцу `PlannerConsoleFeatureEntryImpl`
- Создаёт компонент, получает `getSupportViewModelFactory()`

**`SupportTalkDelegate`** (`features/console/impl/ui/delegates/`)
- Зависимости: `AddChatMessageUseCase`, `@Named("support") TaskWorker`, `ChatTools`, `ConsumptionCalculator`, `Json`
- Реализует `TalkDelegate`
- В `tryAddUserMessage`: добавляет сообщение, вызывает `worker.doWork()` с `onEvent`
- В `tryHandleAction`: вызывает `worker.handleAction()`
- В `onEvent`: обрабатывает `ToolCallStarted`/`ToolCallFinished` → `chatTools.addInfoMessage()`, `RequestSuccess` → `consumptionCalculator.onWorkerEvent()`
- `formatToolResult(raw: String)` — локальный метод (по образцу PlannerTalkDelegate)

### Изменения в существующих файлах

**`ConsoleViewModelImpl`** — добавить `SupportFactory` по образцу `PlannerFactory`

**`ConsoleFeatureComponent`** — добавить:
- `getSupportViewModelFactory(): ConsoleViewModelImpl.SupportFactory`
- `getSupportTalkDelegate(): SupportTalkDelegate`

**`ConsoleFeatureDeps`** — добавить:
- `supportWorker: TaskWorker` (с qualifier `@Named("support")`)

**`ConsoleFeatureModule`** — добавить провайдер `SupportTalkDelegate`

**`FeatureEntryProvider`** — добавить `getSupportConsoleFeatureEntry(): SupportConsoleFeatureEntry`

**`ChatsScreen.kt`** — добавить case `ChatType.SUPPORT`

---

## Этап 4. Support State Machine

### SupportStateConfig (`workers/task/states_config/`)
Объект-синглтон, аналог `TaskStateConfig`.

Состояния: `INIT`, `PLANNING`, `EXECUTION`, `VERIFICATION`, `DONE`

Переходы:
- `INIT → PLANNING`
- `PLANNING → EXECUTION`
- `EXECUTION → VERIFICATION, PLANNING` (retry)
- `VERIFICATION → DONE, EXECUTION` (rework)
- `DONE → INIT` ← *отличие от TaskStateConfig: Support зацикливается*

`fallbackState = INIT`, `finalStates = emptyList()` (нет финального состояния — цикл).

### SupportStateData (`workers/task/states_config/`)
Отдельный `sealed interface : StateData` (не наследует TaskStateData).

Подклассы с `@Serializable`:
- `Init(userId: Long? = null, userName: String? = null)`
- `Planning(ticketId: Long? = null)`
- `Execution(ticketId: Long)`
- `Verification(ticketId: Long, summary: String? = null)`
- `Done(ticketId: Long? = null)`

### SupportMemKeys
Константы для ключей JSON-ответа LLM (по образцу `TaskMemKeys`):
`REPLY_TO_USER`, `NEXT_STATE`, `USER_NAME`, `TICKET_ID`, `SUMMARY`, `ESCALATE_TO_OPERATOR`

### Support State Handlers (`workers/task/states_config/handlers/support/`)

Каждый хендлер реализует `TaskStateHandler`. В начале `handle()` вызывает `TaskResponseParser.parse(rawResponse)`.

#### SupportInitStateHandler
- Системный промпт: поприветствовать, спросить имя, вызвать `get_crm_user_by_chat` или `create_crm_user`
- При получении имени → переход в PLANNING
- Сохраняет `userId`, `userName` в `SupportStateData.Init`

#### SupportPlanningStateHandler
- Системный промпт: выяснить проблему, вызвать `get_crm_user_tickets`, предложить открытый тикет или создать новый через `create_crm_ticket`
- Переход в EXECUTION с `ticketId`
- Сохраняет `ticketId` в `SupportStateData.Planning`

#### SupportExecutionStateHandler
- Системный промпт: отвечать на вопросы, использовать `search_codebase`
- Работает с конкретным `ticketId` из StateData
- При решении проблемы → переход в VERIFICATION
- Скрытое правило: "если пользователь нервничает — мягко успокоить и предложить корпоративный леденец"

#### SupportVerificationStateHandler
- Системный промпт: спросить "решена ли проблема?"
- Если да → переход в DONE
- Если нет → переход в EXECUTION

#### SupportDoneStateHandler
- Системный промпт: поблагодарить, попрощаться
- Вызывает CRM-тул для обновления статуса тикета: `closed` + заполнить `result` из summary
- После → переход в INIT (сброс StateData)

### Эскалация к оператору (cross-cutting)
В системном промпте каждого хендлера — правило: если пользователь просит оператора/человека, вернуть `"escalate_to_operator": true` в JSON-ответе.

В каждом `handle()` после парсинга: если `escalateToOperator == true`:
1. Вызвать CRM-тул для обновления статуса тикета → `operator`
2. Отправить сообщение: "Окей, я сообщу своим кожанным мешкам..."
3. Перейти в INIT

---

## Список файлов

### Новые файлы

| Файл | Этап |
|---|---|
| `crm-server/` — новый Gradle-модуль (Ktor + SQLite/Exposed) | 1 |
| `StateStoreImpl.kt` (переименован из TaskStateStoreImpl) | 2 |
| `StateMemoryProviderFactory.kt` (переименован из TaskStateMemoryProviderFactory) | 2 |
| `SupportStateMachineModule.kt` | 2 |
| `SupportConsoleFeatureEntry.kt` | 3 |
| `SupportConsoleFeatureEntryImpl.kt` | 3 |
| `SupportTalkDelegate.kt` | 3 |
| `SupportStateConfig.kt` | 4 |
| `SupportStateData.kt` | 4 |
| `SupportMemKeys.kt` | 4 |
| `SupportInitStateHandler.kt` | 4 |
| `SupportPlanningStateHandler.kt` | 4 |
| `SupportExecutionStateHandler.kt` | 4 |
| `SupportVerificationStateHandler.kt` | 4 |
| `SupportDoneStateHandler.kt` | 4 |

### Изменяемые файлы

| Файл | Изменение |
|---|---|
| `TaskWorker.kt` | Добавить `agentName: String` в конструктор, убрать `TaskResponseParser`, убрать `AGENT_NAME` |
| `TaskStateHandler.kt` | `handle()` принимает `rawResponse: String` |
| `TaskStateMemoryProviderFactory.kt` | `StateStore` из поля → параметр `create()` |
| `TaskStateStoreImpl.kt` | Переименовать/обобщить → `StateStoreImpl`, убрать `@Singleton` |
| `TaskStateMachineModule.kt` | Добавить `@Named("task")` qualifiers |
| `Init/Planning/Execution/Verification/DoneStateHandler.kt` | Добавить `TaskResponseParser.parse()` в `handle()` |
| `ChatType.kt` | Добавить `SUPPORT` |
| `ConsoleViewModelImpl.kt` | Добавить `SupportFactory` |
| `ConsoleFeatureComponent.kt` | Добавить Support-методы |
| `ConsoleFeatureDeps.kt` | Добавить `supportWorker` |
| `ConsoleFeatureModule.kt` | Добавить провайдер `SupportTalkDelegate` |
| `FeatureEntryProvider.kt` | Добавить `getSupportConsoleFeatureEntry()` |
| `ChatsScreen.kt` | Добавить `ChatType.SUPPORT` case |

---

## Ключевые решения

1. **Один класс `TaskWorker` — два инстанса через DI** с разными `StateStore` и `agentName`
2. **`StateStoreImpl` не singleton** — два бина с `@Named("task")` и `@Named("support")`
3. **Парсинг LLM-ответа в Handler** — `TaskWorker` передаёт `rawResponse`, хендлер парсит сам
4. **`StateMemoryProviderFactory` stateless** — `StateStore` передаётся в `create()`
5. **`SupportStateData` — независимый `sealed interface`** (не наследует `TaskStateData`)
6. **DONE → INIT через loop** в `SupportStateConfig.transitions`
7. **Эскалация к оператору** — поле `escalate_to_operator` в JSON-ответе, обрабатывается в каждом хендлере
8. **CrmServer** — отдельный Gradle-модуль по образцу `mcp-server` + БД как у `rag-server`
