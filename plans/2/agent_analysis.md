# Анализ архитектуры агентов, User-in-the-Loop и Tool Calling

## 1. Обзор архитектуры

### 1.1 Общая структура

Проект представляет собой Android-приложение с многоуровневой архитектурой агентов:

```
AIAgent (оркестратор)
├── ContextStrategy (управление контекстом)
├── MemoryProvider (память: LTM + Working Memory)
├── ToolProvider (инструменты MCP)
├── ToolCallOrchestrator (цикл tool calling)
└── LlmRequestUseCase (запросы к LLM)
```

### 1.2 Типы Workers

| Worker | Команда | Назначение |
|--------|---------|------------|
| `SimpleWorker` | `@@simple` | Прямой запрос к LLM без контекста |
| `StepWorker` | `@@steps` | Пошаговое выполнение задачи |
| `TalkWorker` | `@@talk` | Контекстный диалог с управлением контекстом |
| `TaskWorker` | `@@task` | State machine для сложных задач |
| `PlannerWorker` | `@@plan` | Планирование с этапами |
| `TeamWorker` | `@@team` | Мульти-агентная коллаборация |
| `CompareWorker` | `@@compare` | Сравнение подходов |
| `PromptWorker` | `@@prompt` | Генерация промптов |
| `McpWorker` | `@@mcp` | Работа с MCP серверами |

---

## 2. Как устроена работа с агентами

### 2.1 Создание агентов

Агенты создаются через [`AIAgentFactory`](app/src/main/java/com/example/day/core/core_features/agent/domain/AIAgentFactory.kt:19), которая:

1. Получает или создаёт [`AgentConfig`](app/src/main/java/com/example/day/core/core_features/agent/domain/model/AgentConfig.kt:1) через [`GetOrCreateAgentUseCase`](app/src/main/java/com/example/day/core/core_features/agent/domain/usecase/GetOrCreateAgentUseCase.kt:1)
2. Выбирает стратегию контекста через [`StrategyFactory`](app/src/main/java/com/example/day/core/core_features/agent/domain/strategy/StrategyFactory.kt:1)
3. Создаёт [`MemoryProvider`](app/src/main/java/com/example/day/core/core_features/memory/domain/provider/base/MemoryProvider.kt:1) через [`MemoryProviderFactory`](app/src/main/java/com/example/day/core/core_features/memory/domain/provider/base/MemoryProviderFactory.kt:1)
4. Собирает [`AIAgent`](app/src/main/java/com/example/day/core/core_features/agent/domain/AIAgent.kt:23) со всеми зависимостями

### 2.2 Обработка сообщений

Основной метод [`AIAgent.process()`](app/src/main/java/com/example/day/core/core_features/agent/domain/AIAgent.kt:43):

```kotlin
suspend fun process(
    prompt: AContextMessage,
    onEvent: (suspend (WorkerEvent) -> Unit)?
): Result<AIAgentResult> {
    // 1. Получаем "знания" (LTM + Working Memory)
    val memoryMessages = memoryProvider.getMemoryContext()
    val enrichedPrompt = memoryProvider.appendUserPrompt(prompt)
    
    // 2. Строим контекст через стратегию
    val snapshot = strategy.process(config, contextRepository)
    
    // 3. Выполняем tool calling цикл
    val result = orchestrator.execute(
        initialHistory = snapshot.messages.toModelRequestMessages(),
        memoryMessages = memoryMessages,
        prompt = enrichedPrompt,
        systemPrompt = config.systemPrompt,
        modelSettings = config.modelSettings,
        tools = toolProvider.getTools(agentId = config.id),
        context = ToolCallContext(agentId = config.id),
        onEvent = onEvent
    )
    
    // 4. Сохраняем контекст через стратегию
    return result.map { toolCallingResult ->
        val extendedSnapshot = snapshot.copy(messages = toolCallingResult.allMessages)
        val strategyResult = strategy.afterResponse(...)
        AIAgentResult(...)
    }
}
```

### 2.3 Стратегии контекста

Реализовано 6 стратегий:

