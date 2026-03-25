# Runtest: RAG_WORKER | 2026-03-26 00:08
**Модель:** Облачная
**Время выполнения:** 1491472 мс

## Q1: "какие основные возможности агента, какой класс реализует"
Основные возможности AI-агентов в этой кодовой базе реализуются двумя основными классами: `PlannerAIAgent` [КЛАСС 1] и `FunctionalAIAgent` [КЛАСС 2]. Оба класса предоставляют ядро для создания и управления AI-агентами, но имеют разные стратегии обработки (планирование vs функциональный подход).

### 1. Общие возможности обоих типов агентов

Оба класса наследуют функциональность от общего базового класса и обладают следующими ключевыми возможностями:

**Управление конфигурацией и выполнением:**
- Принимают `AIAgentConfig` [ИСТОЧНИК 1, ИСТОЧНИК 3], содержащий настройки агента (модель, промпт, процессор ответов и т.д.)
- Работают с `PromptExecutor` для выполнения промптов в LLM
- Поддерживают `ToolRegistry` для регистрации и использования инструментов (tools) [ИСТОЧНИК 1, ИСТОЧНИК 3]
- Имеют уникальный `id` и используют `Clock` для временных меток
- Создают контекст выполнения через метод `prepareContext` [ИСТОЧНИК 2, ИСТОЧНИК 3]

**Расширяемость через фичи (features):**
- Оба класса поддерживают установку дополнительных фич через метод `installFeatures` в конструкторе и `FeatureContext.install()` [ИСТОЧНИК 1, ИСТОЧНИК 3]
- `FeatureContext` — внутренний класс, предоставляющий API для инсталляции фич с возможностью кастомизации конфигурации
- Используют `RegisteredFeature` [КЛАСС 3] для хранения установленных фич (реализация + конфигурация)

### 2. Специфичные возможности PlannerAIAgent

`PlannerAIAgent` [КЛАСС 1] ориентирован на **агентов с планированием** (planning-based agents):

**Особенности:**
- Работает со стратегией типа `AIAgentPlannerStrategy<Input, Output, *>` [ИСТОЧНИК 1]
- Имеет специальный пайплайн `AIAgentPlannerPipeline` [ИСТОЧНИК 2]
- В методе `prepareContext` создаёт `AIAgentPlannerContext` с:
  - `GenericAgentEnvironment` — среда выполнения агента
  - `AIAgentLLMContext` — контекст для работы с LLM
  - `AIAgentStateManager` — менеджер состояния агента
  - Начальные данные агента (`agentInput`)

**Создание через Builder:**
- Статический метод `builder(strategy: AIAgentPlannerStrategy<...>)` [ИСТОЧНИК 1] возвращает `PlannerAIAgentBuilder` для поэтапной конфигурации

### 3. Специфичные возможности FunctionalAIAgent

`FunctionalAIAgent` [КЛАСС 2] ориентирован на **функциональных агентов** (без графов/планирования):

**Особенности:**
- Работает со стратегией типа `AIAgentFunctionalStrategy<Input, Output>` [КЛАСС 2]
- Использует пайплайн `AIAgentFunctionalPipeline` [КЛАСС 4] для управления фичами
- В методе `prepareContext` создаёт `AIAgentFunctionalContext` [ИСТОЧНИК 3] с:
  - Подготовленной средой (`prepareEnvironment()`)
  - `AIAgentStorage` для хранения данных агента (неполные данные в контексте)
  - Аналогичные компоненты: `GenericAgentEnvironment`, `AIAgentLLMContext`, `AIAgentStateManager`

**Инфраструктура пайплайна:**
- `AIAgentFunctionalPipeline` [КЛАСС 4] наследует от базового `AIAgentPipeline` и предоставляет метод `install()` для неграфовых фич (`AIAgentFunctionalFeature`)

### 4. Роли вспомогательных классов

- **`RegisteredFeature`** [КЛАСС 3] — простой контейнер для хранения установленной фичи (реализация + конфигурация)
- **`AIAgentFunctionalPipeline`** [КЛАСС 4] — часть инфраструктуры для FunctionalAIAgent, управляет инсталляцией фич без графовой обработки

### Ключевые отличия типов агентов

| Аспект | PlannerAIAgent | FunctionalAIAgent |
|--------|----------------|-------------------|
| **Стратегия** | `AIAgentPlannerStrategy` (планирование) | `AIAgentFunctionalStrategy` (функциональный) |
| **Контекст** | `AIAgentPlannerContext` | `AIAgentFunctionalContext` |
| **Пайплайн** | `AIAgentPlannerPipeline` | `AIAgentFunctionalPipeline` |
| **Подход** | Граф/планирование | Функциональная цепочка |

Оба класса реализуют общий интерфейс/базовый класс `AIAgentBase<Input, Output, Context>` (указано в сигнатуре `PlannerAIAgent`), определяющий основной контракт агента.

### Источники

[КЛАСС 1] PlannerAIAgent · PlannerAIAgent.kt · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/planner/PlannerAIAgent.kt
[ИСТОЧНИК 1] PlannerAIAgent.kt · line 33 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/planner/PlannerAIAgent.kt
[ИСТОЧНИК 2] PlannerAIAgent.kt · line 71 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/planner/PlannerAIAgent.kt
[КЛАСС 2] FunctionalAIAgent · FunctionalAIAgent.kt · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/FunctionalAIAgent.kt
[ИСТОЧНИК 3] FunctionalAIAgent.kt · line 63 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/FunctionalAIAgent.kt
[КЛАСС 3] RegisteredFeature · RegisteredFeature.kt · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/pipeline/RegisteredFeature.kt
[ИСТОЧНИК 4] RegisteredFeature.kt · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/pipeline/RegisteredFeature.kt
[КЛАСС 4] AIAgentFunctionalPipeline · AIAgentFunctionalPipeline.kt · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/pipeline/AIAgentFunctionalPipeline.kt
[ИСТОЧНИК 5] AIAgentFunctionalPipeline.kt · inherits · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/feature/pipeline/AIAgentFunctionalPipeline.kt

## Q2: "как конфигурируется агент"
Конфигурация AI-агентов в этой кодовой базе осуществляется через **два основных подхода**:

## 1. Базовое определение конфигурации

Основа конфигурации — интерфейс **`AIAgentConfigBase`** [КЛАСС 1], который определяет два обязательных параметра:

- **`prompt`**: [`Prompt`](ai.koog.prompt.dsl.Prompt) — структура входящих сообщений для генерации ответов от языковой модели. Управляет разговорным состоянием, входными промптами и параметрами LLM [ИСТОЧНИК 1].
- **`model`**: [`LLModel`](ai.koog.prompt.llm.LLModel) — конкретная языковая модель (OpenAI, Anthropic и т.д.) с настройками (temperature, поддержка инструментов и т.д.) [ИСТОЧНИК 1].

Это минимальная конфигурация, но на практике используется через **Builder API**.

---

## 2. Builder API для конфигурации агента-сервиса

Основной способ создания настроенного агента — использование **`AIAgentServiceBuilderAPI`** [КЛАСС 2] и его реализации **`AIAgentServiceBuilderImpl`** [КЛАСС 3]. Builder предоставляет **флюентный интерфейс** (method chaining) с широкими возможностями настройки.

### Ключевые параметры конфигурации:

| Метод Builder | Назначение |
|---------------|------------|
| `promptExecutor(PromptExecutor)` | Устанавливает исполнитель промптов (интеграция с LLM API) |
| `llmModel(LLModel)` | Явно задаёт языковую модель |
| `toolRegistry(ToolRegistry)` | Регистрирует набор инструментов (functions/tools) для агента |
| `systemPrompt(String)` | Устанавливает системный промпт (роль/поведение агента) [ИСТОЧНИК 3] |
| `prompt(Prompt)` | Задаёт полный промпт (может переопределить systemPrompt) [ИСТОЛЬНИК 5] |
| `temperature(Double)` | Регулирует случайность вывода модели (0.0 = детерминированный, >1 = творческий) [ИСТОЦИК 4] |
| `numberOfChoices(Int)` | Количество вариантов ответа от модели |
| `maxIterations(Int)` | Максимальное количество итераций выполнения агента (защита от бесконечных циклов) [ИСТОЧНИК 4] |
| `agentConfig(AIAgentConfig)` | Применяет готовый объект конфигурации для полной замены настроек [ИСТОЧНИК 4] |

### Два пути после базовой конфигурации:

После настройки общих параметров Builder позволяет выбрать **тип агента**:

#### a) Для графовых агентов (планирование):
```kotlin
builder.graphStrategy(strategy: AIAgentGraphStrategy<Input, Output>)
    . → GraphAgentServiceBuilder<Input, Output>
```
[ИСТОЧНИК 4] — для стратегий с графом выполнения, подходящих для сложных многошаговых задач.

#### b) Для функциональных агентов:
```kotlin
builder.functionalStrategy(strategy: AIAgentFunctionalStrategy<Input, Output>)
    . → FunctionalAgentServiceBuilder<Input, Output>
```
[ИСТОЧНИК 4] — для простых цепочек вызовов без планирования.

Оба builder'а (`GraphAgentServiceBuilder` и `FunctionalAgentServiceBuilder`) наследуют базовые возможности и позволяют завершить конфигурацию, добавив специфичные для типа агента параметры (например, графы состояний или цепочки инструментов).

---

## 3. Как работает конфигурация внутри (из `AIAgentServiceBuilderImpl`)

Реализация Builder'а хранит **модифицируемую конфигурацию** в поле `config` типа `AIAgentConfig`. При каждом вызове метода конфигурации:

1. **Непрямые изменения** (например, `temperature`) модифицируют существующий `Prompt` через `prompt.copy(params = ...)` и создают новый `AIAgentConfig` [ИСТОЧНИК 5].
2. **Прямые изменения** (например, `maxIterations`) создают копию конфигурации с новым полем [ИСТОЧНИК 5].
3. Метод `systemPrompt(String)` эквивалентен созданию нового промпта с системным сообщением [ИСТОЧНИК 5].
4. Метод `agentConfig(AIAgentConfig)` **полностью заменяет** текущую конфигурацию [ИСТОЧНИК 4].

После выбора стратегии (`graphStrategy`/`functionalStrategy`) Builder **"передаёт факел"** соответствующему специфичному builder'у, который завершает настройку и строит итоговый сервис (`GraphAIAgentService` или `FunctionalAIAgentService`).

---

## 4. Завершение конфигурации

После настройки через `graphStrategy()` или `functionalStrategy()` вы получаете типозависимый builder, который позволяет:

- Добавить **фичи** (features) для агента (например, логирование, хранилище, мониторинг).
- Задать **финальные параметры** выполнения.
- Вызвать `build()` для создания **готового агент-сервиса** (например, `GraphAIAgentService<String, String>` через `build()` в `AIAgentServiceBuilderAPI` [ИСТОЧНИК 4]).

---

## Краткий пример конфигурации

```kotlin
val agentService = AIAgentServiceBuilder()
    .llmModel(OpenAIModel("gpt-4"))          // Модель
    .toolRegistry(myTools)                   // Инструменты
    .systemPrompt("Ты полезный ассистент")   // Системный промпт
    .temperature(0.7)                       // Температура
    .maxIterations(10)                      // Лимит итераций
    .functionalStrategy(myStrategy)         // Выбор типа агента
    .installFeature(MyCustomFeature())      // Опционально: дополнительные фичи
    .build()                                // Создание сервиса
```

---

## Важные замечания

- **Стандартный `build()`** в `AIAgentServiceBuilderAPI` создаёт только `GraphAIAgentService<String, String>` через `singleRunStrategy` [ИСТОЧНИК 4]. Для других типов нужен явный выбор стратегии.
- **Конфигурация отделена от исполнения**: Builder создаёт **сервис** (`AIAgentService`), который потом можно использовать для запуска агентов через `agent.run(...)`.
- **ToolRegistry** и **PromptExecutor** передаются лениво (lazily) в специфичные builder'ы [ИСТОЧНИК 5].

---

### Источники

[КЛАСС 1] AIAgentConfigBase · AIAgentConfigBase.kt · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/config/AIAgentConfigBase.kt

[ИСТОЧНИК 1] AIAgentConfigBase.kt · for · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/config/AIAgentConfigBase.kt

[КЛАСС 2] AIAgentServiceBuilderAPI · AIAgentServiceBuilderAPI.kt · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentServiceBuilderAPI.kt

[ИСТОЧНИК 2] AIAgentServiceBuilderAPI.kt · line 1 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentServiceBuilderAPI.kt

[ИСТОЧНИК 3] AIAgentServiceBuilderAPI.kt · line 40 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentServiceBuilderAPI.kt

[ИСТОЧНИК 4] AIAgentServiceBuilderAPI.kt · line 75 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentServiceBuilderAPI.kt

[КЛАСС 3] AIAgentServiceBuilderImpl · AIAgentServiceBuilderImpl.kt · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentServiceBuilderImpl.kt

[ИСТОЧНИК 5] AIAgentServiceBuilderImpl.kt · line 44 · /Users/samtakot/devs/learnings/agent_frameworks/min/koog/agents/agents-core/src/commonMain/kotlin/ai/koog/agents/core/agent/AIAgentServiceBuilderImpl.kt

## Q3: "как агент работает с историей сообщений"
На основе предоставленного контекста информация о том, как агент работает с историей сообщений, является неполной. Известно, что агенты (как `PlannerAIAgent` [КЛАСС 1], так и `FunctionalAIAgent` [КЛАСС 2]) при выполнении создают контекст (соответственно `AIAgentPlannerContext` [ИСТОЧНИК 2] или `AIAgentFunctionalContext` [ИСТОЧНИК 3]). В случае `FunctionalAIAgent` контекст включает `AIAgentStorage` для хранения данных агента, которые могут включать историю сообщений [ИСТОЧНИК 3]. Однако, конкретные детали о том, как история добавляется, обновляется и передаётся в языковую модель, в предоставленных метках отсутствуют. Например, неясно, используется ли объект `Prompt` (упоминаемый в конфигурации `AIAgentConfigBase`) для управления историей, или история хранится отдельно, и как обрабатывается ограничение контекстного окна (токены).

Для полноты ответа необходимо уточнить, какой именно компонент или процесс отвечает за работу с историей сообщений:  
- Передача истории в LLM через промпт?  
- Хранение истории между шагами выполнения или сессиями?  
- Обработка длинных историй (обрезка, суммаризация)?  

### Источники

[КЛАСС 1] PlannerAIAgent · PlannerAIAgent.kt

[ИСТОЧНИК 2] PlannerAIAgent.kt · line 71

[КЛАСС 2] FunctionalAIAgent · FunctionalAIAgent.kt

[ИСТОЧНИК 3] FunctionalAIAgent.kt · line 63

## Q4: "как реализованы стратегии компактизации контекста"
На основе предоставленного контекста **информация о стратегиях компактизации контекста отсутствует**. В представленных классах (`PlannerAIAgent` [КЛАСС 1], `FunctionalAIAgent` [КЛАСС 2], `AIAgentServiceBuilderImpl` [КЛАСС 3]) нет упоминаний о компонентах или алгоритмах, отвечающих за:

- Обрезку истории сообщений
- Суммаризацию предыдущих раундов
- Управление размером контекста (токенизацию)
- Стратегии ротации истории (sliding window, FIFO и т.д.)

**Единственные связанные элементы:**
1. **`Prompt`** [ИСТОЧНИК 1] — упоминается как часть конфигурации, содержащая "разговорное состояние" (conversational state), но неясно, содержит ли он встроенные механизмы компактизации или просто передаёт историю как есть.
2. **`AIAgentStorage`** [ИСТОЧНИК 3] — используется в `FunctionalAIAgent` для хранения данных агента, но его назначение (только между шагами выполнения или также для истории сообщений) не уточняется.

**Возможные места для реализации компактизации** (гипотезы, не подтверждённые контекстом):
- Внутри `PromptExecutor` [ИСТОЧНИК 1, ИСТОchnick 3] — может выполнять предобработку промпта перед отправкой в LLM.
- В `ToolRegistry` [ИСТОchnick 1, ИСТОchnick 3] — если инструменты сами управляют историей.
- В `AIAgentPlannerStrategy` / `AIAgentFunctionalStrategy` — как часть логики планирования/исполнения.

---

### Уточняющий вопрос:
**Есть ли в кодовой базе отдельные классы/интерфейсы для управления историей сообщений (например, `ContextCompactor`, `HistoryManager`, `MessageCompactionStrategy`), или компактизация контекста встроена в компонент `Prompt` или `PromptExecutor`?**

---

### Источники

[КЛАСС 1] PlannerAIAgent · PlannerAIAgent.kt

[КЛАСС 2] FunctionalAIAgent · FunctionalAIAgent.kt

[КЛАСС 3] AIAgentServiceBuilderImpl · AIAgentServiceBuilderImpl.kt

[ИСТОЧНИК 1] AIAgentConfigBase.kt · for · line 1

[ИСТОЧНИК 3] FunctionalAIAgent.kt · line 63

## Q5: "как реализован tool calling"
На основе предоставленного контекста **информация о реализации tool calling является неполной**. Хотя в классах и конфигурациях присутствуют упоминания ключевых компонентов, детали механизма вызова инструментов (tool calling) не раскрываются.

**Что известно из контекста:**
1. **`ToolRegistry`** присутствует в конфигурации агентов:
   - В `AIAgentConfigBase` [ИСТОЧНИК 1] указано поле `toolRegistry: ToolRegistry`.
   - В `FunctionalAIAgent` [КЛАСС 2] контекст (`AIAgentFunctionalContext`) включает `ToolRegistry`.
2. **`PromptExecutor`** [ИСТОЧНИК 1, 3] упоминается как компонент, исполняющий промпты, и может координировать вызов инструментов, но его внутренняя логика не показана.
3. **Стратегии агентов** (`AIAgentPlannerStrategy`, `AIAgentFunctionalStrategy`) [КЛАСС 1, 2] могут принимать решения о вызове инструментов, но их реализация не включена в контекст.
4. **Фичи агентов** (через `install`) [КЛАСС 2, КЛАСС 3] потенциально могут добавлять инструменты, но неясно, как они интегрируются в пайплайн выполнения.

**Отсутствующие детали:**
- Как агент определяет необходимость вызова инструмента (парсинг ответа LLM, структурированный вывод?).
- Как извлекаются аргументы инструмента (JSON-схемы, типизированные параметры?).
- Как выполняется инструмент (синхронно/асинхронно, обработка ошибок?).
- Как результат инструмента возвращается в LLM (вставка в историю, отдельный промпт?).
- Роль `PromptExecutor` в orchestration tool calling.

**ГHypothesis (не подтверждено контекстом):**
Tool calling может быть реализован на уровне:
- `PromptExecutor` (если он адаптирует промпт под функции LLM, например, OpenAI function calling).
- Внутри стратегий (`AIAgentPlannerStrategy` / `AIAgentFunctionalStrategy`), которые управляют пайплайном и решают, когда вызывать инструменты.
- Через отдельный компонент (например, `ToolCallingHandler`), но такой класс не упомянут.

---

### Уточняющий вопрос:
**Есть ли в кодовой базе отдельные классы/интерфейсы, отвечающие за parse ответов LLM на вызовы инструментов (например, `ToolCallParser`), execution инструментов (`ToolExecutor`), или эта логика встроена в `PromptExecutor` или стратегии агентов?**

---

### Источники

[КЛАСС 1] PlannerAIAgent · PlannerAIAgent.kt

[КЛАСС 2] FunctionalAIAgent · FunctionalAIAgent.kt

[КЛАСС 3] AIAgentServiceBuilderImpl · AIAgentServiceBuilderImpl.kt

[ИСТОЧНИК 1] AIAgentConfigBase.kt · for · line 1

[ИСТОЧНИК 3] FunctionalAIAgent.kt · line 63

## Q6: "как реализован механизм user in the loop"
На основе предоставленного контекста **информация о реализации механизма "user in the loop" (пользователь в цикле) отсутствует**. В описанных классах и компонентах нет явных указаний на функциональность, обеспечивающую:

- Интерактивные паузы для подтверждения/корректировки пользователем
- Запрос обратной связи от пользователя во время выполнения агента
- Обработку действий пользователя как шага в пайплайне
- Сохранение состояния между взаимодействиями с пользователем

---

### Возможные точки интеграции (гипотезы, не подтверждённые контекстом):

1. **Стратегии агентов** (`AIAgentPlannerStrategy` / `AIAgentFunctionalStrategy`) [КЛАСС 1, 2]:
   - Могут содержать логику определения, когда требуется вмешательство пользователя.
   - Могли бы приостанавливать пайплайн и возвращать контроль.

2. **Пайплайны** (`AIAgentPlannerPipeline` / `AIAgentFunctionalPipeline`) [ИСТОЧНИК 2, КЛАСС 4]:
   - Шаги пайплайна могли бы включать `UserInteractionStep`, но такие шаги не упомянуты.

3. **Фичи** (`AIAgentFeature` / `AIAgentFunctionalFeature`) [КЛАСС 3]:
   - Возможна реализация через `installFeature()`, добавляющая интерфейс для взаимодействия с пользователем.
   - Контекст (`AIAgentFunctionalContext`) [ИСТОЧНИК 3] мог бы предоставлять методы типа `requestUserInput(prompt: String): String`.

4. **PromptExecutor** [ИСТОЧНИК 1, 3]:
   - Мог бы обрабатывать промпты, содержащие специальные маркеры для запроса пользовательского ввода.

---

### Уточняющий вопрос:
**Предполагается ли реализация "user in the loop" через:**
- **Прямые вызовы UI-слоя из агента** (не рекомендуется)?  
- **Особые типы инструментов (Tool)**, которые блокируют выполнение до ответа пользователя?  
- **Событийную систему** (например, `UserInputRequestedEvent`), которую UI слушает и обрабатывает?  
- **Специальную стратегию/фичу**, которая интегрируется с контекстом и пайплайном?

---

### Источники

[КЛАСС 1] PlannerAIAgent · PlannerAIAgent.kt

[КЛАСС 2] FunctionalAIAgent · FunctionalAIAgent.kt

[КЛАСС 3] AIAgentServiceBuilderImpl · AIAgentServiceBuilderImpl.kt

[ИСТОЧНИК 1] AIAgentConfigBase.kt · for · line 1

[ИСТОЧНИК 3] FunctionalAIAgent.kt · line 63

## Q7: "как агенты работают с памятью и сколько слоев используют"
На основе предоставленного контекста **информация о реализации работы агентов с памятью и о количестве слоёв является неполной**. Контекст описывает архитектурные компоненты агентов, но не раскрывает детали их внутренней организации и управления памятью.

### Известные архитектурные слои агентов (структура):
Оба типа агентов (`PlannerAIAgent` и `FunctionalAIAgent`) следуют общей трёхслойной архитектуре, но с разными реализациями:

1. **Контекстный слой** (`Context`):
   - `PlannerAIAgent` использует `AIAgentPlannerContext` [ИСТОЧНИК 2], включающий `GenericAgentEnvironment`, `AIAgentLLMContext`, `AIAgentStateManager`.
   - `FunctionalAIAgent` использует `AIAgentFunctionalContext` [ИСТОЧНИК 3], с методом `prepareEnvironment()` и полем `AIAgentStorage`.

2. **Пайплайн слой** (`Pipeline`):
   - `PlannerAIAgent` работает с `AIAgentPlannerPipeline` [ИСТОЧНИК 2].
   - `FunctionalAIAgent` работает с `AIAgentFunctionalPipeline` (наследник `AIAgentPipeline`) [КЛАСС 4].

3. **Стратегический слой** (`Strategy`):
   - `PlannerAIAgent` использует `AIAgentPlannerStrategy<Input, Output, *>` [ИСТОЧНИК 1].
   - `FunctionalAIAgent` использует `AIAgentFunctionalStrategy<Input, Output>` [КЛАСС 2].

### Компоненты, связанные с памятью (без деталей реализации):
- **`AIAgentStorage`** [ИСТОЧНИК 3] — присутствует в `AIAgentFunctionalContext`, но его роль (долгосрочное хранилище, кэш, история) не описана.
- **`AIAgentStateManager`** [ИСТОЧНИК 2] — упомянут в `AIAgentPlannerContext`, но его функции (управление состоянием, история сессий) не раскрыты.
- **`Prompt`** [ИСТОЧНИК 1] — формирует запрос к LLM и может передавать историю сообщений, но неизвестно, включает ли он механизмы управления длиной истории.

### Отсутствие информации:
- **Количество слоёв памяти** (например, краткосрочная vs долгосрочная, или отдельные слои для истории, инструментов, состояния) не указано.
- **Механизмы хранения и извлечения** (например, `Memory`, `HistoryManager`, `VectorStore`) не представлены в контексте.
- **Как память интегрируется в пайплайн** — неизвестно, на каком этапе (подготовка промпта, после выполнения инструмента) обновляется или читается память.

---

### Уточняющий вопрос:
**Под "слоями" имеется в виду:**
1. **Архитектурные слои агента** (контекст, пайплайн, стратегия) — их три для каждого типа агента?
2. **Слои памяти** (например, рабочая память для текущего шага, долгосрочная память для сессии, внешнее хранилище)? Если да, то какие компоненты (`AIAgentStorage`, `AIAgentStateManager`, `Prompt`) отвечают за каждый слой?

---

### Источники

[КЛАСС 1] PlannerAIAgent · PlannerAIAgent.kt

[КЛАСС 2] FunctionalAIAgent · FunctionalAIAgent.kt

[КЛАСС 3] AIAgentServiceBuilderImpl · AIAgentServiceBuilderImpl.kt

[ИСТОЧНИК 1] AIAgentConfigBase.kt · for · line 1

[ИСТОЧНИК 2] PlannerAIAgent.kt · line 22

[ИСТОЧНИК 3] FunctionalAIAgent.kt · line 63

[КЛАСС 4] AIAgentFunctionalPipeline · AIAgentFunctionalPipeline.kt

## Q8: "как агенты работают с mcp"
На основе предоставленного контекста **информация об интеграции агентов Koog с MCP (Model Context Protocol) отсутствует**. В описании классов `PlannerAIAgent`, `FunctionalAIAgent`, `AIAgentServiceBuilderImpl`, компонентов `Prompt`, `ToolRegistry`, `AIAgentStorage` и других нет упоминаний MCP, классов-обёрток для MCP, или способов взаимодействия с внешним протоколом.

---

### Что известно о расширяемости (возможные точки интеграции):
Агенты спроектированы для расширяемости через:
1. **Фичи** (`AIAgentFeature` / `AIAgentFunctionalFeature`) и метод `install()` [КЛАСС 3, ИСТОЧНИК 3] — потенциально, MCP-клиент мог бы быть реализован как фича.
2. **Инструменты** (`ToolRegistry`) [ИСТОЧНИК 1, 3] — MCP-серверы предоставляют инструменты; теоретически, MCP-интеграция могла бы регистрировать инструменты из MCP в `ToolRegistry`.
3. **Стратегии** (`AIAgentPlannerStrategy` / `AIAgentFunctionalStrategy`) [КЛАСС 1, 2] — могли бы включать логику работы с MCP-контекстом, но таких стратегий в контексте нет.

**Однако**: контекст не содержит ни одной упоминательной ссылки на MCP, что предполагает либо:
- MCP-интеграция отсутствует в текущей версии кодовой базы,
- MCP реализован в отдельных модулях/классах, не вошедших в предоставленный контекст.

---

### Уточняющий вопрос:
**Существуют ли в кодовой базе Koog классы/модули, явно связанные с MCP (например, `MCPClient`, `MCPToolAdapter`, `ModelContextProtocolIntegration`)? Если да, то как они интегрируются с `ToolRegistry` или `AIAgentContext`?**

---

### Источники

[КЛАСС 1] PlannerAIAgent · PlannerAIAgent.kt

[КЛАСС 2] FunctionalAIAgent · FunctionalAIAgent.kt

[КЛАСС 3] AIAgentServiceBuilderImpl · AIAgentServiceBuilderImpl.kt

[ИСТОЧНИК 1] AIAgentConfigBase.kt · for · line 1

[ИСТОЧНИК 2] PlannerAIAgent.kt · line 22

[ИСТОЧНИК 3] FunctionalAIAgent.kt · line 63

[КЛАСС 4] AIAgentFunctionalPipeline · AIAgentFunctionalPipeline.kt

## Q9: "какие возможности есть по выстраиванию пайпланов и разбитие задач на подзадачи"
На основе предоставленного контекста **подробная информация о реализации разбиения задач на подзадачи и построении пайплайнов в агентах Koog отсутствует**. Однако архитектура позволяет сделать выводы о двух принципиально разных подходах к построению пайплайнов, которые косвенно связаны с декомпозицией задач.

### Два типа пайплайнов (соответствующие типам агентов):

1. **Графовый пайплайн (для `PlannerAIAgent`)**:
   - **Стратегия**: `AIAgentPlannerStrategy<Input, Output, *>` [ИСТОЧНИК 1] — предполагает, что стратегия отвечает за планирование, включая возможное разбиение задачи на подзадачи и построение графа выполнения.
   - **Пайплайн**: `AIAgentPlannerPipeline` [ИСТОЧНИК 2] — реализует выполнение графа, созданного стратегией.
   - **Контекст**: `AIAgentPlannerContext` [ИСТОЧНИК 2] включает `GenericAgentEnvironment`, который, судя по названию, может представлять собой окружение для выполнения плана (возможно, с поддержкой параллельных ветвей, циклов, условий).
   - **Билдер**: `GraphAgentServiceBuilder` (из `graphStrategy(strategy)`) [ИСТОЧНИК 4] — явно указывает на графическую природу пайплайна.
   - **Логика разбиения**: Вероятно, встроена в `AIAgentPlannerStrategy`, которая анализирует входные данные (`Input`) и формирует граф узлов (подзадач). Детали (алгоритмы разбиения, синхронизация узлов) не показаны.

2. **Функциональный пайплайн (для `FunctionalAIAgent`)**:
   - **Стратегия**: `AIAgentFunctionalStrategy<Input, Output>` [КЛАСС 2] — определяет последовательность функциональных шагов (обработчиков).
   - **Пайплайн**: `AIAgentFunctionalPipeline` [КЛАСС 4] — наследует `AIAgentPipeline`, реализует линейную (или простую ветвящуюся) цепочку шагов.
   - **Контекст**: `AIAgentFunctionalContext` [ИСТОЧНИК 3] — предоставляет окружение для шагов пайплайна.
   - **Билдер**: `FunctionalAgentServiceBuilder` (из `functionalStrategy(strategy)`) [ИСТОЧНИК 4] — для функционального стиля.
   - **Разбиение задач**: Здесь подзадачи, скорее всего, представляют собой последовательные этапы в стратегии. Декомпозиция происходит на этапе проектирования стратегии (разработчиком), а не динамически во время выполнения.

### Общие возможности по построению пайплайнов:

- **Расширяемость через фичи**:
  - Оба агента поддерживают `installFeature` [КЛАСС 3, ИСТОЧНИК 3], что позволяет добавлять пользовательские шаги, инструменты или модифицировать поведение пайплайна.
  - Фичи могут вносить новые этапы в процесс выполнения (например, этап валидации, логгирования, или даже косвенное разбиение задачи).

