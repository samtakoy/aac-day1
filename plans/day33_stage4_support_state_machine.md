# Этап 4: Support State Machine — детальный план реализации

## Контекст этапа

Реализуем логику Support-агента: 5 хендлеров, конфигурация состояний, данные состояний.
Подключаем `SupportStateMachineModule` в DI, убираем устаревший биндинг `StateStore`.

Зависимости:
- Этап 2 завершён: `StateStoreImpl` (бывший `TaskStateStoreImpl`) не имеет `@Inject`, принимает `StateConfig` в конструктор
- Этап 3 завершён: `SupportTalkDelegate` использует `@Named("support") TaskWorker`

После этапа: полноценный Support-агент проходит весь цикл INIT → PLANNING → EXECUTION → VERIFICATION → DONE → INIT.

---

## Что получим (критерии успеха)

- [ ] Проект собирается без ошибок
- [ ] Support-агент приветствует пользователя и спрашивает имя
- [ ] Создаётся тикет при определении проблемы
- [ ] Агент отвечает на вопросы, используя `search_codebase`
- [ ] Верификация → подтверждение → прощание → новый цикл
- [ ] При словах "оператор"/"человек" тикет переводится в статус `operator` и агент прощается

---

## Замечание: `StateId` — value class по строке

`StateId` сравнивается по значению строки. Поскольку `SupportStateConfig` использует те же строки ("init", "planning" и т.д.) что и `TaskStateConfig`, функция `buildStateTransitionInfoMessage()` из `HandlerExt.kt` корректно отобразит Support-состояния без изменений.

---

## Блок A: Изменение DI

### A1. `AgentCoreFeatureModule.kt` — добавить `SupportStateMachineModule`

**Файл:** `core/core_features/agent/di/AgentCoreFeatureModule.kt`

**Добавить** в список `includes`:
```
SupportStateMachineModule::class
```

**Примечание:** Удаление `bindsTaskStateStore` из этого файла уже сделано в **Этапе 2**. Здесь только добавляем модуль, который предоставит `@Named("support") StateStore` и `@Named("support") TaskWorker`.

---

## Блок B: Новые файлы состояний

### Расположение всех новых файлов
```
core/core_features/agent/domain/workers/task/states_config/
├── SupportStateData.kt
├── SupportStateConfig.kt
├── SupportMemKeys.kt
└── handlers/support/
    ├── SupportInitStateHandler.kt
    ├── SupportPlanningStateHandler.kt
    ├── SupportExecutionStateHandler.kt
    ├── SupportVerificationStateHandler.kt
    └── SupportDoneStateHandler.kt
```

---

### B1. `SupportMemKeys.kt`

Object с константами JSON-ключей для Support-агента.

**Поля:**
```
REPLY_TO_USER       = "reply_to_user"      // совпадает с TaskMemKeys
NEXT_STATE          = "next_state"         // совпадает с TaskMemKeys
MEM_UPDATES         = "memory_updates"     // совпадает с TaskMemKeys
USER_APPROVE        = "user_approve"       // совпадает с TaskMemKeys
TRUE                = "true"              // совпадает с TaskMemKeys

ESCALATE_OPERATOR   = "escalate_to_operator"
USER_NAME           = "user_name"
TICKET_ID           = "ticket_id"
TICKET_TITLE        = "ticket_title"
TICKET_DESCRIPTION  = "ticket_description"
SUMMARY             = "summary"

PLANNING            = "planning"
EXECUTION           = "execution"
VERIFICATION        = "verification"
DONE                = "done"
```

---

### B2. `SupportStateData.kt`

Отдельный `sealed interface : StateData` — НЕ наследует `TaskStateData`.
Использует `TaskStateMessage` для history (переиспользуем тип, он generic).

**Подклассы:**

`Init`:
- `userId: Long? = null`
- `userName: String? = null`
- `override val history: List<TaskStateMessage> = emptyList()`
- `override val state = SupportState.INIT`

`Planning`:
- `ticketId: Long? = null`
- `ticketTitle: String? = null`
- `ticketDescription: String? = null`
- `override val history: List<TaskStateMessage> = emptyList()`
- `override val state = SupportState.PLANNING`

