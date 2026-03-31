# Stage 4: AssistantWorker — основная логика воркера

## Описание
Создать `AssistantWorker` — воркер нового типа чата. Ключевые особенности:
- Системный промпт настраивается через `onCreateCallback` (как в `JustWorkWorker`) — через `AgentSystemPromptMemoryProvider`
- MCP-инструменты доступны автоматически через `MemoryProviderFactory` (не ограничиваются — `allowedTools` не сохраняем)
- Команда `@@help <вопрос>`: активирует RAG-контекст через `RagContextMemoryProvider` поверх базового `memoryProvider`
- Без `@@help`: агент работает только с MCP-инструментами (git, файлы) без RAG
- Нет `saveTestResults()`, нет `getDebugInfo()` (в отличие от RagWorker)
- Доп. добавить TODO-комментарий в `RagContextMemoryProvider.postProcess()`

## Файлы для создания/изменения

### 1. Новый файл: `AssistantWorker.kt`
**Путь**: `app/src/main/java/com/example/day/core/core_features/agent/domain/workers/concrete/AssistantWorker.kt`

**Класс**: `class AssistantWorker @Inject constructor(...)`

**Constructor dependencies**:
- `aiAgentFactory: AIAgentFactory`
- `chatTools: ChatTools`
- `ragContextMemoryProviderFactory: RagContextMemoryProviderFactory`
- `contextRepository: AgentContextRepository`
- `llmRequestUseCase: LlmRequestUseCase`
- `strategyFactory: StrategyFactory`
- `toolProvider: ToolProvider`
- `toolCallOrchestrator: ToolCallOrchestrator`
- `agentMemoryRepository: AgentMemoryRepository`
- `memoryProviderFactory: MemoryProviderFactory`

**Реализует**: `AWorker`

**Companion object**:
- `AGENT_NAME = "assistant_agent"`
- `HELP_PREFIX = "@@help"`
- `MSG_LIMIT = 8`
- `EXTRA_LIMIT = 8`
- `SYSTEM_PROMPT = "Твоя задача помогать пользователю разобираться с кодовой базой проекта. Ты можешь использовать инструменты чтобы запрашивать контекст по вопросам пользователя у RAG базы данных по проекту, а также можешь использовать инструменты для работы с git"`

**Метод `doWork(userPrompt, chat, userRole, onEvent)`** (override):

1. Определить `isHelpRequest`:
   - `userPrompt.trimStart().startsWith(HELP_PREFIX, ignoreCase = true)`

2. Вычислить `cleanPrompt`:
   - Если `isHelpRequest`: взять `userPrompt.trimStart()`, убрать первые `HELP_PREFIX.length` символов, `trimStart()`
   - Иначе: `userPrompt`

3. Получить/создать агента:
   ```
   aiAgentFactory.getOrCreate(
       systemName = AGENT_NAME,
       chatId = chat.id,
       systemPrompt = "",   // пусто! настраивается через onCreateCallback
       defaultModel = { chat.settings.model },
       defaultContext = {
           AContext(
               params = AContextParams.Summarization(msgLimit = MSG_LIMIT, extraLimit = EXTRA_LIMIT),
               data = AContextState.Summary("", persistentListOf())
           )
       },
       onCreateCallback = { agentId ->
           agentMemoryRepository.upsertFact(
               agentId = agentId,
               memoryKey = AgentSystemPromptMemoryProvider.MEMORY_KEY,
               category = AgentSystemPromptMemoryProvider.CATEGORY,
               fact = SYSTEM_PROMPT
           )
           // allowedTools НЕ сохраняем — все зарегистрированные инструменты разрешены
       }
   )
   ```

4. Получить `agentId = baseAgent.config.id`

5. Построить `baseMemoryProvider`:
   ```
   memoryProviderFactory.create(
       memoryTypes = baseAgent.config.memoryTypes,
       agentId = agentId
   )
   ```
   **Важно**: `aiAgentFactory.getOrCreate()` внутри уже вызывает `memoryProviderFactory.create()` и делает `bindAgentId()` на синглтон-провайдерах (`AgentSystemPromptMemoryProvider`, `AgentToolsMemoryProvider`). Вызов `memoryProviderFactory.create()` здесь делает `bindAgentId()` повторно — но с тем же `agentId`, поэтому состояние консистентно. Этот паттерн аналогичен тому, как RagWorker обходит фабрику. В коде оставить комментарий: `// Строим AIAgent вручную (как RagWorker), чтобы подменить memoryProvider. Двойной bindAgentId на синглтонах — намеренно, agentId совпадает.`