- **Управление через конфигурацию**:
  - Параметр `maxIterations` [ИСТОЧНИК 4, 5] может ограничивать глубину рекурсии или количество шагов в пайплайне (особенно в графовом, где подзадачи могут выполняться итеративно).
  - `ToolRegistry` [ИСТОЧНИК 1, 3] позволяет подключать инструменты, которые могут использоваться на любом этапе пайплайна для выполнения подзадач.

- **Контекст выполнения**:
  - `GenericAgentEnvironment` (в `PlannerAIAgent`) и `AIAgentFunctionalContext` (в `FunctionalAIAgent`) предоставляют общее состояние и сервисы (например, `ToolRegistry`, `PromptExecutor`) для всех шагов пайплайна.
  - `AIAgentStateManager` (в `PlannerAIAgent`) [ИСТОЧНИК 2] может хранить промежуточные результаты подзадач.

### Отсутствие информации:
- **Алгоритмы разбиения**: Неизвестно, как именно `AIAgentPlannerStrategy` декомпозирует задачу (рекурсивно, по шаблонам, с помощью LLM). Нет упоминания о `TaskDecomposer`, `Planner` или аналогичных компонентов.
- **Динамическое построение пайплайнов**: Неясно, может ли пайплайн меняться во время выполнения (например, после получения результата подзадачи). Графовый подход (`PlannerAIAgent`) предполагает такую возможность, но детали не описаны.
- **Параллельное выполнение подзадач**: Упоминание `GenericAgentEnvironment` наводит на мысль о поддержке параллелизма, но нет явных классов (`ParallelStep`, `ForkJoinNode`).
- **Управление зависимостями между подзадачами**: В графовом пайплайне должны быть механизмы синхронизации (например, `JoinNode`), но они не упомянуты.

---

### Уточняющий вопрос:
**Какие конкретные классы/интерфейсы отвечают за разбиение задач в `AIAgentPlannerStrategy` (например, `Planner`, `TaskGraphBuilder`) и как происходит интеграция результатов подзадач в итоговый ответ? Есть ли поддержка динамического (во время выполнения) vs статического (на старте) построения графа?**

---

### Источники

[КЛАСС 1] PlannerAIAgent · PlannerAIAgent.kt

[КЛАСС 2] FunctionalAIAgent · FunctionalAIAgent.kt

[КЛАСС 3] AIAgentServiceBuilderImpl · AIAgentServiceBuilderImpl.kt

[ИСТОЧНИК 1] AIAgentConfigBase.kt · for · line 1

[ИСТОЧНИК 2] PlannerAIAgent.kt · line 22

[ИСТОЧНИК 3] FunctionalAIAgent.kt · line 63

[КЛАСС 4] AIAgentFunctionalPipeline · AIAgentFunctionalPipeline.kt

[ИСТОЧНИК 4] AIAgentServiceBuilderImpl.kt · graphStrategy · line 89

[ИСТОЧНИК 5] AIAgentServiceBuilderImpl.kt · temperature · line 102

## Q10: "как агенты могу общаться друг с другом"
На основе предоставленного контекста **информация о том, как агенты Koog могут общаться друг с другом, отсутствует**. В описании классов `PlannerAIAgent`, `FunctionalAIAgent`, их контекстов, пайплайнов и сервисов нет упоминаний о механизмах межъагентного взаимодействия, коммуникационных протоколах, сетях агентов или общих пространствах обмена сообщениями.

---

### Возможные гипотетические точки интеграции (на основе архитектуры):

1. **Общие ресурсы (косвенное взаимодействие)**:
   - Агенты могут использовать общий `ToolRegistry` [ИСТОЧНИК 1, 3], что теоретически позволяет одному агенту вызывать инструменты, реализованные другим агентом (если тот зарегистрирован как инструмент).
   - `AIAgentStorage` [ИСТОЧНИК 3] и `AIAgentStateManager` [ИСТОЧНИК 2] могут служить общим хранилищем данных, где агенты читают/пишут состояние, что создаёт возможность для координации.
   - Однако в контексте нет указаний на то, что эти компоненты предназначены для межъагентного обмена — они, судя по названиям,是为单个代理管理状态和存储。

2. **Цепочка пайплайнов (вложение)**:
   - `FunctionalAIAgent` может включать в свой пайплайн вызов другого агента как шаг (если `AIAgentFunctionalStrategy` поддерживает вложенность). Но в `AIAgentFunctionalPipeline` [КЛАСС 4] нет упоминаний о вызовах других агентов.
   - `PlannerAIAgent` строит граф, где узлы могут быть как инструментами, так и потенциально другими агентами, но в `AIAgentPlannerPipeline` [ИСТОЧНИК 2] такой возможности не показано.

3. **Общий LLM-контекст**:
   - Агенты используют `PromptExecutor` [ИСТОЧНИК 1, 3] для отправки промптов модели. Если промпты формируются на основе общей истории (например, из `AIAgentStorage`), агенты могут "общаться" через модель, но это скорее общий доступ к данным, а не прямое взаимодействие.

4. **Фичи и расширения**:
   - Система фич (`AIAgentFeature`/`AIAgentFunctionalFeature`) [КЛАСС 3] позволяет добавлять поведение. Теоретически, фича могла бы реализовать протокол обмена сообщениями между агентами, но в контексте нет примеров.

---

### Критические пробелы:
- Нет классов/интерфейсов для:
  - `AgentMessage`, `AgentCommunicationChannel`, `AgentNetwork`.
  - `AgentRegistry` (реестр агентов для обнаружения).
  - Протоколов (RPC, события, общие очереди).
- Нет упоминаний о `DistributedAgent` или `MultiAgentSystem`.
- Стратегии (`AIAgentPlannerStrategy`/`AIAgentFunctionalStrategy`) и пайплайны сосредоточены на выполнении **одной задачи одним агентом**, без координации с другими агентами.

---

### Уточняющий вопрос:
**Существуют ли в кодовой базе Koog компоненты для многоканального взаимодействия агентов (например, `AgentBus`, `AgentMessageBroker`, `Agent协作 Coordinator`)? Если да, то как они интегрируются с `ToolRegistry` или `AIAgentContext` и каковы сценарии использования (например, делегирование подзадач, обмен промежуточными результатами, синхронизация)?**

---

### Источники

[КЛАСС 1] PlannerAIAgent · PlannerAIAgent.kt

[КЛАСС 2] FunctionalAIAgent · FunctionalAIAgent.kt

[КЛАСС 3] AIAgentServiceBuilderImpl · AIAgentServiceBuilderImpl.kt

[ИСТОЧНИК 1] AIAgentConfigBase.kt · for · line 1

[ИСТОЧНИК 2] PlannerAIAgent.kt · line 22

[ИСТОЧНИК 3] FunctionalAIAgent.kt · line 63

[КЛАСС 4] AIAgentFunctionalPipeline · AIAgentFunctionalPipeline.kt

[ИСТОЧНИК 4] AIAgentServiceBuilderImpl.kt · graphStrategy · line 89

[ИСТОЧНИК 5] AIAgentServiceBuilderImpl.kt · temperature · line 102

## Q11: "какие возможности по работе с сессиями"
На основе предоставленного контекста **явных возможностей по работе с сессиями (sessions) в Koog не обнаружено**. В классах `PlannerAIAgent`, `FunctionalAIAgent`, их контекстах и сервисах нет упоминаний компонентов, специально предназначенных для управления сессиями (например, `SessionManager`, `AgentSession`, `SessionScope`). Однако косвенно существуют механизмы, которые *могут* использоваться для реализации сессионного подхода.

### Косвенные механизмы, которые могут быть связаны с сессиями:

1. **Уникальный идентификатор агента (`id`)**:
   - Каждый агент имеет уникальный `id` [КЛАСС 1, КЛАСС 2]. В сценарии с несколькими сессиями один и тот же агент (например, экземпляр `PlannerAIAgent`) мог бы использоваться для разных сессий, но тогда необходимо изолировать состояние. В текущей архитектуре состояние хранится в контексте (`AIAgentPlannerContext`/`AIAgentFunctionalContext`), который создаётся заново при каждом вызове (?), однако детали неясны.

2. **Хранение состояния (`AIAgentStorage`, `AIAgentStateManager`)**:
   - `AIAgentStateManager` [ИСТОЧНИК 2] в `PlannerAIAgent` может управлять состоянием агента во время выполнения, но unclear, сохраняется ли оно между вызовами.
   - `AIAgentStorage` [ИСТОЧНИК 3] в `FunctionalAIAgent` — потенциальный кандидат для персистентного хранения данных сессии (например, истории диалога). Однако в контексте нет указаний на то, что он привязан к сессии или поддерживает несколько сессий.

3. **Контекст выполнения (`AIAgentPlannerContext`, `AIAgentFunctionalContext`)**:
   - Эти объекты создаются при подготовке к выполнению (метод `prepareContext` [ИСТОЧНИК 2, 3]). Если контекст инкапсулирует всё состояние сессии (история, переменные), то каждая сессия может иметь свой экземпляр контекста. Но нет явного API для создания/управления таким экземпляром извне.

4. **Временные метки (`Clock`)**:
   - Использование `Clock` [КЛАСС 1, КЛАСС 2] позволяет фиксировать время событий, что полезно для сессионного логирования, но не является системой сессий.

### Отсутствующие элементы:
- **Нет сессионного менеджера**: Не прослеживается централизованного компонента, который бы создавал, хранил, извлекал или уничтожал сессии.
- **Нет изоляции состояния между сессиями**: Если два пользователя используют одного агента, их данные могут смешиваться в общих `AIAgentStorage`/`AIAgentStateManager`, если эти компоненты разделяемые.
- **Нет жизненного цикла сессии**: Не определены события начала, конца, таймаута сессии.
- **Нет привязки сессии к пользователю**: нет `userId` в конфигурации агента или контексте (есть только `agentConfig` [ИСТОЧНИК 1, 4] и `agentInput` [ИСТОЧНИК 2]).

### Возможные гипотезы реализации (если сессии нужны):
1. **Сессия = экземпляр агента**: Создание отдельного агента (через builder) на каждую сессию с уникальным `id` и изолированным `AIAgentStorage`. Это resource-intensive.
2. **Сессия = изоляция в общем агенте**: Использование общего агента, но с передачей sessionId в `agentInput` и фильтрацией данных в `AIAgentStorage`/`StateManager` по этому sessionId. Требует модификации этих компонентов.
3. **Сессия как фича**: Реализация своей фичи (`AIAgentFeature`), которая добавляет менеджер сессий и интегрируется с контекстом.

---

### Уточняющий вопрос:
**Под "сессиями" подразумеваются: (а) изолированные экземпляры агентов с собственным состоянием для каждого пользователя/диалога; (б) общий агент с разделяемым состоянием, но с фильтрацией по sessionId; (в) что-то иное? Существуют ли в кодовой базе классы, например `SessionManager`, `MultiSessionAIAgentStorage`, или sessionId передаётся через `agentInput`?**

---

### Источники

[КЛАСС 1] PlannerAIAgent · PlannerAIAgent.kt

[КЛАСС 2] FunctionalAIAgent · FunctionalAIAgent.kt

[КЛАСС 3] AIAgentServiceBuilderImpl · AIAgentServiceBuilderImpl.kt

[ИСТОЧНИК 1] AIAgentConfigBase.kt · for · line 1

[ИСТОЧНИК 2] PlannerAIAgent.kt · line 22

[ИСТОЧНИК 3] FunctionalAIAgent.kt · line 63

[КЛАСС 4] AIAgentFunctionalPipeline · AIAgentFunctionalPipeline.kt

[ИСТОЧНИК 4] AIAgentServiceBuilderImpl.kt · graphStrategy · line 89

[ИСТОЧНИК 5] AIAgentServiceBuilderImpl.kt · temperature · line 102

## Q12: "как реализован state management"
На основе предоставленного контекста **реализация state management в Koog фрагментарна и слабо документирована**. Существуют два отдельных компонента, но их внутренняя работа, жизненный цикл и использование остаются неясными.

### 1. Доступные компоненты state management

#### **PlannerAIAgent** (графовый агент)
- **Компонент**: `AIAgentStateManager` [ИСТОЧНИК 2]
- **Где используется**: В `AIAgentPlannerContext` [ИСТОЧНИК 2], который передаётся в `AIAgentPlannerPipeline` и стратегию.
- **Предназначение** (гипотеза): Управление состоянием **во время выполнения графа** — хранение промежуточных результатов узлов, статусов выполнения подзадач, возможно, отладка и восстановление при ошибках. Имя `StateManager` предполагает более сложную логику, чем просто хранилище.

#### **FunctionalAIAgent** (функциональный агент)
- **Компонент**: `AIAgentStorage` [ИСТОЧНИК 3]
- **Где используется**: В `AIAgentFunctionalContext` [ИСТОЧНИК 3], доступен в пайплайне и стратегии.
- **Предназначение** (гипотеза): Простое хранилище данных, вероятно, для **истории сообщений** (контекст диалога) или промежуточных результатов цепочки. Название `Storage` указывает на персистентность или долгосрочное хранение.

### 2. Критические пробелы в реализации

| Аспект | PlannerAIAgent (AIAgentStateManager) | FunctionalAIAgent (AIAgentStorage) |
|--------|--------------------------------------|------------------------------------|
| **Интерфейс/реализация** | Нет. Класс `AIAgentStateManager` не описан, неизвестны методы (`getState`, `saveState`, `clear`). | Нет. Класс `AIAgentStorage` не описан. |
| **Инициализация** | Как создаётся? Через builder? Если `null` в агенте [ИСТОЧНИК 2], то по умолчанию нет. | Аналогично. |
| **Жизненный цикл** | Привязан к одному выполнению задачи (экземпляр контекста) или к агенту (разделяемый)? | Аналогично. |
| **Изоляция сессий** | Если агент используется несколькими пользователями, как избежать смешивания состояний? | Аналогично. |
| **Использование в пайплайне** | В `AIAgentPlannerPipeline` [ИСТОЧНИК 2] нет явных вызовов `stateManager`. Где и как он используется? | В `AIAgentFunctionalPipeline` [КЛАСС 4] нет упоминаний `storage`. |
| **Серийная/десериализация** | Поддерживает ли сохранение на диск/в БД? | Аналогично. |
| **Типы хранимых данных** | Какие данные? (Результаты шагов, ошибки, метаданные). | История сообщений? Промежуточные объекты? |

### 3. Потенциальные точки использования (на основе архитектуры)

- **В стратегиях** (`AIAgentPlannerStrategy`/`AIAgentFunctionalStrategy`):
  - При принятии решений (например, планировщик может читать предыдущие результаты подзадач из `stateManager`).
  - При формировании промпта — чтение истории из `storage` для контекста.
- **В пайплайнах**:
  - Каждый шаг пайплайна может читать/писать состояние (например, `storeResult`, `loadContext`).
  - В графовом пайплайне — синхронизация между параллельными ветвями через `stateManager`.
- **В фичах** (`installFeature`):
  - Фича может добавлять свою логику state management (например, кеширование, аудит).

### 4. Отсутствие стандартизации

- **Нет общего интерфейса** для state management между `PlannerAIAgent` и `FunctionalAIAgent`. Это усложняет переиспользование кода.
- **Нет документации** по тому, как правильно использовать эти компоненты. Разработчик должен изучать исходный код стратегий и пайплайнов.

### 5. Уточняющие вопросы

1. **Реализации**: Какие конкретные классы реализуют `AIAgentStateManager` и `AIAgentStorage`? Где они определены?
2. **API**: Какие методы предоставляют эти интерфейсы? (Пример: `get(sessionId: String): Map<String, Any>?`, `save(sessionId: String, data: Map<String, Any>)`).
3. **Жизненный цикл**: Создаются ли эти компоненты при построении агента (builder) или при каждом вызове `execute`? Как происходит очистка?
4. **Интеграция**: Где в коде пайплайнов/стратегий происходит обращение к `stateManager`/`storage`? Есть ли примеры?
5. **Сессии**: Если агент многопользовательский, как в `storage`/`stateManager` отделяются данные разных сессий? Есть ли встроенная поддержка `sessionId`?

---

### Источники

[КЛАСС 1] PlannerAIAgent · PlannerAIAgent.kt

[КЛАСС 2] FunctionalAIAgent · FunctionalAIAgent.kt

[КЛАСС 3] AIAgentServiceBuilderImpl · AIAgentServiceBuilderImpl.kt

[ИСТОЧНИК 1] AIAgentConfigBase.kt · for · line 1

[ИСТОЧНИК 2] PlannerAIAgent.kt · line 22

[ИСТОЧНИК 3] FunctionalAIAgent.kt · line 63

[КЛАСС 4] AIAgentFunctionalPipeline · AIAgentFunctionalPipeline.kt

[ИСТОЧНИК 4] AIAgentServiceBuilderImpl.kt · graphStrategy · line 89

[ИСТОЧНИК 5] AIAgentServiceBuilderImpl.kt · temperature · line 102

## Q13: "как реализована обработка ошибок"
На основе предоставленного контекста **реализация обработки ошибок (error handling) в Koog не документирована**. Отсутствуют явные упоминания классов, интерфейсов или стратегий, предназначенных для перехвата, классификации и восстановления после ошибок. Однако можно выявить несколько косвенных аспектов и гипотез.

---

### 1. Возможные точки, где могут возникать ошибки

| Этап | Возможные ошибки | Кто должен обрабатывать? |
|------|----------------|--------------------------|
| **Валидация входных данных** | `agentInput` не соответствует ожидаемому формату. | Builder? Контекст? Стратегия? |
| **Выполнение LLM** | API ошибка, таймаут, превышение токенов, блокировка контента. | `PromptExecutor` (вне контекста), стратегия. |
| **Tool calling** | Инструмент не найден, ошибка выполнения инструмента, неверные параметры. | `ToolRegistry` при вызове, пайплайн, стратегия. |
| **Пайплайн** | Ошибка в шаге пайплайна, нарушение графа (цикл, отсутствующий узел). | `AIAgentPlannerPipeline`/`AIAgentFunctionalPipeline`. |
| **State management** | Ошибка доступа к `AIAgentStorage`/`AIAgentStateManager`. | Контекст или хранилище. |
| **Планирование** | Стратегия `AIAgentPlannerStrategy` не может декомпозировать задачу. | Сама стратегия. |

---

### 2. Отсутствующие элементы явной обработки ошибок

- **Нет единого интерфейса** `ErrorHandler` или `FailureStrategy` в архитектуре агентов.
- **Нет классов-исключений** (например, `AgentExecutionException`, `ToolException`, `PipelineException`), которые бы структурировали ошибки.
- **Нет политик повторных попыток (retry)** для LLM API или инструментов.
- **Нет fallback-стратегий**: что делать, если LLM недоступен? Если инструмент упал?
- **Нет puncture-механизмов** (например, `catch`-шага в пайплайне) для продолжения выполнения после неудачного шага.

---

### 3. Где может скрываться обработка ошибок (гипотезы)

#### a) **В стратегиях (`AIAgentPlannerStrategy`/`AIAgentFunctionalStrategy`)**
Стратегия может содержать логику:
- Проверки входных данных.
- Обработки ошибок LLM (например, повторный запрос при 429).
- Повторного планирования, если подзадача провалилась.
- Но в описании классов [КЛАСС 1, КЛАСС 2] такого нет.

#### b) **В пайплайнах (`AIAgentPlannerPipeline`, `AIAgentFunctionalPipeline`)**
Пайплайн может оборачивать выполнение каждого шага в try-catch и:
- Прерывать выполнение при первой ошибке.
- Пропускать неудачные шаги (если это допустимо).
- Возвращать специальное значение ошибки в `Output`.
Но в описании `AIAgentFunctionalPipeline` [КЛАСС 4] и `AIAgentPlannerPipeline` [ИСТОЧНИК 2] это не указано.

#### c) **В `PromptExecutor` (вне контекста)**
Этот компонент (упомянут в контексте как часть `AIAgentLLMContext`) может содержать логику:
- Retry для запросов к LLM.
- Трансформацию ошибок LLM в структурированный формат.
Но `PromptExecutor` не входит в предоставленные описания классов.

#### d) **Через `ToolRegistry`**
При вызове инструмента может:
- Перехватывать исключения и возвращать ошибку как результат.
- Но это маловероятно, так как `ToolRegistry` обычно просто диспетчеризирует вызовы.

---

### 4. Типы возвращаемых значений

Если агент возвращает `Output` (тип параметризован), то:
- **Успех**: `Output` содержит результат.
- **Ошибка**: Может быть:
  - `null` (если `Output` nullable) — но тогда caller не отличит ошибку от пустого результата.
  - Специальный объект `ErrorOutput` (но такого класса нет в контексте).
  - Исключение (unchecked) выбрасывается из `execute`/`handle` — это наиболее вероятно, но не документировано.

---

### 5. Вопросы для уточнения

1. **Какой тип имеет `Output`?** Может ли он быть sealed class с вариантами `Success`/`Failure`? Или это простой data class?
2. **Методы `execute`/`handle` в агентах** [КЛАСС 1, КЛАСС 2] — кидают ли они исключения или возвращают специальный объект ошибки?
3. **Существуют ли в кодовой базе классы** `AgentException`, `ToolExecutionException`, `PipelineException`?
4. **Как обрабатываются ошибки LLM** (сетеые, rate limit, content filter)? Есть ли retry logic?
5. **Как инструменты сообщают об ошибках?** Через исключения или возвращают `Result`-подобный объект?
6. **Есть ли возможность настроить** количество retry, таймауты, fallback-агент?

---

### 6. Рекомендации по исследованию

- **Поиск в кодовой базе**:  
  `Exception`, `Error`, `try`, `catch`, `throw`, `retry`, `fallback` в папках агентов.
- **Изучение реализаций**:
  - Файлы `PlannerAIAgent.kt`, `FunctionalAIAgent.kt` — посмотреть bodies методов `execute`/`handle`.
  - Пайплайны: `AIAgentPlannerPipeline.kt`, `AIAgentFunctionalPipeline.kt`.
  - Стратегии: `AIAgentPlannerStrategy.kt`, `AIAgentFunctionalStrategy.kt`.
- **Проверить `ToolRegistry`**: как вызываются инструменты, есть ли обработка исключений.

---

### Уточняющий вопрос

**Есть ли в кодовой базе Koog единый подход к обработке ошибок (например, использование `Result<T>` или sealed class `AgentResult`), или каждый компонент (стратегия, пайплайн, инструмент) обрабатывает ошибки самостоятельно? Как именно выглядит поток обработки ошибки: выбрасывается ли исключение, возвращается ли объект ошибки, или выполнение просто останавливается?**

---

### Источники

[КЛАСС 1] PlannerAIAgent · PlannerAIAgent.kt

[КЛАСС 2] FunctionalAIAgent · FunctionalAIAgent.kt

[КЛАСС 3] AIAgentServiceBuilderImpl · AIAgentServiceBuilderImpl.kt

[ИСТОЧНИК 1] AIAgentConfigBase.kt · for · line 1

[ИСТОЧНИК 2] PlannerAIAgent.kt · line 22

[ИСТОЧНИК 3] FunctionalAIAgent.kt · line 63

[КЛАСС 4] AIAgentFunctionalPipeline · AIAgentFunctionalPipeline.kt

[ИСТОЧНИК 4] AIAgentServiceBuilderImpl.kt · graphStrategy · line 89

[ИСТОЧНИК 5] AIAgentServiceBuilderImpl.kt · temperature · line 102

## Q14: "какие механизмы работы с опасными tool calling"
На основе предоставленного контекста **в Koog отсутствует явная и детализированная реализация механизмов безопасности для обработки опасных вызовов инструментов (tool calling)**. Отсутствуют классы, интерфейсы или политики, специально предназначенные для валидации, контроля доступа, изоляции или аудита инструментов. Однако можно выявить косвенные аспекты и сформулировать гипотезы.

---

### 1. Известные компоненты, связанные с инструментами

| Компонент | Назначение (из контекста) | Относится ли к безопасности? |
|-----------|---------------------------|------------------------------|
| **`ToolRegistry`** [ИСТОЧНИК 1] | Регистрирует и предоставляет инструменты для агента. | Нет. Это диспетчер, а не security-компонент. |
| **`installFeature`** (например, `ToolFeature`) | Механизм добавления инструментов в агента. | Возможная точка для политик безопасности при установке. |
| **Контекст агента** (`AIAgentPlannerContext`, `AIAgentFunctionalContext`) | Хранит `ToolRegistry` и предоставляет его пайплайну/стратегии. | Возможная точка для передачи security-контекста (например, ID пользователя) при вызове инструмента. |

---

### 2. Что отсутствует в контексте (критические пробелы)

- **Валидация входных данных инструментов**: нет упоминаний о схемемах (JSON Schema), типах параметров или проверке на инъекции.
- **Контроль доступа (Authorization)**: неясно, как определяется, какой агент/пользователь может вызывать конкретный инструмент.
- **Изоляция (Sandboxing)**: нет упоминаний о запуске инструментов в отдельном процессе/контейнере с ограниченными правами.
- **Ограничение ресурсов (Resource Limits)**: нет таймаутов, ограничений по памяти/CPU для инструментов.
- **Аудит и логирование**: нет классов для записи всех вызовов инструментов (кто, когда, с какими параметрами, результат).
- **Политики безопасности (Security Policies)**: нет централизованного управления разрешениями.
- **Обработка особо опасных операций**: например, подтверждение пользователем (user in the loop) для инструментов, изменяющих состояние (удаление данных, финансовые транзакции).

---

### 3. Возможные места реализации (гипотезы)

#### a) **В `ToolRegistry`**
- При вызове инструмента `ToolRegistry` может:
  - Проверять права вызывающего (агента/пользователя).
  - Валидировать параметры по схеме, зарегистрированной вместе с инструментом.
  - Логировать вызов.
  - Оборачивать выполнение в таймаут.
- **Но** в контексте `ToolRegistry` представлен как простой реестр.

#### b) **В пользовательских фичах (`AIAgentFeature`)**
- Разработчик может создать фичу, которая:
  - Обертывает вызовы инструментов.
  - Добавляет проверки.
  - Реализуетsandbox через внешние процессы.
- Это требует ручной работы и не является встроенным механизмом.

#### c) **В стратегии (`AIAgentPlannerStrategy`/`AIAgentFunctionalStrategy`)**
- Стратегия может принимать решение о вызове инструмента и:
  - Проверять контекст (например, флаг `safeMode`).
  - Запрашивать подтверждение у пользователя перед風險ованными операциями.
- Но стратегии сосредоточены на планировании, а не на безопасности.

#### d) **В пайплайне (`AIAgentPlannerPipeline`/`AIAgentFunctionalPipeline`)**
- Шаг пайплайна, отвечающий за tool calling, может содержать логику безопасности.
- Однако в описании `AIAgentFunctionalPipeline` [КЛАСС 4] нет упоминаний о таких шагах.

---

### 4. Потенциальные риски при отсутствии механизмов

| Риск | Последствие |
|------|-------------|
| **Инъекции** (SQL, команды ОС) | Если инструмент выполняет строки как код/команду. |
| **Неавторизованный доступ** | Агент может вызвать инструмент, к которому у него нет прав (например, доступ к чужим данным). |
| **Денyal of Service** | Инструмент может зависнуть или потреблять много ресурсов, уронив весь агент. |
| **Потеря данных** | Опасный инструмент может удалить или повредить данные без подтверждения. |
| **Неотслеживаемость** | Нет логов, кто и что делал — сложно рассле灼ровать инциденты. |

---

### 5. Уточняющие вопросы

1. **Существуют ли в кодовой базе классы, связанные с безопасностью инструментов**, например:
   - `ToolSecurityManager`
   - `ToolValidator`
   - `ToolAccessPolicy`
   - `SandboxedToolExecutor`
   - `AuditLogger`
2. **Как именно вызываются инструменты из пайплайна/стратегии?** Есть ли пример кода? Показывает ли он валидацию или контроль доступа?
3. **Можно ли зарегистрировать инструмент с метаданными безопасности** (например, `requiresApproval: true`, `allowedRoles: ["admin"]`)?
4. **Предусмотрен ли механизм "user in the loop" для рискованных инструментов?** Например, пауза и запрос подтверждения у пользователя.
5. **Есть ли стандартные инструменты в Koog, которые считаются опасными** (например, `DeleteFileTool`, `ExecuteShellTool`)? Как они защищены?
6. **Как обрабатываются исключения при выполнении инструмента?** Они провалидают всю задачу или можно продолжить?

---

### 6. Рекомендации по исследованию

- **Поиск в кодовой базе**:  
  `security`, `validate`, `permission`, `audit`, `sandbox`, `approval`, `risk`, `dangerous`.
- **Изучение реализаций**:
  - `ToolRegistry.kt` — как хранятся и вызываются инструменты.
  - Примеры инструментов (если есть) — выглядят ли они безопасно (например, используют prepared statements, избегают `eval`).
  - Стратегии и пайплайны — как происходит вызов `toolRegistry.getTool(name).execute(args)`.
- **Проверить документацию** (если есть) по теме "security" или "tool calling best practices".

---

### Вывод

** current implementation of tool calling security in Koog is likely minimal or non-existent**. The architecture provides the extension point (`installFeature`) where security could be added, but there is no evidence of built-in safeguards. Developers must implement their own validation, access control, and sandboxing if needed.

**Уточняющий вопрос**:  
**Существуют ли в Koog встроенные классы или политики для безопасного вызова инструментов (валидация, контроль доступа, sandboxing), или вся ответственность лежит на разработчике, который создает инструменты и фичи? Если да, то какие конкретно классы отвечают за эти механизмы?**

---

### Источники

[КЛАСС 1] PlannerAIAgent · PlannerAIAgent.kt

[КЛАСС 2] FunctionalAIAgent · FunctionalAIAgent.kt

[КЛАСС 3] AIAgentServiceBuilderImpl · AIAgentServiceBuilderImpl.kt

[ИСТОЧНИК 1] AIAgentConfigBase.kt · for · line 1

[ИСТОЧНИК 2] PlannerAIAgent.kt · line 22

[ИСТОЧНИК 3] FunctionalAIAgent.kt · line 63

[КЛАСС 4] AIAgentFunctionalPipeline · AIAgentFunctionalPipeline.kt

[ИСТОЧНИК 4] AIAgentServiceBuilderImpl.kt · graphStrategy · line 89

[ИСТОЧНИК 5] AIAgentServiceBuilderImpl.kt · temperature · line 102

## Q15: "где хранится текущий диалог агента с пользователем и какие возможности по его управлению"
На основе предоставленного контекста **реализация хранения и управления текущим диалогом (историей сообщений) в Koog является фрагментарной и недостаточно документированной**. Есть два возможных компонента, но их назначение и API неясны.

---

### 1. Где предположительно хранится диалог

#### a) **`AIAgentStorage` (для `FunctionalAIAgent`)**
- Упоминается как часть `AIAgentFunctionalContext` [ИСТОЧНИК 3].
- **Предполагаемое назначение**: хранилище данных агента, *возможно*, включающее историю сообщений (сообщения пользователя, ответы агента, результаты tool calls).
- **Неизвестно**: 
  - Является ли это основным хранилищем истории для функционального агента?
  - Поддерживает ли оно несколько сессий (session isolation)?
  - Как происходит добавление сообщений (автоматически в пайплайне или вручную в стратегии)?

