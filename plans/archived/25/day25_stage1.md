# Этап 1: Android — инфраструктура RAG-группы чатов

## Что решает этот этап
Создаёт полную инфраструктуру для нового типа чата `RAG_CONTEXT`:
новый ChatType → новый entry point → новый ViewModel factory → новый delegate → новый worker.
После этого этапа можно создать группу чатов типа "Code Rag Context", открыть чат и
получить ответы от LLM с ContextSummaryStrategy (сжатие истории).
AutoRag включён сразу — URL по умолчанию `http://10.0.2.2:3001`.

## Что получим в итоге
- Новый пункт в меню выбора типа группы: "Code Rag Context"
- Чат работает: пишешь вопрос — получаешь ответ с RAG-контекстом (если сервер запущен)
- История автоматически сжимается: хранится 4 последних сообщения, при 6+ — саммаризация

## Зависимости этапа
- Нет зависимостей от других этапов. Самостоятельный.

---

## Пошаговый план реализации

### Шаг 1. Добавить ChatType.RAG_CONTEXT

**Файл:** `app/.../chat/domain/model/ChatType.kt`

```kotlin
enum class ChatType(val dbType: String, val title: String) {
    SIMPLE_HISTORY("simple_history", "Simple History"),
    AGENT_COMMANDS("agent_commands", "Agent Commands"),
    PLANNER("planner", "Project Planner"),
    RAG_CONTEXT("rag_context", "Code Rag Context"),   // ← добавить
    ...
}
```

---

### Шаг 2. Создать RagConsoleFeatureEntry (api interface)

**Файл:** `app/.../features/console/api/RagConsoleFeatureEntry.kt`

```kotlin
package com.example.day.features.console.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface RagConsoleFeatureEntry {
    @Composable
    fun EntryPoint(chatId: Long, modifier: Modifier)
}
```

---

### Шаг 3. Создать RagWorker

**Файл:** `app/.../agent/domain/workers/concrete/RagWorker.kt`

Аналог TaskWorker, но гораздо проще — нет стейт-машины, нет handleAction.
Создаёт агента с:
- `AutoRagMemoryProvider` (через `memoryProviderFactory.create(listOf(MemoryType.AutoRag), agentId)`)
- `ContextSummaryStrategy` (хранит 4 последних, сжатие при 6+)

```kotlin
class RagWorker @Inject constructor(
    private val aiAgentFactory: AIAgentFactory,
    private val chatTools: ChatTools,
    private val memoryProviderFactory: MemoryProviderFactory,
    private val contextRepository: AgentContextRepository,
    private val llmRequestUseCase: LlmRequestUseCase,
    private val strategyFactory: StrategyFactory,
    private val toolProvider: ToolProvider,
    private val toolCallOrchestrator: ToolCallOrchestrator
) : AWorker {

    companion object {
        const val AGENT_NAME = "rag_context_agent"
        private const val MSG_LIMIT = 4      // хранить 4 последних сообщения
        private const val EXTRA_LIMIT = 2    // сжимать при 4+2=6 сообщениях

        private val SYSTEM_PROMPT = """
            Ты — ассистент по кодовой базе.
            При каждом ответе ОБЯЗАТЕЛЬНО указывай источники из предоставленного контекста.
            Формат источников: **Источники:** [список файлов/классов из контекста]
            Если контекст не предоставлен — отвечай на основе своих знаний, указав «Источники: нет контекста».
        """.trimIndent()
    }

    override suspend fun doWork(
        userPrompt: String,
        chat: Chat,
        userRole: AContextMessage.Role,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        val agent = aiAgentFactory.getOrCreate(
            systemName = AGENT_NAME,
            chatId = chat.id,
            systemPrompt = SYSTEM_PROMPT,
            defaultModel = { chat.settings.model },
            defaultContext = {
                // При первом создании агента: настроить стратегию саммаризации
                AContextDefaultFactory.createWithParams(
                    AContextParams.Summarization(msgLimit = MSG_LIMIT, extraLimit = EXTRA_LIMIT)
                )
            }
        )

        val memoryProvider = memoryProviderFactory.create(
            memoryTypes = listOf(MemoryType.AutoRag),
            agentId = agent.config.id
        )
        val strategy = strategyFactory.create(CtxStrategyType.SUMMARIZATION)

        val agentInstance = AIAgent(
            config = agent.config,
            contextRepository = contextRepository,
            llmProvider = llmRequestUseCase,
            strategy = strategy,
            memoryProvider = memoryProvider,
            toolProvider = toolProvider,
            orchestrator = toolCallOrchestrator
        )

        val result = agentInstance.process(
            prompt = AContextMessage(AContextMessage.Role.USER, userPrompt),
            onEvent = onEvent
        )

        result.fold(
            onSuccess = { agentResult ->
                chatTools.addBotMessage(chat.id, agentResult.responseText)
            },
            onFailure = { error ->
                chatTools.addBotMessage(chat.id, "❌ Ошибка: ${error.message}")
            }
        )
    }
}
```

