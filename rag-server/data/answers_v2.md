# Answers: JetBrains Koog Documentation

## Q1: "какие основные возможности агента, какой класс реализует"

Koog Agents — это Kotlin-фреймворк для создания AI-агентов с поддержкой tool calling, сложных workflow и пользовательского взаимодействия. Основные классы агентов:

### Типы агентов

**`AIAgent`** [КЛАСС 1] — основной класс агента, поддерживающий как простые single-run агенты, так и сложные workflow-агенты. Создание агента:

```kotlin
val agent = AIAgent(
    executor = simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")),
    systemPrompt = "You are a helpful assistant. Answer user questions concisely.",
    llmModel = OpenAIModels.Chat.GPT4o,
    temperature = 0.7,
    toolRegistry = ToolRegistry {
        tool(SayToUser)
    }
)
```

**Complex workflow agents** [КЛАСС 2] позволяют создавать сложные стратегии с множеством узлов и рёбер:

```kotlin
val agent = AIAgent(
    promptExecutor = promptExecutor,
    toolRegistry = toolRegistry,
    strategy = agentStrategy,
    agentConfig = agentConfig,
    installFeatures = {
        install(EventHandler) { ... }
    }
)
```

### Основные возможности агента

1. **Tool calling** — агенты могут вызывать инструменты через LLM [КЛАСС 3]
2. **Custom strategies** — определение собственных workflow через DSL [КЛАСС 2]
3. **Features** — расширяемость через EventHandler, Tracing, AgentMemory [КЛАСС 4]
4. **Event handling** — подписка на события жизненного цикла агента [КЛАСС 5]

### Источники

[КЛАСС 1] AIAgent.kt
[КЛАСС 2] AIAgentStrategy.kt
[КЛАСС 3] Tool.kt
[КЛАСС 4] AgentMemory.kt
[КЛАСС 5] EventHandler.kt

---

## Q2: "как конфигурируется агент"

Агент конфигурируется через конструктор `AIAgent` и `AIAgentConfig` с несколькими ключевыми параметрами:

### Основные параметры конфигурации

**1. Prompt Executor** — исполнитель промптов для LLM:
```kotlin
val promptExecutor = simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY"))
```

**2. Tool Registry** — реестр доступных инструментов [КЛАСС 6]:
```kotlin
val toolRegistry = ToolRegistry {
    tool(SayToUser)
    tool(AskUser)
    tool(ExitTool)
}
```

**3. Agent Config** — общая конфигурация [КЛАСС 7]:
```kotlin
val agentConfig = AIAgentConfig(
    prompt = Prompt.build("simple-calculator") { ... },
    model = OpenAIModels.Chat.GPT4o,
    maxAgentIterations = 10
)
```

**4. Strategy** — стратегия выполнения [КЛАСС 2]:
```kotlin
val agentStrategy = strategy("Simple calculator") {
    val nodeSendInput by nodeLLMRequest()
    val nodeExecuteTool by nodeExecuteTool()
    // ...
}
```

**5. Install Features** — установка дополнительных возможностей [КЛАСС 5]:
```kotlin
installFeatures = {
    install(EventHandler) {
        onBeforeAgentStarted { eventContext: AgentStartContext<*> ->
            println("Starting strategy: ${eventContext.strategy.name}")
        }
    }
}
```

### Пример полной конфигурации

```kotlin
val agent = AIAgent(
    executor = simpleOpenAIExecutor(apiKey),
    systemPrompt = "You are a helpful assistant.",
    llmModel = OpenAIModels.Chat.GPT4o,
    toolRegistry = toolRegistry
)
```

### Источники

[КЛАСС 6] ToolRegistry.kt
[КЛАСС 7] AIAgentConfig.kt
[КЛАСС 2] AIAgentStrategy.kt
[КЛАСС 5] EventHandler.kt

---

## Q3: "как агент работает с историей сообщений"

### История сообщений и сессии

Агент работает с историей через **LLM Sessions** и **History Compression Strategies** [КЛАСС 8].

### Стратегии сжатия истории

**`HistoryCompressionStrategy`** позволяет управлять историей сообщений [КЛАСС 9]:

**`FromLastNMessages`** — сохраняет только последние N сообщений:
```kotlin
llm.writeSession {
    replaceHistoryWithTLDR(HistoryCompressionStrategy.FromLastNMessages(10), preserveMemory = true)
}
```

**`WholeHistory`** — суммирует всю историю в TL;DR [КЛАСС 10]:
```kotlin
llm.writeSession {
    replaceHistoryWithTLDR(HistoryCompressionStrategy.WholeHistory, preserveMemory = true)
}
```

### Node для сжатия истории

```kotlin
val compressHistory by nodeLLMCompressHistory<String>(
    strategy = HistoryCompressionStrategy.FromLastNMessages(10),
    preserveMemory = true
)
edge(generateHugeHistory forwardTo compressHistory)
```

### Подход с `RetrieveFactsFromHistory`

```kotlin
val compressHistory by nodeLLMCompressHistory<ProcessedInput>(
    strategy = RetrieveFactsFromHistory(
        Concept(
            keyword = "user_preferences",
            description = "User's preferences...",
            factType = FactType.MULTIPLE
        )
    )
)
```

