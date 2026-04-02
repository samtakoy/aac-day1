# Этап 2. OrchestratorWorker

## Общее описание

Создать `OrchestratorWorker` — реализацию `AWorker`, которая:
1. Принимает `userPrompt` с префиксом `@@task`
2. Через `JustWorkWorker` запускает одноразового LLM-агента с системным промптом декомпозиции
3. Парсит ответ через `OrchestratorResultParser` из Этапа 1
4. Выводит блоки результата в чат через `chatTools.addInfoMessage()`
5. Пробрасывает `onEvent` в `JustWorkWorker` — чтобы `WorkerEvent.RequestSuccess` дошёл до `ConsumptionCalculator`

Паттерн реализации: аналогичен агентам внутри `PrReviewWorker` (использует `JustWorkConfig` + `JustWorkWorker`), но сам оформлен как `AWorker` (в отличие от `PrReviewWorker`).

---

## Задача этапа

### Новый файл

**Путь:** `app/src/main/java/com/example/day/core/core_features/agent/domain/workers/concrete/OrchestratorWorker.kt`

**Пакет:** `com.example.day.core.core_features.agent.domain.workers.concrete`

### Класс `OrchestratorWorker`

**Аннотация:** `@Inject constructor` (без scope — создаётся в AppComponent)

**Реализует:** `AWorker`

**Конструктор (зависимости):**
- `justWorkWorker: JustWorkWorker`
- `chatTools: ChatTools`

**Companion object — константы:**

- `AGENT_NAME: String = "orchestrator_agent"`
- `TASK_PREFIX: String = "@@task"`
- `SYSTEM_PROMPT: String` — системный промпт агента-декомпозитора (полный текст из задачи):

```
Ты эксперт по декомпозиции задач.
Разбей пользовательскую задачу на конкретные подзадачи, которые можно выполнить последовательно и получить решение итоговой задачи.
Текст подзадачи будет дан llm-ассистенту.
Составь текст подзадачи так, чтобы он ее понял максимально полно и решил без галлюцинаций.
Ответ llm-ассистента по каждой подзадаче может быть использован при решении следующей подзадачи, если необходимо.

Правила:
1. Проанализируй задачу
2. Посмотри какие инструменты доступны для реализации задачи
3. Составь план реализации
4. Декомпозируй
5. Если задача пользователя может быть решена без декомпозиции - создай только одну подзадачу
6. Если ты видишь, что задачу пользователя невозможно выполнить с помощью вызовов доступных инструментов, то не создавай подзадачи, а напиши обоснование пользователю, почему задача не может быть выполнена.

Подзадачи должны быть:
- Конкретными и выполнимыми
- Независимыми друг от друга

Формат ответа:
[TASK_START]
Общее описание задачи и как ее можно решить с помощью доступных инструментов
[TASK_END]
[SUBTASKS_START]
Подзадача 1: описание первой подзадачи для llm-ассистента и описание возвращаемого результата.
[SUBTASKS_END]
[SUBTASKS_START]
Подзадача 2: описание второй подзадачи
[SUBTASKS_END]
и т.д.
```

### Метод `doWork`

Сигнатура (из `AWorker`):
```
override suspend fun doWork(
    userPrompt: String,
    chat: Chat,
    userRole: AContextMessage.Role,
    onEvent: (suspend (WorkerEvent) -> Unit)?
)
```

**Реализация (шаги):**

1. **Вычислить `cleanPrompt`:**
   `userPrompt.trimStart().substringAfter(TASK_PREFIX).trimStart()`

2. **Создать `JustWorkConfig`:**
   - `agentName = AGENT_NAME`
   - `chatId = chat.id`
   - `systemPrompt = SYSTEM_PROMPT`
   - `allowedTools = emptyList()`
   - `defaultModel = { chat.settings.model }`
   - `defaultContext = { AContextDefaultFactory.createFull() }`
   - `recreateAgent = true`

3. **Вызвать `justWorkWorker.doWork(config, cleanPrompt, userRole, onEvent)`** → `Result<String>`

4. **На успех** (`onSuccess { rawText -> ... }`):
   - Вызвать `OrchestratorResultParser.parse(rawText)` → `parsed`
   - Если `parsed == null`:
     - `chatTools.addInfoMessage(chat.id, rawText)`
   - Если `parsed != null`:
     - Если `parsed.taskDescription != null` → `chatTools.addInfoMessage(chat.id, parsed.taskDescription)`
     - Для каждого элемента `parsed.subtasks` → `chatTools.addInfoMessage(chat.id, subtask)`

5. **На ошибку** (`onFailure { error -> ... }`):
   - `chatTools.addInfoMessage(chat.id, "❌ OrchestratorWorker: ${error.message}")`

### Импорты (ориентир)

- `com.example.day.core.core_features.agent.domain.strategy.AContextDefaultFactory`
- `com.example.day.core.core_features.agent.domain.workers.base.AWorker`
- `com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent`
- `com.example.day.core.core_features.agent.domain.model.AContextMessage`
- `com.example.day.core.core_features.chat.domain.model.Chat`
- `com.example.day.core.core_features.chat.domain.tools.ChatTools`
- `javax.inject.Inject`

---

## Результат и критерии успеха

**Что получим:** `OrchestratorWorker` — самодостаточная единица, которую можно подключить к делегату в следующем этапе.

**Критерии успеха:**
- Класс компилируется без ошибок
- Реализует `AWorker` (все методы переопределены)
- `onEvent` прокидывается в `JustWorkWorker.doWork()` без модификации — `WorkerEvent.RequestSuccess` долетает наружу
- `recreateAgent = true` — каждый вызов начинает с чистой истории агента
- Выводит INFO-сообщения через `chatTools`, не вызывает `chatTools.addBotMessage()`
- Если `OrchestratorResultParser` вернул `null` — в чат попадает полный raw текст (не теряется)