| Стратегия | Описание |
|-----------|----------|
| [`ContextFullStrategy`](app/src/main/java/com/example/day/core/core_features/agent/domain/strategy/impl/ContextFullStrategy.kt:1) | Полная история сообщений |
| [`ContextSlidingWindowStrategy`](app/src/main/java/com/example/day/core/core_features/agent/domain/strategy/impl/ContextSlidingWindowStrategy.kt:1) | Скользящее окно |
| [`ContextSummaryStrategy`](app/src/main/java/com/example/day/core/core_features/agent/domain/strategy/impl/ContextSummaryStrategy.kt:1) | Сжатие через суммаризацию |
| [`ContextStickyFactsStrategy`](app/src/main/java/com/example/day/core/core_features/agent/domain/strategy/impl/ContextStickyFactsStrategy.kt:1) | Сохранение важных фактов |
| [`ContextBranchingStrategy`](app/src/main/java/com/example/day/core/core_features/agent/domain/strategy/impl/ContextBranchingStrategy.kt:1) | Ветвление диалога |
| [`ContextEmptyStrategy`](app/src/main/java/com/example/day/core/core_features/agent/domain/strategy/impl/ContextEmptyStrategy.kt:1) | Без контекста |

---

## 3. Как устроен Tool Calling

### 3.1 Архитектура Tool Calling

```
ToolCallOrchestratorImpl
├── Цикл while (loopIndex < MAX_TOOL_LOOPS)
│   ├── LLM запрос с tools
│   ├── Если нет tool_calls → финальный ответ
│   └── Если есть tool_calls:
│       ├── Сохраняем assistant message с tool_calls
│       ├── Выполняем каждый tool через ToolProvider
│       ├── Сохраняем tool results
│       └── Следующая итерация
└── Возвращаем ToolCallingResult
```

### 3.2 Реализация [`ToolCallOrchestratorImpl`](app/src/main/java/com/example/day/core/core_features/agent/domain/tools/impl/ToolCallOrchestratorImpl.kt:28)

```kotlin
class ToolCallOrchestratorImpl @Inject constructor(
    private val llmProvider: LlmRequestUseCase,
    private val toolProvider: ToolProvider
) : ToolCallOrchestrator {
    
    private companion object {
        private const val MAX_TOOL_LOOPS = 3
    }
    
    override suspend fun execute(...): Result<ToolCallingResult> {
        val messages = (memoryMessages.map { it.toModelRequestMessage() } + initialHistory).toMutableList()
        val newMessages = mutableListOf<ModelRequest.Message>()
        
        while (loopIndex < MAX_TOOL_LOOPS) {
            // 1. Запрос к LLM
            val llmResult = llmProvider.askLlm(...)
            
            // 2. Если нет tool calls — финальный ответ
            if (toolCalls.isNullOrEmpty()) {
                return Result.success(ToolCallingResult(...))
            }
            
            // 3. Сохраняем assistant message с tool_calls
            val assistantMessage = ModelRequest.Message(
                role = ModelRequest.Role.Assistant,
                content = choice.message.content.orEmpty(),
                toolCalls = toolCalls.map { ... }
            )
            messages.add(assistantMessage)
            newMessages.add(assistantMessage)
            
            // 4. Выполняем tool calls
            for (call in toolCalls) {
                onEvent?.invoke(WorkerEvent.ToolCallStarted(...))
                val toolResult = toolProvider.executeToolCall(call, context)
                // ... обработка результата
                onEvent?.invoke(WorkerEvent.ToolCallFinished(...))
            }
            
            // 5. Добавляем tool messages в историю
            messages.addAll(toolMessages)
            newMessages.addAll(toolMessages)
            
            loopIndex++
        }
        
        // Достигнут лимит итераций
        return Result.success(ToolCallingResult(...))
    }
}
```

### 3.3 MCP Tool Provider

[`McpToolProvider`](app/src/main/java/com/example/day/core/core_features/agent/data/tools/McpToolProvider.kt:25) реализует [`ToolProvider`](app/src/main/java/com/example/day/core/core_features/agent/domain/tools/ToolProvider.kt:6):