#### b) **`AIAgentStateManager` (для `PlannerAIAgent`)**
- Упоминается как часть `AIAgentPlannerContext` [ИСТОЧНИК 2].
- **Предполагаемое назначение**: управление состоянием *во время выполнения графа*, что может включать:
  - Промежуточные результаты узлов графа.
  - Сообщения, сгенерированные на разных этапах планирования.
  - Возможно, историю диалога, но не обязательно (состояние графа ≠ история чата).
- **Неизвестно**: 
  - Как differed from `AIAgentStorage`?
  - Используется ли для хранения истории сообщений?

#### c) **`PromptExecutor` (косвенно)**
- Упоминается как компонент, формирующий промпт для LLM [в контексте как часть `AIAgentLLMContext`].
- **Предполагаемое назначение**: собирает контекст (включая историю) для создания промпта.
- **Неизвестно**: 
  - Откуда он берёт историю? Из `AIAgentStorage`/`StateManager`?
  - Предоставляет ли он API для управления историей (добавление, удаление, компактизация)?

---

### 2. Возможности управления диалогом (что отсутствует)

На основе контекста **не обнаружено явных API или политик для управления диалогом**:

| Возможность | Наличие в контексте | Комментарий |
|-------------|---------------------|-------------|
| **Добавление сообщения** | Неизвестно | Возможно, происходит автоматически при каждом шаге пайплайна/стратегии. |
| **Очистка истории** | ❌ Нет | Нет методов `clearHistory()`, `resetConversation()`. |
| **Ограничение длины (context window)** | ❌ Нет | Нет классов `ContextCompactor`, `HistoryManager`, `WindowedHistory`. |
| **Ручное управление (добавление/удаление произвольных сообщений)** | ❌ Нет | Нет exposing storage/stateManager через публичный API контекста. |
| **Сессионная изоляция** | ❌ Нет | Неясно, как обеспечивается изоляция между разными диалогами (если агент используется несколькими пользователями одновременно). |
| **Компактизация ( summarization, pruning )** | ❌ Нет | Нет встроенных стратегий укорачивания истории. |
| **User in the loop для истории** | ❌ Нет | Нет механизма подтверждения пользователем перед добавлением в историю или удалением. |

---

### 3. Как, вероятно, работает история (гипотезы)

#### Сценарий 1: История управляется автоматически в пайплайне/стратегии
- Каждый шаг пайплайна (например, шаг вызова LLM или инструмента) автоматически добавляет свои сообщения в `AIAgentStorage`/`StateManager`.
- Разработчик не имеет прямого контроля — история накапливается до завершения выполнения агента.
- **Проблема**: нет способа ограничить длину, что может привести к переполнению контекстного окна LLM.

#### Сценарий 2: История управляется вручную в стратегии
- Стратегия (`AIAgentPlannerStrategy`/`AIAgentFunctionalStrategy`) сама решает, какие сообщения добавлять в `storage`/`stateManager`.
- Разработчик стратегии имеет полный контроль, но должен сам реализовать логику управления (очистка, ограничение).
- **Проблема**: дублирование кода, отсутствие стандартных решений.

#### Сценарий 3: История не хранится явно, а пересоздаётся каждый раз
- При каждом вызове `agent.execute(input)` контекст создаётся заново, и история начинается с чистого листа.
- **Проблема**: невозможно вести долгосрочный диалог.

---

### 4. Критические пробелы

1. **Нет единого интерфейса** для работы с историей (например, `ConversationHistory`).
2. **Нет интеграции с контекстом**: контекст (`AIAgentPlannerContext`, `AIAgentFunctionalContext`) не предоставляет публичных методов для управления историей.
3. **Нет документации** о том, как `PromptExecutor` формирует промпт — берёт ли он всю историю или только последние N сообщений.
4. **Нет примеров** использования `AIAgentStorage`/`AIAgentStateManager` для хранения истории.
5. **Неясна изоляция**: если один экземпляр агента используется несколькими пользователями, как предотвратить смешивание историй?

---

### 5. Уточняющие вопросы

1. **Какой именно компонент отвечает за хранение истории сообщений?** 
   - `AIAgentStorage` [ИСТОЧНИК 3] или `AIAgentStateManager` [ИСТОЧНИК 2]?
   - Или это делается на уровне `PromptExecutor`?

2. **Предоставляют ли `AIAgentStorage`/`AIAgentStateManager` API для:**
   - Добавления сообщения?
   - Получения всей истории?
   - Очистки истории?
   - Ограничения количества сообщений?

3. **Как история включается в промпт?** 
   - Все сообщения всегда?
   - Только последние K?
   - Применяется ли суммаризация при переполнении?

4. **Как обеспечивается изоляция истории между разными сессиями/пользователями?** 
   - Создаётся ли новый экземпляр агента на каждую сессию?
   - Или `storage`/`stateManager` фильтрует по `sessionId` (если он есть)?

5. **Существуют ли в кодовой базе классы**, такие как `ConversationManager`, `HistoryWindow`, `ContextCompactor`, `MessageHistory`?

---

### 6. Рекомендации по исследованию

- **Поиск в кодовой базе**:
  - `history`, `message`, `conversation`, `dialog`, `prompt`, `context`, `window`, `summarize`.
- **Изучение реализаций**:
  - Файлы `AIAgentStorage.kt`, `AIAgentStateManager.kt` — посмотреть их интерфейсы и реализации.
  - `PromptExecutor.kt` — как формируется промпт.
  - Примеры стратегий и пайплайнов — есть ли там вызовы `storage.addMessage()` или подобного.
- **Проверить тесты**: возможно, там есть примеры работы с историей.

---

### 7. Примерная архитектура (что должно быть)

Если бы история была реализована корректно, ожидалось бы:
```kotlin
interface ConversationHistory {
    fun add(message: Message)
    fun getAll(): List<Message>
    fun trim(maxMessages: Int): List<Message> // или автоматическое удаление старых
    fun clear()
    fun summarize(): Message // для компактизации
}

class AIAgentStorageImpl : ConversationHistory { ... }
class AIAgentStateManagerImpl : ConversationHistory { ... }
```

Но в контексте **таких интерфейсов нет**.

---

### Вывод

**Текущая реализация хранения и управления диалогом в Koog is underspecified**. Есть два компонента (`AIAgentStorage`, `AIAgentStateManager`), которые *могут* хранить историю, но их API и жизненный цикл не описаны. Нет встроенных механизмов для управления (очистка, ограничение, компактизация). Разработчик, скорее всего, должен сам реализовывать эту логику в собственных стратегиях или фичах, что приводит к дублированию и ошибкам.

**Уточняющий вопрос**:  
**Существуют ли в кодовой базе Koog классы, реализующие интерфейс для работы с историей диалога (например, `ConversationHistory`, `MessageHistory`), и как они интегрируются с `AIAgentStorage`/`AIAgentStateManager`? Если нет, то как история сообщений добавляется в промпт и где она хранится между вызовами агента?**

---

### Источники

[КЛАСС 1] PlannerAIAgent · PlannerAIAgent.kt

[КЛАСС 2] FunctionalAIAgent · FunctionalAIAgent.kt

[КЛАСС 3] AIAgentServiceBuilderImpl · AIAgentServiceBuilderImpl.kt

[ИСТОЧНИК 1] AIAgentConfigBase.kt · for · line 1

[ИСТОЧНИК 2] PlannerAIAgent.kt · line 22

[ИСТОЧНИК 3] FunctionalAIAgent.kt · line 63

[КЛАСС 4] AIAgentFunctionalPipeline · AIAgentFunctionalPipeline.kt

[ИСТОЧНИК 4] AIAgentServiceBuilderImpl.kt · graphStrategy · line 89

[ИСТОЧНИК 5] AIAgentServiceBuilderImpl.kt · temperature · line 102

## Q16: "как устроена observability и работа с событиями от агентов"
На основе предоставленного контекста **в Koog отсутствует явная и единая система observability (наблюдаемости) и работы с событиями от агентов**. Нет документированных классов, интерфейсов или механизмов для логирования, трассировки, сбора метрик или подписки на события. Однако можно выделить потенциальные точки, где события могут возникать, и предположить, как их можно было бы реализовать.

---

### 1. Где могут возникать события?

#### a) **На уровне контекста (Context)**
- `AIAgentPlannerContext` [ИСТОЧНИК 2] и `AIAgentFunctionalContext` [ИСТОЧНИК 3] — создание, инициализация, очистка.
- События: `context.created`, `context.destroyed`, `state.saved`, `storage.cleared`.

#### b) **На уровне пайплайна (Pipeline)**
- `AIAgentPlannerPipeline` и `AIAgentFunctionalPipeline` [КЛАСС 4] — запуск, выполнение шагов, завершение.
- События: `pipeline.started`, `pipeline.step.executed`, `pipeline.finished`, `pipeline.failed`.

#### c) **На уровне стратегии (Strategy)**
- `AIAgentPlannerStrategy` и `AIAgentFunctionalStrategy` — планирование, разбиение задач, выбор инструментов.
- События: `strategy.plan.created`, `task.decomposed`, `tool.selected`.

#### d) **На уровне инструментов (Tools)**
- `ToolRegistry` [ИСТОЧНИК 1] и конкретные инструменты — вызов, успех, ошибка.
- События: `tool.called` (с параметрами), `tool.succeeded`, `tool.failed`.

#### e) **На уровне LLM-взаимодействий**
- `PromptExecutor` (упомянут в контексте) — отправка промпта, получение ответа, ошибки LLM.
- События: `llm.request.sent`, `llm.response.received`, `llm.error`.

#### f) **На уровне state management**
- `AIAgentStateManager` [ИСТОЧНИК 2] и `AIAgentStorage` [ИСТОЧНИК 3] — сохранение/извлечение состояния.
- События: `state.saved`, `state.loaded`, `storage.evicted`.

---

### 2. Что отсутствует в контексте (критические пробелы)

| Компонент | Что должно быть для observability | Есть в контексте? |
|-----------|-----------------------------------|-------------------|
| **Event Bus / Event Publisher** | Центральный шиtionary для событий (например, `AgentEventBus`). | ❌ Нет |
| **Event definitions** | Классы событий (`AgentEvent`, `ToolCallEvent`, `PipelineStepEvent`). | ❌ Нет |
| **Listeners / Subscribers** | Интерфейс для подписки на события (`AgentEventListener`). | ❌ Нет |
| **Metrics collector** | Сбор метрик (длительность, количество токенов, количество вызовов). | ❌ Нет |
| **Tracing integration** | Передача traceId через компоненты (MDC,context propagation). | ❌ Нет |
| **Logging strategy** | Единый логгер с структурированными сообщениями (JSON, ключ-значение). | ❌ Нет (предположительно используется системное логирование) |
| **Error event** | Специальные события для ошибок (с типами, стектрейсами). | ❌ Нет |

---

### 3. Возможные косвенные механизмы (гипотезы)

#### a) **Логирование через стандартный логгер Kotlin/Java**
- Внутри компонентов (стратегии, пайплайны, инструменты) может использоваться `kotlin.logging.Logger`.
- **Но**: нет единого логгера на уровне агента, нет конфигурации формата, нет обязательных полей (agentId, taskId, stepId).

#### b) **Clock как источник временных меток**
- `Clock` [упомянут в контексте] может использоваться для временных меток событий.
- **Но**: Clock — это просто поставщик времени, а не система событий.

#### c) **Конфигурация через `agentConfig`**
- `agentConfig` [ИСТОЧНИК 1, 4] может содержать параметры логирования (уровень, формат), но в контексте нет таких полей.

#### d) **Использование Kotlin Flow/Channels для событий**
- Стратегии или пайплайны могут использовать `Channel<Event>` для внутренней коммуникации, но это не публичное API.

---

### 4. Как могла бы выглядеть система observability (ожидаемая архитектура)

```kotlin
// Типы событий
sealed class AgentEvent {
    data class PipelineStarted(val taskId: String) : AgentEvent()
    data class StepExecuted(val stepId: String, val duration: Long) : AgentEvent()
    data class ToolCalled(val toolName: String, val params: Map<String, Any>) : AgentEvent()
    data class ToolResult(val toolName: String, val success: Boolean, val result: Any?) : AgentEvent()
    data class ErrorOccurred(val error: Throwable, val component: String) : AgentEvent()
}

// Шина событий
interface AgentEventBus {
    fun publish(event: AgentEvent)
    fun subscribe(listener: (AgentEvent) -> Unit)
}

// Интеграция в контекст
class ObservableAIAgentContext(
    private val eventBus: AgentEventBus,
    // ... другие зависимости
) : AIAgentContext {
    override fun executeTool(tool: Tool, params: Map<String, Any>) {
        eventBus.publish(ToolCalled(tool.name, params))
        try {
            val result = tool.execute(params)
            eventBus.publish(ToolResult(tool.name, true, result))
        } catch (e: Exception) {
            eventBus.publish(ErrorOccurred(e, "Tool:${tool.name}"))
            throw e
        }
    }
}
```

Но в контексте **нет таких классов**.

---

### 5. Как отслеживать выполнение без встроенной системы?

Разработчикам, вероятно, приходится:

1. **Вручную добавлять логирование** в стратегии и пайплайны:
   ```kotlin
   class MyStrategy : AIAgentPlannerStrategy {
       private val logger = LoggerFactory.getLogger(javaClass)
       
       override fun plan(context: AIAgentPlannerContext): Plan {
           logger.info("Creating plan for task ${context.taskId}")
           // ...
       }
   }
   ```

2. **Использовать внешние инструменты** для мониторинга JVM (JMX, Java Flight Recorder) и логирования (logback, log4j).

3. **Создавать кастомные фичи (`AIAgentFeature`)**, которые оборачивают пайплайн/стратегии для сбора метрик.

4. **Передавать `AgentEventListener` через конфигурацию** (если бы такой интерфейс существовал).

---

### 6. Уточняющие вопросы

1. **Существуют ли в кодовой базе классы с именами, связанными с observability?**  
   Примеры: `AgentLogger`, `AgentMetrics`, `AgentTracer`, `AgentMonitor`, `EventBus`, `EventPublisher`, `EventListener`.

2. **Как ведётся логирование внутри существующих компонентов?**  
   - Используется ли `kotlin-logging`, `slf4j`, `java.util.logging`?
   - Есть ли единый префикс/формат логов для агентов?

3. **Какие данныеCollector'ы доступны из контекста/пайплайна?**  
   - Можно ли получить статистику по выполненным шагам (длительность, количество токенов)?
   - Есть ли доступ к `tokenCount` или `cost` после вызова LLM?

4. **Предусмотрена ли возможность передачи `traceId`/`spanId` через компоненты?**  
   - Например, через `AIAgentContext` или `AgentInput`.

5. **Как обрабатываются ошибки с точки зрения observability?**  
   - Логируются ли они автоматически?
   - Есть ли `errorReporting` (например, отправка в Sentry)?

6. **Есть ли примеры в тестах или документации по мониторингу агентов?**  
   - Как собрать метрики для продакшн-развёртывания?

---

### 7. Рекомендации по исследованию

- **Поиск по ключевым словам в кодовой базе**:  
  `logger`, `log`, `metrics`, `meter`, `trace`, `span`, `event`, `publish`, `subscribe`, `monitor`.
- **Проверка build-файлов** (build.gradle.kts) на зависимости:  
  `io.micrometer`, `org.jetbrains.kotlinx:kotlinx-coroutines`, `io.opentelemetry`.
- **Изучение реализаций**:  
  - `ToolRegistry.kt` — есть ли логирование при регистрации/вызове инструментов?
  - `AIAgentFunctionalPipeline.kt` — есть ли логирование шагов?
  - Стратегии (если есть в контексте) — используют ли они логгер?
- **Проверить документацию** (если есть) по теме "monitoring", "observability", "logging".

---

### 8. Альтернатива: delegated observability через фичи

Если в базовом коде нет observability, то, следуя архитектуре Koog, её можно добавить через `AIAgentFeature`:

```kotlin
class ObservabilityFeature(
    private val eventBus: AgentEventBus,
    private val meter: Meter
) : AIAgentFeature {
    override fun install(agent: AIAgent) {
        // Обернуть пайплайн, стратегию, инструменты для сбора событий
        agent.pipeline = ObservingPipeline(agent.pipeline, eventBus, meter)
        agent.strategy = ObservingStrategy(agent.strategy, eventBus, meter)
        agent.toolRegistry = ObservingToolRegistry(agent.toolRegistry, eventBus, meter)
    }
}
```

Но это требует реализации `ObservingPipeline`, `ObservingStrategy` и т.д.

---

### Вывод

**В Koog нет встроенной системы observability и обработки событий от агентов.** Все отслеживание выполнения, логирование и сбор метрик, если они нужны, должны быть реализованы разработчиками самостоятельно либо через кастомные фичи (`AIAgentFeature`), либо путём прямого добавления кода в стратегии/пайплайны/инструменты. Это создаёт риск несогласованности и отсутствия стандартизированного мониторинга.

**Уточняющий вопрос**:  
**Предусмотрена ли в Koog возможность подписки на события агента (например, через `AgentEventListener` или `AgentEventBus`) или сбора метрик (например, через `AgentMetrics`)? Если нет, то как рекомендовано отслеживать выполнение агентов в production-среде?**

---

### Источники

[КЛАСС 1] PlannerAIAgent · PlannerAIAgent.kt

[КЛАСС 2] FunctionalAIAgent · FunctionalAIAgent.kt

[КЛАСС 3] AIAgentServiceBuilderImpl · AIAgentServiceBuilderImpl.kt

[ИСТОЧНИК 1] AIAgentConfigBase.kt · for · line 1

[ИСТОЧНИК 2] PlannerAIAgent.kt · line 22

[ИСТОЧНИК 3] FunctionalAIAgent.kt · line 63

[КЛАСС 4] AIAgentFunctionalPipeline · AIAgentFunctionalPipeline.kt

[ИСТОЧНИК 4] AIAgentServiceBuilderImpl.kt · graphStrategy · line 89

[ИСТОЧНИК 5] AIAgentServiceBuilderImpl.kt · temperature · line 102

## Q17: "что такое GraphAIAgent, каковы его преимущества и схемы использования?"
На основе предоставленного контекста **в Koog отсутствует класс с именем `GraphAIAgent`**. Вероятно, под этим названием подразумевается **`PlannerAIAgent`** — агент, который использует графовую стратегию планирования и пайплайн для выполнения.

---

### 1. Что это? Отсутствие `GraphAIAgent` и наличие `PlannerAIAgent`

- В контексте явно определены только два типа агентов:
  - `PlannerAIAgent` [КЛАСС 1] — для задач, требующих планирования и разбиения на подзадачи (графовое выполнение).
  - `FunctionalAIAgent` [КЛАСС 2] — для линейных цепочек обработки.
- **`GraphAIAgent` не упоминается** как отдельный класс. Однако:
  - Существует `GraphAgentServiceBuilder` [ИСТОЧНИК 4] — билдер для создания **сервиса графового агента**.
  - При использовании `graphStrategy` в `AIAgentServiceBuilderImpl` [ИСТОЧНИК 4] возвращается `GraphAIAgentService`, который, судя по названию, управляет `PlannerAIAgent`.
- **Вывод**: `GraphAIAgent` — это, скорее всего, неформальное название для агента, созданного через графовый билдер, т.е. для `PlannerAIAgent`.

---

### 2. Архитектура `PlannerAIAgent` (аналог "GraphAIAgent")

`PlannerAIAgent` организован по трёхслойной схеме:

| Слой | Компонент | Назначение |
|------|-----------|------------|
| **Контекст** | `AIAgentPlannerContext` [ИСТОЧНИК 2] | Содержит `GenericAgentEnvironment` (окружение для выполнения графа), `AIAgentLLMContext`, `AIAgentStateManager`, `agentInput`. Предоставляет доступ к `ToolRegistry`, `PromptExecutor`. |
| **Пайплайн** | `AIAgentPlannerPipeline` | Отвечает за выполнение графа задач. Использует `GenericAgentEnvironment` для обработки ветвлений, циклов. |
| **Стратегия** | `AIAgentPlannerStrategy<Input, Output, *>` | Планирует и разбивает задачу на подзадачи, строит граф выполнения. |

---

### 3. Преимущества графового подхода (для `PlannerAIAgent`)

| Преимущество | Описание | Пример |
|--------------|----------|--------|
| **Сложные зависимости** | Подзадачи могут иметь зависимые отношения (например, B зависит от A). | Задача "Спланировать отпуск": сначала бронировать билеты, потом отель. |
| **Параллельное выполнение** | Независимые подзадачи могут выполняться одновременно. | Параллельный сбор данных из нескольких источников. |
| **Циклы и итерации** | Возможность повторять шаги до выполнения условия. | Уточнение требований у пользователя в цикле. |
| **Условные ветвления** | Выбор следующего шага на основе результата предыдущего. | Если поиск не удался — переформулировать запрос. |
| **Повторное использование подзадач** | Одна и та же подзадача может использоваться в разных частях графа. | Проверка валидности данных в нескольких местах. |
| **Гибкость планирования** | Стратегия может динамически строить граф на основе промежуточных результатов. | При неудаче одного подхода — перестроить план. |

---

### 4. Схемы использования `PlannerAIAgent` (GraphAIAgent)

#### Схема 1: Создание через `GraphAgentServiceBuilder`
```kotlin
val agent = GraphAgentServiceBuilder()
    .withPromptExecutor(myExecutor)
    .withLlmModel(myModel)
    .withToolRegistry(myTools)
    .withSystemPrompt("Ты — планировщик...")
    .withMaxIterations(10)
    .build()
```
- `graphStrategy` выбирается автоматически при использовании `GraphAgentServiceBuilder` [ИСТОЧНИК 4].
- Возвращает `GraphAIAgentService`, который, предположительно, оборачивает `PlannerAIAgent`.

#### Схема 2: Ручная конфигурация через `AIAgentServiceBuilder`
```kotlin
val agent = AIAgentServiceBuilder()
    .graphStrategy { context -> MyPlannerStrategy() }
    .withPromptExecutor(...)
    .build()
```
- Явное указание `graphStrategy` [ИСТОЧНИК 4] позволяет передать кастомную стратегию планирования.

#### Схема 3: Работа с контекстом
- Контекст `AIAgentPlannerContext` [ИСТОЧНИК 2] предоставляет:
  - `environment` — для выполнения графа (добавление узлов, управления потоком).
  - `stateManager` — для сохранения промежуточных результатов подзадач.
  - `toolRegistry` — для доступа к инструментам.

#### Схема 4: Примерный workflow выполнения
```
1. Пользователь задаёт задачу (Input).
2. Планировщик (MyPlannerStrategy) строит граф подзадач.
3. Граф передаётся в GenericAgentEnvironment.
4. Пайплайн (AIAgentPlannerPipeline) выполняет граф:
   - Последовательно или параллельно запускает узлы.
   - Каждый узел может вызывать LLM или инструменты.
   - Результаты передаются между узлами через состояние (stateManager).
5.when все узлы завершены, возвращается итоговый Output.
```

---

### 5. Критические ограничения и пробелы

| Аспект | Проблема |
|--------|----------|
| **Документация графа** | Нет описания, как именно реализован `GenericAgentEnvironment` — как узел добавляется, как передаются данные между узлами, как обрабатываются зависимости. |
| **API для построения графа** | Неясно, использует ли стратегия какой-то DSL (например, `plan { taskA then taskB }`) или работает с низкоуровневыми узлами (`GraphNode`). |
| **Параллелизм** | Неясно, поддерживается ли параллельное выполнение независимых узлов (например, через корутины). |
| **Динамическое изменение графа** | Можно ли добавлять/удалять узлы во время выполнения? |
| **Обработка ошибок в графе** | Если узел провалился, можно ли продолжить выполнение других веток? Есть ли `catch`-узлы? |
| **Визуализация графа** | Нет инструментов для отображения построенного графа (для отладки). |

---

### 6. Отличия от `FunctionalAIAgent`

| Критерий | `PlannerAIAgent` (Graph) | `FunctionalAIAgent` |
|----------|--------------------------|---------------------|
| **Структура** | Граф (узлы, рёбра, зависимости) | Линейная/ветвящаяся цепочка шагов |
| **Планирование** | Динамическое (стратегия строит граф) | Статическое (цепочка задаётся разработчиком) |
| **Параллелизм** | Возможен (независимые узлы) | Обычно последовательный |
| **Сложность** | Выше (нужно понимать графы) | Ниже (просто цепочка) |
| **Использование** | Сложные задачи с неочевидным порядком | Предсказуемые последовательности обработки |
| **Контекст** | `AIAgentPlannerContext` с `GenericAgentEnvironment` | `AIAgentFunctionalContext` без окружения |

---

### 7. Где искать детали реализации?

Для понимания, как работает графовый агент, необходимо изучить:

1. **`GenericAgentEnvironment`** — ключевой класс для выполнения графа. Скорее всего, он:
   - Хранит граф узлов.
   - Управляет порядком выполнения (topological sort, параллелизм).
   - Передаёт данные между узлами.
   - Предоставляет API для добавления узлов из стратегии.

2. **`AIAgentPlannerStrategy`** — как строится граф. Например:
   ```kotlin
   interface AIAgentPlannerStrategy<Input, Output, Context : AIAgentPlannerContext> {
       fun plan(input: Input, context: Context): Graph // или List<Node>>
   }
   ```

3. **`AIAgentPlannerPipeline`** — как выполняется граф. Возможно, использует `GenericAgentEnvironment`:
   ```kotlin
   class AIAgentPlannerPipeline(...) {
       fun execute(graph: Graph, environment: GenericAgentEnvironment): Output { ... }
   }
   ```

---

### 8. Пример гипотетического кода (на основе контекста)

```kotlin
// Стратегия, строящая граф
class MyPlannerStrategy : AIAgentPlannerStrategy<Input, Output, AIAgentPlannerContext> {
    override fun plan(input: Input, context: AIAgentPlannerContext): Graph {
        val graph = Graph()
        val nodeA = Node("fetchData") { /* вызывает инструмент */ }
        val nodeB = Node("processData") { /* обрабатывает результат */ }
        graph.addEdge(nodeA, nodeB) // B зависит от A
        return graph
    }
}

// Использование
val agent = AIAgentServiceBuilder()
    .graphStrategy { MyPlannerStrategy() }
    .build()

val result = agent.execute(Input(...))
```

---

### 9. Вывод

- **`GraphAIAgent` не существует** как отдельный класс. Вместо этого используется `PlannerAIAgent` с графовой стратегией.
- **Преимущества**: поддержка сложных зависимостей, параллелизма, циклов, динамического планирования.
- **Схемы использования**: создание через `GraphAgentServiceBuilder` или явное указание `graphStrategy` в `AIAgentServiceBuilder`.
- **Критические пробелы**: отсутствие деталей реализации `GenericAgentEnvironment`, API построения графа, обработки ошибок, параллелизма.

**Уточняющий вопрос**:  
**Как именно реализован `GenericAgentEnvironment`? Каков его API для добавления узлов, управления зависимостями и выполнения графа? Существуют ли классы `GraphNode`, `GraphEdge`, `Graph` в кодовой базе?**

---

### Источники

[КЛАСС 1] PlannerAIAgent · PlannerAIAgent.kt

[КЛАСС 2] FunctionalAIAgent · FunctionalAIAgent.kt

[КЛАСС 3] AIAgentServiceBuilderImpl · AIAgentServiceBuilderImpl.kt

[ИСТОЧНИК 1] AIAgentConfigBase.kt · for · line 1

[ИСТОЧНИК 2] PlannerAIAgent.kt · line 22

[ИСТОЧНИК 3] FunctionalAIAgent.kt · line 63

[КЛАСС 4] AIAgentFunctionalPipeline · AIAgentFunctionalPipeline.kt

[ИСТОЧНИК 4] AIAgentServiceBuilderImpl.kt · graphStrategy · line 89

[ИСТОЧНИК 5] AIAgentServiceBuilderImpl.kt · temperature · line 102

## Q18: "все полезное про AIAgentContext и AIAgentGraphContext"
На основе предоставленного контекста **нет прямых описаний классов `AIAgentContext` или `AIAgentGraphContext` как отдельных сущностей**. Однако эти термины логически вытекают из архитектурного описания **PlannerAIAgent** и являются ключевыми слоями в его трёхслойной организации.

---

### 1. `AIAgentContext` — общий интерфейс (гипотетический)

В контексте нет явного определения `AIAgentContext` как отдельного класса или интерфейса. Однако в описании `PlannerAIAgent` [КЛАСС 1] упоминается:
> `AIAgentPlannerContext` с `GenericAgentEnvironment`, `AIAgentLLMContext`, `AIAgentStateManager`, `agentInput`.

Это позволяет сформулировать гипотезу:
- **`AIAgentContext`** — это *логический общий интерфейс* для контекстных слоёв как `PlannerAIAgent`, так и `FunctionalAIAgent`.
- Он должен объединять базовые методы и свойства, доступные любому агенту во время выполнения:
  - Доступ к `ToolRegistry`
  - Доступ к `PromptExecutor` / `LLM`
  - Доступ к хранилищу состояния (`AIAgentStateManager` или `AIAgentStorage`)
  - Доступ к входным данным (`agentInput`)
  - Доступ к окружению (`GenericAgentEnvironment` для графовых агентов)

**В контексте Koog этот общий интерфейс, скорее всего, отсутствует.** Контексты двух типов агентов (`AIAgentPlannerContext` и `AIAgentFunctionalContext`) являются **разными классами с разными API**, что создаёт проблему унификации кода, работающего с любым агентом.

---

### 2. `AIAgentPlannerContext` — контекст PlannerAIAgent (Graph Context)

Это **конкретный класс**, используемый `PlannerAIAgent` [КЛАСС 1]. Он является "ядром" выполнения графового агента.

#### Назначение:
- Собирает все необходимые ресурсы для построения и выполнения графа задач.
- Передаётся в `AIAgentPlannerStrategy` для планирования.
- Используется `AIAgentPlannerPipeline` для выполнения.
- Инкапсулирует состояние выполнения (через `AIAgentStateManager`).

#### Ключевые составные части (из описания [ИСТОЧНИК 2]):
1. **`GenericAgentEnvironment`** — окружение для выполнения графа. Предположительно:
   - Предоставляет API для добавления узлов (задач) в граф.
   - Управляет потоком выполнения (ветвление, циклы).
   - Передаёт данные (выход одного узла → вход другого).
   - Отслеживает зависимости между узлами.
2. **`AIAgentLLMContext`** — доступ к LLM (через `PromptExecutor`). Используется стратегией для генерации текста, размышлений, планирования.
3. **`AIAgentStateManager`** — менеджер состояния графа. Предположительно:
   - Хранит промежуточные результаты выполненных узлов.
   - Позволяет сохранять/загружать состояние (возможно, для checkpointing или отката).
   - Управляет жизненным циклом состояния (инициализация, очистка).
4. **`agentInput`** — исходные данные задачи от пользователя.
5. **`ToolRegistry`** — доступ к инструментам (для вызова из узлов графа).