### Источники

[КЛАСС 8] AIAgentLLMSession.kt
[КЛАСС 9] HistoryCompressionStrategy.kt
[КЛАСС 10] AIAgentLLMActions.kt

---

## Q4: "как реализованы стратегии компактизации контекста"

Стратегии компактизации контекста реализованы через **History Compression Strategies** [КЛАСС 9]:

### Доступные стратегии

1. **`FromLastNMessages(n)`** — сохраняет только последние n сообщений [КЛАСС 9]

2. **`WholeHistory`** — заменяет всю историю одним TL;DR резюме [КЛАСС 9]

3. **`RetrieveFactsFromHistory`** — извлекает конкретные факты из истории на основе концепций:
```kotlin
val strategy = RetrieveFactsFromHistory(
    Concept(
        keyword = "user_preferences",
        description = "User's preferences for the recommendation system",
        factType = FactType.MULTIPLE
    )
)
```

### Использование в стратегии

```kotlin
val compressHistory by nodeLLMCompressHistory<ProcessedInput>(
    strategy = HistoryCompressionStrategy.FromLastNMessages(5)
)
```

### Источники

[КЛАСС 9] HistoryCompressionStrategy.kt

---

## Q5: "как реализован tool calling"

### Концепция Tool calling

**Tool** — это функция, которую агент может использовать для выполнения задач или взаимодействия с внешними системами. **Tool call** — запрос LLM на выполнение инструмента [КЛАСС 3].

### Создание инструмента

```kotlin
class WebSearchTool: SimpleTool<WebSearchTool.Args>() {
    @Serializable
    class Args(val query: String) : ToolArgs

    override suspend fun doExecute(args: Args): String {
        return "Searching for ${args.query}..."
    }

    override val argsSerializer: KSerializer<Args> = Args.serializer()

    override val descriptor: ToolDescriptor = ToolDescriptor(
        name = "web_search",
        description = "Search on the web"
    )
}
```

### Tool events в Tracing

События tool calling отслеживаются через Tracing [КЛАСС 11]:

- **`ToolCallEvent`** — базовое событие вызова
- **`ToolValidationErrorEvent`** — ошибка валидации
- **`ToolCallFailureEvent`** — неудачный вызов
- **`ToolCallResultEvent`** — успешный результат

### Выполнение в стратегии

```kotlin
val nodeExecuteTool by nodeExecuteTool()
edge(nodeSendInput forwardTo nodeExecuteTool onToolCall { true })
```

### Параллельные tool calls

```kotlin
flow { emit(Book("Book 1", "Author 1", "Description 1")) }
    .toParallelToolCallsRaw(BookTool::class).collect()
```

### Источники

[КЛАСС 3] Tool.kt
[КЛАСС 11] ToolCallEvent.kt

---

## Q6: "как реализован механизм user in the loop"

### Встроенные инструменты для взаимодействия с пользователем

Koog предоставляет встроенные инструменты для user in the loop [КЛАСС 12]:

**`SayToUser`** — отправка сообщения пользователю:
```kotlin
tool(SayToUser)   // Send message to user
```

**`AskUser`** — запрос ввода от пользователя:
```kotlin
tool(AskUser)     // Ask user for input
```

**`ExitTool`** — завершение сессии:
```kotlin
tool(ExitTool)    // Terminate session
```

### Пример использования

```kotlin
val toolRegistry = ToolRegistry {
    tool(SayToUser)   // Send message to user
    tool(AskUser)     // Ask user for input
    tool(ExitTool)    // Terminate session
}

val agent = AIAgent(
    executor = simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")),
    systemPrompt = "You are a helpful assistant. Use tools to communicate.",
    llmModel = OpenAIModels.Chat.GPT4o,
    toolRegistry = toolRegistry
)

agent.run("Greet the user and ask their name")
```

### Источники

[КЛАСС 12] BuiltInTools.kt

---

## Q7: "как агенты работают с памятью и сколько слоев используют"

### AgentMemory Feature

Агенты работают с памятью через **AgentMemory** feature [КЛАСС 4]:

### Архитектура памяти

**`AgentMemoryProvider`** — интерфейс для сохранения и загрузки фактов:
```kotlin
val memoryProvider = LocalFileMemoryProvider(
    config = LocalMemoryConfig("my-agent-memory"),
    storage = SimpleStorage(JVMFileSystemProvider.ReadWrite),
    fs = JVMFileSystemProvider.ReadWrite,
    root = Path("./memory")
)
```

### Слои памяти (Memory Scopes)

Система использует **MemoryScopes** для организации данных [КЛАСС 13]:

- **`MemoryScope.Product("my-app")`** — память продукта
- **`MemoryScope.Feature("chat")`** — память фичи
- **`MemoryScope.Agent("assistant")`** — память агента
- **`MemoryScope.CrossProduct`** — глобальная память

### Концепты и факты

**`Concept`** — тип знания:
```kotlin
val concept = Concept(
    keyword = "user-name",
    description = "User's name",
    factType = FactType.SINGLE  // или FactType.MULTIPLE
)
```

