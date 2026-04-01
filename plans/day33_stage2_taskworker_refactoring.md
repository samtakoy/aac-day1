# Этап 2: Рефакторинг TaskWorker — детальный план реализации

## Контекст этапа

Делаем `TaskWorker` переиспользуемым для Support-воркера без дублирования логики.
Рефакторинг затрагивает 4 слоя: интерфейс хендлера, воркер, фабрика памяти, DI-модуль.

После этапа:
- `TaskWorker` принимает `agentName` и `StateStore` снаружи — не захардкожены
- `TaskStateHandler.handle()` принимает `rawResponse: String` — воркер не знает о `TaskLlmResponse`
- Все 5 существующих Task-хендлеров обновлены и работают как раньше
- DI настроен так, что `PlannerTalkDelegate` получает `@Named("task") TaskWorker`
- Готовы слоты для `@Named("support") TaskWorker` в следующих этапах

---

## Что получим (критерии успеха)

- [ ] Проект собирается без ошибок компиляции
- [ ] `PlannerTalkDelegate` работает без регрессий (получает тот же TaskWorker через DI)
- [ ] Функциональность Planner-чата не изменилась
- [ ] В DI появился `@Named("task") TaskWorker` и задел для `@Named("support") TaskWorker`

---

## Изменение 1: `TaskStateHandler.handle()` — новая сигнатура

**Файл:** `state_machine/domain/TaskStateHandler.kt`

Было:
```
suspend fun handle(context: StateContext, userInput: String, llmResponse: TaskLlmResponse): HandlerResult
```

Стало:
```
suspend fun handle(context: StateContext, userInput: String, rawResponse: String): HandlerResult
```

Импорт `TaskLlmResponse` из этого файла убрать.

**Почему:** `TaskWorker` больше не должен знать о формате LLM-ответа. Хендлер сам парсит то, что ему нужно.

---

## Изменение 2: Task-хендлеры — добавить парсинг в начало `handle()`

Затрагивает все 5 файлов:
- `InitStateHandler.kt`
- `PlanningStateHandler.kt`
- `ExecutionStateHandler.kt`
- `VerificationStateHandler.kt`
- `DoneStateHandler.kt`

**Шаблон изменения для каждого хендлера:**

В начале метода `handle()` добавить парсинг и обработку ошибки:
```
val llmResponse = TaskResponseParser.parse(rawResponse)
    ?: return HandlerResult(
        messages = listOf(HandlerResult.Message(
            "⚠️ Не удалось разобрать ответ ассистента. Попробуйте переформулировать запрос.\n\nRaw: $rawResponse",
            isInfo = true
        ))
    )
```

Далее тело метода остаётся **без изменений** — `llmResponse` используется как раньше.

`handleParseError` из `TaskWorker` убирается — логика теперь в каждом хендлере.

---

## Изменение 3: `TaskStateMemoryProviderFactory` → `StateMemoryProviderFactory`

**Файл:** `memory/domain/provider/TaskStateMemoryProviderFactory.kt`

Переименовать файл и класс в `StateMemoryProviderFactory`.

**Было:**
```
class TaskStateMemoryProviderFactory @Inject constructor(
    private val taskStateStore: StateStore
) {
    fun create(chat: Chat, agentId: Long): TaskStateMemoryProvider
```

**Стало:**
```
class StateMemoryProviderFactory @Inject constructor() {   // нет зависимостей
    fun create(chat: Chat, agentId: Long, stateStore: StateStore): TaskStateMemoryProvider
```

Внутри `create()`:
```
return TaskStateMemoryProvider(chat = chat, agentId = agentId, taskStateStore = stateStore)
```

`@Inject constructor()` оставить — фабрика остаётся синглтоном в DI (без зависимостей).

---

## Изменение 4: `TaskStateStoreImpl` → `StateStoreImpl`

**Файл:** `workers/task/states_store/TaskStateStoreImpl.kt`

Переименовать файл и класс в `StateStoreImpl`.

**Убрать:**
- `@Singleton` аннотацию с класса
- `@Inject` с конструктора (экземпляры будут создаваться в DI-модулях явно)

Конструктор и вся логика внутри — **без изменений**.