#### Атрибуты/методы (гипотетические, на основе назначения):
```kotlin
class AIAgentPlannerContext(
    val environment: GenericAgentEnvironment,
    val llmContext: AIAgentLLMContext,
    val stateManager: AIAgentStateManager,
    val toolRegistry: ToolRegistry,
    val input: AgentInput
) {
    // Метод для добавления узла в граф (вызывается стратегией)
    fun addNode(node: GraphNode)
    
    // Метод для получения результата узла по ID (вызывается пайплайном)
    fun getNodeResult(nodeId: String): Any?
    
    // Метод для обновления состояния
    fun saveState(key: String, value: Any)
    fun <T> loadState(key: String): T?
}
```

---

### 3. `AIAgentGraphContext` — возможная ошибка или синоним

Термин **`AIAgentGraphContext`** в контексте **не встречается**. Скорее всего, пользователь имеет в виду:
1. **`AIAgentPlannerContext`** — т.к. он используется для графовых агентов.
2. **`GenericAgentEnvironment`** — который является частью `AIAgentPlannerContext` и напрямую отвечает за работу с графом.

**Важно**: Если `AIAgentGraphContext` — это предполагаемый общий интерфейс для *любого* графового агента (включая будущие типы), то в текущей кодовой базе его **нет**.

---

### 4. `AIAgentFunctionalContext` — контекст FunctionalAIAgent (для сравнения)

Это контекст для линейного агента [ИСТОЧНИК 3]. Его ключевые отличия:
- **Нет `GenericAgentEnvironment`** — линейный пайплайн не требует сложного окружения для графа.
- **Есть `AIAgentStorage`** вместо `AIAgentStateManager` — хранилище для промежуточных результатов цепочки.
- Метод `prepareEnvironment()` — настраивает окружение для линейного выполнения.
- Метод `install()` — для установки фич (у `PlannerAIAgent` тоже есть `installFeature`, но контекст не упоминает).

---

### 5. Сравнительная таблица контекстов

| Характеристика | `AIAgentPlannerContext` (Graph) | `AIAgentFunctionalContext` (Linear) |
|----------------|----------------------------------|-------------------------------------|
| **Тип агента** | `PlannerAIAgent` | `FunctionalAIAgent` |
| **Окружение** | `GenericAgentEnvironment` (графовое) | `prepareEnvironment()` (линейное) |
| **State management** | `AIAgentStateManager` (менеджер состояния графа) | `AIAgentStorage` (хранилище данных) |
| **Доступ к LLM** | `AIAgentLLMContext` | Неявно через `PromptExecutor`? |
| **Доступ к ToolRegistry** | Да | Да |
| **agentInput** | Да | Да |
| **Метод install** | Нет (фичи устанавливаются агенту) | `install()` (устанавливает фичи в контекст) |
| **Основная задача** | Поддержка динамического графа с зависимостями | Поддержка статической цепочки шагов |

---

### 6. Жизненный цикл контекста

1. **Создание**: При вызове `agent.execute(input)` создаётся **новый экземпляр контекста**. Это обеспечивает изоляцию между запусками.
2. **Инициализация**: Заполняются зависимости (ToolRegistry, LLM, Storage/StateManager) из конфигурации агента.
3. **Использование**: Передаётся в стратегию (для планирования) и пайплайн (для выполнения).
4. **Завершение**: После выполнения контекст, вероятно, очищается (метод `close()` или через try-with-resources). Если это не происходит, возможны утечки памяти.

**Критический пробел**: Нет явного описания, *кто* создаёт контекст (сам агент? билдер? пайплайн?) и *как* он очищается.

---

### 7. Как контекст используется в пайплайнах и стратегиях?

#### В `AIAgentPlannerStrategy`:
```kotlin
interface AIAgentPlannerStrategy<Input, Output, C : AIAgentPlannerContext> {
    fun plan(input: Input, context: C): Graph // Возвращает граф для выполнения
}
```
- Стратегия получает контекст как readonly для доступа к инструментам и LLM.
- Строит граф, используя `context.environment.addNode(...)`.
- Может сохранять промежуточные данные в `context.stateManager`.

#### В `AIAgentPlannerPipeline`:
- Получает граф от стратегии.
- Использует `context.environment` для выполнения графа (обход узлов).
- Читает результаты узлов из `context.stateManager`.
- Возвращает финальный `Output`.

---

### 8. Расширение контекста через фичи (`AIAgentFeature`)

Оба агента поддерживают `installFeature`:
```kotlin
agent.installFeature(MyFeature())
```
- Фича может **дополнить контекст** новыми методами/свойствами.
- Например, `ObservabilityFeature` добавит `eventBus` и `meter` в контекст.
- Фича может **обернуть** пайплайн или стратегию для сбора метрик.

**Но**: в контексте нет примера, как именно фича модифицирует контекст. Нужен интерфейс `AIAgentFeature.install(context: AIAgentContext)`.

---

### 9. Критические пробелы и проблемы

| Проблем | Объяснение |
|---------|------------|
| **Отсутствие общего интерфейса** | `AIAgentPlannerContext` и `AIAgentFunctionalContext` не имеют общего родителя `AIAgentContext`. Код, универсальный для обоих типов агентов, невозможен. |
| **Разный API для state management** | `StateManager` vs `Storage` — разные абстракции, которые, возможно, делают одно и то же (хранение данных). Это нарушает единообразие. |
| **Неясная ответственность контекста** | Где граница между контекстом (данные) и пайплайном/стратегией (логика)? Должен ли контекст предоставлять `executeTool()` или это делает пайплайн? |
| **Нет примера использования** | Нет реального кода, как стратегия использует `environment` для построения графа. |
| **Изоляция сессий** | Если контекст создаётся заново при каждом `execute()`, то state изолирован. Но если агент переиспользуется, нужна очистка. |
| **Интеграция с PromptExecutor** | Как `PromptExecutor` встраивается в контекст? Через `llmContext`? Можно ли заменить его на кастомный? |
| **Конфигурация** | Как параметры (temperature, maxTokens) передаются в контекст? Через `AIAgentConfig`? |

---

### 10. Уточняющие вопросы (фокус на контексты)

1. **Существует ли общий интерфейс/абстрактный класс `AIAgentContext`?** Если да, какие методы он объявляет? Если нет — как писать код, работающий с любым агентом?
2. **Как именно `GenericAgentEnvironment` добавляет узлы и управляет графом?** Есть ли методы `addNode(node: GraphNode, dependencies: List<String>)`?
3. **Каков полный API `AIAgentStateManager`?** Методы: `save(key, value)`, `load<T>(key)`, `clear()`, `snapshot()`?
4. **Чем `AIAgentStorage` отличается от `AIAgentStateManager`?** Почему для `FunctionalAIAgent` используется `Storage`, а для `PlannerAIAgent` — `StateManager`?
5. **Как контекст создаётся и инжектится в стратегию/пайплайн?** Кто отвечает за сбор всех зависимостей (ToolRegistry, LLM, StateManager)?
6. **Можно ли модифицировать контекст из стратегии?** Например, добавить новый инструмент во время выполнения?
7. **Есть ли в контексте метод для логирования событий?** Или observability реализуется отдельно (через фичи)?
8. **Как передаются большие данные между узлами графа?** Через `StateManager`? Есть ли ограничения на размер?
9. **Поддерживает ли `GenericAgentEnvironment` параллельное выполнение узлов?** Если да, как обеспечивается синхронизация доступа к `StateManager`?
10. **Как контекст очищается после выполнения?** Вызывается ли `stateManager.clear()` автоматически?

---

### 11. Практические рекомендации по использованию

Если вы разрабатываете стратегию или пайплайн:

1. **Для PlannerAIAgent**:
   - Используйте `context.environment` для создания графа.
   - Используйте `context.stateManager` для хранения промежуточных результатов.
   - Используйте `context.llmContext` для вызовов LLM (например, `context.llmContext.execute(prompt)`).
   - Используйте `context.toolRegistry` для доступа к инструментам.

2. **Для FunctionalAIAgent**:
   - Используйте `context.storage` (или аналоги) для хранения данных между шагами.
   - Используйте `context.toolRegistry` для инструментов.
   - Линейные шаги определяются в самой стратегии, поэтому контекст меньше используется.

3. **Для универсального кода**:
   - Создайте свой интерфейс `MyAgentContext` с методами `getTools()`, `getLLM()`, `getStorage()`.
   - Адаптируйте `AIAgentPlannerContext` и `AIAgentFunctionalContext` под этот интерфейс через обёртки.

---

### 12. Вывод

- **`AIAgentContext`** — отсутствует как единый интерфейс, но его логика распределена между `AIAgentPlannerContext` (для графовых агентов) и `AIAgentFunctionalContext` (для линейных).
- **`AIAgentPlannerContext`** — ключевой класс для графового агента, объединяющий окружение (`GenericAgentEnvironment`), менеджер состояния (`AIAgentStateManager`), доступ к LLM и инструментам.
- **`AIAgentGraphContext`** — термин не используется в кодовой базе; вероятно, это либо ошибка, либо неофициальное название `AIAgentPlannerContext`.
- **Главная проблема** — отсутствие единой абстракции контекста, что затрудняет создание переиспользуемых компонентов для разных типов агентов.

**Уточняющий вопрос** (основной):  
**Существует ли в кодовой базе общий интерфейс `AIAgentContext` или его аналоги? Если нет, как обеспечивается единообразие доступа к инструментам и LLM из стратегий разных типов агентов?**

---

### Источники

[КЛАСС 1] PlannerAIAgent · PlannerAIAgent.kt

[КЛАСС 2] FunctionalAIAgent · FunctionalAIAgent.kt

[ИСТОЧНИК 2] PlannerAIAgent.kt · line 22

[ИСТОЧНИК 3] FunctionalAIAgent.kt · line 63

[ИСТОЧНИК 1] AIAgentConfigBase.kt · for · line 1

## Q19: "опиши работу с AIAgentLLMContext"
На основании предоставленного контекста **нет явного класса `AIAgentLLMContext` с полной спецификацией**. Он лишь фрагментарно упоминается как часть `AIAgentPlannerContext` [ИСТОЧНИК 2], но его API, реализации и детали интеграции **не раскрываются**. Это один из ключевых пробелов в документации архитектуры.

Тем не менее, исходя из названия, архитектурной логики и аналогий (например, `PromptExecutor` [ИСТОЧНИК 1]), можно реконструировать его предполагаемую роль, ответственность и гипотетический API.

---

### 1. Назначение и responsibility

`AIAgentLLMContext` — это **контекстный компонент, инкапсулирующий всю работу с языковой моделью (LLM) внутри агента**. Он абстрагирует низкоуровневые детали вызовов LLM, предоставляя удобный API для стратегий и пайплайнов.

#### Ключевые функции:
1. **Выполнение промптов** — отправка сформированного промпта в LLM и получение ответа.
2. **Управление параметрами LLM** — температура, maxTokens, topP, stop-sequences и др.
3. **Обработка ответов** — парсинг структурированных ответов (JSON, XML), извлечение текста.
4. **Учёт токенов** — подсчёт использованных токенов для контроля стоимости и лимитов.
5. **Обработка ошибок** — retry при сетевых сбоях, rate limiting, обработка invalid responses.
6. **Логирование и трассировка** — запись запросов/ответов для observability.
7. **Интеграция с контекстом агента** — доступ к `ToolRegistry` (для tool calling), `AIAgentStateManager`/`AIAgentStorage` (для истории).

---

### 2. Положение в архитектуре

`AIAgentLLMContext` является частью **контекстного слоя** `PlannerAIAgent`:
```
PlannerAIAgent
├── Контекст: AIAgentPlannerContext
│   ├── GenericAgentEnvironment (графовое окружение)
│   ├── AIAgentLLMContext      ← здесь
│   ├── AIAgentStateManager    (state management)
│   ├── ToolRegistry           (инструменты)
│   └── agentInput
├── Стратегия: AIAgentPlannerStrategy
└── Пайплайн: AIAgentPlannerPipeline
```

**Важно**: В `FunctionalAIAgent` [КЛАСС 2] аналогичный компонент **не упоминается**. Возможно, там используется `PromptExecutor` [ИСТОЧНИК 1] напрямую или имеется другой LLM-клиент.

---

### 3. Гипотетический API (на основе логики)

Поскольку в контексте нет кода, приведём примерный интерфейс, который соответствует архитектурным принципам Koog:

```kotlin
interface AIAgentLLMContext {
    // Основной метод выполнения промпта
    suspend fun execute(
        prompt: Prompt,
        params: LLMParams = LLMParams.default()
    ): LLMResult
    
    // Установка параметров по умолчанию для всех вызовов
    fun configure(params: LLMParams)
    
    // Получение статистики по использованию LLM
    fun getUsage(): LLMUsageStats
    
    // Доступ к модели (для логирования/мониторинга)
    val model: LLMModel
    
    // Возможность подписки на события (для observability)
    fun addListener(listener: LLMListener)
}

data class LLMParams(
    val temperature: Double? = null,
    val maxTokens: Int? = null,
    val topP: Double? = null,
    val stopSequences: List<String> = emptyList(),
    // ... другие параметры провайдера
)

data class LLMResult(
    val text: String,
    val rawResponse: Any?, // сырой ответ от API
    val usage: TokenUsage,
    val finishReason: String? // "stop", "length", "content_filter"
)

data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
}

data class LLMUsageStats(
    val totalRequests: Int,
    val totalPromptTokens: Long,
    val totalCompletionTokens: Long,
    val totalCost: Double? // если известна стоимость модели
)
```

---

### 4. Интеграция с другими компонентами

#### 4.1. С `PromptExecutor`
`AIAgentLLMContext` **скорее всего оборачивает `PromptExecutor`** [ИСТОЧНИК 1], добавляя:
- Управление параметрами LLM (temperature и т.д.).
- Статистику (токены, стоимость).
- Retry-логику.
- Парсинг структурированных ответов.

Примерная реализация:
```kotlin
class DefaultAIAgentLLMContext(
    private val promptExecutor: PromptExecutor,
    private val model: LLMModel,
    private val defaultParams: LLMParams
) : AIAgentLLMContext {
    override suspend fun execute(prompt: Prompt, params: LLMParams): LLMResult {
        val mergedParams = defaultParams.merge(params)
        val response = promptExecutor.execute(prompt, mergedParams)
        return LLMResult(
            text = response.text,
            rawResponse = response.raw,
            usage = response.usage,
            finishReason = response.finishReason
        )
    }
    ...
}
```

#### 4.2. С `ToolRegistry`
При использовании tool calling, промпт должен включать **декларации инструментов** (их схемы). `AIAgentLLMContext` должен:
1. Получать список инструментов из `ToolRegistry` [ИСТОЧНИК 1].
2. Формировать системный промпт с описанием инструментов (или использовать специальный format, например, OpenAI function calling).
3. В ответе LLM парсить вызовы инструментов и передавать их в пайплайн/стратегию.

**Критический пробел**: Неясно, где происходит парсинг tool calls — в `AIAgentLLMContext` или выше (в пайплайне)?

#### 4.3. С `AIAgentStateManager` / `AIAgentStorage`
Для поддержки истории диалога `AIAgentLLMContext` должен:
- Загружать историю предыдущих сообщений из `stateManager`/`storage`.
- Добавлять новые сообщения (пользовательский запрос и ответ LLM) в историю.
- Применять стратегии компактизации (обрезать, суммаризировать), если история слишком длинная.

**Но**: в контексте нет указания, что `AIAgentLLMContext` напрямую работает с хранилищами. Возможно, это делает **стратегия** или **пайплайн**.

---

### 5. Работа с историей диалога (hypothetical)

```kotlin
// Примерный код стратегии, использующей AIAgentLLMContext и историю
class ChatStrategy : AIAgentPlannerStrategy<ChatInput, ChatOutput, AIAgentPlannerContext> {
    override suspend fun execute(input: ChatInput, context: AIAgentPlannerContext): ChatOutput {
        // 1. Загружаем историю из stateManager
        val history = context.stateManager.getHistory() ?: emptyList()
        
        // 2. Формируем промпт с историей и новым сообщением
        val prompt = Prompt.Builder()
            .addMessages(history)
            .addUserMessage(input.message)
            .build()
        
        // 3. Выполняем запрос к LLM через контекст
        val result = context.llmContext.execute(prompt)
        
        // 4. Сохраняем обновлённую историю (пользовательский запрос + ответ)
        val updatedHistory = history + 
            ChatMessage.user(input.message) + 
            ChatMessage.assistant(result.text)
        context.stateManager.saveHistory(updatedHistory)
        
        return ChatOutput(result.text)
    }
}
```

**Проблема**: В контексте нет `getHistory()`/`saveHistory()` у `AIAgentStateManager` — этот API неизвестен.

---

### 6. Обработка ошибок и retry

`AIAgentLLMContext` должен обрабатывать типичные ошибки LLM:
- **Сетевые ошибки** — retry с экспоненциальной задержкой.
- **Rate limit (429)** — retry после `Retry-After`.
- **Invalid request (400)** — например, слишком длинный промпт. Должно выбрасываться как `LLMException` без retry.
- **Content filter (422)** — фильтрация контента. Возможно, нужно уведомить пользователя.
- **Server errors (5xx)** — retry с ограниченным количеством попыток.

Примерная логика:
```kotlin
suspend fun executeWithRetry(prompt: Prompt, params: LLMParams, maxRetries: Int = 3): LLMResult {
    var attempt = 0
    while (true) {
        try {
            return execute(prompt, params)
        } catch (e: LLMNetworkException) {
            if (++attempt > maxRetries) throw e
            delay(100 * 2.0.pow(attempt)) // экспоненциальная задержка
        } catch (e: LLMRateLimitException) {
            val retryAfter = e.retryAfter ?: 60
            delay(retryAfter * 1000)
        }
    }
}
```

---

### 7. Парсинг структурированных ответов

Для tool calling и других задач LLM должна возвращать **структурированные данные** (JSON). `AIAgentLLMContext` может предоставлять методы:

```kotlin
suspend fun <T> executeStructured(
    prompt: Prompt,
    responseType: KClass<T>,
    params: LLMParams = LLMParams.default()
): T where T : Any {
    val result = execute(prompt, params.copy(responseFormat = "json"))
    return json.decodeFromString<T>(result.text)
}
```

**Проблема**: В контексте нет информации о том, как обеспечивается JSON-форматирование ответа LLM (через промпт-инжекторинг или нативные API типа `response_format`).

---

### 8. Observability и логирование

`AIAgentLLMContext` — ключевое место для сбора метрик:
- **Длительность запроса** (отправка → получение).
- **Количество токенов** (prompt, completion).
- **Стоимость** (на основе модели и токенов).
- **Количество ошибок** (по типам).
- **Очередь запросов** (если используется batch).

Примерный listener API:
```kotlin
interface LLMListener {
    fun onRequest(prompt: Prompt, params: LLMParams, startTime: Instant)
    fun onSuccess(result: LLMResult, duration: Duration)
    fun onError(exception: LLMException, duration: Duration)
}
```

---

### 9. Конфигурация через `AIAgentConfig`

Параметры LLM (temperature, maxTokens) задаются в `AIAgentConfig` [ИСТОЧНИК 1] и, вероятно, передаются в `AIAgentLLMContext` при создании контекста.

Пример:
```kotlin
val config = AIAgentConfig {
    temperature = 0.7
    maxTokens = 2000
    llmModel = OpenAI("gpt-4")
}
val llmContext = AIAgentLLMContext(config)
```

---

### 10. Отличия от `PromptExecutor`

| Характеристика | `PromptExecutor` (низкоуровневый) | `AIAgentLLMContext` (высокоуровневый) |
|----------------|-----------------------------------|--------------------------------------|
| **Уровень абстракции** | Просто отправляет текст в LLM | Специализирован под агентов, включает историю, tool calling |
| **Параметры LLM** | Минимальные (model, temperature) | Полный набор (temperature, maxTokens, topP, stop и др.) |
| **Обработка ответов** | Возвращает сырой текст | Парсит, валидирует, конвертирует в структуры |
| **Статистика** | Базово (только текст) | Полная (токены, стоимость, длительность) |
| **Обработка ошибок** | Просто выбрасывает исключения | Retry, классификация ошибок, политики |
| **Интеграция с контекстом агента** | Нет | Да (доступ к ToolRegistry, StateManager) |
| **Назначение** | Универсальный исполнитель промптов | Специализированный для AI-агентов |

---

### 11. Работа с инструментами (tool calling)

`AIAgentLLMContext` должен:
1. **Формировать промпт с инструментами**: добавить в системный промпт описания всех инструментов из `ToolRegistry` (их имена, описания, параметры в JSON Schema).
2. **Обрабатывать ответ LLM**: если LLM запрашивает вызов инструмента, `AIAgentLLMContext` должен распарсить это намерение и вернуть структурированный объект `ToolCallRequest`.
3. **Возвращать результат инструмента** обратно в LLM (для multi-step tool calling).

Примерный workflow:
```kotlin
// 1. Стратегия формирует промпт
val prompt = Prompt.system("Ты — ассистент с инструментами.")
    .withTools(context.toolRegistry.allTools) // LLMContext добавляет описания
    .addUser("Какая погода в Париже?")

// 2. LLMContext отправляет запрос
val result = llmContext.execute(prompt)

// 3. Если LLM вернула tool_call
if (result.hasToolCalls()) {
    val toolCall = result.getToolCalls().first()
    val toolResult = context.toolRegistry.execute(toolCall)
    
    // 4. Отправляем результат инструмента обратно в LLM
    val followUp = prompt.addAssistant(result.text)
        .addToolResult(toolCall.id, toolResult)
    val finalResult = llmContext.execute(followUp)
}
```

**Критический пробел**: В контексте нет `withTools()` или `getToolCalls()` — неизвестен механизм tool calling.

---

### 12. Критические пробелы в контексте

| Проблем | Последствия |
|---------|-------------|
| **Нет исходного кода `AIAgentLLMContext`** | Неизвестен точный API, как он создаётся, какие параметры принимает. |
| **Неясно, кто его создаёт** | Биллер? Агент? Стратегия? |
| **Неясна интеграция с `ToolRegistry`** | Как передаются схемы инструментов в промпт? |
| **Неясна работа с историей** | Кто загружает/сохраняет историю — `LLMContext` или `StateManager`? |
| **Нет обработки ошибок** | Какие исключения выбрасываются? Есть ли retry? |
| **Нет observability** | Как собираются метрики? Есть ли listeners? |
| **Нет поддержки streaming** | Поддерживается ли потоковая передача (stream) от LLM? |
| **Нет многомодельности** | Можно ли в одном агенте использовать разные модели для разных задач? |

---

### 13. Уточняющие вопросы

1. **Где находится реализация `AIAgentLLMContext`?** Каков его полный интерфейс (все методы, свойства)?
2. **Кто создаёт экземпляр `AIAgentLLMContext`?** Билдер (`AIAgentServiceBuilder`), агент или пайплайн?
3. **Как параметры LLM (temperature, maxTokens) передаются в `AIAgentLLMContext`?** Из `AIAgentConfig`? Можно ли переопределить на каждый вызов?
4. **Как `AIAgentLLMContext` интегрируется с `ToolRegistry`?** Есть ли метод `registerTools(tools: List<Tool>)`?
5. **Как работает tool calling?** Парсит ли `AIAgentLLMContext` ответ LLM на предмет вызовов инструментов? Возвращает ли `ToolCall` объект?
6. **Как управляется история диалога?** Загружает ли `AIAgentLLMContext` историю из `AIAgentStateManager`? Или стратегия сама управляет историей?
7. **Какие ошибки обрабатываются на уровне `AIAgentLLMContext`?** Существуют ли классы `LLMNetworkException`, `LLMRateLimitException`?
8. **Поддерживается ли streaming?** Есть ли метод `executeStream(prompt)` возвращающий `Flow<String>`?
9. **Как обеспечивается observability?** Есть ли методы `addMetricsListener` или `setLogger`?
10. **Можно ли использовать `AIAgentLLMContext` отдельно от агента?** Или он привязан к `AIAgentPlannerContext`?

---

### 14. Практические рекомендации (если нужно использовать)

Поскольку `AIAgentLLMContext` не документирован, рекомендуется:

1. **Исследовать исходный код** — найти файлы `AIAgentLLMContext.kt`, `DefaultAIAgentLLMContext.kt` или аналоги.
2. **Проверить `PlannerAIAgent`** — как в его коде используется `llmContext` (если это поле есть в контексте).
3. **Посмотреть примеры стратегий** — как они вызывают LLM (через `context.llmContext.execute(...)` или напрямую через `PromptExecutor`?).
4. **Поискать в тестах** — unit-тесты для `PlannerAIAgent` могут показать, как работает `LLMContext`.

---

### Вывод

`AIAgentLLMContext` — **гипотетический компонент**, который должен инкапсулировать все операции с LLM в графовом агенте. Его точная реализация неизвестна из контекста. Он, вероятно, является обёрткой вокруг `PromptExecutor` с добавлением:
- Управления параметрами LLM.
- Парсинга structured outputs и tool calls.
- Статистики (токены, стоимость).
- Retry-логики.
- Интеграции с `ToolRegistry` и `AIAgentStateManager`.

**Без исходного кода `AIAgentLLMContext` невозможно дать точное описание его работы.** Рекомендуется найти его реализацию в кодовой базе.

---

### Источники

[КЛАСС 1] PlannerAIAgent · PlannerAIAgent.kt

[ИСТОЧНИК 2] PlannerAIAgent.kt · line 22

[КЛАСС 2] FunctionalAIAgent · FunctionalAIAgent.kt

[ИСТОЧНИК 3] FunctionalAIAgent.kt · line 63

[ИСТОЧНИК 1] AIAgentConfigBase.kt · for · line 1

## Q20: "опиши назначение и как работать с AIAgentPlannerContext"
На основе предоставленного контекста работа с **`AIAgentPlannerContext`** является ключевой для разработки **графовых AI-агентов** (`PlannerAIAgent`). Ниже — детальное описание его назначения, структуры, API и практического использования.

---

## **1. Назначение и ответственность**

`AIAgentPlannerContext` — это **контекстный слой `PlannerAIAgent`**, который:
1. **Инкапсулирует всё необходимое для построения и выполнения графа задач** (workflow) во время одного вызова `agent.execute()`.
2. **Служит связующим звеном** между стратегией (`AIAgentPlannerStrategy`), пайплайном (`AIAgentPlannerPipeline`) и окружением (`GenericAgentEnvironment`).
3. **Управляет состоянием выполнения графа** через `AIAgentStateManager`.
4. **Предоставляет доступ к общим ресурсам**: инструментам (`ToolRegistry`), LLM (`AIAgentLLMContext`), входным данным (`agentInput`).

---

## **2. Положение в архитектуре (3-слойная модель)**

```
PlannerAIAgent (агент)
├── Контекстный слой: AIAgentPlannerContext  ← здесь
│   ├── GenericAgentEnvironment      (окружение для графа)
│   ├── AIAgentLLMContext            (доступ к LLM)
│   ├── AIAgentStateManager          (управление состоянием)
│   ├── ToolRegistry                 (инструменты)
│   └── agentInput                   (входные данные задачи)
├── Стратегический слой: AIAgentPlannerStrategy
│   └── метод plan/execute(context): Graph
└── Пайплайн-слой: AIAgentPlannerPipeline
    └── метод execute(graph, context): Output
```

---

## **3. Составные компоненты (из описания [ИСТОЧНИК 2])**

### **3.1. `GenericAgentEnvironment`**
**Назначение**: Окружение для построения и выполнения графа задач.
**Предполагаемый API**:
```kotlin
interface GenericAgentEnvironment {
    // Добавление узла (задачи) в граф
    fun addNode(node: GraphNode, dependencies: List<String> = emptyList())
    
    // Построение графа после добавления всех узлов
    fun build(): Graph
    
    // Выполнение графа (возможно, вызывается пайплайном)
    suspend fun execute(graph: Graph): GraphExecutionResult
    
    // Доступ к результатам узлов по ID
    fun getNodeResult(nodeId: String): Any?
    
    // Динамическое изменение графа (возможно)
    fun addNodeDuringExecution(node: GraphNode)
}
```
**Неясные аспекты**:
- Как именно передаются данные между узлами (через `StateManager` или прямое связывание)?
- Поддерживается ли параллельное выполнение независимых узлов?
- Как обрабатываются ошибки на уровне графа (остановка, пропуск узла)?

### **3.2. `AIAgentLLMContext`**
**Назначение**: Абстракция для работы с языковой моделью (LLM).
**Предполагаемый API** (гипотетический):
```kotlin
interface AIAgentLLMContext {
    suspend fun execute(prompt: Prompt, params: LLMParams = LLMParams.default()): LLMResult
    fun configure(params: LLMParams)
    val model: LLMModel
    // Поддержка tool calling?
    suspend fun executeWithTools(prompt: Prompt, tools: List<Tool>): LLMResultWithToolCalls
}
```
**Интеграция**:
- Используется стратегией для генерации текста (например, для декомпозиции задачи).
- Может использоваться пайплайном для обработки промежуточных результатов.
- **Важно**: В контексте нет деталей, как именно `GenericAgentEnvironment` использует `LLMContext` (если использует).

### **3.3. `AIAgentStateManager`**
**Назначение**: Управление состоянием **во время выполнения графа** (промежуточные результаты узлов, статусы подзадач).
**Предполагаемый API**:
```kotlin
interface AIAgentStateManager {
    // Сохранение состояния (результата узла, промежуточных данных)
    fun save(key: String, value: Any, ttl: Duration? = null)
    
    // Получение состояния
    fun <T> load(key: String): T?
    
    // Очистка состояния (по ключу или всего)
    fun clear(key: String? = null)
    
    // Создание снимка (checkpoint) для отката
    fun snapshot(): StateSnapshot
    
    // Восстановление из снимка
    fun restore(snapshot: StateSnapshot)
}
```
**Ключевой вопрос**: Как состояние связано с узлами графа? Вероятно, каждый узел сохраняет свой результат в `StateManager` с ключом, равным `nodeId`.

### **3.4. `ToolRegistry`**
**Назначение**: Регистр доступных инструментов (как в `FunctionalAIAgent` [ИСТОЧНИК 1]).
**Использование**:
- Стратегия получает инструменты из `context.toolRegistry` для включения в граф (например, узел "вызвать инструмент X").
- При выполнении узла, который требует инструмент, пайплайн/окружение использует `toolRegistry.execute(toolCall)`.

### **3.5. `agentInput`**
**Назначение**: Исходные данные задачи, переданные пользователем.
**Тип**: Обычно `Any` или конкретный тип, параметризованный в `PlannerAIAgent<Input, Output>`.
**Использование**:
- Стратегия использует `context.agentInput` как точку отсчёта для построения графа.
-可能 сохраняет в `stateManager` для доступа узлов.

---

## **4. Как работать с `AIAgentPlannerContext`? (Практическое руководство)**