**`SingleFact`** и **`MultipleFacts`** — хранимые значения:
```kotlin
memoryProvider.save(
    fact = SingleFact(
        concept = concept,
        value = "John",
        timestamp = DefaultTimeProvider.getCurrentTimestamp()
    ),
    subject = MemorySubject.Everything,
    scope = MemoryScope.Product("my-app")
)
```

### Установка в агента

```kotlin
val agent = AIAgent(...) {
    install(AgentMemory) {
        this.memoryProvider = memoryProvider
        agentName = "assistant"
        featureName = "chat"
        organizationName = "myorg"
        productName = "my-app"
    }
}
```

### Источники

[КЛАСС 4] AgentMemory.kt
[КЛАСС 13] MemoryScope.kt

---

## Q8: "как агенты работают с mcp"

### Model Context Protocol (MCP)

Koog поддерживает **MCP** для интеграции с внешними MCP-серверами [КЛАСС 14]:

### Подключение к MCP серверу

**Через SSE transport:**
```kotlin
val toolRegistry = McpToolRegistryProvider.fromTransport(
    transport = McpToolRegistryProvider.defaultSseTransport("http://localhost:8931")
)
```

**Через stdio transport (Docker):**
```kotlin
val process = ProcessBuilder(
    "docker", "run", "-i",
    "-e", "GOOGLE_MAPS_API_KEY=${System.getenv("GOOGLE_MAPS_API_KEY")}",
    "mcp/google-maps"
).start()

val toolRegistry = McpToolRegistryProvider.fromTransport(
    transport = McpToolRegistryProvider.defaultStdioTransport(process)
)
```

**Через существующий MCP клиент:**
```kotlin
val existingMcpClient = Client(clientInfo = Implementation(name = "mcpClient", version = "dev"))
val toolRegistry = McpToolRegistryProvider.fromClient(mcpClient = existingMcpClient)
```

### Пример с Playwright MCP

```kotlin
val process = ProcessBuilder(
    "npx", "@playwright/mcp@latest", "--port", "8931"
).start()

val toolRegistry = McpToolRegistryProvider.fromTransport(
    transport = McpToolRegistryProvider.defaultSseTransport("http://localhost:8931")
)

val agent = AIAgent(
    executor = simpleOpenAIExecutor(openAIApiToken),
    llmModel = OpenAIModels.Chat.GPT4o,
    toolRegistry = toolRegistry,
)
agent.run("Open a browser, navigate to jetbrains.com...")
```

### Использование с агентом

```kotlin
val agent = AIAgent(
    executor = executor,
    strategy = strategy,
    llmModel = OpenAIModels.Chat.GPT4o,
    toolRegistry = toolRegistry
)
val result = agent.run("Use the MCP tool to perform a task")
```

### Источники

[КЛАСС 14] McpToolRegistryProvider.kt

---

## Q9: "какие возможности есть по выстраиванию пайпланов и разбитие задач на подзадачи"

### Subgraphs — основной механизм

**Subgraph** — это самодостаточная единица обработки внутри стратегии агента, с собственным набором инструментов, контекстом и ответственностью [КЛАСС 15].

### Создание subgraph

```kotlin
val researchSubgraph by subgraph<String, String>(
    "research_subgraph",
    tools = listOf(WebSearchTool())
) {
    val nodeCallLLM by nodeLLMRequest("call_llm")
    val nodeExecuteTool by nodeExecuteTool()
    val nodeSendToolResult by nodeLLMSendToolResult()

    edge(nodeStart forwardTo nodeCallLLM)
    edge(nodeCallLLM forwardTo nodeExecuteTool onToolCall { true })
    edge(nodeExecuteTool forwardTo nodeSendToolResult)
    edge(nodeSendToolResult forwardTo nodeExecuteTool onToolCall { true })
    edge(nodeCallLLM forwardTo nodeFinish onAssistantMessage { true })
}
```

### Последовательное выполнение subgraphs

```kotlin
nodeStart then inputProcessing then reasoning then toolRun then responseGeneration then nodeFinish
```

### subgraphWithTask — утилита для задач

```kotlin
val processQuery by subgraphWithTask<String>(
    tools = listOf(searchTool, calculatorTool, weatherTool),
    llmModel = OpenAIModels.Chat.GPT4o,
) {
    "You are a helpful assistant..."
}
```

### Преимущества subgraphs

1. **Модульность** — каждый subgraph может иметь свой набор инструментов [КЛАСС 15]
2. **Изоляция** — собственный контекст выполнения
3. **Переиспользование** — можно использовать одни subgraph в разных стратегиях

### Источники

[КЛАСС 15] AIAgentSubgraph.kt

---

## Q10: "как агенты могу общаться друг с другом"

### Agent-to-Agent Communication

В Koog агенты могут взаимодействовать через **subgraphs** в рамках одной стратегии, где результат одного subgraph передаётся в другой [КЛАСС 2]:

### Через subgraphs

```kotlin
val firstSubgraph by subgraph<String, String>("first") { ... }
val secondSubgraph by subgraph<String, String>("second") { ... }

edge(nodeStart forwardTo firstSubgraph)
edge(firstSubgraph forwardTo secondSubgraph)
edge(secondSubgraph forwardTo nodeFinish)
```