6. Построить итоговый `memoryProvider`:
   - Если `isHelpRequest`:
     - `val ragContextProvider = ragContextMemoryProviderFactory.create(agentId)`
     - `memoryProvider = CompositeMemoryProvider(listOf(baseMemoryProvider, ragContextProvider))`
   - Иначе:
     - `memoryProvider = baseMemoryProvider`
     - `ragContextProvider = null`

7. Построить `strategy = strategyFactory.create(baseAgent.config.contextStrategyType)`

8. Построить `agent = AIAgent(config = baseAgent.config, contextRepository, llmRequestUseCase, strategy, memoryProvider, toolProvider, toolCallOrchestrator)`

9. Вызвать `agent.process(AContextMessage(userRole, cleanPrompt), onEvent)`

10. Обработать результат:
    - `onSuccess`: `chatTools.addBotMessage(chat.id, agentResult.responseText)` + если `ragContextProvider != null` → `ragContextProvider.postProcess()`
    - `onFailure`: `chatTools.addBotMessage(chat.id, "❌ Ошибка: ${error.message}")`

**Необходимые импорты**:
- `AIAgent`, `AIAgentFactory`, `AgentContextRepository`
- `AContext`, `AContextMessage`, `AContextParams`, `AContextState`
- `StrategyFactory`, `ToolCallOrchestrator`, `ToolProvider`
- `ChatTools`
- `RagContextMemoryProviderFactory`
- `AgentMemoryRepository`, `AgentSystemPromptMemoryProvider`
- `MemoryProviderFactory`, `CompositeMemoryProvider`
- `LlmRequestUseCase`
- `AWorker`, `WorkerEvent`
- `Chat`
- `kotlinx.collections.immutable.persistentListOf`

### 2. `RagContextMemoryProvider.kt`
**Путь**: `app/src/main/java/com/example/day/core/core_features/memory/domain/provider/RagContextMemoryProvider.kt`

В методе `postProcess()` добавить комментарий в начале тела:
```
// TODO: Используется не там где надо. Перенести в AIAgent
```

## Взаимодействие с другими классами

| Класс | Роль |
|-------|------|
| `AIAgentFactory.getOrCreate()` | Создание/получение конфига агента, запуск `onCreateCallback` при первом создании |
| `AgentMemoryRepository.upsertFact()` | Сохранение системного промпта в памяти агента |
| `AgentSystemPromptMemoryProvider` | Читает системный промпт из памяти и передаёт в контекст LLM |
| `AgentToolsMemoryProvider` | Нет ограничений — список tools не сохраняется, возвращает пустой список |
| `MemoryProviderFactory.create()` | Всегда включает AgentSystemPromptMemoryProvider + AgentToolsMemoryProvider + ToolCallHelperMemoryProvider |
| `RagContextMemoryProviderFactory.create(agentId)` | Создаёт RagContextMemoryProvider с привязкой к агенту (только для @@help) |
| `CompositeMemoryProvider` | Объединяет baseMemoryProvider + ragContextProvider (только для @@help) |
| `AIAgent.process()` | Выполняет запрос к LLM с учётом всех memory providers и доступных tools |

## Резюме
**Что получим**: `AssistantWorker` — полностью рабочий воркер, который:
- Обрабатывает обычные сообщения через LLM с MCP-инструментами
- При `@@help` — дополнительно обогащает запрос RAG-контекстом из кодовой базы

**Критерии успеха**:
- `AssistantWorker` компилируется
- Dagger может заинжектировать все зависимости (все они уже доступны в графе)
- При тестовом вызове с `@@help` в `onEvent` приходит `WorkerEvent.RequestSuccess`
- При обычном вызове агент использует только MCP-инструменты (нет обращений к RagServer)