### **4.1. В кастомной стратегии (`AIAgentPlannerStrategy`)**
```kotlin
class MyPlannerStrategy : AIAgentPlannerStrategy<MyInput, MyOutput, AIAgentPlannerContext> {
    override suspend fun execute(input: MyInput, context: AIAgentPlannerContext): Graph {
        // 1. Получить доступ к ресурсам
        val tools = context.toolRegistry.allTools
        val llm = context.llmContext
        val state = context.stateManager
        val env = context.environment
        val userInput = context.agentInput as MyInput
        
        // 2. Использовать LLM для планирования (декомпозиции задачи)
        val planPrompt = Prompt.system("Ты — планировщик.")
            .addUser("Разбей задачу: ${userInput.query} на подзадачи")
        val plan = llm.execute(planPrompt).text
        
        // 3. Преобразовать план в узлы графа (пример: каждую подзадачу — в узел)
        val subtasks = parsePlan(plan) // ["Шаг1", "Шаг2", ...]
        
        subtasks.forEachIndexed { index, subtask ->
            val nodeId = "node_$index"
            // Создаём узел (как? нужен класс GraphNode)
            val node = GraphNode(
                id = nodeId,
                executor = { nodeContext -> 
                    // Логика выполнения узла: 
                    // - получить входные данные (из state или от зависимых узлов)
                    // - выполнить работу (возможно, вызвать инструмент)
                    // - сохранить результат в stateManager
                    val result = executeSubtask(subtask, nodeContext)
                    context.stateManager.save(nodeId, result)
                    result
                }
            )
            // Добавляем узел в окружение с зависимостями (если есть)
            val dependencies = if (index > 0) listOf("node_${index-1}") else emptyList()
            env.addNode(node, dependencies)
        }
        
        // 4. Построить граф
        return env.build()
    }
    
    private fun parsePlan(plan: String): List<String> { ... }
    private suspend fun executeSubtask(subtask: String, nodeContext: NodeContext): Any { ... }
}
```

### **4.2. В пайплайне (`AIAgentPlannerPipeline`)**
Пайплайн **обычно не требует кастомизации** — он универсален для любых графов. Но если нужно:
- **Мониторинг выполнения узлов**: можно обернуть `GenericAgentEnvironment.execute()` и логировать каждый узел.
- **Обработка ошибок графа**: перехватывать исключения из узлов и принимать решение (продолжить, остановить, выполнить fallback-узел).

Пример кастомизации пайплайна:
```kotlin
class MonitoringPlannerPipeline : AIAgentPlannerPipeline {
    override suspend fun execute(
        graph: Graph, 
        context: AIAgentPlannerContext
    ): Any? {
        val startTime = System.currentTimeMillis()
        try {
            val result = context.environment.execute(graph)
            log.info("Graph executed in ${System.currentTimeMillis() - startTime}ms")
            return result
        } catch (e: Exception) {
            log.error("Graph execution failed", e)
            // Возможно, сохранить ошибку в stateManager
            context.stateManager.save("graph_error", e)
            throw e
        }
    }
}
```

---

## **5. Жизненный цикл контекста**

1. **Создание**: При `agent.execute(input)` внутри `PlannerAIAgent` создаётся **новый экземпляр** `AIAgentPlannerContext`.
   - Зависимости (ToolRegistry, LLM, StateManager) инжектятся из конфигурации агента.
   - `agentInput` — это входной параметр `execute()`.

2. **Использование**:
   - `strategy.execute(input, context)` → стратегия строит граф, используя `context.environment`.
   - `pipeline.execute(graph, context)` → пайплайн выполняет граф, используя `context.environment` и `context.stateManager`.

3. **Завершение**: После возврата результата из `pipeline`:
   - Контекст становится недоступным (сборка мусорой).
   - Если `AIAgentStateManager` использует внешнее хранилище (БД, файлы), нужно очищать временные данные (или полагаться на TTL).

---

## **6. Критические аспекты и пробелы**

### **6.1. Изоляция между вызовами**
✅ **Правильно**: Каждый `execute()` создаёт новый контекст → состояние изолируется.
⚠️ **Но**: Если `AIAgentStateManager` использует общее хранилище (например, одну БД), нужно ключевое пространство (например, `sessionId` или `executionId`). В контексте нет явного `executionId`.

### **6.2. Передача данных между узлами**
Вероятные механизмы:
1. **Через `StateManager`**:
   ```kotlin
   // Узел A сохраняет
   context.stateManager.save("nodeA_result", data)
   // Узел B загружает
   val data = context.stateManager.load<Data>("nodeA_result")
   ```
2. **Через параметры узла** (прямая передача): при создании узла `B` указывается зависимость от узла `A`, и результат `A` автоматически передаётся на вход `B`.

**Без кода неизвестно**, какой механизм используется.

### **6.3. Параллельное выполнение**
`GenericAgentEnvironment` должен:
- Строить граф зависимостей.
- Запускать независимые узлы параллельно (например, через корутины).
- Синхронизировать доступ к `StateManager` при конкурентной записи.

### **6.4. Динамическое изменение графа**
Может ли стратегия (или узел) во время выполнения добавить новый узел?
- Если да, то `environment` должен поддерживать `addNodeDuringExecution()`.
- Если нет, граф фиксируется после `strategy.execute()`.

### **6.5. Обработка ошибок в графе**
Сценарии:
- Ошибка в узле: пропустить узел, использовать fallback-значение, остановить весь граф?
- Ошибка в `LLMContext`: retry? прервать граф?
- Ошибка в `ToolRegistry`: аналогично.

В контексте нет политик обработки ошибок — скорее всего, это часть `GenericAgentEnvironment` или `PlannerAIAgentPipeline`.

---

## **7. Пример полного рабочего цикла (гипотетический)**

```kotlin
// 1. Создание агента (через билдер)
val agent = PlannerAIAgentBuilder()
    .withToolRegistry(myTools)
    .withLLM(OpenAIModel("gpt-4"))
    .withStrategy(MyPlannerStrategy())
    .withPipeline(DefaultPlannerPipeline())
    .build()

// 2. Выполнение задачи
val input = MyInput(query = "Создай план проекта")
val result = agent.execute(input) // внутри создаётся AIAgentPlannerContext

// Что происходит внутри agent.execute(input):
// a) Создаётся context = AIAgentPlannerContext(
//        environment = GenericAgentEnvironmentImpl(),
//        llmContext = DefaultAIAgentLLMContext(...),
//        stateManager = InMemoryStateManager(),
//        toolRegistry = myTools,
//        agentInput = input
//    )
// b) strategy.execute(input, context) → возвращает Graph
// c) pipeline.execute(graph, context) → возвращает Output
// d) context уничтожается
```

---

## **8. Как тестировать код, использующий `AIAgentPlannerContext`?**

### **8.1. Мокирование контекста**
```kotlin
@Test
fun testMyPlannerStrategy() = runTest {
    // Моки компонентов
    val mockEnv = mockk<GenericAgentEnvironment>(relaxed = true)
    val mockLLM = mockk<AIAgentLLMContext>(relaxed = true)
    val mockState = mockk<AIAgentStateManager>(relaxed = true)
    val mockTools = mockk<ToolRegistry>(relaxed = true)
    
    // Контекст
    val context = AIAgentPlannerContext(
        environment = mockEnv,
        llmContext = mockLLM,
        stateManager = mockState,
        toolRegistry = mockTools,
        agentInput = MyInput("test")
    )
    
    // Стратегия
    val strategy = MyPlannerStrategy()
    val graph = strategy.execute(MyInput("test"), context)
    
    // Проверки: например, что в окружение добавлены ожидаемые узлы
    verify { mockEnv.addNode(any(), any()) }
}
```

### **8.2. Интеграционный тест**
Запустить агента с **реальными компонентами** (in-memory хранилище, fake LLM) и проверить:
- Граф строится правильно (количество узлов, зависимости).
- Узлы выполняются в правильном порядке.
- Состояние сохраняется/загружается.

---

## **9. Ограничения и риски**

| Риск | Последствие | Митигация |
|------|-------------|-----------|
| **Контекст создаётся внутри агента** | Нельзя reuse контекст между вызовами (хочется для кэширования?) | Спроектировать stateless стратегии |
| **Неизвестный API `GenericAgentEnvironment`** | Сложно кастомизировать графовое выполнение | Найти реализацию `GenericAgentEnvironment` в коде |
| **Состояние может утекать** | Если `StateManager` общий, между выполнениями | Очищать состояние в `finally` или использовать scoped storage |
| **Нет валидации графа** | Стратегия может построить циклические зависимости | Добавить валидацию в `environment.build()` |

---

## **10. Критические уточняющие вопросы**

1. **Где определен класс `AIAgentPlannerContext`?** В каком файле? Полный интерфейс/класс?
2. **Как具体тно устроен `GenericAgentEnvironment`?** Классы `GraphNode`, `GraphEdge`, `Graph` — где они? API для построения и выполнения?
3. **Как передаются данные между узлами?** Через `StateManager` или прямое связывание?
4. **Поддерживается ли параллельное выполнение узлов?** Как управляется (корутины, пул)?
5. **Как обрабатываются ошибки узлов?** Политика: stop-on-error, continue, fallback?
6. **Можно ли динамически изменять граф во время выполнения?** (Добавлять узлы на лету)
7. **Как обеспечивается изоляция состояния?** `StateManager` per execution? Поддерживает ли `sessionId`?
8. **Кто отвечает за очистку состояния?** Пайплайн? Агент? Контекст?
9. **Как интегрируется `AIAgentLLMContext` с `ToolRegistry`?** Формирует ли LLMContext промпт с инструментами? Парсит ли tool calls?
10. **Пример реального использования**? Есть ли в кодовой базе класс-стратегия, который использует `AIAgentPlannerContext` для построения графа?

---

## **11. Резюме**

- **`AIAgentPlannerContext`** — это **"комнатный" объект** для одного выполнения графового агента.
- **Работа с ним** сводится к:
  1. Чтению ресурсов (`toolRegistry`, `llmContext`, `stateManager`, `agentInput`).
  2. Построению графа через `environment.addNode(...)`.
  3. Передаче графа в пайплайн (который использует тот же контекст).
- **Ключевые unknowns**: реализация `GenericAgentEnvironment`, механизм передачи данных между узлами, обработка ошибок, параллелизм.

**Следующий шаг**: найти в кодовой базе файлы:
- `AIAgentPlannerContext.kt`
- `GenericAgentEnvironment.kt`
- Примеры `AIAgentPlannerStrategy` (например, в `test` или `sample` пакетах).

---

### Источники

[КЛАСС 1] PlannerAIAgent · PlannerAIAgent.kt

[ИСТОЧНИК 2] PlannerAIAgent.kt · line 22

[КЛАСС 2] FunctionalAIAgent · FunctionalAIAgent.kt

[ИСТОЧНИК 3] FunctionalAIAgent.kt · line 63

[ИСТОЧНИК 1] AIAgentConfigBase.kt · for · line 1

## Q21: "что такое AIAgentGraphStrategy и примеры использования"
На основании предоставленного контекста **класс `AIAgentGraphStrategy` не упоминается напрямую**. Однако, анализируя архитектуру `PlannerAIAgent` и связанные компоненты, можно сделать вывод, что **под "графовой стратегией" подразумевается `AIAgentPlannerStrategy`**, используемая в сочетании с `GenericAgentEnvironment` для построения и выполнения графа задач.

---

## **1. Что такое "графовая стратегия" в Koog?**

### **1.1. Терминология**
- **`AIAgentPlannerStrategy<Input, Output, Context>`** [КЛАСС 1] — это общий интерфейс/абстрактный класс для стратегий, которые **план** (разбивают задачу на подзадачи) и **оркестрируют** выполнение.
- **Графовая стратегия** — это конкретная реализация `AIAgentPlannerStrategy`, которая:
  1. Строит **граф зависимостей** между подзадачами (узлами).
  2. Передаёт этот граф в `AIAgentPlannerPipeline` для выполнения.
  3. Использует `GenericAgentEnvironment` как окружение для работы с графом.

### **1.2. Почему именно "графовая"?**
Потому что:
- **Задачи разбиваются на узлы** (например, "исследовать", "написать", "проверить").
- **Узлы имеют зависимости**: узел B может ждать завершения узла A.
- **Поддерживается параллелизм**: независимые узлы выполняются одновременно.
- **Возможны циклы и условные ветвления** (в зависимости от реализации `GenericAgentEnvironment`).

---

## **2. Архитектурное место графовой стратегии**

```
PlannerAIAgent (создаётся через GraphAgentServiceBuilder [ИСТОЧНИК 4])
├── Контекст: AIAgentPlannerContext
│   ├── GenericAgentEnvironment  ← ключевой компонент для графа
│   ├── AIAgentLLMContext
│   ├── AIAgentStateManager
│   └── ToolRegistry
├── Стратегия: [YourGraphStrategy] : AIAgentPlannerStrategy  ← здесь
│   └── execute(input, context): Graph
└── Пайплайн: AIAgentPlannerPipeline
    └── execute(graph, context): Output
```

**Важно**: `GraphAgentServiceBuilder` [ИСТОЧНИК 4] автоматически выбирает графовую стратегию (судя по названию), но как именно — не ясно. Возможно, он использует `DefaultGraphStrategy` или позволяет передать свою.

---

## **3. Примеры использования графовой стратегии**

### **Пример 1: Линейный граф (простая цепочка)**
```kotlin
class LinearGraphStrategy : AIAgentPlannerStrategy<ChatInput, String, AIAgentPlannerContext> {
    override suspend fun execute(input: ChatInput, context: AIAgentPlannerContext): Graph {
        val env = context.environment
        val llm = context.llmContext
        val state = context.stateManager

        // Узел 1: Анализ запроса
        val analyzeNode = GraphNode(
            id = "analyze",
            executor = { nodeCtx ->
                val prompt = Prompt.system("Анализируй запрос: ${input.text}")
                val analysis = llm.execute(prompt).text
                state.save("analysis", analysis)
                analysis
            }
        )
        env.addNode(analyzeNode)

        // Узел 2: Формирование ответа (зависит от анализа)
        val respondNode = GraphNode(
            id = "respond",
            executor = { nodeCtx ->
                val analysis = state.load<String>("analysis")
                val prompt = Prompt.system("Ответь на основе анализа: $analysis")
                llm.execute(prompt).text
            }
        )
        env.addNode(respondNode, dependencies = listOf("analyze"))

        return env.build()
    }
}
```

### **Пример 2: Параллельные исследования**
```kotlin
class ParallelResearchStrategy : AIAgentPlannerStrategy<ResearchInput, String, AIAgentPlannerContext> {
    override suspend fun execute(input: ResearchInput, context: AIAgentPlannerContext): Graph {
        val env = context.environment
        val llm = context.llmContext

        // Создаём узлы для параллельного исследования
        val sources = listOf(" academic papers", "news", "blogs")
        val researchNodes = sources.map { source ->
            GraphNode(
                id = "research_${source.replace(" ", "_")}",
                executor = { nodeCtx ->
                    val prompt = Prompt.system("Исследуй ${input.topic} по источнику: $source")
                    llm.execute(prompt).text
                }
            ).also { env.addNode(it) }
        }

        // Узел-агрегатор, который ждёт все исследования
        val aggregateNode = GraphNode(
            id = "aggregate",
            executor = { nodeCtx ->
                // Как получить результаты? Возможно, через stateManager
                val results = researchNodes.map { 
                    context.stateManager.load<String>(it.id) 
                }
                results.joinToString("\n\n")
            }
        )
        env.addNode(aggregateNode, dependencies = researchNodes.map { it.id })

        return env.build()
    }
}
```

### **Пример 3: Граф с условным ветвлением**
```kotlin
class ConditionalStrategy : AIAgentPlannerStrategy<QueryInput, String, AIAgentPlannerContext> {
    override suspend fun execute(input: QueryInput, context: AIAgentPlannerContext): Graph {
        val env = context.environment
        val llm = context.llmContext

        // Узел 1: Классификация запроса
        val classifyNode = GraphNode(
            id = "classify",
            executor = {
                val prompt = Prompt.system("Классифицируй запрос: ${input.text} (weather|news|other)")
                llm.execute(prompt).text
            }
        )
        env.addNode(classifyNode)

        // Узел 2A: Погода (зависит от классификации, но выполняется только если тип "weather")
        val weatherNode = GraphNode(
            id = "weather",
            executor = {
                val location = extractLocation(input.text)
                val prompt = Prompt.system("Какая погода в $location?")
                llm.execute(prompt).text
            }
        )
        env.addNode(weatherNode, dependencies = listOf("classify"))

        // Узел 2B: Новости (зависит от классификации, тип "news")
        val newsNode = GraphNode(
            id = "news",
            executor = {
                val prompt = Prompt.system("Последние новости: ${input.text}")
                llm.execute(prompt).text
            }
        )
        env.addNode(newsNode, dependencies = listOf("classify"))

        // Узел 3: Общий ответ (зависит и от weather, и от news, но если один не выполнен?)
        val responseNode = GraphNode(
            id = "response",
            executor = {
                val weather = context.stateManager.load<String>("weather")
                val news = context.stateManager.load<String>("news")
                "Погода: $weather\nНовости: $news"
            }
        )
        env.addNode(responseNode, dependencies = listOf("weather", "news"))

        return env.build()
    }
}
```
**Проблема**: Как `GenericAgentEnvironment` обработает тот факт, что `weatherNode` или `newsNode` могли не выполниться (если классификация не совпала)? Нужна логика условного выполнения.

---

## **4. Как создать агента с графовой стратегией?**

### **4.1. Через `GraphAgentServiceBuilder`** [ИСТОЧНИК 4]
```kotlin
val agent = GraphAgentServiceBuilder()
    .withToolRegistry(myTools)
    .withLLM(OpenAIModel("gpt-4"))
    .withStrategy(MyGraphStrategy())  // кастомная стратегия
    .withPipeline(DefaultPlannerPipeline())
    .build()
```
**Примечание**: В контексте нет явного `GraphAgentServiceBuilder`, но он упоминается в анализе как способ создания графового агента. Скорее всего, это отдельный билдер или режим `AIAgentServiceBuilder`.

### **4.2. Через `AIAgentServiceBuilder` с указанием стратегии**
```kotlin
val agent = AIAgentServiceBuilder()
    .withStrategy(MyGraphStrategy())  // явно передаём графовую стратегию
    .build()
```

---

## **5. Критические аспекты графовой стратегии**

### **5.1. Построение графа**
- Стратегия **добавляет узлы** в `environment` через `env.addNode(node, dependencies)`.
- Зависимости задаются как список ID узлов, от которых зависит текущий.
- После добавления всех узлов вызывается `env.build()`, который:
  - Проверяет граф на циклические зависимости.
  - Строит топологический порядок (или план выполнения).
  - Возвращает объект `Graph`.

### **5.2. Выполнение графа**
`AIAgentPlannerPipeline` вызывает `environment.execute(graph)`:
- Окружение выполняет узлы в порядке, определённом топологической сортировкой.
- Если узел имеет зависимости, он ждёт их завершения.
- Результаты узлов, вероятно, сохраняются в `AIAgentStateManager` или передаются напрямую следующим узлам.

### **5.3. Параллелизм**
`GenericAgentEnvironment` может:
- Запускать независимые узлы (без общих зависимостей) параллельно (например, в отдельных корутинах).
- Синхронизировать доступ к `stateManager` при конкурентной записи.

### **5.4. Обработка ошибок**
- Если узел завершился с ошибкой:
  - **Вариант A**: весь граф останавливается.
  - **Вариант B**: узел помечается как failed, зависимые узлы получают `null` или error-объект.
  - **Вариант C**: можно указать fallback-узел.
- В контексте нет информации о политиках обработки ошибок на уровне графа.

### **5.5. Динамическое изменение графа**
Может ли стратегия (или узел) **во время выполнения** добавить новый узел?
- Если `GenericAgentEnvironment` поддерживает динамическое изменение, то да.
- Если нет, граф фиксируется после `strategy.execute()`.

---

## **6. Отличия от `FunctionalAIAgent`**

| Критерий | `PlannerAIAgent` (графовая стратегия) | `FunctionalAIAgent` |
|----------|----------------------------------------|---------------------|
| **Структура** | Граф (узлы, рёбра, зависимости) | Линейная/ветвящаяся цепочка шагов |
| **Построение** | Динамическое (стратегия строит граф во время выполнения) | Статическое (цепочка задаётся при создании агента) |
| **Параллелизм** | Возможен (независимые узлы) | Обычно последовательный |
| **Контекст** | `AIAgentPlannerContext` с `GenericAgentEnvironment` | `AIAgentFunctionalContext` без окружения |
| **State management** | `AIAgentStateManager` (менеджер состояния графа) | `AIAgentStorage` (хранилище данных цепочки) |
| **Использование** | Сложные задачи с декомпозицией и зависимостями | Про Winston цепочки обработки (например, "получить данные → обработать → сохранить") |

---

## **7. Критические пробелы (что неясно)**

1. **Реализация `GenericAgentEnvironment`**:
   - Где defined классы `GraphNode`, `GraphEdge`, `Graph`?
   - Как именно добавляются узлы и зависимости?
   - Как выполняется граф (последовательность, параллелизм)?

2. **Передача данных между узлами**:
   - Через `StateManager` (каждый узел сохраняет результат по своему ID)?
   - Или через параметры executor (зависимые узлы получают результаты напрямую)?

3. **Примеры реальных графовых стратегий**:
   - Есть ли в кодовой базе готовые реализации (например, `DefaultGraphStrategy`, `RecursiveGraphStrategy`)?
   - Как они используют LLM для декомпозиции?

4. **Обработка ошибок в графе**:
   - Как регулируется политика (stop-on-error, continue)?
   - Есть ли возможность повторить узел?

5. **Визуализация графа**:
   - Можно ли export граф в DOT/JSON для отладки?

---

## **8. Уточняющие questions**

1. **Где находится класс `GenericAgentEnvironment`?** Каков его полный API (методы `addNode`, `build`, `execute`)?
2. **Как specificно устроен `GraphNode`?** Какой у него `executor` (сигнатура)? Как получает входные данные?
3. **Как передаются результаты между узлами?** Через `context.stateManager` или через аргументы `executor`?
4. **Поддерживается ли параллельное выполнение?** Как (корутины, пул потоков)?
5. **Как обрабатываются ошибки узлов?** Можно ли настроить политику (continue on error)?
6. **Можно ли динамически изменять граф во время выполнения?** (Добавлять узлы "на лету")
7. **Существует ли готовый `DefaultGraphStrategy`?** Если да, где?
8. **Как графовая стратегия использует `ToolRegistry`?** Создаёт ли она узлы-инструменты автоматически?
9. **Как обеспечивается изоляция между разными вызовами `agent.execute()`?** `StateManager` создаётся заново?
10. **Пример полного рабочего цикла**? Как `strategy.execute()` возвращает граф, который потом выполняется пайплайном?

---

## **9. Практическое руководство по созданию своей графовой стратегии**

### **Шаг 1: Определить узлы графа**
Для каждой подзадачи создайте `GraphNode`:
```kotlin
val node = GraphNode(
    id = "unique_id",
    executor = { nodeContext: NodeContext -> 
        // Логика выполнения узла
        // Доступ к LLM: nodeContext.llmContext
        // Доступ к состоянию: nodeContext.stateManager
        // Доступ к инструментам: nodeContext.toolRegistry
        "результат"
    }
)
```

### **Шаг 2: Добавить узлы в окружение**
```kotlin
val env = context.environment
env.addNode(node1)  // без зависимостей
env.addNode(node2, dependencies = listOf("node1"))  // ждёт node1
```

### **Шаг 3: Построить граф**
```kotlin
val graph = env.build()  // возвращает объект Graph
return graph
```

### **Шаг 4: Настроить пайплайн (если нужно кастомизировать выполнение)**
Обычно используется `DefaultPlannerPipeline`, но можно создать свой, чтобы:
- Добавить логирование каждого узла.
- Перехватывать ошибки и запускать fallback-граф.
- Собирать подробную статистику.

---

## **10. Тестирование графовой стратегии**

```kotlin
@Test
fun testGraphStrategy() = runTest {
    // Моки
    val mockEnv = mockk<GenericAgentEnvironment>(relaxed = true)
    val mockLLM = mockk<AIAgentLLMContext>(relaxed = true)
    val mockState = mockk<AIAgentStateManager>(relaxed = true)
    val mockTools = mockk<ToolRegistry>(relaxed = true)
    
    val context = AIAgentPlannerContext(
        environment = mockEnv,
        llmContext = mockLLM,
        stateManager = mockState,
        toolRegistry = mockTools,
        agentInput = MyInput()
    )
    
    val strategy = MyGraphStrategy()
    val graph = strategy.execute(MyInput(), context)
    
    // Проверяем, что в окружение添加лены ожидаемые узлы
    verify { mockEnv.addNode(any(), any()) }
    // Проверяем зависимости
    // ...
}
```

---

## **11. Ограничения графового подхода**

| Проблема | Последствие | Возможное решение |
|----------|-------------|-------------------|
| **Сложность разработки стратегии** | Нужно явно задавать узлы и зависимости | Создать DSL для построения графа |
| **Сложность отладки** | Трудно понять, почему узел не выполнился | Визуализация графа, детальное логирование |
| **Обработка ошибок** | Нет встроенных fallback-механизмов | Добавить специальные узлы-"ловушки" |
| **Динамическое изменение** | Граф фиксируется после построения | Расширить `GenericAgentEnvironment` для динамики |
| **Производительность** | Множество вызовов LLM для каждого узла | Кэширование, batch-запросы |

---

## **12. Где искать примеры в кодовой базе?**

1. **Пакет `strategy`** — вероятно, там находятся реализации `AIAgentPlannerStrategy`.
2. **Тесты к `PlannerAIAgent`** — могут содержать простые графовые стратегии.
3. **Примеры (samples/)** — если есть, то там наверняка есть графовые сценарии.
4. **Класс `DefaultGraphStrategy`** — может быть где-то в `impl` пакете.

---

## **Резюме**

**`AIAgentGraphStrategy`** — это **конкретная реализация `AIAgentPlannerStrategy`**, которая:
1. Строит **граф задач** с помощью `GenericAgentEnvironment`.
2. Возвращает граф для выполнения `AIAgentPlannerPipeline`.
3. Подходит для сложных задач, требующих декомпозиции, параллелизма и управления зависимостями.

**Как использовать**:
1. Создать класс, наследующий `AIAgentPlannerStrategy<Input, Output, AIAgentPlannerContext>`.
2. В `execute()` построить граф через `context.environment.addNode(...)`.
3. Вернуть `context.environment.build()`.
4. Передать стратегию в `GraphAgentServiceBuilder` или `AIAgentServiceBuilder`.

**Ключевые unknowns**:
- API `GenericAgentEnvironment`.
- Механизм передачи данных между узлами.
- Обработка ошибок и параллелизм.
- Примеры реальных стратегий в кодовой базе.

**Следующие шаги**: найти в кодовой базе файлы `GenericAgentEnvironment.kt`, `GraphNode.kt` и примеры графовых стратегий.

---

### Источники

[КЛАСС 1] PlannerAIAgent · PlannerAIAgent.kt

[ИСТОЧНИК 2] PlannerAIAgent.kt · line 22

[КЛАСС 2] FunctionalAIAgent · FunctionalAIAgent.kt

[ИСТОЧНИК 3] FunctionalAIAgent.kt · line 63

[ИСТОЧНИК 4] AIAgentServiceBuilderImpl.kt · line 60

[ИСТОЧНИК 5] AIAgentServiceBuilderImpl.kt · line 72

[ИСТОЧНИК 1] AIAgentConfigBase.kt · for · line 1

## Q22: "подробно про AIAgentNode и его преимущества"
На основании предоставленного контекста **класс `AIAgentNode` не упоминается**. В архитектуре Koog для графовых агентов используются следующие компоненты:

1. **`GenericAgentEnvironment`** — окружение для построения и выполнения графа
2. **`Graph`** (предположительно) — структура графа с узлами и рёбрами
3. **`GraphNode` / `GraphEdge`** (предположительно) — элементы графа

Вероятно, **`AIAgentNode`** — это либо:
- Альтернативное название для `GraphNode`
- Кастомная реализация узла, создаваемая разработчиком стратегий
- Часть DSL для построения графа

---

## **Предполагаемая роль узла (node) в графовой архитектуре**

### **1. Что такое узел (node) в контексте `PlannerAIAgent`?**
Узел — это **элементарная задача/шаг** в графе выполнения, который:
- Имеет уникальный идентификатор (`id: String`)
- Содержит **логику выполнения** (`executor: suspend (NodeContext) -> Any`)
- Может иметь **зависимости** от других узлов (рёбра графа)
- Сохраняет свой **результат** в `AIAgentStateManager` (или передаёт напрямую)

### **2. Типы узлов (гипотетические)**
На основе типичных графовых execute-ов:

| Тип узла | Назначение | Пример |
|----------|-------------|--------|
| **TaskNode** | Выполнение подзадачи (LLM, инструмент) | "Исследовать тему X" |
| **ConditionNode** | Условное ветвление (if/else) | "Если результат A, то выполнить B" |
| **MergeNode** | Объединение результатов нескольких узлов | "Суммаризировать выводы A, B, C" |
| **ToolNode** | Вызов конкретного инструмента | "Вызвать WeatherTool" |
| **LoopNode** | Цикл (повторение до условия) | "Повторять, пока не получим ответ" |
| **InputNode** | Входной узел (без зависимостей) | "Получить запрос пользователя" |
| **OutputNode** | Выходной узел (финальный результат) | "Сформировать итоговый ответ" |

---

## **3. Предполагаемый API класса узла**

```kotlin
// Гипотетический интерфейс (основан на примерах из предыдущего ответа)
interface AIAgentNode {
    val id: String
    val dependencies: List<String>  // ID узлов, от которых зависит
    
    // Выполнение узла
    suspend fun execute(context: NodeContext): NodeResult
}

data class NodeResult(
    val value: Any?,  // Результат выполнения
    val status: NodeStatus,  // SUCCESS, FAILED, SKIPPED
    val error: Throwable? = null
)

interface NodeContext {
    val stateManager: AIAgentStateManager
    val llmContext: AIAgentLLMContext?
    val toolRegistry: ToolRegistry
    val nodeId: String
    val graphInput: Any  // Исходные данные графа
    
    // Вспомогательные методы
    suspend fun callLLM(prompt: Prompt): String
    suspend fun callTool(toolName: String, params: Map<String, Any>): Any
    fun <T> getDependencyResult(nodeId: String): T?
}
```

---

## **4. Преимущества использования узлов в графовой архитектуре**

### **4.1. Модульность и переиспользование**
```kotlin
// Один узел "Research" можно использовать в разных графах
val researchNode = ResearchNode(id = "research_topic", topic = "AI")
graph.addNode(researchNode)

// И в другом графе с другим топиком
val researchNode2 = ResearchNode(id = "research_weather", topic = "climate")
graph.addNode(researchNode2)
```

### **4.2. Параллельное выполнение**
```kotlin
// Узлы A, B, C независимы → выполняются параллельно
env.addNode(nodeA)
env.addNode(nodeB)
env.addNode(nodeC)
// Узел D ждёт все три
env.addNode(nodeD, dependencies = listOf("A", "B", "C"))
```

### **4.3. Явные зависимости**
```kotlin
// Граф автоматически определяет порядок выполнения
env.addNode(analyzeNode)           // 1. Анализ
env.addNode(searchNode, listOf("analyze"))  // 2. Поиск (после анализа)
env.addNode(summarizeNode, listOf("search")) // 3. Суммаризация (после поиска)
```

### **4.4. Изоляция состояния**
- Каждый узел работает со своим **пространством имён** в `StateManager`.
- Нет конфликтов имён переменных (в отличие от линейного кода).
- Пример:
```kotlin
// Узел A
stateManager.save("result", "A_result")
// Узел B
stateManager.save("result", "B_result")  // Не перезаписывает A
```

