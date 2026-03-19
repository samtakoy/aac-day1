# Runtest: RERANKED_LLM | 2026-03-19 21:45

## Q1: "какие основные возможности агента, какой класс реализует"
На основе предоставленного контекста можно выделить следующие ключевые возможности агента и классы, которые их реализуют:

### Основные возможности агента

1. **Предоставление конкретной функциональности и конфигурационных возможностей** — это основная цель системы фич (features). Фичи добавляют специализированное поведение агенту [ИСТОЧНИК 1].
2. **Управление состоянием и жизненным циклом** — агент поддерживает внутренние состояния (например, `NotStarted`, `Running`, `Finished`, `Failed`) и обеспечивает потокобезопасные операции [ИСТОЧНИК 4, ИСТОЧНИК 5].
3. **Выполнение стратегии обработки ввода** — агент использует `AIAgentStrategy` для определения, как обрабатывать входные данные и производить выходные [ИСТОЧНИК 4].
4. **Интеграция с пайплайном** — агент работает в контексте пайплайна (графического, функционального или планировщика), который определяет поток обработки [ИСТОЧНИК 1, ИСТОЧНИК 2].
5. **Управление несколькими агентами** — сервис позволяет создавать, удалять, запрашивать и завершать несколько агентов [ИСТОЧНИК 3].
6. **Доступ к инструментам (tools)** — агент может использовать реестр инструментов (`ToolRegistry`) для расширения своих возможностей [ИСТОЧНИК 3].

### Классы, реализующие возможности

1. **`AIAgentBase<Input, Output, TContext>`** — абстрактный базовый класс, представляющий **одноразового агента с состоянием**. Он обеспечивает:
   - Уникальный идентификатор `id` (генерируется UUID, если не задан) [ИСТОЧНИК 4].
   - Определение стратегии выполнения через абстрактное свойство `strategy: AIAgentStrategy` [ИСТОЧНИК 4].
   - Пайплайн, с которым агент работает (например, `AIAgentGraphPipeline`, `AIAgentFunctionalPipeline` или `AIAgentPlannerPipeline`) [ИСТОЧНИК 4].
   - Защиту состояний с использованием мьютекса для безопасных переходов [ИСТОЧНИК 4].
   - Это **основной класс-реализация для конкретного агента**, который должен быть расширен для создания конкретных агентов.

2. **`AIAgentFeature<TConfig, TFeatureImpl>`** и его подтипы — интерфейсы для **модулей функциональности** (фич), которые можно установить в пайплайн агента:
   - `AIAgentGraphFeature` — для графических пайплайнов [ИСТОЧНИК 1].
   - `AIAgentFunctionalFeature` — для функциональных пайплайнов [ИСТОЧНИК 2].
   - `AIAgentPlannerFeature` — для пайплайнов планировщика [ИСТОЧНИК 1].
   - Каждая фича предоставляет `key` для идентификации в хранилище и метод `createInitialConfig` для создания конфигурации [ИСТОЧНИК 1].

3. **`AIAgentServiceBase<Input, Output, TAgent>`** — абстрактный класс для **управления агентами**. Он:
   - Хранит управляемые агенты в `managedAgents` (MutableMap) [ИСТОЧНИК 3].
   - Позволяет создавать и запускать агентов через `createAgentAndRun` [ИСТОЧНИК 3].
   - Определяет абстрактный метод `createAgent` для создания конкретных агентов [ИСТОЧНИК 3].
   - Это **центральный компонент для жизненного цикла агентов** (создание, удаление, запрос, завершение).

4. **`AIAgentRunSessionImpl`** — внутренняя реализация **сессии выполнения агента**. Она:
   - Управляет полным циклом выполнения агента: подготовка пайплайна, стратегия, очистка [ИСТОЧНИК 5].
   - Отслеживает состояние выполнения (`NotStarted` → `Starting` → `Running` → `Finished`/`Failed`) [ИСТОЧНИК 5].
   - Обрабатывает ошибки и уведомляет пайплайн на каждом этапе [ИСТОЧНИК 5].
   - Это **компонент, инкапсулирующий выполнение одного запуска агента**.

### Ключевые взаимосвязи
- **`AIAgentBase`** является фундаментальной реализацией агента, которая использует **стратегию** (`AIAgentStrategy`) и **пайплайн** (например, `AIAgentGraphPipeline`).
- **Фичи** (`AIAgentFeature` и подтипы) добавляют специализированную функциональность в пайплайн агента через метод `install`.
- **`AIAgentServiceBase`** управляет жизненным циклом агентов (создаёт, хранит, завершает), используя для выполнения **сессии** (`AIAgentRunSessionImpl`).

### Недостатки контекста
Контекст не содержит деталей о:
- Конкретных реализациях `AIAgentStrategy` (например, как именно стратегия определяет шаги выполнения).
- Полном API `AIAgent` (интерфейс, который расширяет `AIAgentBase`).
- Примеров конкретных фич (например, как выглядит реализация `AIAgentGraphFeature`).

---
### Источники
[ИСТОЧНИК 1] AIAgentFeature.kt · for · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/AIAgentFeature.kt  
[ИСТОЧНИК 2] AIAgentFeature.kt · for · line 40 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/AIAgentFeature.kt  
[ИСТОЧНИК 3] AIAgentService.kt · for · line 226 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentService.kt  
[ИСТОЧНИК 4] AIAgentBase.kt · representing · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentBase.kt  
[ИСТОЧНИК 5] AIAgentRunSessionImpl.kt · handles · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentRunSessionImpl.kt

## Q2: "как конфигурируется агент"
Конфигурация агента в Koog выполняется через два основных подхода: использование `AIAgentServiceBuilderAPI` для создания сервиса или `AIAgentBuilderImpl` для построения конкретного агента. Базовая конфигурация требует задания промпта и модели LLM, после чего можно дополнительно настроить стратегию обработки и установить функциональные особенности (features) через DSL.

### 1. Базовый builder-подход
Конфигурация начинается с построения через `AIAgent.builder()` (или аналогичного метода), где обязательными параметрами являются:
- **Промпт** (`prompt`) — определяет входную структуру для модели, включая сообщения и параметры [ИСТОЧНИК 5].
- **Модель LLM** (`llmModel`) —-selects конкретную языковую модель и её характеристики [ИСТОЧНИК 3].

Пример базовой конфигурации:
```kotlin
val agent = AIAgent.builder()
    .prompt(myPrompt)
    .llmModel(OpenAIModel("gpt-4"))
    .build()
```

### 2. Конфигурация через AIAgentServiceBuilderAPI
`AIAgentServiceBuilderAPI` предоставляет более высокоуровневый интерфейс для настройки параметров агента [ИСТОЧНИК 3]:
- `promptExecutor` — исполнитель промптов.
- `llmModel` — модель языкового процессора.
- `toolRegistry` — реестр инструментов.
- `systemPrompt` — системный промпт.
- `maxIterations` — ограничение на количество итераций.
- `numberOfChoices` — количество генерируемых вариантов.

Метод `agentConfig()` применяет готовый объект `AIAgentConfig` [ИСТОЧНИК 3].

### 3. Настройка стратегии обработки
После базовой конфигурации выбирается стратегия обработки:
- **Графовая стратегия** (`graphStrategy`) — для сложных workflow на основе графа, возвращает `GraphAgentServiceBuilder` [ИСТОЧНИК 3].
- **Функциональная стратегия** (`functionalStrategy`) — для простых цепочек, возвращает `FunctionalAgentServiceBuilder` [ИСТОЧНИК 3].

Пример:
```kotlin
val builder = AIAgent.builder()
    .prompt(...)
    .llmModel(...)
    .graphStrategy(myGraphStrategy)
```

### 4. Установка и конфигурация фич (features)
Фичи устанавливаются через метод `install()` в DSL, доступный в строителе [ИСТОЧНИК 2]:
```kotlin
install(SomeFeature) {
    // Конфигурация свойств фичи
    someProperty = "value"
    
    // Добавление процессоров сообщений (для некоторых фич)
    addMessageProcessor(myProcessor)
}
```
Каждая фича имеет свой класс конфигурации (`FeatureConfig`), который задаётся в блоке `configure` [ИСТОЧНИК 1]. Конфигурация может включать:
- Параметры фичи (например, `someProperty` [ИСТОЧНИК 2]).
- **Фильтрацию событий** через `setEventFilter` — предикат, определяющий, какие события агента будет обрабатывать фича. Фильтр composable с фильтрацией на уровне процессора сообщений [ИСТОЧНИК 2].
- Для фич, требующих всех событий, фильтрацию можно отключить, переопределив `setEventFilter` в конфигурации [ИСТОЧНИК 2].

### 5. Дополнительные параметры
- `toolRegistry` — регистр инструментов, доступных агенту.
- `clock` — для тестирования (опционально).
- `id` — идентификатор агента.

### 6. Валидация и сборка
Перед `build()` выполняется проверка конфигурации. В `AIAgentBuilderImpl` валидация требует обязательной установки модели (`model`), иначе выбрасывается `IllegalArgumentException` [ИСТОЧНИК 4].

### Ключевые особенности:
- **Модульность** — агент состоит из ядра и настраиваемых фич.
- **Иммутабельность** — после сборки агент неизменяем.
- **Расширяемость** — возможность создания кастомных фич через `AIAgentFeature`.

### Пример полной конфигурации:
```kotlin
val agent = AIAgent.builder()
    .prompt(Prompt { /* ... */ })
    .llmModel(OpenAIModel("gpt-4"))
    .toolRegistry(myTools)
    .maxIterations(10)
    .install(TraceFeature) {
        traceLevel = TraceLevel.DEBUG
    }
    .install(MyCustomFeature) {
        customParam = 123
        setEventFilter { ctx -> ctx.eventType != EventType.SOME_TYPE }
    }
    .graphStrategy(myStrategy)
    .build()
```

### Источники
[ИСТОЧНИК 1] RegisteredFeature.kt · RegisteredFeature · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/pipeline/RegisteredFeature.kt  
[ИСТОЧНИК 2] FEATURES.md · to · line 45 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/FEATURES.md  
[ИСТОЧНИК 3] AIAgentServiceBuilderAPI.kt · AIAgentServiceBuilderAPI · line 75 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentServiceBuilderAPI.kt  
[ИСТОЧНИК 4] AIAgentBuilderImpl.kt · AIAgentBuilderImpl · line 130 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentBuilderImpl.kt  
[ИСТОЧНИК 5] AIAgentConfigBase.kt · for · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/config/AIAgentConfigBase.kt

## Q3: "как агент работает с историей сообщений"
Агент работает с историей сообщений через несколько ключевых механизмов, основанных на предоставленных классах:

## 1. **Хранение истории в контексте агента** [ИСТОЧНИК 1]
Класс `AgentContextData` хранит историю сообщений как часть состояния агента:
- Поле `messageHistory: List<Message>` содержит полную историю диалога
- История сохраняется в неизменяемом виде (`val`), обеспечивая надёжность
- При откате (rollback) агента используется стратегия `RollbackStrategy`:
  - `Default` — полный откат состояния, включая всю историю
  - `MessageHistoryOnly` — откат только истории сообщений

## 2. **Управление историей во время сессии** [ИСТОЧНИК 2] [ИСТОЧНИК 3]
Через `AIAgentLLMWriteSession` агент может динамически модифицировать историю:
- `clearHistory()` — полная очистка истории
- `leaveLastNMessages(n)` — сохранение только последних N сообщений (с опцией сохранения системных сообщений)
- `dropLastNMessages(n)` — удаление последних N сообщений
- `leaveMessagesFromTimestamp(timestamp)` — фильтрация по времени (сообщения старше указанного времени удаляются)
- Все операции сбрасывают промпт агента через `prompt = prompt.withMessages { ... }`

## 3. **Автоматическое сжатие истории** [ИСТОЧНИК 4]
Стратегия `singleRunStrategyWithHistoryCompression` добавляет автоматическое управление размером истории:
- Проверяет, не стала ли история слишком большой через `isHistoryTooBig(Prompt)`
- При превышении порога запускается `compressionStrategy` (алиасирование, извлечение фактов и т.д.)
- Сжатие выполняется после каждого выполнения инструмента
- Опционально можно указать отдельную модель (`retrievalModel`) для компрессии

## 4. **Сохраняемость истории между запусками** [ИСТОЧНИК 5]
Фича `ChatMemory` обеспечивает персистентность истории:
- Через `ChatHistoryProvider` история сохраняется после завершения стратегии
- Перед выполнением стратегии история загружается из хранилища (`chatHistoryProvider.load(runId)`)
- Поддерживаются `preProcessors` для постобработки истории при загрузке/сохранении
- Интегрируется с тремя типами конвейеров: `AIAgentGraphPipeline`, `AIAgentFunctionalPipeline`, `AIAgentPlannerPipeline`

## Ключевые особенности:
- **Неизменяемость**: История хранится как `List<Message>` — неизменяемая коллекция
- **Типизация**: Сообщения имеют строгий тип `Message` (системные, пользовательские, ассистента, инструменты)
- **Метаданные**: Каждое сообщение содержит `metaInfo.timestamp` для операций по времени
- **Рolipback-безопасность**: Стратегии отката гарантируют восстановление истории
- **Контекстная интеграция**: История тесно связана с `prompt` агента через `AIAgentLLMWriteSession`

## Поток работы:
1. Агент загружает историю (если есть персистентное хранилище)
2. При каждом шаге добавляет новые сообщения в историю
3. При превышении размера контекста может автоматически сжимать историю
4. При необходимости может вручную модифицировать историю через расширения
5. Перед откатом восстанавливает историю из `AgentContextData`
6. После завершения сохраняет обновлённую историю (если включено)

**Недостаток контекста**: Не хватает деталей о:
- Точной реализации `HistoryCompressionStrategy`
- Взаимодействии между `AgentContextData` и `AIAgentLLMWriteSession`
- Формате хранения в `ChatHistoryProvider`
- Триггерах для автоматического сжатия (конкретные пороговые значения)

### Источники
[ИСТОЧНИК 1] AgentContextData.kt · AgentContextData · line 1
[ИСТОЧНИК 2] AIAgentLLMActions.kt · AIAgentLLMWriteSession · line 1
[ИСТОЧНИК 3] AIAgentLLMActions.kt · AIAgentLLMWriteSession · line 46
[ИСТОЧНИК 4] SingleRunStrategyWithHistoryCompression.kt · HistoryCompressionConfig · line 32
[ИСТОЧНИК 5] ChatMemory.kt · ChatMemory · line 92

## Q4: "как реализованы стратегии компактизации контекста"
В предоставленном контексте не найдено информации о реализации стратегий компактизации контекста. Контекст документирует компоненты управления жизненным циклом стратегий AI-агентов (интерфейсы `StrategyEventContext`, классы `StrategyStartingContext`/`StrategyCompletedContext`), их устаревшие аналоги (`DeprecatedStrategyEventHandlerContext`) и механизм передачи событий через `AIAgentPipelineImpl`, но не описывает какие-либо алгоритмы, функции или классы, предназначенные для уплотнения, сжатия или оптимизации контекста.

Уточните, пожалуйста: существует ли в проекте отдельный модуль, пакет или классы, ответственные за компактизацию контекста (например, с названиями, содержащими `Compaction`, `Compressor`, `ContextReducer`)? Или, возможно, эта функциональность встроена внутрь конкретных реализаций `AIAgentStrategy` или `AIAgentContext`?

## Q5: "как реализован tool calling"
Исходя из предоставленного контекста, можно описать **частичную** реализацию tool calling — в частности, **систему событий и контекстов**, связанных с жизненным циклом вызовов инструментов (tools). Однако, контекст не включает ключевые компоненты, такие как механизм принятия решений о вызове инструментов, их выполнение или интеграция с LLM. Ниже — детальный разбор того, что видно из кода, и что остаётся непонятным.

---

## Что видно из контекста: система событий и контекстов

### 1. Базовый интерфейс `ToolCallEventContext` [ИСТОЧНИК 1]
Представляет контекст для обработки событий tool call. Все конкретные контексты наследуют от него:
- `eventId` — уникальный идентификатор события/группы событий;
- `executionInfo` — информация о выполнении (родительский элемент, часть выполнения);
- `runId` — идентификатор конкретного запуска/инстанса вызова;
- `toolCallId` — уникальный ID вызова инструмента (может быть `null`);
- `toolName` — имя инструмента;
- `toolDescription` — описание инструмента (может быть `null`);
- `toolArgs` — аргументы вызова в формате `JSONObject`;
- `context` — контекст агента `AIAgentContext`;
- `eventType` — тип события (например, `ToolCallStarting`, `ToolValidationFailed` и т.д.).

---

### 2. Конкретные контексты событий [ИСТОЧНИК 1, ИСТОЧНИК 2]
#### a) `ToolCallStartingContext` [ИСТОЧНИК 1]
Представляет контекст в момент старта вызова инструмента. Содержит только базовые поля, `eventType = AgentLifecycleEventType.ToolCallStarting`.

#### b) `ToolValidationFailedContext` [ИСТОЧНИК 1]
Контекст при ошибке валидации аргументов инструмента. Добавляет:
- `message` — сообщение об ошибке валидации;
- `error` — экземпляр `AIAgentError`, описывающий проблему.

#### c) `ToolCallFailedContext` [ИСТОЧНИК 2]
Контекст при сбое выполнения инструмента. Добавляет:
- `message` — описание сбоя;
- `error` — экземпляр ошибки (может быть `null`).

#### d) `ToolCallCompletedContext` [ИСТОЧНИК 2]
Контекст после успешного завершения вызова. Добавляет:
- `toolResult` — результат выполнения в формате `JSONElement?` (может быть `null`, если результат не применим).

---

### 3. События (events) tool execution [ИСТОЧНИК 3, ИСТОЧНИК 4]
В отдельном файле определены сериализуемые события, которые, видимо, используются для логирования или внутренней передачи данных:

#### `ToolCallStartingEvent` [ИСТОЧНИК 3]
Событие старта вызова инструмента. Поля:
- `eventId`, `executionInfo`, `runId`, `toolCallId`, `toolName`, `toolArgs`, `toolDescription` (в упрощённом конструкторе `null`), `error` (сообщение об ошибке, если есть), `timestamp`.

Есть **устаревший конструктор** без `executionInfo` — для обратной совместимости.

#### `ToolCallFailedEvent` [ИСТОЧНИК 3]
Событие сбоя выполнения инструмента. Поля:
- `error` — объект `AIAgentError` (может быть `null`);
- `timestamp`.

Также есть упрощённый устаревший конструктор.

#### `ToolCallCompletedEvent` [ИСТОЧНИК 4]
Событие успешного завершения. Поля:
- `result` — результат в `JSONElement?`;
- `timestamp`.

Аналогично — упрощённый устаревший конструктор.

---

### 4. Устаревшие (deprecated) typealiases [ИСТОЧНИК 5]
Для поддержки обратной совместимости введены алиасы типов, которые указывают на новые классы:
- `ToolEventHandlerContext` → `ToolCallEventContext`
- `ToolCallContext` → `ToolCallStartingContext`
- `ToolValidationErrorContext` → `ToolValidationFailedContext`
- `ToolCallFailureContext` → `ToolCallFailedContext`
- `ToolCallResultContext` → `ToolCallCompletedContext`

Это говорит о рефакторинге: раньше использовались более общие названия, теперь — более конкретные.

---

## Что НЕ ясно из контекста (недостаточная информация)

1. **Как система решает, какой инструмент вызвать?**  
   Нет информации о том, как LLM запрашивает вызов инструмента, как парсится ответ модели, как определяется соответствие аргументов схеме инструмента.

