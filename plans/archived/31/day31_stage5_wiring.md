# Stage 5: Full Wiring — EntryImpl + AssistantTalkDelegate + Factory + DI

## Описание
Завершить интеграцию: создать `AssistantConsoleFeatureEntryImpl`, `AssistantTalkDelegate`, `ConsoleViewModelImpl.AssistantFactory`, обновить все DI-связи и UI-роутинг. После этого этапа ASSISTANT-чат полностью работоспособен.

**Требует**: Stage 3 (ChatType, интерфейс) + Stage 4 (AssistantWorker).

## Файлы для создания/изменения

### 1. Новый файл: `AssistantConsoleFeatureEntryImpl.kt`
**Путь**: `app/src/main/java/com/example/day/features/console/impl/AssistantConsoleFeatureEntryImpl.kt`

- `class AssistantConsoleFeatureEntryImpl @Inject constructor() : AssistantConsoleFeatureEntry`
- Внутри `EntryPoint`:
  - `val appComponent = LocalAppComponent.current`
  - `val featureComponent: ConsoleFeatureComponent = retain { DaggerConsoleFeatureComponent.factory().create(appComponent) }`
  - `val extras = remember(chatId) { MutableCreationExtras().apply { set(ConsoleViewModelImpl.CHAT_ID_KEY, chatId) } }`
  - `val viewModel: ConsoleViewModelImpl = viewModel(key = "${ConsoleViewModelImpl::class.qualifiedName}_assistant_$chatId", factory = featureComponent.getAssistantViewModelFactory(), extras = extras)`
  - `ConsoleScreen(viewModel = viewModel, modifier = modifier)`
- Структура идентична `RagConsoleFeatureEntryImpl`

---

### 2. Новый файл: `AssistantTalkDelegate.kt`
**Путь**: `app/src/main/java/com/example/day/features/console/impl/ui/delegates/AssistantTalkDelegate.kt`

**Класс**: `internal class AssistantTalkDelegate @Inject constructor(...)`

**Constructor dependencies**:
- `addChatMessageUseCase: AddChatMessageUseCase`
- `assistantWorker: AssistantWorker`
- `chatTools: ChatTools`
- `consumptionCalculator: ConsumptionCalculator`

**Реализует**: `TalkDelegate`

**Метод `tryAddUserMessage(chat, inputText, onSuccess)`**:
1. Добавить сообщение пользователя через `addChatMessageUseCase` (идентично RagTalkDelegate: `chatId, timestamp, UserType.User, inputText, ChatMessageStatus.Viewed, ChatMessage.Type.User`)
2. Вызвать `onSuccess()`
3. `var lastAnswer: String? = null`
4. В `try/catch(Throwable)`:
   ```
   assistantWorker.doWork(
       userPrompt = inputText,
       chat = chat,
       // userRole не передаём — дефолт AContextMessage.Role.USER
       onEvent = { event ->
           if (event is WorkerEvent.RequestSuccess) {
               lastAnswer = event.result.choices.firstOrNull()?.message?.content
           }
           consumptionCalculator.onWorkerEvent(chat, event)
       }
   )
   ```
5. При catch: `chatTools.addInfoMessage(chat.id, "❌ ${e.stackTraceToString()}")`
6. Вернуть `lastAnswer`

**Метод `tryHandleAction(chat, messageId, action)`**: пустая реализация (no-op)

**Метод `getPlannerEvents()`**: `@Suppress("UNCHECKED_CAST") override fun <T> getPlannerEvents(): SharedFlow<T>? = null`

**Примечание**: `assistantWorker.doWork()` вызывается без `userRole` — в `AWorker` интерфейсе он имеет дефолт `AContextMessage.Role.USER`.

---

### 3. `ConsoleViewModelImpl.kt` — добавить `AssistantFactory`
**Путь**: `app/src/main/java/com/example/day/features/console/impl/ui/viewmodel/ConsoleViewModelImpl.kt`

Добавить inner class `AssistantFactory` по аналогии с `RagFactory`:

**`class AssistantFactory @Inject constructor(...)`**:
- `getMessagesUseCase: GetChatMessagesAsFlowUseCase`
- `clearUnviewedUseCase: ClearChatNotViewedMessageUseCase`
- `talkDelegate: AssistantTalkDelegate`
- `getChatByIdAsFlowUseCase: GetChatByIdAsFlowUseCase`
- `updateChatSettingsUseCase: UpdateChatSettingsUseCase`
- `updateChatTitleUseCase: UpdateChatTitleUseCase`
- `createPlannerStageChatUseCase: CreatePlannerStageChatUseCase`
- `handleMessageButtonClickUseCase: HandleMessageButtonClickUseCase`