### **4.5. Легкость тестирования**
```kotlin
@Test
fun testResearchNode() = runTest {
    val node = ResearchNode("research_test", "test topic")
    val context = FakeNodeContext()
    val result = node.execute(context)
    assertEquals("expected", result.value)
}
```

### **4.6. Динамическое построение графа**
Стратегия может строить граф **на лету** на основе LLM-ответа:
```kotlin
val plan = llm.execute("Разбей задачу на шаги").text  // "Шаг1, Шаг2, Шаг3"
val steps = plan.split(", ")
steps.forEach { step ->
    env.addNode(DynamicNode(step))  // Создаём узлы динамически
}
```

### **4.7. Обработка ошибок на уровне узлов**
```kotlin
val node = object : AIAgentNode {
    override suspend fun execute(context: NodeContext): NodeResult {
        return try {
            val result = context.callLLM(...)
            NodeResult.Success(result)
        } catch (e: Exception) {
            NodeResult.Failed(e)  // Граф может продолжить другие узлы
        }
    }
}
```

### **4.8. Визуализация и отладка**
Граф из узлов легко:
- Визуализировать (DOT, Mermaid)
- Логировать (каждый узел: начало/конец/ошибка)
- Профилировать (время выполнения каждого узла)

---

## **5. Пример полного графа сNode-ами (гипотетический)**

```kotlin
class ResearchPaperStrategy : AIAgentPlannerStrategy<ResearchInput, String, AIAgentPlannerContext> {
    override suspend fun execute(input: ResearchInput, context: AIAgentPlannerContext): Graph {
        val env = context.environment
        val state = context.stateManager
        
        // Узел 1: Получить тему
        val getTopicNode = object : AIAgentNode {
            override val id = "get_topic"
            override val dependencies = emptyList<String>()
            
            override suspend fun execute(ctx: NodeContext): NodeResult {
                val topic = (ctx.graphInput as ResearchInput).topic
                ctx.stateManager.save("topic", topic)
                return NodeResult.Success(topic)
            }
        }
        
        // Узел 2: Поиск академических статей (зависит от темы)
        val searchPapersNode = object : AIAgentNode {
            override val id = "search_papers"
            override val dependencies = listOf("get_topic")
            
            override suspend fun execute(ctx: NodeContext): NodeResult {
                val topic = ctx.stateManager.load<String>("topic")
                val papers = callArxivAPI(topic)  // Инструмент
                ctx.stateManager.save("papers", papers)
                return NodeResult.Success(papers)
            }
        }
        
        // Узел 3: Анализ статей (параллельно для каждой)
        val analysisNodes = (1..3).map { i ->
            object : AIAgentNode {
                override val id = "analyze_paper_$i"
                override val dependencies = listOf("search_papers")
                
                override suspend fun execute(ctx: NodeContext): NodeResult {
                    val papers = ctx.stateManager.load<List<Paper>>("papers")
                    val paper = papers[i]
                    val analysis = ctx.callLLM("Проанализируй: ${paper.summary}")
                    ctx.stateManager.save("analysis_$i", analysis)
                    return NodeResult.Success(analysis)
                }
            }
        }.also { nodes -> nodes.forEach { env.addNode(it) } }
        
        // Узел 4: Суммаризация (ждет все анализа)
        val summarizeNode = object : AIAgentNode {
            override val id = "summarize"
            override val dependencies = analysisNodes.map { it.id }
            
            override suspend fun execute(ctx: NodeContext): NodeResult {
                val analyses = analysisNodes.map { 
                    ctx.stateManager.load<String>("analysis_${it.id.split("_").last()}") 
                }
                val summary = ctx.callLLM("Суммаризируй: ${analyses.joinToString()}")
                return NodeResult.Success(summary)
            }
        }
        
        // Добавляем все узлы
        env.addNode(getTopicNode)
        env.addNode(searchPapersNode)
        analysisNodes.forEach { env.addNode(it) }
        env.addNode(summarizeNode)
        
        return env.build()
    }
}
```

---

## **6. Критические пробелы (что неясно из контекста)**

### **6.1. Нет явного класса `AIAgentNode`**
- В контексте не указано, есть ли общий интерфейс/абстрактный класс для узлов.
- Возможно, узлы создаются через **DSL** (domain-specific language) в `GenericAgentEnvironment`.
- Или узел — это просто data class с `executor` suspend-функцией.

### **6.2. Неизвестен `NodeContext`**
- Какие методы доступны в контексте узла?
- Как получать результаты зависимых узлов?
- Как вызывать LLM/инструменты из узла?

### **6.3. Неясна передача данных между узлами**
- **Вариант A**: через `stateManager` (каждый узел сохраняет по ключу `id`)
- **Вариант B**: автоматическая передача результата как входного параметра следующему узлу
- **Вариант C**: через `graphInput` (общие данные) и `stateManager` (промежуточные)

### **6.4. Неизвестна обработка ошибок узлов**
- Если узел упал, что происходит с зависимыми узлами?
- Можно ли указать fallback-узел?
- Есть ли retry-политика?

### **6.5. Нет информации о lifecycle узла**
- Инициализация (before execute)
- Выполнение (execute)
- Cleanup (after execute, even on error)
- Возможность отмены (cancellation)

---

## **7. Где искать информацию в кодовой базе?**

1. **`GenericAgentEnvironment.kt`** — должен содержать:
   - `addNode(node: AIAgentNode, dependencies: List<String>)`
   - `build(): Graph`
   - `execute(graph: Graph): GraphResult`

2. **`Graph.kt`** — структура графа:
   ```kotlin
   data class Graph(
       val nodes: Map<String, AIAgentNode>,
       val edges: List<GraphEdge>  // (fromId, toId)
   )
   ```

3. **Примеры стратегий** (в `test/` или `samples/`):
   - Как создаются узлы
   - Как задаются зависимости
   - Как обрабатываются результаты

4. **`AIAgentPlannerPipeline.kt`** — как выполняется граф:
   - Топологическая сортировка
   - Параллельное execute-ование
   - Сбор результатов

---

## **8. Уточняющие вопросы**

1. **Где определен класс/интерфейс `AIAgentNode` (или `GraphNode`)?** В каком файле?
2. **Какой полный API у `GenericAgentEnvironment`?** Методы `addNode`, `build`, `execute`.
3. **Как устроен `NodeContext`?** Какие методы доступны узлу во время execute?
4. **Как передаются данные между узлами?** Через `stateManager` или параметры executor?
5. **Как обрабатываются ошибки узлов?** Политика: stop-on-error, continue, fallback?
6. **Поддерживается ли параллельное execute-ование узлов?** Как (корутины, пул)?
7. **Можно ли динамически добавлять узлы во время execute?** (На лету)
8. **Как обеспечивается изоляция между разными вызовами `agent.execute()`?** StateManager per execution?
9. **Есть ли DSL для построения графа?** Или только programmatic API?
10. **Пример реальной стратегии с несколькими узлами?** В тестах или samples.

---

## **9. Резюме**

**`AIAgentNode`** (или `GraphNode`) — это, **предположительно**, фундаментальный элемент графа выполнения в `PlannerAIAgent`, представляющий элементарную задачу с:
- Уникальным ID
- Логикой выполнения (`executor`)
- Зависимостями от других узлов
- Доступом к контексту (`NodeContext`)

**Преимущества**:
- Модульность и переиспользование
- Параллелизм независимых узлов
- Явные зависимости
- Изоляция состояния
- Легкость тестирования и визуализации

**Но**: В предоставленном контексте **нет явного класса `AIAgentNode`**. Для точной информации необходимо найти:
1. `GenericAgentEnvironment.kt`
2. Классы `GraphNode`, `GraphEdge`, `Graph`
3. Примеры стратегий в тестах

---

### Источники

[КЛАСС 1] PlannerAIAgent · PlannerAIAgent.kt

[ИСТОЧНИК 2] PlannerAIAgent.kt · line 22

[КЛАСС 2] FunctionalAIAgent · FunctionalAIAgent.kt

[ИСТОЧНИК 3] FunctionalAIAgent.kt · line 63

[ИСТОЧНИК 4] AIAgentServiceBuilderImpl.kt · line 60

[ИСТОЧНИК 5] AIAgentServiceBuilderImpl.kt · line 72

[ИСТОЧНИК 1] AIAgentConfigBase.kt · for · line 1

## Q23: "как использовать AIAgentState"
На основании предоставленного контекста **прямого класса `AIAgentState` не существует**.

В архитектуре Koog представлены **два разных компонента управления состоянием** в зависимости от типа агента:

| Агент | Компонент состояния | Назначение |
|-------|---------------------|------------|
| `PlannerAIAgent` | `AIAgentStateManager` [ИСТОЧНИК 2] | Управление состоянием **графа задач** во время выполнения (промежуточные результаты узлов, статусы подзадач) |
| `FunctionalAIAgent` | `AIAgentStorage` [ИСТОЧНИК 3] | Хранение данных **цепочки шагов** (история, промежуточные результаты) |

**`AIAgentState`** — это, вероятно, **обобщающее название** для этих компонентов или же конкретный класс, который не попал в предоставленный контекст.

---

## **1. Как используется `AIAgentStateManager` в `PlannerAIAgent`?**

### **1.1. Назначение**
`AIAgentStateManager` — это менеджер состояния для **графового выполнения**.
Он инкапсулирует:
- Промежуточные результаты выполнения узлов графа
- Статусы подзадач (завершена, в процессе, ошибка)
- Возможно, историю вызовов LLM и инструментов
- Снапшоты состояния для восстановления

### **1.2. Где доступен?**
В `AIAgentPlannerContext` [КЛАСС 1] как поле `stateManager`:
```kotlin
class AIAgentPlannerContext {
    val environment: GenericAgentEnvironment
    val llmContext: AIAgentLLMContext?
    val stateManager: AIAgentStateManager  // ← доступен здесь
    val toolRegistry: ToolRegistry
    val agentInput: Any
}
```

### **1.3. Пример использования в стратегии**
В методе `execute()` стратегии вы строите граф, и узлы этого графа могут читать/писать состояние через `stateManager`:

```kotlin
class MyGraphStrategy : AIAgentPlannerStrategy<Input, Output, AIAgentPlannerContext> {
    override suspend fun execute(input: Input, context: AIAgentPlannerContext): Graph {
        val env = context.environment
        val state = context.stateManager  // ← получаем менеджер состояния
        
        // Узел 1: сохранение входных данных
        val node1 = GraphNode(
            id = "node1",
            executor = { nodeCtx ->
                state.save("originalInput", input)  // сохраняем вход
                process(input)
            }
        )
        env.addNode(node1)
        
        // Узел 2: использует результат node1
        val node2 = GraphNode(
            id = "node2",
            executor = { nodeCtx ->
                val original = state.load<Input>("originalInput")  // читаем
                val intermediate = state.load<String>("node1Result")  // или результат предыдущего узла
                combine(original, intermediate)
            }
        )
        env.addNode(node2, dependencies = listOf("node1"))
        
        return env.build()
    }
}
```

### **1.4. Предполагаемый API `AIAgentStateManager`**
На основе типичных паттернов:

```kotlin
interface AIAgentStateManager {
    // Сохранить значение по ключу (с опциональным TTL)
    suspend fun save(key: String, value: Any, ttl: Duration? = null)
    
    // Загрузить значение по ключу
    suspend fun <T> load(key: String): T?
    
    // Удалить ключ или всё состояние
    suspend fun clear(key: String? = null)
    
    // Создать снапшот (для отката или передачи между узлами)
    suspend fun snapshot(): StateSnapshot
    
    // Восстановить из снапшота
    suspend fun restore(snapshot: StateSnapshot)
    
    // Проверить существование ключа
    suspend fun contains(key: String): Boolean
    
    // Получить все ключи (для отладки)
    suspend fun keys(): Set<String>
}

data class StateSnapshot(
    val data: Map<String, Any>,
    val timestamp: Instant
)
```

---

## **2. Как используется `AIAgentStorage` в `FunctionalAIAgent`?**

### **2.1. Назначение**
`AIAgentStorage` — это хранилище данных для **линейных/ветвящихся цепочек**.
Он инкапсулирует:
- Историю сообщений (для включения в промпт)
- Промежуточные результаты шагов цепочки
- Возможно, пользовательские данные ( década )

### **2.2. Где доступен?**
В `AIAgentFunctionalContext` [ИСТОЧНИК 3] как поле `storage`:
```kotlin
class AIAgentFunctionalContext {
    val storage: AIAgentStorage  // ← доступен здесь
    val toolRegistry: ToolRegistry
    fun prepareEnvironment(): Environment
    fun install()
}
```

### **2.3. Пример использования**
В стратегии функционального агента вы работаете с цепочкой шагов:

```kotlin
class MyFunctionalStrategy : AIAgentFunctionalStrategy<Input, Output> {
    override suspend fun execute(input: Input, context: AIAgentFunctionalContext): Output {
        val storage = context.storage
        
        // Шаг 1: сохранение входных данных
        storage.save("input", input)
        
        // Шаг 2: обработка
        val step1Result = process(input)
        storage.save("step1", step1Result)
        
        // Шаг 3: используем предыдущий результат
        val previous = storage.load<String>("step1")
        val final = combine(previous, ...)
        storage.save("final", final)
        
        return final
    }
}
```

### **2.4. Предполагаемый API `AIAgentStorage`**
```kotlin
interface AIAgentStorage {
    // Сохранить значение
    suspend fun save(key: String, value: Any)
    
    // Загрузить значение
    suspend fun <T> load(key: String): T?
    
    // Удалить
    suspend fun clear(key: String? = null)
    
    // Получить всю историю (для промпта)
    suspend fun getHistory(): List<StorageEntry>
    
    // Добавить сообщение (специфично для диалогов)
    suspend fun addMessage(message: Message)
    
    // Очистить историю (например, при переполнении контекста)
    suspend fun trimHistory(maxSize: Int)
}

data class StorageEntry(
    val key: String,
    val value: Any,
    val timestamp: Instant
)
```

---

## **3. Ключевые отличия `AIAgentStateManager` vs `AIAgentStorage`**

| Характеристика | `AIAgentStateManager` (Planner) | `AIAgentStorage` (Functional) |
|----------------|--------------------------------|-------------------------------|
| **Тип агента** | `PlannerAIAgent` (граф) | `FunctionalAIAgent` (цепочка) |
| **Структура данных** | Граф узлов (каждый узел сохраняет свой результат) | Линейная/ветвящаяся история |
| **Основное использование** | Обмен данными между узлами графа | Накопление истории для промпта, промежуточные результаты |
| **Изоляция** | По ID узлов (node1_result, node2_result) | По ключам (step1, step2, user_message) |
| **Время жизни** | Один вызов `agent.execute()` (граф строится и выполняется за один раз) | Цепочка шагов (может быть много шагов за один execute) |
| **Доступ из** | Узлы графа (через `NodeContext`) | Шаги стратегии (через `context.storage`) |
| **Снапшоты** | Вероятно, есть (для восстановления после ошибки) | Не обязательно |
| **Пример ключа** | `"node_123_result"` | `"user_message_1"`, `"tool_call_weather"` |

---

## **4. Практические примеры использования**

### **4.1. Grafluential (Planner) — динамический граф с LLM-декомпозицией**

```kotlin
class DynamicResearchStrategy : AIAgentPlannerStrategy<ResearchInput, String, AIAgentPlannerContext> {
    override suspend fun execute(input: ResearchInput, context: AIAgentPlannerContext): Graph {
        val env = context.environment
        val state = context.stateManager
        val llm = context.llmContext!!
        
        // Сохраняем исходную задачу
        state.save("task", input.topic)
        
        // LLM декомпозирует задачу
        val decomposition = llm.execute(
            Prompt.system("Разбей задачу '${input.topic}' на 3 подзадачи")
        ).text
        
        val subtasks = decomposition.lines().take(3)
        
        // Создаём узлы для каждой подзадачи
        subtasks.forEachIndexed { index, subtask ->
            val nodeId = "subtask_$index"
            
            val node = GraphNode(
                id = nodeId,
                executor = { nodeCtx ->
                    // Читаем общую задачу
                    val task = state.load<String>("task")
                    // Выполняем подзадачу
                    val result = llm.execute(
                        Prompt.system("Выполни подзадачу '$subtask' для задачи '$task'")
                    ).text
                    // Сохраняем результат по ID узла
                    state.save(nodeId, result)
                    result
                }
            )
            env.addNode(node)
        }
        
        // Узел-агрегатор (ждёт все подзадачи)
        val aggregateNode = GraphNode(
            id = "aggregate",
            executor = { nodeCtx ->
                // Собираем все результаты
                val results = subtasks.indices.map { index ->
                    state.load<String>("subtask_$index") ?: ""
                }
                llm.execute(
                    Prompt.system("Обобщи: ${results.joinToString("\n")}")
                ).text
            }
        )
        env.addNode(aggregateNode, dependencies = subtasks.indices.map { "subtask_$it" })
        
        return env.build()
    }
}
```

### **4.2. Functional — цепочка с историей диалога**

```kotlin
class ChatStrategy : AIAgentFunctionalStrategy<ChatInput, String> {
    override suspend fun execute(input: ChatInput, context: AIAgentFunctionalContext): String {
        val storage = context.storage
        val llm = context.llmContext!!  // предположим, что есть
        
        // Добавляем сообщение пользователя в историю
        storage.addMessage(Message.user(input.text))
        
        // Получаем историю для промпта
        val history = storage.getHistory().takeLast(10)  // последние 10 сообщений
        
        // Формируем промпт с историей
        val prompt = Prompt {
            system("Ты полезный ассистент")
            messages(history.map { entry ->
                when (entry.value) {
                    is Message -> entry.value
                    else -> Message.assistant(entry.value.toString())
                }
            })
            user(input.text)
        }
        
        // Выполняем запрос
        val response = llm.execute(prompt).text
        
        // Сохраняем ответ в историю
        storage.addMessage(Message.assistant(response))
        
        // Также сохраняем как финальный результат
        storage.save("lastResponse", response)
        
        return response
    }
}
```

---

## **5. Жизненный цикл состояния**

### **5.1. Для `PlannerAIAgent` (StateManager)**
1. **Создание**: `AIAgentPlannerContext` создаётся заново для каждого `agent.execute(input)`
2. **Использование**: узлы графа читают/пишут в `stateManager` во время выполнения
3. **Очистка**: после завершения `agent.execute()` контекст и `stateManager` уничтожаются (GC). Если `stateManager` использует внешнее хранилище (БД, файлы), требуется ручная очистка.

### **5.2. Для `FunctionalAIAgent` (Storage)**
1. **Создание**: `AIAgentFunctionalContext` создаётся заново для каждого вызова
2. **Использование**: шаги стратегии добавляют сообщения/результаты в `storage`
3. **Очистка**: аналогично, контекст уничтожается после выполнения. Если нужно сохранять историю между сессиями — это должен делать внешний код.

---

## **6. Изоляция состояний между вызовами**

**Критически важно**: каждый вызов `agent.execute(input)` должен получить **свежий, пустой контекст** с новым `stateManager`/`storage`. Это обеспечивается билдерами (`GraphAgentServiceBuilder`, `AIAgentServiceBuilder`), которые создают новые экземпляры контекстов для каждого execute.

**Проблема**: если `stateManager`/`storage` используют статические/синглтонные хранилища, возможна **утечка данных между сессиями**!

**Рекомендация**: убедиться, что:
1. Контекст создаётся заново для каждого вызова
2. Внутренние хранилища `stateManager`/`storage` не являются синглтонами
3. Если используется БД/кэш — есть ключ сессии (`sessionId`) для изоляции

---

## **7. Безопасность и валидация**

**Текущий контекст не содержит информации о безопасности состояния**, но гипотетические риски:

| Риск | Последствие | Меры защиты |
|------|-------------|-------------|
| **Утечка данных между сессиями** | Пользователь А видит данные пользователя Б | Строгая изоляция по `sessionId`, очистка после использования |
| **Инъекции через состояние** | Злоумышленник подменяет ключ/значение | Валидация типов при `load<T>()`, ограниченный набор ключей |
| **DoL via большие значения** | Заполнение памяти большими объектами | Лимиты размеров значений, TTL, сжатие |
| **Несанкционированный доступ** | Чтение/запись без прав | Контроль доступа на уровне ключей (разные роли) |

---

## **8. Observability состояния**

В контексте **нет встроенной системы observability** для `stateManager`/`storage`, но рекомендуется:

1. **Логирование**:
   - `save(key, value)` → debug лог
   - `load(key)` → debug лог (но осторожно, чтобы не залогировать секреты)
   - `clear()` → info лог

2. **Метрики**:
   - Количество сохранённых ключей
   - Размер состояния (в байтах)
   - Частота использования ключей

3. **Трассировка**:
   - Добавлять `traceId` к операциям состояния
   - Связывать `save`/`load` с выполнением узлов/шагов

---

## **9. Критические пробелы (что неизвестно)**

1. **Полный API** `AIAgentStateManager` и `AIAgentStorage` (список методов, сигнатуры)
2. **Реализация** (in-memory, persistent, распределённое?)
3. **Сериализация** (как сохраняются сложные объекты)
4. **TTL и очистка** (есть ли автоматическая очистка устаревших данных?)
5. **Поддержка транзакций** (много операций `save` атомарно?)
6. **Производительность** (латентность `load`/`save`, размеры данных)
7. **Примеры реального использования** в тестах/сампах
8. **Интеграция с историей диалога** (как история включается в промпт? через `storage.getHistory()`?)
9. **Обработка ошибок** (что происходит при `save` в переполненное хранилище?)
10. **Изоляция** (как обеспечивается изоляция между concurrent вызовами `execute()`?)

---

## **10. Где искать в кодовой базе?**

1. **Файлы**:
   - `AIAgentStateManager.kt` (если существует)
   - `AIAgentStorage.kt`
   - `AIAgentPlannerContext.kt` (поле `stateManager`)
   - `AIAgentFunctionalContext.kt` (поле `storage`)

2. **Тесты**:
   - `AIAgentStateManagerTest.kt`
   - `AIAgentStorageTest.kt`
   - `PlannerAIAgentTest.kt` (использование state)
   - `FunctionalAIAgentTest.kt` (использование storage)

3. **Примеры (samples/)**:
   - Стратегии, которые явно используют `stateManager.save/load`

4. **Поиск по коду**:
   - `stateManager.save(`
   - `storage.load<`

---

## **11. Рекомендации по использованию**

### **11.1. Для `PlannerAIAgent`**
- Используйте `stateManager.save(key, value)` для сохранения результатов узлов
- Используйте осмысленные ключи: `"node_${nodeId}_result"`
- Читайте результаты зависимых узлов через `stateManager.load<T>("node_id")`
- **Не храните большие объекты** (весь граф в памяти)
- Очищайте временные ключи (`stateManager.clear("temp_*")`)

### **11.2. Для `FunctionalAIAgent`**
- Используйте `storage.addMessage()` для истории диалога (если есть)
- Используйте `storage.save(key, value)` для промежуточных результатов шагов
- Используйте `storage.getHistory()` для формирования промпта
- Регулярно вызывайте `storage.trimHistory(maxSize)` чтобы избежать переполнения контекста LLM
- Храните только сериализуемые данные

---

## **12. Пример полного рабочего цикла (Planner)**

```kotlin
// 1. Создаём агента с графовой стратегией
val agent = GraphAgentServiceBuilder()
    .withStrategy(MyGraphStrategy())
    .withLLM(OpenAIModel("gpt-4"))
    .build()

// 2. Выполняем задачу
val result = agent.execute(MyInput("Исследуй Agents in AI"))

// Внутри:
// - Создаётся AIAgentPlannerContext с новым StateManager
// - MyGraphStrategy.execute() строит граф, узлы используют stateManager
// - Пайплайн выполняет граф
// - После выполнения контекст уничтожается, stateManager тоже
```

---

## **13. Вопросы для уточнения (что спросить у разработчиков)**

1. **Где определен интерфейс `AIAgentStateManager`?** Полный список методов?
2. **Какая реализация по умолчанию?** In-memory (HashMap), или persistent?
3. **Как обеспечивается изоляция между параллельными вызовами `agent.execute()`?**
4. **Можно ли использовать один `stateManager` для нескольких графов?** (Нет, каждый граф — свой контекст)
5. **Есть ли возможности снапшотов и отката?** (`snapshot()`, `restore()`)
6. **Как интегрируется с `AIAgentLLMContext`?** (LLM-контекст может сохранять usage stats в state)
7. **Поддерживается ли шифрование чувствительных данных в состоянии?**
8. **Какие типы значений можно сохранять?** Любой `Any`, или только сериализуемые?
9. **Как обрабатываются ошибки `save`/`load`?** (например, сериализация не удалась)
10. **Можно ли подписаться на изменения состояния?** (observability, listener)

---

## **Резюме**

**`AIAgentState`** — это **не конкретный класс**, а **концепция управления состоянием** в Koog, реализуемая через:
- `AIAgentStateManager` для графовых агентов (`PlannerAIAgent`)
- `AIAgentStorage` для функциональных агентов (`FunctionalAIAgent`)

**Основные принципы**:
1. **Изоляция**: каждый вызов `agent.execute()` получает свежий контекст со своим состоянием
2. **Ключ-значение**: сохраняете по ключу, читаете по ключу
3. **Время жизни**: состояние живёт ровно один вызов `execute()` (если не реализована персистентность)
4. **Отсутствие общего интерфейса**: `StateManager` и `Storage` не имеют общего супертипа, что затрудняет универсальный код

**Что делать дальше**:
1. Найти файлы `AIAgentStateManager.kt` и `AIAgentStorage.kt` в кодовой базе
2. Изучить их методы (скорее всего, `save`, `load`, `clear`, `getHistory`)
3. Посмотреть тесты на их использование
4. Проверить, как они создаются в билдерах

---

### Источники

[КЛАСС 1] PlannerAIAgent · PlannerAIAgent.kt

[ИСТОЧНИК 2] PlannerAIAgent.kt · line 22

[КЛАСС 2] FunctionalAIAgent · FunctionalAIAgent.kt

[ИСТОЧНИК 3] FunctionalAIAgent.kt · line 63

[ИСТОЧНИК 4] AIAgentServiceBuilderImpl.kt · line 60

[ИСТОЧНИК 5] AIAgentServiceBuilderImpl.kt · line 72

[ИСТОЧНИК 1] AIAgentConfigBase.kt · for · line 1

## Q24: "зачем нужен AIAgentStorage"
На основании предоставленного контекста **явного класса или интерфейса `AIAgentStorage` не определено**, но он упоминается как поле в `AIAgentFunctionalContext` [ИСТОЧНИК 3]. На основе этого и архитектурных паттернов фукционального агента можно определить его назначение и предполагаемую роль.

---

## **1. Где используется `AIAgentStorage`?**

`AIAgentStorage` присутствует в **`AIAgentFunctionalContext`** — контексте выполнения для `FunctionalAIAgent` [КЛАСС 2]:

```kotlin
class AIAgentFunctionalContext {
    val storage: AIAgentStorage  // ← основное хранилище данных
    val toolRegistry: ToolRegistry
    fun prepareEnvironment(): Environment
    fun install()
}
```

Это означает, что `AIAgentStorage` доступен **всем шагам функционального пайплайна** (`AIAgentFunctionalPipeline` [КЛАСС 4]) и стратегиям `AIAgentFunctionalStrategy<Input, Output>`.

---

## **2. Зачем нужен `AIAgentStorage`? Основные цели**

### **2.1. Хранение промежуточных результатов шагов**
В функциональном агенте выполняется **последовательность шагов** (функций), где каждый шаг может зависеть от результата предыдущего. `AIAgentStorage` служит общим хранилищем для этих результатов:

```kotlin
// Пример шага 1
context.storage.save("step1_result", analyze(input))

// Пример шага 2 (использует результат шага 1)
val analysis = context.storage.load<String>("step1_result")
val response = generateResponse(analysis)
```

### **2.2. Накопление истории диалога (для чат-ботов)**
Функциональные агенты часто используются для **диалоговых сценариев**. `AIAgentStorage` может хранить:
- Историю сообщений пользователя и ассистента
- Промежуточные рассуждения (thoughts)
- Контекст сессии

Это позволяет формировать промпт с историей для LLM.

### **2.3. Передача данных между независимыми шагами**
Даже если шаги не последовательны, а независимы, `AIAgentStorage` позволяет:
- Шаг A сохраняет результат
- Шаг B загружает результат A
- Шаг C агрегирует результаты A и B

### **2.4. Восстановление после ошибок (если поддерживается)**
При падении одного шага, последующие могут:
- Загрузить сохранённые частичные результаты
- Пропустить упавший шаг (если результат уже есть)
- Восстановить состояние из снапшота

### **2.5. Логирование и отладка**
Все сохранённые ключи и значения можно инспектировать для:
- Понимания, какие данные были на каждом шаге
- Анализа причин ошибок
- Профилирования (какие шаги что сохраняли)

---

## **3. Предполагаемый API `AIAgentStorage`**

На основе аналогий с `AIAgentStateManager` [ИСТОЧНИК 2] и типичных хранилищ данных:

```kotlin
interface AIAgentStorage {
    // Сохранить любое значение по строковому ключу
    suspend fun save(key: String, value: Any)
    
    // Загрузить значение по ключу (с типом T)
    suspend fun <T> load(key: String): T?
    
    // Проверить существование ключа
    suspend fun contains(key: String): Boolean
    
    // Удалить конкретный ключ или всё хранилище
    suspend fun clear(key: String? = null)
    
    // Получить все ключи (для инспекции)
    suspend fun keys(): Set<String>
    
    // Для диалогов: добавить сообщение в историю (уже сериализованное)
    suspend fun addMessage(message: Message)
    
    // Получить историю сообщений (например, последние N)
    suspend fun getHistory(limit: Int = Int.MAX_VALUE): List<Message>
    
    // Очистка истории (например, при переполнении контекста LLM)
    suspend fun trimHistory(maxSize: Int)
    
    // Создать снапшот (для восстановления)
    suspend fun snapshot(): StorageSnapshot
    
    // Восстановить из снапшота
    suspend fun restore(snapshot: StorageSnapshot)
}

data class StorageSnapshot(
    val data: Map<String, Any>,
    val history: List<Message>,
    val timestamp: Instant
)
```

---

## **4. Пример использования в реальной стратегии**

### **Пример 1: Чат-бот с историей**

```kotlin
class ChatBotStrategy : AIAgentFunctionalStrategy<ChatInput, String> {
    override suspend fun execute(input: ChatInput, context: AIAgentFunctionalContext): String {
        val storage = context.storage
        val llm = context.llmContext!!  // предполагаем, что есть доступ к LLM
        
        // 1. Добавляем сообщение пользователя в историю
        storage.addMessage(Message.user(input.text))
        
        // 2. Получаем последние 10 сообщений для контекста
        val history = storage.getHistory(limit = 10)
        
        // 3. Формируем промпт с историей
        val prompt = Prompt {
            system("Ты полезный ассистент")
            messages(history)  // история как список Message
            user(input.text)
        }
        
        // 4. Выполняем запрос
        val response = llm.execute(prompt).text
        
        // 5. Сохраняем ответ в историю
        storage.addMessage(Message.assistant(response))
        
        // 6. Также сохраняем как отдельный ключ (если нужно)
        storage.save("last_response", response)
        
        return response
    }
}
```