2. **Где и как хранятся определения инструментов (tools)?**  
   Нет классов, описывающих метаданные инструментов (кроме `toolDescription` в событиях). Не видно структуры регистрации инструментов.

3. **Как происходит валидация аргументов?**  
   Есть событие `ToolValidationFailedContext`, но нет кода валидации — где и как проверяется соответствие `toolArgs` ожидаемой схеме.

4. **Как инструменты физически выполняются?**  
   Нет классов, представляющих сами инструменты (например, `Tool` с методом `execute`). Нет кода, который бы вызывал пользовательские функции на основе `toolName` и `toolArgs`.

5. **Как результаты возвращаются в LLM?**  
   Есть `toolResult` в `ToolCallCompletedContext`, но нет логики, как этот результат формируется в сообщение для модели.

6. **Как связаны `ToolCallEventContext` и `ToolCallStartingEvent`?**  
   Контексты и события, вероятно, используются в разных слоях (обработчики vs. логирование), но связь между ними не раскрыта.

7. **Кто генерирует `runId` и `toolCallId`?**  
   Эти идентификаторы приходят извне, но их происхождение не показано.

---

## Вывод

Предоставленный контекст описывает **только модель данных для событий и контекстов** при вызове инструментов, но **не включает логику работы**:
- принятия решения о вызове,
- валидации аргументов,
- выполнения инструментов,
- возврата результатов в LLM.

Таким образом, нельзя на его основе объяснить **полную реализацию tool calling**. Для этого требуются дополнительные файлы, например:
- классы/интерфейсы для регистрации инструментов (`Tool`, `ToolRegistry`);
- обработчики, которые парсят ответ LLM и формируют `toolCallId`;
- executor, который выполняет инструменты;
- интеграция с AI-агентом (`AIAgent`) или LLM-клиентом.

---

### Уточняющий вопрос
**Не знаю. Уточните:**  
Можете ли вы предоставить код, который отвечает за:
1. Принятие решения о вызове инструмента на основе ответа LLM (парсинг, извлечение `toolName` и `toolArgs`)?
2. Регистрацию и хранение доступных инструментов?
3. Непосредственное выполнение инструментов (как `toolArgs` передаются в пользовательскую функцию и как возвращается `toolResult`)?

## Q6: "как реализован механизм user in the loop"
На основе предоставленного контекста можно проанализировать, какие компоненты **могут быть использованы** для реализации механизма "user in the loop" (пользователь в цикле), однако **прямого и явного упоминания** такого механизма или его конкретной реализации в представленных файлах **не обнаружено**.

Тем не менее, рассмотрим как существующие классы облегчают создание интерактивных агентов, где пользователь может вмешиваться в процесс выполнения:

### 1. `AIAgentFunctionalStrategy` — основа для циклической логики
Этот интерфейс и фабричная функция `functionalStrategy` позволяют определить **кастомную петлю выполнения** agentа, где разработчик сам управляет потоком: сколько шагов, когда запрашивать LLM, когда выполнять инструменты, и **когда привлекать пользователя**[ИСТОЧНИК 1][ИСТОЧНИК 2].  
Функция `func: suspend AIAgentFunctionalContext.(input: Input) -> Output` получает контекст как получателя (`this`), что даёт полный доступ к среде, хранилищу, состоянию и инструментам. Внутри этой функции можно вставить **точки приостановки и ожидания ввода от пользователя**.

### 2. `AIAgentFunctionalContextBase` — инструменты для интеграции пользователя
Контекст предоставляет ключевые возможности:
*   **`environment: AIAgentEnvironment`** — интерфейс для взаимодействия с внешним миром. Скорее всего, через него агент может **отправлять сообщения пользователю** и **получать от него ответы** (например, подтверждения, уточнения, выбор из вариантов)[ИСТОЧНИК 3][ИСТОЧНИК 4].
*   **`agentInput: Any?`** — исходный ввод от пользователя/системы, который можно использовать как отправную точку.
*   **`storage: AIAgentStorage`** — позволяет **сохранять промежуточные результаты или вопрос к пользователю**, чтобы восстановить состояние после паузы.
*   **`stateManager: AIAgentStateManager`** — может использоваться для **отслеживания этапа, на котором ожидается ввод** (например, `AWAITING_USER_CONFIRMATION`).
*   **`getHistory()`** — доступ к истории сообщений, чтобы **показать пользователю контекст** перед его ответом.
*   **`requestLLM(...)`** и другие методы запроса к модели — позволяют **agenту сформулировать вопрос к пользователю**, предварительно обдумав ситуацию.

### Предполагаемый сценарий реализации
Разработчик создал бы стратегию (`functionalStrategy`), в которой после некоторых шагов (например, после планирования или предварительного ответа LLM) агент:
1.  Через `environment` отправляет пользователю **запрос на подтверждение/уточнение**.
2.  **Приостанавливает** свою корутину (например, через `suspendCoroutine` или кастомный `suspend`-метод в `environment`).
3.  Ждёт, пока пользователь (внешний actor, например CLI, WebSocket, API) не вызовет метод, который разблокирует эту корутину и переданный в `environment` ответ запишется в `storage` или обновит `stateManager`.
4.  После возобновления агент использует ответ пользователя для следующего шага.

### Вывод
**Контекст не содержит готовой реализации "user in the loop"**, но предоставляет **все необходимые低级别 строительные блоки** (`AIAgentFunctionalStrategy`, `AIAgentFunctionalContext` с его `environment`, `storage`, `stateManager`). Конкретный механизм (например, как именно передаётся сообщение пользователю и как агент ждёт ответа) зависит от реализации `AIAgentEnvironment` и может быть добавлен разработчиком в тело стратегии.

**Уточняющий вопрос для более точного ответа:**
Есть ли в кодовой базе конкретные реализации `AIAgentEnvironment` (например, `ConsoleEnvironment`, `WebSocketEnvironment`) или примеры стратегий, которые явно демонстрируют ожидание ввода от пользователя (например, через `suspend fun requestUserInput(...)`)?

---
### Источники
[ИСТОЧНИК 1] AIAgentFunctionalStrategy.kt · interface declaration · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentFunctionalStrategy.kt  
[ИСТОЧНИК 2] AIAgentFunctionalStrategy.kt · functionalStrategy function · line 34 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentFunctionalStrategy.kt  
[ИСТОЧНИК 3] AIAgentFunctionalContextBase.kt · class declaration · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentFunctionalContextBase.kt  
[ИСТОЧНИК 4] AIAgentFunctionalContextBase.kt · property list · line 34 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentFunctionalContextBase.kt  
[ИСТОЧНИК 5] AIAgentFunctionalContextBase.kt · method list · line 57 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentFunctionalContextBase.kt

## Q7: "как агенты работают с памятью и  сколько слоев используют в"
На основе предоставленного контекста можно описать работу системы памяти для агентов, но информация об агентах и их архитектурных слоях недостаточна.

### Работа с памятью в системе Koog Agents

В контексте представлен интерфейс `AgentMemoryProvider` [ИСТОЧНИК 1] и его реализация `LocalFileMemoryProvider` [ИСТОЧНИК 4], которые управляют устойчивым хранилищем знаний агента.

**Ключевые принципы организации памяти:**

1. **Структура знаний**:
   - Знания хранятся как `Fact` (факты), связанные с `Concept` (концепции/категории) [ИСТОЧНИК 2].
   - Пример: концепция "build-system" может содержать факт "Gradle 8.0" [ИСТОЧНИК 1].

2. **Контекстная организация**:
   - **Subject (Субъект)**: категория контекста (например, `MemorySubject.Project`, `MemorySubject.Machine`) [ИСТОЧНИК 2].
   - **Scope (Область видимости)**: границы доступа:
     - `MemoryScope.Agent(agentName)` — для конкретного агента,
     - `MemoryScope.Product(productName)` — для продукта,
     - `MemoryScope.Feature(featureId)` — для фичи,
     - `MemoryScope.CrossProduct` — для межпродуктового доступа [ИСТОЧНИК 1][ИСТОЧНИК 3].

3. **Иерархическое хранение** (в `LocalFileMemoryProvider`):
   ```
   root/
     storageDirectory/
       agent/                    # MemoryScope.Agent
         [agent-name]/
           subject/
             [subject-name]/
               facts.json
       feature/                  # MemoryScope.Feature
         [feature-id]/
           subject/
             [subject-name]/
               facts.json
       product/                  # MemoryScope.Product
         [product-name]/
           subject/
             [subject-name]/
               facts.json
       organization/             # MemoryScope.CrossProduct
         subject/
           [subject-name]/
             facts.json
   ```
   [ИСТОЧНИК 4]

4. **Операции**:
   - `save(fact, subject, scope)` — атомарное сохранение факта [ИСТОЧНИК 2].
   - `load(concept, subject, scope)` — выборка фактов по концепции [ИСТОЧНИК 2].
   - `loadAll(subject, scope)` — получение всех фактов в контексте [ИСТОЧНИК 2].
   - Поддержка семантического поиска (не fully shown в контексте) [ИСТОХНИК 2].

5. **Безопасность и надёжность**:
   - Потокобезопасность через `Mutex` [ИСТОЧНИК 5].
   - Поддержка шифрования через `Storage`-обёртку [ИСТОЧНИК 4][ИСТОЧНИК 5].
   - Хранение в JSON с "pretty printing" для читаемости [ИСТОЧНИК 4].

**О слоях памяти:**
В предоставленном контексте **не обнаружено информации** об архитектурных слоях памяти (например, краткосрочная/долгосрочная, рабочий контекст vs постоянное хранилище). Единственный "слой" — это абстракция `AgentMemoryProvider`, которая может использоваться различными компонентами агента. Конкретные агенты (классы типа `Agent`, `LLMAgent` и др.) и их внутренняя работа с памятью в контексте не представлены.

### Выводы и ограничения контекста
- Контекст описывает **только систему persistent-памяти** (долговременное хранение фактов), но не оперативную память агента (контекст выполнения, историю сообщений и т.д.).
- Количество "слоев" (если под слоями понимаются уровни абстракции) — минимум два: интерфейс `AgentMemoryProvider` и реализация `LocalFileMemoryProvider`. Возможно, есть и другие реализации (например, сетевые), но они не в контексте.
- **Нет данных** о том, как именно агенты (например, классы `Agent`) *используют* эту память в своём рабочем цикле (принятии решений, планировании и т.д.).

### Уточняющий вопрос
Контекст не содержит информации о самих агентах и их архитектуре памяти. Для ответа на вопрос о "слоях" и работе агентов с памятью требуется дополнительный контекст:
> **Уточните**: Имеются ли в виду **архитектурные слои** (например, краткосрочная vs долгосрочная память) или **уровни абстракции** в кодовой базе? Также, пожалуйста, предоставьте контекст по классам агентов (например, `Agent`, `LLMAgent`) и их взаимодействию с `AgentMemoryProvider`.

### Источники
[ИСТОЧНИК 1] AgentMemoryProvider.kt · for · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/memory/providers/AgentMemoryProvider.kt
[ИСТОЧНИК 2] AgentMemoryProvider.kt · for · line 50 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/memory/providers/AgentMemoryProvider.kt
[ИСТОЧНИК 4] LocalFileMemoryProvider.kt · LocalFileMemoryProvider · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/memory/providers/LocalFileMemoryProvider.kt
[ИСТОЧНИК 5] LocalFileMemoryProvider.kt · LocalFileMemoryProvider · line 45 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/memory/providers/LocalFileMemoryProvider.kt

## Q8: "как агенты работают с mcp"
На основе предоставленного контекста можно описать, как агенты работают с Model Context Protocol (MCP) в рамках Kotlin-фреймворка `koog`. Интеграция MCP осуществляется через специализированный компонент, который преобразует MCP-инструменты во внутренний формат фреймворка, делая их доступными для агентов.

### Основной механизм работы

1.  **Подключение к MCP-серверу и получение инструментов.** Центральным звеном является объект `McpToolRegistryProvider` [ИСТОЧНИК 1]. Его основная задача — "facilitates the integration of MCP tools into the agent framework" [ИСТОЧНИК 1], то есть облегчение интеграции инструментов MCP в фреймворк агентов.
    *   Подключение к серверу осуществляется через различные транспортные механизмы: `SseClientTransport` (Server-Sent Events) или `StdioClientTransport` (стандартный ввод/вывод) [ИСТОЧНИК 2, 3].
    *   После установления соединения с помощью клиента `Client` из SDK MCP вызывается метод `listTools()` для получения списка доступных на сервере инструментов в формате SDK (`List<Tool>`) [ИСТОЧНИК 3].

2.  **Преобразование (парсинг) и регистрация инструментов.** Полученные из MCP SDK инструменты (`sdkTools`) преобразуются во внутренние инструменты фреймворка (`Tool`) с помощью `McpToolDescriptorParser` [ИСТОЧНИК 3].
    *   В процессе создания каждого инструмента также формируется метаданные (словарь `toolMetaData`), которая включает критическую информацию о происхождении инструмента:
        *   `McpMetadataKeys.ToolId`: Имя инструмента с сервера.
        *   `McpMetadataKeys.McpTransportType`: Тип транспорта (TCP для SSE, Stdio) [ИСТОЧНИК 3].
        *   `McpMetadataKeys.ServerUrl` и `McpMetadataKeys.ServerPort`: Адрес сервера (если применимо) [ИСТОЧНИК 3].
    *   Преобразованный инструмент `McpTool` оборачивает оригинальный MCP-клиент (`mcpClient`) для последующих вызовов [ИСТОЧНИК 3]. Все инструменты затем регистрируются в `ToolRegistry` [ИСТОЧНИК 3].

3.  **Предоставление инструментов агенту.** `ToolRegistry`, содержащий MCP-инструменты, передаётся в контекст агента — `AIAgentLLMContext` [ИСТОЧНИК 4].
    *   `AIAgentLLMContext` — это класс, который "Manages tools, prompts, and interactions for an AI agent's language model" [ИСТОЧНИК 4]. Он хранит ссылку на реестр инструментов (`toolRegistry`) [ИСТОЧНИК 4, 5].
    *   Таким образом, агент, работающий через этот контекст, получает полный доступ к списку MCP-инструментов как к своим собственным. Когда агенту нужно выполнить действие, он может выбрать инструмент из `toolRegistry` (включая MCP-инструменты) и вызвать его. Вызов через `McpTool` перенаправляется на соответствующий метод оригинального MCP-клиента, который общается с MCP-сервером.

### Ключевые компоненты и их роли

| Компонент | Роль в работе с MCP |
| :--- | :--- |
| **`McpToolRegistryProvider`** | Фабрика для создания `ToolRegistry` с инструментами MCP. Управляет транспортом, парсингом и регистрацией. |
| **`Client` (из MCP SDK)** | Низкоуровневый клиент для общения с MCP-сервером по протоколу. Инкапсулирован внутри `McpTool`. |
| **`McpToolDescriptorParser`** | Преобразует DTO `Tool` из MCP SDK в `ToolDescriptor` фреймворка. |
| **`ToolRegistry`** | Единый реестр всех доступных агенту инструментов, включая MCP-инструменты. |
| **`AIAgentLLMContext`** | Контекст выполнения агента, содержащий `toolRegistry`. Агент использует этот контекст для доступа к инструментам. |
| **`McpTool`** | Адаптер, реализующий интерфейс `Tool` фреймворка. Делегирует вызов (`invoke`) оригинальному MCP-клиенту. |

### Центральный код

Ключевая точка интеграции — метод `fromClient` в `McpToolRegistryProvider`:
```kotlin
public suspend fun fromClient(
    mcpClient: Client,
    serverInfo: McpServerInfo,
    mcpToolParser: McpToolDescriptorParser = DefaultMcpToolDescriptorParser,
): ToolRegistry {
    val sdkTools = mcpClient.listTools().tools // 1. Получение инструментов от MCP-сервера
    return buildToolRegistry(sdkTools, mcpToolParser, serverInfo, mcpClient) // 2. Преобразование и регистрация
}
```

### Важные замечания

*   **Инкапсуляция MCP:** Для агента и его контекста (`AIAgentLLMContext`) MCP-инструменты — это просто часть общего `ToolRegistry`. Агент не знает о протоколе MCP, он работает с унифицированным интерфейсом `Tool`. Вся специфика MCP (транспорт, вызовы по протоколу) скрыта внутри реализации `McpTool`.
*   **Метаданные:** MCP-инструменты сопровождаются метаданными (`McpMetadataKeys`), что позволяет в дальнейшем анализировать их происхождение, отключать/подключать группы инструментов или собирать статистику.
*   **Жизненный цикл клиента:** MCP-клиент (`Client`) создаётся и управляется внешним кодом (скорее всего, тем, что вызывает `McpToolRegistryProvider.fromClient`). Важно, чтобы клиент оставался активным всё время, пока `McpTool` может быть вызван агентом.

**Ограничение контекста:** Предоставленные файлы показывают только **создание и регистрацию** MCP-инструментов. Контекст не содержит информации о том, **как именно агент (например, `AIAgent`) использует эти инструменты в процессе своего выполнения** (планировании, цикле "мысль-действие"), nor о жизненном цикле самого `Client` (создание, закрытие). Ответ описывает только механизм *предоставления* MCP-инструментов в фреймворк.

---
### Источники
[ИСТОЧНИК 1] McpToolRegistryProvider.kt · facilitates · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-mcp/src/commonMain/kotlin/ai/koog/agents/mcp/McpToolRegistryProvider.kt
[ИСТОЧНИК 2] McpToolRegistryProvider.kt · defaultSseTransport · line 38 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-mcp/src/commonMain/kotlin/ai/koog/agents/mcp/McpToolRegistryProvider.kt
[ИСТОЧНИК 3] McpToolRegistryProvider.kt · fromClient · line 78 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-mcp/src/commonMain/kotlin/ai/koog/agents/mcp/McpToolRegistryProvider.kt
[ИСТОЧНИК 4] AIAgentLLMContext.kt · AIAgentLLMContext · line 31 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentLLMContext.kt
[ИСТОЧНИК 5] AIAgentLLMContext.kt · AIAgentLLMContext · line 59 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentLLMContext.kt

## Q9: "какие возможности есть по выстраиванию пайпланов и разбитие задач на подзадачи"
Основываясь на предоставленном контексте, в кодовой базе реализованы следующие возможности для выстраивания пайплайнов и разбиения задач на подзадачи:

### 1. Построение пайплайнов через итеративный цикл "план-исполнение" (`AIAgentPlanner`)
Класс `AIAgentPlanner` реализует основной исполнительный цикл агента [ИСТОЧНИК 2]:
- **Цикл**: `buildPlan` → `executeStep` → `isPlanCompleted` (повторяется до завершения или достижения лимита итераций) [ИСТОЧНИК 2].
- **Интеграция с пайплайном**: На каждом этапе (построение плана, выполнение шага, оценка завершения) вызываются методы `context.pipeline.on*` [ИСТОЧНИК 3][ИСТОЧНИК 4], что позволяет встраивать пользовательскую логику обработки событий в пайплайн.
- **Управление состоянием**: Используется `stateManager.withStateLock` для безопасного доступа к состоянию (счётчику итераций) [ИСТОЧНИК 3][ИСТОЧНИК 4].
- **Лимиты**: Максимальное число итераций контролируется через `context.config.maxAgentIterations` [ИСТОЧНИК 4].