```
// Было:
@Singleton
class TaskStateStoreImpl @Inject constructor(
    private val agentMemoryRepository: AgentMemoryRepository,
    private val agentContextRepository: AgentContextRepository,
    private val stateConfig: StateConfig
) : StateStore

// Стало:
class StateStoreImpl constructor(
    private val agentMemoryRepository: AgentMemoryRepository,
    private val agentContextRepository: AgentContextRepository,
    private val stateConfig: StateConfig
) : StateStore
```

---

## Изменение 5: `TaskWorker` — добавить `agentName`, обновить зависимости

**Файл:** `workers/concrete/TaskWorker.kt`

**Изменения в конструкторе:**

Добавить параметры:
```
private val agentName: String,
private val stateMemoryProviderFactory: StateMemoryProviderFactory,   // было TaskStateMemoryProviderFactory
```

Убрать `@Inject` с конструктора (воркер предоставляется через `@Provides` явно).

Убрать `companion object` с `AGENT_NAME`.

**Изменения в `doWork()`:**

Строка `aiAgentFactory.getOrCreate(AGENT_NAME, ...)` → заменить `AGENT_NAME` на `agentName`.

Строка `aiAgentFactory.getOrCreate(AGENT_NAME, ...)` в `handleAction()` — аналогично.

**Изменения в `buildMemoryProvider()`:**

Было:
```
val taskStateProvider = taskStateMemoryProviderFactory.create(chat = chat, agentId = agent.config.id)
```

Стало:
```
val taskStateProvider = stateMemoryProviderFactory.create(
    chat = chat,
    agentId = agent.config.id,
    stateStore = stateStore   // передаём свой stateStore
)
```

**Изменения в `processSuccessResponse()`:**

Убрать параметр `llmResponse: TaskLlmResponse`, заменить на `rawResponse: String`.

Убрать вызов `TaskResponseParser.parse()` и блок `if (llmResponse != null)`.

Новый вызов хендлера:
```
val result = handler.handle(context, userInput, rawResponse)
```

Убрать метод `handleParseError()` — его логика переехала в хендлеры.

Убрать импорт `TaskResponseParser` и `TaskLlmResponse` из файла.

**Изменения в `doWork()` — блок onSuccess:**

Было:
```
onSuccess = { agentResult ->
    val llmResponse = TaskResponseParser.parse(agentResult.responseText)
    if (llmResponse != null) {
        processSuccessResponse(chat, taskContext, userPrompt, llmResponse, agentResult.responseText, onEvent)
    } else {
        handleParseError(chat.id, agentResult.responseText)
    }
}
```

Стало:
```
onSuccess = { agentResult ->
    processSuccessResponse(chat, taskContext, userPrompt, agentResult.responseText, onEvent)
}
```

---

## Изменение 6: `TaskStateMachineModule` — явное создание TaskWorker

**Файл:** `agent/di/TaskStateMachineModule.kt`

Полностью переписать.

**Текущее состояние:**
```
@Module
internal interface TaskStateMachineModule {
    companion object {
        @Provides @Singleton
        internal fun provideTaskStateConfig(): StateConfig {
            return TaskStateConfig.config
        }
    }
}
```

**После рефакторинга:**
```
@Module
internal object TaskStateMachineModule {

    @Provides @Named("task") @Singleton
    internal fun provideTaskStateStore(
        agentMemoryRepository: AgentMemoryRepository,
        agentContextRepository: AgentContextRepository
    ): StateStore = StateStoreImpl(
        agentMemoryRepository = agentMemoryRepository,
        agentContextRepository = agentContextRepository,
        stateConfig = TaskStateConfig.config
    )

    @Provides @Named("task")
    internal fun provideTaskWorker(
        aiAgentFactory: AIAgentFactory,
        chatTools: ChatTools,
        memoryProviderFactory: MemoryProviderFactory,
        stateMemoryProviderFactory: StateMemoryProviderFactory,
        contextRepository: AgentContextRepository,
        llmRequestUseCase: LlmRequestUseCase,
        strategyFactory: StrategyFactory,
        @Named("task") stateStore: StateStore,
        toolProvider: ToolProvider,
        toolCallOrchestrator: ToolCallOrchestrator
    ): TaskWorker = TaskWorker(
        aiAgentFactory = aiAgentFactory,
        chatTools = chatTools,
        memoryProviderFactory = memoryProviderFactory,
        stateMemoryProviderFactory = stateMemoryProviderFactory,
        contextRepository = contextRepository,
        llmRequestUseCase = llmRequestUseCase,
        strategyFactory = strategyFactory,
        stateStore = stateStore,
        toolProvider = toolProvider,
        toolCallOrchestrator = toolCallOrchestrator,
        agentName = "task_state_agent"
    )
}
```