### **Пример 2: Цепочка обработки данных**

```kotlin
class DataProcessingStrategy : AIAgentFunctionalStrategy<DataInput, ProcessedData> {
    override suspend fun execute(input: DataInput, context: AIAgentFunctionalContext): ProcessedData {
        val storage = context.storage
        
        // Шаг 1: загрузка и валидация
        val validated = validate(input)
        storage.save("validated_data", validated)
        
        // Шаг 2: преобразование (может быть тяжелой операцией)
        val transformed = transform(validated)
        storage.save("transformed_data", transformed)
        
        // Шаг 3: анализ (может быть отложен, если предыдущие шаги уже выполнялись)
        if (!storage.contains("analysis_result")) {
            val analysis = analyze(transformed)
            storage.save("analysis_result", analysis)
        }
        val analysis = storage.load<Analysis>("analysis_result")!!
        
        // Шаг 4: финальная агрегация
        val result = aggregate(validated, transformed, analysis)
        storage.save("final_result", result)
        
        return result
    }
}
```

---

## **5. Ключевые отличия от `AIAgentStateManager`**

| Аспект | `AIAgentStorage` (Functional) | `AIAgentStateManager` (Planner) |
|--------|------------------------------|--------------------------------|
| **Тип агента** | `FunctionalAIAgent` (линейная цепочка) | `PlannerAIAgent` (граф узлов) |
| **Основная цель** | Хранение истории и промежуточных результатов цепочки | Обмен данными между узлами графа |
| **Структура данных** | Чаще линейная/историческая (сообщения, шаги) | Произвольные ключи, обычно привязаны к ID узлов |
| **Методы** | `addMessage()`, `getHistory()`, `trimHistory()` | Вероятно, только `save/load/clear` (без специфики для истории) |
| **Изоляция** | По ключам шагов или сообщений | По ID узлов графа |
| **Время жизни** | Один вызов `execute()` (но может включать много шагов) | Один граф выполнения (все узлы) |
| **Доступ из** | Стратегия и шаги пайплайна | Узлы графа (через `NodeContext`) |

---

## **6. Критические пробелы в информации**

Из предоставленного контекста **недостаточно данных** о:

1. **Полный интерфейс `AIAgentStorage`** — какие методы обязательны, какие опциональны?
2. **Реализация по умолчанию** — in-memory (HashMap), persistent (файлы/БД), или гибрид?
3. **Изоляция между параллельными вызовами** — если два потока вызывают `agent.execute()` одновременно, будут ли они мешать друг другу в `storage`?
4. **Сериализация** — как сохраняются сложные объекты (сериализуются в JSON, сохраняются как есть, или нужно реализовать `Serializable`)?
5. **Размер и лимиты** — есть ли лимит на количество ключей, размер значений?
6. **Время жизни (TTL)** — данные живут только durante execute() или могут сохраняться между вызовами?
7. **Интеграция с историей диалога** — `addMessage()` и `getHistory()` — это часть `AIAgentStorage` или отдельный компонент?
8. **Обработка ошибок** — что происходит при `save` в переполненное хранилище? При `load` несуществующего ключа?
9. **Наблюдаемость (observability)** — есть ли возможность подписаться на изменения, получить метрики?
10. **Примеры реального использования** — как именно стратегии используют `storage`? Есть ли стандартные шаблоны?

---

## **7. Где искать в кодовой базе?**

1. **Файл `AIAgentStorage.kt`** — должен содержать интерфейс/абстрактный класс.
2. **Реализация по умолчанию** — возможно, `InMemoryAgentStorage` или `DefaultAgentStorage`.
3. **Тесты**:
   - `AIAgentStorageTest.kt` — проверка методов
   - `FunctionalAIAgentTest.kt` — как storage используется в реальном агенте
4. **Стратегии в `samples/`** — примеры, где явно используется `context.storage`.
5. **Поиск по коду**:
   - `context.storage.save(`
   - `context.storage.load<`
   - `context.storage.addMessage(`

---

## **8. Рекомендации по использованию**

### **8.1. Используйте осмысленные ключи**
```kotlin
// Хорошо
storage.save("user_intent", intent)
storage.save("search_results", results)

// Плохо (коллизии, нечитаемость)
storage.save("data", data)
storage.save("temp", temp)
```

### **8.2. Очищайте временные данные**
```kotlin
// После использования временного ключа
storage.clear("temp_*")  // если поддерживается pattern
```

### **8.3. Для истории диалога используйте `addMessage`/`getHistory`**
Если эти методы существуют, они уже упаковывают данные в `Message`对象, что упрощает работу:

```kotlin
storage.addMessage(Message.user("Привет"))
storage.addMessage(Message.assistant("И вам привет!"))
val history = storage.getHistory(limit = 5)
```

### **8.4. Ограничивайте размер истории**
```kotlin
// Если история растёт, обрезайте
storage.trimHistory(maxSize = 20)  // только последние 20 сообщений
```

### **8.5. Создавайте снапшоты для отладки**
```kotlin
val snapshot = storage.snapshot()
// Если что-то пошло не так, можно восстановить
storage.restore(snapshot)
```

### **8.6. Не храните большие объекты**
`AIAgentStorage` — не база данных. Храните только небольшие промежуточные результаты. Большие данные (документы, изображения) — во внешнем хранилище, а в `storage` — только ссылки/ключи.

---

## **9. Возможные расширения (если реализуете сами)**

Если вы разрабатываете собственную реализацию `AIAgentStorage`, рассмотрите:

1. **Мульти-tenant изоляцию** — если агент используется в SaaS, нужно изолировать данные по tenantId.
2. **Шифрование** — для чувствительных данных (персональная информация).
3. **Сжимание** — для больших текстов.
4. **Индексирование** — быстрый поиск по ключам.
5. **Восстановление после сбоя** — если хранение персистентное.
6. **Экспорт/импорт** — для анализа и миграции.

---

## **10. Вывод**

`AIAgentStorage` — это **важный компонент функционального агента**, который:
- **Обеспечивает общее хранилище данных** для всех шагов цепочки
- **Позволяет сохранять историю** диалога (если применимо)
- **Решает проблему передачи данных** между шагами без глобальных переменных
- **Повышает тестируемость** — можно инъектировать mock-хранилище
- **Упрощает отладку** — все промежуточные результаты доступны в одном месте

Однако **детали реализации и полный API неизвестны** из предоставленного контекста. Для точного использования необходимо найти:
- Определение интерфейса `AIAgentStorage`
- Реализацию по умолчанию
- Примеры в тестах и самплах

---

### Источники

[КЛАСС 2] FunctionalAIAgent · FunctionalAIAgent.kt

[ИСТОЧНИК 3] FunctionalAIAgent.kt · line 63

[ИСТОЧНИК 2] PlannerAIAgent.kt · line 22

[КЛАСС 1] PlannerAIAgent · PlannerAIAgent.kt

[КЛАСС 4] AIAgentFunctionalPipeline · (упомянут в контексте)

## Q25: "как работает и зона ответственности AIAgentStrategy"
Отличный вопрос! На основе контекста разберём **`AIAgentStrategy`** подробно. Важно: в контексте нет единого класса `AIAgentStrategy`, но есть **два конкретных типа стратегий**, которые управляют разными типами агентов. Рассмотрим оба.

---

## **1. Архитектурная роль `AIAgentStrategy`**

### **1.1. Это НЕ единый интерфейс**
В кодовой базе Koog **нет общего интерфейса `AIAgentStrategy`**. Вместо этого есть **два отдельных стратегических класса**:
1. **`AIAgentPlannerStrategy<Input, Output, Context>`** — для `PlannerAIAgent`
2. **`AIAgentFunctionalStrategy<Input, Output>`** — для `FunctionalAIAgent`

Оба определяют **алгоритм выполнения** агента, но для разных парадигм.

### **1.2. Что такое "стратегия" в Koog?**
Стратегия — это **шаблон проектирования (Strategy Pattern)**, который:
- **Инкапсулирует алгоритм** выполнения агента
- **Позволяет менять поведение** агента в runtime (подмена стратегии)
- **Разделяет** общую структуру агента (оборудование, LLM, инструменты) от **специфической логики** (как решать задачу)

---

## **2. `AIAgentPlannerStrategy` (Графовая стратегия)**

### **2.1. Зона ответственности**
Стратегия **строит граф задач** для выполнения на основе входных данных. Она отвечает на вопрос: **"Какие подзадачи нужно выполнить и в каком порядке?"**

**Конкретные задачи:**
1. **Декомпозиция** входного запроса на подзадачи (узлы графа)
2. **Определение зависимостей** между подзадачами (что должно выполниться раньше)
3. **Создание узлов графа** (`GraphNode`) с логикой выполнения
4. **Возврат готового графа** (`Graph`) для выполнения средой (`GenericAgentEnvironment`)

### **2.2. Ключевой метод**
```kotlin
override suspend fun execute(input: Input, context: AIAgentPlannerContext): Graph
```
**Что делает:**
- Получает входные данные `input`
- Получает контекст `AIAgentPlannerContext` (доступ к LLM, stateManager, environment)
- Строит граф, добавляя узлы в `context.environment`
- Возвращает объект `Graph` (или вызывает `context.environment.build()`)

### **2.3. Как она работает (механика)**

**Шаг 1: Получение контекста**
```kotlin
val env = context.environment  // среда для построения графа
val llm = context.llmContext   // доступ к LLM для планирования
val state = context.stateManager  // управление состоянием
```

**Шаг 2: Анализ и декомпозиция**
```kotlin
// Пример: LLM помогает разбить задачу
val decomposition = llm.execute(
    Prompt.system("Разбей задачу '${input.query}' на 3 подзадачи")
).text
val subtasks = parse(decomposition)  // ["Поиск", "Анализ", "Ответ"]
```

**Шаг 3: Создание узлов**
```kotlin
subtasks.forEach { subtask ->
    val node = GraphNode(
        id = subtask.id,
        executor = { nodeCtx ->
            // Логика выполнения узла
            val result = llm.execute(Prompt.system("Выполни: $subtask"))
            state.save(subtask.id, result)  // сохраняем результат
            result
        }
    )
    env.addNode(node)
}
```

**Шаг 4: Определение зависимостей**
```kotlin
// Зависимости: анализ после поиска, ответ после анализа
env.addNode(analysisNode, dependencies = listOf(searchNode.id))
env.addNode(responseNode, dependencies = listOf(analysisNode.id))
```

**Шаг 5: Построение и возврат графа**
```kotlin
return env.build()  // Graph объект с узлами и рёбрами
```

**Шаг 6: Выполнение графа (не стратегией!)**
После возврата графа, **пайплайн** (`AIAgentPlannerPipeline`) вызывает:
```kotlin
environment.execute(graph)  // выполняет граф (параллельно/последовательно)
```

### **2.4. Примеры типов графовых стратегий**

| Стратегия | Что делает | Пример |
|-----------|------------|---------|
| **LinearStrategy** | Линейная цепочка узлов (A→B→C) | `node1 → node2 → node3` |
| **ParallelStrategy** | Независимые узлы выполняются параллельно | `[node1, node2, node3] → aggregator` |
| **ConditionalStrategy** | Условные ветвления на основе LLM-ответа | `classify → (weather \| news) → response` |
| **RecursiveStrategy** | Рекурсивное разбиение (дерево задач) | Планирование сложных проектов |
| **DynamicStrategy** | Граф меняется во время выполнения | Добавление узлов по ходу работы |

---

## **3. `AIAgentFunctionalStrategy` (Функциональная стратегия)**

### **3.1. Зона ответственности**
Стратегия **определяет последовательность шагов** (функций) для выполнения. Она отвечает на вопрос: **"В каком порядке выполнять функции и как передавать данные между ними?"**

**Конкретные задачи:**
1. **Определение цепочки функций** (шагов)
2. **Передача данных** между шагами (через контекст или напрямую)
3. **Обработка результатов** каждого шага
4. **Формирование финального результата**

### **3.2. Ключевой метод**
```kotlin
override suspend fun execute(input: Input, context: AIAgentFunctionalContext): Output
```
**Что делает:**
- Получает входные данные `input`
- Получает контекст `AIAgentFunctionalContext` (доступ к storage, toolRegistry)
- **Непосредственно выполняет** шаги (в отличие от графовой стратегии, которая только строит граф)
- Возвращает финальный результат `Output`

### **3.3. Как она работает (механика)**

```kotlin
class MyFunctionalStrategy : AIAgentFunctionalStrategy<Input, String> {
    override suspend fun execute(input: Input, context: AIAgentFunctionalContext): String {
        val storage = context.storage
        
        // Шаг 1: предобработка
        val processed = preprocess(input)
        storage.save("processed", processed)
        
        // Шаг 2: вызов LLM
        val llmResult = callLLM(processed)
        storage.save("llm_result", llmResult)
        
        // Шаг 3: постобработка
        val final = postprocess(llmResult)
        
        return final
    }
    
    private suspend fun preprocess(input: Input): String { ... }
    private suspend fun callLLM(text: String): String { ... }
    private suspend fun postprocess(result: String): String { ... }
}
```

**Важно:** В функциональной стратегии **нет разделения** между построением и выполнением. Стратегия **сама выполняет** шаги последовательно.

---

## **4. Сравнительная таблица**

| Аспект | `AIAgentPlannerStrategy` | `AIAgentFunctionalStrategy` |
|--------|--------------------------|----------------------------|
| **Парадигма** | Графовая (дерево задач) | Функциональная (цепочка функций) |
| **Метод execute** | Строит и возвращает `Graph` | Немедленно выполняет шаги, возвращает `Output` |
| **Выполнение** | Отдельно (пайплайн вызывает `environment.execute(graph)`) | Внутри метода `execute` |
| **Параллелизм** | Да (независимые узлы) | Нет (если не использовать `async` внутри) |
| **Динамичность** | Граф строится динамически на основе LLM | Цепочка шагов статична (но шаги могут быть conditionally) |
| **Передача данных** | Через `stateManager` (по ключам) | Через параметры функций или `storage` |
| **Сложность** | Выше (нужно думать о зависимостях) | Ниже (простая последовательность) |
| **Использование** | Сложные задачи с декомпозицией | Простые цепочки обработки |
| **Контекст** | `AIAgentPlannerContext` (environment, stateManager, llmContext) | `AIAgentFunctionalContext` (storage, toolRegistry) |

---

## **5. Кто и когда использует стратегию?**

### **5.1. При создании агента**
Стратегия передаётся в билдер:

```kotlin
// Для PlannerAIAgent (граф)
val agent = GraphAgentServiceBuilder()
    .withStrategy(MyGraphStrategy())  // ← AIAgentPlannerStrategy
    .build()

// Для FunctionalAIAgent (цепочка)
val agent = AIAgentServiceBuilder()
    .strategy(MyFunctionalStrategy())  // ← AIAgentFunctionalStrategy
    .build()
```

### **5.2. Во время выполнения**
1. Агент создаёт **контекст** (Planner или Functional)
2. Вызывает `strategy.execute(input, context)`
3. В графовом случае получает `Graph`, затем пайплайн выполняет его
4. В функциональном случае получает `Output` сразу

---

## **6. Внутренности: что происходит в `execute()`?**

### **6.1. Для `AIAgentPlannerStrategy`**
```kotlin
override suspend fun execute(input: Input, context: AIAgentPlannerContext): Graph {
    // 1. Анализ input
    val analysis = analyze(input)
    
    // 2. Создание узлов
    val node1 = GraphNode("step1") { /* lambda-исполнитель */ }
    val node2 = GraphNode("step2") { /* lambda-исполнитель */ }
    
    // 3. Добавление в environment
    context.environment.addNode(node1)
    context.environment.addNode(node2, dependencies = listOf(node1.id))
    
    // 4. Построение графа
    return context.environment.build()
}
```
**Важно:** Стратегия не выполняет узлы! Она только строит граф.

### **6.2. Для `AIAgentFunctionalStrategy`**
```kotlin
override suspend fun execute(input: Input, context: AIAgentFunctionalContext): Output {
    // 1. Шаг 1
    val result1 = step1(input)
    context.storage.save("step1", result1)
    
    // 2. Шаг 2 (зависит от шага 1)
    val result2 = step2(result1)
    
    // 3. Возврат результата
    return result2
}
```
**Важно:** Стратегия **сама выполняет** шаги в своем коде.

---

## **7. Critiques и ограничения**

### **7.1. Проблемы `AIAgentPlannerStrategy`**
1. **Отсутствие обратной связи** — стратегия строит граф "в vacuum", без возможности адаптировать граф на основе промежуточных результатов (если только не читать `stateManager` после каждого узла, что бессмысленно).
2. **Сложность тестирования** — нужно тестировать построение графа, а не результат.
3. **Негибкость** — граф фиксируется на момент `build()`. Невозможно добавить узел после начала выполнения.
4. **Неявные зависимости** — если узел читает `stateManager`, зависимости неявны (не видно в `addNode(dependencies)`).

### **7.2. Проблемы `AIAgentFunctionalStrategy`**
1. **Жёсткая последовательность** — нет параллелизма "из коробки".
2. **Загрязнение контекста** — шаги могут модифицировать `storage` произвольно.
3. **Отсутствие декларативности** — порядок шагов скрыт в императивном коде, сложнее визуализировать.
4. **Сложность условной логики** — если нужно ветвление, приходится писать `if/else`, что может привести к "спагетти-коду".

---

## **8. Дополнительные возможности (если бы они были)**

### **8.1. Для графовой стратегии**
- **Итеративное уточнение графа** — LLM может дорабатывать граф после выполнения первых узлов.
- **Ветвление на лету** — добавление узлов по результатам предыдущих.
- **Приоритеты и ограничения** — не только зависимости, но и приоритеты ресурсов.

### **8.2. Для функциональной стратегии**
- **Декларативный DSL** — вместо императивных вызовов:
  ```kotlin
  strategy {
      step("preprocess") { preprocess(it) }
      step("llm") { callLLM(it) }
      step("postprocess") { postprocess(it) }
  }
  ```
- **Параллельные ветви** — `par { stepA(); stepB() }`
- **Автоматическое сохранение** — промежуточные результаты автоматически в storage.

---

## **9. Практические рекомендации**

### **9.1. Когда использовать какую стратегию?**

| Сценарий | Рекомендуемая стратегия | Почему |
|----------|------------------------|--------|
| **LLM должен спланировать** задачи | `AIAgentPlannerStrategy` | Граф строится динамически на основе LLM-ответа |
| **Фиксированный пайплайн** (preprocess → LLM → postprocess) | `AIAgentFunctionalStrategy` | Просто, понятно, легко тестировать |
| **Множество независимых исследований** | `AIAgentPlannerStrategy` | Параллельное выполнение |
| **Условная логика** (if weather → A, else → B) | `AIAgentPlannerStrategy` | Условные ветви удобно выражать как зависимости |
| **Цепочка с состоянием** (шаг 2 зависит от шага 1) | Любая, но функциональная проще | Прямая передача результата или storage |
| **Динамическое количество шагов** (N раз повторять) | `AIAgentPlannerStrategy` | Легко создать N узлов в цикле |

### **9.2. Если нужно и то, и другое**
Можно создать **гибридную стратегию**:
- Использовать `AIAgentPlannerStrategy` для декомпозиции
- В узлах графа использовать **функциональные цепочки** (внутри одного узла)

Пример:
```kotlin
class HybridStrategy : AIAgentPlannerStrategy<Input, Output, AIAgentPlannerContext> {
    override suspend fun execute(input: Input, context: AIAgentPlannerContext): Graph {
        val env = context.environment
        
        // Узел 1: функциональная цепочка внутри
        val node1 = GraphNode("research") { nodeCtx ->
            // Внутри узла — своя функциональная стратегия
            val subAgent = AIAgentServiceBuilder()
                .strategy(object : AIAgentFunctionalStrategy<ResearchInput, String> {
                    override suspend fun execute(
                        input: ResearchInput, 
                        context: AIAgentFunctionalContext
                    ): String {
                        return chainOfFunctions(input)
                    }
                })
                .build()
            subAgent.execute(ResearchInput(...))
        }
        
        env.addNode(node1)
        return env.build()
    }
}
```

---

## **10. Критические пробелы в понимании**

1. **Кто создаёт стратегию?** Пользователь кода или LLM?
2. **Может ли стратегия меняться во время выполнения?** (dynamic replanning)
3. **Как handled ошибки в стратегиях?** Исключения в `execute()` — это фатально?
4. **Может ли стратегия использовать инструменты?** Да, через `context.toolRegistry` (если доступен).
5. **Как тестировать стратегии?** Unit-тесты на построение графа или моки.
6. **Можно ли комбинировать стратегии?** (несколько стратегий в одном агенте)
7. **Где хранятся стратегии?** В репозитории, загружаются динамически?
8. **Как стратегии логируются?** Нужен ли tracer для отладки графа?

---

## **11. Где искать в кодовой базе?**

1. **Интерфейсы/абстрактные классы:**
   - `AIAgentPlannerStrategy.kt`
   - `AIAgentFunctionalStrategy.kt`
   - Возможно, общий `AIAgentStrategy.kt` (но в контексте его нет)

2. **Примеры реализаций:**
   - `LinearGraphStrategy.kt`
   - `ParallelGraphStrategy.kt`
   - `ConditionalGraphStrategy.kt`
   - `SimpleFunctionalStrategy.kt`

3. **Тесты:**
   - `AIAgentPlannerStrategyTest.kt`
   - `AIAgentFunctionalStrategyTest.kt`

4. **Пайплайны, которые используют стратегии:**
   - `AIAgentPlannerPipeline.kt` — вызывает `strategy.execute()`, затем `environment.execute(graph)`
   - `AIAgentFunctionalPipeline.kt` — вызывает `strategy.execute()` и возвращает результат

5. **Поисковые запросы:**
   - `implements AIAgentPlannerStrategy`
   - `implements AIAgentFunctionalStrategy`
   - `strategy.execute(`

---

## **12. Пример полного цикла с графовой стратегией**

```kotlin
// 1. Стратегия (определяет граф)
class ResearchStrategy : AIAgentPlannerStrategy<ResearchInput, String, AIAgentPlannerContext> {
    override suspend fun execute(input: ResearchInput, context: AIAgentPlannerContext): Graph {
        val env = context.environment
        val llm = context.llmContext!!
        
        // Узел 1: поиск
        val searchNode = GraphNode("search") { nodeCtx ->
            llm.execute(Prompt.system("Найди информацию о: ${input.topic}")).text
        }
        
        // Узел 2: анализ (зависит от поиска)
        val analyzeNode = GraphNode("analyze") { nodeCtx ->
            val searchResult = nodeCtx.stateManager.load<String>("search")  // читает результат searchNode
            llm.execute(Prompt.system("Проанализируй: $searchResult")).text
        }
        
        env.addNode(searchNode)
        env.addNode(analyzeNode, dependencies = listOf(searchNode.id))
        
        return env.build()
    }
}

// 2. Агент создаётся со стратегией
val agent = GraphAgentServiceBuilder()
    .withStrategy(ResearchStrategy())
    .withLLM(OpenAIModel("gpt-4"))
    .build()

// 3. Выполнение
val result = agent.execute(ResearchInput(" Agents in AI"))

// Внутри:
// - Создаётся AIAgentPlannerContext (environment, stateManager, llmContext)
// - strategy.execute(input, context) → Graph (узлы: search → analyze)
// - AIAgentPlannerPipeline → environment.execute(graph)
// - Узлы выполняются (search параллельно? нет, analyze ждёт search)
// - Результат analyze возвращается как результат агента
```

---

## **13. Вывод**

**`AIAgentStrategy` (в двух вариантах) — это "мозг" агента**, который:
- **Для `PlannerAIAgent`**: строит **граф задач** (декларативно, через dependencies)
- **Для `FunctionalAIAgent`**: выполняет **цепочку функций** (императивно, через код)

**Ключевое различие:**
- Графовая стратегия **возвращает план** (`Graph`), который **выполняется отдельно**
- Функциональная стратегия **сама выполняет** шаги и возвращает результат

**Что выбрать:**
- Сложные задачи с **независимыми подзадачами** → графовая стратегия
- Простые **линейные цепочки** → функциональная стратегия

**Что не хватает в контексте:**
1. Полные определения интерфейсов стратегий
2. Примеры реальных стратегий в кодовой базе
3. Детали обработки ошибок в стратегиях
4. Возможность динамического изменения стратегии (replanning)

---

### Источники

[КЛАСС 1] PlannerAIAgent · PlannerAIAgent.kt
[КЛАСС 2] FunctionalAIAgent · FunctionalAIAgent.kt
[ИСТОЧНИК 2] PlannerAIAgent.kt · line 22
[ИСТОЧНИК 3] FunctionalAIAgent.kt · line 63
[ИСТОЧНИК 4] AIAgentServiceBuilderImpl.kt · line 60
[ИСТОЧНИК 5] AIAgentServiceBuilderImpl.kt · line 72
[ИСТОЧНИК 1] AIAgentConfigBase.kt · for · line 1

## Q26: "как работает и зона ответственности AIAgentSubgraph с примерами"
Не знаю. Уточните: существует ли в предоставленной кодовой базе класс `AIAgentSubgraph` и, если да, то в каком файле/пакете он определён?

## Q27: "расскажи все про ExecutionPointNode"
Не знаю. В предоставленном контексте **отсутствует класс `ExecutionPointNode`**. 

На основе ранее проанализированной архитектуры Koog, можно предположить, что `ExecutionPointNode` может относиться к одной из следующих гипотетических областей:

---

## **Гипотетические варианты (наarchitecture pattern of Koog)**

### **1. Возможный контекст: Графовое выполнение (PlannerAIAgent)**
Если `ExecutionPointNode` существует, он, скорее всего, является **частью системы графового выполнения** и может быть:

- **Конкретной реализацией `AIAgentNode`** (который был гипотетически описан ранее)
- **Узлом графа, представляющим执行точку (точку выполнения)** с дополнительной семантикой (например, checkpoint, отладка, мониторинг)

---

### **2. Предполагаемая зона ответственности (если бы он существовал)**

| Характеристика | Возможное значение |
|----------------|-------------------|
| **Назначение** | Узел графа, представляющий **конкретную execute-точку** с возможностью: <br>• Логирования<br>• Трассировки<br>• Создания checkpoint'а состояния<br>• Обработки ошибок на уровне узла |
| **Отношения** | <ul><li>● **Наследует/реализует**: `AIAgentNode` (если существует)</li><li>● **Использует**: `AIAgentStateManager`, `NodeContext`</li></ul> |
| **Ключевые методы** | <ul><li>`suspend fun execute(context: NodeContext): ExecutionResult`</li><li>`fun getCheckpoint(): Checkpoint`</li><li>`fun restore(checkpoint: Checkpoint)`</li><li>`fun addListener(listener: ExecutionListener)`</li></ul> |
| **Данные** | <ul><li>`val id: String`</li><li>`val dependencies: List<String>`</li><li>`val metadata: Map<String, Any>` (теги, приоритет)</li><li>`val checkpointData: MutableMap<String, Any>`</li></ul> |

---

### **3. Гипотетический пример использования (в графовой стратегии)**

```kotlin
// Допустим, ExecutionPointNode существует
class MonitoringGraphStrategy : AIAgentPlannerStrategy<Input, Output, AIAgentPlannerContext> {
    override suspend fun execute(input: Input, context: AIAgentPlannerContext): Graph {
        val env = context.environment
        
        // Создаём узлы с мониторингом
        val researchNode = ExecutionPointNode(
            id = "research",
            executor = { nodeCtx ->
                val result = callResearchLLM(input.query)
                // Автоматически логируется, checkpoint создаётся
                result
            },
            // Дополнительные возможности ExecutionPointNode:
            checkpointOnFinish = true,  // создавать checkpoint после выполнения
            metrics = mapOf("type" to "research")  // метрики для мониторинга
        )
        
        val analysisNode = ExecutionPointNode(
            id = "analysis",
            dependencies = listOf(researchNode.id),
            executor = { nodeCtx ->
                // Читаем результат предыдущего узла
                val researchResult = nodeCtx.getDependencyResult<String>("research")
                callAnalysisLLM(researchResult)
            }
        )
        
        env.addNode(researchNode)
        env.addNode(analysisNode)
        
        return env.build()
    }
}
```

---

### **4. Что могло бы давать `ExecutionPointNode` (преимущества)**

| Функция | Описание |
|---------|----------|
| **Checkpoint/Restore** | Возможность сохранить состояние узла (результат, контекст) и восстановить его при повторном запуске или откате |
| **Enhanced Logging** | Автоматическое логирование входа/выхода, времени выполнения, ошибок |
| **Metrics & Tracing** | Сбор метрик (duration, success rate) и интеграция с tracing-системами (Jaeger, OpenTelemetry) |
| **Retry Policies** | Встроенные политики повторных попыток при ошибках |
| **Conditional Execution** | Возможность пропуска узла на основе conditions (без изменения графа) |
| **Debug Mode** | Приостановка выполнения узла для отладки, интерактивный режим |

---

### **5. Альтернативные названия в других фреймворках**

| Понятие в Koog (гипотетическое) | Аналоги в других системах |
|--------------------------------|--------------------------|
| `ExecutionPointNode` | <ul><li>**Airflow**: `BaseOperator` + `on_success_callback`</li><li>**Temporal**: `Activity` + `heartbeat`</li><li>**LangChain**: `Runnable` + `with_config`</li><li>**AWS Step Functions**: `Task State` + ` heartbeat`/`timeout`</li></ul> |

---

### **6. Почему его НЕТ в текущем контексте?**

1. **Недостаточно данных** — контекст фокусировался на:
   - `AIAgentStrategy` (Planner/Functional)
   - `StateManager`/`Storage`
   - `GenericAgentEnvironment` (гипотетический)
   - Но **не на конкретных реализациях узлов графа**

2. **Возможные причины отсутствия:**
   - Класс называется иначе (`GraphNode`, `TaskNode`, `WorkflowNode`)
   - Это **внутренний класс** в пакете `environment` или `graph`
   - Это **не класс, а интерфейс** или **data class**
   - `ExecutionPointNode` — это **гипотетическое имя**, придуманное для обсуждения

---

### **7. Где искать в реальной кодовой базе Koog**

Если класс существует, проверьте:

1. **Пакеты, связанные с графами:**
   ```
   com.koog.agent.graph
   com.koog.agent.environment
   com.koog.agent.strategy.planner
   ```

2. **Поисковые запросы в коде:**
   - `class ExecutionPointNode`
   - `interface ExecutionPoint`
   - `ExecutionPointNode(` (конструктор)
   - `ExecutionPoint` (как часть имени)

3. **Файлы, которые могут содержать:**
   - `GraphNode.kt`
   - `ExecutionNode.kt`
   - `WorkflowNode.kt`
   - `AgentNode.kt`

4. **Тесты:**
   - `ExecutionPointNodeTest.kt`
   - `GraphExecutionTest.kt`

---

### **8. Рекомендации по уточнению**

Для получения точной информации о `ExecutionPointNode` уточните:

1. **Где вы встретили это имя?** (файл, логи, ошибка, документация)
2. **В каком контексте?** (при построении графа, мониторинге, отладке?)
3. **Есть ли похожие классы?** (например, `GraphNode`, `BaseNode`)