### 2. Декомпозиция задач на подзадачи через подграфы (`AIAgentSubgraphExt.kt`)
Метод `setupSubgraphWithTask` [ИСТОЧНИК 5] предоставляет механизм для создания изолированных подграфов (подзадач) внутри основного графа агента:
- **Finish Tool**: Каждая подзадача имеет специальный инструмент (`finishTool`), который сигнализирует о завершении подзадачи и преобразует результат [ИСТОЧНИК 5].
- **Динамическое управление инструментами**: При запуске подграфа к инструментам добавляется `finishTool`, а после завершения восстанавливается исходный набор инструментов [ИСТОЧНИК 5].
- **Управление повторами**: Есть счётчик `askAssistantToFinishCounterKey` и ограничение `assistantResponseRepeatMax` для предотвращения бесконечных циклов в подзадаче [ИСТОЧНИК 5].
- **Режимы выполнения**: Параметр `runMode` (например, `ToolCalls.PARALLEL` или `ToolCalls.SEQUENTIAL`) управляет параллельностью/последовательностью вызовов инструментов в подграфе [ИСТОЧНИК 5].

### 3. Создание "кирпичиков" для планов через Action Builder (`ActionBuilderApi`)
Интерфейс `ActionBuilderApi` [ИСТОЧНИК 1] позволяет гибко определять атомарные действия (элементы плана) с):
- **Предусловием** (`precondition`): Условие, которое должно быть выполнено для применения действия [ИСТОЧНИК 1].
- **Верой** (`belief`): Информация, которая считается истинной после выполнения действия [ИСТОЧНИК 1].
- **Стоимостью** (`cost`): Функция оценки "затрат" на действие для алгоритма планировщика (например, GOAP) [ИСТОЧНИК 1].
- **Логикой исполнения** (`execute`): Функция, реализующая эффект действия [ИСТОЧНИК 1].
- **Метаданными**: `name` и `description` [ИСТОЧНИК 1].

### Ключевые концепции интеграции:
1. **Иерархия задач**: Основной пайплайн (`AIAgentPlanner`) может создавать подграфы (`AIAgentSubgraphExt`), что позволяет строить иерархические планы (задача → подзадача → атомарное действие).
2. **Событийная модель**: Пайплайн (`context.pipeline`) оборачивает критические этапы планирования и исполнения, позволяя добавлять логику аудита, валидации или кастомизации без изменения ядра [ИСТОЧНИК 3][ИСТОЧНИК 4].
3. **Безопасность состояний**: Все модификации состояния (итерации, данные подграфа) защищены блокировками через `stateManager.withStateLock` [ИСТОЧНИК 3][ИСТОЧНИК 4].
4. **Ограничения и обработка ошибок**: Лимит итераций (`maxAgentIterations`) защищает от бесконечных циклов, а исключение `AIAgentMaxNumberOfIterationsReachedException` сигнализирует о сбое [ИСТОЧНИК 4].

### Неполнота контекста:
- В контексте отсутствуют конкретные примеры реализации `buildPlan` (например, алгоритм GOAP или LLM-based планировщик).
- Не показана полная структура `Plan` и `State` — типы параметризуются (`<State: Any, Plan: Any>`), но их конкретные реализации не приведены [ИСТОЧНИК 2].
- Нет деталей о работе `context.pipeline` (интерфейсы `onPlanCreationStarting` и т.д.).
- Не описаны типы `Condition<State>`, `Belief<State>`, `Cost<State>`, `Execute<State>` из `ActionBuilderApi` [ИСТОЧНИК 1].

### Вывод:
Пайплайны строятся на основе **итеративного цикла планирования-исполнения** (`AIAgentPlanner`), а декомпозиция задач достигается через **вложенные подграфы с finish-инструментом** (`setupSubgraphWithTask`). "Кирпичики" планов (действия) создаются через `ActionBuilderApi` с явным указанием предусловий, веры и стоимости. Архитектура поддерживает событийную модель, безопасную работу с состоянием и защиту от бесконечных циклов.

### Источники
[ИСТОЧНИК 1] ActionBuilderApi.kt · ActionBuilderApi · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/planner/goap/ActionBuilderApi.kt  
[ИСТОЧНИК 2] AIAgentPlanner.kt · AIAgentPlanner · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/planner/AIAgentPlanner.kt  
[ИСТОЧНИК 3] AIAgentPlanner.kt · AIAgentPlanner · line 42 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/planner/AIAgentPlanner.kt  
[ИСТОЧНИК 4] AIAgentPlanner.kt · AIAgentPlanner · line 90 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/planner/AIAgentPlanner.kt  
[ИСТОЧНИК 5] AIAgentSubgraphExt.kt · providing · line 588 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/AIAgentSubgraphExt.kt

## Q10: "как агенты могу общаться друг с другом"
### Как агенты могут общаться друг с другом

В предоставленном контексте **нет явной документации или реализации прямого общения между агентами**. Однако можно выделить **косвенные механизмы и предполагаемые паттерны**, основанные на архитектуре агентов:

---

#### 1. **Общая среда (Environment) как посредник**
Агенты взаимодействуют с внешним миром (включая другие агенты, если они зарегистрированы как инструменты) через интерфейс `AIAgentEnvironment` [ИСТОЧНИК 3].  
- Каждый агент может выполнять **инструменты (tools)** через `executeTool(toolCall)` [ИСТОЧНИК 3] или `executeTools(toolCalls)` [ИСТОЧНИК 4].  
- Если другой агент представлен в виде **инструмента** (например, с именем `"agent_X"`), вызов этого инструмента будет перенаправлен соответствующему агенту для обработки.  
- Таким образом, общение между агентами может быть **симулировано через вызовы инструментов**, где один агент запрашивает действие у другого, а результат возвращается через `ReceivedToolResult`.

---

#### 2. **Стратегии агентов (Agent Strategies)**
Стратегии, такие как `chatAgentStrategy()` [ИСТОЧНИК 1] и `structured_output_with_tools_strategy()` [ИСТОЧНИК 2], определяют **граф обработки**:  
- Агент получает входные данные, генерирует запрос к LLM, **вызывает инструменты** (включая, потенциально, вызовы других агентов) и формирует ответ.  
- В `chatAgentStrategy()` есть явный переход `nodeCallLLM forwardTo nodeExecuteTool onToolCall { true }` [ИСТОЧНИК 1], который означает:  
  *Если LLM отвечает вызовом инструмента, агент переходит к его выполнению.*  
- Если инструмент связан с другим агентом, это создаёт цепочку: **Агент А → LLM → tool call (к Агенту Б) → результат → LLM → ответ**.

---

#### 3. **Сообщения (Messages) как единый протокол**
Все взаимодействия в контексте представлены через `Message` (например, `Message.Tool.Call` [ИСТОЧНИК 3]). Предполагается:  
- Агент А отправляет `Message.Tool.Call` с именем инструмента, соответствующим Агенту Б.  
- Среда (`AIAgentEnvironment`) маршрутизирует этот вызов, и Агент Б обрабатывает его, возвращая `ReceivedToolResult` [ИСТОЧНИК 3].  
- Это позволяет агентам **обмениваться структурированными данными**, но требует предварительной регистрации агентов как инструментов в общей среде.

---

#### 4. **Ограничения контекста**
- В предоставленных фрагментах **нет кода, явно реализующего сеть агентов** (например, класс `AgentNetwork` или `MultiAgentSystem`).  
- Не описано, как несколько агентов **координируют действия без внешнего оркестратора** (например, через общую память или шину событий).  
- Стратегии (`AIAgentGraphStrategy`) ориентированы на **одиночный агент**, взаимодействующий со средой, а не на **межагентное взаимодействие**.

---

### Выводы
На основе контекста:
1. **Основной механизм** — **инструменты (tools)**, зарегистрированные в общей среде (`AIAgentEnvironment`), где один агент может вызвать другой, как внешний сервис [ИСТОЧНИК 3].  
2. **Протокол общения** — **структурированные сообщения** (`Message.Tool.Call` / `ReceivedToolResult`), обрабатываемые LLM в рамках стратегии агента [ИСТОЧНИК 1].  
3. **Архитектурное ограничение** — контекст не показывает **прямого API для peer-to-peer общения между агентами** без участия LLM или среды. Вероятно, такая функциональность реализована в других модулях (например, в `agents-network`).

---

### Источники
[ИСТОЧНИК 1] AIAgentStrategies.kt · chatAgentStrategy · line 31 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/AIAgentStrategies.kt  
[ИСТОЧНИК 2] AIAgentStrategies.kt · chatAgentStrategy · line 217 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/AIAgentStrategies.kt  
[ИСТОЧНИК 3] AIAgentEnvironment.kt · with · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/environment/AIAgentEnvironment.kt  
[ИСТОЧНИК 4] AIAgentEnvironment.kt · with · line 38 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/environment/AIAgentEnvironment.kt  
[ИСТОЧНИК 5] AIAgentStrategy.kt · representing · line 37 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/AIAgentStrategy.kt

## Q11: "какие возможности по работе с сессиями"
На основе предоставленного контекста можно выделить следующие возможности по работе с сессиями в кодовой базе. Учитывая, что контекст является частичным (извлечены только 3 класса), описание будет основано исключительно на доступных данных.

### Общая архитектура сессий
В кодовой базе явно присутствует разделение на **сессии для чтения** (`AIAgentLLMReadSession`) и **сессии для записи/изменения** (`AIAgentLLMWriteSession`), что следует принципу разделения ответственности. Управление историей сообщений и параметрами сессии вынесено в отдельные расширения (файл `AIAgentLLMActions.kt`).

---

### 1. Возможности сессии для записи (`AIAgentLLMWriteSession`)
Эта сессия, представленная классом `AIAgentLLMWriteSessionImpl` [ИСТОЧНИК 3], позволяет не только отправлять запросы к LLM, но и **модифицировать состояние сессии** (историю сообщений и параметры) перед запросом.

#### 1.1. Управление историей сообщений (через расширения `AIAgentLLMActions`)
Эти функции доступны для `AIAgentLLMWriteSession` и позволяют манипулировать историей диалога [ИСТОЧНИК 1, 2]:
*   **`clearHistory()`** — Полностью очищает историю сообщений в текущей сессии [ИСТОЧНИК 1]. Полезно для начала нового диалога.
*   **`leaveLastNMessages(n: Int, preserveSystemMessages: Boolean = true)`** —Оставляет только последние `n` сообщений, удаляя более ранние [ИСТОЧНИК 1]. Параметр `preserveSystemMessages` гарантирует, что системные сообщения не будут удалены.
*   **`dropLastNMessages(n: Int, preserveSystemMessages: Boolean = true)`** — Удаляет последние `n` сообщений из истории [ИСТОЧНИК 1]. Также с опцией сохранения системных сообщений.
*   **`leaveMessagesFromTimestamp(timestamp: Instant, preserveSystemMessages: Boolean = true)`** — Удаляет все сообщения, timestamp которых раньше указанного [ИСТОЧНИК 2]. Позволяет "обрезать" историю по времени.

#### 1.2. Управление параметрами LLM (Tool Choice)
Чрезylvania_setToolChoice()` и связанные методы позволяют динамически управлять стратегией вызова инструментов (tools) моделью [ИСТОЧНИК 2]:
*   **`setToolChoice(toolChoice: LLMParams.ToolChoice?)`** — Устанавливает конкретный выбор инструмента.
*   **`setToolChoiceAuto()`** — Разрешает модели автоматически решать, вызывать ли инструмент [ИСТОЧНИК 2].
*   **`setToolChoiceRequired()`** — Принуждает модель всегда вызывать какой-либо инструмент [ИСТОЧНИК 2].
*   **`setToolChoiceNone()`** — Запрещает модели вызывать инструменты [ИСТОЧНИК 2].
*   **`setToolChoiceNamed(toolName: String)`** — Принуждает модель вызвать **конкретный** инструмент с именем `toolName` [ИСТОЧНИК 2].

#### 1.3. Основные методы запроса к LLM
Класс `AIAgentLLMWriteSessionImpl` предоставляет методы для взаимодействия с языковой моделью. В контексте доступны упомянутые:
*   **`requestLLMStructured`** — Запрашивает структурированный ответ (десериализуемый в тип `T`), опционально с примерами и исправляющим парсером [ИСТОЧНИК 4]. При успешном выполнении результат автоматически добавляется в историю сессии.
*   **`parseResponseToStructuredResponse`** — Парсит уже полученное сообщение ассистента (`Message.Assistant`) в структурированный ответ [ИСТОЧНИК 4].
*   **`requestLLMMultipleChoices`** — Запрашивает несколько вариантов ответа от модели [ИСТОЧНИК 4].
*   **`requestLLMStreaming(definition: StructureDefinition?)`** — Выполняет запрос в потоковом режиме [ИСТОЧНИК 4]. Если передан `definition`, он добавляется в промпт как пользовательское сообщение. Возвращает `Flow<StreamFrame>`.
*   **`appendPrompt(body: PromptBuilder.() -> Unit)`** (упомянут в Key Methods) — Позволяет дополнить текущий промпт новыми сообщениями через DSL builder. Этот метод явно не показан в извлеченных фрагментах, но указан в описании ответственности класса [ИСТОЧНИК 3] и является стандартным для подобных абстракций.
*   **`toParallelToolCallsImpl` / `toParallelToolCallsRawImpl`** — Внутренние (Internal API) методы для выполнения потока аргументов инструментов параллельно с заданной степенью конкурентности [ИСТОЧНИК 4].

---

### 2. Возможности сессии только для чтения (`AIAgentLLMReadSession`)
Экспере class `AIAgentLLMReadSession` [ИСТОЧНИК 5] предназначена для **только чтения** состояния (промпта) и выполнения запросов к LLM, **без возможности модифицировать** историю или параметры через этот интерфейс. Это более строгий и безопасный контракт.

В контексте упомянуты следующие ключевые методы (их реализация, вероятно, делегируется `AIAgentLLMWriteSessionImpl`):
*   **`requestLLMWithoutTools()`** — Запрос к модели без доступных инструментов.
*   **`requestLLMMultipleWithoutTools()`** — Запрос нескольких вариантов без инструментов.
*   **`requestLLMOnlyCallingTools()`** — Запрос, где модель должна вызвать только инструменты (без генерации текста).
*   **`requestLLMMultipleOnlyCallingTools()`** — Несколько вариантов, только вызов инструментов.
*   **`requestLLMForceOneTool(tool: ToolDescriptor)`** — Принудительный вызов **конкретного** указанного инструмента.

---

### Ключевые отличия и взаимосвязь
1.  **Запись vs Чтение**: `AIAgentLLMWriteSession` позволяет изменять промпт и параметры (`prompt = ...`), а `AIAgentLLMReadSession` — только выполнять запросы на основе текущего состояния.
2.  **Делегирование**: Судя по коду `AIAgentLLMWriteSessionImpl` [ИСТОЧНИК 4], внутренний `readSessionImpl` создается на основе текущего состояния write-сессии. Это означает, что write-сессия может "создать" соответствующую read-сессию для выполнения конкретного запроса, что обеспечивает изоляцию изменений.
3.  **Управление инструментами**: Оба типа сессий управляют доступными инструментами через `toolRegistry`, но `WriteSession` предоставляет больше контроля над *стратегией* их вызова (Tool Choice), а `ReadSession` — над *собственно вызовом* конкретных инструментов.

### Что не охвачено контекстом
*   Точная жизненная цикличность сессий (создание, закрытие).
*   Полный список методов `AIAgentLLMReadSession` (в контексте только именованный список).
*   Детали работы с `PromptExecutor`, `ResponseProcessor` и `StructureFixingParser`.
*   Механизмы потоковой обработки (`StreamFrame`) и структурированных запросов (`StructuredRequestConfig`).
*   Взаимодействие с `AIAgentEnvironment` и `SafeTool`.

### Источники
[ИСТОЧНИК 1] AIAgentLLMActions.kt · clearHistory, leaveLastNMessages, dropLastNMessages · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/extension/AIAgentLLMActions.kt  
[ИСТОЧНИК 2] AIAgentLLMActions.kt · leaveMessagesFromTimestamp, setToolChoice, setToolChoiceAuto, setToolChoiceRequired, setToolChoiceNone, setToolChoiceNamed · line 46 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/extension/AIAgentLLMActions.kt  
[ИСТОЧНИК 3] AIAgentLLMWriteSessionImpl.kt · AIAgentLLMWriteSessionImpl (класс, ответственность) · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/session/AIAgentLLMWriteSessionImpl.kt  
[ИСТОЧНИК 4] AIAgentLLMWriteSessionImpl.kt · requestLLMStructured, parseResponseToStructuredResponse, requestLLMMultipleChoices, requestLLMStreaming, toParallelToolCallsImpl, toParallelToolCallsRawImpl · line 173 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/session/AIAgentLLMWriteSessionImpl.kt  
[ИСТОЧНИК 5] AIAgentLLMReadSession.kt · specifying (класс, ключевые методы) · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/session/AIAgentLLMReadSession.kt

## Q12: "как реализован state managment"
На основе предоставленного контекста можно описать реализацию **state management** в системе AI агентов.

### Основные принципы реализации

1.  **Иерархия состояний (State Hierarchy)**
    *   Состояния агента представлены запечатанным интерфейсом `sealed interface AIAgentState<Output>` [ИСТОЧНИК 1].
    *   Это обеспечивает **исчерпывающий и безопасный набор возможных состояний** на уровне типа системы. Компилятор гарантирует, что все возможные состояния обработаны в коде (например, в `when`-выражениях).
    *   Конкретные реализации состояний (все `class`-ы внутри `AIAgentState`) и их жизненный цикл детализированы в `AIAgentRunSessionImpl` [ИСТОЧНИК 2, 3].

2.  **Жизненный цикл и управление состоянием (Lifecycle & State Management)**
    *   **Конкретный исполнитель жизненного цикла** — класс `AIAgentRunSessionImpl` [ИСТОЧНИК 2, 3]. Он управляет состоянием **одного конкретного запуска (run)** агента.
    *   **Внутреннее поле состояния**: `private var state: AIAgentState<Output> = NotStarted()` [ИСТОЧНИК 3]. Состояние хранится в сессии и мутирует в течение её выполнения.
    *   **Последовательность состояний**, отражающая стандартный жизненный цикл:
        1.  `NotStarted`: Начальное состояние, установленное при создании сессии [ИСТОЧНИК 3].
        2.  `Starting`: Устанавливается в самом начале метода `run()` [ИСТОЧНИК 3].
        3.  `Running`: Устанавливается перед вызовом стратегии `strategy.execute()`. В это состояние передается `rootContext` агента [ИСТОЧНИК 3].
        4.  `Finished` (успех) или `Failed` (ошибка): Конечные состояния, устанавливаемые после завершения `strategy.execute()` (в `try-catch` блоке) [ИСТОЧНИК 3].
    *   **Корректная работа с контекстом**: Поле `ctx: TContext?` инициализируется в `run()`, хранит контекст исполнения для текущей сессии и становится доступным через `context()` [ИСТОЧНИК 3]. Состояние `Running` напрямую связано с этим контекстом.

3.  **Интеграция с пайплайном (Pipeline Integration)**
    *   Сессия (`AIAgentRunSessionImpl`) активно **уведомляет пайплайн (`AIAgentPipeline`)** о ключевых переходах состояний [ИСТОЧНИК 3]:
        *   `sessionPipeline.onAgentStarting(...)` — при переходе в `Starting`.
        *   `context.pipeline.onStrategyStarting(...)` — при переходе в `Running`, перед выполнением стратегии.
    *   Это позволяет **внешним системам и middleware-компонентам (логирование, мониторинг,特种 обработчики)** следить за ходом выполнения и реагировать на события жизненного цикла.

4.  **Архитектурный контекст (Role of AIAgentBase)**
    *   `AIAgentBase` [ИСТОЧНИК 4, 5] — это **абстрактный базовый класс для самого агента** (не для сессии).
    *   Он определяет **"одноразовость" агента**: агент может быть запущен только один раз через `run()` [ИСТОЧНИК 5].
    *   При вызове `run()` он **создает новую сессию** (`AIAgentRunSessionImpl`), передавая ей свою стратегию, пайплайн и функцию для создания контекста `::prepareContext` [ИСТОЧНИК 5].
    *   Таким образом, **инкапсуляция состояния**: сам агент (`AIAgentBase`) не хранит состояние выполнения. Он хранит конфигурацию (стратегию, пайплайн). **Состояние выполнения хранится и управляется исключительно внутри создаваемой им сессии (`AIAgentRunSessionImpl`)**. Это разделение позволяет агенту быть "фабрикой" для запусков, а не хранителем состояния одного запуска.

### Схема interaction

```mermaid
graph TD
    A[AIAgentBase] -->|вызывает run()| B[AIAgentRunSessionImpl];
    B -->|создает и управляет| S[AIAgentState];
    S -->|возможные значения| S1[NotStarted];
    S --> S2[Starting];
    S --> S3[Running <br/> (с rootContext)];
    S --> S4[Finished];
    S --> S5[Failed];
    B -->|уведомляет| P[AIAgentPipeline];
    B -->|хранит| C[AIAgentContext];
    A -->|определяет| Strat[AIAgentStrategy];
    B -->|выполняет| Strat;