`Execution`:
- `ticketId: Long`
- `override val history: List<TaskStateMessage> = emptyList()`
- `override val state = SupportState.EXECUTION`

`Verification`:
- `ticketId: Long`
- `summary: String? = null`
- `override val history: List<TaskStateMessage> = emptyList()`
- `override val state = SupportState.VERIFICATION`

`Done`:
- `ticketId: Long? = null`
- `override val history: List<TaskStateMessage> = emptyList()`
- `override val state = SupportState.DONE`

Все подклассы аннотированы `@Serializable` с `@SerialName` на полях.

---

### B3. `SupportStateConfig.kt`

Object-синглтон, аналог `TaskStateConfig`.

**Константы состояний:**
```
INIT         = StateId("init")
PLANNING     = StateId("planning")
EXECUTION    = StateId("execution")
VERIFICATION = StateId("verification")
DONE         = StateId("done")
```

**`val config = StateConfig(...)`:**

- `states` = listOf(INIT, PLANNING, EXECUTION, VERIFICATION, DONE)
- `serializers` = по одному для каждого `SupportStateData.*`
- `transitions`:
  - `INIT → listOf(PLANNING)`
  - `PLANNING → listOf(EXECUTION)`
  - `EXECUTION → listOf(VERIFICATION, PLANNING)` — retry
  - `VERIFICATION → listOf(DONE, EXECUTION)` — rework
  - `DONE → listOf(INIT)` ← **отличие от TaskStateConfig: loop back**
- `handlers` = по одному для каждого состояния
- `initialState = INIT`
- `finalStates = emptyList()` — нет финального состояния, цикл бесконечный
- `fallbackState = INIT`
- `fallbackStateData = SupportStateData.Init()`
- `stateInfoProvider` — описания состояний по-русски

---

### B4. `SupportStateMachineModule.kt`

**Файл:** `core/core_features/agent/di/SupportStateMachineModule.kt`

По структуре — точная копия `TaskStateMachineModule` после рефакторинга в Этапе 2, но с `SupportStateConfig.config` и `agentName = "support_agent"`.

**Предоставляет:**

`@Provides @Named("support") @Singleton fun provideSupportStateStore(agentMemoryRepository, agentContextRepository): StateStore`
→ `StateStoreImpl(agentMemoryRepository, agentContextRepository, SupportStateConfig.config)`

`@Provides @Named("support") fun provideSupportWorker(..., @Named("support") stateStore: StateStore): TaskWorker`
→ `TaskWorker(..., stateStore = stateStore, agentName = "support_agent")`

Параметры `provideTaskWorker` совпадают с `TaskStateMachineModule.provideTaskWorker` — только `stateStore` берётся с qualifier `@Named("support")` и `agentName = "support_agent"`.

---

## Блок C: Хендлеры

### Общий паттерн каждого хендлера

Каждый хендлер:
1. Реализует `TaskStateHandler`
2. В `handle()`: вызывает `TaskResponseParser.parse(rawResponse)` → при null возвращает ошибку
3. Проверяет эскалацию: `if (llmResponse.memoryUpdates["escalate_to_operator"] == "true") return handleEscalation(...)`
4. Выполняет state-specific логику
5. Использует extension-функции из `HandlerExt.kt`: `withBot`, `withButton`, `withTitle`, `withInfo`, `continueHistory`

**Метод `handleEscalation(context, llmResponse)`** — private в каждом хендлере (дублирование допустимо):

1. Получить `ticketId` из текущих StateData (Planning/Execution/Verification) — может быть null (если INIT)
2. Перейти в состояние DONE: `context.updateState(SupportState.DONE)`
3. Вернуть `HandlerResult`:
   - messages: `[Message(llmResponse.replyToUser)]`
   - llmRequest: `LlmRequest(userPrompt = "...")` с текстом:
     ```
     если ticketId != null:
       "Немедленно вызови update_crm_ticket с ticketId=$ticketId и status='operator'.
        После вызова инструмента скажи пользователю:
        'Окей, я сообщу своим кожанным мешкам о вашем вопросе, и они скоро свяжутся с вами по телефону.'"
     иначе:
       "Скажи пользователю: 'Окей, я сообщу своим кожанным мешкам о вашем вопросе,
        и они скоро свяжутся с вами по телефону.'"
     ```

