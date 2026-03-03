# Plan: Исправление отображения трёх видов памяти агентов

## Найденные проблемы

### [КРИТИЧНО] P0 — Память агентов не отображается корректно

#### Проблема 1: `updateMemoryInspectorState()` никогда не вызывается
Функция объявлена в `ConsoleViewModelImpl`, но не вызывается **нигде**:
- Не в `init {}`
- Не при изменении сообщений (onEach messages)
- Не при изменении данных чата (onEach chatData)
- Не при открытии инспектора

Результат: `_memoryInspectorState` всегда содержит пустой `MemoryInspectorUiModel()`.

#### Проблема 2: Long-Term Memory (LTM) не загружается
В `updateMemoryInspectorState()`:
```kotlin
// Long-term: placeholder - would need repository injection to get real data
val longTermFacts = emptyList<LongTermFactItem>()
```
`LongTermMemoryRepository` НЕ инжектируется в `ConsoleViewModelImpl` — ни через конструктор,
ни через фабрику. Поэтому LTM-слой **всегда пустой**.

#### Проблема 3: `isExpanded` toggle — no-op
В `ConsoleScreen.kt`:
```kotlin
MemoryInspectorView(
    uiModel = memoryInspectorState,
    onToggleExpand = { /* Toggle internal expansion */ }  // ← NO-OP!
)
```
Нажатие на заголовок инспектора ничего не делает. `isExpanded` по умолчанию `false`,
поэтому содержимое **никогда не видно** даже если данные были бы загружены.

---

### [ВАЖНО] P1 — Прочие баги

#### Проблема 4: Working Memory не реактивна
`updateMemoryInspectorState()` читает `chat?.workingSummary` из поля `chat: Chat?`.
Эта переменная обновляется в `onEach { chatData -> chat = chatData }`, но
`updateMemoryInspectorState()` не вызывается при обновлении, значит данные устаревают.

#### Проблема 5: Дублирование логики подтверждения создания этапа
В `ConsoleViewModelImpl` есть два места с логикой confirmStageCreation:
1. В `onEvent(ConfirmStageCreation)` — использует `mainChat.id` (chat из поля)
2. В `fun confirmStageCreation()` — использует `chatId` (из конструктора)
Оба метода делают одно и то же, но второй — публичный и дублирует логику.

#### Проблема 6: Stage-чаты в ChatsScreen не отличаются визуально
`ChatsViewModelImpl` показывает ВСЕ чаты группы (включая stage-подчаты) одинаково.
Для PLANNER-группы нужна визуальная иерархия: главный чат + отступ для этапов.

---

## План реализации

### Шаг 1: GetLongTermMemoryByGroupUseCase
**Файл:** `core/core_features/chat/domain/usecase/GetLongTermMemoryByGroupUseCase.kt`

Создать UseCase-обёртку для реактивного чтения LTM из репозитория.
```kotlin
class GetLongTermMemoryByGroupUseCase @Inject constructor(
    private val repository: LongTermMemoryRepository
) {
    operator fun invoke(groupId: Long): Flow<List<LongTermMemory>> =
        repository.getFactsByGroupFlow(groupId)
}
```
Dagger создаст его автоматически (зависимость `LongTermMemoryRepository` уже есть в `ConsoleFeatureDeps`).

---

### Шаг 2: Обновить ConsoleViewModel интерфейс
**Файл:** `ConsoleViewModel.kt`

Добавить события:
```kotlin
object OpenMemoryInspector : Event
object ToggleMemoryInspector : Event
```

---

### Шаг 3: Переработать ConsoleViewModelImpl
**Файл:** `ConsoleViewModelImpl.kt`

**3а) Добавить параметр в конструктор:**
```kotlin
private val getLtmByGroupUseCase: GetLongTermMemoryByGroupUseCase?,
```
Nullable — чтобы не ломать Factory и AgentFactory (они передают null).
PlannerFactory получает и передаёт реальный UseCase.

**3б) Добавить поле для хранения LTM данных:**
```kotlin
private var currentLtmFacts: List<LongTermMemory> = emptyList()
```

**3в) В init{} — добавить реактивную подписку на LTM:**
```kotlin
// Подписка на LTM (только для PLANNER-чатов, где UseCase предоставлен)
getLtmByGroupUseCase?.let { ltmUseCase ->
    getChatByIdAsFlowUseCase(chatId)
        .filterNotNull()
        .flatMapLatest { chat -> ltmUseCase(chat.chatGroup.id) }
        .onEach { facts ->
            currentLtmFacts = facts
            refreshMemoryInspector()
        }
        .launchIn(viewModelScope)
}
```

**3г) В `onEach { chatData ->` — вызывать обновление инспектора:**
```kotlin
.onEach { chatData ->
    chat = chatData
    chatData?.settings?.let { settings -> chatSettings = settings }
    refreshMemoryInspector()  // ← ДОБАВИТЬ
}
```

**3д) В `onEach { messages ->` — вызывать обновление:**
```kotlin
.onEach { messages ->
    // ... existing mapping ...
    refreshMemoryInspector()  // ← ДОБАВИТЬ (после обновления _state)
}
```