```

### Выводы и особенности

*   **State as a Value**: Состояние — это иммутабельный объект (`copy()` метод в интерфейсе [ИСТОЧНИК 1]), что упрощает отладку и делает изменения состояния предсказуемыми (новый объект на каждом переходе).
*   **Context-driven State**: Состояние `Running` тесно связано с `AIAgentContext`, который является центральным объектом, предоставляющим доступ к ресурсам, пайплайну и информации об исполнении.
*   **Session-Scoped State**: Состояние живет в пределах одного `run()` сессии. После завершения сессии её состояние можно считать финализированным (`Finished`/`Failed`). Новый запуск создаст новую сессию с новым состоянием `NotStarted`.
*   **Event-Driven Transitions**: Ключевые переходы состояний (`Starting` -> `Running`) сопровождаются вызовами методов пайплайна, что реализует паттерн **наблюдатель** для компонентов, заинтересованных в прогрессе.

### Недостающая информация (ограничения контекста)

В предоставленных файлах **не показаны классы `Finished` и `Failed`** для `AIAgentState`, хотя они упоминаются в комментариях [ИСТОЧНИК 2, 3]. Их реализация, вероятно, содержит результат (`result` для `Finished`) или исключение (`error` для `Failed`). Также не показана полная реализация `AIAgentPipeline` и методов `onAgentStarting`/`onStrategyStarting`.

---

### Источники

[ИСТОЧНИК 1] AIAgentState.kt · sealed interface · line 1 · `/Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentState.kt`
[ИСТОЧНИК 2] AIAgentRunSessionImpl.kt · class · line 1 · `/Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentRunSessionImpl.kt`
[ИСТОЧНИК 3] AIAgentRunSessionImpl.kt · run() method & state transitions · line 33 · `/Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentRunSessionImpl.kt`
[ИСТОЧНИК 4] AIAgentBase.kt · class · line 1 · `/Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentBase.kt`
[ИСТОЧНИК 5] AIAgentBase.kt · run() method · line 102 · `/Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentBase.kt`

## Q13: "как реализована обработка ошибок"
Обработка ошибок в представленной кодовой базе реализована на нескольких уровнях с использованием нескольких ключевых классов и интерфейсов, каждый из которых решает определённую задачу. Вот подробный обзор:

### 1. **`AIAgentError` — Структурированное представление ошибки**
Этот `data class` служит универсальным контейнером для передачи информации об ошибке, occurred в рамках работы AI агента. Он сериализуем (`@Serializable`), что позволяет его передавать между модулями, сохранять или отправлять по сети.
*   **Поля:**
    *   `message`: Человекочитаемое описание ошибки. Если `throwable.message` равен `null`, используется значение по умолчанию `"Unknown error"`.
    *   `stackTrace`: Строковое представление полного стека вызовов, полученное через `throwable.stackTraceToString()`.
    *   `cause`: Необязательное поле, содержащее строковое представление стека вызовов из *корневой причины* (`throwable.cause?.stackTraceToString()`).
*   **Конструкторы:**
    *   Основной конструктор принимает три параметра (`message`, `stackTrace`, `cause`).
    *   Вторичный конструктор принимает объект `Throwable` и автоматически извлекает из него все необходимые поля, обеспечивая стандартизированное представление любой ошибки [ИСТОЧНИК 1, 2].
*   **Extension-функция:** `Throwable.toAgentError()` предоставляет удобный синтаксис для преобразования исключения в объект `AIAgentError` [ИСТОЧНИК 2].

**Цель usage:** Предоставить детализированный, сериализуемый объект ошибки для логирования, сообщений пользователю или возврата из асинхронных операций без необходимости пробрасывать исходный `Throwable`.

### 2. **`SafeTool.Result` — Сейф-выполнение инструментов без исключений**
Класс `SafeTool` оборачивает выполнение `Tool` (инструмента) и управляет его результатом через **запечатанный интерфейс (sealed interface)** `Result`. Это подход «типобезопасной» обработки ошибок, который заставляет вызывающий код явно обрабатывать оба исхода.
*   **`Result<TResult>`:** Сеализованный интерфейс с двумя конкретными реализациями:
    *   `Success<TResult>`: Содержает успешный результат типа `TResult` и строковое `content` (например, текст для ответа).
    *   `Failure<TResult>`: Содержит `content` — строку с сообщением об ошибке.
*   **Методы для работы с результатом:**
    *   `isSuccessful()`, `isFailure()`: Возвращают булево значение для проверки типа результата.
    *   `asSuccessful()`, `asFailure()`: Приводят результат к конкретному типу, выбрасывая `IllegalStateException`, если приведение невозможно. Это позволяет безопасно извлекать данные после проверки через `isSuccessful()`/`isFailure()` [ИСТОЧНИК 3, 4].
*   **Как работает:** `SafeTool` вызывает `environment.executeTool(...)`. Результат выполнения (успех или异常) преобразуется в соответствующий подтип `Result`. Это предотвращает распространяющиеся исключения (uncaught exceptions) из кода инструментов и переводит все ошибки в управляемую, предсказуемую форму.

**Цель usage:** Гарантировать, что выполнение внешнего инструмента (например, вызов API, запрос к БД) не сломает поток агента, а его ошибка будет явно представлена в виде значения `Result.Failure`.

### 3. **`AIAgentException` — Специализированные исключения для критических сбоев**
Это иерархия пользовательских исключений (`Exception`), предназначенных для прерывания нормального потока выполнения **агента** при серьёзных, непредвиденных ошибках, которые нельзя обработать на месте.
*   **Базовый класс `AIAgentException`:** Принимает `problem` (строка с описанием) и необязательный `throwable` (причина). Формирует сообщение `"AI Agent has run into a problem: $problem"`.
*   **Специализированные исключения:**
    *   `AIAgentStuckInTheNodeException`: Бросается, когда агент застрял в узле графа выполнения — ни одно outgoing-условие не совпало с выводом текущего узла. Содержит информацию о node и проблемном output.
    *   `AIAgentMaxNumberOfIterationsReachedException`: Бросается при достижении лимита итераций (конфигурационный параметр `maxAgentIterations`). Сигнализирует, что агент не смог завершить задачу в отведённое число шагов [ИСТОЧНИК 5].
*   **Принцип использования:** Эти исключения, вероятно, выбрасываются внутри ядра движка агента (например, при обработке графа состояний или управлении циклом итераций) и должны быть перехвачены на верхнем уровне для корректного завершения работы агента, логирования или уведомления внешнего кода.

**Цель usage:** Отслеживать и прерывать выполнение при состояниях, которые нарушают логику работы агента (застревание в графе, бесконечный цикл), которые не могут быть предсказаны или обработаны как обычные ошибки инструментов.

### **Общая картина обработки ошибок:**
1.  **На уровне инструментов:** Используется `SafeTool.Result`. Ошибка инструмента **не бросает исключение**, а возвращается как `Result.Failure(content)`. Вызывающий код обязан проверить `isSuccessful()`.
2.  **На уровне данных/сериализации:** Для передачи информации об ошибке (например, из одного модуля в другой или клиенту) используется сериализуемый `AIAgentError`, создаваемый из `Throwable`.
3.  **На уровне движка агента:** При критических сбоях в логике выполнения агента (граф, циклы) бросаются специализированные `AIAgentException`, которые разрывают нормальный поток и должны быть обработаны как "фатальные" для текущей сессии агента.

### **Источники**
[ИСТОЧНИК 1] AIAgentError.kt · provides · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/model/AIAgentError.kt  
[ИСТОЧНИК 2] AIAgentError.kt · provides · line 33 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/model/AIAgentError.kt  
[ИСТОЧНИК 3] SafeTool.kt · designed · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/environment/SafeTool.kt  
[ИСТОЧНИК 4] SafeTool.kt · designed · line 40 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/environment/SafeTool.kt  
[ИСТОЧНИК 5] AIAgentException.kt · for · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/exception/AIAgentException.kt

## Q14: "какие механизмы работы с опасными tool calling"
На основании предоставленного контекста можно выделить следующие механизмы, связанные с безопасной и управляемой работой с вызовами инструментов (tool calling):

### 1. Специализированная система исключений для инструментов
Для обработки ошибок, возникающих при работе с инструментами, используется отдельная иерархия исключений `ToolException` [ИСТОЧНИК 1].
*   **Базовый sealed-класс** `ToolException` наследуется от `Exception` и предназначен исключительно для ошибок, специфичных для работы с инструментами [ИСТОЧНИК 1].
*   **Конкретный тип исключения**: `ValidationFailure` используется для обозначения сбоев на этапе валидации входных данных или условий выполнения [ИСТОЧНИК 1].
*   **Утилитарные функции для валидации**:
    *   `validate(expectation, message)` — проверяет булево условие и выбрасывает `ValidationFailure` в случае неудачи [ИСТОЧНИК 1].
    *   `validateNotNull(value, message)` — проверяет, что значение не `null`, и выбрасывает `ValidationFailure` если оно `null` [ИСТОЧНИК 2].
    *   `fail(message)` — напрямую создаёт и выбрасывает исключение `ValidationFailure` с указанным сообщением [ИСТОЧНИК 2].
Эти механизмы позволяют централизованно обрабатывать ошибки валидации, что является ключевым аспектом предотвращения неожиданного или опасного поведения при выполнении инструментов.

### 2. Интерфейс окружения для выполнения инструментов
`AIAgentEnvironment` определяет контракт для компонента, который фактически выполняет вызовы инструментов и управляет их жизненным циклом [ИСТОЧНИК 4].
*   **Метод `executeTool(toolCall)`**: Выполняет **один** вызов инструмента и возвращает объект `ReceivedToolResult` [ИСТОЧНИК 4]. Это основная точка входа для выполнения, где может происходить вся логика проверки, безопасности и изоляции.
*   **Метод `executeTools(toolCalls)`**: Предоставляет удобный способ для **пакетного** выполнения нескольких вызовов инструментов. Он использует `supervisorScope` и `async/awaitAll` для параллельного запуска, при этом исключение в одной из корутин не отменяет выполнение других [ИСТОЧНИК 5]. Это помогает избежать каскадных сбоев, но также требует аккуратной обработки результатов.
*   **Метод `reportProblem(exception)`**: Позволяет окружению **асинхронно сообщать о проблемах** (например, исключениях, возникших в `executeTool`), не прерывая основной поток работы [ИСТОЧНИК 4]. Это механизм для централизованной обработки ошибок и логирования.

### 3. Парсинг и валидация определений инструментов (пример для MCP)
Класс `McpToolDefinitionParser` демонстрирует механизм **предварительной обработки и валидации метаданных** инструмента при его получении из внешнего источника (MCP SDK) [ИСТОЧНИК 3].
*   Парсер преобразует внешнее определение `SDKTool` во внутренний `ToolDescriptor`.
*   В процессе парсинга (`parseParameters`) происходит анализ схемы входных параметров JSON, что само по себе является формой валидации структуры инструмента ещё до его возможного вызова [ИСТОЧНИК 3].
*   Наличие защитного ограничения `MAX_DEPTH` при рекурсивном разборе вложенных типов предотвращает возможные переполнения стека [ИСТОЧНИК 3].

### Важное замечание по контексту
Предоставленный контекст **фокусируется на структурных элементах** (исключения, интерфейсы, парсинг), но **не показывает конкретную реализацию** бизнес-логики, которая бы:
*   Реализовывала политики безопасности (разрешение/запрет вызовов определённых инструментов).
*   Выполняла саму "опасную" операцию (например, вызов shell-команды, доступ к файловой системе).
*   Применяла rate limiting или квоты.
*   Осуществляла детальную валидацию аргументов *перед* выполнением (помимо базовых `validate`/`validateNotNull`).

Таким образом, описанные механизмы представляют собой **каркас для безопасной обработки tool calling** ( centralized error handling, structured validation, separation of concerns), но конкретные политики безопасности должны быть реализованы в классах, которые реализуют интерфейс `AIAgentEnvironment` и используют `ToolException`.

### Historical Sources
[ИСТОЧНИК 1] ToolException.kt · representing · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-tools/src/commonMain/kotlin/ai/koog/agents/core/tools/ToolException.kt  
[ИСТОЧНИК 2] ToolException.kt · representing · line 39 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-tools/src/commonMain/kotlin/ai/koog/agents/core/tools/ToolException.kt  
[ИСТОЧНИК 3] McpToolDefinitionParser.kt · McpToolDescriptorParser · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-mcp/src/commonMain/kotlin/ai/koog/agents/mcp/McpToolDefinitionParser.kt  
[ИСТОЧНИК 4] AIAgentEnvironment.kt · with · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/environment/AIAgentEnvironment.kt  
[ИСТОЧНИК 5] AIAgentEnvironment.kt · with · line 38 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/environment/AIAgentEnvironment.kt

## Q15: "где хранится текущий диалог агента с пользователем и какие возможности по его управлению"
Текущий диалог агента с пользователем хранится и управляется через компонент **`ChatMemory`** [ИСТОЧНИК 4], который является специальной фичей (feature) для работы с историей разговоров.

### Где хранится диалог:
1. **Во время выполнения агента**: история диалога загружается из внешнего хранилища и записывается в **сессию LLM** (поле `prompt.messages`), откуда она подаётся в модель при генерации ответов [ИСТОЧНИК 5]. Это обеспечивает контекст для текущего диалога.
2. **Между сессиями**: диалог persистируется во внешнем хранилище через интерфейс **`ChatHistoryProvider`** [ИСТОЧНИК 4], который привязан к идентификатору запуска (`runId`). Таким образом, история сохраняется между отдельными сессиями агента.

### Возможности управления диалогом:
Конфигурация управляется через **`ChatMemoryConfig`** при установке фичи `ChatMemory` в агента [ИСТОЧНИК 4]:
- **Выбор хранилища**: задаётся кастомный `chatHistoryProvider` [ИСТОЧНИК 4], который определяет, куда и как сохраняется история (например, в память, файл, базу данных).
- **Ограничение размера окна**: параметр `windowSize` [ИСТОЧНИК 4] позволяет хранить только последние N сообщений (например, `windowSize(20)`), что помогает управлять объёмом контекста и стоимостью запросов.
- **Препроцессинг сообщений**: список `preprocessors` [ИСТОЧНИК 5] применяется к загруженной истории перед передачей в LLM-сессию, а также, предположительно, к сообщениям перед сохранением (логика сохранения обрезана в контексте). Это позволяет фильтровать, обрезать или трансформировать сообщения (например, удалять системные промпты или сжимать историю).
- **Интеграция с пайплайнами**: `ChatMemory` поддерживает установку в Graph, Functional и Planner пайплайны агента [ИСТОЧНИК 4], что делает его универсальным для разных архитектур агентов.
- **Автоматическая загрузка/сохранение**: фича автоматически загружает историю при старте стратегии (`interceptStrategyStarting`) и сохраняет после завершения (`interceptStrategyCompleted`) [ИСТОЧНИК 5], минимизируя ручное управление.

### Важное замечание:
Контекст частичный — в методе `installInternal` [ИСТОЧНИК 5] код обрезан после `val proc`, поэтому детали применения препроцессоров при сохранении и явный вызов `chatHistoryProvider.save` не видны. Рекомендуется изучить полный исходный код для понимания всех этапов обработки.

### Связь с `AgentMemoryProvider`:
Интерфейс `AgentMemoryProvider` [ИСТОЧ hunts 1-3] предназначен для хранения структурированных знаний (фактов и концептов) с семантическим поиском, но **не используется напрямую для диалогов**. `ChatMemory` работает с не структурированными сообщениями (`Message`) и независим от `AgentMemoryProvider`, хотя оба отвечают за разные типы памяти агента.

### Источники
[ИСТОЧНИК 1] AgentMemoryProvider.kt · for · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/memory/providers/AgentMemoryProvider.kt  
[ИСТОЧНИК 2] AgentMemoryProvider.kt · for · line 50 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/memory/providers/AgentMemoryProvider.kt  
[ИСТОЧНИК 3] AgentMemoryProvider.kt · for · line 137 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/memory/providers/AgentMemoryProvider.kt  
[ИСТОЧНИК 4] ChatMemory.kt · ChatMemory · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/chatMemory/feature/ChatMemory.kt  
[ИСТОЧНИК 5] ChatMemory.kt · ChatMemory · line 46 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-memory/src/commonMain/kotlin/ai/koog/agents/chatMemory/feature/ChatMemory.kt

## Q16: "как устроена observability и работа с событиями от агентов "
Основываясь на предоставленном контексте, можно описать архитектуру observability и работу с событиями от агентов в фреймворке Koog как **событийно-ориентированную систему на основе обработчиков (handlers)**, организованную вокруг концепции **Feature Key** (ключа фичи) и **Event Type** (типа события).

### 1. **Типы событий и их контексты (Event Types & Contexts)**
Система определяет иерархию событий жизненного цикла агента и предоставляет типизированные контексты для каждого события. Базовым интерфейсом является `AgentEventContext`, который расширяет `AgentLifecycleEventContext` [ИСТОЧНИК 1].
Для конкретных этапов жизненного цикла определяются специализированные data-классы контекстов, которые передаются в обработчики [ИСТОЧНИК 2]:
*   **`AgentStartingContext`** — событие перед началом выполнения агента. Содержит: `agent` (экземпляр агента), `runId` (идентификатор сессии), `executionInfo` (информация об исполнении, включая parentId и путь).
*   **`AgentCompletedContext`** — событие успешного завершения агента. Содержит: `agentId`, `runId`, `result` (результат выполнения, если есть).
*   **`AgentExecutionFailedContext`** — событие ошибки во время выполнения агента. Содержит: `agentId`, `runId`, `throwable` (исключение).

Таким образом, каждое событие имеет уникальный `eventType` (из `AgentLifecycleEventType`) и сопровождается детальным контекстом, который предоставляет доступ к состоянию агента, результатам и метаданным исполнения.

### 2. **Менеджер обработчиков (Handlers Collector)**
Ключевым компонентом для управления Observability является **`AgentLifecycleHandlersCollector`** — внутренний сборщик и реестр обработчиков [ИСТОЧНИК 3] [ИСТОЧНИК 4].

**Принцип работы:**
*   **Регистрация по фиче и типу события:** Обработчики регистрируются в коллекторе в привязке к двум параметрам:
    1.  `featureKey` (`AIAgentStorageKey<*>`): Ключ, идентифицирующий конкретную **фичу (feature)** агента (например, его плагин или модуль). Это позволяет изолировать обработчики, принадлежащие разным компонентам агента.
    2.  `eventType` (`AgentLifecycleEventType`): Тип события (старт, завершение, ошибка и т.д.).
*   **Хранение:** Внутренняя структура `featureToHandlersMap` хранит для каждой фичи (`FeatureEventHandlers`) мапу `handlersByEventType` — список обработчиков для каждого типа события.
*   **Получение обработчиков:** Метод `getHandlersForEvent(eventType)` возвращает **карту**, где ключ — это `featureKey`, а значение — список **типизированных** обработчиков (`AgentLifecycleEventHandler<TContext, TReturn>`), зарегистрированных для заданного `eventType` во всех фичах. При этом происходит безопасное приведение типов (`as?`) при извлечении [ИСТОЧНИК 4].

Этот дизайн обеспечивает **модульность и декомпозицию**: каждая фича агента может независимо регистрировать свои обработчики для интересующих её событий жизненного цикла. Во время выполнения агента система может собрать **все** обработчики, предназначенные для произошедшего события, и выполнить их последовательно.

### 3. **Конфигурация и подключение обработчиков (Configuration)**
Публичный API для настройки событий предоставляется через **`EventHandlerConfigImpl`** (реализация конфигурации фичи `EventHandler`) [ИСТОЧНИК 5].

В этом классе определены **удобные методы-сеттеры** для подписки на основные события агента и LLM, например:
*   `_setOnAgentStarting(handler: suspend (AgentStartingContext) -> Unit)`
*   `_setOnAgentCompleted(handler: suspend (AgentCompletedContext) -> Unit)`
*   `_onAgentExecutionFailed(handler: suspend (AgentExecutionFailedContext) -> Unit)`
*   А также события для LLM-вызовов, узлов (nodes), стратегий (strategies) и потоковой передачи (streaming).

Методы с префиксом `_` (например, `_setOnAgentStarting`) являются **внутренними** (`@InternalAgentsApi`), что указывает на то, что их использование, скорее всего, предназначено для самой системы или для продвинутых сценариев расширения, а не для конечного разработчика.

### **Как это работает вместе (краткий сценарий):**

1.  **Конфигурация:** Разработчик (или встроенная фича) в коде конфигурации агента вызывает, например, `_setOnAgentStarting { context: AgentStartingContext -> ... }`. Этот вызов передаёт лямбду-обработчик в `EventHandlerConfigImpl`, который, в свою очередь, регистрирует её в **`AgentLifecycleHandlersCollector`** с ключом своей фичи (`featureKey`) и типом события `AgentStarting` [ИСТОЧНИК 5] -> [ИСТОЧНИК 4].
2.  **Выполнение:** Когда в процессе работы агента происходит событие (например, агент начал выполнение), система формирует соответствующий контекст (`AgentStartingContext`) [ИСТОЧНИК 2].
3.  **Диспетчеризация:** Система (вероятно, внутри движка выполнения агента) запрашивает у коллектора все обработчики для типа `AgentStarting` [ИСТОЧНИК 4].
4.  **Исполнение:** Полученный список обработчиков (от всех зарегистрированных фич) выполняется **последовательно**, получая на вход созданный контекст. Каждый обработчик может, например, логировать событие, обновлять метрики observability, отправлять уведомления или изменять состояние в контексте.

### **Итог:**
Observability в Koog строится на **гибкой, feather-weight системе событий**:
*   **События** — чётко определены через `AgentLifecycleEventType` и имеют богатые, типизированные **контексты** (`AgentEventContext` derivatives).
*   **Обработчики** регистрируются **модульно** (по `featureKey`) и **типизированно** (по `eventType`) в централизованном **сборщике** (`AgentLifecycleHandlersCollector`).
*   **Конфигурация** происходит через декларативные методы в конфигурационном классе фичи (`EventHandlerConfigImpl`).

Эта архитектура позволяет **наблюдать за агентом (observability)**, **отслеживать его состояние** и **реагировать на ключевые точки** его жизненного цикла с минимальным связыванием кода и высокой расширяемостью.

---
### Источники
[ИСТОЧНИК 1] AgentEventContext.kt · extends · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/handler/agent/AgentEventContext.kt
[ИСТОЧНИК 2] AgentEventContext.kt · extends · line 34 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/handler/agent/AgentEventContext.kt
[ИСТОЧНИК 3] AgentLifecycleHandlersCollector.kt · serves · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/handler/AgentLifecycleHandlersCollector.kt
[ИСТОЧНИК 4] AgentLifecycleHandlersCollector.kt · serves · line 36 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/handler/AgentLifecycleHandlersCollector.kt
[ИСТОЧНИК 5] EventHandlerConfigImpl.kt · EventHandlerConfigImpl · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-features/agents-features-event-handler/src/commonMain/kotlin/ai/koog/agents/features/eventHandler/feature/EventHandlerConfigImpl.kt

## Q17: "что такое GraphAIAgent, каковы его преимущества и схемы использования?"
На основе предоставленного контекста можно сделать следующие выводы о `GraphAIAgent`.

### Что такое GraphAIAgent?
В предоставленном коде **отсутствует класс с точным именем `GraphAIAgent`**. Однако, анализируя предоставленные файлы, можно реконструировать, что `GraphAIAgent` — это, вероятно, **концепция или компонент системы, который использует графовые стратегии (`AIAgentGraphStrategy`) для определения логики выполнения AI-агента**. Его работа координируется контекстом `AIAgentGraphContext` [ИСТОЧНИК 4].

Ключевые составляющие этой концепции:
1.  **`AIAgentGraphStrategy`**: Определяет *схему* (graph) выполнения агента — последовательность узлов (nodes) и рёбер (edges). В коде представлены несколько готовых стратегий:
    *   `chatAgentStrategy()` [ИСТОЧНИК 1]: Для интерактивного диалога, где агент поочередно вызывает LLM, выполняет инструменты (tools) и отправляет результаты.
    *   `reactAgentStrategy()` [ИСТОЧНИК 2]: Реализует методологию ReAct (Reason + Act), где агент циклически переходит между этапами "Reasoning" (размышление) и "Action" (выполнение инструмента).
    *   `structuredOutputWithToolsStrategy()` [ИСТОЧНИК 3]: Для получения структурированного вывода (structured output) с возможностью вызова инструментов.
2.  **`AIAgentGraphContext`**: Инкапсулирует состояние и окружение для выполнения агента по графовой стратегии. Он содержит [ИСТОЧНИК 4]:
    *   `agentInput` и `agentInputType`: Входные данные.
    *   `config`: Конфигурацию агента.
    *   `llm`: Контекст для взаимодействия с языковой моделью.
    *   `stateManager` и `storage`: Для управления внутренним состоянием и хранения данных.
    *   `strategyName` и `pipeline`: Идентификатор выбранной стратегии и конвейер (pipeline) её выполнения.
    *   `environment`: Окружение, ответственное за выполнение инструментов (tools).

Таким образом, **`GraphAIAgent` можно понимать как "агент, работающий по графовой стратегии"**, где конфигурируемый граф (`AIAgentGraphStrategy`) определяет его "мозг" и "поведение", а `AIAgentGraphContext` обеспечивает всю необходимую среду для его работы.

### Преимущества использования графовых стратегий для AI-агентов
Основываясь на структуре кода, можно выделить следующие преимущества:

1.  **Явное и декларативное описание потока выполнения**: Логика агента (последовательность шагов, условия перехода) задаётся явно через узлы и рёбра графа, а не скрывается внутри императивного кода. Это упрощает понимание и отладку.
2.  **Гибкость и переиспользование**: Разные стратегии (`chat`, `react`, `structuredOutput`) представляют собой изолированные, готовые "шаблоны" поведения. Их можно комбинировать, конфигурировать и переиспользовать в разных агентах [ИСТОЧНИК 1, 2, 3].
3.  **Структурированное управление состоянием**: Контекст (`AIAgentGraphContext`) централизованно управляет состоянием агента, его хранилищем (`storage`), конфигурацией и сессией LLM, что облегчает создание сложных, состояний-зависимых агентов [ИСТОЧНИК 4].
4.  **Модульность узлов**: Каждый узел (например, `nodeCallLLM`, `nodeExecuteTool`, `nodeSendToolResult` в `chatAgentStrategy` [ИСТОЧНИК 1]) выполняет одну четкую задачу. Это упрощает тестирование и модификацию отдельных этапов.
5.  **Чёткое разделение ответственности**: Стратегия определяет *схему*, контекст предоставляет *данные и сервисы*, а пайплайн (`pipeline`) отвечает за *координацию* выполнения графа. Это соответствует принципам чистой архитектуры.

### Схемы использования (на примере предоставленных стратегий)
Предоставленный код демонстрирует три основные схемы (стратегии) использования графового агента:

#### 1. Чат-агент (`chatAgentStrategy`)
**Цель**: Интерактивное общение с пользователем, где агент *должен* использовать доступные инструменты вместо простого текстового ответа.
**Схема выполнения** [ИСТОЧНИК 1]:
```
Start -> LLMRequest ("sendInput") -> 
    [Ветвление]
    ├─(если LLM вернула tool_calls)--> ExecuteTool -> LLMSendToolResult -> 
    │                                               │
    │                                               ├─(если tool_call для "__exit__")--> Finish
    │                                               └─(иначе)-----------------------------> ExecuteTool (цикл)
    │
    └─(если LLM вернула plain text)--> GiveFeedbackToCallTools (увещевание вызвать инструмент) -> 
                                               │
                                               ├─(повтор plain text)--> GiveFeedbackToCallTools (цикл упрёков)
                                               └─(появился tool_call)--> ExecuteTool
                                                       │
                                                       └---> LLMSendToolResult (далее как выше)