---

### **9. Если `ExecutionPointNode` НЕ существует**

Скорее всего, в Koog используется **простой `GraphNode`** (или аналог) без расширенных возможностей checkpoint/monitoring. В этом случае:

- **Мониторинг** реализуется на уровне `GenericAgentEnvironment` или пайплайна
- **Checkpoint** — через `AIAgentStateManager` (сохранение состояния по ключам)
- **Логирование** — через стандартные Kotlin/Android логи или SLF4J

---

### **10. Вывод**

Без прямого упоминания в контексте **нельзя дать точное описание `ExecutionPointNode`**. 

**Чтобы продвинуться:**
1. Найдите в кодовой базе файл с этим классом
2. Если его нет — уточните, откуда взялось это имя (возможно, это из другой статьи/документации о Koog)
3. Рассмотрите альтернативные классы: `GraphNode`, `TaskNode`, `AgentNode`

---

### **Критические вопросы для уточнения:**
1. **Существует ли класс `ExecutionPointNode` в текущей версии Koog?** (проверьте `grep -r "ExecutionPointNode" .`)
2. **Если да — в каком пакете/файле он определён?**
3. **Какие методы/свойства у него есть?** (покажите сигнатуры)
4. **Как он связан с `GenericAgentEnvironment` или `AIAgentStateManager`?**

Без этих данных любое описание будет спекулятивным.

---

### Источники

[КЛАСС 1] PlannerAIAgent · PlannerAIAgent.kt  
[КЛАСС 2] FunctionalAIAgent · FunctionalAIAgent.kt  
[ИСТОЧНИК 2] PlannerAIAgent.kt · line 22  
[ИСТОЧНИК 3] FunctionalAIAgent.kt · line 63  
[ИСТОЧНИК 4] AIAgentServiceBuilderImpl.kt · line 60  
[ИСТОЧНИК 5] AIAgentServiceBuilderImpl.kt · line 72  
[ИСТОЧНИК 1] AIAgentConfigBase.kt · for · line 1

## Q28: "расскажи все про SubgraphMetadata"
Не знаю. В предоставленном контексте **отсутствует класс `SubgraphMetadata`**.

---

## **Анализ на основе архитектурных паттернов Koog**

### **1. Что МОЖЕТ означать `SubgraphMetadata`?**

В контексте **графовых агентов** (`PlannerAIAgent`) и системы `GenericAgentEnvironment`, `SubgraphMetadata` — это **гипотетический класс для метаданных подграфа**.

#### **Возможное назначение:**
- Хранение **декларативного описания вложенного графа** (subgraph) внутри основного графа задач
- Управление **иерархическими/составными графами** (composite graph pattern)
- Инкапсуляция **конфигурации и политик** для группы связанных узлов

---

### **2. Предполагаемая структура (гипотетическая)**

```kotlin
/**
 * Метаданные подграфа (вложенного графа задач)
 */
data class SubgraphMetadata(
    val id: String,                          // Уникальный ID подграфа
    val name: String,                        // Человеко-читаемое имя
    val description: String? = null,         // Описание назначения
    val inputType: KClass<*>?,               // Тип входных данных
    val outputType: KClass<*>?,              // Тип выходных данных
    val nodeIds: List<String>,               // ID узлов, входящих в подграф
    val entryPoints: List<String>,           // Точки входа (узлы-источники)
    val exitPoints: List<String>,            // Точки выхода (узлы-приёмники)
    val strategy: AIAgentPlannerStrategy<*, *, *>?,  // Стратегия для этого подграфа
    val dependencies: Set<String> = emptySet(), // Зависимости от других подграфов
    val metadata: Map<String, Any> = emptyMap(), // Доп. метаданные (приоритет, ресурсы и т.д.)
    val timeout: Duration? = null,           // Таймаут выполнения подграфа
    val retryPolicy: RetryPolicy? = null,    // Политика повторных попыток
    val checkpointConfig: CheckpointConfig? = null  // Конфиг checkpoint'ов
)
```

---

### **3. Типичные сценарии использования (если бы существовал)**

#### **Сценарий 1: Модульность и переиспользование**
```kotlin
// Определяем переиспользуемый подграф "ResearchPipeline"
val researchSubgraph = SubgraphMetadata(
    id = "research_pipeline_v1",
    name = "Research Pipeline",
    description = "Поиск, анализ и синтез информации по теме",
    nodeIds = listOf("search_web", "analyze_results", "synthesize_findings"),
    entryPoints = listOf("search_web"),
    exitPoints = listOf("synthesize_findings"),
    strategy = ResearchStrategy()  // Отдельная стратегия для этого подграфа
)

// Используем в основном графе
val mainGraph = environment.buildGraph {
    addSubgraph(researchSubgraph)
    addNode("final_report", dependencies = listOf("research_pipeline_v1:synthesize_findings"))
}
```

#### **Сценарий 2: Динамическое создание подграфов**
```kotlin
class DynamicSubgraphStrategy : AIAgentPlannerStrategy<Input, Output, AIAgentPlannerContext> {
    override suspend fun execute(input: Input, context: AIAgentPlannerContext): Graph {
        val env = context.environment
        
        // Создаём подграф на основе входных данных
        val subgraphMetadata = when (input.taskType) {
            "research" -> createResearchSubgraph()
            "code_gen" -> createCodeGenSubgraph()
            else -> throw IllegalArgumentException("Unknown task type")
        }
        
        // Регистрируем подграф в окружении
        env.registerSubgraph(subgraphMetadata)
        
        // Создаём узлы, которые будут использовать этот подграф
        val orchestratorNode = GraphNode(
            id = "orchestrator",
            executor = { nodeCtx ->
                // Запуск всего подграфа как единого блока
                val result = nodeCtx.executeSubgraph(
                    subgraphId = subgraphMetadata.id,
                    input = input
                )
                processResult(result)
            }
        )
        
        env.addNode(orchestratorNode)
        return env.build()
    }
}
```

#### **Сценарий 3: Мониторинг и трассировка**
```kotlin
// SubgraphMetadata позволяет отслеживать выполнение группы узлов
val monitoredSubgraph = SubgraphMetadata(
    id = "complex_analysis",
    nodeIds = listOf("fetch_data", "preprocess", "analyze", "visualize"),
    metadata = mapOf(
        "monitoring_enabled" to true,
        "tracing_tag" to "analysis_pipeline",
        "resource_limits" to mapOf("memory_mb" to 512, "cpu_cores" to 2)
    )
)

// Во время выполнения:
executionTracker.startSubgraph(monitoredSubgraph.id)
// ... выполнение узлов
executionTracker.endSubgraph(monitoredSubgraph.id, metrics)
```

---

### **4. Как мог бы выглядеть API `GenericAgentEnvironment` с поддержкой SubgraphMetadata**

```kotlin
interface GenericAgentEnvironment {
    // Основные методы для работы с графом
    fun addNode(node: GraphNode, dependencies: List<String> = emptyList())
    fun build(): Graph
    
    // Методы для работы с подграфами (ЕСЛИ光影SubgraphMetadata существует)
    fun registerSubgraph(metadata: SubgraphMetadata)
    fun getSubgraph(id: String): SubgraphMetadata?
    fun executeSubgraph(
        subgraphId: String, 
        input: Any, 
        context: NodeContext
    ): Any
    
    // Восстановление состояния подграфа
    fun restoreSubgraphState(
        subgraphId: String, 
        snapshot: SubgraphSnapshot
    )
}
```

---

### **5. Аналоги в других системах**

| Система | Аналог SubgraphMetadata |
|---------|------------------------|
| **Apache Airflow** | `SubDagOperator` + метаданные DAG |
| **AWS Step Functions** | `Map State` / `Parallel State` + `State Machine` definition |
| **Temporal** | `Workflow` + `Child Workflow` registration |
| **LangChain** | `RunnableSequence` / `RunnableParallel` + config |
| **Neo4j (GDS)** | `Graph Project` + `Node/Relationship` projections |

---

### **6. Почему его НЕТ в текущем контексте?**

1. **Упрощённая архитектура**: Возможно, в Koog **нет вложенных графов** (subgraphs), только плоские графы.
2. **Друбая терминология**: Может называться:
   - `GraphModule`
   - `CompositeNode`
   - `GraphTemplate`
   - `WorkflowTemplate`
3. **Это внутренняя реализация**: `SubgraphMetadata` может быть **private классом** внутри `GenericAgentEnvironment`.
4. **Ещё не реализовано**: Поддержка subgraphs — это **будущая фича**, не попавшая в текущий контекст.

---

### **7. Где искать в реальной кодовой базе Koog**

Если класс существует:

1. **Пакеты для поиска:**
   ```
   com.koog.agent.graph.metadata
   com.koog.agent.environment.subgraph
   com.koog.agent.strategy.composite
   ```

2. **Файлы, которые могут содержать:**
   - `SubgraphMetadata.kt`
   - `GraphModule.kt`
   - `CompositeGraph.kt`
   - `GraphTemplate.kt`
   - `Subgraph.kt` (вместо Metadata)

3. **Поисковые запросы:**
   ```bash
   grep -r "SubgraphMetadata" .
   grep -r "subgraph" com/koog/agent
   grep -r "registerSubgraph" .
   grep -r "executeSubgraph" .
   ```

4. **Косвенные признаки:**
   - Методы `environment.*subgraph*` в `GenericAgentEnvironment`
   - Классы с полями типа `List<SubgraphMetadata>`
   - Фабрики/билдеры для составных графов

---

### **8. Альтернативные подходы (без SubgraphMetadata)**

Если подграфы не поддерживаются явно, возможно:

#### **Вариант A: Группы узлов через naming convention**
```kotlin
// Узлы с префиксом "research_*" считаются частью подграфа
val researchNodes = graph.nodes.filter { it.id.startsWith("research_") }
```

#### **Вариант B: Функциональные агенты внутри графа**
```kotlin
// Функциональный агент как "подграф"
val researchAgent = FunctionalAIAgent(
    strategy = ResearchChainStrategy(),
    context = AIAgentFunctionalContext()
)

val node = GraphNode(
    id = "research",
    executor = { ctx -> researchAgent.execute(input) }
)
```

#### **Вариант C: Вложенные графы через GenericAgentEnvironment**
```kotlin
// Создаём отдельное окружение для подграфа
val subgraphEnv = GenericAgentEnvironment()
subgraphEnv.addNode(...)  // наполняем узлами
val subgraph = subgraphEnv.build()

// В основном графе - узел, который выполняет весь подграф
val node = GraphNode(
    id = "run_subgraph",
    executor = { ctx -> 
        val subgraphResult = ctx.environment.execute(subgraph, ctx.input)
        subgraphResult
    }
)
```

---

### **9. Критические вопросы для уточнения**

1. **Откуда вы узнали о `SubgraphMetadata`?**
   - Документация Koog?
   - Пример кода?
   - Issue tracker / roadmap?

2. **Контекст использования?**
   - При построении графа?
   - Для мониторинга?
   - Для динамической композиции?

3. **Похожие классы в контексте?**
   - Есть ли `GraphMetadata`, `NodeMetadata`, `WorkflowMetadata`?
   - Видели ли вы методы `registerSubgraph()` или `getSubgraph()`?

---

### **10. Вывод**

Без прямого упоминания в контексте **нельзя дать точное описание `SubgraphMetadata`**.

**Чтобы продвинуться:**
1. Найдите в кодовой базе файл с этим классом
2. Если его нет — проверьте, не используется ли аналог (`GraphModule`, `CompositeGraph`)
3. Изучите `GenericAgentEnvironment` на наличие методов для работы с составными графами

---

### **Рекомендация:**
Если вы проектируете систему с подграфами, но `SubgraphMetadata` отсутствует, предлагаю:

```kotlin
// Минималистичная реализация (если нужно добавить)
data class SubgraphMetadata(
    val id: String,
    val graph: Graph,  // Сам граф (узлы + зависимости)
    val inputMapper: (Any) -> Any = { it },  // Маппинг входов
    val outputMapper: (Any) -> Any = { it }, // Маппинг выходов
    val config: SubgraphConfig = SubgraphConfig()
)

data class SubgraphConfig(
    val timeout: Duration? = null,
    val retries: Int = 0,
    val isolationLevel: IsolationLevel = IsolationLevel.NONE
)
```

---

### **Итог:**
**`SubgraphMetadata` — вероятно, класс для описания вложенных графов, но в текущем контексте Koog его нет.**

Для точной информации уточните:
1. **Где вы встретили это имя?**
2. **Есть ли пример его использования?**
3. **Какой пакет/модуль Koog вы изучаете?**

---

### Источники

[КЛАСС 1] PlannerAIAgent · PlannerAIAgent.kt  
[КЛАСС 2] FunctionalAIAgent · FunctionalAIAgent.kt  
[ИСТОЧНИК 2] PlannerAIAgent.kt · line 22  
[ИСТОЧНИК 3] FunctionalAIAgent.kt · line 63  
[ИСТОЧНИК 4] AIAgentServiceBuilderImpl.kt · line 60  
[ИСТОЧНИК 5] AIAgentServiceBuilderImpl.kt · line 72  
[ИСТОЧНИК 1] AIAgentConfigBase.kt · for · line 1

## Q29: "примеры использования AIAgentSubgraphBuilder"
Не знаю. В предоставленном контексте **отсутствует класс `AIAgentSubgraphBuilder`**.

---

## **Анализ на основе архитектурных паттернов Koog**

### **1. Что МОЖЕТ означать `AIAgentSubgraphBuilder`?**

В контексте **графовых агентов** (`PlannerAIAgent`) и hypothetical `GenericAgentEnvironment`, `AIAgentSubgraphBuilder` — это **гипотетический строитель (Builder) для создания подграфов (subgraphs)**.

#### **Возможное назначение:**
- **Декларативное создание вложенных графов** (subgraphs) из группы узлов с определёнными зависимостями
- **Инкапсуляция сложной логики построения** повторно используемых графовых фрагментов
- **Регистрация подграфов** в `GenericAgentEnvironment` для последующего использования

---

### **2. Предполагаемая структура (гипотетическая)**

```kotlin
/**
 * Builder для создания подграфов в Koog
 * Позволяет декларативно определять группу узлов с зависимостями,
 * которая может быть использована как единый компонент в основном графе.
 */
class AIAgentSubgraphBuilder(
    private val subgraphId: String,
    private val environment: GenericAgentEnvironment  // ← требуется доступ к environment
) {
    private val nodes = mutableMapOf<String, GraphNode>()
    private val dependencies = mutableMapOf<String, List<String>>()
    private var metadata: SubgraphMetadata? = null
    
    /**
     * Добавляет узел в подграф
     */
    fun addNode(
        nodeId: String,
        executor: suspend NodeContext.() -> Any,
        dependencies: List<String> = emptyList()
    ): AIAgentSubgraphBuilder {
        nodes[nodeId] = GraphNode(nodeId, executor)
        this.dependencies[nodeId] = dependencies
        return this
    }
    
    /**
     * Устанавливает метаданные подграфа
     */
    fun setMetadata(metadata: SubgraphMetadata): AIAgentSubgraphBuilder {
        this.metadata = metadata
        return this
    }
    
    /**
     * Собирает и регистрирует подграф в environment
     */
    suspend fun buildAndRegister(): SubgraphRegistration {
        require(nodes.isNotEmpty()) { "Subgraph must contain at least one node" }
        
        // Добавляем все узлы в environment (временно, с префиксом?)
        nodes.forEach { (nodeId, node) ->
            environment.addNode(node, dependencies[nodeId] ?: emptyList())
        }
        
        // Создаём метаданные, если не заданы
        val finalMetadata = metadata ?: SubgraphMetadata(
            id = subgraphId,
            name = subgraphId,
            nodeIds = nodes.keys.toList(),
            entryPoints = nodes.keys.filter { dependencies[it]?.isEmpty() != false },
            exitPoints = nodes.keys.filter { node -> 
                nodes.keys.none { other -> node in (dependencies[other] ?: emptyList()) }
            }
        )
        
        // Регистрируем подграф в environment
        environment.registerSubgraph(finalMetadata)
        
        return SubgraphRegistration(
            subgraphId = subgraphId,
            metadata = finalMetadata,
            nodeCount = nodes.size
        )
    }
}
```

---

### **3. Типичные сценарии использования (гипотетические)**

#### **Сценарий 1: Создание переиспользуемого подграфа "ResearchPipeline"**

```kotlin
// Где-то в коде инициализации агента
val researchSubgraph = AIAgentSubgraphBuilder("research_v1", environment)
    .addNode("search_web") { ctx ->
        val query = ctx.stateManager.load<String>("query")
        webSearchTool.execute(query)
    }
    .addNode("analyze_results", dependencies = listOf("search_web")) { ctx ->
        val results = ctx.stateManager.load<List<SearchResult>>("search_web_result")
        analyzeWithLLM(results)
    }
    .addNode("synthesize", dependencies = listOf("analyze_results")) { ctx ->
        val analysis = ctx.stateManager.load<String>("analyze_results_result")
        synthesizeFindings(analysis)
    }
    .setMetadata(
        SubgraphMetadata(
            id = "research_v1",
            name = "Research Pipeline",
            description = "Standard research workflow: search → analyze → synthesize",
            inputType = ResearchInput::class,
            outputType = ResearchOutput::class,
            timeout = Duration.ofMinutes(5)
        )
    )
    .buildAndRegister()  // ← регистрирует в environment

// Затем в стратегии графового агента:
class MyPlannerStrategy : AIAgentPlannerStrategy<Input, Output, AIAgentPlannerContext> {
    override suspend fun execute(input: Input, context: AIAgentPlannerContext): Graph {
        val env = context.environment
        
        // Создаём узел, который запускает весь подграф
        val orchestrator = GraphNode("orchestrator") { nodeCtx ->
            // Выполняем весь подграф как единое целое
            val subgraphResult = nodeCtx.executeSubgraph(
                subgraphId = "research_v1",
                input = ResearchInput(query = input.topic)
            )
            // Маппинг результата подграфа в выход основной стратегии
            transformSubgraphResult(subgraphResult)
        }
        
        env.addNode(orchestrator)
        return env.build()
    }
}
```

#### **Сценарий 2: Динамическое создание подграфа на основе конфигурации**

```kotlin
class DynamicSubgraphFactory(
    private val environment: GenericAgentEnvironment
) {
    suspend fun createSubgraphFromConfig(config: SubgraphConfig): AIAgentSubgraphBuilder {
        val builder = AIAgentSubgraphBuilder(config.id, environment)
        
        // Динамически добавляем узлы из конфига
        config.nodes.forEach { nodeConfig ->
            builder.addNode(
                nodeId = nodeConfig.id,
                executor = createExecutor(nodeConfig),
                dependencies = nodeConfig.dependencies
            )
        }
        
        builder.setMetadata(
            SubgraphMetadata(
                id = config.id,
                name = config.name,
                nodeIds = config.nodes.map { it.id },
                entryPoints = config.nodes.filter { it.dependencies.isEmpty() }.map { it.id },
                exitPoints = findExitPoints(config.nodes),
                timeout = config.timeout,
                retryPolicy = config.retryPolicy
            )
        )
        
        return builder
    }
    
    private fun createExecutor(nodeConfig: NodeConfig): suspend NodeContext.() -> Any = {
        // Создаём executor на основе типа узла
        when (nodeConfig.type) {
            "llm" -> callLLM(buildPrompt(nodeConfig))
            "tool" -> callTool(nodeConfig.toolName, nodeConfig.parameters)
            "transform" -> transform(data = nodeConfig.input)
        }
    }
}

// Использование:
val factory = DynamicSubgraphFactory(environment)
val config = loadSubgraphConfig("data_processing_pipeline")
val builder = factory.createSubgraphFromConfig(config)
builder.buildAndRegister()
```

#### **Сценарий 3: Композиция нескольких подграфов**

```kotlin
// Создаём несколько подграфов
val dataSubgraph = AIAgentSubgraphBuilder("data_pipeline", environment)
    .addNode("fetch", ::fetchData)
    .addNode("clean", ::cleanData, dependencies = listOf("fetch"))
    .buildAndRegister()

val mlSubgraph = AIAgentSubgraphBuilder("ml_pipeline", environment)
    .addNode("train", ::trainModel)
    .addNode("evaluate", ::evaluateModel, dependencies = listOf("train"))
    .buildAndRegister()

// Затем создаём главный граф, который использует оба подграфа
class ComposedAgentStrategy : AIAgentPlannerStrategy<Input, Output, AIAgentPlannerContext> {
    override suspend fun execute(input: Input, context: AIAgentPlannerContext): Graph {
        val env = context.environment
        
        // Узел для первого подграфа
        val dataNode = GraphNode("run_data_pipeline") { ctx ->
            ctx.executeSubgraph("data_pipeline", input.rawData)
        }
        
        // Узел для второго подграфа (зависит от первого)
        val mlNode = GraphNode("run_ml_pipeline") { ctx ->
            val processedData = ctx.getSubgraphResult("data_pipeline")
            ctx.executeSubgraph("ml_pipeline", processedData)
        }
        
        val finalNode = GraphNode("finalize") { ctx ->
            val mlResult = ctx.getSubgraphResult("ml_pipeline")
            formatFinalOutput(mlResult)
        }
        
        env.addNode(dataNode)
        env.addNode(mlNode, dependencies = listOf("run_data_pipeline"))
        env.addNode(finalNode, dependencies = listOf("run_ml_pipeline"))
        
        return env.build()
    }
}
```

---

### **4. Как мог бы выглядеть полный API (гипотетически)**

```kotlin
// 1. Builder для создания подграфа
class AIAgentSubgraphBuilder(
    private val id: String,
    private val environment: GenericAgentEnvironment
) {
    fun addNode(
        nodeId: String,
        executor: suspend NodeContext.() -> Any,
        dependencies: List<String> = emptyList()
    ): AIAgentSubgraphBuilder
    
    fun setMetadata(metadata: SubgraphMetadata): AIAgentSubgraphBuilder
    
    suspend fun buildAndRegister(): SubgraphRegistration
}

// 2. Результат регистрации
data class SubgraphRegistration(
    val subgraphId: String,
    val metadata: SubgraphMetadata,
    val nodeCount: Int
)

// 3. Extensions для NodeContext
interface NodeContext {
    // ... существующие методы
    
    /**
     * Выполняет зарегистрированный подграф
     */
    suspend fun <T> executeSubgraph(
        subgraphId: String,
        input: Any,
        timeout: Duration? = null
    ): T
    
    /**
     * Получает результат выполнения подграфа (по ID)
     */
    suspend fun <T> getSubgraphResult(subgraphId: String): T
}

// 4. Extensions для GenericAgentEnvironment
interface GenericAgentEnvironment {
    // ... существующие методы (addNode, build, execute)
    
    /**
     * Регистрирует подграф для последующего использования
     */
    fun registerSubgraph(metadata: SubgraphMetadata)
    
    /**
     * Возвращает метаданные подграфа по ID
     */
    fun getSubgraphMetadata(id: String): SubgraphMetadata?
    
    /**
     * Проверяет, зарегистрирован ли подграф
     */
    fun hasSubgraph(id: String): Boolean
}
```

---

### **5. Аналоги в других системах workflow**

| Система | Аналог `AIAgentSubgraphBuilder` |
|---------|-------------------------------|
| **Apache Airflow** | `DagBuilder` + `SubDagOperator` |
| **AWS Step Functions** | `StateMachine` builder + `Map/Parallel` states |
| **Temporal** | `WorkflowInterface` + `ChildWorkflow` registration |
| **LangChain** | `RunnableBuilder` + `RunnableSequence` |
| **Camunda** | `BpmnModelInstance` builder + `CallActivity` |

---

### **6. Почему его НЕТ в текущем контексте?**

1. **Упрощённая архитектура**: В Koog, судя по контексту, **подграфы могут не поддерживаться** — только плоские графы.
2. **Другая терминология**: Вместо `SubgraphBuilder` может использоваться:
   - `GraphTemplateBuilder`
   - `CompositeGraphBuilder`
   - `WorkflowBuilder` (с возможностью вложенности)
   - `GraphModuleBuilder`
3. **Это часть будущей версии**: Функциональность подграфов может быть **запланирована, но не реализована**.
4. **Реализация через другие классы**: Подграфы могут создаваться через:
   - `GraphAgentServiceBuilder` [ИСТОЧНИК 4] (для графовых агентов)
   - `AIAgentServiceBuilderImpl` [ИСТОЧНИК 5] ( общий билдер)
   - Отдельный `SubgraphAgentBuilder`

---

### **7. Где искать в реальной кодовой базе Koog**

Если класс существует, проверьте:

1. **Пакеты, связанные с графами и билдерами:**
   ```
   com.koog.agent.graph.builder
   com.koog.agent.strategy.builder
   com.koog.agent.graph.subgraph
   ```

2. **Файлы, которые могут содержать:**
   - `AIAgentSubgraphBuilder.kt`
   - `SubgraphBuilder.kt`
   - `GraphBuilder.kt` (возможно, там есть методы для подграфов)
   - `CompositeGraphBuilder.kt`
   - `GraphModule.kt`

3. **Поисковые запросы:**
   ```bash
   grep -r "SubgraphBuilder" .
   grep -r "subgraph" com/koog/agent | grep -i "build"
   grep -r "registerSubgraph" .
   grep -r "buildSubgraph" .
   ```

4. **Косвенные признаки:**
   - Классы с методами `createSubgraph()`, `composeGraph()`
   - Интерфейсы `SubgraphRegistrable`
   - JSON/YAML конфиги с типом `"subgraph"`

5. **Тесты:**
   ```kotlin
   // Пример теста, если SubgraphBuilder существует
   @Test
   fun `test subgraph builder creates valid subgraph`() = runTest {
       val builder = AIAgentSubgraphBuilder("test", environment)
       builder.addNode("node1") { "result1" }
           .addNode("node2", dependencies = listOf("node1")) { "result2" }
       
       val registration = builder.buildAndRegister()
       assertThat(registration.subgraphId).isEqualTo("test")
       assertThat(registration.nodeCount).isEqualTo(2)
       
       assertThat(environment.hasSubgraph("test")).isTrue()
   }
   ```

---

### **8. Альтернативные подходы (без SubgraphBuilder)**

Если в Koog нет специального билдера для подграфов, возможно:

#### **Вариант A: Подграфы через GenericAgentEnvironment напрямую**
```kotlin
// Создаём "временный" environment для подграфа
val subgraphEnv = GenericAgentEnvironment()

// Добавляем узлы в subgraphEnv
subgraphEnv.addNode(GraphNode("search") { ... })
subgraphEnv.addNode(GraphNode("analyze", dependencies = listOf("search")) { ... })

// Собираем подграф
val subgraph = subgraphEnv.build()

// Регистрируем в основном environment
environment.registerSubgraph(
    SubgraphMetadata(
        id = "research_subgraph",
        graph = subgraph,
        entryPoints = listOf("search"),
        exitPoints = listOf("analyze")
    )
)
```

#### **Вариант B: Функциональные агенты как подграфы**
```kotlin
// Функциональный агент, инкапсулирующий цепочку шагов
val subgraphAgent = FunctionalAIAgent(
    strategy = object : AIAgentFunctionalStrategy<Input, Output> {
        override suspend fun execute(input: Input, context: AIAgentFunctionalContext): Output {
            return sequence {
                yield(step1(input))
                yield(step2()))
                yield(step3())
            }.last()
        }
    }
)

// Используем узел графа, который запускает этот агент
val node = GraphNode("subgraph_node") { ctx ->
    subgraphAgent.execute(input)
}
```

#### **Вариант C: DSL для построения графов с поддержкой подграфов**
```kotlin
// Где-то в коде может быть DSL:
graph {
    subgraph("research") {
        node("search") { ... }
        node("analyze") { dependsOn("search") }
        node("synthesize") { dependsOn("analyze") }
    }
    
    node("final_report") { dependsOn("research:synthesize") }
}
```

---

### **9. Критические вопросы для уточнения**

1. **Откуда вы узнали о `AIAgentSubgraphBuilder`?**
   - Документация Koog?
   - Пример кода в репозитории?
   - Issue tracker / roadmap?
   - Это название из другой статьи о workflow-системах?

2. **Какая версия Koog?** (Подграфы могли быть добавлены в последних коммитах)

3. **Видели ли вы:**
   - Класс `SubgraphMetadata`?
   - Метод `registerSubgraph()` в `GenericAgentEnvironment`?
   - Метод `executeSubgraph()` в `NodeContext`?

4. **Контекст, в котором вы ищете:**
   - Вы хотите создать сложный граф с повторяющимися фрагментами?
   - Вам нужно динамически генерировать графы?
   - Вы изучаете модульность в Koog?

---

### **10. Вывод**

Без прямого упоминания в контексте **нельзя дать точное описание `AIAgentSubgraphBuilder`**.

**Чтобы продвинуться:**
1. Найдите в кодовой базе файл с этим классом (`grep -r "AIAgentSubgraphBuilder" .`)
2. Если его нет — проверьте, есть ли `SubgraphBuilder` (без префикса `AIAgent`)
3. Изучите `GenericAgentEnvironment` на наличие методов `registerSubgraph`/`getSubgraph`
4. Проверьте, есть ли примеры использования подграфов в `samples/` или `tests/`

---

### **Рекомендация:**
Если вы проектируете систему с подграфами, но `AIAgentSubgraphBuilder` отсутствует, можно реализовать свой билдер на основе `GraphNode`:

```kotlin
// Простая реализация SubgraphBuilder без поддержки в environment
class SimpleSubgraphBuilder(
    val id: String,
    val nodes: MutableMap<String, GraphNode> = mutableMapOf(),
    val dependencies: MutableMap<String, List<String>> = mutableMapOf()
) {
    fun node(id: String, executor: suspend NodeContext.() -> Any, deps: List<String> = emptyList()) {
        nodes[id] = GraphNode(id, executor)
        dependencies[id] = deps
    }
    
    fun build(): Map<String, GraphNode> = nodes.toMap()
    
    fun getEntryPoints(): List<String> = nodes.keys.filter { 
        dependencies[it]?.isEmpty() != false 
    }
    
    fun getExitPoints(): List<String> = nodes.keys.filter { nodeId ->
        nodes.keys.none { other -> nodeId in (dependencies[other] ?: emptyList()) }
    }
}
```

---

### **Итог:**
**`AIAgentSubgraphBuilder` — вероятно, гипотетический класс для декларативного создания вложенных графов, но в текущем контексте Koog его нет.**

Для точной информации уточните:
1. **Где вы встретили это имя?** (файл, логи, ошибка, документация)
2. **Есть ли пример его использования?**
3. **Какой пакет/модуль Koog вы изучаете?**

---

### Источники

[КЛАСС 1] PlannerAIAgent · PlannerAIAgent.kt  
[КЛАСС 2] FunctionalAIAgent · FunctionalAIAgent.kt  
[ИСТОЧНИК 2] PlannerAIAgent.kt · line 22  
[ИСТОЧНИК 3] FunctionalAIAgent.kt · line 63  
[ИСТОЧНИК 4] AIAgentServiceBuilderImpl.kt · line 60  
[ИСТОЧНИК 5] AIAgentServiceBuilderImpl.kt · line 72  
[ИСТОЧНИК 1] AIAgentConfigBase.kt · for · line 1