Это запускает ещё один `doWork` (через `HandlerResult.llmRequest`). LLM вызывает CRM-тул и возвращает прощальное сообщение. Следующий `handle()` попадает в `SupportDoneStateHandler`, который завершает цикл переходом в INIT.

---

### C1. `SupportInitStateHandler`

**`stateName` = `SupportState.INIT`**

**`buildSystemPrompt()`:**
```
Ты — дружелюбный ассистент поддержки пользователей.

[Доступные инструменты]
- get_crm_user_by_chat(chatId) — проверить существующего пользователя
- create_crm_user(chatId, userName) — создать нового пользователя

[Цель]
1. Поприветствовать пользователя
2. Вызвать get_crm_user_by_chat(chatId={chat.id}) для проверки
3. Если пользователь найден — обратиться к нему по имени и перейти в PLANNING
4. Если не найден — спросить имя, затем вызвать create_crm_user и перейти в PLANNING

[Протокол ответа — JSON]
{
  "reply_to_user": "...",
  "memory_updates": { "user_name": "...", "user_id": "...", "escalate_to_operator": "true" или null },
  "next_state": "planning" или null
}
Поле escalate_to_operator помещается внутрь memory_updates как строка "true" (или вовсе опускается).
```

**Примечание:** `chatId` передаётся в системный промпт через `context.agentId` (agentId = chatId в архитектуре).

**`buildAssistantPreFillPrompt()`:**
`"Сейчас я поздороваюсь, проверю есть ли пользователь в системе и выясню как к нему обращаться."`

**`handle()`:**
1. Парсинг + проверка эскалации
2. Извлечь `userName` и `userId` из `llmResponse.memoryUpdates`
3. `nextState` = `toValidStateOrNull(llmResponse.nextState)`
4. `data = Init(userId, userName).copy(history = continueHistory(...))`
5. Если `nextState == PLANNING` и userName не blank:
   - `context.saveStateData(data)`
   - Перейти в PLANNING: `context.updateState(PLANNING)`
   - Вернуть `HandlerResult(messages = [Message(reply)], llmRequest = LlmRequest())`
6. Иначе: сохранить, вернуть только сообщение

**`handleUserAction()`:** Не используется — вернуть `HandlerResult(messages = [].withBot("Неизвестная команда"))`

---

### C2. `SupportPlanningStateHandler`

**`stateName` = `SupportState.PLANNING`**

**`buildSystemPrompt()`:**
```
Ты — ассистент поддержки пользователей. Твоя задача — определить проблему и создать тикет.

Данные пользователя:
- Имя: {userName из SupportStateData.Init}
- chatId: {context.agentId}

[Доступные инструменты]
- get_crm_user_tickets(chatId) — получить открытые тикеты пользователя
- create_crm_ticket(chatId, title, description) — создать новый тикет

[Цель]
1. Выяснить с какой проблемой пришёл пользователь
2. Вызвать get_crm_user_tickets для проверки существующих тикетов
3. Если есть открытый тикет по этой проблеме — предложить продолжить его
4. Если нет — вызвать create_crm_ticket и получить ticket_id
5. Перейти в EXECUTION с полученным ticket_id

[Протокол ответа — JSON]
{
  "reply_to_user": "...",
  "memory_updates": { "ticket_id": "123", "ticket_title": "...", "ticket_description": "...", "escalate_to_operator": "true" или null },
  "next_state": "execution" или null
}
Поле escalate_to_operator помещается внутрь memory_updates как строка "true" (или вовсе опускается).
```

**`buildAssistantPreFillPrompt()`:**
`"Проверю открытые тикеты пользователя и определю проблему с которой он пришёл."`

