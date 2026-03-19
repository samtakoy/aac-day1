# Runtest: BASELINE | 2026-03-19 22:21

## Q1: "какие основные возможности агента, какой класс реализует"
Основные возможности AI-агента, включая управление конфигурацией, выполнение запросов, работу с инструментами (tools), установку расширений (features) и обработку событий жизненного цикла, реализуются через несколько ключевых классов, образующих иерархию и композицию:

### 1. **Базовый класс `AIAgentBase` (не показан в контексте)**
   Предположительно, это абстрактный базовый класс, определяющий общий контракт для всех типов агентов. Он задаёт основные свойства и методы, такие как:
   - `agentConfig: AIAgentConfig` — конфигурация агента [ИСТОЧНИК 3].
   - `toolRegistry: ToolRegistry` — реестр доступных инструментов [ИСТОЧНИК 3, 4, 5].
   - `execute(...)` — метод выполнения (явно присутствует у `GraphAIAgent` [ИСТОЧНИК 5]).
   - Управление pipeline и контекстом выполнения.
   *Примечание: класс `AIAgentBase` не входит в предоставленный контекст, но упоминается как родительский для `FunctionalAIAgent`, `PlannerAIAgent` и `GraphAIAgent`.*

### 2. **Конкретные реализации агентов**
   Каждый специализированный агент наследует базовый функционал и добавляет тип-specific поведение через стратегии (strategies):

   - **`FunctionalAIAgent<Input, Output>`** [ИСТОЧНИК 3]:
     - Реализует агентов с функциональной стратегией (`AIAgentFunctionalStrategy`).
     - Использует `AIAgentFunctionalPipeline` для управления pipeline.
     - Поддерживает `installFeatures` для установки дополнительных features.
     - Пример: агент, выполняющий прямые запросы к LLM с инструментами.

   - **`PlannerAIAgent<Input, Output>`** [ИСТОЧНИК 4]:
     - Реализует агентов со стратегией планирования (`AIAgentPlannerStrategy`).
     - Создаёт контекст `AIAgentPlannerContext` с окружением (`GenericAgentEnvironment`).
     - Также поддерживает `installFeatures` через `FeatureContext`.
     - Пример: агент, разбивающий задачу на подзадачи и выполняющий их последовательно.

   - **`GraphAIAgent<Input, Output>`** [ИСТОЧНИК 5]:
     - Реализует агентов с графовой стратегией (`AIAgentGraphStrategy`).
     - Явно exposes метод `execute(prompt: String)` [ИСТОЧНИК 5].
     - Имеет `inputType`/`outputType` для сериализации.
     - Пример: агент, управляющий сложными workflow в виде графа состояний.

### 3. **Классы поддержки возможностей**
   - **`AgentLifecycleHandlersCollector`** [ИСТОЧНИК 1]:
     - Собирает и управляет обработчиками событий жизненного цикла агента (`AgentLifecycleEventType`).
     - Позволяет добавлять (`addHandlerForFeature`) и получать (`getHandlersForEvent`) обработчики для конкретных features.
     - *Не является агентом напрямую, но обеспечивает расширяемость через события.*

   - **`RegisteredFeature`** [ИСТОЧНИК 2]:
     - Представляет установленную feature (реализация + конфигурация).
     - Используется pipeline для хранения зарегистрированных компонентов.

### 4. **Механизм установки features**
   Все агенты предоставляют `FeatureContext` (вложенный класс), позволяющий через `install(feature, configure)` добавлять кастомные расширения [ИСТОЧНИК 3, 4]. Features затем регистрируются в pipeline (например, `AIAgentFunctionalPipeline`), который координирует их жизненный цикл.

### Итоговая архитектура
1. **Базовый контракт** (`AIAgentBase`) определяет общий интерфейс.
2. **Стратегии** (`Functional`/`Planner`/`Graph`) специализируют поведение выполнения.
3. **Pipeline** (например, `AIAgentFunctionalPipeline`) управляет установленными features и их конфигурациями.
4. **Обработчики событий** (`AgentLifecycleHandlersCollector`) обеспечивают реакцию на этапы жизненного цикла.
5. **Реестр инструментов** (`ToolRegistry`) и **конфигурация** (`AIAgentConfig`) инкапсулируют внешние зависимости и параметры.

Таким образом, основные возможности агента распределены между:
- **Базовым классом** (общая структура).
- **Конкретными агентами** (специализированные стратегии).
- **Вспомогательными классами** (handlers, features, pipeline).

### Источники
[ИСТОЧНИК 1] AgentLifecycleHandlersCollector.kt · AgentLifecycleHandlersCollector · line 1  
[ИСТОЧНИК 2] RegisteredFeature.kt · RegisteredFeature · line 1  
[ИСТОЧНИК 3] FunctionalAIAgent.kt · FunctionalAIAgent · line 31  
[ИСТОЧНИК 4] PlannerAIAgent.kt · PlannerAIAgent · line 71  
[ИСТОЧНИК 5] GraphAIAgent.kt · GraphAIAgent · line 32  

*Примечание: класс `AIAgentBase` не представлен в контексте, но косвенно упоминается через наследование. Полная иерархия может включать дополнительные абстракции (например, `AIAgent`), не вошедшие в предоставленные данные.*

## Q2: "как конфигурируется агент"
Агент конфигурируется через систему **builders** и **модульных фич (features)**, которые позволяют детально настраивать его компоненты и поведение. Основные аспекты конфигурации:

### 1. **Использование Builders для базовой конфигурации**
Для создания агента используются специализированные builders, такие как `PlannerAgentBuilder` [ИСТОЧНИК 2], которые предоставляют методы для установки ключевых компонентов:
- **Стратегия (`strategy`)**: определяет алгоритм обработки ввода (например, `AIAgentPlannerStrategy` или `AIAgentFunctionalStrategy`). Устанавливается при создании builder'а.
- **Конфигурация (`agentConfig`)**: объект `AIAgentConfig`, содержащий параметры LLM (модель, промпт, обработчик ответов, сериализатор) [ИСТОЧНИК 1][ИСТОЧНИК 3].
- **PromptExecutor**: исполнитель промптов, ответственный за взаимодействие с языковой моделью. Устанавливается через метод `promptExecutor()` [ИСТОЧНИК 2].
- **ToolRegistry**: реестр инструментов, доступных агенту. По умолчанию `ToolRegistry.EMPTY`, настраивается через `withToolRegistry()` [ИСТОЧНИК 2].
- **Идентификатор (`id`)**: опциональный строковый идентификатор агента [ИСТОЧНИК 2].
- **Часы (`clock`)**: экземпляр `Clock` для временных операций, по умолчанию `Clock.System` [ИСТОЧНИК 2].
- **Фичи (`featureInstallers`)**: список установщиков фич, которые расширяют функциональность агента [ИСТОЧНИК 2].

Пример настройки builder'а (на основе [ИСТОЧНИК 2]):
```kotlin
val builder = PlannerAgentBuilder(
    strategy = myStrategy,
    config = myAgentConfig
).apply {
    promptExecutor(myPromptExecutor)
    withToolRegistry(myToolRegistry)
    id = "my-agent"
    // Фичи устанавливаются позже через install()
}
```

### 2. **Установка и конфигурация фич (Features)**
Фичи — модульные расширения, которые добавляют агентам новые возможности (логирование, отладка, обработка сообщений и т.д.) [ИСТОЧНИК 4]. Они устанавливаются в **блоке инициализации** builder'а или напрямую в агенте через вложенный класс `FeatureContext` [ИСТОЧНИК 1][ИСТОЧНИК 3]:
- Метод `install(feature, configure)` принимает реализацию фичи (например, `AIAgentFunctionalFeature` или `AIAgentPlannerFeature`) и опциональную лямбду `configure` для настройки её конфигурации (`FeatureConfig`) [ИСТОЧНИК 1][ИСТОЧНИК 3].
- Конфигурация фич выполняется в лямбде, где можно изменять свойства конфигурационного объекта [ИСТОЧНИК 4].

Пример из [ИСТОЧНИК 4]:
```kotlin
val agent = AIAgent(
    localEngine = localEngine,
    toolRegistry = toolRegistry,
    strategy = strategy,
    agentConfig = agentConfig
) {
    // Установка фичи с настройкой
    install(MyFeature) {
        someProperty = "value"
    }
    // Установка другой фичи
    install(AnotherFeature) {
        anotherProperty = 42
    }
}
```

Внутренне, при построении агента, фичи регистрируются как `RegisteredFeature` (связка реализации и конфигурации) [ИСТОЧНИК 5] и интегрируются в пайплайн агента.

### 3. **Сборка агента**
После настройки всех компонентов и фич, вызывается метод `build()` (или аналогичный) для создания экземпляра агента. В зависимости от типа агента (функциональный или планирующий), инициализация включает:
- Создание `GenericAgentEnvironment` с реестром инструментов, сериализатором и логгером [ИСТОЧНИК 1][ИСТОЧНИК 3].
- Подготовка `AIAgentLLMContext` с параметрами LLM (модель, промпт, инструменты) [ИСТОЧНИК 1][ИСТОЧНИК 3].
- Инициализацию менеджера состояния и хранилища.
- Автоматический вызов `installFeatures()` из `FeatureContext` для применения всех зарегистрированных фич [ИСТОЧНИК 1][ИСТОЧНИК 3].

### 4. **Важные замечания**
- Конфигурация **обязательно** требует передачи `strategy` и `agentConfig` [ИСТОЧНИК 2].
- Фичи могут добавлять **message processors** (например, для логирования) или **pipeline interceptors** для модификации поведения агента на разных этапах [ИСТОЧНИК 4].
- В контексте представлены два типа агентов: `FunctionalAIAgent` (функциональный, без планирования) и `PlannerAIAgent` (с планированием), поэтому используемые фичи и стратегии различаются [ИСТОЧНИК 1][ИСТОЧНИК 3].

### Пример полной конфигурации (гипотетический, на основе контекста)
```kotlin
val agent = AIAgent.builder(PlannerAgentBuilder) { // или аналогично
    strategy = myPlannerStrategy
    agentConfig = AIAgentConfig(
        prompt = "You are a helpful assistant",
        model = myLLModel,
        responseProcessor = myResponseProcessor,
        serializer = mySerializer
    )
    promptExecutor(myPromptExecutor)
    withToolRegistry(myToolRegistry)
    id = "advanced-agent"
    install(DebugFeature) {
        logLevel = LogLevel.DEBUG
    }
    install(TraceFeature) {
        enabled = true
    }
}.build()
```

Таким образом, конфигурация агента — это многоступенчатый процесс, сочетающий **билдеры для базовых компонентов** и **систему фич для расширения функциональности**, что обеспечивает гибкость и модульность.

### Источники
[ИСТОЧНИК 1] FunctionalAIAgent.kt · FunctionalAIAgent · line 63 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/FunctionalAIAgent.kt  
[ИСТОЧНИК 2] AIAgentBuilder.kt · PlannerAgentBuilder · line 478 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentBuilder.kt  
[ИСТОЧНИК 3] PlannerAIAgent.kt · PlannerAIAgent · line 71 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/planner/PlannerAIAgent.kt  
[ИСТОЧНИК 4] FEATURES.md · to · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/FEATURES.md  
[ИСТОЧНИК 5] RegisteredFeature.kt · RegisteredFeature · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/pipeline/RegisteredFeature.kt

## Q3: "как агент работает с историей сообщений"
На основе предоставленного контекста, работа агента с историей сообщений организована на нескольких уровнях абстракции, от низкоуровневых манипуляций до стратегий сжатия и персистентного хранения.

### 1. Управление историей в рамках сессии (Write Session)
На самом низком уровне агент работает с историей через **`AIAgentLLMWriteSession`**, которая представляет сессию взаимодействия с языковой моделью (LLM). История сообщений хранится в поле `prompt` этого объекта. Для её управления предоставляется набор расширений (extension functions) в классе `AIAgentLLMActions` [ИСТОЧНИК 1]:
- **`clearHistory()`** — полностью сбрасывает историю, устанавливая пустой список сообщений.
- **`leaveLastNMessages(n, preserveSystemMessages)`** — сохраняет только `n` последних сообщений, опционально сохраняя системные сообщения.
- **`dropLastNMessages(n, preserveSystemMessages)`** — удаляет последние `n` сообщений, также с опциональным сохранением системных.
- **`leaveMessagesFromTimestamp(timestamp, preserveSystemMessages)`** — удаляет все сообщения, которые были созданы ранее указанного временного штампа `Instant`.

Все эти методы работают путем пересоздания списка сообщений в `prompt` через `prompt.withMessages { ... }`.

### 2. Сжатие истории (Compression Strategies)
Для управления размером контекста (context window) и соблюдения ограничений LLM существует система стратегий сжатия в классе **`HistoryCompressionStrategy`** [ИСТОЧНИК 3]. Это абстрактный класс с основным методом `compress(llmSession, memoryMessages)`.

