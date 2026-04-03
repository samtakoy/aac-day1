# Этап 3. DI + AssistantTalkDelegate wiring

## Общее описание

Подключить `OrchestratorWorker` к существующей инфраструктуре:
1. Добавить его в граф зависимостей (через `ConsoleFeatureDeps` → `AppComponent`)
2. Инжектировать в `AssistantTalkDelegate`
3. Добавить ветку обработки `@@task` в `tryAddUserMessage`

Все изменения — минимальные и изолированные. `assistantWorker` и `consumptionCalculator` не трогаются.

---

## Задача этапа

### Изменение 1: `ConsoleFeatureDeps`

**Файл:** `app/src/main/java/com/example/day/features/console/impl/di/ConsoleFeatureDeps.kt`

Добавить новое свойство в интерфейс рядом с `assistantWorker`:

```
val orchestratorWorker: OrchestratorWorker
```

Добавить импорт:
```
import com.example.day.core.core_features.agent.domain.workers.concrete.OrchestratorWorker
```

`AppComponent` реализует `ConsoleFeatureDeps`. Поскольку `OrchestratorWorker` имеет `@Inject constructor` и все его зависимости (`JustWorkWorker`, `ChatTools`) уже в графе `AppComponent` — Dagger предоставит его автоматически без изменений в `AppComponent`.

---

### Изменение 2: `ConsoleFeatureModule`

**Файл:** `app/src/main/java/com/example/day/features/console/impl/di/ConsoleFeatureModule.kt`

В методе `provideAssistantTalkDelegate(deps: ConsoleFeatureDeps)` добавить параметр при создании делегата:

```
orchestratorWorker = deps.orchestratorWorker
```

Итоговый вызов конструктора:
```
AssistantTalkDelegate(
    addChatMessageUseCase = deps.addChatMessageUseCase,
    assistantWorker = deps.assistantWorker,
    orchestratorWorker = deps.orchestratorWorker,   // новое
    chatTools = deps.chatTools,
    consumptionCalculator = deps.consuption,
)
```

---

### Изменение 3: `AssistantTalkDelegate`

**Файл:** `app/src/main/java/com/example/day/features/console/impl/ui/delegates/AssistantTalkDelegate.kt`

#### 3.1. Добавить зависимость в конструктор

Добавить рядом с `assistantWorker`:
```
private val orchestratorWorker: OrchestratorWorker,
```

#### 3.2. Изменить `tryAddUserMessage`

Текущая логика после `addChatMessageUseCase.invoke(...)` и `onSuccess()`:

```kotlin
var lastAnswer: String? = null
try {
    assistantWorker.doWork(
        userPrompt = inputText,
        chat = chat,
        onEvent = { event ->
            if (event is WorkerEvent.RequestSuccess) {
                lastAnswer = event.result.choices.firstOrNull()?.message?.content
            }
            consumptionCalculator.onWorkerEvent(chat, event)
        }
    )
} catch (e: Throwable) {
    chatTools.addInfoMessage(chat.id, "❌ ${e.stackTraceToString()}")
}
return lastAnswer
```

Заменить блок `try { assistantWorker.doWork(...) }` на ветку:

```
val isOrchestratorTask = inputText.trimStart().startsWith(OrchestratorWorker.TASK_PREFIX, ignoreCase = true)

val worker: AWorker = if (isOrchestratorTask) orchestratorWorker else assistantWorker

try {
    worker.doWork(
        userPrompt = inputText,
        chat = chat,
        onEvent = { event ->
            if (event is WorkerEvent.RequestSuccess) {
                lastAnswer = event.result.choices.firstOrNull()?.message?.content
            }
            consumptionCalculator.onWorkerEvent(chat, event)
        }
    )
} catch (e: Throwable) {
    chatTools.addInfoMessage(chat.id, "❌ ${e.stackTraceToString()}")
}
```

Добавить импорт:
```
import com.example.day.core.core_features.agent.domain.workers.concrete.OrchestratorWorker
import com.example.day.core.core_features.agent.domain.workers.base.AWorker
```

---

## Ключевые инварианты

- `onEvent`-лямбда идентична для обоих воркеров: `consumptionCalculator` и `lastAnswer` работают одинаково
- `addChatMessageUseCase` (добавление сообщения пользователя в историю чата) вызывается до ветки — не дублируется
- `assistantWorker.doWork()` для всех остальных сообщений не меняется

---

## Результат и критерии успеха

**Что получим:** полностью рабочая фича.

**Критерии успеха:**
- Проект компилируется без ошибок
- Сообщение `@@task <текст>` в ASSISTANT-чате → вызывается `OrchestratorWorker`, выводятся INFO-блоки в чат
- Обычное сообщение (без `@@task`) → поведение не изменилось, `assistantWorker` работает как прежде
- `@@help <текст>` → поведение не изменилось (обрабатывается внутри `AssistantWorker`)
- В чате видны расходы токенов от запроса декомпозитора (consumptionCalculator сработал)
- При ответе без тегов — полный текст появляется одним INFO-сообщением
- При ответе с тегами — каждый блок отдельным INFO-сообщением
