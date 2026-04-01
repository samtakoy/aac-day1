# Этап 5: UI — ChatSettings + ConsoleViewModelImpl

## Общее описание

Добавление переключателя "Handle PR" в окно настроек чата. При включении — запускается `TelegramPollingWorker`. При выключении — останавливается. Только один чат может быть привязан к этой фиче.

**Зависимости:** Этапы 2, 3, 4 (все предыдущие)

**Что получим:**
- В окне настроек чата появляется переключатель "Мониторинг PR"
- При включении — в DataStore сохраняется `chatId` и `isEnabled=true`, запускается WorkManager
- При выключении — останавливается WorkManager
- Если другой чат включит мониторинг — он перезапишет `chatId` (only one)

**Критерии успеха:**
- Открыть настройки любого чата-консоли → виден переключатель "Мониторинг PR"
- Включить → в logcat `TelegramPolling: пусто` раз в минуту
- Открыть настройки другого чата → переключатель выключен (только один chatId в DataStore)
- Выключить → logcat замолкает

---

## Задача 5.1: ChatSettingsUiModel — добавить handlePr

### Файл для изменения

**`app/src/main/java/com/example/day/features/console/impl/ui/components/ChatSettingsUiModel.kt`**

### Текущее состояние

```kotlin
@Immutable
data class ChatSettingsUiModel(
    val title: String,
    val chatTitle: String,
    val settingsState: ChatSettings
)
```

### Что изменить

Добавить поле `handlePr: Boolean`:

```kotlin
@Immutable
data class ChatSettingsUiModel(
    val title: String,
    val chatTitle: String,
    val settingsState: ChatSettings,
    val handlePr: Boolean = false
)
```

`handlePr` читается из DataStore (через `GetPrHandleStateUseCase`), не из `ChatSettings` (Room БД). Значение `true` только у того чата, чей `chatId` совпадает с сохранённым в DataStore.

---

## Задача 5.2: ChatSettingsView — добавить переключатель

### Файл для изменения

**`app/src/main/java/com/example/day/features/console/impl/ui/components/ChatSettingsView.kt`**

### Описание изменения

Добавить Switch-переключатель в `ChatSettingsView`. Разместить его в конце списка настроек, перед кнопками Cancel/OK.

### Что добавить

Параметр в функцию `ChatSettingsView`:
```kotlin
onHandlePrToggle: (Boolean) -> Unit
```

Внутри Composable, после существующих полей настроек, добавить Row с Label и Switch:

```kotlin
Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Column {
        Text(
            text = "Мониторинг PR",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "Автоматическое ревью Pull Request-ов",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Switch(
        checked = uiModel.handlePr,
        onCheckedChange = onHandlePrToggle
    )
}
```

### Важно

`uiModel` здесь — это `ChatSettingsUiModel`, у которого теперь есть поле `handlePr`. Убедиться что в `ChatSettingsView` передаётся актуальный `uiModel` и callback `onHandlePrToggle`.

Найти место вызова `ChatSettingsView` в коде (скорее всего в основном Composable экрана консоли) — обновить вызов, добавив `onHandlePrToggle`.

---

## Задача 5.3: ConsoleViewModelImpl — подключить PR Handle

### Файл для изменения

**`app/src/main/java/com/example/day/features/console/impl/ui/viewmodel/ConsoleViewModelImpl.kt`**

### Изменения в конструкторе / Factory

Добавить зависимости в `Factory`:
- `GetPrHandleStateUseCase`
- `SetPrHandleEnabledUseCase`

Посмотреть на существующие Factory-классы внутри `ConsoleViewModelImpl` (`Factory`, `AgentFactory`, `PlannerFactory`, и др.). Все фичи PR Handle нужны для **всех типов чатов** (любой чат может включить мониторинг), поэтому добавить в базовый `Factory` или вынести в отдельный общий блок инициализации.

Наиболее подходящий вариант — добавить в общий `init` блок `ConsoleViewModelImpl` который вызывается из всех Factory:

```kotlin
// В ConsoleViewModelImpl:
private val getPrHandleStateUseCase: GetPrHandleStateUseCase,
private val setPrHandleEnabledUseCase: SetPrHandleEnabledUseCase,
```

### Новый flow в init блоке

В `init {}` блоке `ConsoleViewModelImpl`, рядом с другими `launch` блоками для подписок:

```kotlin
// Подписка на состояние PR Handle
viewModelScope.launch {
    getPrHandleStateUseCase().collect { prHandleState ->
        _state.update { currentState ->
            val currentSettings = currentState.settings
            if (currentSettings != null) {
                val handlePrForThisChat = prHandleState.isEnabled && prHandleState.chatId == chatId
                currentState.copy(
                    settings = currentSettings.copy(handlePr = handlePrForThisChat)
                )
            } else {
                currentState
            }
        }
    }
}
```