### Через shared tools

Агенты могут использовать общие инструменты для коммуникации:

```kotlin
tool(SayToUser)   // Отправка сообщения
tool(AskUser)     // Запрос ввода
```

### Через AgentMemory

Агенты могут обмениваться информацией через общую память [КЛАСС 4]:

```kotlin
install(AgentMemory) {
    memoryProvider = sharedMemoryProvider
    productName = "shared-product"
}
```

### Источники

[КЛАСС 2] AIAgentStrategy.kt
[КЛАСС 4] AgentMemory.kt

---

## Q11: "какие возможности по работе с сессиями"

### LLM Sessions

Агент работает с LLM через **sessions**, которые позволяют управлять историей и параметрами [КЛАСС 8].

### Управление историей в сессии

**`writeSession`** — доступ к сессии для записи:
```kotlin
llm.writeSession {
    replaceHistoryWithTLDR(HistoryCompressionStrategy.FromLastNMessages(10))
}
```

### История сообщений

**`AIAgentLLMSession`** — интерфейс сессии [КЛАСС 8]:

- **`prompt`** — управление промптом и сообщениями
- **`tools`** — управление инструментами
- **`model`** — выбор модели LLM

### Сжатие истории

```kotlin
llm.writeSession {
    replaceHistoryWithTLDR(HistoryCompressionStrategy.WholeHistory, preserveMemory = true)
}
```

### Параметры LLM

```kotlin
val agentConfig = AIAgentConfig(
    prompt = Prompt.build("my-prompt") { ... },
    model = OpenAIModels.Chat.GPT4o,
    maxAgentIterations = 10
)
```

### Источники

[КЛАСС 8] AIAgentLLMSession.kt

---

## Q12: "как реализован state management"

### State management в стратегиях

State management реализован через:
- **Subgraph context** — каждый subgraph имеет доступ к состоянию через контекст [КЛАСС 16]
- **Agent Memory** — персистентное хранение фактов

### Subgraph Context

Каждый subgraph выполняется в контексте, который предоставляет доступ к:
- Окружению (environment)
- Входным данным агента
- Конфигурации агента
- LLM контексту (включая историю)
- State manager
- Storage

### AgentMemory для состояния

```kotlin
install(AgentMemory) {
    memoryProvider = memoryProvider
    agentName = "assistant"
    productName = "my-app"
}
```

### Управление состоянием в узлах

```kotlin
val myNode by node<String, String>("node_name") { input ->
    // Получение данных из контекста
    val state = environment.getState()
    // Обработка и возврат результата
    input
}
```

### Источники

[КЛАСС 16] AIAgentContext.kt

---

## Q13: "как реализована обработка ошибок"

### Tool Call Events — ошибки

Обработка ошибок реализована через события в Tracing [КЛАСС 11]:

**`ToolValidationErrorEvent`** — ошибка валидации аргументов:
```kotlin
// Событие содержит:
// - toolName: имя инструмента
// - toolArgs: аргументы
// - errorMessage: сообщение об ошибке
```

**`ToolCallFailureEvent`** — неудачный вызов инструмента:
```kotlin
// Событие содержит:
// - toolName: имя инструмента
// - toolArgs: аргументы
// - error: AIAgentError
```

### Результат выполнения

**`ToolCallResultEvent`** — успешный результат:
```kotlin
// Событие содержит:
// - toolName: имя инструмента
// - toolArgs: аргументы
// - result: ToolResult
```

### Тестирование ошибок

```kotlin
mockTool(myTool) alwaysReturns myResult
// или с условием
mockTool(myTool) returns myResult onArguments myArgs
```

### Источники

[КЛАСС 11] ToolCallEvent.kt

---

## Q14: "какие механизмы работы с опасными tool calling"

### Информация неполная

В текущей документации Koog Agents нет явного описания механизма "dangerous tool calling" или approval workflow для опасных операций.

### Возможные подходы

1. **User in the loop** — использование `AskUser` для подтверждения опасных действий [КЛАСС 12]

2. **Custom tool validation** — кастомная валидация в `doExecute`:
```kotlin
override suspend fun doExecute(args: DangerousArgs): String {
    if (args.isDangerous) {
        throw AgentException("Dangerous operation requires approval")
    }
    return "Executed safely"
}
```

3. **Tool selection strategy** — ограничение набора инструментов в subgraph:
```kotlin
val safeSubgraph by subgraph<String, String>(
    "safe_subgraph",
    tools = listOf(safeTool1, safeTool2)  // Без опасных инструментов
) { ... }
```

### Источники

[КЛАСС 12] BuiltInTools.kt

---

## Q15: "где хранится текущий диалог агента с пользователем и какие возможности по его управлению"

### Хранение диалога

Диалог хранится в **LLM Session** как история сообщений [КЛАСС 8]:

### Управление историей сообщений

**`writeSession`** используется для модификации истории:
```kotlin
llm.writeSession {
    rewritePrompt { ... }  // Перезаписать промпт
    updatePrompt { ... }   // Обновить промпт
}
```

### Сжатие истории

```kotlin
replaceHistoryWithTLDR(HistoryCompressionStrategy.FromLastNMessages(10))
```

### AgentMemory для персистентного хранения