**`handle()`:**
1. Парсинг + эскалация
2. Извлечь `ticketId` из `memoryUpdates["ticket_id"]?.toLongOrNull()`
3. Извлечь `ticketTitle` из `memoryUpdates["ticket_title"]`
4. Извлечь `ticketDescription` из `memoryUpdates["ticket_description"]`
5. `nextState` = `toValidStateOrNull(...)`
6. `data = Planning(ticketId, ticketTitle, ticketDescription).copy(history = continueHistory(...))`
7. Если `nextState == EXECUTION` и `ticketId != null`:
   - `context.saveStateData(data)`
   - `context.updateState(EXECUTION)`
   - Вернуть с `llmRequest = LlmRequest()`
8. Иначе: сохранить, вернуть сообщение

---

### C3. `SupportExecutionStateHandler`

**`stateName` = `SupportState.EXECUTION`**

**`buildSystemPrompt()`:**

Читает `ticketId` и `ticketTitle/Description` из `SupportStateData.Planning`.

```
Ты — технический эксперт по поддержке пользователей нашей кодовой базы.

Тикет #{ticketId}: {ticketTitle}
Описание проблемы: {ticketDescription}

[Доступные инструменты]
- search_codebase(query) — поиск по кодовой базе для ответа на технические вопросы

[Цель]
Отвечать на вопросы пользователя, используя search_codebase для точных ответов.
Когда проблема решена — переходить в VERIFICATION.

[Протокол ответа — JSON]
{
  "reply_to_user": "...",
  "memory_updates": { "escalate_to_operator": "true" или null },
  "next_state": "verification" или null,
  "user_approve": "true" или null
}
Поле escalate_to_operator помещается внутрь memory_updates как строка "true" (или вовсе опускается).
```

**`handle()`:**
1. Парсинг + эскалация
2. Получить `ticketId` из `SupportStateData.Planning`
3. `data = Execution(ticketId).copy(history = continueHistory(...))`
4. Если `nextState == VERIFICATION`:
   - `context.saveStateData(data)`
   - Если `user_approve == "true"` → `handleUserAction(context, ACTION_PROCEED)`
   - Иначе → вернуть сообщение с кнопкой "Да, проблема решена" (`withButton(ACTION_PROCEED, "Да, решено")`)
5. Иначе: сохранить, вернуть сообщение

**`handleUserAction(ACTION_PROCEED)`:**
```
context.updateState(VERIFICATION)
HandlerResult(
    messages = [].withTitle(buildStateTransitionInfoMessage(..., VERIFICATION)),
    llmRequest = LlmRequest()
)
```

---

### C4. `SupportVerificationStateHandler`

**`stateName` = `SupportState.VERIFICATION`**

**`buildSystemPrompt()`:**

```
Ты — ассистент поддержки. Проверь, решена ли проблема пользователя.

Тикет #{ticketId}

[Цель]
1. Спросить пользователя: решена ли проблема
2. Если решена → перейти в DONE с кратким summary
3. Если не решена → вернуться в EXECUTION

[Протокол ответа — JSON]
{
  "reply_to_user": "...",
  "memory_updates": { "summary": "краткое резюме решения", "escalate_to_operator": "true" или null },
  "next_state": "done" или "execution" или null,
  "user_approve": "true" или null
}
Поле escalate_to_operator помещается внутрь memory_updates как строка "true" (или вовсе опускается).
```

**`buildAssistantPreFillPrompt()`:**
`"Уточню у пользователя — решена ли проблема, с которой он обратился."`

**`handle()`:**
1. Парсинг + эскалация
2. Получить `ticketId` из `SupportStateData.Execution`
3. Извлечь `summary` из `memoryUpdates`
4. `data = Verification(ticketId, summary).copy(history = continueHistory(...))`
5. `nextState = toValidStateOrNull(...)`
6. Если `nextState == DONE`:
   - Сохранить данные
   - Если `user_approve == "true"` → `handleUserAction(context, ACTION_DONE)`
   - Иначе → вернуть сообщение с кнопкой "Да, всё устраивает" (`withButton(ACTION_DONE, "Всё решено!")`)
7. Если `nextState == EXECUTION`:
   - Сохранить данные, перейти в EXECUTION через `handleUserAction(context, ACTION_RETRY)`
8. Иначе: сохранить, вернуть сообщение

**`handleUserAction()`:**
- `ACTION_DONE`:
  ```
  context.updateState(DONE)
  HandlerResult(messages = [].withTitle(...), llmRequest = LlmRequest())
  ```