**Важно:** Проверить существование `AContextDefaultFactory.createWithParams()`. Если нет — посмотреть как TaskWorker инициализирует `AContextParams.Summarization` в контексте через `contextRepository.saveContextParams()`. Адаптировать по аналогии.

---

### Шаг 4. Создать RagTalkDelegate

**Файл:** `app/.../features/console/impl/ui/delegates/RagTalkDelegate.kt`

Аналог `PlannerTalkDelegate` — без PlannerUiEvent, без handleAction (RAG-чат не имеет кнопок действий).

```kotlin
internal class RagTalkDelegate @Inject constructor(
    private val addChatMessageUseCase: AddChatMessageUseCase,
    private val ragWorker: RagWorker,
    private val chatTools: ChatTools,
) : TalkDelegate {

    override suspend fun tryAddUserMessage(
        chat: Chat,
        inputText: String,
        onSuccess: () -> Unit
    ) {
        addChatMessageUseCase.invoke(
            chatId = chat.id,
            timestamp = System.currentTimeMillis(),
            userType = UserType.User,
            text = inputText,
            status = ChatMessageStatus.Viewed,
            type = ChatMessage.Type.User
        )
        onSuccess.invoke()

        try {
            ragWorker.doWork(
                userPrompt = inputText,
                chat = chat,
                onEvent = null
            )
        } catch (e: Throwable) {
            chatTools.addInfoMessage(chat.id, "Ошибка: ${e.stackTraceToString()}")
        }
    }

    override suspend fun tryHandleAction(chat: Chat, messageId: Long, action: String) {
        // RAG-чат не использует action-кнопки
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getPlannerEvents(): SharedFlow<T>? = null
}
```

---

### Шаг 5. Добавить ConsoleViewModelImpl.RagFactory

**Файл:** `app/.../features/console/impl/ui/viewmodel/ConsoleViewModelImpl.kt`

Добавить новый inner class рядом с `PlannerFactory`:

```kotlin
class RagFactory @Inject constructor(
    private val getMessagesUseCase: GetChatMessagesAsFlowUseCase,
    private val clearUnviewedUseCase: ClearChatNotViewedMessageUseCase,
    private val talkDelegate: RagTalkDelegate,
    private val getChatByIdAsFlowUseCase: GetChatByIdAsFlowUseCase,
    private val updateChatSettingsUseCase: UpdateChatSettingsUseCase,
    private val updateChatTitleUseCase: UpdateChatTitleUseCase,
    private val createPlannerStageChatUseCase: CreatePlannerStageChatUseCase,
    private val handleMessageButtonClickUseCase: HandleMessageButtonClickUseCase,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val chatId = extras[CHAT_ID_KEY] ?: error("ID not found in extras")
        return ConsoleViewModelImpl(
            getMessagesUseCase,
            clearUnviewedUseCase,
            talkDelegate,
            getChatByIdAsFlowUseCase,
            updateChatSettingsUseCase,
            updateChatTitleUseCase,
            createPlannerStageChatUseCase,
            handleMessageButtonClickUseCase,
            getLtmByGroupUseCase = null,
            artifactRepository = null,
            chatId = chatId
        ) as T
    }
}
```

---

### Шаг 6. Расширить ConsoleFeatureComponent

**Файл:** `app/.../features/console/impl/di/ConsoleFeatureComponent.kt`

Добавить:
```kotlin
fun getRagViewModelFactory(): ConsoleViewModelImpl.RagFactory
fun getRagTalkDelegate(): RagTalkDelegate
```