```
**Особенность**: Агент заточён на использование инструментов. Если LLM отвечает текстом, агент специальным узлом `giveFeedbackToCallTools` добавляет в промпт инструкцию вызвать инструмент и запрашивает LLM снова.

#### 2. ReAct агент (`reactAgentStrategy`)
**Цель**: Последовательное решение сложных задач методом "размышление -> действие -> наблюдение".
**Схема выполнения** [ИСТОЧНИК 2]:
```
Start -> CallLLMReason (LLM генерирует мысли и план) ->
    [Ветвление]
    ├─(если есть tool_calls в мысли)--> CallLLMAction (LLM формирует конкретный вызов инструмента) -> 
    │                                      │
    │                                      ├─(инструмент != "__exit__")--> ExecuteTool -> 
    │                                      │                               │
    │                                      │                               └---> CallLLMReason (обратная связь от инструмента, новый цикл размышления)
    │                                      │
    │                                      └─(инструмент == "__exit__")--> Finish
    │
    └─(если нет tool_calls, finished?)--> Finish
```
**Особенность**: Чёткое разделение на фазу "Reason" (обдумывание, планирование) и фазу "Action" (выполнение конкретного шага). Граф обеспечивает цикличность этого процесса.

#### 3. Агент со структурированным выводом (`structuredOutputWithToolsStrategy`)
**Цель**: Извлечь из ответа LLM строго определённую структуру данных (`Output`), при этом разрешив LLM вызывать инструменты для сбора информации.
**Схема выполнения** [ИСТОЧНИК 3]:
```
Start -> SetStructuredOutput (установка схемы вывода в LLM) -> TransformInput -> CallLLMRequestMultiple ->
    [Ветвление]
    ├─(есть multiple tool_calls)--> ExecuteMultipleTools (параллельно) -> LLMSendMultipleToolResults ->
    │                                   │
    │                                   ├─(есть повторные tool_calls)--> ExecuteMultipleTools (цикл)
    │                                   └─(нет tool_calls, есть assistant messages)--> TransformToStructuredOutput (парсинг ответа в Output) -> Finish
    │
    └─(нет tool_calls, есть assistant messages)--> TransformToStructuredOutput -> Finish