```kotlin
internal class McpToolProvider @Inject constructor(
    private val repository: McpRepository,
    private val mcpTools: McpTools,
    private val agentMemoryRepository: AgentMemoryRepository,
    private val json: Json
) : ToolProvider {
    
    override suspend fun getTools(agentId: Long?): List<ModelRequest.Tool> {
        val servers = getEnabledServers()
        val allowedTools = agentId?.let { getAllowedTools(it) }
        
        // Фильтрация по ALLOWED_TOOL_NAMES и allowedTools агента
        servers.forEach { server ->
            val tools = getConnectedTools(server.id)
            tools.forEach { tool ->
                if (!McpToolNames.ALLOWED_TOOL_NAMES.contains(tool.name)) return@forEach
                if (allowedTools != null && !allowedTools.contains(tool.name)) return@forEach
                // ... добавляем tool
            }
        }
    }
    
    override suspend fun executeToolCall(...): Result<String> {
        // Проверка разрешений
        if (!McpToolNames.ALLOWED_TOOL_NAMES.contains(toolName)) {
            return Result.failure(IllegalArgumentException("Tool not allowed"))
        }
        
        // Вызов через McpTools
        return mcpTools.callTool(
            serverId = serverId,
            toolName = toolName,
            arguments = arguments,
            context = McpToolCallContext(agentId = context.agentId)
        )
    }
}
```

### 3.4 Доступные инструменты

Согласно [`McpToolNames`](app/src/main/java/com/example/day/core/core_features/mcp/domain/McpConstants.kt:7):

```kotlin
val ALLOWED_TOOL_NAMES = setOf(
    "search_codebase",
    "search_codebase_fixed"
    // Остальные закомментированы:
    // GET_ISSUE, LIST_ISSUES, GET_ISSUE_COMMENTS, GET_USER,
    // CREATE_ISSUE, CREATE_COMMENT, SET_REMINDER,
    // INVESTIGATE_GIT_FILE, GET_FILE_ANALYSIS, ANALYZE_CODE_CONTENT,
    // GET_GIT_FILE_LIST, GET_FILE_CONTENT, RESET_GIT_FILE_LIST_CACHE
)
```

---

## 4. Как устроен User-in-the-Loop

### 4.1 State Machine для задач

[`TaskWorker`](app/src/main/java/com/example/day/core/core_features/agent/domain/workers/concrete/TaskWorker.kt:32) реализует state machine с 5 состояниями:

```
INIT → PLANNING → EXECUTION → VERIFICATION → DONE
  ↑         ↑          ↑            ↑
  └─────────┴──────────┴────────────┘
           (retry/rework)
```

### 4.2 Каждое состояние имеет handler

| State | Handler | Назначение |
|-------|---------|------------|
| INIT | [`InitStateHandler`](app/src/main/java/com/example/day/core/core_features/agent/domain/workers/task/states_config/handlers/InitStateHandler.kt:22) | Сбор требований |
| PLANNING | [`PlanningStateHandler`](app/src/main/java/com/example/day/core/core_features/agent/domain/workers/task/states_config/handlers/PlanningStateHandler.kt:22) | Декомпозиция на этапы |
| EXECUTION | [`ExecutionStateHandler`](app/src/main/java/com/example/day/core/core_features/agent/domain/workers/task/states_config/handlers/ExecutionStateHandler.kt:22) | Выполнение этапа |
| VERIFICATION | [`VerificationStateHandler`](app/src/main/java/com/example/day/core/core_features/agent/domain/workers/task/states_config/handlers/VerificationStateHandler.kt:31) | QA ревью |
| DONE | [`DoneStateHandler`](app/src/main/java/com/example/day/core/core_features/agent/domain/workers/task/states_config/handlers/DoneStateHandler.kt:21) | Финальный отчёт |

### 4.3 Механизм кнопок (User-in-the-Loop)

Каждый handler может вернуть кнопки через [`HandlerResult.Message`](app/src/main/java/com/example/day/core/core_features/state_machine/domain/HandlerResult.kt:1):

```kotlin
// Пример из InitStateHandler
HandlerResult(
    messages = chatMessages.withButton(
        action = ACTION_PROCEED,
        title = "К планированию",
        messageText = buildUserTask(title, description, goal, expert),
    )
)

// Пример из VerificationStateHandler
HandlerResult(
    messages = chatMessages.addButtons(
        data = data,
        messageText = buildUserTask(data)
    )
)
```

### 4.4 Обработка действий пользователя

[`TaskWorker.handleAction()`](app/src/main/java/com/example/day/core/core_features/agent/domain/workers/concrete/TaskWorker.kt:120) обрабатывает нажатия кнопок:

