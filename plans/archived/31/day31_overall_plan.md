# День 31. Ассистент разработчика — Уточнённый план

## Цель
Создать новый тип чата `ASSISTANT` — ассистент разработчика, который:
- Понимает кодовую базу через RAG (команда `@@help <вопрос>`)
- Работает с git через MCP (текущая ветка, список файлов, содержимое файлов)
- Отображает статистику потребления токенов

---

## Доработки RagServer

### Переработка `registerRagTools()`
**Новая сигнатура**:
- `server: Server`
- `buildPipeline: (PipelineConfig) -> PipelineExecutor`
- `sessionLogger: SessionLogger? = null`

**Что делает**:
- Регистрирует инструмент `search_codebase` (единственный активный)
- Логика инструмента: получает `query`, строит `PipelineExecutor` через переданный `buildPipeline(PipelineConfig())`, выполняет `pipeline.execute(query)`, возвращает `ContextFormatter.format(ctx.packed)` (или сообщение "НЕДОСТАТОЧНО_КОНТЕКСТА")
- Включает fallback: если `postRerankThreshold` зафильтровал всё — повтор без оптимизации
- Логирует через `sessionLogger`

**Комментируются объявления** (тела методов оставить, только `registerSearchCodebase`, `registerSearchCodebaseFixed`, `registerSearchCodebaseSmart`, `registerGetIndexStatus` убрать из вызываемых):
- `registerSearchCodebaseFixed` — закомментировать объявление функции
- `registerSearchCodebaseSmart` — закомментировать объявление функции
- `registerGetIndexStatus` — закомментировать объявление функции

**Вызов в `RagServer.kt`**:
- Обновить вызов `registerRagTools()` — передать лямбду `{ config -> buildPipeline(config) }` и `sessionLogger`

---

## Доработки McpServer

### Новый инструмент `get_current_git_branch`

**Новая env-переменная** `GIT_PROJECT_PATH`:
- Необязательна, дефолт — текущая директория (`"."`)
- Читается при старте сервера

**Инструмент** `get_current_git_branch`:
- Входные параметры: нет
- Описание: "Возвращает текущую git-ветку проекта"
- Реализация: `ProcessBuilder("git", "-C", projectPath, "branch", "--show-current")` → `stdout.trim()`
- При ошибке: возвращает `isError = true` с сообщением об ошибке

**Добавляется в `GitHubToolNames`**:
- `GET_CURRENT_GIT_BRANCH = "get_current_git_branch"`

**Регистрируется** в `registerMcpTools()`.

---

## Android: Инфраструктура нового типа чата

### ChatType
**Файл**: `core/core_features/chat/domain/model/ChatType.kt`
- Добавить значение: `ASSISTANT("assistant", "Dev Assistant")`

### ChatDatabase
**Файл**: `core/core_features/chat/data/local/ChatDatabase.kt`
- Инкремент версии: `16 → 17`

### AssistantConsoleFeatureEntry (новый интерфейс)
**Файл**: `features/console/api/AssistantConsoleFeatureEntry.kt`
- Метод: `EntryPoint(chatId: Long, modifier: Modifier)` — `@Composable`

### AssistantConsoleFeatureEntryImpl (новая реализация)
**Файл**: `features/console/impl/AssistantConsoleFeatureEntryImpl.kt`
- `@Inject constructor()`
- Внутри `EntryPoint`: `retain { DaggerConsoleFeatureComponent.factory().create(appComponent) }`
- Создаёт `ConsoleViewModelImpl` через `featureComponent.getAssistantViewModelFactory()`
- Ключ ViewModel: `"${ConsoleViewModelImpl::class.qualifiedName}_assistant_$chatId"`

### FeatureEntryProvider
**Файл**: `core/feature_entries/FeatureEntryProvider.kt`
- Добавить: `@Stable fun getAssistantConsoleFeatureEntry(): AssistantConsoleFeatureEntry`

### ConsoleFeatureApiModule
**Файл**: `features/console/impl/di/ConsoleFeatureApiModule.kt`
- Добавить `@Binds fun bindAssistantFeatureEntry(impl: AssistantConsoleFeatureEntryImpl): AssistantConsoleFeatureEntry`

### ConsoleFeatureComponent
**Файл**: `features/console/impl/di/ConsoleFeatureComponent.kt`
- Добавить: `fun getAssistantViewModelFactory(): ConsoleViewModelImpl.AssistantFactory`

### ChatsScreen
**Файл**: `features/chats/impl/ui/ChatsScreen.kt`
- В `ChatsScreenInternal()`: получить `assistantChatEntry = appComponent.getAssistantConsoleFeatureEntry()`
- В `when(chip.chatType)`: добавить ветку `ChatType.ASSISTANT → assistantChatEntry.EntryPoint(...)`

---

## Android: AssistantWorker

### AssistantWorker
**Файл**: `core/core_features/agent/domain/workers/concrete/AssistantWorker.kt`

**Зависимости (constructor inject)**:
- `AIAgentFactory`
- `ChatTools`
- `RagContextMemoryProviderFactory`
- `AgentContextRepository`
- `LlmRequestUseCase`
- `StrategyFactory`
- `ToolProvider`
- `ToolCallOrchestrator`
- `AgentMemoryRepository`
- `MemoryProviderFactory`

**Константы**:
- `AGENT_NAME = "assistant_agent"`
- `SYSTEM_PROMPT = "Твоя задача помогать пользователю разобраться с кодовой базой проекта..."`
- `MSG_LIMIT = 8`, `EXTRA_LIMIT = 8`
- `HELP_PREFIX = "@@help"`