```
**Особенность**: Стратегия оптимизирована для работы с *множественными* вызовами инструментов за один ход LLM (`multiple tool_calls`). Финальный результат не является текстом, а преобразуется (`TransformToStructuredOutput`) в заданную при инициализации структуру данных (`Output`).

### Заключение
`GraphAIAgent` — это не конкретный класс, а **архитектурный подход**, реализованный через связку `AIAgentGraphStrategy` (определяет граф) и `AIAgentGraphContext` (обеспечивает исполнение). Его главные преимущества — это **декларативность, гибкость и управляемость**. Выбор конкретной графовой стратегии (`chat`, `react`, `structuredOutput`) определяет "личность" и способ решения задач агента.

---
### Источники
[ИСТОЧНИК 1] AIAgentStrategies.kt · chatAgentStrategy · line 31 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/AIAgentStrategies.kt
[ИСТОЧНИК 2] AIAgentStrategies.kt · reactAgentStrategy · line 72 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/AIAgentStrategies.kt
[ИСТОЧНИК 3] AIAgentStrategies.kt · structuredOutputWithToolsStrategy · line 217 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/AIAgentStrategies.kt
[ИСТОЧНИК 4] AIAgentGraphContext.kt · extends · line 106 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentGraphContext.kt

## Q18: "все полезное про AIAgentContext и AIAgentGraphContext"
На основе предоставленного контекста можно дать только **частичную информацию** об `AIAgentContext` и `AIAgentGraphContext`. Контекст содержит фрагменты, но недостаточные для полного описания:

### Что можно сказать на основе контекста:

1.  **`AIAgentGraphContext`** (упоминается в файле `AIAgentGraphContext.kt` [ИСТОЧНИК 2]):
    *   Это **интерфейс** `AIAgentGraphContextBase`, который расширяет (наследует от) `AIAgentContext`.
    *   Он предназначен для предоставления базового контекста для AI-агентов, работающих внутри **графовой структуры** (graph structure).
    *   Унаследованные от `AIAgentContextBase` возможности включают: управление окружением (`environment`), доступ к конфигурации (`config`), трекинг сессии (`session`), управление состоянием (`state`) и пользовательские рабочие процессы (`workflow`).
    *   Основная задача интерфейса — служить основой для определения дополнительных конструкций и поведения, которые облегчают выполнение агента в **графовых рабочих процессах** (graph-based workflows) или **конвейерах выполнения** (execution pipelines). Ожидается, что реализации будут использовать предоставленные возможности для обработки граф-специфичной логики, такой как обход узлов (node traversal), управление вводом/выводом (input/output management) и обработка сложных зависимостей между узлами графа.

2.  **`AIAgentGraphContextBase`** (упомянут в нескольких файлах):
    *   Это более низкоуровневый интерфейс, который, судя по импортам, лежит в основе `AIAgentGraphContext`.
    *   Его ключевые свойства, видимые в сигнатурах методов:
        *   `pipeline: AIAgentGraphPipeline` — конвейер, управляющий выполнением графа.
        *   `agentInputType: TypeToken` — тип данных, которые являются входом для агента в этом контексте.
        *   `copy(...)` — функция для создания копии контекста с возможностью переопределения его свойств (окружение, ID агента, входные данные, конфигурация).

3.  **Связь с другими компонентами**:
    *   `AIAgentGraphStrategy` [ИСТОЧНИК 3] — стратегия, управляющая рабочими процессами агента, построенными как **подграфы** (subgraphs) взаимосвязанных узлов. Он использует `StartNode` и `FinishNode` для определения界限 подграфа и `ToolSelectionStrategy` для управления инструментами.
    *   `AIAgentNodeBase` [ИСТОЧНИК 4] — абстрактный узел в графе стратегии агента. Он отвечает за выполнение конкретной операции и управляет исходящими ребрами (edges) к другим узлам. Имеет `inputType` и `outputType`.
    *   `AIAgentGraphPipelineImpl` [ИСТОЧНИК 5] — внутренняя реализация конвейера для графа. Она обрабатывает события жизненного цикла узлов: `onNodeExecutionStarting`, `onNodeExecutionCompleted`, `onNodeExecutionFailed`, передавая в обработчики контекст `AIAgentGraphContextBase` и информацию об узле `AIAgentNodeBase<*, *>`.

4.  **Ограничения контекста**:
    *   В предоставленных фрагментах **отсутствует полное определение интерфейса `AIAgentContext`**, от которого всё наследуется.
    *   Нет деталей о том, как именно создается экземпляр `AIAgentGraphContext`, какие еще методы есть в этом интерфейсе (например, для доступа к состоянию, истории сообщений, инструментам и т.д.).
    *   Нет примеров использования или архитектурных диаграмм, поясняющих, как `AIAgentGraphContext` взаимодействует с `AIAgentGraphStrategy` и `AIAgentNode` на практике.
    *   Файл `LLMAsAJudge.kt` [ИСТОЧНИК 1] не содержит прямого отношения к контексту, кроме импорта `AIAgentGraphContextBase`. Он демонстрирует другую функциональность (оценка задач LLM).

### Вывод:
`AIAgentGraphContext` (и его базовый интерфейс `AIAgentGraphContextBase`) — это **специализированный контекст выполнения для агентов, использующих графовую архитектуру**. Он предоставляет граф-специфичные данные (тип входных данных, конвейер) поверх базовых возможностей `AIAgentContext` (окружение, конфиг, состояние). Он активно используется внутренними компонентами движка (`AIAgentGraphPipelineImpl`) для передачи состояния и метаданных во время обхода узлов графа.

Для полного понимания **необходимо дополнить контекст**:
1.  Полным определением интерфейса `AIAgentContext` (его родительского интерфейса).
2.  Классом-реализацией `AIAgentGraphContext` (где находится его `copy` метод, какие еще свойства и методы он имеет).
3.  Примером кода, показывающим создание графа стратегии (`AIAgentGraphStrategy`) и как контекст "протекает" через узлы (`AIAgentNode`).

### Уточняющий вопрос:
Могли бы вы предоставить полные определения файлов `AIAgentContext.kt` (родительский интерфейс) и `AIAgentGraphContext.kt` (конкретная реализация или полный интерфейс), чтобы понять полный набор методов и свойств, доступных в контексте агента?

## Q19: "опиши работу с AIAgentLLMContext"
### Обзор `AIAgentLLMContext`

`AIAgentLLMContext` — это **ожидаемый класс (expect class)**, который выступает в роли основного контекста для работы с языковой моделью (LLM) в рамках AI-агента [ИСТОЧНИК 1]. Он инкапсулирует состояние и логику взаимодействия с LLM, включая управление инструментами (tools), промптами (prompts), моделью (model) и окружением (environment), обеспечивая **потокобезопасность** через механизм сессий чтения (`AIAgentLLMReadSession`) и записи (`AIAgentLLMWriteSession`) [ИСТОЧНИК 1, 4].

---

### Ключевые характеристики и назначение

1.  **Роль и ответственность**: Управляет инструментами, промптами и взаимодействием с LLM для AI-агента [ИСТОЧНИК 1]. Он служит центральным хранилищем и менеджером конфигурации, необходимой для вызова языковой модели.

2.  **Структура и зависимости (через конструктор)**:
    Контекст инициализируется со следующими обязательными и опциональными параметрами [ИСТОЧНИК 2]:
    *   **`tools`**: Список дескрипторов доступных инструментов (`ToolDescriptor`), которые агент может использовать.
    *   **`toolRegistry`**: Реестр метаданных об инструментах (по умолчанию `ToolRegistry.EMPTY`).
    *   **`prompt`**: Текущий промпт (`Prompt`), который будет обрабатываться.
    *   **`model`**: Конкретная языковая модель (`LLModel`), используемая для генерации.
    *   **`responseProcessor`**: Необязательный процессор (`ResponseProcessor`) для постобработки ответов модели.
    *   **`promptExecutor`**: Исполнитель (`PromptExecutor`), ответственный за операционную логику обработки промпта.
    *   **`environment`**: Среда (`AIAgentEnvironment`), управляющая выполнением инструментов и взаимодействием с внешними зависимостями.
    *   **`config`**: Конфигурация агента (`AIAgentConfig`).
    *   **`clock`**: Часы (`Clock`) для управления временными метками сообщений.

3.  **Архитектурный паттерн (Expect/Actual)**:
    `AIAgentLLMContext` объявлен как `expect class` [ИСТОЧНИК 1, 2]. Это означает, что его **фактическая реализация** (`actual class`) предоставляется в платформо-специфичных модулях (например, для JVM, Native). Сам класс просто делегирует все вызовы внутреннему полю `delegate: AIAgentLLMContextImpl` [ИСТОЧНИК 1]. В предоставленном контексте **реализация `AIAgentLLMContextImpl` не показана**, что является ограничением данного фрагмента.

---

### Основной публичный интерфейс (через `AIAgentLLMContextAPI`)

`AIAgentLLMContext` реализует интерфейс `AIAgentLLMContextAPI` [ИСТОЧНИК 1, 2]. Через этот интерфейс предоставляется доступ к критически важным свойствам:

*   **`toolRegistry`**: Доступ к реестру инструментов [ИСТОЧНИК 4]. Позволяет получать метаданные о доступных `ToolDescriptor`.
*   **`promptExecutor`**: Доступ к исполнителю промптов [ИСТОЧНИК 4]. **Важно**: Доступ к этому API требует явного `opt-in` из-за аннотации `@DetachedPromptExecutorAPI` [ИСТОЧНИК 4]. Это означает, что вызовы `PromptExecutor` **не связаны с логикой агента** и не влияют на его состояние, а стратегия конвертации инструментов (`ToolsConversionStrategy`) на них не применяется. Использование требует осторожности [ИСТОЧНИК 4].
*   **`environment`**: Доступ к среде агента [ИСТОЧНИК 4], помеченной как `@InternalAgentsApi`. Предназначена для внутреннего использования фреймворком.

---

### Механизм сессий и потокобезопасность

Ключевая особенность контекста — разделение операций на **чтение** и **запись** через соответствующие сессии, что обеспечивает безопасность в многопоточной среде [ИСТОЧНИК 1].

1.  **`handleRead()`**: Предположительно, создает или возвращает сессию **только для чтения** (`AIAgentLLMReadSession`). В рамках такой сессии можно безопасно получать доступ к текущему состоянию промпта, модели, инструментов и т.д., но нельзя изменять контекст.
2.  **`handleWrite()`**: Предположительно, создает или возвращает сессию **для записи** (`AIAgentLLMWriteSession`). Позволяет изменять состояние контекста (например, добавлять сообщения в промпт, обновлять модель). Запись должна быть изолирована, чтобы не создавать гонок данных с параллельными операциями чтения.

*Примечание: В предоставленном фрагменте кода bodies методов `handleRead()` и `handleWrite()` не показаны.*

---

### Создание копии контекста

Существует метод `copy(...)`, который создает **новый экземпляр** `AIAgentLLMContext` с измененными параметрами, оставляя исходный контекст неизменным [ИСТОЧНИК 3]. Это полезно для создания изолированных сред для подзадач или ветвления исполнения без влияния на основной поток.

---

### Связь с другими компонентами

*   **`AIAgentGraphStrategy`**: Стратегия выполнения агента, работающая с графами узлов, вероятно, использует `AIAgentLLMContext` (или его потомка `AIAgentGraphContextBase`) для получения доступа к промпту, модели и инструментам на каждом шаге выполнения [ИСТОШНИК 5].
*   **`AIAgentConfig`**: Конфигурационные настройки агента передаются в контекст, что позволяет единообразно управлять поведением (например, параметрами LLM).

---

### Важные замечания по контексту

1.  **Неполнота данных**: В предоставленном контексте **отсутствует**:
    *   Тело конструктора и свойств.
    *   Реализация методов `handleRead()` и `handleWrite()`.
    *   Класс `AIAgentLLMContextImpl` (необходимая платформо-специфичная реализация).
    *   Интерфейсы `AIAgentLLMReadSession` и `AIAgentLLMWriteSession`.
    *   Детали работы `PromptExecutor` и `ToolsConversionStrategy`.

2.  **`DetachedPromptExecutorAPI`**: Это **аннотация**, а не часть рабочего кода. Она маркирует API (`promptExecutor`), нарушающие инкапсуляцию жизненного цикла агента [ИСТОЧНИК 4]. Её использование требует `OptIn` и несет риск неожиданного поведения, так как такие вызовы "отсоединены" от основной логики агента.

---

### Резюме

`AIAgentLLMContext` — это **потокобезопасный, неизменяемый (вне сессии записи) контейнер конфигурации** для взаимодействия агента с LLM. Он предоставляет управляемый доступ к промпту, модели, инструментам и среде выполнения через **сессии чтения/записи**. Его основная ценность — в обеспечении целостности состояния агента при параллельных операциях и явном раздещении конфигурационных и исполняемых компонентов. Для полного понимания механизма сессий и делегирования необходима реализация `AIAgentLLMContextImpl`.

### Источники
[ИСТОЧНИК 1] AIAgentLLMContext.kt · AIAgentLLMContext · line 1 · `/Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentLLMContext.kt`  
[ИСТОЧНИК 2] AIAgentLLMContext.kt · AIAgentLLMContext · line 31 · `/Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentLLMContext.kt`  
[ИСТОЧНИК 3] AIAgentLLMContext.kt · AIAgentLLMContext · line 156 · `/Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentLLMContext.kt`  
[ИСТОЧНИК 4] AIAgentLLMContextAPI.kt · DetachedPromptExecutorAPI · line 1 · `/Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentLLMContextAPI.kt`  
[ИСТОЧНИК 5] AIAgentGraphStrategy.kt · AIAgentGraphStrategy · line 1 · `/Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/AIAgentGraphStrategy.kt`

## Q20: "опиши назначение и как работать с AIAgentPlannerContext"
На основе предоставленного контекста невозможно дать полный ответ о классе `AIAgentPlannerContext`, так как он отсутствует в найденных файлах. 

В контексте представлены только:
1. `DummyAIAgentContext` — мок-реализация `AIAgentContext` для тестов [ИСТОЧНИК 1][ИСТОЧНИК 2][ИСТОЧНИК 3].
2. `AIAgentSubgraphBuilder` — билдер для создания подграфов агента [ИСТОЧНИК 4][ИСТОЧНИК 5].

`AIAgentPlannerContext` может быть отдельным классом, интерфейсом или частью другой модуля, который не попал в текущий поисковый запрос.

### Предлагаемые уточняющие действия:
1. Проверьте полное имя класса (возможно, он находится в другом пакете, например `ai.koog.agents.core.context` или подобном).
2. Уточните, в каком контексте вы встречали этот класс (например, при работе с планировщиком задач агента, стратегиями выполнения и т.д.).
3. Если есть пример использования или сигнатура методов, это поможет точнее локализовать класс.

---

**Не знаю. Уточните:** Полное имя пакета/модуля, в котором определен `AIAgentPlannerContext`, или приведите пример кода, где он используется.

## Q21: "что такое AIAgentGraphStrategy  и примеры использования"
**AIAgentGraphStrategy** — это стратегия выполнения AI-агента, построенная в виде направленного графа из interconnected nodes (взаимосвязанных узлов), где каждый узел представляет собой этап обработки [ИСТОЧНИК 3]. Эта стратегия управляет полным жизненным циклом агента, начиная от получения входных данных через стартовый узел (`StartNode`) и завершая возвратом результата через финишный узел (`FinishNode`), при этом определяя доступный набор инструментов (tools) через `ToolSelectionStrategy` [ИСТОЧНИК 3].

### Ключевые характеристики:
1.  **Графовая структура**: Взаимодействие между узлами определяется связями (edges), которые могут быть как статическими, так и динамическими (например, на основе условий или результатов предыдущих узлов).
2.  **Типобезопасность**: Стратегия параметризована типами `TInput` (входной тип) и `TOutput` (выходной тип), что гарантирует корректность данных на границах графа.
3.  **Инкапсуляция метаданных**: Владеет `metadata` типа `SubgraphMetadata`, содержащим информацию о структуре графа (маппинг имён узлов на их реализации, уникальность имён) [ИСТОЧНИК 3].
4.  **Контекст выполнения**: Исполняется в контексте `AIAgentGraphContextBase`, который хражит состояние агента во время выполнения.

### Процесс построения (через Builder):
Стратегия создаётся с помощью `AIAgentGraphStrategyBuilder`, который:
-   Определяет `name` (уникальный идентификатор стратегии).
-   Автоматически создаёт `StartNode<TInput>` и `FinishNode<TOutput>`.
-   Принимает `toolSelectionStrategy` (например, `ALL` или кастомную логику фильтрации инструментов).
-   В DSL-блоке `init` конфигурирует узлы и их соединения [ИСТОЧНИК 1, 2].

### Пример использования (DSL):
```kotlin
val strategy: AIAgentGraphStrategy<String, String> = strategy(
    name = "simple-pipeline",
    toolSelectionStrategy = ToolSelectionStrategy.ALL
) {
    // Пример определения узлов и соединений (детали зависят от других частей кодовой базы)
    // node("stage1") { ... }
    // node("stage2") { ... }
    // start connectTo "stage1"
    // "stage1" connectTo "stage2"
    // "stage2" connectTo finish
}
```
Конкретный синтаксис добавления узлов и связей не показан в предоставленном контексте, но обязательным является наличие `start` и `finish` узлов, управляемых бطفом.

### Интеграция в тестирование:
В `TestingFeature` стратегии используются через `NodeReference.Strategy` для декларативного описания ожидаемой структуры графа в тестах [ИСТОЧНИК 4, 5]:
```kotlin
// В тестовом DSL:
graphAssertions {
    strategy("simple-pipeline") {
        // Проверка достижимости узлов, выходов, условий
    }
}
```
Метод `resolve` в `NodeReference.Strategy` проверяет, что подграф с заданным именем действительно является экземпляром `AIAgentGraphStrategy` [ИСТОЧНИК 5].

### Ограничения контекста:
Предоставленные файлы фокусируются на **архитектуре и строителе стратегии**, а не на:
-   Конкретных типах узлов (например, `LLMNode`, `ToolCallNode`).
-   Логике автоматического определения следующего узла (перехода) в графе.
-   Примеров с бизнес-логикой (например, "анализ данных → генерация отчёта → верификация").
Для полного понимания необходимо изучать смежные модули, определяющие `AIAgentGraphContext`, конкретные реализации `Node` и механизмы `ToolSelectionStrategy`.

### Источники
[ИСТОЧНИК 1] AIAgentGraphStrategyBuilder.kt · responsible · line 1 · `/Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/builder/AIAgentGraphStrategyBuilder.kt`  
[ИСТОЧНИК 2] AIAgentGraphStrategyBuilder.kt · responsible · line 34 · `/Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/builder/AIAgentGraphStrategyBuilder.kt`  
[ИСТОЧНИК 3] AIAgentGraphStrategy.kt · AIAgentGraphStrategy · line 33 · `/Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/AIAgentGraphStrategy.kt`  
[ИСТОЧНИК 4] TestingFeature.kt · NodeReference · line 154 · `/Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-test/src/commonMain/kotlin/ai/koog/agents/testing/feature/TestingFeature.kt`  
[ИСТОЧНИК 5] TestingFeature.kt · NodeReference · line 186 · `/Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-test/src/commonMain/kotlin/ai/koog/agents/testing/feature/TestingFeature.kt`

## Q22: "подробно про AIAgentNode  и его преимущества"
На основании предоставленного контекста проанализируем **AIAgentNode** и его преимущества в рамках фреймворка **ai.koog.agents**.

### Определение и сущность AIAgentNode
**AIAgentNode** (в контексте представленных файлов — `AIAgentNodeDelegate`) является фундаментальным строительным блоком для определения логики агента в виде графа. Это делегат, который связывает **входной тип** данных с **выходным типом**, encapsulating (инкапсулируя) определенную операцию или этап обработки [ИСТОЧНИК 1, ИСТОЧНИК 3]. В коде узлы создаются через DSL-функции с аннотацией `@AIAgentBuilderDslMarker`, что обеспечивает типобезопасный и удобный способ построения графов агентов.

Примеры конкретных узлов:
1.  **`nodeLLMSendResultsMultipleChoices`**: Принимает на вход `List<ReceivedToolResult>` (результаты выполнения инструментов) и возвращает `List<LLMChoice>` (несколько вариантов ответа от LLM). Его задача — отправить результаты инструментов в сессию LLM и запросить множество вариантов выбора [ИСТОЧНИК 1].
2.  **`nodeSelectLLMChoice`**: Принимает `List<LLMChoice>` и, используя стратегию `ChoiceSelectionStrategy`, возвращает один выбранный `LLMChoice`. После выбора он также добавляет выбранный вариант обратно в промпт [ИСТОЧНИК 1].

### Архитектурная роль и преимущества

Преимущества **AIAgentNode** вытекают из его роли в архитектуре агентов Koog:

1.  **Композируемость и модульность**. Узлы позволяют разбивать сложную логику агента на небольшие, переиспользуемые и тестируемые этапы. Каждый узел выполняет одну четко определенную задачу (например, "отправить результаты инструментов" или "выбрать лучший ответ"). Граф агента собирается путем последовательного или условного соединения таких узлов [ИСТОЧНИК 2, ИСТОЧНИК 3].
2.  **Типобезопасность**. Делегат `AIAgentNodeDelegate<Input, Output>` строго типизирует данные, которые он потребляет и производит. Это гарантирует совместимость узлов при их соединении на этапе компиляции и предотвращает ошибки несоответствия типов в графе [ИСТОЧНИК 1, ИСТОЧНИК 3].
3.  **Интеграция с LLM и инструментами**. Специализированные узлы (как `nodeLLMSendResultsMultipleChoices`) прямым образом работают с сессией LLM (`llm.writeSession { ... }`), абстрагируя детали управления промптом, добавления сообщений от инструментов (`tool { result(it) }`) и выполнения запросов (`requestLLMMultipleChoices()`). Это централизует логику взаимодействия с моделью [ИСТОЧНИК 1].
4.  **Гибкость через стратегии**. Поведение узлов может настраиваться путем передачи стратегий. Например, `nodeSelectLLMChoice` принимает `choiceSelectionStrategy: ChoiceSelectionStrategy`, что позволяет гибко определять, *как* из множества вариантов LLM выбирается окончательный (например, первый, лучший по вероятности, по кастомному правилу). Аналогично, на уровне подграфов (`subgraphWithTask`) задается `toolSelectionStrategy`, управляющая набором доступных инструментов [ИСТОЧНИК 1, ИСТОЧНИК 2].
5.  **Поддержка сложных сценариев (множественные выборы)**. Наличие узлов для работы с `List<LLMChoice>` указывает на поддержку сценариев, где LLM может предложить несколько альтернативных действий (например, несколько параллельных вызовов инструментов), а агент затем должен выбрать одну или скомбинировать их. Это важно для более продвинутых паттернов планирования и рассуждения.
6.  **Структурированное управление подграфами**. Узлы являются частью более крупной сущности — `AIAgentSubgraphDelegate` (или `subgraph`). Подграф сам по себе действует как сложный узел со своими `nodeStart` и `nodeFinish` [ИСТОЧНИК 3]. Это позволяет иерархически строить агенты: большой агент может состоять из подграфов, каждый из которых — из узлов. Это promotes reuse и organization.
7.  **Конфигурируемость**. При построении графа или подграфа можно задавать параметры: `llmModel`, `llmParams`, `runMode` (последовательный/параллельный запуск инструментов), `assistantResponseRepeatMax` и `responseProcessor` для пост-обработки ответов [ИСТОЧНИК 2]. Это делает узлы и подграфы гибкими компонентами.

### Пример использования в контексте
Файл `AIAgentGraphStrategyBuilder.kt` [ИСТОЧНИК 4, ИСТОЧНИК 5] показывает, как на уровне стратегии (`strategy { ... }`) определяются `nodeStart` и `nodeFinish`. Внутри блока `init` разработчик, используя предоставленные DSL-функции (включая, вероятно, `nodeLLMSendResultsMultipleChoices` и `nodeSelectLLMChoice`), конструирует последовательность или граф узлов, которые связывают старт и финиш. Это демонстрирует, что узлы — это основной материал, из которого строятся все агенты в этом фреймворке.

### Ограничения контекста
Предоставленные файлы фокусируются на **расширениях (ext)** для работы с выбором LLM (`llm.choice`) и на **билдерах подграфов и стратегий**. Они не содержат:
*   Базового определения интерфейса/класса `AIAgentNodeDelegate` (только его использование).
*   Детальной реализации простейших узлов (например, простого вызова LLM без множественных выборов или обработки инструментов).
*   Примеров полного графа агента, использующего оба узла (`sendResults` -> `selectChoice`) последовательно.
*   Информации о том, как узелы обрабатывают ошибки или условия перехода.

### Вывод
**AIAgentNode (AIAgentNodeDelegate)** — это типобезопасный, композируемый и настраиваемый компонент в фреймворке Koog Agents, представляющий отдельный шаг обработки в графе агента. Его ключевые преимущества — модульность, строгая типизация, глубокая интеграция с LLM и инструментами, а также гибкость благодаря передаваемым стратегиям. Он является основой для построения как простых, так и сложных иерархических агентов, поддерживающих продвинутые сценарии вроде множественных предложений от LLM и их последующего выбора.

---
### Источники
[ИСТОЧНИК 1] AIAgentNodesWithChoiceExt.kt · nodeLLMSendResultsMultipleChoices, nodeSelectLLMChoice · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/llm/choice/AIAgentNodesWithChoiceExt.kt
[ИСТОЧНИК 2] AIAgentSubgraphExt.kt · subgraphWithTask · line 197 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/AIAgentSubgraphExt.kt
[ИСТОЧНИК 3] AIAgentSubgraphBuilder.kt · AIAgentSubgraphDelegate · line 265 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/builder/AIAgentSubgraphBuilder.kt
[ИСТОЧНИК 4] AIAgentGraphStrategyBuilder.kt · AIAgentGraphStrategyBuilder · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/builder/AIAgentGraphStrategyBuilder.kt
[ИСТОЧНИК 5] AIAgentGraphStrategyBuilder.kt · strategy · line 34 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/dsl/builder/AIAgentGraphStrategyBuilder.kt

## Q23: "как использовать AIAgentState"
Использование `AIAgentState` зависит от того, какую из двух сущностей с этим именем вы имеете в виду. В контексте представлены две разные, но связанные концепции:

### 1. `AIAgentState` как sealed interface (высокоуровневое состояние агента)
Этот интерфейс, located in `ai.koog.agents.core.agent.AIAgentState` [ИСТОЧНИК 1], моделирует **жизненный цикл агента** как конечный автомат. Он содержит пять конкретных состояний:

*   `NotStarted<Output>`: агент ещё не запущен.
*   `Starting<Output>`: процесс запуска агента в Founder.
*   `Running<Output>`: агент активно выполняет задачи. В этом состоянии доступен `rootContext` (тип `AIAgentContext`), который позволяет взаимодействовать со средой выполнения и управлять состоянием [ИСТОЧНИК 3].
*   `Finished<Output>`: агент успешно завершил выполнение и вернул результат (`result` типа `Output`).
*   `Failed<Output>`: выполнение агента завершилось с исключением (`exception`).

**Как использовать:**
Вы обычно **не создаёте экземпляры этих состояний напрямую**. Вместо этого вы получаете текущее состояние (`AIAgentState<Output>`) из `AIAgentContext` или следите за переходом состояний при запуске агента. Метод `copy()` у каждого состояния позволяет получить его неизменяемую копию (для `Running` при копировании копируется и `rootContext`).

**Пример (логика обработки состояния):**
```kotlin
fun handleAgentState(state: AIAgentState<String>) {
    when (state) {
        is AIgentState.NotStarted -> println("Агент не запущен")
        is AIgentState.Starting -> println("Агент запускается")
        is AIgentState.Running -> {
            println("Агент работает. Доступен контекст: ${state.rootContext}")
            // Использование state.rootContext для управления
        }
        is AIgentState.Finished -> println("Результат: ${state.result}")
        is AIgentState.Failed -> println("Ошибка: ${state.exception.message}")
    }
}
```

### 2. `AIAgentState` как класс (низкоуровневая, волокно-безопасная сущность)
Этот **конкретный класс** находится в `ai.koog.agents.core.agent.entity.AIAgentState` [ИСТОЧНИК 2]. Он представляет **мутабельное состояние**, связанное с **итерациями** выполнения агента. Он реализует `AutoCloseable` и содержит одно поле:
*   `iterations: Int`: счётчик завершённых итераций. Обновляется через делегат `ActiveProperty`, что обеспечивает потокобезопасность при условии, что `isActive == true`.

Этот класс **не используется напрямую**. Вместо этого он управляется через `AIAgentStateManager`.

### 3. `AIAgentStateManager` — основной способ управления состоянием
Этот **класс** (также в файле [ИСТОЧНИК 2]) является основным интерфейсом для работы с "низкоуровневым" состоянием агента в потокобезопасном режиме. Он инкапсулирует `AIAgentState` и использует `Mutex` для эксклюзивного доступа.

**Ключевые методы:**
*   `withStateLock(block: suspend (AIAgentState) -> T): T` [ИСТОЧНИК 2]:
    Это **основной метод для работы с состоянием**. Он гарантирует, что блок кода `block` выполняется с эксклюзивной блокировкой (`mutex.withLock`). Внутри блока вы работаете со снепшотом (`snapshot`) текущего состояния (`state`). После выполнения блока **старый снепшот закрывается** (`state.close()`), а создаётся новый на основе изменённого `iterations`. Это обеспечивает предсказуемость и потокобезопасность.

    ```kotlin
    // Пример увеличения счётчика итераций
    val newIterations = context.stateManager.withStateLock { snapshot ->
        val current = snapshot.iterations
        // Логика на основе текущих итераций...
        current + 1 // Новое значение, которое будет сохранено в состояние после блокировки
    }
    ```

*   `copy()`: Создаёт потокобезопасную копию менеджера состояния (используется внутренне).

**Как получить доступ:**
`AIAgentContext` имеет свойство `stateManager: AIAgentStateManager` [ИСТОЧНИК 4], которое предоставляет доступ к менеджеру состояния. Это центральный механизм для отслеживания количества итераций и применения ограничений на выполнение.

### 4. Использование в тестах: `DummyAIAgentContext`
Для unit-тестов можно использовать `DummyAIAgentContext` (в пакете `testing.tools`), где свойство `stateManager` можно настроить или заменить на мок [ИСТОЧНИК 5].

### Краткое резюме по использованию:

1.  Для **отслеживания жизненного цикла** (запущен/завершён/ошибка) и доступа к `rootContext` в состоянии `Running` используйте **`AIAgentState<Output>` (sealed interface)**, получая его из контекста выполнения агента.
2.  Для **модификации и чтения счётчика итераций** в потокобезопасном режиме используйте **`context.stateManager.withStateLock { ... }`**.
3.  Вы **никогда не создаёте экземпляры `AIAgentState` (класс) или `AIAgentStateManager` вручную** в продакшн-коде. Они создаются и управляются фреймворком Koog. Вы взаимодействуете с ними через `AIAgentContext`.
4.  Для **тестирования** используйте `DummyAIAgentContext` и настраивайте его `stateManager`.

---

### Источники
[ИСТОЧНИК 1] AIAgentState.kt · provides · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentState.kt
[ИСТОЧНИК 2] AIAgentState.kt · AIAgentState · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/AIAgentState.kt
[ИСТОЧНИК 3] AIAgentState.kt · provides · line 41 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentState.kt
[ИСТОЧНИК 4] AIAgentContext.kt · represents · line 80 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentContext.kt
[ИСТОЧНИК 5] DummyAIAgentContext.kt · used · line 383 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-test/src/commonMain/kotlin/ai/koog/agents/testing/tools/DummyAIAgentContext.kt

## Q24: "зачем нужен AIAgentStorage"
### Назначение AIAgentStorage

**AIAgentStorage** — это concurrent-safe (потокобезопасное) key-value хранилище, предназначенное для хранения и управления данными, связанными с конкретным экземпляром AI agent (агента искусственного интеллекта) [ИСТОЧНИК 1]. Его основная цель — предоставить агенту надёжное и типизированное пространство для сохранения промежуточного состояния, конфигурации, результатов вычислений или любых других данных, необходимых для его работы.

#### Ключевые характеристики и reasons "зачем он нужен":

1. **Типобезопасность через типизированные ключи**  
   Хранилище использует параметризованные ключи `AIAgentStorageKey<T>`, где `T` определяет тип хранимого значения. Это гарантирует, что при получении данных из хранилища не произойдёт ошибочного приведения типов, так как компилятор знает тип, связанный с каждым ключом [ИСТОЧНИК 1]. Например, можно создать ключ `createStorageKey<Int>("counter")` и гарантированно получать `Int`.

2. **Потокобезопасность (Concurrent-safe)**  
   Как явно указано в описании класса, реализация хранилища рассчитана на использование из нескольких корутин/потоков одновременно без риска гонок данных или повреждения состояния [ИСТОЧНИК 1]. Это критично для агентов, которые могут параллельно обрабатывать多个 запросов или выполнять асинхронные операции.

3. **Управление жизненным циклом агента**  
   - **Изоляция состояния**: Каждый агент имеет своё собственное хранилище, что предотвращает конфликты между разными экземплярами агентов.
   - **Копирование состояния**: Метод `copy()` позволяет создавать глубокую копию всего хранилища. Это полезно для:
     - Создания снапшотов состояния агента (например, для отката или проверpoints).
     --transfer состояния между агентами (например, при миграции или тестировании) [ИСТОЧНИК 1].
   - **Очистка**: Метод `clear()` позволяет полностью сбросить состояние хранилища, что может потребоваться при перезапуске агента или освобождении ресурсов [ИСТОЧНИК 3].

4. **API с чёткой семантикой операций**  
   Интерфейс `AIAgentStorageAPI` (реализуемый `AIAgentStorage`) предоставляет стандартные операции:
   - `set(key, value)`: сохранение значения по ключу.
   - `get(key)`: получение значения с возвратом `null` при отсутствии.
   - `getValue(key)`: получение значения с выбрасыванием `NoSuchElementException` если ключ отсутствует — для случаев, когда значение обязательно должно быть.
   - `remove(key)`: удаление пары ключ-значение с возвратом удалённого значения.
   - `toMap()` и `putAll(map)`: сериализация/десериализация хранилища для log, caches или передачи данных [ИСТОЧНИК 3].
   
   Эти методы семантически четкие, что упрощает использование в сложных агентских workflow.

5. **Возможные практические применения в рамках фреймворка агентов**:
   - **Хранение промежуточных результатов**: агент может сохранять промежуточные выводы, результаты вызовов инструментов (`Tool`), или состояния внутренних процессов.
   - **Управление контекстом диалога**: в chat-агентах — история сообщений, пользовательские предпочтения.
   - **Кэширование**: результаты дорогостоящих операций (например, вызовов LLM) могут кэшироваться.
   - **Передача данных между шагами агента**: если агент состоит из нескольких этапов (steps), хранилище служит общей mutable областью для передачи данных между ними.
   - **Сессионные данные**: хранение ID сессии, метаданных, временных токенов.

#### Почему именно отдельный класс, а не обычная `Map`?
   - **Инкапсуляция и безопасность**: доступ к данным контролируется через suspend-функции, что позволяет легко добавлять логирование, аудит, валидацию или синхронизацию.
   - **Переносимость**: `expect/actual` объявление (`AIAgentStorage` — expect class) позволяет предоставлять разные реализации для разных платформ (JVM, Native, JS) с оптимизациями под каждую среду [ИСТОЧНИК 1].
   - **Стандартизация**: единый интерфейс (`AIAgentStorageAPI`) для всех компонентов фреймворка, что упрощает тестирование и замену реализации.

#### Связь с другими компонентами (на основе контекста)
   В контексте также упоминается `ToolRegistry` [ИСТОЧНИК 5], который управляет коллекцией инструментов (`Tool`). Логично предположить, что `AIAgentStorage` может использоваться для хранения:
   - **Результатов выполнения инструментов** (например, ответы от внешних API).
   - **Состояния инструментов**, если они mutable.
   - **Конфигурации инструментов** или их кэшей.

   Это типичный паттерн: агент имеетToolRegistry для-statics инструментов и AIAgentStorage для динамических runtime-данных.

### Вывод
`AIAgentStorage` — это фундаментальный компонент для поддержания состояния AI агента в рамках фреймework Koog. Он обеспечивает thread-safe, типизированное и изолированное хранилище, необходимое для реализации сложных, многокомандных агентов, которым требуется сохранять и обновлять данные на протяжении всего жизненного цикла выполнения.

---

### Источники
[ИСТОЧНИК 1] AIAgentStorage.kt · AIAgentStorageKey · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/AIAgentStorage.kt  
[ИСТОЧНИК 2] AIAgentStorage.kt · AIAgentStorageKey · line 44 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/AIAgentStorage.kt  
[ИСТОЧНИК 3] AIAgentStorageAPI.kt · AIAgentStorageAPI · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/AIAgentStorageAPI.kt  
[ИСТОЧНИК 4] AIAgentStorageAPI.kt · AIAgentStorageAPI · line 37 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/AIAgentStorageAPI.kt  
[ИСТОЧНИК 5] ToolRegistry.kt · ToolRegistry · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-tools/src/commonMain/kotlin/ai/koog/agents/core/tools/ToolRegistry.kt

## Q25: "как работает и зона ответственности AIAgentStrategy"
На основе предоставленного контекста невозможно дать полный и точный ответ на вопрос о работе и зоне ответственности интерфейса/класса `AIAgentStrategy`.

**Причина:** В предоставленных фрагментах кода `AIAgentStrategy` упоминается только как тип параметра конструктора (`private val strategy: AIAgentStrategy<Input, Output, TContext>`) в классе `AIAgentRunSessionImpl` [ИСТОЧНИК 2]. Его собственное определение, методы или подробная документация отсутствуют в переданных файлах (`AIAgentRunSessionImpl.kt` и `AIAgentFunctionalContextBaseAPI.kt`). Также встречается в строке лога `strategy.name`, что указывает на наличие свойства `name`.

Косвенно можно предположить, что `AIAgentStrategy` — это интерфейс или абстрактный класс, который инкапсулирует **основную логику выполнения (core execution logic)** агента:
1.  Он принимает контекст (`TContext`) и, судя по обрывку кода в `AIAgentRunSessionImpl`, его метод `execute` возвращает результат типа `Output` [ИСТОЧНИК 2].
2.  Он является ключевым компонентом, который запускается в рамках сессии выполнения (`AIAgentRunSession`).
3.  Имя стратегии (`strategy.name`) используется для логирования и, возможно, для идентификации в пайплайне.

Однако без явного определения `AIAgentStrategy.kt` нельзя точно сказать:
*   Какой единственный метод он объявляет (вероятно, `suspend fun execute(context: TContext): Output`).
*   Какие дополнительные возможности или хуки он предоставляет (например, возможность обрабатывать шаги, управлять памятью, работать с пайплайном).
*   Какие конкретныеstdlib-реализации существуют (например, `SimpleAgentStrategy`, `TreeSearchStrategy` и т.д.).

### Вывод
Для ответа на вопрос требуется исходный код файла, где определен `AIAgentStrategy` (например, `AIAgentStrategy.kt`), или более полный контекст его использования.

### Уточняющий вопрос
Можете ли вы предоставить код файла, в котором объявлен интерфейс/класс `AIAgentStrategy` (или его основную реализацию), чтобы понять его полную сигнатуру методов и назначение?

---
### Источники
[ИСТОЧНИК 1] AIAgentRunSessionImpl.kt · handles · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentRunSessionImpl.kt
[ИСТОЧНИК 2] AIAgentRunSessionImpl.kt · handles · line 33 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentRunSessionImpl.kt
[ИСТОЧНИК 3] AIAgentFunctionalContextBaseAPI.kt · AIAgentFunctionalContextBaseAPI · line 273 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentFunctionalContextBaseAPI.kt
[ИСТОЧНИК 4] AIAgentFunctionalContextBaseAPI.kt · AIAgentFunctionalContextBaseAPI · line 304 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentFunctionalContextBaseAPI.kt
[ИСТОЧНИК 5] AIAgentFunctionalContextBaseAPI.kt · AIAgentFunctionalContextBaseAPI · line 331 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/context/AIAgentFunctionalContextBaseAPI.kt

## Q26: "как работает и зона ответственности AIAgentSubgraph с примерами"
### Зона ответственности AIAgentSubgraph

**AIAgentSubgraph** (в кодовой базе представлен классом `AIAgentSubgraphBase` [ИСТОЧНИК 5]) является фундаментальной единицей для инкапсуляции и выполнения специализированной логики внутри большего графа агента (agent graph). Его основная зона ответственности — управление ограниченным подграфом (subgraph) узлов с чётко определённым началом, концом и целью.

**Ключевые аспекты его работы:**

1.  **Инкапсуляция задачи:** Подграф представляет собой отдельную, самодостаточную задачу или «мозговой штурм» (brainstorm), который агент может выполнять как часть своего общего процесса. Он имеет строго типизированный вход (`TInput`) и выход (`TOutput`) [ИСТОЧНИК 5].
2.  **Управление жизненным циклом:** Он определяет, как начинать (`StartNode`) и заканчивать (`FinishNode`) свою работу [ИСТОЧНИК 5]. Логика, которая связывает начало и конец, определяется при создании подграфа.
3.  **Конфигурация инструментов и LLM:** Для своей работы подграф может:
    *   Использовать собственную стратегию выбора доступных инструментов (`toolSelectionStrategy`) [ИСТОЧНИК 5].
    *   Переопределять модель языковой модели (`llmModel`) и параметры запроса (`llmParams`), которые будут использоваться внутри этого подграфа, отличные от настроек основного агента [ИСТОЧНИК 5].
    *   Применять собственный процессор ответов (`responseProcessor`) для пост-обработки сообщений от LLM [ИСТОЧНИК 2, 3].
4.  **Интеграция в общий пайплайн:** Подграф является узелом (`AIAgentNodeBase`) в большом графе. Его выполнение управляется центральным компонентом `AIAgentGraphPipeline`, который вызывает зарегистрированные обработчики (interceptors) до и после выполнения подграфа [ИСТОЧНИК 1].

---

### Как работает: процесс с примерами

#### 1. Создание подграфа (Фабричный метод)

Подграф обычно не создаётся напрямую через конструктор, а создаётся с помощью удобных DSL-функций-расширений из файла `AIAgentSubgraphExt.kt`. Это делает код декларативным и конфигурируемым.

**Пример 1: Создание подграфа для выполнения одной конкретной задачи (`subgraphWithTask`)**

Эта функция создаёт подграф, который принимает структурированный ввод, определяет задачу через лямбду `defineTask`, использует выбранные инструменты и возвращает структурированный результат через `FinishTool` [ИСТОЧНИК 3].

```kotlin
// Пример: Создание подграфа для анализа тональности текста
val sentimentAnalysisSubgraph = agent.subgraphWithTask<String, SentimentResult>(
    name = "analyzeSentiment",
    inputType = typeToken<String>(),
    outputType = typeToken<SentimentResult>(),
    toolSelectionStrategy = ToolSelectionStrategy.ALL, // Использовать все доступные инструменты
    runMode = ToolCalls.PARALLEL, // Выполнять инструменты параллельно
    defineTask = { text ->
        // Это описание задачи, которое будет отправлено в LLM внутри подграфа
        "Проанализируй тональность следующего текста и верни объект SentimentResult. Текст: '$text'"
    }
)
```

**Пример 2: Создание подграфа через `subgraph` (более общий API)**

Более низкоуровневая функция `subgraph` [ИСТОЧНИК 2] позволяет вручную настроить процесс, задав конечный инструмент (`finishTool`) и выполнив любую логику внутри его блока:

```kotlin
val customSubgraph = agent.subgraph<Input, Output>(name = "customTask") {
    // Настраиваем FinishTool, который завершит подграф и вернёт Output
    val finishTool = FinishTool<Output>(typeToken<Output>())

    // setupSubgraphWithTask — это внутренняя функция, которая настраивает
    // стандартный цикл "запрос -> обработка инструментов -> запрос -> ... -> finishTool"
    setupSubgraphWithTask(
        finishTool = finishTool,
        runMode = ToolCalls.SEQUENTIAL,
        assistantResponseRepeatMax = 5,
        defineTask = { input -> "Сделай что-то с $input и заверши через инструмент finishTool" }
    )
    // Можно добавить дополнительную конфигурацию узлов и потоков
}
```

#### 2. Выполнение подграфа

Подграф вызывается как функция на контексте агента (`IAgentGraphContextBase`). Он принимает входной параметр и возвращает результат. Конкретная реализация `handleRunStep` (которая не показана в предоставленном контексте, но упоминается в заголовке [ИСТОЧНИК 5]) оркестрирует цикл:
1.  Формирует начальное сообщение для LLM на основе `defineTask` и входных данных.
2.  Отправляет запрос LLM (с possibly переопределённой моделью/параметрами).
3.  Обрабатывает ответ: извлекает вызовы инструментов (tool calls).
4.  Выполняет вызовы инструментов в соответствии с `runMode` (параллельно или последовательно [ИСТОЧНИК 3]).
5.  Возвращает результаты инструментов в LLM как новые сообщения.
6.  Повторяет шаги 2-5 до тех пор, пока LLM не вызовет `finishTool`.
7.  Когда `finishTool` вызван, результат его работы (возможно, трансформированный) становится финальным выходом `TOutput` подграфа.

#### 3. Мониторинг и перехват выполнения (Интеграция с пайплайном)

`AIAgentGraphPipeline` позволяет подписаться на события жизненного цикла подграфа для логирования, метрик или модификации поведения [ИСТОЧНИК 1]. Это демонстрирует, как `AIAgentSubgraph` вписан в общую систему.

**Пример: Логирование результата выполнения любого подграфа**

```kotlin
pipeline.interceptSubgraphExecutionCompleted(feature) { eventContext ->
    // eventContext содержит: подграф (subgraph), вход (input), выход (output)
    logger.info("Подграф '${eventContext.subgraph.name}' успешно завершён. " +
                "Вход: ${eventContext.input}, Выход: ${eventContext.output}")
}
```

**Пример: Логирование ошибки в подграфе**

```kotlin
pipeline.interceptSubgraphExecutionFailed(feature) { eventContext ->
    logger.error("Подграф '${eventContext.subgraph.name}' упал с ошибкой: " +
                 "${eventContext.throwable.message}", eventContext.throwable)
}
```

---

### Итог

*   **Зона ответственности:** `AIAgentSubgraph` — это **инкапсулированный, переиспользуемый блок бизнес-логики** в графе агента. Он отвечает за выполнение одной целенаправленной задачи (например, «запланировать действия», «проверить факты», «сгенерировать код») с собственной конфигурацией инструментов и, при необходимости, LLM.
*   **Как работает:** Создаётся через DSL (`subgraph`/`subgraphWithTask`), внутри которого задаётся `defineTask` и `finishTool`. Его ядро (`handleRunStep`) реализует цикл взаимодействия «агент-инструменты» до завершения по `finishTool`. Его выполнение наблюдается и управляется внешним `AIAgentGraphPipeline`.
*   **Примеры использования:**
    1.  `subgraphWithTask` для простых задач с типизированным результатом.
    2.  Создание отдельного подграфа для сложного, многошагового процесса, который нужно вызывать из разных мест основного графа агента.
    3.  Подграф с переопределённой моделью LLM (например, дешёвой для планирования, дорогой для генерации финального ответа).
    4.  Настройка пайплайна на прослушивание событий `interceptSubgraphExecutionCompleted` для записи хода выполнения в базу данных или отправки уведомлений.

### Источники
[ИСТОЧНИК 1] AIAgentGraphPipeline.kt · manages · line 268 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/pipeline/AIAgentGraphPipeline.kt
[ИСТОЧНИК 2] AIAgentSubgraphExt.kt · providing · line 165 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/AIAgentSubgraphExt.kt
[ИСТОЧНИК 3] AIAgentSubgraphExt.kt · providing · line 197 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/AIAgentSubgraphExt.kt
[ИСТОЧНИК 4] AIAgentSubgraphExt.kt · providing · line 547 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/ext/agent/AIAgentSubgraphExt.kt
[ИСТОЧНИК 5] AIAgentSubgraph.kt · for · line 33 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/AIAgentSubgraph.kt

## Q27: "расскажи все про ExecutionPointNode"
На основе предоставленного контекста, я могу рассказать только об **`ExecutionPointNode`** как об интерфейсе, поскольку остальные классы (RegisteredFeature, MySQLJdbcPersistenceStorageProvider, PostgresJdbcPersistenceStorageProvider, NoMemory) не имеют прямого отношения к этому конкретному вопросу.

### ExecutionPointNode: суть и назначение

`ExecutionPointNode` — это **интерфейс**, который управляет и принудительно задает "точки выполнения" (execution points) в графе (graph) выполнения AI-агента [ИСТОЧНИК 1]. Его основная задача — предоставить возможность явно указать, какой узел графа агента должен быть выполнен следующим, с какими входными данными, тем самым переопределяя стандартный, обычно линейный или определяемый логикой агента, поток выполнения.

Этот механизм полезен в сценариях, таких как:
*   Восстановление состояния (checkpoint/restore) — после сохранения состояния агента можно принудительно вернуться к выполнению на конкретном узле.
*   Отладка и тестирование — возможность запустить выполнение с определенного узла с заданными входными данными.
*   Интерактивное управление — вмешательство в процесс выполнения агента извне.

### Ключевые компоненты

1.  **`ExecutionPoint` (data class)**: Представляет собой конкретную точку выполнения. Состоит из двух полей [ИСТОЧНИК 1]:
    *   `node: AIAgentNodeBase<*, *>` — обязательный узел в графе стратегии (strategy graph) агента, который должен быть выполнен.
    *   `input: Any? = null` — необязательные входные данные, которые будут переданы в этот узел при его выполнении.

2.  **`AIAgentNodeBase<*, *>`**: Это тип узла, используемый в `ExecutionPointNode`. Судя по сигнатуре, это является базовым классом (или интерфейсом) для всех узлов в графе агента, определяющих операцию и связанную с ней метадату. **В предоставленном контексте детали реализации `AIAgentNodeBase` отсутствуют.**

### Публичные методы интерфейса

Интерфейс предоставляет три ключевых операции:

1.  **`getExecutionPoint(): ExecutionPoint?`** [ИСТОЧНИК 1]
    *   Возвращает текущую *принудительно заданную* точку выполнения, если таковая имеется.
    *   Возвращает `null`, если никакой узел не был принудительно назначен (т.е. агент работает в обычном режиме).

2.  **`enforceExecutionPoint(node: AIAgentNodeBase<*, *>, input: Any? = null)`** [ИСТОЧНИК 1]
    *   **Основная метод для установки принудительной точки.**
    *   Параметры: узел (`node`), который нужно выполнить, и необязательные входные данные (`input`).
    *   После вызова этого метода система будет стремиться выполнить именно указанный узел с указанными данными, игнорируя стандартную логику перехода между узлами.

3.  **`resetExecutionPoint()`** [ИСТОЧНИК 1]
    *   Сбрасывает (очищает) текущую принудительно заданную точку выполнения.
    *   После вызова система возвращается к своему стандартному поведению (ветвлению по логике агента, результатам LLM и т.д.).

### Ограничения контекста

*   **Отсутствие контекста использования:** В предоставленном фрагменте показан только контракт (интерфейс и data class). Неясно, **какой конкретно класс/компонент в системе реализует этот интерфейс** и как именно он интегрируется в цикл выполнения (execution loop) агента.
*   **Отсутствие `AIAgentNodeBase`:** Для полного понимания необходимо знать структуру и назначение `AIAgentNodeBase`, которая не описана в данном контексте.
*   **Отсутствие примера использования:** Нет примеров того, как именно `enforceExecutionPoint` вызывается (например, извне, из другой части агента, из системы восстановления состояния) и как механизм "принуждения" сочетается с обычным потоком управления.

### Вывод

`ExecutionPointNode` — это **управленческий интерфейс для перехвата и перенаправления потока выполнения графа AI-агента**. Он позволяет задать "приоритетную" точку входа в граф с конкретными данными, что является мощным механизмом для восстановления состояния, тестирования и управления. Для понимания его полной роли в архитектуре агента требуется больше контекста о реализующем его классе и о системе узлов (`AIAgentNodeBase`), в которой он работает.

### Источники
[ИСТОЧНИК 1] ExecutionPointNode.kt · provides · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/ExecutionPointNode.kt

## Q28: "расскажи все про SubgraphMetadata"
### Подробный анализ класса `SubgraphMetadata`

#### 1. **Назначение и общая концепция**
`SubgraphMetadata` — это **data class**, предназначенный для хранения и предоставления метаданных о **сабграфе (subgraph)** в рамках стратегического графа AI-агента [ИСТОЧНИК 1]. Основная его роль — инкапсулировать информацию о структурном составе сабграфа и о уникальности имен его узлов. Это позволяет системе анализировать и управлять сабграфами на уровне метаданных, не трогая непосредственно исполняемые узлы.

#### 2. **Структура класса**
Класс имеет два публичных свойства:
- **`nodesMap: Map<String, AIAgentNodeBase<*, *>>`**  
  Это карта (словарь), где:
  - **Ключ** (`String`) — имя узла в сабграфе.
  - **Значение** (`AIAgentNodeBase<*, *>`) — экземпляр базового класса узла AI-агента.  
  Карта `nodesMap` собственно и представляет структуру сабграфа, описывая, какие узлы (и с какими именами) в него входят [ИСТОЧНИК 1].

- **`uniqueNames: Boolean = false`**  
  Флаг, указывающий, гарантирована ли уникальность имен узлов внутри сабграфа. Если `true` — все имена в `nodesMap` гарантированно различны. Это свойство полезно для операций, требующих однозначной идентификации узлов (например, при объединении или сравнении сабграфов), так как помогает избежать конфликтов имён [ИСТОЧНИК 1].

#### 3. **Особенности реализации**
- Как **data class** в Kotlin, `SubgraphMetadata` автоматически получает стандартные методы:
  - `equals(other: Any?)` — сравнение по значению (сравнивает `nodesMap` и `uniqueNames`).
  - `hashCode()` — хэш-код, совместимый с `equals`.
  - `toString()` — строковое представление, включающее все свойства.
  Эти методы генерируются компилятором на основе свойств класса [ИСТОЧНИК 1].

#### 4. **Взаимодействие с другими компонентами системы**
Хотя в предоставленном контексте нет прямых примеров использования `SubgraphMetadata` (например, его создания или передачи в методы), его назначение вытекает из архитектуры:
- **`AIAgentGraphPipeline`** (менеджер выполнения графа) оперирует сабграфами (`AIAgentSubgraphBase`) и связанными событиями (например, `onSubgraphExecutionCompleted`, `onSubgraphExecutionFailed`). В этих методах сабграф передаётся как объект `AIAgentSubgraphBase<*, *>`, который, вероятно, содержит или может быть связан с `SubgraphMetadata` для доступа к метаданным о своей структуре [ИСТОЧНИК 2, 3].
- **События выполнения сабграфов** (например, `SubgraphExecutionCompletedEvent`, `SubgraphExecutionStartingEvent`) передают информацию о запущенном сабграфе (его имя, входные/выходные данные). `SubgraphMetadata` мог бы использоваться для более детального описания сабграфа (например, какие узлы в нём присутствуют) в рамках этих событий или контекстов обработки [ИСТОЧНИК 4, 5].
- **Контексты обработчиков событий** (например, `SubgraphExecutionCompletedContext`) содержат ссылку на `subgraph: AIAgentSubgraphBase<*, *>`. Если `AIAgentSubgraphBase` имеет свойство `metadata` типа `SubgraphMetadata`, то через этот контекст обработчики могут получать доступ к метаданным сабграфа [ИСТОЧНИК 5].

#### 5. **Непрояснённые аспекты (на основе ограниченного контекста)**
- **Точное место создания `SubgraphMetadata`**: В предоставленных файлах нет явного кода, где создаётся экземпляр `SubgraphMetadata` (например, при построении графа или сабграфа). Вероятно, это происходит внутри класса `AIAgentSubgraphBase` или в фабриках/билдерах графа, которые не представлены в контексте.
- **Практическое использование `uniqueNames`**: Не показано, как именно флаг `uniqueNames` используется в логике пайплайна или обработчиках. Возможно, он используется для валидации сабграфа или оптимизации операций с ним.
- **Связь `nodesMap` с исполнением**: Неясно, влияет ли `nodesMap` на порядок или способ выполнения узлов в сабграфе. Судя по описанию, это скорее статическая структурная информация.

#### 6. **Роль в системе AI-агента**
`SubgraphMetadata` служит **инструментом рефлексии и управления** на уровне графа стратегий:
- Позволяет системе (или разработчикам) получать информацию о составе сабграфа без необходимости исполнения.
- Может использоваться для:
  - Валидации целостности сабграфа (например, проверка уникальности имён).
  - Логирования и отладки (отображение структуры сабграфа).
  - Динамического анализа графа (например, поиск узлов по имени, если имена уникальны).
  - Разделения композиции графа (структура) и его поведения (исполнение узлов).

---

### Источники
[ИСТОЧНИК 1] SubgraphMetadata.kt · holds · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/entity/SubgraphMetadata.kt  
[ИСТОЧНИК 2] AIAgentGraphPipeline.kt · manages · line 147 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/pipeline/AIAgentGraphPipeline.kt  
[ИСТОЧНИК 3] AIAgentGraphPipeline.kt · manages · line 268 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/pipeline/AIAgentGraphPipeline.kt  
[ИСТОЧНИК 4] subgraphExecutionEvents.kt · SubgraphExecutionStartingEvent · line 38 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/model/events/subgraphExecutionEvents.kt  
[ИСТОЧНИК 5] SubgraphExecutionEventContext.kt · SubgraphExecutionEventContext · line 38 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/handler/subgraph/SubgraphExecutionEventContext.kt

## Q29: "примеры использования AIAgentSubgraphBuilder"
### Примеры использования AIAgentSubgraphBuilder

На основе предоставленного контекста можно выделить несколько ключевых аспектов использования `AIAgentSubgraphBuilder` и связанных с ним классов. Поскольку `AIAgentSubgraphBuilder` является абстрактным классом [ИСТОЧНИК 2], его прямое использование предполагает либо наследование, либо использование более высокоуровневых DSL-функций, которые сами создают и настраивают builder'а, как это делает `subgraphWithTask` из `AIAgentSubgraphExt` [ИСТОЧНИК 1]. Ниже рассмотрены практические сценарии.

---

#### 1. Создание подграфа с задачей через `subgraphWithTask` (рекомендуемый способ)

Функция-расширение `subgraphWithTask` (из `AIAgentSubgraphExt`) предоставляет удобный DSL для создания подграфа, который выполняет конкретную задачу с использованием финишного инструмента (`finishTool`). Она внутренне использует `AIAgentSubgraphBuilder` для конфигурации.

**Пример:**
```kotlin
// Определение подграфа для задачи "поиска"
subgraphWithTask(
    inputType = typeToken<SearchInput>(),
    toolSelectionStrategy = ToolSelectionStrategy.Required, // или другая стратегия
    finishTool = searchResultTool, // инструмент, возвращающий результат поиска
    name = "search-subgraph",
    llmModel = myModel,
    llmParams = LLMParams(temperature = 0.7),
    runMode = ToolCalls.SEQUENTIAL,
    defineTask = { input: SearchInput ->
        // Логика определения задачи для LLM на основе входных данных
        "Найди информацию о: ${input.query}"
    }
) // Возвращает AIAgentSubgraphDelegate<SearchInput, SearchResult>
```

**Ключевые параметры [ИСТОЧНИК 1]:**
- `inputType`: Тип входных данных для подграфа (используется `TypeToken`).
- `toolSelectionStrategy`: Стратегия выбора инструментов (например, обязательный или разрешённый).
- `finishTool`: Инструмент, который завершает подграф и преобразует результат в `OutputTransformed`.
- `defineTask`: Лямбда, которая на основе входного `input` формирует текстовое описание задачи для LLM.
- `llmModel` и `llmParams`: Опциональные параметры модели и её настройки.

Этот подход абстрагирует от прямого взаимодействия с `AIAgentSubgraphBuilder`, но фактически использует его внутри для создания структуры подграфа [ИСТОЧНИК 1].

---

#### 2. Ручное построение подграфа через `AIAgentSubgraphBuilder` (низкоуровнево)

`AIAgentSubgraphBuilder` (конкретнее, его вероятная реализация, например `AIAgentSubgraphBuilderImpl`) позволяет явно определять узлы подграфа (`nodeStart` и `nodeFinish`) и соединять их через промежуточные узлы с помощью метода `edge` [ИСТОЧНИК 2, 3]. Однако в предоставленном контексте нет примера прямой инстанциации builder'а, так как он абстрактный. Типичный сценарий — использование внутри DSL или других расширений.

Гипотетический пример (основан на API `AIAgentSubgraphBuilderBase` [ИСТОЧНИК 2, 3]):
```kotlin
val builder = object : AIAgentSubgraphBuilder<Input, Output>(
    name = "custom",
    inputType = typeToken<Input>(),
    outputType = typeToken<Output>(),
    toolSelectionStrategy = ToolSelectionStrategy.Auto,
    llmModel = null,
    llmParams = null
) {
    override fun build(): AIAgentSubgraphDelegate<Input, Output> {
        // Здесь конфигурируются узлы и соединения
        // Например, добавление LLM-ноды:
        val llmNode = nodeLLMRequest(
            prompt = "Обработай входные данные",
            model = llmModel ?: defaultModel
        )
        // Соединение стартового узла с LLM-нодой
        edge(builder.nodeStart to llmNode)
        // Соединение LLM-ноды с финишным узлом
        edge(llmNode to builder.nodeFinish)
        return // ... сконфигурированный делегат
    }
}
```
**Примечание:** Прямой вызов `AIAgentSubgraphBuilder` в пользовательском коде маловероятен — обычно используется через `subgraph { ... }` или `subgraphWithTask` [ИСТОЧНИК 1]. Доступные методы для построения (например, `nodeLLMRequest`, `nodeTool`) не показаны в контексте, но они предположительно существуют в расширениях DSL.

---

#### 3. Обработка событий выполнения подграфа через `AIAgentGraphPipeline`

`AIAgentGraphPipeline` управляет выполнением графа и позволяет перехватывать события жизненного цикла подграфов, включая успешное выполнение и ошибки [ИСТОЧНИК 4]. Это полезно для логирования, мониторинга или дополнительной обработки.

**Пример:**
```kotlin
// В конфигурации агента или пайплайна
pipeline.interceptSubgraphExecutionCompleted(feature) { eventContext ->
    val subgraphName = eventContext.subgraph.name
    val input = eventContext.input
    val output = eventContext.output
    logger.info("Подграф '$subgraphName' успешно выполнен. Вход: $input, Выход: $output")
}