```kotlin
suspend fun handleAction(chat: Chat, action: String, onEvent: (suspend (WorkerEvent) -> Unit)?) {
    val currentState = taskContext.getState()
    val handler = TaskStateConfig.config.handlers[currentState]
    
    val result = handler?.handleUserAction(taskContext, action)
    result?.messages?.forEach { msg ->
        if (msg.isTitle) chatTools.addTitleMessage(chat.id, msg.message, msg.buttons)
        else if (msg.isInfo) chatTools.addInfoMessage(chat.id, msg.message, msg.buttons)
        else chatTools.addBotMessage(chat.id, msg.message, msg.buttons)
    }
    
    if (result?.llmRequest != null) {
        doWork(result.llmRequest.userPrompt.orEmpty(), chat, AContextMessage.Role.USER, onEvent)
    }
}
```

### 4.5 Примеры User-in-the-Loop

**INIT state:**
- LLM собирает требования через диалог
- Когда данные собраны, показывает кнопку "К планированию"
- Пользователь нажимает кнопку → переход в PLANNING

**VERIFICATION state:**
- LLM проводит QA ревью, оценивает по шкале 1-10
- Если оценка < 8, показывает кнопки "К этапу N" для доработки
- Если оценка >= 8, показывает кнопку "К результатам"
- Пользователь выбирает действие → переход в EXECUTION или DONE

**EXECUTION state:**
- LLM описывает решение этапа
- Показывает кнопку "К следующему этапу"
- Пользователь нажимает → переход к следующему этапу или в VERIFICATION

---

## 5. Система памяти

### 5.1 MemoryProvider иерархия

```
MemoryProvider (интерфейс)
├── UserProfileMemoryProvider (LTM: скиллы, имя)
├── TaskWorkingMemoryProvider (Working Memory: текущий проект)
├── TaskStateMemoryProvider (промпты для state machine)
└── CompositeMemoryProvider (композитный провайдер)
```

### 5.2 Стратегии контекста

Каждая стратегия реализует [`ContextStrategy`](app/src/main/java/com/example/day/core/core_features/agent/domain/strategy/ContextStrategy.kt:1):

```kotlin
interface ContextStrategy {
    suspend fun process(agent: AgentConfig, store: AgentContextRepository): ContextSnapshot
    suspend fun afterResponse(
        agent: AgentConfig,
        response: String,
        store: AgentContextRepository,
        fullContext: ContextSnapshot
    ): ContextStrategyResult
    suspend fun getInfoReport(agent: AgentConfig, store: AgentContextRepository): String
    suspend fun getFullContextReport(agent: AgentConfig, store: AgentContextRepository): String
    suspend fun updateParams(agent: AgentConfig, params: Map<String, String>, store: AgentContextRepository): String
}
```

---

## 6. Рекомендации по улучшению

### 6.1 Tool Calling

#### 6.1.1 Убрать хардкод ALLOWED_TOOL_NAMES ✅ РЕАЛИЗОВАНО

**Проблема:** В [`McpConstants.kt`](app/src/main/java/com/example/day/core/core_features/mcp/domain/McpConstants.kt:24) есть TODO:
```kotlin
// TODO это как тут оказалось? это инструменты сервера, мы не должны их хардкодить
val ALLOWED_TOOL_NAMES = setOf(
    "search_codebase",
    "search_codebase_fixed"
)
```

**Реализовано:**
- Удалён хардкодированный `ALLOWED_TOOL_NAMES` из констант
- [`ToolProvider`](app/src/main/java/com/example/day/core/core_features/agent/domain/tools/ToolProvider.kt:1) теперь использует только per-agent `allowedTools` из AgentMemoryRepository
- Если `allowedTools == null`, все инструменты с MCP серверов доступны
- **Добавлен namespace подход** для поддержки нескольких MCP серверов:
  - Формат: `serverId:toolName` для конфликтующих имён
  - Оригинальное имя сохраняется для первого сервера
  - Добавлен метод `getToolToServerMap()` в ToolProvider

#### 6.1.2 Увеличить MAX_TOOL_LOOPS

**Проблема:** [`MAX_TOOL_LOOPS = 3`](app/src/main/java/com/example/day/core/core_features/agent/domain/tools/impl/ToolCallOrchestratorImpl.kt:35) может быть недостаточно для сложных задач.

**Рекомендация:**
- Сделать `MAX_TOOL_LOOPS` конфигурируемым через [`AgentConfig`](app/src/main/java/com/example/day/core/core_features/agent/domain/model/AgentConfig.kt:1)
- Добавить возможность динамического увеличения лимита
- Рассмотреть возможность обнаружения циклических вызовов