**Примечание:** `@Named("task") TaskWorker` не `@Singleton` — воркер создаётся один раз в скоупе компонента, но `@Singleton` явно не нужен (достаточно скоупа компонента).

---

## Изменение 7: `AgentCoreFeatureModule.kt` — убрать `bindsTaskStateStore`

**Файл:** `core/core_features/agent/di/AgentCoreFeatureModule.kt`

После переименования `TaskStateStoreImpl` → `StateStoreImpl` и удаления с него `@Inject`, старый биндинг больше не компилируется. Убрать:
```
@Binds
@Singleton
fun bindsTaskStateStore(impl: TaskStateStoreImpl): StateStore
```
И импорты `TaskStateStoreImpl` и `StateStore` из этого файла.

Именованные инстансы `@Named("task") StateStore` (и позже `@Named("support") StateStore`) предоставляются через `@Provides` в соответствующих модулях стейт-машин.

---

## Изменение 8: DI-граф — обновить привязку `TaskStateMemoryProviderFactory`

В DI-компоненте (или модуле где был привязан `TaskStateMemoryProviderFactory`) нужно убедиться что везде используется новый `StateMemoryProviderFactory`.

Поиск по проекту: найти все использования `TaskStateMemoryProviderFactory` и заменить на `StateMemoryProviderFactory`.

**Где используется:**
- Конструктор `TaskWorker` (изменён в шаге 5)
- DI-граф (возможно, в `ConsoleFeatureDeps` или другом модуле)

Проверить grep по `TaskStateMemoryProviderFactory` — обновить все вхождения.

---

## Порядок реализации (checklist)

> **Важно:** шаги 1-3 нужно делать вместе, иначе проект не скомпилируется между шагами.

1. [ ] `TaskStateHandler.kt` — изменить сигнатуру `handle()` (шаг 1)
2. [ ] Все 5 Task-хендлеров — добавить `TaskResponseParser.parse()` в `handle()` (шаг 2)
3. [ ] `TaskWorker.kt` — обновить `processSuccessResponse()` и `doWork()` (часть шага 5)
4. [ ] Убедиться что проект компилируется (handle-цепочка починена)
5. [ ] `TaskStateMemoryProviderFactory.kt` → переименовать/переписать в `StateMemoryProviderFactory.kt` (шаг 3)
6. [ ] `TaskStateStoreImpl.kt` → переименовать/переписать в `StateStoreImpl.kt` (шаг 4)
7. [ ] `TaskWorker.kt` — убрать `@Inject`, добавить `agentName`, обновить `buildMemoryProvider()` (шаг 5)
8. [ ] `TaskStateMachineModule.kt` — переписать с явными `@Provides` (шаг 6)
9. [ ] `AgentCoreFeatureModule.kt` — убрать `bindsTaskStateStore` и импорты (шаг 7)
10. [ ] Grep по `TaskStateMemoryProviderFactory` — убедиться что нет других использований (шаг 8)
11. [ ] Полная сборка проекта
12. [ ] Запустить Planner-чат, убедиться что всё работает как раньше

---

## Зависимости между этапами

- Этап 3 (инфраструктура Support UI) добавляет `@get:Named("support") val supportWorker` в `ConsoleFeatureDeps`, но сам провайдер `@Named("support") TaskWorker` появится только в Этапе 4 (`SupportStateMachineModule`). Между Этапами 3 и 4 проект не компилируется — это нормально.
- Этап 4 (Support State Machine) зависит от `StateStoreImpl` и `StateMemoryProviderFactory` (созданы здесь)
- `SupportStateMachineModule` по структуре будет точной копией нового `TaskStateMachineModule`, но с `SupportStateConfig.config` и `agentName = "support_agent"`

---

## Что НЕ меняется в этом этапе

- Логика внутри Task-хендлеров после строки `val llmResponse = TaskResponseParser.parse(...)`
- `TaskStateConfig.kt` — без изменений
- `TaskStateData.kt` — без изменений
- `TaskStateMemoryProvider.kt` — без изменений (только его фабрика)
- `StateContext.kt` — без изменений
- `HandlerResult.kt` — без изменений
- Все остальные делегаты (`RagTalkDelegate`, `AssistantTalkDelegate` и др.) — без изменений