Предоставляются готовые реализации стратегий [ИСТОЧНИК 4]:
- **`WholeHistory`** (`WholeHistoryCompressionStrategy`): Сохраняет все системные сообщения, первый пользовательский запрос (если есть) и сообщения из памяти (memory messages), а всё остальное сжимается в одним "TL;DR" (too long; didn't read) резюме. Резюме генерируется вызовом LLM через `compressPromptIntoTLDR`.
- **`WholeHistoryMultipleSystemMessages`** (`WholeCompressionStrategyWithMultipleSystemMessages`): Обрабатывает историю с несколькими системными сообщениями. Разбивает историю на блоки по границам системных сообщений, для каждого блока (кроме первого, где сохраняются memory messages) генерируется свой TL;DR, затем собирает их в хронологическом порядке.
- **`FromLastNMessages`**: Сохраняет только последние `n` сообщений.
- **`FromTimestamp`**: Сохраняет только сообщения, начиная с указанного временного штампа.
- **`Chunked`**: Сжимает историю по частям (чанкам).

Ключевой механизм: стратегия сжимает исходную историю (`memoryMessages`) в список сообщений (часто один — TL;DR) и **обновляет prompt в переданном `llmSession`**.

### 3. Персистентность и сквозная история (Chat History Memory)
Для сохранения истории между разными сессиями агента существует **фича `ChatMemory`** [ИСТОЧНИК 2]. Это компонент, который можно "установить" в агента (`installChatMemory`).

Её ключевые возможности:
- **Хранение истории:** Использует `chatHistoryProvider` для загрузки (load) и сохранения (save) истории диалога между агентом и пользователем. Конкретная реализация провайдера (например, база данных, файл) остается за пользователем.
- **Ограничение размера (окно):** Поддерживает `windowSize`, чтобы хранить только ограниченное количество последних сообщений, например, `windowSize(20)`.
- **Предобработка:** Через `applyPreProcessors` можно модифицировать загруженные сообщения перед их отправкой в LLM.

Таким образом, `ChatMemory` отвечает за долгосрочное хранение, а `HistoryCompressionStrategy` — за адаптацию загруженной истории под ограничения текущего вызова LLM. Обычно workflow таков: загрузить историю через `ChatMemory`, затем, если она слишком большая, применить `HistoryCompressionStrategy` для её сжатия перед вызовом модели.

### 4. Трейсинг и аудит (Tracing)
Фича **`Tracing`** [ИСТОХОДНИК 5] не управляет историей напрямую, но **собирает полные данные о всех сообщениях** (LLM calls, prompts, tool calls) во время выполнения агента. Это данные для анализа, отладки и оценки. Она использует `FeatureMessage` для передачи событий в настроенные процессоры (например, логгер или файловыйwriter). Таким образом, трейсинг предоставляет полную неизмененную историю всех взаимодействий для post-mortem анализа.

### Сводная схема взаимодействия
1. **Начало сессии:** `ChatMemory` загружает историю предыдущих диалогов (если configured) и предоставляет её как `memoryMessages`.
2. **Подготовка промпта:** Перед вызовом LLM, если история превышает лимиты:
    - Применяется выбранная `HistoryCompressionStrategy.compress()`.
    - Она модифицирует текущий `AIAgentLLMWriteSession` (обновляет `prompt`).
3. **Вызов LLM:** Агент отправляет промпт (уже возможно сжатый) в модель.
4. **Пост-обработка:** Новые сообщения ((user, assistant, tool results)) добавляются в историю сессии. `ChatMemory` может сохранить их для будущих сессий.
5. **Трейсинг:** На каждом этапе `Tracing` записывает сырые сообщения и события в лог/трейс.

**Важно:** Все манипуляции с историей (очистка, удаление, сжатие) происходят **в рамках текущей сессии** (`AIAgentLLMWriteSession`). Персистентность обеспечивается отдельной фичей (`ChatMemory`), а полное аудирование — `Tracing`.

### Источники
[ИСТОЧНИК 1] AIAgentLLMActions.kt · clearHistory/leaveLastNMessages/dropLastNMessages/leaveMessagesFromTimestamp · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/extension/AIAgentLLMActions.kt
[ИСТОЧНИК 2] ChatMemory.kt · ChatMemory · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/chatMemory/feature/ChatMemory.kt
[ИСТОЧНИК 3] HistoryCompressionStrategy.kt · HistoryCompressionStrategy · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/extension/HistoryCompressionStrategy.kt
[ИСТОЧНИК 4] HistoryCompressionStrategy.kt · WholeHistory/WholeHistoryMultipleSystemMessages · line 147 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/extension/HistoryCompressionStrategy.kt
[ИСТОЧНИК 5] Tracing.kt · Tracing · line 25 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-trace/src/commonMain/kotlin/ai/koog/agents/features/tracing/feature/Tracing.kt

## Q4: "как реализованы стратегии компактизации контекста"
На основании предоставленного контекста, информация о реализации стратегий компактизации контекста отсутствует. Представленные классы касаются управления жизненным циклом стратегий агентов (`StrategyEventContext` [ИСТОЧНИК 1]), дополнения промптов контекстом из памяти (`PromptAugmenter` [ИСТОЧНИК 2]), а также устаревших типов контекстов и функциональности A2A-сервера. Ни в одном из файлов не описаны механизмы уменьшения, сжатия или оптимизации размера контекста (компактизации), такие как суммирование, фильтрация или другие стратегии управления объёмом данных, передаваемых в LLM.

Для получения полного ответа требуется дополнительный контекст, включающий классы, интерфейсы или модули, непосредственно ответственные за компактизацию контекста (например, связанные с long-term memory, векторными базами данных или обработкой промптов).

Уточните: какие конкретные компоненты кодовой базы (например, пакеты `longtermmemory`, `retrieval` или другие) реализуют стратегии компактизации контекста, и имеются ли там отдельные классы/интерфейсы для этой цели?

## Q5: "как реализован tool calling"
Отлично, разберём подробно реализацию **tool calling** (вызова инструментов) на основе предоставленного контекста. Система использует **событийно-ориентированный подход** для отслеживания всего жизненного цикла вызова инструмента (tool call).

### Основные события (Events) жизненного цикла вызова инструмента

Вся логика отслеживания вызовов инструментов построена вокруг трёх основных событий, которые являются данными классами (`data class`), помеченными аннотацией `@Serializable` [ИСТОЧНИК 1, 2, 3]:

1.  **`ToolCallStartingEvent`**: Событие,triggered, когда инструмент начинает выполняться.
    *   Ключевые данные:
        *   `runId` — уникальный идентификатор сессии вызова инструмента.
        *   `toolCallId` — уникальный идентификатор конкретного вызова.
        *   `toolName` — имя вызываемого инструмента.
        *   `toolArgs` — объект `JSONObject` с аргументами, переданными в инструмент.
        *   `executionInfo` — информация о контексте выполнения (родительские события, путь выполнения) [ИСТОЧНИК 1].
    *   Имеет два конструктора: современный с параметром `executionInfo` и устаревший (`@Deprecated`) без него [ИСТОЧНИК 1].

2.  **`ToolCallCompletedEvent`**: Событие,triggered при успешном завершении вызова инструмента.
    *   Помимо общей информации (как в `StartingEvent`), содержит:
        *   `toolDescription` — описание инструмента (может быть `null`).
        *   `result` — результат выполнения инструмента в виде сериализуемого элемента `JSONElement?` [ИСТОЧНИК 3].

3.  **`ToolCallFailedEvent`**: Событие,triggered, если вызов инструмента завершился с ошибкой.
    *   Помимо общей информации, содержит:
        *   `toolDescription` — описание инструмента.
        *   `error` — объект ошибки типа `AIAgentError?`, инкапсулирующий детали сбоя [ИСТОЧНИК 2].

**Важный момент по устареванию:** Для всех трёх событий существуют устаревшие конструкторы, не принимающие параметр `executionInfo`. Современная реализация требует передачи `AgentExecutionInfo` для построения иерархии и трассировки выполнения, что критически важно для отладки сложных, многошаговых цепочек вызовов [ИСТОЧНИК 1, 2, 3].

### Результаты выполнения инструментов (Tool Results)

Интерфейс `ToolResult` [ИСТОЧНИК 4] является **устаревшим (deprecated)**. Комментарий прямо указывает, что расширение этого интерфейса больше не требуется.

> "Extending ToolResult is no longer required. Tool results are entirely handled by KotlinX Serialization." [ИСТОЧНИК 4]

Это означает, что система полностью перешла на **декларативный подход**: результат работы инструмента — это любой объект, который может быть сериализован/десериализован библиотекой `kotlinx.serialization`. В событии `ToolCallCompletedEvent` поле `result` имеет тип `JSONElement?` (из пакета `ai.koog.serialization`), что и является универсальным представлением любого сериализуемого значения [ИСТОЧНИК 3].

### Контекст обработки событий (Event Handling Context)

Для обработки событий вызова инструментов существует специализированный интерфейс `ToolCallEventContext` [ИСТОЧНИК 5], который расширяет более общий `AgentLifecycleEventContext`. Этот контекст предоставляет обработчику событий (handler) весь необходимый контекст:

*   **Идентификаторы**: `runId`, `toolCallId`.
*   **Метаданные вызова**: `toolName`, `toolDescription`, `toolArgs` (аргументы в виде `JSONObject`).
*   **Контекст агента**: `context` — ссылка на текущий контекст выполнения агента (`AIAgentContext`), через который можно получить доступ к состоянию, конфигурации и другим ресурсам [ИСТОЧНИК 5].

Существует конкретная реализация этого контекста — `ToolCallStartingContext` — которая используется для события старта вызова и также содержит `executionInfo` [ИСТОЧНИК 5].

### Упрощённая схема потока (workflow)

1.  **Подготовка вызова**: Когда агенту необходимо использовать инструмент, система формирует `ToolCallStartingEvent`, предоставляя все необходимые параметры (`runId`, `toolName`, `toolArgs` и т.д.). Это событие публикуется в системе [ИСТОЧНИК 1].
2.  **Обработка**: Обработчики (handlers), подписанные на событие типа `ToolCallStarting`, получают контекст `ToolCallStartingContext` [ИСТОЧНИК 5]. В этом контексте они могут:
    *   Проанализировать аргументы (`toolArgs`).
    *   Получить доступ к зависимостям через `context`.
    *   Реализовать логику *перед* фактическим выполнением инструмента (например, валидацию, логирование).
3.  **Выполнение**: Логика самого инструмента выполняется (этот код, скорее всего, находится вне предоставленных файлов, в модуле `agents-tools`). Результат или ошибка сериализуются в `JSONElement`.
4.  **Публикация результата**:
    *   При успехе публикуется `ToolCallCompletedEvent` с полем `result`.
    *   При ошибке публикуется `ToolCallFailedEvent` с полем `error` [ИСТОЧНИК 2, 3].
5.  **Пост-обработка**: Другие обработчики могут реагировать на завершающие события, например, для агрегации результатов или обновления состояния агента.

### Вывод

Реализация **tool calling** в этой кодовой базе — это **событийная система** с чёткими контрактами:
*   События (`*Event`) описывают *что произошло* и содержат все данные о вызове (кто, что, с какими аргументами, результат/ошибка).
*   Контексты (`*Context`) предоставляют *инструменты* для обработки этих событий (доступ к состоянию агента, идентификаторам).
*   Архитектура использует **дерево выполнения** (`executionInfo`) для трассировки сложных сценариев.
*   Результаты инструментов — **просто сериализуемые данные** (`JSONElement`), без привязки к конкретным Java/Kotlin классам, что обеспечивает гибкость и слабую связанность.

**Недостаток контекста:** В предоставленных файлах отсутствует код, который *фактически вызывает* инструмент (исполнитель) и код, который *диспетчеризирует* эти события к обработчикам (`ToolHandler` или аналогичные). Также не ясно, как именно `AgentExecutionInfo` заполняется и используется в*graph* выполнения агента.

---
### Источники
[ИСТОЧНИК 1] toolExecutionEvents.kt · ToolCallStartingEvent · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/model/events/toolExecutionEvents.kt
[ИСТОЧНИК 2] toolExecutionEvents.kt · ToolCallFailedEvent · line 113 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/model/events/toolExecutionEvents.kt
[ИСТОЧНИК 3] toolExecutionEvents.kt · ToolCallCompletedEvent · line 193 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/model/events/toolExecutionEvents.kt
[ИСТОЧНИК 4] ToolResult.kt · implemented · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-tools/src/commonMain/kotlin/ai/koog/agents/core/tools/ToolResult.kt
[ИСТОЧНИК 5] ToolCallEventContext.kt · ToolCallEventContext · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/handler/tool/ToolCallEventContext.kt

## Q6: "как реализован механизм user in the loop"
На основе предоставленного контекста **нельзя однозначно определить реализацию механизма "user in the loop"**.

Предоставленные файлы описывают базовые компоненты ядра агента (`AIAgentFunctionalStrategy`, `AIAgentFunctionalContextBase`), интерфейс взаимодействия со средой (`AIAgentEnvironment`), вспомогательные утилиты (`RollbackToolRegistry`) и конфигурацию трассировки (`TraceFeatureConfig`). Однако ни в одном из них открыто не обсуждается паттерн или компонент, который бы:

1.  Явно приостанавливал выполнение агента для запроса ввода/решения от пользователя.
2.  Хранил состояние, ожидающее подтверждения (например, "pending approval").
3.  Имел интерфейс для отправки уведомлений или запросов пользователю.
4.  Обрабатывал ответ пользователя для возобновления или изменения цикла агента.

Возможные косвенные связи, которые требуют проверки:
*   **Класс `RollbackToolRegistry`** [ИСТОЧНИК 3] может использоваться в сценариях, где действия agents отменяются (или требуют подтверждения) пользователем, но его основная ответственность — управление соответствием инструментов и их откатывающих версий, а не прямой запрос к пользователю.
*   **Конфигурация `TraceFeatureConfig`** [ИСТОЧНИК 5] может логировать события, связанные с взаимодействием с пользователем, но не реализует само взаимодействие.
*   Интерфейс `AIAgentEnvironment` [ИСТОЧНИК 2] предоставляет абстракцию для внешнего мира, включая инструменты (`executeTool`). Логика "user in the loop" могла бы быть реализована как одна из конкретных реализаций этого интерфейса (например, `HumanInTheLoopEnvironment`), но такой класс в предоставленном контексте отсутствует.
*   `AIAgentFunctionalContextBase` [ИСТОЧНИК 4] предоставляет доступ к окружению (`environment()`), что является точкой входа для любого взаимодействия, но сам по себе не определяет логику запроса к пользователю.

**Вывод:** В предоставленном наборе файлов нет явного компонента, реализующего паттерн "user in the loop". Эта функциональность, скорее всего, реализуется в виде **конкретной стратегии агента (`AIAgentFunctionalStrategy`)** или **конкретной реализации `AIAgentEnvironment`**, которые не вошли в данный отбор.

### Источники
[ИСТОЧНИК 1] AIAgentFunctionalStrategy.kt · allows · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentFunctionalStrategy.kt
[ИСТОЧНИК 2] AIAgentEnvironment.kt · with · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/environment/AIAgentEnvironment.kt
[ИСТОЧНИК 3] RollbackToolRegistry.kt · allows · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-snapshot/src/commonMain/kotlin/ai/koog/agents/snapshot/feature/RollbackToolRegistry.kt
[ИСТОЧНИК 4] AIAgentFunctionalContextBase.kt · allowing · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentFunctionalContextBase.kt
[ИСТОЧНИК 5] TraceFeatureConfig.kt · allows · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-trace/src/commonMain/kotlin/ai/koog/agents/features/tracing/feature/TraceFeatureConfig.kt

---
**Уточняющий вопрос:** Можете ли вы указать конкретный класс, интерфейс или ключевое слово (например, `HumanApproval`, `UserInput`, `ConsoleEnvironment`, `ApprovalTool`), которое использует система для реализации запроса к пользователю? Или, возможно, эта логика встроена в конкретную реализацию `AIAgentStrategy` или `AIAgentEnvironment`, которая не попала в текущий отбор?

## Q7: "как агенты работают с памятью и  сколько слоев используют в"
На основе предоставленного контекста, система памяти агентов в Koog организована вокруг концепций structured knowledge storage с использованием контекстно-зависимой организации. Вот как это работает:

### Архитектура системы памяти
1. **Основной интерфейс**: `AgentMemoryProvider` [ИСТОЧНИК 2] определяет ядро системы, предоставляя операции `save` и `load` для работы с единицами знания (`Fact`) в рамках заданных `MemorySubject` и `MemoryScope`.

2. **Организация знаний**:
   - **Concept** [ИСТОЧНИК 4] представляет отдельный тип знания (например, "build-system") с уникальным `keyword` и описанием. Каждый концепция имеет `FactType` (SINGLE или MULTIPLE), определяющий, может ли храниться одна или множественные значения.
   - **Fact** содержит конкретное значение знания с временной меткой, привязанное к концепции.

3. **Контекстная изоляция (Scopes)**: Система использует четырёхуровневую иерархию `MemoryScope` [ИСТОЧНИК 5] для управления видимостью памяти:
   - `Agent(name)` — память изолирована для конкретного агента.
   - `Feature(id)` — разделяемая память в пределах одной функциональности.
   - `Product(name)` — общая память в рамках продукта.
   - `CrossProduct` — глобальная память, доступная во всех продуктах.

4. **Тематическая категоризация**: `MemorySubject` (например, `PROJECT`, `MACHINE`) добавляет дополнительный контекст для группировки фактов.

### Физическое хранение
`LocalFileMemoryProvider` [ИСТОЧНИК 1] реализует персистентное хранение в иерархической файловой структуре, соответствующей областям видимости:
```
root/storageDirectory/
  agent/[agent-name]/subject/[subject-name]/facts.json
  feature/[feature-id]/subject/[subject-name]/facts.json
  product/[product-name]/subject/[subject-name]/facts.json
  organization/subject/[subject-name]/facts.json
```
Каждый файл `facts.json` содержит сериализованные факты в формате JSON. Операции являются атомарными и потокобезопасными благодаря использованию `Mutex`.

### Ответ на вопрос о "слоях"
В контексте нет явного упоминания термина "слои" (layers) в архитектурном смысле. Однако система организована в виде нескольких уровней абстракции:
1. **Семантический уровень**: Concepts и Facts определяют структуру знаний.
2. **Контекстный уровень**: MemoryScope и MemorySubject обеспечивают изоляцию и группировку.
3. **Физический уровень**: LocalFileMemoryProvider управляет хранением в файловой системе.

Если под "слоями" подразумеваются области видимости памяти, то система использует **четыре типа `MemoryScope`** [ИСТОЧНИК 5], которые можно рассматривать как уровни общности данных (от изолированных до глобальных).

### Пример использования
```kotlin
provider.save(
    fact = SingleFact(
        concept = Concept("build-system", "Project build configuration", FactType.SINGLE),
        timestamp = System.currentTimeMillis(),
        value = "Gradle 8.0"
    ),
    subject = MemorySubject.Project,
    scope = MemoryScope.Product("my-app")
)
```

### Ограничения контекста
Контекст не содержит информации о:
- Взаимодействии агентов с памятью в реальном времени (например, механизмы кэширования или индексирования).
- Других реализациях `AgentMemoryProvider` помимо файловой (например, сетевые или in-memory варианты).
- Подробностях о процессе семантического поиска, упомянутого в интерфейсе [ИСТОЧНИК 2].

Для более полного ответа потребуется дополнительный контекст об архитектуре агентов и других компонентах системы.

### Источники
[ИСТОЧНИК 1] LocalFileMemoryProvider.kt · LocalFileMemoryProvider · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/memory/providers/LocalFileMemoryProvider.kt  
[ИСТОЧНИК 2] AgentMemoryProvider.kt · for · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/memory/providers/AgentMemoryProvider.kt  
[ИСТОЧНИК 4] Concept.kt · FactType · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/memory/model/Concept.kt  
[ИСТОЧНИК 5] Concept.kt · FactType · line 207 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/memory/model/Concept.kt

## Q8: "как агенты работают с mcp"
Не знаю. В предоставленном контексте отсутствует информация о MCP (Model Context Protocol) или любом другом компоненте/технологии с таким обозначением. Уточните, пожалуйста, что вы имеете в виду под 'mcp'? Например, это может быть Model Context Protocol, внутренний компонент фреймворка Koog или что-то иное?

### Источники
(нет соответствующих источников в контексте)

## Q9: "какие возможности есть по выстраиванию пайпланов и разбитие задач на подзадачи"
На основе предоставленного контекста можно выделить следующие возможности по выстраиванию пайплайнов и разбиению задач на подзадачи в фреймворке AI Koog Agents:

### 1. Создание подграфов с задачами (Subgraphs with Tasks)
Основной механизм реализован через утилиту `SubgraphWithTaskUtils` в файле `AIAgentSubgraphExt.kt`. Она предоставляет функцию builder `subgraphWithTask` [ИСТОЧНИК 2], которая позволяет:
- Определить изолированный подграф (subgraph) для выполнения конкретной подзадачи.
- Задать набор инструментов (`tools`), доступных в рамках этой подзадачи.
- Настроить параметры LLM (`llmModel`, `llmParams`).
- Указать режим выполнения инструментов (`runMode`, например, `ToolCalls.SEQUENTIAL`).
- Задать логику задачи через `defineTask` — suspend-функцию, которая получает входные данные и возвращает описание задачи для LLM.

### 2. Стратегии управления подграфами
Класс `AIAgentGraphStrategy` [ИСТОЧНИК 4] управляет выполнением рабочих процессов (workflows), построенных как взаимосвязанные узлы (interconnected nodes):
- Имеет стартовый узел (`nodeStart`) и финальный узел (`nodeFinish`).
- Использует `toolSelectionStrategy` для определения набора инструментов во время выполнения подграфа.

### 3. Динамический выбор инструментов
Интерфейс `ToolSelectionStrategy` в `AIAgentSubgraph.kt` [ИСТОЧНИК 5] предоставляет гибкие варианты:
- `ALL` — использовать все доступные инструменты.
- `NONE` — запретить использование инструментов.
- Динамический выбор (например, `Tools`), когда подмножество инструментов определяется на основе описания подзадачи (subtask description) через LLM. Это позволяет оптимизировать инструментарий под конкретную подзадачу.

### 4. Финализация результатов подзадач
Внутренний инструмент `FINALIZE_SUBGRAPH_TOOL_NAME` [ИСТИННИК 1] используется для завершения подзадачи и возврата её результата. Это часть структурированного подхода к обработке результатов подграфов.

### 5. Структурированное описание вызовов инструментов
Интерфейс `ToolCallDescriber` [ИСТОЧНИК 3] позволяет преобразовывать вызовы инструментов и их результаты в структурированные сообщения (например, в JSON-формат). Это важно для передачи данных между подзадачами в пайплайне.

### Ключевые особенности:
- **Модульность**: Каждая подзадача может иметь свой набор инструментов, LLM и параметры.
- **Контролируемое выполнение**: Возможность ограничить количество попыток LLM (`assistantResponseRepeatMax`).
- **Обработка результатов**: Через `ResponseProcessor` можно настроить пост-обработку сообщений от LLM.
- **Внутренний API**: Многие возможности помечены `@InternalAgentToolsApi` [ИСТОЧНИК 1], что указывает на их экспериментальный статус и возможные изменения.

### Ограничения контекста:
В предоставленных фрагментах нет деталей по:
- Конкретным реализациям `LLModel`, `ToolCalls`.
- Примеров связывания нескольких подграфов в единый пайплайн.
- Механизмах обработки ошибок и повторных попыток на уровне пайплайна.

Для полного понимания пайплайнов рекомендуется изучить дополнительные классы контекста выполнения (`AIAgentContext`) и например, `AgentExecutor`.

### Источники
[ИСТОЧНИК 1] AIAgentSubgraphExt.kt · providing · line 31 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/AIAgentSubgraphExt.kt  
[ИСТОЧНИК 2] AIAgentSubgraphExt.kt · providing · line 237 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/AIAgentSubgraphExt.kt  
[ИСТОЧНИК 3] ToolCallDescriber.kt · ToolCallDescriber · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/config/ToolCallDescriber.kt  
[ИСТОЧНИК 4] AIAgentGraphStrategy.kt · AIAgentGraphStrategy · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/AIAgentGraphStrategy.kt  
[ИСТОЧНИК 5] AIAgentSubgraph.kt · for · line 383 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/AIAgentSubgraph.kt

## Q10: "как агенты могу общаться друг с другом"
На основе предоставленного контекста можно сказать, что агенты могут общаться друг с другом через **A2AClient (Agent-to-Agent Client)**, который предоставляется функцией **A2AAgentClient**. [ИСТОЧНИК 2]

Когда агент устанавливает (installs) функциональность `A2AAgentClient` как часть своих особенностей (features), он получает доступ к зарегистрированным A2A-клиентам в своей стратегии (strategy) или контексте. [ИСТОЧНИК 2] Это позволяет агенту:

1.  **Отправлять сообщения** удаленным A2A-агентам.
2.  **Получать карточки агентов (agent cards)** и их capabilities.
3.  **Управлять задачами (tasks)** на удаленных агентах.
4.  **Подписываться на события задач (task events)** и стримовые ответы (streaming responses).
5.  **Настраивать push-уведомления.**

Указано, что `A2AAgentClient` предоставляет также **узлы (convenience nodes) для общих операций A2A-клиента**, таких как отправка сообщений, получение задач и управление подписками. [ИСТОЧНИК 2] Однако конкретные реализации этих узлов или детали протокола общения (например, формат сообщений) в предоставленных фрагментах кода не раскрываются.

**Важно:** Контекст является частичным. Он описывает инфраструктурный компонент для общения, но не показывает конкретный пример использования (как именно агент в своей стратегии вызывает эти методы), детали сериализации сообщений или механизмов стриминга. Представлены только каркасные классы.

### Источники
[ИСТОЧНИК 1] ExitTool.kt · representing · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-ext/src/commonMain/kotlin/ai/koog/agents/ext/tool/ExitTool.kt
[ИСТОЧНИК 2] A2AAgentClient.kt · A2AAgentClient · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-a2a-client/src/commonMain/kotlin/ai/koog/agents/a2a/client/feature/A2AAgentClient.kt
[ИСТОЧНИК 3] GraphAIAgent.kt · GraphAIAgent · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/GraphAIAgent.kt
[ИСТОЧНИК 4] FunctionalAIAgent.kt · FunctionalAIAgent · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/FunctionalAIAgent.kt
[ИСТОЧНИК 5] PlannerAIAgent.kt · PlannerAIAgent · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/planner/PlannerAIAgent.kt

## Q11: "какие возможности по работе с сессиями"
На основе предоставленного контекста можно выделить следующие ключевые возможности по работе с сессиями (session) AI-агентов, работающих с LLM:

### 1. Основной интерфейс сессии для записи (Write Session API)

Интерфейс `AIAgentLLMWriteSessionAPI` [ИСТОЧНИК 1] определяет основные управляемые свойства активной сессии. Все свойства используют делегат `ActiveProperty`, что гарантирует, что доступ к ним возможен только в активном состоянии сессии.

*   **Управление промптом (`prompt`)**: Позволяет читать и изменять основной промпт сессии, включая список сообщений и параметры [ИСТОЧНИК 1].
*   **Управление инструментами (`tools`)**: Позволяет получать и изменять список доступных инструментов (дескрипторов) для текущей сессии [ИСТОЧНИК 1].
*   **Выбор модели (`model`)**: Позволяет динамически изменять или переопределять модель LLM, используемую в сессии [ИСТОЧНИК 1].
*   **Доступ к реестру инструментов (`toolRegistry`)**: Предоставляет доступ к реестру, где хранятся их фактические реализации (экземпляры `Tool`) [ИСТОЧНИК 1]. Этоnamented with `@InternalAgentsApi`, указывая на внутренний характер API.
*   **Доступ к часам (`clock`)**: Предоставляет экземпляр `Clock` для добавления временных меток к событиям, например, ответам LLM [ИСТОЧНИК 1].
*   **Процессор ответов (`responseProcessor`)**: Определяет логику пост-обработки (например, парсинга) сообщений, возвращаемых LLM [ИСТОЧНИК 4].

### 2. Расширения для манипуляции историей сообщений (DSL Extensions)

Файл `AIAgentLLMActions.kt` предоставляет convenient-функции-расширения для `AIAgentLLMWriteSession`, которые упрощают управление историей сообщений в промпте.

*   **Очистка истории (`clearHistory`)**: Полностью сбрасывает список сообщений в промпте на пустой список. Полезно для начала нового диалога [ИСТОЧНИК 2].
*   **Сохранение последних N сообщений (`leaveLastNMessages`)**: Удаляет все сообщения, кроме `n` самых последних. Опционально может сохранять системные сообщения (`Message.System`) независимо от правила [ИСТОЧНИК 2].
*   **Удаление последних N сообщений (`dropLastNMessages`)**: Удаляет `n` последних сообщений из истории. Также с опциональным сохранением системных сообщений [ИСТОЧНИК 2].
*   **Фильтрация по времени (`leaveMessagesFromTimestamp`)**: Удаляет все сообщения с временной меткой раньше указанного `Instant`. Также с опциональным сохранением системных сообщений [ИСТОЧНИК 3].

### 3. Управление параметрами инструментов (Tool Choice)

Также через расширения в `AIAgentLLMActions.kt` предоставляются функции для тонкой настройки параметра `ToolChoice` в `LLMParams`, который управляет поведением LLM относительно использования инструментов.

*   **Автоматический выбор (`setToolChoiceAuto`)**: Позволяет LLM саму решать, вызывать ли инструмент или генерировать текст [ИСТОЧНИК 3].
*   **Принудительный вызов инструментов (`setToolChoiceRequired`)**: Заставляет LLM обязательно использовать какой-либо инструмент [ИСТОЧНИК 3].
*   **Запрет вызова инструментов (`setToolChoiceNone`)**: Запрещает LLM вызывать какие-либо инструменты [ИСТОЧНИК 3].
*   **Вызов конкретного инструмента (`setToolChoiceNamed`)**: Принуждает LLM вызвать инструмент с указанным именем [ИСТОЧНИК 3].

### 4. Интерфейс сессии для чтения и запросов (Session API)

Интерфейс `AIAgentLLMSessionAPI` [ИСТОЧНИК 4] определяет основные операции по взаимодействию с LLM в рамках сессии.

*   **Запросы без инструментов**:
    *   `requestLLMWithoutTools()`: Отправляет промпт в LLM и возвращает один ответ (`Message.Response`). Если инструменты не используются, параметр `toolChoice` сбрасывается [ИСТОЧНИК 4].
    *   `requestLLMMultipleWithoutTools()`: Запрос, возвращающий список нескольких ответов (например, для sampling) [ИСТОЧНИК 4].
*   **Запросы с инструментами**:
    *   `requestLLMOnlyCallingTools()`: Запрос, который *требует* от LLM вызвать инструмент. Метод автоматически устанавливает `ToolChoice.Required` и возвращает первый ответ типа `Message.Tool.Call`. Кидает исключение, если LLM отказалась или задала вопрос [ИСТОЧНИК 4].
    *   `requestLLMForceOneTool(tool: ToolDescriptor)` / `requestLLMForceOneTool(tool: Tool<*, *>)`: Запрос, который *принуждает* LLM использовать *конкретный* инструмент [ИСТОЧНИК 5].
*   **Универсальный запрос (`requestLLM()`)**: Отправляет текущий промпт и возвращает первый ответ. Является, по сути, общей точкой входа [ИСТОЧНИК 5].
*   **Потоковый запрос (`requestLLMStreaming()`)`: Отправляет потоковый запрос к LLM и возвращает `Flow<StreamFrame>` для поступления токенов/чанков в реальном времени [ИСТОЧНИК 5].
*   **Модерация (`requestModeration()`)`: Отправляет запрос на модерацию контента к указанной или дефолтной модели [ИСТОЧНИК 4].

### Важное замечание о контексте
Предоставленный контекст является **частичным**. Он включает в себя только заголовки методов и их краткие описания/документацию, но **не содержит** их полных реализаций. Поэтому детали внутренней логики (например, как именно `ActiveProperty` проверяет активность сессии, или как `requestLLM` обрабатывает ошибки) изучить нельзя.

### Источники
[ИСТОЧНИК 1] AIAgentLLMWriteSessionAPI.kt · AIAgentLLMWriteSessionAPI · line 40 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/session/AIAgentLLMWriteSessionAPI.kt  
[ИСТОЧНИК 2] AIAgentLLMActions.kt · AIAgentLLMWriteSession · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/extension/AIAgentLLMActions.kt  
[ИСТОЧНИК 3] AIAgentLLMActions.kt · AIAgentLLMWriteSession · line 46 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/extension/AIAgentLLMActions.kt  
[ИСТОЧНИК 4] AIAgentLLMSessionAPI.kt · AIAgentLLMSessionAPI · line 46 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/session/AIAgentLLMSessionAPI.kt  
[ИСТОЧНИК 5] AIAgentLLMSessionAPI.kt · AIAgentLLMSessionAPI · line 115 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/session/AIAgentLLMSessionAPI.kt

## Q12: "как реализован state managment"
На основании предоставленного контекста, реализация управления состоянием (state management) в этой кодовой базе (фреймворк Koog Agents) затрагивает несколько ключевых компонентов, отвечающих за разные аспекты жизненного цикла агента, его контрольные точки и безопасность изменений состояния.

### 1. **Модель состояния жизненного цикла агента (`AIAgentState`)**
Это основная модель, описывающая возможные состояния агента в рамках его выполнения [ИСТОЧНИК 5]. Реализация использует sealed interface, что обеспечивает исчерпывающий и безопасный на уровне компиляции набор состояний:
*   **`NotStarted`**: Агент не был запущен.
*   **`Starting`**: Агент находится в процессе запуска.
*   **`Running`**: Агент активно выполняет свою задачу. Это состояние предоставляет доступ к `rootContext` — корневому контексту выполнения.
*   **`Success`**: Агент успешно завершил выполнение и имеет результат (тип `Output`).
*   **`Failure`**: Агент завершил выполнение с ошибкой (имеет исключение `Throwable`).

Каждое состояние предоставляет метод `copy()` для создания несвязанной копии объекта состояния, что важно для иммутабельности и безопасного распространения состояния.

### 2. **Базовый класс агента с управлением состоянием (`AIAgentBase`)**
Это абстрактный базовый класс для "одноразовых" (single-use) агентов, который инкапсулирует логику управления состоянием [ИСТОЧНИК 4].
*   **Идентификатор**: У каждого агента есть уникальный `id` (строка), который генерируется как случайный UUID, если не передан явно.
*   **Внутреннее состояние**: Класс хранит текущее состояние агента. Хотя в предоставленном фрагменте это не показано явно, из описания следует, что он поддерживает состояний running status, started flag, result и root context.
*   **Потокобезопасность**: Операции с состоянием защищены `mutex` (мьютексом), что гарантирует корректное изменение состояния в многопоточном окружении.
*   **Стратегия и конвейер**: Агент имеет `strategy` (конкретная стратегия выполнения) и `pipeline` (конвейер обработки), которые определяют его поведение.

### 3. **Сохранение и восстановление состояния (`AgentCheckpointData`)**
Этот класс представляет данные контрольной точки (checkpoint) — снапшота состояния агента в определённый момент его сессии [ИСТОЧНИК 1, 2, 3].
*   **Данные контрольной точки**: Включает:
    *   `checkpointId`: Уникальный идентификатор точки.
    *   `createdAt`: Метка времени создания.
    *   `nodePath`: Идентификатор узла (node) в графе выполнения агента, на котором была сделана точка.
    *   `lastOutput` (ранее `lastInput`): Сериализованный последний входной или выходной данные для узла. В новой версии (`since 0.6.1`) используется `lastOutput`, `lastInput` помечен как deprecated. Есть логическая проверка: `lastInput` и `lastOutput` не могут быть заданы одновременно, но хотя бы один должен быть задан (за исключением специального состояния "tombstone").
    *   `messageHistory`: История сообщений (обмен с пользователем, системой, инструментами) на момент создания точки.
    *   `properties`: Дополнительные произвольные данные состояния в виде JSONObject.
    *   `version`: Версия структуры данных контрольной точки.
*   **Томбстон (tombstone)**: Существует специальная функция `tombstoneCheckpoint` для создания "надгробия" — контрольной точки, которая не содержит реальных данных взаимодействий и служит маркером завершённой или невалидной сессии [ИСТОЧНИК 3]. Её `nodePath` равен константе `PersistenceUtils.TOMBSTONE_CHECKPOINT_NAME`.
*   **Иммутабельность и Сериализация**: Класс является `data class` (иммутабельный) и помечен аннотацией `@Serializable`, что облегчает его сохранение в различные хранилища (файлы, БД) и последующее восстановление.

### 4. **Взаимодействие и общий подход**
State management в фреймворке, судя по контексту, организован по следующим принципам:
1.  **Жизненный цикл**: Состояние агрегируется иtrackируется через `AIAgentState` [ИСТОЧНИК 5].
2.  **Исполнение**: Базовый класс `AIAgentBase` управляет этим состоянием в процессе работы, обеспечивая потокобезопасность [ИСТОЧНИК 4].
3.  **Снепшотинг и восстановление**: Для возможности отката (rollback) или сохранения прогресса используются `AgentCheckpointData`, которые делают полную или частичную копию состояния (история сообщений, данные последнего узла, свойства) в конкретный момент времени [ИСТОЧНИК 1, 2, 3]. Логика `RollbackStrategy` упоминается в импортах `AgentCheckpointData`, что указывает на наличие механизмов восстановления агента из контрольной точки.
4.  **Иммутабельность**: Основные модели состояния (`AIAgentState` реализации, `AgentCheckpointData`) являются иммутабельными. Изменение состояния, вероятно, происходит через создание новой копии объекта с обновлёнными данными.

**Недостающая информация в контексте:**
Контекст не показывает:
*   Класс `AIAgentContext` и `AIAgentRunSession`, которые, вероятно, содержат текущий изменяемый контекст выполнения и сессию.
*   Конкретную реализацию `RollbackStrategy` и как именно из `AgentCheckpointData` восстанавливается `AIAgentContext`.
*   Как и когда именно создаются `AgentCheckpointData` внутри работы агента (например, после каждого шага в графе).
*   Полную структуру `AIAgentBase` (тела методов, поля `state`, `strategy`, `pipeline`).

### Вывод
State management реализован как комбинация:
1.  **fabricted sealed-интерфейса** (`AIAgentState`) для моделирования строгого жизненного цикла агента.
2.  **Иммутабельных data-классов** (`AgentCheckpointData`) для создания неизменяемых снапшотов состояния, способных к сериализации.
3.  **Абстрактного базового класса** (`AIAgentBase`), который управляет переходом между состояниями в потокобезопасном режиме.
4.  **Стратегии отката** (`RollbackStrategy`), использующей контрольные точки для восстановления.

Это обеспечивает предсказуемость, возможность отладки (через историю) и восстановления после сбоев.

### Источники
[ИСТОЧНИК 1] AgentCheckpointData.kt · AgentCheckpointData · line 1
[ИСТОЧНИК 2] AgentCheckpointData.kt · AgentCheckpointData · line 32
[ИСТОЧНИК 3] AgentCheckpointData.kt · AgentCheckpointData · line 62
[ИСТОЧНИК 4] AIAgentBase.kt · representing · line 1
[ИСТОЧНИК 5] AIAgentState.kt · provides · line 1

## Q13: "как реализована обработка ошибок"
Исходя из предоставленного контекста, реализация обработки ошибок в кодовой базе koog/agents строится на нескольких ключевых компонентах, каждый из которых отвечает за определённый аспект. Вот детальный разбор:

### 1. Универсальная модель ошибки агента: `AIAgentError`
Это главный класс, предназначенный для инкапсуляции любой ошибки, возникшей в процессе работы AI-агента (стратегий, инструментов, нод). [ИСТОЧНИК 1]
*   **Структура:** Содержит три поля:
    *   `message` — человекочитаемое описание.
    *   `stackTrace` — полная трассировка стека как строка.
    *   `cause` — опциональная трассировка корневой причины (причина причины).
*   **Гибкость создания:** Имеет два конструктора:
    1.  Прямой конструктор для создания из готовых данных.
    2.  **Конструктор из `Throwable`** — ключевой механизм для перехвата и стандартизации исключений. Он автоматически извлекает сообщение, трассировку и причину из переданного исключения, подставляя "Unknown error" если сообщение отсутствует [ИСТОЧНИК 1]. Это позволяет оборачивать низкоуровневые ошибки (например, от сети, БД, сторонних библиотек) в единый понятный формат для всего фреймворка.

### 2. Классификация сервисных ошибок: `AgentServiceErrorType`
Это перечисление (`enum`) служит для **категоризации типовых ошибок**, специфичных для взаимодействия с сервисом агента. [ИСТОЧНИК 2]
Оно помогает differentiating между:
*   `UNEXPECTED_MESSAGE_TYPE` — получено сообщение неожиданного типа.
*   `MALFORMED_MESSAGE` — сообщение имеет неверный формат (пропущены поля, неверные типы данных).
*   `AGENT_NOT_FOUND` — запрошенный агент не существует.
*   `UNEXPECTED_ERROR` — любая другая непредвиденная ошибка.
*   **Применение:** Этот тип, вероятно, используется внутри других структур (например,答案是 ошибках ответа API) для быстрой программной обработки и маршрутизации ошибок на основе их категории, а не только для логирования текста.

### 3. Безопасное выполнение инструментов: `SafeTool` и `Result`
Этот класс описывает механизм для **безопасного вызова инструментов (`Tool`)** внутри агент-среды. Его ядро — sealed interface `Result<TResult>` с двумя состояниями: [ИСТОЧНИК 3]
*   **`Success<TResult>`** — выполнение прошло успешно, `content` содержит результат.
*   **`Failure`** — выполнение завершилось с ошибкой, `content` содержит сообщение об ошибке.
*   **Методы `isSuccessful()` / `isFailure()`** предоставляют type-safe способы проверки результата без использования `try-catch` в вызывающем коде. Это паттерн "Either" (Результат), который явно modelling успех и неудачу как части одного типа, что принуждает разработчика обрабатывать обе ситуации.

### 4. Специализированные контексты обработки событий инструментов
Контекст `DeprecatedExecuteToolEventHandlerContext` (хотя и помечен как deprecated) явно указывает на существование целой системы обработки событий жизненного цикла вызова инструмента, включая обработку ошибок [ИСТОЧНИК 5]. Из имён типов-алиасов видно, что есть отдельные контексты для:
*   `ToolCallFailedContext` — обработка **неудачного** выполнения (untime exception).
*   `ToolValidationFailedContext` — обработка ошибки **валидации** аргументов перед выполнением.
*   `ToolCallCompletedContext` — обработка **успешного** завершения.
*   Это указывает на **event-driven архитектуру**, где разные фазы (валидация, старт, завершение, ошибка) генерируют события, и можно подписываться на них для кастомной логики (логирование, метрики, пользовательские реакции на ошибки).

### 5. Надёжность сериализации: `jsonConfig`
Конфигурация JSON (`defaultFeatureMessageJsonConfig`) настраивает сериализатор для сообщений между компонентами (особенно в remote-сценариях). [ИСТОЧНИК 4]
*   **`ignoreUnknownKeys = true`** — критически важная настройка для **отказоустойчивости при десериализации**. Если отправитель (агент) добавил новое поле в сообщение, а получатель (сервис) ещё не знает об этом, ошибка десериализации не произойдёт. Это предотвращает падение системы из-за несовместимости версий протокола.
*   `explicitNulls = false` — оптимизирует сериализацию, убирая `null` поля.
*   `isLenient = true` — делает парсинг более гибким.
*   **Вместе:** Эти настройки обеспечивают стабильность при передаче **сообщений об ошибках** (`AIAgentError`, `AgentServiceErrorType`) между сервисами.

### Общая картина обработки ошибок

1.  **Возникновение:** Ошибка возникает внутри логи инструмента, стратегии или при коммуникации (`Throwable`).
2.  **Стандартизация:** `Throwable` оборачивается в `AIAgentError` через конструктор, что гарантирует единый формат для всех ошибок агента [ИСТОЧНИК 1].
3.  **Классификация:** Для сервисных/протокольных ошибок определение типа (`AgentServiceErrorType`) помогает в маршрутизации и пользовательских сообщениях [ИСТОЧНИК 2].
4.  **Передача результата:** Вызов инструмента возвращает `SafeTool.Result`, который явно разделяет успех (`Success`) и провал (`Failure`), обеспечивая compile-time безопасность для вызывающей стороны [ИСТОЧНИК 3].
5.  **События:** На разных стадиях (включая ошибку валидации или выполнения) генерируются события (`ToolCallFailedEvent`, `ToolValidationFailedEvent` — судя по импортам в `jsonConfig.kt` [ИСТОЧНИК 4]), позволяя подписчикам реагировать.
6.  **Маршалинг:** При передаче через сеть ошибки сериализуются в JSON с настройками, устойчивыми к изменениям в структуре сообщений [ИСТОЧНИК 4].

### Источники
[ИСТОЧНИК 1] AIAgentError.kt · provides · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/model/AIAgentError.kt
[ИСТОЧНИК 2] Error.kt · AgentServiceErrorType · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/model/Error.kt
[ИСТОЧНИК 3] SafeTool.kt · designed · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/environment/SafeTool.kt
[ИСТОЧНИК 4] jsonConfig.kt · featureMessageJsonConfig · line 25 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/remote/jsonConfig.kt
[ИСТОЧНИК 5] DeprecatedExecuteToolEventHandlerContext.kt · ToolEventHandlerContext · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/handler/DeprecatedExecuteToolEventHandlerContext.kt

## Q14: "какие механизмы работы с опасными tool calling"
На основе предоставленного контекста можно выделить следующие механизмы работы с потенциально опасными или проблемными вызовами инструментов (tool calling) в системе:

### 1. Событийная система для отслеживания состояний вызовов инструментов
Система использует классов-событий для моделирования жизненного цикла вызова инструмента, включая его проблемные этапы:
*   **`ToolCallStartingEvent` [ИСТОЧНИК 1-2]**: Событие, фиксирующее начало выполнения вызова инструмента. Содержит идентификаторы (`runId`, `toolCallId`), имя инструмента (`toolName`), его аргументы (`toolArgs`) и временную метку. Позволяет отследить, какой инструмент и с какими параметрами был запущен.
*   **`ToolValidationFailedEvent` [ИСТОЧНИК 3]**: Событие, возникающее **на этапе валидации** инструмента (до его реального выполнения). Оно фиксирует ошибку валидации, содержащую описание (`error: AIAgentError`), сообщение (`message`) и, опционально, описание инструмента (`toolDescription`). Этот механизм отделяет проблемы с корректностью запроса от проблем во время исполнения.
*   **`ToolCallFailedEvent` [ИСТОЧНИК 4]**: Событие, фиксирующее **сбой во время фактического выполнения** инструмента. Содержит информацию об ошибке (`error: AIAgentError?`), что позволяет различить типы сбоев (валидация vs. исполнение).

### 2. Интерфейс для выполнения инструментов и обработки проблем
Интерфейс `AIAgentEnvironment` [ИСТОЧНИК 5] определяет ключевые операции для работы со средой выполнения агента:
*   **`executeTool(toolCall: Message.Tool.Call)`** и **`executeTools(toolCalls: List<Message.Tool.Call>)`**: Основные методы для запуска инструментов. Контекст не раскрывает их внутреннюю логику обработки ошибок, но их наличие предполагает, что именно через них происходит инициирование вызовов, которые затем генерируют описанные выше события (`ToolCallStartingEvent`, `ToolCallFailedEvent` и т.д.).
*   **`reportProblem(exception: Throwable)`**: Явно выделенный метод для **сообщения о проблемах (error reporting)** в среде выполнения. Его назначение — обработать исключения или иные критические инциденты, возникшие в процессе работы агента или его инструментов. Это точка сбора информации о непредвиденных обстоятельствах, которая затем может интегрироваться в систему логирования через соответствующие события.

### Ключевые аспекты "опасности" и обработки:
1.  **Разделение фаз**: Система явно разделяет **ошибку валидации** (`ToolValidationFailedEvent`) и **ошибку выполнения** (`ToolCallFailedEvent`). Это позволяет более точно диагностировать проблему: в первом случае запрос некорректен (опасность на уровне данных/аргументов), во втором — сам инструмент или внешняя среда не справились с задачей (опасность на уровне исполнения).
2.  **Структурированная отчетность**: Все протоколируемые события (`ToolCallStartingEvent`, `ToolValidationFailedEvent`, `ToolCallFailedEvent`) наследуют общий интерфейс `DefinedFeatureEvent` и содержат стандартизированные поля: `eventId`, `executionInfo` (контекст выполнения), `runId`, `toolCallId`, `timestamp`. Это обеспечивает сквозную трассируемость (tracing) каждого вызова инструмента от старта до результата (успешного или нет).
3.  **Типизированные ошибки**: Ошибки передаются через структурированный тип `AIAgentError` (используется в `ToolValidationFailedEvent` и `ToolCallFailedEvent`), а не как простые строки. Это предполагает, что система поддерживает классификацию и программную обработку разных типов ошибок.
4.  **Явная точка для критических проблем**: Метод `reportProblem` служит "последней инстанцией" для экстренных ситуаций, которые могут не укладываться в парадигму одного инструмента/одного вызова (например, крах всего агента или сбой в инфраструктуре).

**Важное ограничение**:
Предоставленный контекст содержит **только определения интерфейсов и data-классов событий**. Он не показывает:
*   **Конкретную логику обработки** этих событий (кто их слушает и что делает).
*   **Политики безопасности** (например, ограничения на типы инструментов, валидацию аргументов по схеме, sandboxing).
*   **Механизмы повторов, таймаутов или circuit breakers** для нестабильных инструментов.
*   **Как именно `reportProblem` интегрируется** сFlow событий.
*   **Реализацию** интерфейса `AIAgentEnvironment`.

Таким образом, система предоставляет **каркас для мониторинга и классификации проблем** с вызовами инструментов через события и явный интерфейс для эскалации критических инцидентов, но детали реальной "безопасной" обработки (валидация, ограничения, восстановление) требуют изучения других частей кодовой базы.

### Уточнение
Чтобы дать более полный ответ о механизмах **защиты** от опасных вызовов (например, валидация входных данных, ограничение прав, sandboxing), необходимо изучить:
1.  Как и где происходит **валидация аргументов** `toolArgs` перед вызовом `executeTool`?
2.  Каковы **реализации** `AIAgentEnvironment`? (Например, `RealAIAgentEnvironment` или моки). В них может быть инкапсулирована вся логика безопасности.
3.  Есть ли отдельные модули/интерфейсы, отвечающие за **проверку разрешений (permissions)** или **сценарии использования (usage policies)** для инструментов?

---
### Источники
[ИСТОЧНИК 1] toolExecutionEvents.kt · ToolCallStartingEvent · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/model/events/toolExecutionEvents.kt
[ИСТОЧНИК 2] toolExecutionEvents.kt · ToolCallStartingEvent · line 38 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/model/events/toolExecutionEvents.kt
[ИСТОЧНИК 3] toolExecutionEvents.kt · ToolCallStartingEvent · line 75 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/model/events/toolExecutionEvents.kt
[ИСТОЧНИК 4] toolExecutionEvents.kt · ToolCallStartingEvent · line 113 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/model/events/toolExecutionEvents.kt
[ИСТОЧНИК 5] AIAgentEnvironment.kt · with · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/environment/AIAgentEnvironment.kt

## Q15: "где хранится текущий диалог агента с пользователем и какие возможности по его управлению"
Основываясь на предоставленном контексте, можно дать следующий ответ:

### Где хранится текущий диалог агента с пользователем
Текущий диалог (история разговора) между агентом и пользователем хранится и управляется через компонент **`ChatMemory`**, который является встроенной возможностью (feature) агента [ИСТОЧНИК 1].

Ключевая часть этого механизма — это **`chatHistoryProvider`** (провайдер истории чата), который передаётся при конфигурации `ChatMemory` в блоке `installChatMemory`. Этот провайдер реализует логику физического сохранения и загрузки сообщений (объектов `Message`) в различных хранилищах (память, файлы, базы данных и т.д.). Внешний провайдер подключается к агенту и становится источником данных для `ChatMemory`.

### Возможности по управлению историей диалога
Управление историей диалога (что, как и когда хранится) осуществляется через конфигурацию `ChatMemoryConfig`, которая передаётся при установке (install) фичи `ChatMemory`. Основные возможности включают:

1.  **Ограничение размера истории (окно)**: С помощью метода `windowSize(Int)` можно задать фиксированное количество последних сообщений, которые будут сохраняться. Это реализует стратегию "скользящего окна" и помогает управлять размером контекста, отправляемого в LLM [ИСТОЧНИК 1].
2.  **Предварительная обработка сообщений**: Через `applyPreProcessors(messages, preProcessors)` можно цепочкой применить к загруженной истории или к новым сообщениям одно или несколько преобразований (например, фильтрацию, обрезку, модификацию) перед их использованием [ИСТОЧНИК 1].
3.  **Выбор провайдера хранения**: Гибкость в выборе хранилища (in-memory, файловое, удалённое) определяется реализацией, передаваемой в `chatHistoryProvider`. Это позволяет адаптировать механизм под конкретные требования по производительности, долговечности или безопасности.

### Технический контекст интеграции
*   `ChatMemory` интегрируется в сценарии выполнения агента на уровне **пайплайна** (`AIAgentGraphPipeline`, `AIAgentFunctionalPipeline`, `AIAgentPlannerPipeline`), что позволяет ему автоматически загружать историю перед шагом обработки и сохранять её после [ИСТОЧНИК 1].
*   Для доступа к текущему состоянию, включая загруженные сообщения, внутри Agents SDK существует контекст выполнения — `AIAgentFunctionalContext`. Он содержит ссылку на `storage: AIAgentStorage` и `stateManager: AIAgentStateManager` [ИСТОЧНИК 2, ИСТОЧНИК 3], которые, возможно, используются самим `ChatMemory` или связанными компонентами для хранения сессионных данных.

### Важное уточнение
Контекст также содержит `AgentMemoryProvider` [ИСТОЧНИК 4] — это интерфейс для работы с **персистентной, структурированной памятью агента** (хранение фактов, концепций, знаний о мире), а не для хранения неструктурированной истории диалога. Обе системы (`ChatMemory` для истории разговоров и `AgentMemoryProvider` для фактов) могут сосуществовать в одном агенте, но решают разные задачи.

**Вывод:** Текущий диалог хранится через подключённый к агенту `ChatMemory` с использованием переданного `chatHistoryProvider`. Управление сводится к ограничению количества сообщений (`windowSize`) и цепочке их предобработки (`applyPreProcessors`) на уровне конфигурации `ChatMemoryConfig`.

### Источники
[ИСТОЧНИК 1] ChatMemory.kt · ChatMemory · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/chatMemory/feature/ChatMemory.kt  
[ИСТОЧНИК 2] AIAgentFunctionalContext.kt · AIAgentFunctionalContext · line 28 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentFunctionalContext.kt  
[ИСТОЧНИК 3] AIAgentFunctionalContext.kt · AIAgentFunctionalContext · line 46 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentFunctionalContext.kt  
[ИСТОЧНИК 4] AgentMemoryProvider.kt · for · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/memory/providers/AgentMemoryProvider.kt  
[ИСТОЧНИК 5] AgentMemoryProvider.kt · for · line 137 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/memory/providers/AgentMemoryProvider.kt

## Q16: "как устроена observability и работа с событиями от агентов "
На основе предоставленного контекста можно описать архитектуру **системы обработки событий (event handling)** в AI-агентах фреймворка Koog, но недостаточно информации о полной системе **observability** (наблюдаемости) с интеграцией мониторинга, логов и трассировки.

### Архитектура обработки событий от агентов

В фреймворме реализована ** расширяемая система обработки событий жизненного цикла агента и его компонентов ** через和下 concepts:

**1. Иерархия контекстов событий (Event Contexts)**
   - Базовый интерфейс: `AgentEventContext` [ИСТОЧНИК 3], расширяющий `AgentLifecycleEventContext`.
   - Конкретные типы событий представлены data-классами:
     - `AgentStartingContext` — событие запуска агента [ИСТОЧНИК 3].
     - `AgentCompletedContext` — успешное завершение агента [ИСТОЧНИК 3].
     - `AgentExecutionFailedContext` — ошибка выполнения агента [ИСТОЧНИК 1].
     - `AgentClosingContext` — процесс завершения агента [ИСТОЧНИК 1].
   - Также предусмотрены контексты для подкомпонентов:
     - Стратегий (`StrategyStartingContext`, `StrategyCompletedContext`).
     - Узлов (`NodeExecutionStartingContext`, `NodeExecutionFailedContext`).
     - Вызовов LLM (`LLMCallStartingContext` и другие, включая потоковые `LLMStreamingFrameReceivedContext`).
     - Инструментов (`ToolCallStartingContext`, `ToolCallCompletedContext`, `ToolValidationFailedContext`).
     - Подграфов (`SubgraphExecutionStartingContext`, `SubgraphExecutionFailedContext`).

**2. Конфигурация обработчиков (EventHandlerConfig)**
   - `EventHandlerConfig` [ИСТОЧНИК 4] — публичный конфигурационный класс, предоставляющий DSL для регистрации лямбда-обработчиков:
     ```kotlin
     handleEvents {
         onToolCallStarting { eventContext -> ... }
         onAgentCompleted { eventContext -> ... }
     }
     ```
   - Внутренняя реализация `EventHandlerConfigImpl` [ИСТОЧНИК 5] хранит приватные свойства-обработчики (например, `_onAgentStarting`) и, вероятно, предоставляет методы для их установки (`_setOnAgentStarting`) и выполнения.

**3. Механизм регистрации и вызова**
   - Контекстные классы (например, `AgentStartingContext`) содержат:
     - `eventId` — уникальный идентификатор события.
     - `executionInfo` — информация об исполнении (родительские ID, путь выполнения).
     - Ссылки на агент (`AIAgent<*, *>`), контекст выполнения (`AIAgentContext`), результат или ошибку.
   - Это позволяет передавать в обработчики полный контекст для логирования, метрик или реакции.

**4. Устаревшие типы**
   - Файл `DeprecatedAgentEventHandlerContext` [ИСТОЧНИК 1] показывает миграцию со старых typealias' (`AgentEventHandlerContext`, `AgentTransformEnvironmentContext` и др.) на новые классы из пакета `ai.koog.agents.core.feature.handler.agent`.

### Недостающие элементы для полноценной Observability

Предоставленные классы описывают **только механизм обработки событий внутри агента**, но не показывают:
- **Как события публикуются в систему observability** (например, через Event Bus, логирование, отправка в внешние системы типа Prometheus, Jaeger, OpenTelemetry).
- **Интеграцию с логгерами, метриками или трейсерами** — в контексте нет упоминаний SLF4J, Micrometer, OpenTelemetry API.
- **Агрегацию событий или централизованный сборщик** — обработчики, судя по коду, локальны для каждого агента/запуска.
- **Структуру топиков/каналов** или способов подписки на события извне.

Вывод: в фреймворме заложена **основа для observability через события жизненного цикла**, но для реализации полноценной наблюдаемости потребуется дополнительная интеграция с внешними инструментами мониторинга и логирования, которая в предоставленных файлах не отражена.

### Уточняющий вопрос
Насколько понимаю, система обработки событий сама по себе не предоставляет готовой интеграции с observability-стеком. **Нужно ли уточнять, как именно публикуются эти события (например, через общий event bus) или как подключить внешние системы сбора метрик/логов?**

## Q17: "что такое GraphAIAgent, каковы его преимущества и схемы использования?"
На основе предоставленного контекста **прямого определения класса `GraphAIAgent` не найдено**. Вместо этого контекст описывает инфраструктуру для построения, управления и тестирования **агентов, архитектура которых основана на графах (graph-based AI agents)**. Таким образом, под термином `GraphAIAgent` можно понимать **агент, реализованный как исполняемый граф узлов (stages/nodes)**, управляемый ядром `AIAgentGraphPipeline` и конфигурируемый через `AIAgentBuilderAPI` с использованием DSL из `AIAgentNodes`.

Основываясь на анализе кодовой базы, можно описать его ключевые аспекты.

### 1. Что такое GraphAIAgent (в контексте этого фреймворка)
Это архитектурный паттерн реализации AI-агента, где его логика выполнения представлена в виде **направленного графа**.
*   **Граф** состоит из **узлов (nodes/stages)**, каждый из которых выполняет определённую задачу (например, вызов LLM, выполнение инструмента, работа с промптом).
*   **Узлы связаны рёбрами (edges)**, определяющими поток данных и контроля между этапами.
*   **Исполнение графа** управляется специальным компонентом — `AIAgentGraphPipeline`, который координирует выполнение узлов в соответствии с их связями [ИСТОЧНИК 3].
*   **Конфигурация графа** происходит на этапе сборки агента через `AIAgentBuilderAPI`, где можно устанавливать графовые фичи (`install(feature)`) [ИСТОЧНИК 4].
*   **Построение графа** осуществляется с помощью Domain-Specific Language (DSL), предоставляемого через функции вроде `nodeLLMSendToolResultOnlyCallingTools`, `nodeExecuteMultipleTools` и других из `AIAgentNodes` [ИСТОЧНИК 5].

### 2. Преимущества такой архитектуры (логические выводы из контекста)
Использование графовой модели предоставляет несколько ключевых преимуществ:
*   **Явное и декларативное описание потока выполнения.** Логика агента представляется как наглядный граф узлов и связей, а не как сложная последовательность императивных вызовов.
*   **Высокая модульность и переиспользование.** Каждый узел — это независимый, тестируемый компонент с чётким входом и выходом. Один и тот же узел (например, `callLLM`) можно использовать в разных частях графа.
*   **Мощные возможности тестирования.** Фреймворк включает специальный `Testing` feature с API `testGraph { ... }`, который позволяет проводить **структурные тесты графа**: проверять порядок стадий (`assertStagesOrder`), достижимость узлов (`assertReachable`), поведение узлов на конкретных входных данных (`assertNodes`) и корректность соединений рёбер (`assertEdges`) [ИСТОЧНИК 1, ИСТОЧНИК 2]. Это напрямую следует из целевого назначения `graph(test: ...)` функции.
*   **Гибкость и расширяемость.** Благодаря системе **фич (features)** и пайплайну `AIAgentGraphPipeline`, можно легко внедрять перехватчики (interceptors) для логирования, мониторинга, обработки ошибок или модификации executions на уровнях "до узла", "после узла" и "завершения подграфа" (смотри методы `interceptNodeExecution...` и `interceptSubgraphExecution...` в [ИСТОЧНИК 3]).
*   **Чёткое разделение ответственности.** DSL-функции из `AIAgentNodes` [ИСТОЧНИК 5] абстрагируют сложность работы с окружением (`environment`), сессиями LLM (`llm.writeSession`), промптами и инструментами, позволяя разработчику фокусироваться на бизнес-логике потока.

### 3. Схемы использования (на основе примеров и API)
Использование графового агента выглядит как процесс построения и конфигурации графа:

**Схема 1: Определение агента через DSL (основной паттерн)**
```kotlin
val agent = AIAgent(
    promptExecutor = ..., // Обязательные зависимости
    toolRegistry = ...,
    strategy = ...,
    eventHandler = ...,
    agentConfig = ...
) {
    // В этом блоке (FeatureContext) строится граф с помощью DSL-функций
    node("first_stage") { input ->
        // Логика узла
        ...
    }

    node("second_stage") { previousOutput ->
        ...
    }

    // Установка связи между узками (edges)
    edge(from = "first_stage", to = "second_stage")
}
```

**Схема 2: Установка графовых фич при сборке**
Через `AIAgentBuilderAPI` можно явно установить графовую фичь (например, для тестирования) до построения агента [ИСТОЧНИК 4]:
```kotlin
val builder = AIAgent {
    promptExecutor = ...
    // Установка фичи тестирования графа
    install(testingFeature) {
        // конфигурация фичи
    }
    // ... другие настройки
    // Далее можно использовать DSL для построения графа внутри builder
}
val agent = builder.build()
```

**Схема 3: Тестирование структуры и поведения графа**
Использование специального API для интеграционных тестов, который запускает агент в режиме верификации графа [ИСТОЧНИК 2]:
```kotlin
agent.testGraph("my_test_subgraph") {
    // Проверка порядка стадий/узлов
    assertStagesOrder("input_processing", "llm_call", "tool_execution", "final_response")

    // Детальная проверка конкретного подграфа (стадии)
    stage("llm_call") {
        val start = startNode()
        val finish = finishNode()
        val callLLM = assertNodeByName<String, Message.Response>("callLLM")

        // Проверка достижимости
        assertReachable(start, callLLM)
        assertReachable(callLLM, finish)

        // Проверка поведения узла: вход -> ожидаемый выход
        assertNodes {
            callLLM withInput "What's the weather?" outputs Message.Assistant("I need to use a tool.")
        }

        // Проверка рёбер: выход одного узла должен вести к другому
        assertEdges {
            callLLM withOutput Message.Assistant("I need to use a tool.") goesTo node.executeTool
        }
    }
}
```

**Схема 4: Перехват событий выполнения графа**
Через `AIAgentGraphPipeline` можно подписаться на события жизненного цикла узлов и подграфов для логирования, сбора метрик или дополнительной обработки [ИСТОЧНИК 3]:
```kotlin
pipeline.interceptNodeExecutionBefore(feature) { eventContext ->
    logger.debug("Node ${eventContext.node.name} is about to execute with input: ${eventContext.input}")
}

pipeline.interceptSubgraphExecutionCompleted(feature) { eventContext ->
    logger.info("Subgraph ${eventContext.subgraph.name} finished. Output: ${eventContext.output}")
}
```

### Вывод
`GraphAIAgent` в данном контексте — это не отдельный класс, а **архитектурный стиль агента**, реализуемый через комбинацию компонентов:
*   **`AIAgentGraphPipeline`** — исполнитель графа [ИСТОЧНИК 3].
*   **`AIAgentBuilderAPI` с `install(feature)`** — конфигуратор, позволяющий добавлять графовые возможности [ИСТОЧНИК 4].
*   **DSL из `AIAgentNodes`** — конструктор графа из узлов [ИСТОЧНИК 5].
*   **`Testing` feature с `testGraph`** — инструмент для верификации графовой структуры и поведения [ИСТОЧНИК 1, ИСТОЧНИК 2].

Главное преимущество — **возможность создавать сложные, многозадачные агенты с явной, тестируемой и модульной структурой**, где каждый шаг (узел) изолирован и имеет предсказуемые точки входа/выхода.

---
### Источники
[ИСТОЧНИК 1] Api.kt · Testing.Config.graph · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-test/src/commonMain/kotlin/ai/koog/agents/testing/feature/Api.kt
[ИСТОЧНИК 2] Api.kt · FeatureContext.testGraph · line 48 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-test/src/commonMain/kotlin/ai/koog/agents/testing/feature/Api.kt
[ИСТОЧНИК 3] AIAgentGraphPipeline.kt · manages · line 268 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/pipeline/AIAgentGraphPipeline.kt
[ИСТОЧНИК 4] AIAgentBuilderAPI.kt · AIAgentBuilderAPI.install · line 169 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentBuilderAPI.kt
[ИСТОЧНИК 5] AIAgentNodes.kt · nodeLLMSendToolResultOnlyCallingTools · line 539 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/extension/AIAgentNodes.kt

## Q18: "все полезное про AIAgentContext и AIAgentGraphContext"
На основе предоставленных исходников можно извлечь следующую информацию о `AIAgentContext` и `AIAgentGraphContext`. Важно отметить, что данные несколько фрагментарны, и полная картина требует дополнительных файлов.

### 1. Иерархия и основная роль
*   `AIAgentGraphContextBase` — это интерфейс, расширяющий (наследующий) более общий `AIAgentContext` [ИСТОЧНИК 3]. Он предоставляет базовый контекст, специально предназначенный для AI-агентов, работающих в рамках **графовой структуры** (graph-based workflows).
*   Основная ответственность интерфейса — служить фундаментом для определения дополнительных конструкций и поведения, необходимых для выполнения агента в графовых конвейерах. Это включает обработку граф-специфической логики, такой как обход узлов (node traversal), управление вводами/выводами и работа со сложными зависимостями между узлами графа [ИСТОЧНИК 3].

### 2. Ключевые возможности AIAgentGraphContextBase
Из интерфейса [ИСТОЧНИК 3] видны следующие обязательные члены:
*   `pipeline: AIAgentGraphPipeline` — предоставляет доступ к конвейеру (pipeline) управления выполнением узлов графа.
*   `agentInputType: TypeToken` — хранит информацию о типе входных данных (`agentInput`) для агента.
*   `copy(...)` — метод для создания копии текущего контекста с возможностью выборочного переопределения его свойств (например, environment, agentId, agentInput). Это позволяет создавать новые контекстные экземпляры для разных этапов или ветвей выполнения.

### 3. Связь с выполнением подграфов (Subgraphs)
Контекст графа (`AIAgentGraphContextBase`) является неотъемлемой частью событий, связанных с выполнением **подграфов** (subgraphs) — повторно используемых фрагментов графа [ИСТОЧНИК 1]. Он передаётся в следующие классы контекстов событий:
*   `SubgraphExecutionStartingContext` — контекст события начала выполнения подграфа. Содержит ссылку на `context: AIAgentGraphContextBase`, в котором будет выполняться подграф [ИСТОЧНИК 1].
*   `SubgraphExecutionCompletedContext` — контекст события успешного завершения подграфа. Также содержит `context: AIAgentGraphContextBase` [ИСТОЧНИК 1].
*   `SubgraphExecutionFailedContext` — контекст события сбоя выполнения подграфа. Также содержит `context: AIAgentGraphContextBase` [ИСТОЧНИК 1].

### 4. Интеграция с конвейером (Pipeline)
Конвейер агента графа (`AIAgentGraphPipeline`) тесно взаимодействует с контекстом. `AIAgentGraphContextBase` предоставляет экземпляр этого конвейера через свойство `pipeline` [ИСТОЧНИК 3]. Сам конвейер, в свою очередь, использует контекст графа (`context: AIAgentGraphContextBase`) как параметр при триггере обработчиков (handlers) узлов `triggerNodeHandlersBeforeExecution` [ИСТОЧНИК 2]. Это демонстрирует циклическую зависимость: контекст содержит пайплайн, а пайплайн использует контекст для управления выполнением.

### 5. Отсутствие полного определения AIAgentContext
В предоставленных исходниках **нет** полного определения класса/интерфейса `AIAgentContext`. Он лишь упоминается как родительский интерфейс для `AIAgentGraphContextBase` [ИСТОЧНИК 3]. Следовательно, невозможно детально описать его свойства и методы (такие как `environment`, `stateManager`, `storage` и т.д.), так как они могли бы быть определены в родительском интерфейсе или классе, который не вошёл в выборку.

### 6. Примеры использования в других фичах
Контекст графа используется как зависимость в других компонентах фреймворка, например:
*   Фича `MessageTokenizer` может быть установлена для `AIAgentGraphFeature` и получает доступ к контексту агента, который реализует `AIAgentGraphContextBase` [ИСТОЧНИК 4].
*   Фича `A2AAgentServer` (для режима A2A-сервера) также работает с `AIAgentGraphFeature` и предоставляет доступ к `RequestContext` и `SessionEventProcessor` внутри стратегий агента, оперируя контекстом агента [ИСТОЧНИК 5].

### 7. Критические пробелы в информации
На основе анализа источников **недостаточно информации** о следующих ключевых аспектах:
*   **Полное определение `AIAgentContext`**: его свойства, методы, назначение.
*   **Конкретный класс `AIAgentGraphContext`**: в источниках представлен только интерфейс `AIAgentGraphContextBase`. Неясно, существует ли отдельный финальный класс с таким именем или это синоним. Возможно, `AIAgentGraphContext` — это конкретный класс, реализующий этот интерфейс, но его исходник не предоставлен.
*   **Внутренняя реализация `copy`**: как именно создаётся копия контекста, какие поля копируются глубоко, а какие поверхностно.
*   **Работа с состояниями (state) и хранилищем (storage)**: хотя они упоминаются в imports `AIAgentGraphContext.kt`, их связь с контекстом графа не раскрыта в предоставленных фрагментах.

### Вывод
`AIAgentGraphContextBase` — это специализированный контекст для графовых агентов, который управляет пайплайном и типизированным вводом, а также служит связующим звеном для событий выполнения подграфов. Его родительский интерфейс `AIAgentContext` остаётся без подробного описания. Для полного понимания архитектуры необходимы исходники `AIAgentContext.kt` и конкретной реализации `AIAgentGraphContext`.

---
### Источники
[ИСТОЧНИК 1] SubgraphExecutionEventContext.kt · SubgraphExecutionEventContext · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/handler/subgraph/SubgraphExecutionEventContext.kt
[ИСТОЧНИК 2] AIAgentGraphPipeline.kt · manages · line 268 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/pipeline/AIAgentGraphPipeline.kt
[ИСТОЧНИК 3] AIAgentGraphContext.kt · extends · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentGraphContext.kt
[ИСТОЧНИК 4] MessageTokenizer.kt · for · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-tokenizer/src/commonMain/kotlin/ai/koog/agents/features/tokenizer/feature/MessageTokenizer.kt
[ИСТОЧНИК 5] A2AAgentServer.kt · A2AAgentServer · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-a2a-server/src/commonMain/kotlin/ai/koog/agents/a2a/server/feature/A2AAgentServer.kt

## Q19: "опиши работу с AIAgentLLMContext"
На основе предоставленного контекста опишу работу с `AIAgentLLMContext`. Важно отметить, что контекст является частичным — приведены только заголовки классов и фрагменты кода, без полной реализации методов. Ниже представлено описание того, что можно определить из имеющихся данных.

### Общая роль и архитектура
`AIAgentLLMContext` [ИСТОЧНИК 2] является ключевым компонентом, который управляет всем жизненным циклом взаимодействия AI-агента с языковой моделью (LLM). Он служит основным контейнером для:
- **Инструментов (tools)**: список `ToolDescriptor` и регистр `ToolRegistry` для метаданных об инструментах.
- **Промптов (prompts)**: текущий промпт, который может модифицироваться в сессиях записи.
- **Модели (model)**: выбранная языковая модель типа `LLModel`.
- **Обработки ответов (response processing)**: необязательный `ResponseProcessor` для пост-обработки сообщений от LLM.
- **Исполнителя промптов (prompt executor)**: компонент `PromptExecutor`, отвечающий за выполнение операций над промптом.
- **Окружения (environment)**: `AIAgentEnvironment`, управляющий выполнение инструментов и внешними зависимостями.
- **Времени (clock)**: `Clock` для временных меток сообщений.

Контекст обеспечивает **потокобезопасность** через разделение на сессии чтения (`AIAgentLLMReadSession`) и записи (`AIAgentLLMWriteSession`) [ИСТОЧНИК 3]. Это позволяет выполнять конкурентные операции без состояния гонки.

### Конструктор и инициализация
Конструктор `AIAgentLLMContext` [ИСТОЧНИК 2] принимает:
- `tools: List<ToolDescriptor>` — доступные инструменты.
- `toolRegistry: ToolRegistry` — реестр инструментов.
- `prompt: Prompt` — начальный промпт.
- `model: LLModel` — языковая модель.
- `responseProcessor: ResponseProcessor?` — обработчик ответов (может быть `null`).
- `promptExecutor: PromptExecutor` — исполнитель промптов.
- `environment: AIAgentEnvironment` — окружение агента.
- `config: AIAgentConfig` — конфигурация агента (упомянута в API).
- `clock: Clock` — часы для временных меток.

Класс объявлен как `expect` (многоплатформенный), с реализацией в платформо-специфичных модулях (например, JVM). В контексте используется оптимизация `OptIn(DetachedPromptExecutorAPI::class, InternalAgentsApi::class)` [ИСТОЧНИК 4].

### Сессии работы: чтение и запись
`AIAgentLLMContext` предоставляет два основных типа сессий (через методы `handleRead()` и `handleWrite()`, упомянутые в описании, но не показанные в коде):
- **`AIAgentLLMReadSession`** (по `AIAgentLLMSessionAPI` [ИСТОЧНИК 3]):
  - Только для чтения состояния контекста (prompt, tools, model, responseProcessor).
  - Позволяет выполнять запросы к LLM: `requestLLMWithoutTools()`, `requestLLMWithTools()`, `requestLLMMultipleWithoutTools()`.
  - Не изменяет состояние контекста (prompt не меняется).
  - Использует текущие инструменты из `tools` и модель из `model`.

- **`AIAgentLLMWriteSession`** (аналогичен `AIAgentLLMSessionAPI`):
  - Позволяет изменять состояние контекста (например, добавлять сообщения в промпт через `prompt.add(...)`).
  - После изменений изменения фиксируются в контексте.
  - Также поддерживает запросы к LLM, но с возможностью модифицировать промпт перед/после запроса.

### Управление инструментами
- Инструменты описаны через `ToolDescriptor` [ИСТОЧНИК 2], который содержит метаданные (имя, описание, параметры).
- Реестр `ToolRegistry` [ИСТОЧНИК 2,4] хранит информацию обо всех доступных инструментах. Сессии получают список `tools` на основе актуального состояния.
- **Важно**: `ToolArgs` [ИСТОЧНИК 1] объявлен устаревшим (`@Deprecated`). Раньше инструменты требовали отдельного класса аргументов, но теперь вся сериализация аргументов полностью обрабатывается через KotlinX Serialization. Это означает, что аргументы инструментов должны быть сериализуемыми типами, и ToolArgs больше не нужен.

### Работа с промптами и моделью
- `Prompt` [ИСТОЧНИК 3] содержит цепочку сообщений (Message) и параметры для LLM.
- `PromptExecutor` [ИСТОЧНИК 2,4] выполняет низкоуровневые операции над промптом (например, отправку в LLM). Существует аннотация `@DetachedPromptExecutorAPI` [ИСТОЧНИК 4] для методов, которые работают **вне** жизненного цикла агента (опасно, так как не влияет на состояние агента и игнорирует `ToolsConversionStrategy`).
- `LLModel` [ИСТОЧНИК 3] определяет конкретную модель (например, GPT-4).
- `ResponseProcessor` [ИСТОЧНИК 3] опционален и используется для пост-обработки ответов LLM (модерация, структурирование и т.д.).

### Взаимодействие с окружением
`AIAgentEnvironment` [ИСТОЧНИК 2,4] отвечает за:
- Выполнение инструментов (вызов внешних API, вычисления).
- Обработку событий окружения.
- Интеграцию с внешними системами.

### Потокобезопасность и транзакционность
Контекст использует паттерн **сессий** для обеспечения безопасности:
- `handleRead()` создает сессию только для чтения. Множество чтений могут работать параллельно.
- `handleWrite()` создает сессию для записи. Записи блокируют другие записи и чтения (полная эксклюзия).
- Изменения в `WriteSession` фиксируются в контексте только при успешном завершении.

### Интеграция с графом агента
`AIAgentGraphStrategyBuilder` [ИСТОЧНИК 5] строит стратегии выполнения агента (старт/финиш узлы, выбор инструментов). Хотя он напрямую не упоминает `AIAgentLLMContext`, стратегии, скорее всего, используют контекст для выполнения этапов графа.

### Примерный usage pattern (на основе контекста):
```kotlin
// Создание контекста (примерное)
val context = AIAgentLLMContext(
    tools = listOf(toolDescriptor1, toolDescriptor2),
    toolRegistry = toolRegistry,
    prompt = initialPrompt,
    model = LLModel.GPT4,
    responseProcessor = null,
    promptExecutor = promptExecutor,
    environment = environment,
    config = agentConfig,
    clock = Clock.System
)

// Чтение: запрос без инструментов
context.handleRead { session ->
    val response = session.requestLLMWithoutTools()
    // response - результат от LLM
}

// Запись: добавление сообщения и запрос с инструментами
context.handleWrite { session ->
    session.prompt.add(Message.User("Погода в Москве?"))
    val response = session.requestLLMWithTools()
    session.prompt.add(Message.Assistant(response))
}
```

### Критические ограничения контекста
1. **Неполная информация**: В контексте отсутствуют:
   - Полная сигнатура конструктора `AIAgentLLMContext` (обрыв на `@param`).
   - Реализации `handleRead()`, `handleWrite()`.
   - Детали `ToolRegistry`, `PromptExecutor`.
   - Различия между `AIAgentLLMReadSession` и `AIAgentLLMWriteSession` (только общий API).
   - Обработка ошибок, откаты транзакций.

2. **Устаревшие компоненты**: `ToolArgs` [ИСТОЧНИК 1] помечен как deprecated. Новые инструменты должны использовать сериализацию KotlinX напрямую.

3. **Детached API**: Использование `PromptExecutor` через `@DetachedPromptExecutorAPI` [ИСТОЧНИК 4] небезопасно — не влияет на состояние агента и обходит стратегии выбора инструментов.

### Источники
[ИСТОЧНИК 1] ToolArgs.kt · ToolArgs · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-tools/src/commonMain/kotlin/ai/koog/agents/core/tools/ToolArgs.kt  
[ИСТОЧНИК 2] AIAgentLLMContext.kt · AIAgentLLMContext · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/коог/agents/core/agent/context/AIAgentLLMContext.kt  
[ИСТОЧНИК 3] AIAgentLLMSessionAPI.kt · AIAgentLLMSessionAPI · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/session/AIAgentLLMSessionAPI.kt  
[ИСТОЧНИК 4] AIAgentLLMContextAPI.kt · DetachedPromptExecutorAPI · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentLLMContextAPI.kt  
[ИСТОЧНИК 5] AIAgentGraphStrategyBuilder.kt · responsible · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/builder/AIAgentGraphStrategyBuilder.kt

## Q20: "опиши назначение и как работать с AIAgentPlannerContext"
На основе предоставленного контекста **нельзя дать полное описание** класса или интерфейса `AIAgentPlannerContext`, так как ни один из представленных исходников не содержит прямого упоминания этого типа.

Предоставленные файлы описывают следующие смежные концепции:
1.  **`DummyAIAgentContext`** — тестовый ("mocked") контекст агента, который позволяет переопределять или проверять наличие LLM и окружения, а также управлять состоянием агента [ИСТОЧНИК 1][ИСТОЧНИК 2][ИСТОЧНИК 3].
2.  **`AIAgentSubgraphExt`** и **`AIAgentSubgraphBuilder`** — API для построения и управления подграфами (самостоятельными цепочками узлов) внутри основного графа агента. Они отвечают за определение задачи (`subgraphWithTask`), создание стартового и финального узлов, а также выбор инструментов и моделей [ИСТОЧНИК 4][ИСТОЧНИК 5].

Судя по названию `AIAgentPlannerContext`, он, вероятно, связан с **планированием (planner)** задач или последовательности шагов для агента, что является более высокой абстракцией над построением графов. Однако его структура, назначение и API не раскрыты в текущем наборе исходников.

**Чтобы дать точный ответ, необходимы исходники, в которых объявлен тип `AIAgentPlannerContext`.**

---
### Уточняющий вопрос:
Не могли бы вы предоставить исходный код файла, где определен класс/интерфейс `AIAgentPlannerContext`, или файлы, где он используется как параметр/возвращаемое значение?

## Q21: "что такое AIAgentGraphStrategy  и примеры использования"
В предоставленном контексте **не обнаружено класса или интерфейса с именем `AIAgentGraphStrategy`**. Однако система построения графов для AI-агентов (на основе фреймворка Koog Agents) actively использует концепции **графовых стратегий** через параметры, такие как `toolSelectionStrategy` и специализированные расширения для подграфов. Основываясь на контексте, можно описать, как реализуются графовые стратегии в этой кодовой базе.

### Что такое графовые стратегии в Koog Agents?
В данном контексте **графовая стратегия** определяется как способ организации и управления выполнением узлов (nodes) и ребер (edges) в графе агента, особенно в части:
1. **Выбора инструментов** (tools) для вызова языковой моделью (LLM) через `ToolSelectionStrategy`.
2. **Управления подграфами** (subgraphs) с повторными попытками (retry), задачами (task) и обработкой результатов.
3. **Конфигурации параметров LLM** (температура, количество вариантов и т.д.) через `AIAgentServiceBuilder`.

Ключевые компоненты:
- **`ToolSelectionStrategy`** (упоминается в [ИСТОЧНИК 3] и [ИСТОЧНИК 4]) — определяет, как LLM выбирает инструменты из реестра. Например, `ToolSelectionStrategy.ALL` требует использования всех доступных инструментов.
- **Подграфы (subgraphs)** — изолированные части графа с собственными входными/выходными типами, LLM-моделями и стратегиями. Они создаются через функции-расширения, такие как `subgraphWithTask` [ИСТОЧНИК 3] и `subgraphWithRetrySimple` [ИСТОЧНИК 4].
- **Построители (builders)** — `AIAgentSubgraphBuilder` [ИСТОЧНИК 5] и `AIAgentServiceBuilder` [ИСТОЧНИК 1, 2] предоставляют DSL для определения графов.

### Примеры использования графовых стратегий
#### 1. Создание подграфа с задачей (`subgraphWithTask`)
Этот метод [ИСТОЧНИК 3] определяет подграф, который выполняет конкретную задачу с помощью LLM и инструментов. Пример:
```kotlin
val processData by subgraphWithTask<Input, Output>(
    name = "processData",
    inputType = typeToken<Input>(),
    outputType = typeToken<Output>(),
    toolSelectionStrategy = ToolSelectionStrategy.ALL, // стратегия выбора всех инструментов
    llmModel = myModel,
    runMode = ToolCalls.SEQUENTIAL // последовательный вызов инструментов
) { input ->
    // Лямбда определяет задачу: формирует промпт для LLM
    "Process the input: $input and return structured output."
}
```
Здесь:
- `toolSelectionStrategy` задает, как LLM выбирает инструменты (все доступные).
- `runMode` управляет порядком вызова инструментов (последовательно или параллельно).
- Подграф автоматически создает узел `FinishTool` для возврата результата.

#### 2. Подграф с повторными попытками (`subgraphWithRetrySimple`)
Из [ИСТОЧНИК 4] — добавляет логику повторения, если условие не выполнено. Пример:
```kotlin
val retryLLMCall by subgraphWithRetrySimple(
    condition = { (it is Message.Tool.Call).asConditionResult }, // условие: ожидается вызов инструмента
    maxRetries = 2,
    toolSelectionStrategy = ToolSelectionStrategy.REQUIRED, // только обязательные инструменты
    strict = true // бросать исключение после exhausted retries
) {
    val nodeCallLLM by nodeLLMRequest("sendInput")
    nodeStart then nodeCallLLM then nodeFinish
}
```
- Условие проверяет, что LLM вернула вызов инструмента. Если нет, подграф повторяется до `maxRetries`.
- `toolSelectionStrategy` может отличаться от глобальной настройки.

#### 3. Настройка глобального сервиса агента через `AIAgentServiceBuilder`
[ИСТОЧЕНИК 1, 2] показывает, как конфигурировать整个 сервис агента, включая LLM-параметры:
```kotlin
val service = AIAgentServiceBuilder.graphAgentService<String, Result> {
    llmModel(OpenAIModel("gpt-4"))
    toolRegistry(myTools)
    systemPrompt("You are a helpful assistant.")
    temperature(0.7) // стратегия случайности вывода
    numberOfChoices(1)
}
```
Здесь:
- `temperature` и `numberOfChoices` влияют на поведение LLM, что косвенно является частью графовой стратегии генерации.

### Важные замечания
- **Информация неполная**: В контексте нет явного класса `AIAgentGraphStrategy`. Возможно, пользователь имел в виду `ToolSelectionStrategy` или общую концепцию стратегий построения графов, которая реализована через функции-расширения и параметры построителей.
- **DSL-подход**: Графы строятся в стиле Kotlin DSL с помощью `AIAgentSubgraphBuilder` [ИСТОЧНИК 5], где узлы (`nodeLLMRequest`, `nodeExecuteTool`) и ребра (`edge`) соединяются в цепочки.
- **Обработка результатов**: Методы в `AIAgentSubgraphExt` (как `toSafeResult`) [ИСТОЧНИК 3] обеспечивают безопасную обработку вывода, что тоже часть стратегии.

Если вам нужно точное определение `AIAgentGraphStrategy`, уточните, возможно, это внутренний класс или интерфейс, не попавший в предоставленные файлы. На основе контекста можно сказать, что **графовые стратегии в Koog Agents инкапсулированы в параметрах `toolSelectionStrategy`, `runMode` и расширениях для подграфов**.

### Источники
[ИСТОЧНИК 3] AIAgentSubgraphExt.kt · providing · line 197 · `/Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/AIAgentSubgraphExt.kt`
[ИСТОЧНИК 4] SubgraphWithRetry.kt · RetrySubgraphResult · line 192 · `/Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/SubgraphWithRetry.kt`
[ИСТОЧНИК 1] AIAgentServiceBuilder.kt · AIAgentServiceBuilder · line 106 · `/Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentServiceBuilder.kt`
[ИСТОЧНИК 2] AIAgentServiceBuilder.kt · AIAgentServiceBuilder · line 296 · `/Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentServiceBuilder.kt`
[ИСТОЧНИК 5] AIAgentSubgraphBuilder.kt · for · line 292 · `/Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/builder/AIAgentSubgraphBuilder.kt`

## Q22: "подробно про AIAgentNode  и его преимущества"
На основе предоставленного контекста можно детально описать концепцию `AIAgentNode` и его преимущества в рамках фреймворка **AI Koog Agents**. Важно отметить, что контекст содержит конкретные реализации и расширения (например, для работы с выбором LLM), что позволяет сделать обоснованные выводы о роли и сильных сторонах этого компонента.

### Что такое `AIAgentNode`?
`AIAgentNode` (в контексте представлен через классы-делегаты и строители) — это фундаментальный, переиспользуемый строительный блок для составления графов (workflows) ИИ-агентов. Он представляет собой узел в графе выполнения, который:
1.  **Принимает входные данные** определенного типа (`Input`).
2.  **Выполняет определенную логику** (например, вызов LLM, работу с инструментами, обработку результатов).
3.  **Возвращает выходные данные** определенного типа (`Output`).

Основная реализация абстрагирована через `AIAgentNodeDelegate` [ИСТОЧНИК 5], который инкапсулирует логику выполнения (`execute`). Этот делегат используется как в построении отдельных подграфов (`AIAgentSubgraphBuilder` [ИСТОЧНИК 3, 4]), так и в расширениях для специфических задач, таких как работа с множественным выбором ответов LLM (`AIAgentNodesWithChoiceExt` [ИСТОЧНИК 1]).

### Ключевые характеристики и преимущества `AIAgentNode`

#### 1. Высокая модульность и композируемость
*   **Преимущество**: Легко создавать сложные, многошаговые рабочие процессы из простых, самодостаточных компонентов.
*   **Как это работает**: Каждый узел решает одну четкую задачу (запрос к LLM, выбор решения, вызов одного инструмента). Они могут быть последовательно (пайплайн) или параллельно объединены в граф. Например, `nodeLLMSendResultsMultipleChoices` [ИСТОЧНИК 1] и `nodeSelectLLMChoice` [ИСТОЧНИК 1] — это два отдельных узла, которые вместе реализуют паттерн "запрос нескольких вариантов -> выбор одного".
*   **Контекст**: `AIAgentSubgraphBuilder` [ИСТОЧНИК 3] явно предназначен для сборки таких графов из узлов, определяя `nodeStart` и `nodeFinish`.

#### 2. Строгая безопасность типов (Type Safety)
*   **Преимущество**: Ошибки несоответствия типов обнаруживаются на этапе компиляции, а не во время выполнения, что критически важно для надежных систем.
*   **Как это работает**: Каждый узел (`AIAgentNodeDelegate<Input, Output>`) строго типизирован. Вход и выход узла согласуются при построении графа. Например, выход `nodeLLMSendResultsMultipleChoices` — `List<LLMChoice>` [ИСТОЧНИК 1], что становится входом для `nodeSelectLLMChoice`, который ожидает ровно этот тип. Это гарантирует корректность соединений.
*   **Контекст**: Использование `TypeToken` вместо `KType` (как в `AIAgentSubgraphBuilder` [ИСТОЧНИК 3]) указывает на современный, надежный подход к сохранению информации о типах в JVM без рефлексии.

#### 3. Четкое разделение ответственности (Separation of Concerns)
*   **Преимущество**: Упрощает разработку, тестирование и повторное использование кода.
*   **Как это работает**:
    *   Узел **не знает** о контексте всего агента, он работает только с `AIAgentGraphContextBase` (или подобным) и своими входными данными.
    *   Логика **интеграции с LLM** (`llm.writeSession { ... }` [ИСТОЧНИК 1]) и **работа с результатами инструментов** (`ReceivedToolResult` [ИСТОХНИК 1]) инкапсулирована внутри конкретных узлов.
    *   `ResponseProcessor?` в конструкторе подграфа [ИСТОЧНИК 3] позволяет выделить пост-обработку ответов в отдельный компонент.

#### 4. Гибкость конфигурации и стратегий
*   **Преимущество**: Позволяет адаптировать поведение агента под разные задачи без изменения кода узлов.
*   **Как это работает**: Поведение узлов настраивается через параметры:
    *   **Стратегии выбора**: `ChoiceSelectionStrategy` [ИСТОчник 1] для выбора из нескольких вариантов LLM, `ToolSelectionStrategy` [ИСТОчник 3] для выбора инструментов.
    *   **Модели и параметры LLM**: `llmModel` и `llmParams` передаются на уровень подграфа [ИСТОчник 2, 3].
    *   **Режимы выполнения**: `runMode: ToolCalls` (последовательный/параллельный) для подграфов с инструментами [ИСТОчник 2].
    *   **Повторение и обработка**: `assistantResponseRepeatMax`, `responseProcessor` [ИСТОчник 2, 3].

#### 5. Встроенная поддержка продвинутых паттернов LLM
*   **Преимущество**: Готовые решения для сложных сценариев взаимодействия с языковыми моделями.
*   **Как это работает**: Фреймворк предоставляет готовые узлы-расширения. Яркий пример — работа с **множественным выбором (multiple choices)**:
    1.  **`nodeLLMSendResultsMultipleChoices`**: Собирает результаты выполнения нескольких инструментов в промпт и запрашивает у LLM несколько вариантов ответа (`requestLLMMultipleChoices()`).
    2.  **`nodeSelectLLMChoice`**: Применяет стратегию (`ChoiceSelectionStrategy`) для выбора одного из этих вариантов и включает выбранный ответ обратно в историю промпта.
    *Это позволяет реализовать, например, "размышление" или "само-исправление", когда агент генерирует несколько мыслей, а затем выбирает лучшую.* [ИСТОчник 1]

#### 6. Безопасность и управление состоянием через подграфы (Subgraphs)
*   **Преимущество**: Позволяет создавать изолированные, многократно используемые модули с собственным жизненным циклом, возможно, с собственной LLM.
*   **Как это работает**: `subgraphWithTask` [ИСТОчник 2] и `AIAgentSubgraphBuilder` [ИСТОчник 3] позволяют инкапсулировать группу узлов в отдельный подграф. У подграфа есть:
    *   Собственные `StartNode` и `FinishNode` [ИСТОчник 4].
    *   Собственные стратегии выбора инструментов и настройки LLM.
    *   Специальный `FinishTool` для возврата результата в тип `Output` [ИСТОчник 2].
    *   Это означает, что сложную логику можно собрать один раз, протестировать и затем использовать как черный ящик в других графах.

#### 7. Простота трансформации данных
*   **Преимущество**: Легко адаптировать выход одного узла под вход другого.
*   **Как это работает**: Метод `transform` у `AIAgentNodeDelegate` [ИСТОчник 5] позволяет создать новый делегат, который применяет пользовательскую функцию-преобразователь к выходу исходного узла. Это элегантный способ решения проблем несовместимости типов без создания дополнительных промежуточных узлов с одноэлементной логикой.

### Пример использования из контекста (гипотетический сценарий)
Допустим, нужно создать агента, который:
1.  Анализирует запрос пользователя.
2.  Определяет, нужны ли ему внешние данные (инструменты).
3.  Запрашивает у LLM несколько гипотез о том, какие данные нужны.
4.  Выбирает одну лучшую гипотезу.
5.  Получает данные по выбранному плану.
6.  Формирует финальный ответ.

Использование `AIAgentNode` позволит:
*   Создать узел для первоначального запроса к LLM.
*   Использовать `nodeLLMSendResultsMultipleChoices` для запроса нескольких гипотез ( pretend инструменты уже были "воображаемо" вызваны).
*   Использовать `nodeSelectLLMChoice` с стратегией "первый/лучший по оценке" для выбора гипотезы.
*   Передать выбранный план в реальный узел вызова инструментов.
*   Собрать все это в подграф с четким входом (запрос пользователя) и выходом (финальный ответ).

### Заключение
`AIAgentNode` является краеугольным камнем архитектуры **AI Koog Agents**, предлагая **типобезопасную, композируемую и стратегически гибкую** модель построения LLM-приложений. Его главные преимущества — это способность инкапсулировать любую логику (от простого вызова API до многоэтапного "мышления" LLM), четкие контракты ввода/вывода и rich ecosystem готовых расширений для сложных паттернов. Это Move Away от monolithic скриптов в сторону инженерных, поддерживаемых workflow-систем.

---
### Источники
[ИСТОЧНИК 1] AIAgentNodesWithChoiceExt.kt · nodeLLMSendResultsMultipleChoices, nodeSelectLLMChoice · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/llm/choice/AIAgentNodesWithChoiceExt.kt
[ИСТОЧНИК 2] AIAgentSubgraphExt.kt · subgraphWithTask · line 197 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/AIAgentSubgraphExt.kt
[ИСТОЧНИК 3] AIAgentSubgraphBuilder.kt · AIAgentSubgraphBuilder, constructor · line 194 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/builder/AIAgentSubgraphBuilder.kt
[ИСТОЧНИК 4] AIAgentSubgraphBuilder.kt · getValue (subgraph creation) · line 292 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/builder/AIAgentSubgraphBuilder.kt
[ИСТОЧНИК 5] AIAgentNodeDelegate.kt · transform · line 103 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/builder/AIAgentNodeDelegate.kt

## Q23: "как использовать AIAgentState"
AIAgentState — это запечатанный интерфейс (sealed interface) в Kotlin, который моделирует различные состояния, которые может принимать AI-агент в течение своего жизненного цикла [ИСТОЧНИК 3]. Он обеспечивает типизированное представление этапов выполнения агента, от начала до завершения или сбоя. Интерфейс определяет абстрактный метод `copy()`, позволяющий создавать копии состояния, что полезно для иммутабельности и контроля над изменениями [ИСТОЧНИК 3].

### Подклассы AIAgentState и их назначение
Интерфейс имеет пять конкретных подклассов, каждый из которых отражает определённый этап:

1. **`NotStarted<Output>`** — состояние, указывающее, что агент ещё не начал выполнение. Используется как начальная точка. Метод `copy()` возвращает новый экземпляр `NotStarted` [ИСТОЧНИК 3].
2. **`Starting<Output>`** — состояние старта, когда агент инициализируется, но ещё не выполняет основные задачи. Метод `copy()` возвращает новый экземпляр `Starting` [ИСТОЧНИК 3].
3. **`Running<Output>`** — состояние активного выполнения. Содержит свойство `rootContext` типа `AIAgentContext`, которое предоставляет доступ к контексту агента, включая управление состоянием (`stateManager`), хранилище (`storage`), информацию о стратегии (`strategyName`) и другие ресурсы [ИСТОЧНИК 4]. Это свойство помечено аннотацией `@InternalAgentsApi`, указывая на внутреннее использование. Метод `copy()` создаёт копию с тем же `rootContext` [ИСТОЧНИК 4].
4. **`Finished<Output>`** — конечное состояние успешного завершения. Содержит свойство `result` типа `Output`, где сохранён результат работы агента. Метод `copy()` возвращает новый экземпляр с тем же `result` [ИСТОЧНИК 4].
5. **`Failed<Output>`** — состояние сбоя. Содержит свойство `exception` типа `Throwable`, описывающее причину ошибки. Метод `copy()` возвращает новый экземпляр с тем же `exception` [ИСТОЧНИК 4].

### Как использовать AIAgentState
На практике AIAgentState, скорее всего, управляется внутренне библиотекой или фреймворком (например, классом `AIAgent`, который не представлен в контексте). Однако на основе предоставленной информации можно описать типичные сценарии взаимодействия:

1. **Проверка текущего состояния**: Если у вас есть экземпляр `state: AIAgentState<*>`, используйте оператор `when` или проверку типов (`is`) для обработки каждого подкласса. Например:
   ```kotlin
   when (state) {
       is AIAgentState.NotStarted -> println("Агент не запущен")
       is AIAgentState.Starting -> println("Агент инициализируется")
       is AIAgentState.Running -> {
           // Доступ к контексту для взаимодействия с агентом
           val context = state.rootContext
           println("Агент выполняется с контекстом: ${context.strategyName}")
       }
       is AIAgentState.Finished -> println("Результат: ${state.result}")
       is AIAgentState.Failed -> println("Ошибка: ${state.exception.message}")
   }
   ```

2. **Доступ к данным состояния**:
   - В состоянии `Running` можно получить `rootContext` (`AIAgentContext`), который предоставляет доступ к управлению состоянием (`stateManager`), хранилищу (`storage`), идентификатору сессии (`runId`) и другим свойствам [ИСТОЧНИК 4] [ИСТОЧНИК 5]. Например, `state.rootContext.stateManager` может использоваться для отслеживания итераций или ограничений выполнения.
   - В состояниях `Finished` и `Failed` извлекайте `result` или `exception` для обработки итога работы.

3. **Создание копий состояния**: Метод `copy()` позволяет создавать immutable-копии, что полезно при передаче состояния между компонентами или сохранении истории изменений.

4. **Связь с контекстом**: Состояние `Running` напрямую содержит `rootContext`, что позволяет во время выполнения получать доступ к среде агента. Контекст (`AIAgentContext`) включает такие элементы, как `stateManager` для управления изменениями состояния, `storage` для хранения данных, `strategyName` для идентификации стратегии и `executionInfo` для наблюдаемости [ИСТОЧНИК 5].

### Дополнительные соображения
- **GoapAgentState**: Это абстрактный класс, специфичный для GOAP-агентов (Goal-Oriented Action Planning). Он содержит `agentInput` и абстрактный метод `provideOutput()` для возврата результата [ИСТОЧНИК 1]. Возможно, он интегрируется с `AIAgentState` (например, `GoapAgentState` может использовать `AIAgentState` для внутреннего отслеживания), но в предоставленном контексте прямая связь не показана.
- **Управление состоянием**: В `AIAgentContext` присутствует `stateManager: AIAgentStateManager?`, который, вероятно, координирует переходы между состояниями, но детали его использования (например, методы для изменения состояния) не приведены в контексте [ИСТОЧНИК 2] [ИСТОЧНИК 5].
- **Контекстные ограничения**: Предоставленные фрагменты не раскрывают, как именно создаются или изменяются экземпляры `AIAgentState` (например, нет API в `AIAgentContext` для получения текущего состояния). Также отсутствует информация о классах-исполнителях (например, `AIAgent`), которые бы демонстрировали полный цикл использования.

### Источники
[ИСТОЧНИК 1] GoapAgentState.kt · класс GoapAgentState · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/planner/goap/GoapAgentState.kt  
[ИСТОЧНИК 3] AIAgentState.kt · sealed interface AIAgentState · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentState.kt  
[ИСТОЧНИК 4] AIAgentState.kt · классы Running, Finished, Failed · line 41 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentState.kt  
[ИСТОЧНИК 5] AIAgentContext.kt · свойства stateManager, storage и другие · line 80 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentContext.kt

**Примечание**: Контекст частичный. Не хватает информации о:
- Классе `AIAgent` или эквивалентном, который бы управлял жизненным циклом и предоставлял текущее состояние.
- Детальном API `AIAgentStateManager` для переключения состояний.
- Примеров интеграции `GoapAgentState` с `AIAgentState`.

Уточните, пожалуйста: **Как в вашем проекте создаётся и обновляется экземпляр `AIAgentState`? Есть ли доступный метод в `AIAgentContext` для получения текущего состояния агента?**

## Q24: "зачем нужен AIAgentStorage"
На основе предоставленного контекста невозможно дать полный и точный ответ о назначении `AIAgentStorage`, поскольку в контексте присутствует только его публичный API (`AIAgentStorageAPI`), но отсутствует сам класс `AIAgentStorage` и пояснения к его роли в системе.

Из доступного контекста можно сделать следующий вывод:

*   Существует интерфейс `AIAgentStorageAPI` [ИСТОЧНИК 4], который определяет контракт для управления хранилищем данных агента.
*   Этот API предоставляет базовые операции CRUD (создание, чтение, удаление) над ключо-значениями: `set`, `get`, `getValue`, `remove`, а также преобразование в карту `toMap()`.
*   Ключами в хранилище являются объекты типа `AIAgentStorageKey<T>`.
*   Вероятно, класс `AIAgentStorage` является конкретной реализацией этого интерфейса, но его код, ответственность и интеграция с другими компонентами (например, с `AIAgentState` или `ToolRegistry`) в предоставленном контексте не показаны.

**Почему требуется уточнение:**
1.  Неизвестно, является ли `AIAgentStorage` единой точкой доступа к состоянию агента, отдельным модулем для хранения артефактов выполнения, или чем-то иным.
2.  Неясно, как именно он связан с жизненным циклом агента (`AIAgentState` [ИСТОЧНИК 2]) или с реестром инструментов (`ToolRegistry` [ИСТОЧНИК 5]).
3.  Нет информации о том, хранятся ли в нём результаты вызовов инструментов, промежуточные данные планировщика (GOAP-агента, см. `GoapAgentState` [ИСТОЧНИК 1]), конфигурация или что-то ещё.
4.  Неизвестны характеристики реализации (in-memory, персистентное, распределённое и т.д.).

**Не знаю. Уточните:**
Можете ли вы предоставить код класса `AIAgentStorage` или описать, в каком конкретно контексте (например, при выполнении какого типа агентов или задач) используется `AIAgentStorage`?

## Q25: "как работает и зона ответственности AIAgentStrategy"
На основе предоставленного контекста могу описать только часть информации о `AIAgentStrategy`.

### Зона ответственности и принцип работы

`AIAgentStrategy` — это компонент, который определяет **alogic обработки входных данных и производства выходных результатов** в рамках выполнения AI агента [ИСТОЧНИК 3]. Она инкапсулирует конкретную стратегию или алгоритм агента.

**Ключевые аспекты:**

1. **Интеграция в жизненный цикл агента:** Стратегия передаётся в `AIAgentRunSessionImpl` (сессию выполнения агента) и используется для непосредственного выполнения логики агента [ИСТОЧНИК 3].
2. **Выполнение:** В процессе выполнения (`run`), когда агент переходит в состояние `Running`, стратегия запускается через свой метод `execute`, который принимает `context` (контекст выполнения) [ИСТОЧНИК 3].
3. **Определение поведения:** Стратегия определяет, как именно агент будет обрабатывать данные, какие шаги выполнять, как взаимодействовать с LLM, инструментами и т.д. Однако конкретные методы и интерфейс `AIAgentStrategy` в предоставленных файлах не раскрываются.

**Пример использования кода (из [ИСТОЧНИК 3]):**
```kotlin
val result = context.with(partName = strategy.name) { ... ->
    runCatchingCancellable {
        state = AIAgentState.Running(context.parentContext ?: context)
        context.pipeline.onStrategyStarting(...)
        val result = strategy.execute(context = ...) // <-- Здесь вызывается стратегия
        // ...
    }
}
```

**Важное ограничение:** В предоставленном контексте **отсутствует полное определение класса `AIAgentStrategy`** — его интерфейс, абстрактные методы или конкретные реализации. Известно только, что он параметризован типами `Input`, `Output` и `TContext`, и имеет метод `execute`.

### Неполнота контекста

Информация является частичной, так как:
- Не показан исходный код интерфейса/класса `AIAgentStrategy`.
- Неизвестны его ключевые методы (кроме `execute`), контракты, особенности реализации.
- Не ясно, как именно разные типы стратегий (например, циклические, рекурсивные, с критикой) меняют поведение агента.
- Нет примеров конкретных реализаций стратегий в рамках фреймворка.

### Уточняющий вопрос

Для полного ответа необходим исходный код `AIAgentStrategy`. **Можете ли вы предоставить определение класса `AIAgentStrategy` (интерфейс или абстрактный класс) из файлов кодовой базы?** Это позволит детально описать его API, обязательные методы и общую архитектуру.

### Источники
[ИСТОЧНИК 3] AIAgentRunSessionImpl.kt · handles · line 33 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentRunSessionImpl.kt
[ИСТОЧНИК 3] AIAgentRunSessionImpl.kt · handles · line 116 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentRunSessionImpl.kt

## Q26: "как работает и зона ответственности AIAgentSubgraph с примерами"
## Обзор и зона ответственности `AIAgentSubgraph`

`AIAgentSubgraph` представляет собой **инкапсулированный подграф (subgraph) внутри графа агента (AI agent graph)**, предназначенный для выполнения одной конкретной, типизированной задачи [ИСТОЧНИК 4]. Это фундаментальный строительный блок, который позволяет декомпозировать сложный workflow агента на логически независимые, изолированные компоненты.

### Ключевые характеристики и зона ответственности:

1.  **Инкапсуляция одной задачи**: Каждый `AIAgentSubgraph` предназначен для решения одной чётко определённой задачи, которая определено лямбдой `defineTask` и имеет строго типизированный результат (тип `Output`) [ИСТОЧНИК 2].
2.  **Структурная целостность**: Подграф имеет обязательные, фиксированные точки входа и выхода — `StartNode` и `FinishNode` [ИСТОЧНИК 4]. Вся внутренняя логика (последовательность вызовов LLM, использование инструментов) скрыта между ними.
3.  **Изолированность конфигурации**: Подграф может иметь собственную конфигурацию LLM (`llmModel`, `llmParams`) и процессор ответов (`responseProcessor`), которые переопределяют настройки родительского графа [ИСТООЧНИК 2, 4].
4.  **Управление завершением**: Завершение работы подграфа и формирование финального результата `Output` жестко контролируется специальным внутренним инструментом (tool) с именем `FINALIZE_SUBGRAPH_TOOL_NAME` [ИСТОЧНИК 1].

---

## Как это работает (на примере)

Основной API для создания подграфа — extension-функция `subgraphWithTask` из объекта `SubgraphWithTaskUtils` [ИСТОЧНИК 2]. Вот как выглядит типичное использование в DSL:

### Пример: Подграф для получения погоды в городе

```kotlin
// 1. Создаётся подграф через DSL внутри основного графа агента
val getWeatherSubgraph = subgraphWithTask<String, WeatherData>(
    name = "get_weather",
    toolSelectionStrategy = ToolSelectionStrategy.Exclusive(WeatherTool), // Только инструмент погоды
    llmModel = LLModel.GPT4 // Используем конкретную модель
) { cityName: String ->
    // 2. defineTask: Здесь описываетсятекстовая задача для LLM.
    // Это то, что "видит" модель. Она должна понять, что для выполнения запроса
    // нужно вызвать инструмент WeatherTool.
    "Find current weather in $cityName and provide the final result."
}
```

**Как происходит выполнение (`handleRun`):**

1.  **Запуск**: Внешний граф передаёт входные данные (`cityName: "Paris"`) в `nodeStart` этого подграфа [ИСТОЧНИК 4].
2.  **Выполнение задачи**:
    *   Данные попадают во внутренний контекст подграфа.
    *   LLM (настроенная для этого подграфа) получает системный промпт, сформированный из `defineTask` ("Find current weather in Paris...").
    *   Стратегия `toolSelectionStrategy` ограничивает доступные инструменты только `WeatherTool`.
    *   LLM генерирует запрос на вызов инструмента.
    *   Инструмент `WeatherTool` выполняется, получает данные (например, `temperature: 18°C, condition: "Cloudy"`).
    *   Результат инструмента возвращается LLM в формате `toolResult`.
3.  **Завершение и финализация**:
    *   На этом этапе LLM должна понять, что задача выполнена. Фреймворк помогает ей, предоставляя в список доступных инструментов **специальный служебный инструмент** `FINALIZE_SUBGRAPH_TOOL_NAME` (`"finalize_task_result"`) [ИСТОЧНИК 1].
    *   Когда LLM вызывает этот инструмент, она передаёт в него итоговый структурированный результат (например, экземпляр `WeatherData`).
    *   Этот вызов **сигнализирует фреймворку, что подграф завершён**.
4.  **Возврат результата**: Данные, переданные в `FINALIZE_SUBGRAPH_TOOL_NAME`, проходят через завершающий узел (`FinishNode`) и возвращаются как `TOutput` (тип `WeatherData`) из всего подграфа [ИСТОЧНИК 4].

---

### Почему это нужно?

*   **Разделение ответственности**: Главный граф агента может содержать много таких подграфов, каждый отвечает за свою микрозадачу (поиск, вычисление, проверка, форматирование).
*   **Безопасность типов**: Вся цепочка от входа `Input` до выхода `Output` строго типизирована на уровне Kotlin.
*   **Повторное использование**: Один и тот же подграф (например, `parseUserIntent`) можно использовать в разных местах основного графа.
*   **Контроль над LLM**: Возможность задавать отдельную модель и параметры для критически важных подзадач.

### Технические детали реализации:

*   **Построитель (Builder)**: Конкретный класс-реализация строится через `AIAgentSubgraphBuilder`, который наследуется от `AIAgentSubgraphBuilderBase` [ИСТОЧНИК 5]. В нём определяется `nodeStart` и `nodeFinish`.
*   **Делегат**: Функция `subgraphWithTask` возвращает объект типа `AIAgentSubgraphDelegate<Input, Output>`, который является "адаптером" и позволяет встроить подграф в родительский контекст [ИСТОЧНИК 2].
*   **Режимы выполнения**: Поддерживается разные `runMode` для инструментов (например, `SEQUENTIAL` или `PARALLEL`), что позволяет контролировать, вызывать ли несколько инструментов подряд или одновременно [ИСТОЧНИК 3].

---

### Резюме

**`AIAgentSubgraph` — это "черный ящик" с одним входом и одним выходом, который решает одну задачу с помощью LLM и инструментов.** Его зона ответственности — изолированное, управляемое и типизированное выполнение подзадачи в рамках большего workflow агента. Внешний мир видит только `StartNode` и `FinishNode`, вся внутренняя магия (многократные вызовы LLM, обработка инструментов, циклы) происходит внутри и контролируется фреймворком до тех пор, пока не будет вызван служебный инструмент `finalize_task_result`.

## Источники
[ИСТОЧНИК 1] AIAgentSubgraphExt.kt · FINALIZE_SUBGRAPH_TOOL_NAME · line 31
[ИСТОЧНИК 2] AIAgentSubgraphExt.kt · subgraphWithTask · line 165
[ИСТОЧНИК 3] AIAgentSubgraphExt.kt · setupSubgraphWithTask (Deprecated) · line 547
[ИСТОЧНИК 4] AIAgentSubgraph.kt · AIAgentSubgraphBase class definition · line 33
[ИСТОЧНИК 5] AIAgentSubgraphBuilder.kt · AIAgentSubgraphBuilderBase · line 37

## Q27: "расскажи все про ExecutionPointNode"
### ExecutionPointNode: Подробное описание

**ExecutionPointNode** — это интерфейс в Koog Agents Core, который определяет контракт для управления и контроля точек выполнения в графе стратегии AI-агента [ИСТОЧНИК 4]. Его основная цель — предоставить возможность явного переопределения стандартного потока выполнения, принудительно направляя агента на выполнение конкретного узла с заданными входными данными.

#### Ключевые компоненты

1.  **Интерфейс `ExecutionPointNode`**:
    *   Предназначен для сущностей (вероятно, самого агента или его менеджера выполнения), которые могут "запоминать" и сбрасывать принудительную точку выполнения.
    *   **`getExecutionPoint(): ExecutionPoint?`** — возвращает текущую активную точку выполнения, если она установлена. Возвращает `null`, если принудительное выполнение не задано [ИСТОЧНИК 4].
    *   **`resetExecutionPoint()`** — сбрасывает/очищает любую установленную принудительную точку выполнения. После вызова этого метода система должна вернуться к своему стандартному поведению (например, определению следующего узла для выполнения на основе состояния и логики агента) [ИСТОЧНИК 4].
    *   **`enforceExecutionPoint(node: AIAgentNodeBase<*, *>, input: Any? = null)`** — устанавливает (принуждает) конкретный узел `node` из графа агента для выполнения. Может дополнительно передать ему входной параметр `input` [ИСТОЧНИК 4]. Это ключевой метод для перехвата и управления потоком.

2.  **Класс данных `ExecutionPoint`**:
    *   Простой контейнер (value class/data class), который инкапсулирует информацию о точке выполнения [ИСТОЧНИК 4].
    *   **`node: AIAgentNodeBase<*, *>`** — узел графа агента, который должен быть выполнен. Тип `AIAgentNodeBase` является общим базовым классом для всех узлов в системе.
    *   **`input: Any? = null`** — необязательные входные данные, которые будут переданы в узел `node` при его выполнении [ИСТОЧНИК 4].

#### Контекст использования и значение

*   **Управление потоком выполнения**: Интерфейс позволяет внешнему коду (например, тестовому окружению, интерфейсу отладки или механизму обработки ошибок) вмешиваться в нормальный цикл выполнения агента, заставляя его выполнить определенный шаг.
*   **Тестирование и отладка**: Позволяет "перепрыгнуть" к конкретному узлу в графе для проверки его логики в изоляции или воспроизведения конкретного сценария.
*   **Обработка исключительных ситуаций**: Может использоваться для принудительного перехода к узлу восстановления (recovery node) или узлу, solicitum'ающего вмешательство человека, при возникновении ошибки.
*   **Семантика**: Класс выполняется в пакете `ai.koog.agents.core.agent.entity`, что указывает на его фундаментальную роль в моделировании сущностей агента.

#### Важные замечания

*   В предоставленном контексте **отсутствует информация о конкретной реализации** этого интерфейса. Неизвестно, какой класс (например, `Agent` или `AgentExecutor`) его реализует и как именно интегрирует механизм принудительных точек в основной цикл выполнения.
*   Неясно, как `enforceExecutionPoint` взаимодействует с событиями `NodeExecutionStartingEvent` и `NodeExecutionCompletedEvent` из `nodeExecutionEvents.kt`. Вероятно, принудительное выполнение также генерирует эти события, но это требует проверки в коде исполнителя.
*   Отношение к `ToolArgs` [ИСТОЧНИК 5] косвенное: входной параметр `input` в `ExecutionPoint` имеет тип `Any?`, что позволяет передавать любой объект, включая, возможно, сериализованные аргументы инструментов, но прямой связи нет.

### Заключение

`ExecutionPointNode` предоставляет **механизм для приоритетного управления потоком выполнения** в графе AI-агента, позволяя явно задать, какой узел и с какими данными должен быть выполнен следующим, временно отменяя стандартную логику планировщика. Его основное применение — сценарии, требующие точного контроля над выполнением, такие как тестирование, отладка и управление в runtime.

### Источники
[ИСТОЧНИК 4] ExecutionPointNode.kt · provides · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/ExecutionPointNode.kt

## Q28: "расскажи все про SubgraphMetadata"
SubgraphMetadata — это data class в Kotlin, предназначенный для хранения и предоставления метаданных о подграфе (subgraph) в графе стратегии AI-агента [ИСТОЧНИК 1]. Он играет ключевую роль в управлении структурой подграфов, обеспечивая информацию о составе узлов и уникальности их имён.

### Структура и свойства
Класс включает два основных свойства:
1. **`nodesMap: Map<String, AIAgentNodeBase<*, *>>`** — карта, где ключами являются имена узлов (строки), а значениями — соответствующие реализации `AIAgentNodeBase`. Эта карта представляет структурный состав подграфа, то есть перечень всех узлов, входящих в него, и их идентификаторы [ИСТОЧНИК 1].
2. **`uniqueNames: Boolean = false`** — флаг, указывающий, уникальны ли имена узлов внутри `nodesMap`. Если значение `true`, это гарантирует, что все имена в подграфе различны, что полезно для предотвращения конфликтов при операциях с подграфами, таких как построение или валидация графа [ИСТОЧНИК 1].

Как data class, `SubgraphMetadata` автоматически получает стандартные методы `equals()`, `hashCode()` и `toString()` на основе своих свойств, что упрощает сравнение, хэширование и отладку [ИСТОЧНИК 1].

### Назначение и контекст использования
`SubgraphMetadata` используется для описания метаданных подграфа, вероятно, в процессе создания, анализа или выполнения графа агента. На основе контекста можно предположить следующие сценарии:
- **Структурная валидация**: Флаг `uniqueNames` может применяться для проверки, что в подграфе нет дублирующихся имён узлов, что важно для корректной маршрутизации или обработки.
- **Оптимизация и диагностика**: `nodesMap` предоставляет сводку о компонентах подграфа, что может использоваться для логирования, отладки или оптимизации выполнения (например, при подстановке подграфов в основной граф).
- **Интеграция с подграфами**: Класс тесно связан с концепцией подграфов (например, `AIAgentSubgraphBase` из [ИСТОЧНИК 2] и [ИСТОЧНИК 4]), которые представляют собой группу узлов, объединённых для повторного использования. `SubgraphMetadata` может сопровождать такие подграфы, чтобы описать их внутреннюю структуру.

Однако в предоставленном контексте **нет явных примеров** того, как `SubgraphMetadata` создаётся или передаётся в другие компоненты, такие как `AIAgentGraphPipeline` (который управляет выполнением узлов и подграфов [ИСТОЧНИК 2, 3]) или `SubgraphWithRetry` (который добавляет логику повторных попыток [ИСТОЧНИК 5]). Возможно, он используется на уровне построения графа (например, в builder-функциях) или в механизмах анализа графа, но это требует уточнения.

### Ограничения контекста
Информация о `SubgraphMetadata` в предоставленных фрагментах ограничена только его определением [ИСТОЧНИК 1]. Другие упомянутые классы (например, `SubgraphExecutionEventContext` [ИСТОЧНИК 4]) описывают события выполнения подграфов, но не ссылаются на `SubgraphMetadata` напрямую. Следовательно, **недостаточно контекста** для полного понимания:
- Где и как экземпляры `SubgraphMetadata` создаются (например, в `AIAgentSubgraphBuilder` или при компиляции графа).
- Как он взаимодействует с `AIAgentGraphPipeline` или обработчиками событий.
- Является ли он частью публичного API или внутренней реализации.

Для более полного ответа потребуется дополнительный контекст, особенно касающийся использования `SubgraphMetadata` в построении или исполнении графов агентов.

### Источники
[ИСТОЧНИК 1] SubgraphMetadata.kt · holds · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/SubgraphMetadata.kt

## Q29: "примеры использования AIAgentSubgraphBuilder"
На основе предоставленного контекста **не могу привести примеры использования `AIAgentSubgraphBuilder`**, так как в контексте отсутствуют конкретные реализации или вызовы этого класса. Приведённые данные ограничиваются только следующим:

1.  `AIAgentSubgraphBuilder` описан как абстрактный базовый класс (`AIAgentSubgraphBuilderBase`), который определяет два обязательных абстрактных свойства: `nodeStart` (начальный узел подграфа) и `nodeFinish` (завершающий узел подграфа), а также метод `edge` для создания связей между узлами [ИСТОЧНИК 2].
2.  Функция-расширение `subgraphWithTask` из файла `AIAgentSubgraphExt.kt` (которая, судя по названию, является удобным фабричным методом для создания подграфов с задачей) **не демонстрирует** прямого использования конструктора или методов `AIAgentSubgraphBuilder`. Вместо этого она вызывает другую функцию `subgraph` (не показана в контексте), которая, вероятно, и инкапсулирует логику построения с использованием `AIAgentSubgraphBuilder` [ИСТОЧНИК 1].
3.  Остальные классы (`AIAgentGraphPipeline`, `BaseBuilder`, `ActionBuilderApi`) не содержат информации о практическом применении `AIAgentSubgraphBuilder` в цепочке построения агента.

**Вывод:** Контекст описывает *архитектурную роль* `AIAgentSubgraphBuilder` (как базовый класс для построения подграфов через DSL) и то, что существует высокоуровневая функция `subgraphWithTask` для его создания, но **не содержит конкретных фрагментов кода (например, из тестов или примеров), где этот билдер используется напрямую в DSL-блоке** (например, `subgraph { ... }`).

Для ответа на вопрос "примеры использования" необходимы дополнительные данные: либо сам код вызова `subgraph { ... }` (где реализуется `AIAgentSubgraphBuilder`), либо код производных классов от `AIAgentSubgraphBuilderBase`. В предоставленном контексте таких примеров нет.

---
### Источники
[ИСТОЧНИК 1] AIAgentSubgraphExt.kt · subgraphWithTask · line 286 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/AIAgentSubgraphExt.kt
[ИСТОЧНИК 2] AIAgentSubgraphBuilder.kt · for · line 37 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/builder/AIAgentSubgraphBuilder.kt