#### 6.1.3 Добавить retry логику для tool calls

**Проблема:** При ошибке tool call цикл продолжается, но нет retry логики.

**Рекомендация:**
- Добавить retry с exponential backoff для transient ошибок
- Различать retryable и non-retryable ошибки
- Добавить метрики для мониторинга成功率

#### 6.1.4 Добавить streaming для tool results

**Проблема:** Tool results передаются как целые строки, нет streaming.

**Рекомендация:**
- Добавить поддержку streaming для длинных tool results
- Реализовать chunked responses для больших файлов
- Добавить прогресс-бар для длительных операций

### 6.2 User-in-the-Loop

#### 6.2.1 Унифицировать верификацию данных

**Проблема:** В [`InitStateHandler`](app/src/main/java/com/example/day/core/core_features/agent/domain/workers/task/states_config/handlers/InitStateHandler.kt:106) есть TODO:
```kotlin
// TODO это условие верификации - сделать единообразно во всех хендлерах
```

**Рекомендация:**
- Создать общий интерфейс `DataValidator` для валидации данных состояний
- Реализовать валидаторы для каждого состояния
- Добавить unit тесты для валидации

#### 6.2.2 Добавить undo/redo для действий

**Проблема:** Нет возможности отменить действие пользователя.

**Рекомендация:**
- Добавить стек истории действий
- Реализовать undo для кнопок
- Добавить подтверждение для критических действий

#### 6.2.3 Улучшить UX кнопок

**Проблема:** Кнопки генерируются динамически, нет предсказуемого UI.

**Рекомендация:**
- Создать UI компонент для кнопок с иконками
- Добавить анимации для нажатий
- Реализовать disabled state для недоступных действий

#### 6.2.4 Добавить таймауты для действий

**Проблема:** Нет таймаутов для ожидания действий пользователя.

**Рекомендация:**
- Добавить configurable timeout для каждого состояния
- Реализовать auto-proceed по таймауту
- Добавить напоминания пользователю

### 6.3 Memory и Context

#### 6.3.1 Улучшить суммаризацию

**Проблема:** [`ContextSummaryStrategy`](app/src/main/java/com/example/day/core/core_features/agent/domain/strategy/impl/ContextSummaryStrategy.kt:1) может терять важную информацию.

**Рекомендация:**
- Добавить иерархическую суммаризацию (summary of summaries)
- Реализовать selective compression (сохранять важные сообщения)
- Добавить quality metrics для суммаризации

#### 6.3.2 Добавить RAG интеграцию

**Проблема:** RAG упоминается в [`TalkWorker`](app/src/main/java/com/example/day/core/core_features/agent/domain/workers/concrete/TalkWorker.kt:35), но нет детальной реализации.

**Рекомендация:**
- Реализовать [`RagMemoryProvider`](app/src/main/java/com/example/day/core/core_features/memory/domain/provider/base/MemoryProvider.kt:1) для RAG
- Добавить vector search для семантического поиска
- Интегрировать с [`rag-server`](rag-server/SETUP.md:1)

#### 6.3.3 Добавить memory persistence

**Проблема:** Working memory не всегда сохраняется между сессиями.

**Рекомендация:**
- Реализовать persistent working memory
- Добавить memory snapshots для восстановления
- Добавить memory cleanup по расписанию

### 6.4 Workers

#### 6.4.1 Добавить worker orchestration

**Проблема:** Workers работают независимо, нет coordination.

**Рекомендация:**
- Реализовать [`WorkerOrchestrator`](app/src/main/java/com/example/day/core/core_features/agent/domain/workers/base/AWorker.kt:1) для coordination
- Добавить dependency injection между workers
- Реализовать pipeline из нескольких workers

#### 6.4.2 Добавить worker monitoring

**Проблема:** Нет метрик для мониторинга производительности workers.

**Рекомендация:**
- Добавить metrics collection для каждого worker
- Реализовать dashboard для мониторинга
- Добавить alerting для аномалий

#### 6.4.3 Улучшить error handling

**Проблема:** Ошибки обрабатываются по-разному в разных workers.

**Рекомендация:**
- Создать общий `WorkerErrorHandler`
- Добавить retry policies для разных типов ошибок
- Реализовать circuit breaker для external services

### 6.5 State Machine

#### 6.5.1 Добавить state persistence