```kotlin
install(AgentMemory) {
    memoryProvider = memoryProvider
    productName = "my-app"
}
```

### Структура хранения в LocalFileMemoryProvider

```
root/
  agent/[agent-name]/subject/[subject-name]/facts.json
  feature/[feature-id]/subject/[subject-name]/facts.json
  product/[product-name]/subject/[subject-name]/facts.json
```

### Источники

[КЛАСС 8] AIAgentLLMSession.kt

---

## Q16: "как устроена observability и работа с событиями от агентов"

### Tracing Feature

**Tracing** — основной механизм observability [КЛАСС 17]:

```kotlin
install(Tracing) {
    addMessageProcessor(TraceFeatureMessageLogWriter(logger))
}
```

### Фильтрация событий

```kotlin
install(Tracing) {
    // Only trace LLM calls
    messageFilter = { message ->
        message is BeforeLLMCallEvent || message is AfterLLMCallEvent
    }
    addMessageProcessor(writer)
}
```

### Типы событий в Tracing

- **`BeforeLLMCallEvent`** / **`AfterLLMCallEvent`** — вызовы LLM
- **`ToolCallEvent`** — вызов инструмента
- **`ToolValidationErrorEvent`** — ошибка валидации
- **`ToolCallFailureEvent`** — неудачный вызов
- **`ToolCallResultEvent`** — результат инструмента

### EventHandler для жизненного цикла

```kotlin
install(EventHandler) {
    onBeforeAgentStarted { eventContext: AgentStartContext<*> ->
        println("Starting strategy: ${eventContext.strategy.name}")
    }
    onAgentFinished { eventContext: AgentFinishedContext ->
        println("Result: ${eventContext.result}")
    }
}
```

### OpenTelemetry Support

```kotlin
install(OpenTelemetry) {
    // Configuration options
}
```

### Источники

[КЛАСС 17] Tracing.kt

---

## Q17: "что такое GraphAIAgent, каковы его преимущества и схемы использования?"

### GraphAIAgent

**`GraphAIAgent`** — это агент, который использует **graph-based strategy** для определения workflow через nodes и edges [КЛАСС 2].

### Преимущества

1. **Визуальное представление** — граф легче понимать и отлаживать
2. **Гибкость** — можно определять сложные маршруты
3. **Subgraphs** — модульность через subgraphs
4. **Условия** — edges могут иметь условия перехода

### Схема использования

```kotlin
val strategy = strategy<String, String>("assistant") {
    val researchSubgraph by subgraph<String, String>(
        "research_subgraph",
        tools = listOf(WebSearchTool())
    ) { ... }

    val planSubgraph by subgraph<String, String>("plan_subgraph") { ... }
    val executeSubgraph by subgraph<String, String>("execute_subgraph") { ... }

    // Определение маршрута
    nodeStart then researchSubgraph then planSubgraph then executeSubgraph then nodeFinish
}
```

### Edges с условиями

```kotlin
edge(nodeCallLLM forwardTo nodeExecuteTool onToolCall { true })
edge(nodeCallLLM forwardTo nodeFinish onAssistantMessage { true })
```

### Источники

[КЛАСС 2] AIAgentStrategy.kt

---

## Q18: "все полезное про AIAgentContext и AIAgentGraphContext"

### AIAgentContext

**`AIAgentContext`** предоставляет доступ к окружению агента во время выполнения [КЛАСС 16].

### Subgraph Context

Каждый subgraph выполняется в контексте, который предоставляет доступ к:

- **`environment`** — окружение для выполнения инструментов
- **`agentInput`** — входные данные агента
- **`agentConfig`** — конфигурация агента
- **`llm`** — LLM контекст (история, промпт)
- **State manager** — управление состоянием
- **Storage** — хранилище

### Использование в узле

```kotlin
val myNode by node<String, String>("node_name") { input ->
    // Доступ к окружению
    val toolResult = environment.executeTool(toolCall)
    
    // Доступ к LLM сессии
    llm.writeSession {
        updatePrompt { user("Additional context") }
    }
    
    input
}
```

### AIAgentGraphContext

Для graph-based стратегий контекст дополнительно предоставляет доступ к информации о графе и subgraphs.

### Источники

[КЛАСС 16] AIAgentContext.kt

---

## Q19: "опиши работу с AIAgentLLMContext"

### AIAgentLLMContext

**`AIAgentLLMContext`** — интерфейс для работы с LLM в рамках сессии агента [КЛАСС 8].

### Основные операции

**`writeSession`** — запись в сессию:
```kotlin
llm.writeSession {
    // Модификация промпта
    rewritePrompt { prompt("new_prompt") { ... } }
    updatePrompt { user("additional") }
}
```

**`replaceHistoryWithTLDR`** — сжатие истории:
```kotlin
llm.writeSession {
    replaceHistoryWithTLDR(HistoryCompressionStrategy.WholeHistory)
}
```

### Доступные данные

- **`prompt`** — текущий промпт с историей сообщений
- **`model`** — модель LLM
- **`tools`** — доступные инструменты

### API Sessions

Sessions создаются через extension functions на `AIAgentLLMContext`:
> "Sessions are created using extension functions on the `AIAgentLLMContext` class. These functions take a lambda block that runs within the context of the session."

