# Этап 3: ChatType.SUPPORT — инфраструктура UI, детальный план

## Контекст этапа

Добавляем новый тип чата SUPPORT по точному образцу существующих типов.
Ближайший аналог: `ASSISTANT` (нет LTM, нет артефактов, есть ConsumptionCalculator) + `PLANNER` (есть handleAction для кнопок).

Зависимость от Этапа 2: `@Named("task") TaskWorker` уже в DI-графе, `StateStoreImpl` и `StateMemoryProviderFactory` готовы к переиспользованию.

**Важно:** После этапа 3 проект **не компилируется** — `ConsoleFeatureDeps` потребует `@Named("support") TaskWorker`, который появится только в Этапе 4 (`SupportStateMachineModule`). Этапы 3 и 4 нужно завершить прежде чем делать полную сборку.

После этапа (вместе с Этапом 4): можно создать чат типа SUPPORT и писать в него сообщения.
Support State Machine (Этап 4) подключается через `SupportStateMachineModule` — делегат уже готов принять Worker с любым StateConfig.

---

## Что получим (критерии успеха)

- [ ] Проект собирается без ошибок
- [ ] В `ChatsScreen` отображается SUPPORT-чат (нет крашей при переходе)
- [ ] Сообщения в SUPPORT-чат доходят до `SupportTalkDelegate.tryAddUserMessage()`
- [ ] Tool call события отображаются как INFO-сообщения в чате
- [ ] ConsumptionCalculator получает события от воркера

---

## Изменение 1: `ChatType.kt`

**Файл:** `core/core_features/chat/domain/model/ChatType.kt`

Добавить новое значение в конец enum:
```
SUPPORT("support", "User Support")
```

После этого компилятор укажет на все `when(chatType)` без `else` — это поможет найти все места где нужны правки.

---

## Изменение 2: `ConsoleFeatureDeps.kt` — добавить `supportWorker`

**Файл:** `features/console/impl/di/ConsoleFeatureDeps.kt`

Добавить два изменения:

**2а.** К существующему `taskWorker` добавить qualifier (изменение от Этапа 2):
```
@get:Named("task")
val taskWorker: TaskWorker
```

**2б.** Добавить новое поле:
```
@get:Named("support")
val supportWorker: TaskWorker
```

Добавить импорт `javax.inject.Named`.

**Почему `@get:Named` на интерфейсном свойстве:** `AppComponent` реализует `ConsoleFeatureDeps` как Dagger component dependency. Аннотация `@get:Named` на геттере сообщает Dagger какой именно binding использовать при сатисфакции этого свойства из AppComponent.

---

## Новый файл 3: `SupportConsoleFeatureEntry.kt`

**Расположение:** `features/console/api/SupportConsoleFeatureEntry.kt`

Точная копия `PlannerConsoleFeatureEntry.kt` с заменой имени:

```
interface SupportConsoleFeatureEntry {
    @Composable
    fun EntryPoint(chatId: Long, modifier: Modifier)
}
```

---

## Новый файл 4: `SupportTalkDelegate.kt`

**Расположение:** `features/console/impl/ui/delegates/SupportTalkDelegate.kt`

**Паттерн:** `AssistantTalkDelegate` (общая структура) + кусок из `PlannerTalkDelegate` (`handleAction` и `formatToolResult`).

**Конструктор:**
```
class SupportTalkDelegate(
    private val addChatMessageUseCase: AddChatMessageUseCase,
    private val supportWorker: TaskWorker,
    private val chatTools: ChatTools,
    private val consumptionCalculator: ConsumptionCalculator,
    private val json: Json
) : TalkDelegate
```

Аннотация `@Inject` НЕ нужна — делегат создаётся через `@Provides` в `ConsoleFeatureModule`.

**`tryAddUserMessage()`:**
1. Вызвать `addChatMessageUseCase(chatId, timestamp, UserType.User, text, Viewed, ChatMessage.Type.User)`
2. Вызвать `onSuccess()`
3. `try { supportWorker.doWork(userPrompt = inputText, chat = chat, onEvent = ::handleWorkerEvent) }`
4. `catch (e: Throwable) { chatTools.addInfoMessage(chat.id, e.stackTraceToString()) }`
5. Вернуть `null`

**`tryHandleAction()`:**
```
try {
    supportWorker.handleAction(chat, action) { event -> handleWorkerEvent(event, chat.id) }
} catch (e: Throwable) {
    chatTools.addInfoMessage(chat.id, "Action error: ${e.message}", emptyList())
}
```

**`getPlannerEvents()`:**
```
override fun <T> getPlannerEvents(): SharedFlow<T>? = null
```

