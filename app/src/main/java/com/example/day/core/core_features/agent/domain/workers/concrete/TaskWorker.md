# TaskWorker — Описание и руководство по конфигурированию

`TaskWorker` — универсальный воркер для агентов с конечным автоматом состояний. Он управляет жизненным циклом диалога, делегируя логику каждого состояния отдельному `TaskStateHandler`.

---

## Архитектура

```
TalkDelegate
    └── TaskWorker.doWork(userPrompt, chat)
            │
            ├── validateInitialState()       — восстановить/установить начальное состояние
            ├── buildMemoryProvider()        — собрать CompositeMemoryProvider
            │       ├── TaskStateMemoryProvider  — динамический system prompt от текущего состояния
            │       └── BaseMemoryProvider       — UserProfile / LTM (из конфига агента)
            │
            ├── AIAgent.process(userPrompt)  — вызов LLM (с тулами, контекстом, tool-call loop)
            │
            └── processSuccessResponse(rawResponse)
                    └── handler.handle(context, userInput, rawResponse)
                            │
                            ├── парсит JSON-ответ LLM (TaskResponseParser)
                            ├── сохраняет StateData (context.saveStateData)
                            ├── переключает состояние (context.updateState)
                            └── возвращает HandlerResult
                                    ├── messages   — сообщения в чат
                                    └── llmRequest — если нужен ещё один вызов LLM (loop)
```

---

## Ключевые компоненты

### `StateConfig`

Описывает всю конфигурацию автомата:

```kotlin
StateConfig(
    states      = listOf(INIT, PLANNING, ...),   // все состояния
    serializers = mapOf(INIT to Init.serializer(), ...),  // для персистентности
    transitions = mapOf(INIT to listOf(PLANNING), ...),   // допустимые переходы
    handlers    = mapOf(INIT to InitHandler(), ...),       // обработчик каждого состояния
    initialState     = INIT,
    finalStates      = emptyList(),       // [] — автомат зацикленный; или [DONE] — конечный
    fallbackState    = INIT,              // состояние при ошибке или первом запуске
    fallbackStateData = StateData.Init(), // данные при fallback
    stateInfoProvider = ...               // человекочитаемое описание состояний
)
```

### `StateStore`

Персистентное хранилище состояния агента (через `AgentMemoryRepository`). Каждый экземпляр `TaskWorker` получает свой `StateStore`, сконфигурированный под свою `StateConfig`.

### `StateContext`

Фасад для работы обработчика с хранилищем:

| Метод | Описание |
|---|---|
| `getState()` | Текущее состояние |
| `updateState(stateId)` | Перейти в новое состояние |
| `saveStateData(data)` | Сохранить данные текущего состояния |
| `getStateData(stateId, version)` | Прочитать данные любого предыдущего состояния |
| `clearTaskMemory()` | Сбросить всё (для зацикленных автоматов: DONE → INIT) |

### `TaskStateHandler`

Интерфейс каждого состояния:

```kotlin
interface TaskStateHandler {
    val stateName: StateId

    // System prompt для LLM — формируется с учётом данных предыдущих состояний
    suspend fun buildSystemPrompt(context: StateContext): String

    // Pre-fill ответ ассистента (показывается когда история пустая)
    suspend fun buildAssistantPreFillPrompt(context: StateContext): String?

    // Основная логика: парсит rawResponse, сохраняет данные, возвращает HandlerResult
    suspend fun handle(context: StateContext, userInput: String, rawResponse: String): HandlerResult

    // Обработка кнопок/действий пользователя (ACTION_* константы)
    suspend fun handleUserAction(context: StateContext, action: String): HandlerResult
}
```

### `HandlerResult`

Возвращается из `handle()` и `handleUserAction()`:

```kotlin
HandlerResult(
    messages   = listOf(Message("текст"), Message("инфо", isInfo=true), Message("заголовок", isTitle=true)),
    llmRequest = HandlerResult.LlmRequest(userPrompt = "..."),  // null = без повторного вызова LLM
    errorMessage = null
)
```

Если `llmRequest != null` — `TaskWorker` немедленно вызывает `doWork()` ещё раз с указанным промптом. Это используется для:
- **Автоматического перехода состояний** (PLANNING → EXECUTION без ввода пользователя)
- **Эскалации** (переход в DONE с инструкцией LLM закрыть тикет через инструмент)

---

## Паттерны в обработчиках

### Парсинг ответа LLM

Все обработчики используют `TaskResponseParser.parse(rawResponse)`:

```kotlin
val llmResponse = TaskResponseParser.parse(rawResponse)
    ?: return HandlerResult(
        messages = listOf(HandlerResult.Message("⚠️ Не удалось разобрать ответ...", isInfo = true))
    )
// llmResponse.replyToUser, llmResponse.nextState, llmResponse.memoryUpdates, llmResponse.userApprove
```

LLM должна отвечать в JSON-формате (поддерживаются markdown-блоки ` ```json ... ``` `):

```json
{
  "reply_to_user": "Текст для пользователя",
  "memory_updates": { "ключ": "значение" },
  "next_state": "execution",
  "user_approve": "true"
}
```

### Кнопки действий

```kotlin
// В handle() — добавить кнопку к сообщению
HandlerResult(
    messages = listOf(HandlerResult.Message(llmResponse.replyToUser))
        .withButton(action = ACTION_PROCEED, title = "Да, готово")
)

// В handleUserAction() — обработать нажатие
ACTION_PROCEED -> {
    context.updateState(NEXT_STATE)
    HandlerResult(
        messages = emptyList<HandlerResult.Message>()
            .withTitle(context.buildStateTransitionInfoMessage(1, NEXT_STATE)),
        llmRequest = HandlerResult.LlmRequest()  // пустой userPrompt → LLM вызывается для нового состояния
    )
}
```