### Источники

[КЛАСС 8] AIAgentLLMSession.kt

---

## Q20: "опиши назначение и как работать с AIAgentPlannerContext"

### AIAgentPlannerContext

**`AIAgentPlannerContext`** предоставляет контекст для агентов со стратегией планирования (planner-based agents).

### Использование в PlannerAgent

```kotlin
val plannerAgent = PlannerAIAgent(
    config = myAgentConfig,
    strategy = plannerStrategy
) {
    installFeatures { ... }
}
```

### Особенности PlannerAgent

1. **Создание подзадач** — разбивает задачу на подзадачи
2. **Последовательное выполнение** — выполняет подзадачи в порядке
3. **Контекст окружения** — использует `GenericAgentEnvironment`

### Контекст в узле

```kotlin
val planNode by node<String, PlanResult>("planner") { input ->
    // Использование planner context для генерации плана
    val plan = generatePlan(input)
    PlanResult(plan)
}
```

### Источники

[КЛАСС 18] PlannerAIAgent.kt

---

## Q21: "что такое AIAgentGraphStrategy и примеры использования"

### AIAgentGraphStrategy

**`AIAgentGraphStrategy`** — стратегия, основанная на графах, где workflow определяется как набор **nodes** (узлов) и **edges** (рёбер) [КЛАСС 2].

### Ключевые компоненты

- **Node** — операция или преобразование данных
- **Edge** — связь между узлами с условием перехода
- **Subgraph** — группа узлов с общим контекстом

### Пример определения стратегии

```kotlin
val strategy = strategy<String, String>("assistant") {
    val nodeSendInput by nodeLLMRequest()
    val nodeExecuteTool by nodeExecuteTool()
    val nodeSendToolResult by nodeLLMSendToolResult()

    // Определение рёбер
    edge(nodeStart forwardTo nodeSendInput)
    edge(nodeSendInput forwardTo nodeExecuteTool onToolCall { true })
    edge(nodeExecuteTool forwardTo nodeSendToolResult)
    edge(nodeSendToolResult forwardTo nodeFinish onAssistantMessage { true })
}
```

### Условия на edges

```kotlin
edge(nodeCallLLM forwardTo nodeExecuteTool onToolCall { true })
edge(nodeCallLLM forwardTo nodeFinish onAssistantMessage { true })
```

### Преимущества

1. **Гибкость** — можно определить любой workflow
2. **Условия** — динамическая маршрутизация
3. **Модульность** — subgraphs как строительные блоки

### Источники

[КЛАСС 2] AIAgentStrategy.kt

---

## Q22: "подробно про AIAgentNode и его преимущества"

### AIAgentNode

**Node** — это фундаментальный строительный блок в workflow агента, представляющий операцию или преобразование данных [КЛАСС 19].

### Типы узлов

**1. `nodeLLMRequest()`** — запрос к LLM:
```kotlin
val nodeSendInput by nodeLLMRequest()
```

**2. `nodeExecuteTool()`** — выполнение инструмента:
```kotlin
val nodeExecuteTool by nodeExecuteTool()
```

**3. `nodeLLMSendToolResult()`** — отправка результата инструмента:
```kotlin
val nodeSendToolResult by nodeLLMSendToolResult()
```

**4. `nodeLLMCompressHistory()`** — сжатие истории:
```kotlin
val compressHistory by nodeLLMCompressHistory<String>(
    strategy = HistoryCompressionStrategy.FromLastNMessages(10)
)
```

**5. Кастомный узел:**
```kotlin
val myNode by node<String, String>("node_name") { input ->
    // Логика узла
    "result: $input"
}
```

### Преимущества

1. **Типизация** — generic типы для входа и выхода
2. **Композиция** — узлы можно комбинировать
3. **Переиспользование** — одни узлы можно использовать в разных стратегиях
4. **Тестируемость** — легко тестировать отдельные узлы

### Источники

[КЛАСС 19] AIAgentNode.kt

---

## Q23: "как использовать AIAgentState"

### AIAgentState

**`AIAgentState`** используется для управления состоянием агента во время выполнения.

### Доступ к состоянию

Через **AgentMemory** [КЛАСС 4]:
```kotlin
install(AgentMemory) {
    memoryProvider = memoryProvider
    agentName = "assistant"
    productName = "my-app"
}
```

### Сохранение фактов

```kotlin
val saveProjectInfo by node<Unit, Unit> {
    withMemory {
        saveFactsFromHistory(
            Concept("project-name", "Name of the project", FactType.SINGLE),
            subject = MemorySubjects.User,
            scope = MemoryScope.Product("my-app")
        )
    }
}
```

### Загрузка фактов

```kotlin
val loadProjectInfo by node<Unit, Unit> {
    withMemory {
        loadFactsToAgent(
            Concept("project-name", "Name of the project", FactType.SINGLE)
        )
    }
}
```

### Snapshot и Persistency

Для более продвинутого state management доступен snapshot mechanism [КЛАСС 20]:
```kotlin
import ai.koog.agents.snapshot.feature.persistency

context.persistency().setExecutionPoint(
    agentContext = context,
    nodeId = "target-node-id",
    messageHistory = customMessageHistory,
    input = customInput
)
```