**`create(modelClass, extras)`**:
```
val chatId = extras[CHAT_ID_KEY] ?: error("ID not found in extras")
return ConsoleViewModelImpl(
    getMessagesUseCase, clearUnviewedUseCase, talkDelegate,
    getChatByIdAsFlowUseCase, updateChatSettingsUseCase, updateChatTitleUseCase,
    createPlannerStageChatUseCase, handleMessageButtonClickUseCase,
    getLtmByGroupUseCase = null, artifactRepository = null,
    chatId = chatId
) as T
```

---

### 4. `ConsoleFeatureComponent.kt` — добавить метод
**Путь**: `app/src/main/java/com/example/day/features/console/impl/di/ConsoleFeatureComponent.kt`

Добавить в интерфейс:
```
fun getAssistantViewModelFactory(): ConsoleViewModelImpl.AssistantFactory
```

---

### 5. `ConsoleFeatureModule.kt` — добавить provide
**Путь**: `app/src/main/java/com/example/day/features/console/impl/di/ConsoleFeatureModule.kt`

Добавить метод:
```
@Provides
fun provideAssistantTalkDelegate(deps: ConsoleFeatureDeps): AssistantTalkDelegate {
    return AssistantTalkDelegate(
        addChatMessageUseCase = deps.addChatMessageUseCase,
        assistantWorker = deps.assistantWorker,
        chatTools = deps.chatTools,
        consumptionCalculator = deps.consuption,   // поле называется consuption (typo в оригинале)
    )
}
```

---

### 6. `ConsoleFeatureDeps.kt` — добавить AssistantWorker
**Путь**: `app/src/main/java/com/example/day/features/console/impl/di/ConsoleFeatureDeps.kt`

Добавить:
```
val assistantWorker: AssistantWorker
```

---

### 7. `ConsoleFeatureApiModule.kt` — добавить binding
**Путь**: `app/src/main/java/com/example/day/features/console/impl/di/ConsoleFeatureApiModule.kt`

Добавить:
```
@Binds
fun bindAssistantFeatureEntry(impl: AssistantConsoleFeatureEntryImpl): AssistantConsoleFeatureEntry
```

---

### 8. `FeatureEntryProvider.kt` — добавить метод
**Путь**: `app/src/main/java/com/example/day/core/feature_entries/FeatureEntryProvider.kt`

Добавить:
```
@Stable
fun getAssistantConsoleFeatureEntry(): AssistantConsoleFeatureEntry
```

---

### 9. `ChatsScreen.kt` — добавить роутинг
**Путь**: `app/src/main/java/com/example/day/features/chats/impl/ui/ChatsScreen.kt`

В `ChatsScreenInternal()`:
- Добавить: `val assistantChatEntry = appComponent.getAssistantConsoleFeatureEntry()`
- В `when(chip.chatType)` добавить ветку:
  ```
  ChatType.ASSISTANT -> {
      assistantChatEntry.EntryPoint(chatId = chip.id, modifier = Modifier.fillMaxSize())
  }
  ```

---

## Порядок применения изменений в Stage 5

Все изменения Stage 5 должны применяться вместе (один коммит или одна сборка):
1. `AssistantConsoleFeatureEntryImpl` — нужен для `ConsoleFeatureApiModule` binding
2. `ConsoleFeatureApiModule` — нужен для Dagger-binding `AssistantConsoleFeatureEntry`
3. `FeatureEntryProvider` — нужен AppComponent + ChatsScreen
4. Остальные — взаимно зависимы через Dagger

---

## Схема DI-цепочки (полная, Stage 3 + 4 + 5)

```
AppComponent (implements FeatureEntryProvider)
    ↓ getAssistantConsoleFeatureEntry()      ← ConsoleFeatureApiModule @Binds
        → AssistantConsoleFeatureEntryImpl   ← @Inject constructor()
            → retain(ConsoleFeatureComponent)
                → getAssistantViewModelFactory() → AssistantFactory @Inject
                    → AssistantTalkDelegate  ← ConsoleFeatureModule @Provides
                        → deps.assistantWorker    ← AssistantWorker @Inject (Stage 4)
                        → deps.consuption         ← уже есть в ConsoleFeatureDeps
```

---

## Резюме
**Что получим**: полностью рабочий ASSISTANT-чат:
- Обычные сообщения → агент с MCP-инструментами (git branch, файлы, github)
- `@@help <вопрос>` → агент с MCP + RAG-контекст из кодовой базы
- После каждого ответа LLM → info-сообщение со статистикой токенов

**Критерии успеха**:
- Приложение компилируется и запускается
- В GroupChoiceScreen виден тип "Dev Assistant"
- Можно создать группу и открыть чат типа ASSISTANT
- Обычный вопрос → ответ LLM (через MCP-инструменты если нужно)
- `@@help Как работает RagWorker` → ответ с цитатами из кодовой базы
- После каждого ответа — info-сообщение `📊 Расходы за текущий запрос / Всего за чат`