pipeline.interceptSubgraphExecutionFailed(feature) { eventContext ->
    val subgraphName = eventContext.subgraph.name
    val error = eventContext.throwable
    logger.error("Подграф '$subgraphName' завершился с ошибкой: ${error.message}", error)
}
```
**Контекст события [ИСТОЧНИК 5]:**
- `SubgraphExecutionCompletedContext`: Содержит `subgraph`, `input`, `output`, типы данных и информацию об исполнении.
- `SubgraphExecutionFailedContext`: Содержит `subgraph`, `input`, `throwable` (исключение) и типы.

---

#### 4. Работа с типами через `TypeToken`

Во всех примерах используется `TypeToken` для передачи типов в runtime (вместо устаревших `KType` [ИСТОЧНИК 3]). Например:
```kotlin
inputType = typeToken<MyData>()
outputType = typeToken<MyResult>()
```
Это необходимо для корректной работы системы типов в графе.

---

### Важные замечания

1. **Абстрактность `AIAgentSubgraphBuilder`**: Прямое использование класса маловероятно. Основной способ создания подграфа — через функции-расширения, такие как `subgraphWithTask` [ИСТОЧНИК 1] или, возможно, `subgraph` (не показан в контексте, но упомянут).
2. **Неполный контекст**: В предоставленных фрагментах нет полной реализации методов `build()` в `AIAgentSubgraphBuilder` или деталей добавления узлов (например, `nodeLLMRequest`). Для полного понимания требуется изучение дополнительных исходных файлов (например, `AIAgentSubgraphExt` полностью, или DSL-файлы).
3. **Депрекейт `KType`**: Конструкторы с `KType` устарели, используйте `TypeToken` [ИСТОЧНИК 3].

---

### Рекомендации по использованию
- Для типовой задачи с финишным инструментом используйте `subgraphWithTask` [ИСТОЧНИК 1].
- Для кастомного графа, если требуются сложные цепочки узлов, нужно изучать DSL-функции, которые добавляются в контекст `AIAgentGraphContextBase` (например, `nodeLLMRequest`, `nodeTool`). Они находятся в других файлах, не вошедших в текущий контекст.
- Для отслеживания работы подграфов настраивайте перехватчики в `AIAgentGraphPipeline` [ИСТОЧНИК 4] с использованием контекстов `SubgraphExecutionEventContext` [ИСТОЧНИК 5].

Если нужны примеры с конкретными узлами (например, `nodeLLMRequestMultiple` из `AIAgentSubgraphExt` [ИСТОЧНИК 1]), требуется дополнительный контекст по DSL-методам.