### Источники

[КЛАСС 4] AgentMemory.kt
[КЛАСС 20] AgentPersistency.kt

---

## Q24: "зачем нужен AIAgentStorage"

### AIAgentStorage

**`AIAgentStorage`** предоставляет интерфейс для персистентного хранения данных агента.

### Использование в AgentMemory

**`LocalFileMemoryProvider`** использует `SimpleStorage` для файлового хранилища [КЛАСС 13]:
```kotlin
val memoryProvider = LocalFileMemoryProvider(
    config = LocalMemoryConfig("my-agent-memory"),
    storage = SimpleStorage(JVMFileSystemProvider.ReadWrite),
    fs = JVMFileSystemProvider.ReadWrite,
    root = Path("./memory")
)
```

### Операции Storage

- **save** — сохранение факта
- **load** — загрузка факта по концепту
- **loadAll** — загрузка всех фактов
- **loadByDescription** — загрузка по описанию

### Источники

[КЛАСС 13] MemoryScope.kt

---

## Q25: "как работает и зона ответственности AIAgentStrategy"

### AIAgentStrategy

**Strategy** определяет **что** делает агент и **как** он это делает — структуру workflow через nodes и edges [КЛАСС 2].

### Зона ответственности

1. **Определение workflow** — последовательность операций
2. **Маршрутизация** — условия переходов между узлами
3. **Обработка данных** — преобразование входных данных
4. **Интеграция subgraphs** — композиция сложных стратегий

### Создание стратегии

```kotlin
val strategy = strategy<String, String>("strategy-name") {
    // Определение узлов
    val firstNode by nodeLLMRequest()
    val executeTool by nodeExecuteTool()
    
    // Определение рёбер
    edge(nodeStart forwardTo firstNode)
    edge(firstNode forwardTo executeTool onToolCall { true })
    edge(executeTool forwardTo nodeFinish onAssistantMessage { true })
}
```

### Типы стратегий

- **Single-run strategy** — простая стратегия для одной итерации
- **Graph strategy** — графовая стратегия с множеством узлов
- **Custom strategy** — полностью кастомная логика

### Источники

[КЛАСС 2] AIAgentStrategy.kt

---

## Q26: "как работает и зона ответственности AIAgentSubgraph с примерами"

### AIAgentSubgraph

**Subgraph** — это **самостоятельная единица обработки** внутри стратегии с собственным набором инструментов и контекстом [КЛАСС 15].

### Зона ответственности

1. **Изоляция логики** — каждая подзадача в отдельном subgraph
2. **Инструменты** — свой набор инструментов или подмножество
3. **Контекст** — доступ к окружению, LLM, состоянию
4. **Композиция** — subgraphs соединяются в единый workflow

### Пример: Research → Plan → Execute

```kotlin
val strategy = strategy<String, String>("assistant") {
    // Research subgraph
    val researchSubgraph by subgraph<String, String>(
        "research_subgraph",
        tools = listOf(WebSearchTool())
    ) {
        val nodeCallLLM by nodeLLMRequest("call_llm")
        val nodeExecuteTool by nodeExecuteTool()
        val nodeSendToolResult by nodeLLMSendToolResult()

        edge(nodeStart forwardTo nodeCallLLM)
        edge(nodeCallLLM forwardTo nodeExecuteTool onToolCall { true })
        edge(nodeExecuteTool forwardTo nodeSendToolResult)
        edge(nodeCallLLM forwardTo nodeFinish onAssistantMessage { true })
    }

    // Plan subgraph
    val planSubgraph by subgraph<String, String>("plan_subgraph") {
        val nodeUpdatePrompt by node<String, Unit> { research ->
            llm.writeSession {
                rewritePrompt {
                    prompt("research_prompt") {
                        system("You are given research...")
                        user("Research: $research")
                    }
                }
            }
        }
        val nodeCallLLM by nodeLLMRequest("call_llm")
        edge(nodeStart forwardTo nodeUpdatePrompt)
        edge(nodeUpdatePrompt forwardTo nodeCallLLM)
        edge(nodeCallLLM forwardTo nodeFinish onAssistantMessage { true })
    }

    // Execute subgraph
    val executeSubgraph by subgraph<String, String>(
        "execute_subgraph",
        tools = listOf(DoAction(), DoAnotherAction())
    ) { ... }

    // Соединение subgraphs
    nodeStart then researchSubgraph then planSubgraph then executeSubgraph then nodeFinish
}
```

### Выбор инструментов для subgraph

```kotlin
val safeSubgraph by subgraph<String, String>(
    "safe_subgraph",
    tools = listOf(safeTool1, safeTool2)  // Только безопасные инструменты
) { ... }
```

### Источники

[КЛАСС 15] AIAgentSubgraph.kt

---

## Q27: "расскажи все про ExecutionPointNode"

### ExecutionPointNode

**ExecutionPointNode** (nodeStart, nodeFinish) — это специальные узлы, определяющие **начало и конец** выполнения subgraph или стратегии [КЛАСС 21].

### nodeStart

Точка входа — с неё начинается выполнение:
```kotlin
edge(nodeStart forwardTo firstNode)
```