**Важно:** `handlePr = true` только если `prHandleState.isEnabled == true` И `prHandleState.chatId == chatId` (т.е. именно этот чат включил мониторинг). `chatId` — это ID текущего чата (консоли), который уже доступен в ViewModel.

### Новый Event: HandlePrToggled

В sealed interface `Event` внутри `ConsoleViewModelImpl` (или в отдельном файле если Events выделены):

```kotlin
data class HandlePrToggled(val isEnabled: Boolean) : Event
```

### Обработчик в `onEvent`

В `when(event)` блоке обработки событий добавить:

```kotlin
is Event.HandlePrToggled -> {
    viewModelScope.launch {
        // Передаём ModelSettings текущего чата — они будут использованы агентами ревью
        val currentModelSettings = _state.value.settings?.settingsState?.model
        setPrHandleEnabledUseCase(event.isEnabled, chatId, currentModelSettings)
    }
}
```

`currentModelSettings` — это `ChatSettings.model` (`ModelSettings`) текущего чата из `_state.value.settings.settingsState`. Именно эти настройки модели будут сохранены в DataStore и использованы агентами при ревью. Если настройки ещё не загружены (settings == null) — `SetPrHandleEnabledUseCase` принимает `null` и использует `ModelSettings.default()` как fallback (в `PrReviewWorker`).

### Инициализация settings в существующем коде

Найти место где `ChatSettingsUiModel` создаётся (при загрузке чата / открытии settings). Убедиться что `handlePr` инициализируется корректно — через `getPrHandleStateUseCase` или из текущего `_state.value.settings?.handlePr` (который уже обновляется через flow).

При первом открытии окна настроек, `handlePr` уже будет актуальным благодаря flow-подписке в `init`.

---

## Задача 5.4: Связать ChatSettingsView с ConsoleViewModelImpl

### Файл для изменения

Найти где вызывается `ChatSettingsView` — скорее всего в основном Screen Composable для консоли:

**`app/src/main/java/com/example/day/features/console/impl/ui/ConsoleScreen.kt`** (или аналогичный файл)

Обновить вызов `ChatSettingsView`, добавив:
```kotlin
onHandlePrToggle = { isEnabled ->
    viewModel.onEvent(ConsoleViewModelImpl.Event.HandlePrToggled(isEnabled))
}
```

---

## Задача 5.5: DI — добавить новые use cases в Factory

### Файлы для изменения

Все Factory-классы внутри `ConsoleViewModelImpl` (AgentFactory, PlannerFactory и т.д.) создают ViewModel с определённым набором зависимостей. Нужно добавить `GetPrHandleStateUseCase` и `SetPrHandleEnabledUseCase` в каждый Factory.

**Подход:** добавить их в inner `class Factory @Inject constructor(...)` как параметры. Dagger инжектирует их автоматически так как они используют `@Inject constructor`.

Посмотреть как это сделано для других use cases в Factory-классах — повторить паттерн.

---

## Структура изменений этапа

```
Изменяемые файлы:
app/src/.../console/impl/ui/components/ChatSettingsUiModel.kt
    — добавить поле handlePr: Boolean = false

app/src/.../console/impl/ui/components/ChatSettingsView.kt
    — добавить параметр onHandlePrToggle: (Boolean) -> Unit
    — добавить Row со Switch внутри Composable

app/src/.../console/impl/ui/viewmodel/ConsoleViewModelImpl.kt
    — добавить зависимости: GetPrHandleStateUseCase, SetPrHandleEnabledUseCase
    — добавить flow-подписку в init {}
    — добавить Event.HandlePrToggled
    — добавить обработчик в onEvent
    — обновить все Factory-классы

app/src/.../console/impl/ui/ConsoleScreen.kt (или где вызывается ChatSettingsView)
    — добавить onHandlePrToggle callback
```

---

## Примечания

### Про "только один чат"

DataStore хранит один `HANDLE_PR_CHAT_ID`. Если пользователь включает HandlePr в чате A, а потом в чате B — чат A больше не является активным (его `handlePr = false` в UiModel). Это происходит автоматически: flow в `init` для чата A пересчитает `handlePr = (prHandleState.chatId == chatIdA)` → false.

### Про settings == null

Flow-подписка на `prHandleState` обновляет `_state.settings` только если `settings != null` (т.е. только когда окно настроек открыто). Это нормально — при открытии settings значение подтянется из потока.

Альтернативно: обновлять только при открытии окна настроек. Текущий подход (обновление всегда) немного избыточен, но безопасен.

### Про State.settings создание

Найти где в ViewModel создаётся `ChatSettingsUiModel` при событии "открыть настройки" (SettingsOpenEvent или аналог). Убедиться что там тоже подставляется актуальный `handlePr`. Можно взять из текущего `_state.value.settings?.handlePr ?: false` — оно уже актуально благодаря flow.