- `ACTION_RETRY`:
  ```
  context.updateState(EXECUTION)
  HandlerResult(messages = [].withTitle(...), llmRequest = LlmRequest())
  ```

---

### C5. `SupportDoneStateHandler`

**`stateName` = `SupporSupportStatetStateConfig.DONE`**

**`buildSystemPrompt()`:**

Читает `summary` из `SupportStateData.Verification`.

```
Ты — ассистент поддержки.

Резюме решения: {summary}

[Доступные инструменты]
- update_crm_ticket(ticketId, status, result) — обновить статус тикета

[Инструкции]
1. Вызови update_crm_ticket(ticketId={ticketId}, status='closed', result='{summary}')
2. Поблагодари пользователя и попрощайся
3. Предложи обращаться снова

[Протокол ответа — JSON]
{
  "reply_to_user": "..."
}
Поле next_state не нужно — переход в INIT происходит автоматически.
```

**`buildAssistantPreFillPrompt()`:**
`"Закрою тикет и попрощаюсь с пользователем."`

**`handle()`:**
1. Парсинг (только `replyToUser` нужен — простой формат)
2. Получить `ticketId` из `SupportStateData.Verification`
3. Сохранить данные Done
4. Сбросить состояние: `context.clearTaskMemory()`
   - После очистки воркер при следующем вызове вернётся в INIT
5. Вернуть `HandlerResult(messages = [Message(llmResponse.replyToUser)])`
   - Без `llmRequest` — разговор завершён до следующего сообщения пользователя

**`handleUserAction()`:** Не используется.

---

## Блок D: Вспомогательная функция для получения ticketId

В каждом хендлере где нужен `ticketId` для эскалации — читать из StateData соответствующего состояния:

```
private suspend fun getCurrentTicketId(context: StateContext): Long? {
    // Читаем из Planning, затем Execution, затем Verification
    return (context.getStateData(SupportState.PLANNING, 1) as? SupportStateData.Planning)?.ticketId
        ?: (context.getStateData(SupportState.EXECUTION, 1) as? SupportStateData.Execution)?.ticketId
        ?: (context.getStateData(SupportState.VERIFICATION, 1) as? SupportStateData.Verification)?.ticketId
}
```

Эту функцию дублировать в каждом хендлере, где нужна эскалация (PLANNING, EXECUTION, VERIFICATION). В INIT и DONE ticketId ещё нет / уже не нужен.

---

## Порядок реализации (checklist)

1. [ ] `SupportMemKeys.kt` — константы (шаг B1)
2. [ ] `SupportStateData.kt` — sealed interface с 5 подклассами (шаг B2)
3. [ ] `SupportStateConfig.kt` — config объект (шаг B3, без handlers пока — добавить после)
4. [ ] `SupportInitStateHandler.kt` (шаг C1)
5. [ ] `SupportPlanningStateHandler.kt` (шаг C2)
6. [ ] `SupportExecutionStateHandler.kt` (шаг C3)
7. [ ] `SupportVerificationStateHandler.kt` (шаг C4)
8. [ ] `SupportDoneStateHandler.kt` (шаг C5)
9. [ ] Дополнить `SupportStateConfig.config` — добавить все хендлеры в `handlers` map
10. [ ] `SupportStateMachineModule.kt` — DI модуль (шаг B4)
11. [ ] `AgentCoreFeatureModule.kt` — убрать `bindsTaskStateStore`, добавить `SupportStateMachineModule` (шаг A1)
12. [ ] Сборка проекта
13. [ ] Запустить Support-чат, пройти полный цикл INIT → DONE → INIT

---

## Что НЕ меняется в этом этапе

- `TaskStateConfig`, `TaskStateData`, все Task-хендлеры — без изменений
- `HandlerExt.kt` — без изменений (все extension функции переиспользуются)
- `TaskStateMessage` — без изменений (используется как история в SupportStateData)
- `TaskResponseParser` — без изменений (используется в каждом Support-хендлере)
- `StateContext`, `StateStore`, `StateConfig` — без изменений
- `SupportTalkDelegate`, `SupportConsoleFeatureEntryImpl` — без изменений