**Метод `doWork(userPrompt, chat, userRole, onEvent)`**:
1. Определить `isHelpRequest = userPrompt.trimStart().startsWith(HELP_PREFIX, ignoreCase = true)`
2. Вычислить `cleanPrompt`: если `isHelpRequest` — стрипировать префикс `@@help` и пробелы; иначе — `userPrompt` как есть
3. Вызвать `aiAgentFactory.getOrCreate(AGENT_NAME, chat.id, systemPrompt = "", ..., onCreateCallback = { agentId → upsert system prompt через AgentSystemPromptMemoryProvider.MEMORY_KEY })` — allowedTools НЕ сохранять (все инструменты разрешены)
4. Построить `baseMemoryProvider = memoryProviderFactory.create(config.memoryTypes, agentId)`
5. Если `isHelpRequest`: создать `ragContextProvider = ragContextMemoryProviderFactory.create(agentId)`, итоговый `memoryProvider = CompositeMemoryProvider(listOf(baseMemoryProvider, ragContextProvider))`; иначе: `memoryProvider = baseMemoryProvider`
6. Построить `AIAgent` вручную (как RagWorker)
7. Вызвать `agent.process(AContextMessage(userRole, cleanPrompt), onEvent)`
8. При успехе: `chatTools.addBotMessage(...)`, если `isHelpRequest` — `ragContextProvider.postProcess()`
9. При ошибке: `chatTools.addBotMessage("❌ Ошибка: ...")`

---

## Android: AssistantTalkDelegate + ConsumptionCalculator

### AssistantTalkDelegate
**Файл**: `features/console/impl/ui/delegates/AssistantTalkDelegate.kt`
- `@Inject constructor(addChatMessageUseCase, assistantWorker, chatTools, consumptionCalculator)`
- Реализует `TalkDelegate`

**`tryAddUserMessage(chat, inputText, onSuccess)`**:
1. Добавить пользовательское сообщение через `addChatMessageUseCase`
2. Вызвать `onSuccess()`
3. Вызвать `assistantWorker.doWork(inputText, chat, onEvent = { event → consumptionCalculator.onWorkerEvent(chat, event) })`
4. Возвращать `lastAnswer` (из `WorkerEvent.RequestSuccess`)

**`tryHandleAction(...)`**: no-op

**`getPlannerEvents()`**: возвращает `null`

### ConsoleFeatureModule
**Файл**: `features/console/impl/di/ConsoleFeatureModule.kt`
- Добавить `@Provides fun provideAssistantTalkDelegate(deps): AssistantTalkDelegate`

### ConsoleFeatureDeps
**Файл**: `features/console/impl/di/ConsoleFeatureDeps.kt`
- Добавить: `val assistantWorker: AssistantWorker`

### ConsoleViewModelImpl.AssistantFactory
**Файл**: `features/console/impl/ui/viewmodel/ConsoleViewModelImpl.kt`
- Новый inner class `AssistantFactory @Inject constructor(...)` по аналогии с `RagFactory`
- Зависимости как у `RagFactory` но `talkDelegate: AssistantTalkDelegate`
- Создаёт `ConsoleViewModelImpl` с `getLtmByGroupUseCase = null`, `artifactRepository = null`

---

## Другие доработки

### RagContextMemoryProvider
**Файл**: `core/core_features/memory/domain/provider/RagContextMemoryProvider.kt`
- В методе `postProcess()`: добавить TODO-комментарий: `// TODO: Используется не там где надо. Перенести в AIAgent`

---

## Схема взаимодействия AssistantWorker

```
AssistantTalkDelegate.tryAddUserMessage(inputText)
  → addChatMessageUseCase (сохранить сообщение пользователя)
  → assistantWorker.doWork(inputText, chat, onEvent)
      → onEvent → consumptionCalculator.onWorkerEvent(chat, event)
      ├── isHelpRequest=true:
      │     aiAgentFactory.getOrCreate(onCreateCallback→saveSystemPrompt)
      │     baseMemoryProvider = memoryProviderFactory.create(...)
      │     ragContextProvider = ragContextMemoryProviderFactory.create(agentId)
      │     CompositeMemoryProvider([baseMemoryProvider, ragContextProvider])
      │     AIAgent.process(cleanPrompt)  → LLM + MCP tools + RAG tools
      │     ragContextProvider.postProcess()
      └── isHelpRequest=false:
            aiAgentFactory.getOrCreate(onCreateCallback→saveSystemPrompt)
            baseMemoryProvider = memoryProviderFactory.create(...)
            AIAgent.process(userPrompt)   → LLM + MCP tools (no RAG)
```

---

## Этапы реализации

1. **Stage 1**: Доработки RagServer — `search_codebase` через PipelineExecutor (идентично `/search`)
2. **Stage 2**: Доработки McpServer — `get_current_git_branch` через ProcessBuilder + `GIT_PROJECT_PATH`
3. **Stage 3**: Android задел — `ChatType.ASSISTANT` (enum), DB v17, интерфейс `AssistantConsoleFeatureEntry` (компилируется независимо)
4. **Stage 4**: `AssistantWorker` — логика `@@help`/RAG + MCP tools через `CompositeMemoryProvider`
5. **Stage 5**: Полный wiring — `AssistantConsoleFeatureEntryImpl`, `AssistantTalkDelegate`, `AssistantFactory`, DI-биндинги, `FeatureEntryProvider`, `ChatsScreen`

**Порядок зависимостей**: Stage 3 → Stage 4 → Stage 5 (все три применяются последовательно для Android-части). Stage 1 и Stage 2 независимы.