### Эскалация к оператору

```kotlin
if (llmResponse.memoryUpdates["escalate_to_operator"] == "true") {
    context.updateState(SupportStateConfig.DONE)
    return HandlerResult(
        messages = listOf(HandlerResult.Message(llmResponse.replyToUser)),
        llmRequest = HandlerResult.LlmRequest(
            userPrompt = "Вызови update_crm_ticket(ticketId=$ticketId, status='operator')..."
        )
    )
}
```

---

## Как настроить TaskWorker под новую задачу

### Шаг 1. Определить состояния и данные

Создать `MyStateConfig.kt` и `MyStateData.kt`:

```kotlin
// MyStateConfig.kt
object MyStateConfig {
    val INIT = StateId("init")
    val WORKING = StateId("working")
    val DONE = StateId("done")

    val config = StateConfig(
        states = listOf(INIT, WORKING, DONE),
        transitions = mapOf(INIT to listOf(WORKING), WORKING to listOf(DONE)),
        handlers = mapOf(
            INIT to MyInitHandler(),
            WORKING to MyWorkingHandler(),
            DONE to MyDoneHandler()
        ),
        initialState = INIT,
        finalStates = emptyList(),   // [] для зацикленного, listOf(DONE) для завершаемого
        fallbackState = INIT,
        fallbackStateData = MyStateData.Init(),
        stateInfoProvider = ...
    )
}

// MyStateData.kt
sealed interface MyStateData : StateData {
    @Serializable
    data class Init(...) : MyStateData {
        override val state get() = MyStateConfig.INIT
        override val history: List<TaskStateMessage> = emptyList()
    }
    // ...
}
```

### Шаг 2. Реализовать обработчики

```kotlin
class MyInitHandler : TaskStateHandler {
    override val stateName = MyStateConfig.INIT

    override suspend fun buildSystemPrompt(context: StateContext): String = """
        Ты — ассистент. [Протокол ответа — JSON]
        { "reply_to_user": "...", "next_state": "working или null" }
    """.trimIndent()

    override suspend fun buildAssistantPreFillPrompt(context: StateContext) = "Начну работу."

    override suspend fun handle(context: StateContext, userInput: String, rawResponse: String): HandlerResult {
        val llmResponse = TaskResponseParser.parse(rawResponse)
            ?: return HandlerResult(listOf(HandlerResult.Message("⚠️ Ошибка парсинга", isInfo = true)))

        val data = MyStateData.Init(...)
        context.saveStateData(data)

        return if (llmResponse.nextState == "working") {
            context.updateState(MyStateConfig.WORKING)
            HandlerResult(
                messages = listOf(HandlerResult.Message(llmResponse.replyToUser)),
                llmRequest = HandlerResult.LlmRequest()
            )
        } else {
            HandlerResult(messages = listOf(HandlerResult.Message(llmResponse.replyToUser)))
        }
    }

    override suspend fun handleUserAction(context: StateContext, action: String) =
        HandlerResult(messages = listOf(HandlerResult.Message("Неизвестная команда", isInfo = true)))
}
```

### Шаг 3. Создать Dagger-модуль

```kotlin
// MyStateMachineModule.kt
@Module
internal object MyStateMachineModule {

    @Provides @Named("my_task") @Singleton
    internal fun provideMyStateStore(
        agentMemoryRepository: AgentMemoryRepository,
        agentContextRepository: AgentContextRepository
    ): StateStore = StateStoreImpl(agentMemoryRepository, agentContextRepository, MyStateConfig.config)

    @Provides @Named("my_task")
    internal fun provideMyWorker(
        ...,
        @Named("my_task") stateStore: StateStore
    ): TaskWorker = TaskWorker(..., stateStore = stateStore, agentName = "my_task_agent")
}
```

### Шаг 4. Подключить к UI

```kotlin
// В ConsoleFeatureDeps
@get:Named("my_task") val myWorker: TaskWorker

// В ConsoleViewModelImpl — добавить Factory
class MyFactory @Inject constructor(@Named("my_task") private val myWorker: TaskWorker, ...) {
    fun create(...) = ConsoleViewModelImpl(..., talkDelegate = MyTalkDelegate(myWorker, ...))
}
```

---

## Зацикленный vs завершаемый автомат

| | Зацикленный (Support) | Завершаемый (Task) |
|---|---|---|
| `finalStates` | `emptyList()` | `listOf(DONE)` |
| `transitions[DONE]` | `listOf(INIT)` | нет (или `listOf(INIT)` если нужен ресет) |
| В `DoneHandler.handle()` | `context.clearTaskMemory()` | нет |
| Поведение | После DONE следующее сообщение стартует с INIT | Автомат остаётся в DONE |

---

## Файловая структура

```
states_config/
├── MyStateConfig.kt          — StateConfig со всеми состояниями, переходами, обработчиками
├── MyStateData.kt            — sealed interface : StateData (по классу на состояние)
├── MyMemKeys.kt              — константы ключей JSON-ответа LLM
└── handlers/
    └── my_task/
        ├── MyInitHandler.kt
        ├── MyWorkingHandler.kt
        └── MyDoneHandler.kt

agent/di/
└── MyStateMachineModule.kt   — @Named("my_task") StateStore + TaskWorker
```