**3е) Переименовать и исправить `updateMemoryInspectorState()` → `refreshMemoryInspector()`:**
```kotlin
private fun refreshMemoryInspector() {
    val currentChat = chat
    val currentMessages = _state.value.chatList.messages

    val shortTermMessages = currentMessages.map { msg ->
        ShortTermMemoryItem(
            role = if (msg.userType == ChatMessageUiType.User) "user" else "assistant",
            content = msg.text,
            timestamp = msg.id
        )
    }

    val longTermFacts = currentLtmFacts.map { ltm ->
        LongTermFactItem(
            memoryKey = ltm.memoryKey,
            category = ltm.category,
            fact = ltm.fact,
            updatedAt = ltm.updatedAt
        )
    }

    _memoryInspectorState.update { current ->
        current.copy(
            shortTermMessages = shortTermMessages.toImmutableList(),
            workingMemory = currentChat?.workingSummary,
            longTermFacts = longTermFacts.toImmutableList()
        )
    }
}
```

**3ж) Добавить обработку новых событий в onEvent:**
```kotlin
ConsoleViewModel.Event.OpenMemoryInspector -> {
    refreshMemoryInspector()
    _memoryInspectorState.update { it.copy(isExpanded = true) }
}
ConsoleViewModel.Event.ToggleMemoryInspector -> {
    _memoryInspectorState.update { it.copy(isExpanded = !it.isExpanded) }
}
```

**3з) Удалить публичные дубли `confirmStageCreation()` и `declineStageCreation()`:**
Оставить только логику в `onEvent(ConfirmStageCreation)` и `onEvent(DeclineStageCreation)`.

**3и) Обновить PlannerFactory — добавить UseCase:**
```kotlin
class PlannerFactory @Inject constructor(
    ...
    private val getLtmByGroupUseCase: GetLongTermMemoryByGroupUseCase,  // ← ДОБАВИТЬ
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val chatId = extras[CHAT_ID_KEY] ?: error("ID not found in extras")
        return ConsoleViewModelImpl(
            ...,
            getLtmByGroupUseCase = getLtmByGroupUseCase,  // ← ПЕРЕДАТЬ
            chatId = chatId
        ) as T
    }
}
```

**3к) Factory и AgentFactory — передают null:**
```kotlin
getLtmByGroupUseCase = null
```

---

### Шаг 4: Исправить ConsoleScreen
**Файл:** `ConsoleScreen.kt`

**4а) При клике на Memory кнопку — вызывать ViewModel:**
```kotlin
onMemoryClick = {
    viewModel.onEvent(ConsoleViewModel.Event.OpenMemoryInspector)
    showMemoryInspector = true
}
```

**4б) Исправить no-op toggle:**
```kotlin
MemoryInspectorView(
    uiModel = memoryInspectorState,
    onToggleExpand = { viewModel.onEvent(ConsoleViewModel.Event.ToggleMemoryInspector) }
)
```

**4в) Убрать дублирующие импорты** (`AlertDialog`, `Text`, `TextButton` дважды).

---

### Шаг 5: Stage-чаты в ChatsScreen — визуальная иерархия
**Файл:** `ChatsViewModelImpl.kt` + `ChatsViewModel.kt` + `ChatsScreen.kt`

**5а) В `ChatsViewModel.State.Chat` добавить поле:**
```kotlin
data class Chat(
    val id: Long,
    val chatType: ChatType,
    val title: String,
    val isSelected: Boolean,
    val isStageChat: Boolean = false,  // ← ДОБАВИТЬ
    val depth: Int = 0               // ← ДОБАВИТЬ (0 = main, 1 = stage)
)
```

**5б) В ChatsViewModelImpl при маппинге:**
```kotlin
State.Chat(
    id = model.id,
    chatType = model.chatGroup.chatType,
    title = model.title,
    isSelected = selectedIndex == idx,
    isStageChat = model.parentId != null,
    depth = if (model.parentId != null) 1 else 0
)
```

**5в) В ChipsRow — стилизация stage-чатов:**
```kotlin
FilterChip(
    selected = pagerState.currentPage == index,
    onClick = { onChipClick(chat.id, index) },
    label = {
        Text(
            text = if (chat.isStageChat) "↳ ${chat.title}" else chat.title,
            style = if (chat.isStageChat)
                MaterialTheme.typography.labelSmall
            else
                MaterialTheme.typography.bodyMedium
        )
    },
    colors = if (chat.isStageChat)
        FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    else FilterChipDefaults.filterChipColors()
)
```

---

## Файлы для изменения (итого)

| Файл | Изменение |
|------|-----------|
| `domain/usecase/GetLongTermMemoryByGroupUseCase.kt` | Создать новый |
| `console/impl/ui/viewmodel/ConsoleViewModel.kt` | Добавить события |
| `console/impl/ui/viewmodel/ConsoleViewModelImpl.kt` | Основные исправления |
| `console/impl/ui/ConsoleScreen.kt` | Исправить обработчики |
| `chats/impl/ui/viewmodel/ChatsViewModel.kt` | Добавить поле isStageChat |
| `chats/impl/ui/viewmodel/ChatsViewModelImpl.kt` | Передать isStageChat |
| `chats/impl/ui/ChatsScreen.kt` | Визуальная иерархия чипов |

## Порядок реализации

1. Создать `GetLongTermMemoryByGroupUseCase`
2. Обновить `ConsoleViewModel` интерфейс (события)
3. Переработать `ConsoleViewModelImpl` (всё вместе)
4. Исправить `ConsoleScreen`
5. Stage-чаты в ChatsScreen

## Что НЕ трогаем в этом плане

- PlannerWorker — работает корректно
- PlannerTalkDelegate — работает корректно
- База данных / репозитории — реализованы правильно
- GroupChoiceScreen / GroupChoiceViewModel — PLANNER создание уже реализовано
- StageCreationDialog — компонент реализован правильно