---

### Шаг 7. Расширить ConsoleFeatureModule

**Файл:** `app/.../features/console/impl/di/ConsoleFeatureModule.kt`

Добавить provide-метод для `RagTalkDelegate` (по аналогии с `PlannerTalkDelegate`):

```kotlin
@Provides
fun provideRagTalkDelegate(deps: ConsoleFeatureDeps): RagTalkDelegate {
    return RagTalkDelegate(
        addChatMessageUseCase = deps.addChatMessageUseCase,
        ragWorker = deps.ragWorker,
        chatTools = deps.chatTools,
    )
}
```

---

### Шаг 8. Расширить ConsoleFeatureDeps

**Файл:** `app/.../features/console/impl/di/ConsoleFeatureDeps.kt`

Добавить в интерфейс:
```kotlin
val ragWorker: RagWorker
```

`RagWorker` инжектируется Dagger автоматически (все его зависимости уже в AppComponent).

---

### Шаг 9. Создать RagConsoleFeatureEntryImpl

**Файл:** `app/.../features/console/impl/RagConsoleFeatureEntryImpl.kt`

Точная копия `PlannerConsoleFeatureEntryImpl`, но использует `getRagViewModelFactory()`:

```kotlin
class RagConsoleFeatureEntryImpl @Inject constructor() : RagConsoleFeatureEntry {
    @Composable
    override fun EntryPoint(chatId: Long, modifier: Modifier) {
        val appComponent = LocalAppComponent.current
        val featureComponent: ConsoleFeatureComponent = retain {
            DaggerConsoleFeatureComponent.factory().create(appComponent)
        }
        val extras = remember(chatId) {
            MutableCreationExtras().apply {
                set(ConsoleViewModelImpl.CHAT_ID_KEY, chatId)
            }
        }
        val viewModel: ConsoleViewModelImpl = viewModel(
            key = "${ConsoleViewModelImpl::class.qualifiedName}_rag_$chatId",
            factory = featureComponent.getRagViewModelFactory(),
            extras = extras
        )
        ConsoleScreen(viewModel = viewModel, modifier = modifier)
    }
}
```

---

### Шаг 10. Добавить биндинг в ConsoleFeatureApiModule

**Файл:** `app/.../features/console/impl/di/ConsoleFeatureApiModule.kt`

```kotlin
@Binds
fun bindRagFeatureEntry(impl: RagConsoleFeatureEntryImpl): RagConsoleFeatureEntry
```

---

### Шаг 11. Добавить getRagConsoleFeatureEntry() в FeatureEntryProvider

**Файл:** `app/.../core/feature_entries/FeatureEntryProvider.kt`

```kotlin
@Stable
fun getRagConsoleFeatureEntry(): RagConsoleFeatureEntry
```

---

### Шаг 12. Добавить ChatType.RAG_CONTEXT в ChatsScreen

**Файл:** `app/.../features/chats/impl/ui/ChatsScreen.kt`

В `ChatsScreenInternal()`:
1. Получить entry: `val ragChatEntry = appComponent.getRagConsoleFeatureEntry()`
2. Добавить в `when(chip.chatType)`:
```kotlin
ChatType.RAG_CONTEXT -> {
    ragChatEntry.EntryPoint(chatId = chip.id, modifier = Modifier.fillMaxSize())
}
```

---

## Что проверить после реализации

1. Создать новую группу чатов типа "Code Rag Context"
2. Открыть чат — интерфейс такой же как у других типов
3. Написать сообщение — получить ответ от LLM
4. Написать 7+ сообщений — убедиться, что срабатывает саммаризация (в чате появится info-сообщение о сжатии)
5. Если rag-server запущен — ответ должен содержать "Источники:"

---

## Риски и нюансы

- `AContextDefaultFactory.createWithParams(AContextParams.Summarization(...))` — проверить существование этого метода, при необходимости адаптировать инициализацию контекстных параметров
- `RagWorker` добавляет зависимость `ToolProvider` и `ToolCallOrchestrator` — они уже в графе Dagger, но проверить что `ConsoleFeatureDeps` их не нужно явно указывать (инжектируются в RagWorker напрямую)