**`handleWorkerEvent(event: WorkerEvent, chatId: Long)` — private:**
```
when (event) {
    is WorkerEvent.ToolCallStarted -> {
        chatTools.addInfoMessage(chatId, "MCP tool: ${event.toolName}")
    }
    is WorkerEvent.ToolCallFinished -> {
        val status = if (event.isError) "error" else "ok"
        chatTools.addInfoMessage(chatId, "MCP result ($status): ${event.toolName}\n${formatToolResult(event.result)}")
    }
    is WorkerEvent.RequestSuccess -> {
        consumptionCalculator.onWorkerEvent(chat, event)
    }
    else -> Unit
}
```

**Проблема с `chat` в RequestSuccess:** `handleWorkerEvent` принимает `chatId: Long`, но `consumptionCalculator.onWorkerEvent` требует `Chat`. Решение: сохранять `chat` как поле экземпляра перед вызовом `doWork`, или передавать `Chat` в `handleWorkerEvent`. Использовать второй вариант — передавать `Chat` параметром в `handleWorkerEvent(event, chat)` и внутри брать `chat.id` для chatTools.

**`formatToolResult(raw: String)` — private:**
Точная копия из `PlannerTalkDelegate` (парсинг JSON для красивого вывода).

---

## Изменение 5: `ConsoleViewModelImpl.kt` — добавить `SupportFactory`

**Файл:** `features/console/impl/ui/viewmodel/ConsoleViewModelImpl.kt`

Добавить класс после `AssistantFactory`. Точная копия `AssistantFactory` с заменой типа делегата:

```
class SupportFactory @Inject constructor(
    private val getMessagesUseCase: GetChatMessagesAsFlowUseCase,
    private val clearUnviewedUseCase: ClearChatNotViewedMessageUseCase,
    private val talkDelegate: SupportTalkDelegate,
    private val getChatByIdAsFlowUseCase: GetChatByIdAsFlowUseCase,
    private val updateChatSettingsUseCase: UpdateChatSettingsUseCase,
    private val updateChatTitleUseCase: UpdateChatTitleUseCase,
    private val createPlannerStageChatUseCase: CreatePlannerStageChatUseCase,
    private val handleMessageButtonClickUseCase: HandleMessageButtonClickUseCase,
    private val getPrHandleStateUseCase: GetPrHandleStateUseCase,
    private val setPrHandleEnabledUseCase: SetPrHandleEnabledUseCase,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val chatId = extras[CHAT_ID_KEY] ?: error("ID not found in extras")
        return ConsoleViewModelImpl(
            getMessagesUseCase, clearUnviewedUseCase, talkDelegate,
            getChatByIdAsFlowUseCase, updateChatSettingsUseCase, updateChatTitleUseCase,
            createPlannerStageChatUseCase, handleMessageButtonClickUseCase,
            getLtmByGroupUseCase = null,
            artifactRepository = null,
            chatId = chatId,
            getPrHandleStateUseCase = getPrHandleStateUseCase,
            setPrHandleEnabledUseCase = setPrHandleEnabledUseCase
        ) as T
    }
}
```

---

## Изменение 6: `ConsoleFeatureComponent.kt` — добавить Support-методы

**Файл:** `features/console/impl/di/ConsoleFeatureComponent.kt`

Добавить два метода по образцу существующих:
```
fun getSupportViewModelFactory(): ConsoleViewModelImpl.SupportFactory
fun getSupportTalkDelegate(): SupportTalkDelegate
```

Добавить импорт `SupportTalkDelegate`.

---

## Изменение 7: `ConsoleFeatureModule.kt` — добавить провайдер `SupportTalkDelegate`

**Файл:** `features/console/impl/di/ConsoleFeatureModule.kt`

Добавить метод по образцу `provideAssistantTalkDelegate()`:

```
@Provides
fun provideSupportTalkDelegate(deps: ConsoleFeatureDeps): SupportTalkDelegate {
    return SupportTalkDelegate(
        addChatMessageUseCase = deps.addChatMessageUseCase,
        supportWorker = deps.supportWorker,
        chatTools = deps.chatTools,
        consumptionCalculator = deps.consuption,
        json = deps.json
    )
}
```

---

## Новый файл 8: `SupportConsoleFeatureEntryImpl.kt`

**Расположение:** `features/console/impl/SupportConsoleFeatureEntryImpl.kt`

Точная копия `PlannerConsoleFeatureEntryImpl.kt` с тремя заменами:
- Имя класса: `SupportConsoleFeatureEntryImpl`
- Реализуемый интерфейс: `SupportConsoleFeatureEntry`
- Ключ ViewModel: `"${ConsoleViewModelImpl::class.qualifiedName}_support_$chatId"`
- Фабрика: `featureComponent.getSupportViewModelFactory()`