### nodeFinish

Точка завершения — после неё выполнение прекращается:
```kotlin
edge(lastNode forwardTo nodeFinish)
```

### Использование в тестах

```kotlin
testGraph("test") {
    val start = startNode()
    val finish = finishNode()
    
    assertReachable(start, askLLM)
    assertReachable(askLLM, callTool)
}
```

### Продвинутое управление

**setExecutionPoint** позволяет установить произвольную точку выполнения [КЛАСС 20]:
```kotlin
context.persistency().setExecutionPoint(
    agentContext = context,
    nodeId = "target-node-id",
    messageHistory = customMessageHistory,
    input = customInput
)
```

### Источники

[КЛАСС 21] ExecutionPointNode.kt
[КЛАСС 20] AgentPersistency.kt

---

## Q28: "расскажи все про SubgraphMetadata"

### SubgraphMetadata

**SubgraphMetadata** содержит метаинформацию о subgraph, включая имя, типы входа/выхода и инструменты.

### Структура

При определении subgraph автоматически создаётся metadata:
```kotlin
val researchSubgraph by subgraph<String, String>(
    "research_subgraph",
    tools = listOf(WebSearchTool())
) { ... }
// metadata.name = "research_subgraph"
// metadata.inputType = String::class
// metadata.outputType = String::class
// metadata.tools = listOf(WebSearchTool)
```

### Доступ к metadata

В тестировании [КЛАСС 22]:
```kotlin
testGraph("test") {
    val firstSubgraph = assertSubgraphByName<String, String>("first")
    val secondSubgraph = assertSubgraphByName<String, String>("second")
    
    assertEdges {
        startNode() alwaysGoesTo firstSubgraph
        firstSubgraph alwaysGoesTo secondSubgraph
        secondSubgraph alwaysGoesTo finishNode()
    }
}
```

### Использование

1. **Валидация** — проверка структуры графа
2. **Документация** — описание subgraph
3. **Отладка** — понимание структуры workflow

### Источники

[КЛАСС 22] SubgraphMetadata.kt

---

## Q29: "примеры использования AIAgentSubgraphBuilder"

### AIAgentSubgraphBuilder

**`subgraph`** — это DSL функция для построения subgraphs [КЛАСС 15].

### Базовый пример

```kotlin
val firstSubgraph by subgraph<String, String>("first") {
    val nodeSendInput by nodeLLMRequest()
    val nodeExecuteTool by nodeExecuteTool()
    
    edge(nodeStart forwardTo nodeSendInput)
    edge(nodeSendInput forwardTo nodeExecuteTool onToolCall { true })
    edge(nodeExecuteTool forwardTo nodeFinish onAssistantMessage { true })
}
```

### С инструментами

```kotlin
val searchSubgraph by subgraph<String, String>(
    "search_subgraph",
    tools = listOf(WebSearchTool(), CalculatorTool())
) {
    // Узлы для работы с инструментами
}
```

### С типами

```kotlin
typealias FirstInput = String
typealias FirstOutput = Int

val typedSubgraph by subgraph<FirstInput, FirstOutput>("typed") {
    // input: String, output: Int
}
```

### subgraphWithTask

Готовый builder для типичных задач [КЛАСС 23]:
```kotlin
val processQuery by subgraphWithTask<String>(
    tools = listOf(searchTool, calculatorTool),
    llmModel = OpenAIModels.Chat.GPT4o,
) {
    "You are a helpful assistant that can answer questions..."
}
```

### Параметры subgraph

- **name** — уникальное имя
- **tools** — список инструментов (опционально)
- **inputType/outputType** — типы данных

### Источники

[КЛАСС 15] AIAgentSubgraph.kt
[КЛАСС 23] SubgraphBuilder.kt

---

## Q30-Q39

Информация неполная — для ответов на вопросы Q30-Q39 требуется дополнительный контекст из документации Koog Agents.

### Источники

[КЛАСС 1] AIAgent.kt
[КЛАСС 2] AIAgentStrategy.kt
[КЛАСС 3] Tool.kt
[КЛАСС 4] AgentMemory.kt
[КЛАСС 5] EventHandler.kt
[КЛАСС 6] ToolRegistry.kt
[КЛАСС 7] AIAgentConfig.kt
[КЛАСС 8] AIAgentLLMSession.kt
[КЛАСС 9] HistoryCompressionStrategy.kt
[КЛАСС 10] AIAgentLLMActions.kt
[КЛАСС 11] ToolCallEvent.kt
[КЛАСС 12] BuiltInTools.kt
[КЛАСС 13] MemoryScope.kt
[КЛАСС 14] McpToolRegistryProvider.kt
[КЛАСС 15] AIAgentSubgraph.kt
[КЛАСС 16] AIAgentContext.kt
[КЛАСС 17] Tracing.kt
[КЛАСС 18] PlannerAIAgent.kt
[КЛАСС 19] AIAgentNode.kt
[КЛАСС 20] AgentPersistency.kt
[КЛАСС 21] ExecutionPointNode.kt
[КЛАСС 22] SubgraphMetadata.kt
[КЛАСС 23] SubgraphBuilder.kt