**Проблема:** State machine состояние хранится в [`StateStore`](app/src/main/java/com/example/day/core/core_features/state_machine/domain/StateStore.kt:1), но нет backup.

**Рекомендация:**
- Добавить state snapshots
- Реализовать state migration для обновлений
- Добавить state validation

#### 6.5.2 Добавить state transitions logging

**Проблема:** Нет логирования переходов состояний.

**Рекомендация:**
- Добавить audit log для всех переходов
- Реализовать state diagram visualization
- Добавить analytics для оптимизации flow

#### 6.5.3 Добавить parallel states

**Проблема:** States работают последовательно, нет parallel execution.

**Рекомендация:**
- Добавить поддержку parallel states
- Реализовать synchronization points
- Добавить conflict resolution

### 6.6 Безопасность

#### 6.6.1 Добавить input validation

**Проблема:** Нет валидации входных данных от пользователя.

**Рекомендация:**
- Добавить input sanitization
- Реализовать rate limiting
- Добавить content filtering

#### 6.6.2 Добавить audit trail

**Проблема:** Нет аудита действий пользователя и агентов.

**Рекомендация:**
- Добавить comprehensive logging
- Реализовать audit trail для compliance
- Добавить anomaly detection

#### 6.6.3 Добавить access control

**Проблема:** Нет granular access control для инструментов.

**Рекомендация:**
- Реализовать RBAC для tools
- Добавить permission management UI
- Добавить access logging

### 6.7 Производительность

#### 6.7.1 Добавить caching

**Проблема:** Нет кэширования для часто используемых данных.

**Рекомендация:**
- Добавить LRU cache для tool results
- Реализовать cache для context snapshots
- Добавить cache invalidation策略

#### 6.7.2 Оптимизировать database queries

**Проблема:** Нет оптимизации запросов к Room.

**Рекомендация:**
- Добавить database indexing
- Реализовать query optimization
- Добавить connection pooling

#### 6.7.3 Добавить async processing

**Проблема:** Некоторые операции блокируют main thread.

**Рекомендация:**
- Перевести все I/O на coroutines
- Добавить background processing
- Реализовать job scheduling

---

## 7. Приоритеты реализации

### Высокий приоритет (P0)

1. **Убрать хардкод ALLOWED_TOOL_NAMES** - критично для гибкости
2. **Унифицировать верификацию данных** - улучшит качество
3. **Добавить retry логику для tool calls** - повысит надёжность
4. **Добавить input validation** - важно для безопасности

### Средний приоритет (P1)

1. **Увеличить MAX_TOOL_LOOPS** - улучшит UX
2. **Добавить undo/redo** - улучшит UX
3. **Улучшить суммаризацию** - улучшит качество контекста
4. **Добавить worker monitoring** - важно для运维

### Низкий приоритет (P2)

1. **Добавить streaming для tool results** - nice to have
2. **Добавить parallel states** - nice to have
3. **Добавить caching** - оптимизация производительности
4. **Добавить RAG интеграция** - расширение функциональности

---

## 8. Заключение

Архитектура проекта хорошо структурирована и следует принципам Clean Architecture. Основные сильные стороны:

1. **Чёткое разделение ответственности** между слоями
2. **Гибкая система стратегий** для управления контекстом
3. **Хорошая реализация tool calling** цикла
4. **Продуманная state machine** для сложных задач
5. **Модульная архитектура** workers

Основные области для улучшения:

1. **Гибкость конфигурации** (убрать хардкоды)
2. **Обработка ошибок** (retry, circuit breaker)
3. **Мониторинг и observability** (метрики, логирование)
4. **Безопасность** (валидация, аудит)
5. **Производительность** (кэширование, оптимизация)

Рекомендуется начать с P0 приоритетов, так как они критичны для стабильности и гибкости системы.

## Соответствие терминологии OpenRouter

В проекте используется современная терминология OpenRouter API вместо устаревших терминов:

- `function_call` (устаревший термин) ↔ `tool_calls` в `assistant` сообщении
- `function_call_output` (устаревший термин) ↔ `tool` сообщение с `tool_call_id`

Это соответствует текущему OpenRouter API и более современным подходам к вызову инструментов, где `tool_calls` содержат информацию о вызовах инструментов в `assistant` сообщении, а `tool` сообщения содержат результаты выполнения инструментов с привязкой к конкретным вызовам через `tool_call_id`.