```
class SupportConsoleFeatureEntryImpl @Inject constructor(): SupportConsoleFeatureEntry {
    @Composable
    override fun EntryPoint(chatId: Long, modifier: Modifier) {
        val appComponent = LocalAppComponent.current
        val featureComponent: ConsoleFeatureComponent = retain {
            DaggerConsoleFeatureComponent.factory().create(appComponent)
        }
        val extras = remember(chatId) {
            MutableCreationExtras().apply { set(ConsoleViewModelImpl.CHAT_ID_KEY, chatId) }
        }
        val viewModel: ConsoleViewModelImpl = viewModel(
            key = "${ConsoleViewModelImpl::class.qualifiedName}_support_$chatId",
            factory = featureComponent.getSupportViewModelFactory(),
            extras = extras
        )
        ConsoleScreen(viewModel = viewModel, modifier = modifier)
    }
}
```

---

## Изменение 9: `ConsoleFeatureApiModule.kt` — зарегистрировать `SupportConsoleFeatureEntryImpl`

**Файл:** `features/console/impl/di/ConsoleFeatureApiModule.kt`

Добавить биндинг по образцу:
```
@Binds
fun bindSupportFeatureEntry(impl: SupportConsoleFeatureEntryImpl): SupportConsoleFeatureEntry
```

Добавить импорты `SupportConsoleFeatureEntry`, `SupportConsoleFeatureEntryImpl`.

---

## Изменение 10: `FeatureEntryProvider.kt` — добавить геттер

**Файл:** `core/feature_entries/FeatureEntryProvider.kt`

Добавить метод по образцу:
```
@Stable
fun getSupportConsoleFeatureEntry(): SupportConsoleFeatureEntry
```

Добавить импорт `SupportConsoleFeatureEntry`.

---

## Изменение 11: `ChatsScreen.kt` — добавить SUPPORT case

**Файл:** `features/chats/impl/ui/ChatsScreen.kt`

**11а.** В блоке получения entry-points (рядом с `assistantChatEntry`):
```
val supportChatEntry = appComponent.getSupportConsoleFeatureEntry()
```

**11б.** В `when(chip.chatType)` добавить case (компилятор уже подскажет из-за шага 1):
```
ChatType.SUPPORT -> {
    supportChatEntry.EntryPoint(chatId = chip.id, modifier = Modifier.fillMaxSize())
}
```

---

## Порядок реализации (checklist)

> Шаги 1-2 первыми — иначе `@Named` на `taskWorker` вызовет ошибки компиляции до обновления модуля.

1. [ ] `ChatType.kt` — добавить `SUPPORT` (шаг 1)
2. [ ] `ConsoleFeatureDeps.kt` — добавить `@get:Named("task")` к `taskWorker`, добавить `supportWorker` (шаг 2)
3. [ ] `SupportConsoleFeatureEntry.kt` — создать интерфейс (шаг 3)
4. [ ] `SupportTalkDelegate.kt` — создать класс (шаг 4)
5. [ ] `ConsoleViewModelImpl.kt` — добавить `SupportFactory` (шаг 5)
6. [ ] `ConsoleFeatureComponent.kt` — добавить Support-методы (шаг 6)
7. [ ] `ConsoleFeatureModule.kt` — добавить провайдер (шаг 7)
8. [ ] `SupportConsoleFeatureEntryImpl.kt` — создать класс (шаг 8)
9. [ ] `ConsoleFeatureApiModule.kt` — добавить биндинг (шаг 9)
10. [ ] `FeatureEntryProvider.kt` — добавить геттер (шаг 10)
11. [ ] `ChatsScreen.kt` — добавить entry-point и case (шаг 11)
12. [ ] Полная сборка — убедиться что нет ошибок
13. [ ] Создать SUPPORT-чат, открыть, написать сообщение — убедиться что нет крашей

---

## Связь с другими этапами

- **Этап 2** должен быть завершён до этого этапа: `@Named("task")` и `@Named("support") TaskWorker` должны быть в DI-графе
- **Этап 4** (Support State Machine) подключается через `@Named("support") StateStore` с `SupportStateConfig` — `SupportTalkDelegate` и `TaskWorker` не меняются, меняется только StateConfig в связанном StateStore
- `SupportStateMachineModule` (создаётся в Этапе 4) предоставит `@Named("support") TaskWorker` — именно тот, что сейчас используется в `SupportTalkDelegate`

---

## Что НЕ меняется в этом этапе

- Логика `ConsoleViewModelImpl` (ViewModel) — без изменений
- Все существующие делегаты — без изменений
- `AgentCoreFeatureModule` / `TaskStateMachineModule` — без изменений
- `AssistantConsoleFeatureEntryImpl` — без изменений (аналог для справки)
