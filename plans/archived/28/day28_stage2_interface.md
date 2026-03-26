# Этап 2. TalkDelegate: изменение интерфейса + захват ответа

## Описание
Изменяем контракт `TalkDelegate.tryAddUserMessage` — метод теперь возвращает `String?` (текст ответа модели).
Только `RagTalkDelegate` возвращает реальное значение; остальные реализации возвращают `null`.
В `RagTalkDelegate` добавляем захват ответа через `WorkerEvent.RequestSuccess`.

## Задачи

### 2.1 TalkDelegate (интерфейс)
Файл: `app/src/.../delegates/TalkDelegate.kt`

- Изменить сигнатуру: `suspend fun tryAddUserMessage(...): String?`

### 2.2 Реализации, возвращающие null
Файлы: `AgentsTalkDelegate.kt`, `LlmTalkDelegate.kt`, `PlannerTalkDelegate.kt`

- Добавить `return null` в конец каждого метода (или изменить последнее выражение на `return null`)

### 2.3 RagTalkDelegate — захват ответа
Файл: `app/src/.../delegates/RagTalkDelegate.kt`

- Завести локальную переменную `var lastAnswer: String? = null`
- Передать `onEvent` в `ragWorker.doWork()`:
  - При `WorkerEvent.RequestSuccess` → записать `lastAnswer = result.choices.firstOrNull()?.message?.content`
- Вернуть `lastAnswer` в конце метода

---

## Резюме

**Что получим:** метод `tryAddUserMessage` в `RagTalkDelegate` возвращает текст последнего успешного ответа LLM. Это основа для `@@testqueries` (Этап 4).

**Критерии успеха:**
- Компиляция без ошибок
- Поведение обычного RAG-чата не изменилось (ответы по-прежнему добавляются в чат)
- Ручной тест: ввести вопрос в RAG_CONTEXT чат → ответ появляется в чате (return value никем не используется — но внутри захватывается)

---

## Подробный план реализации

### Шаг 1. TalkDelegate.kt

Изменить сигнатуру метода:
```
suspend fun tryAddUserMessage(chat: Chat, inputText: String, onSuccess: () -> Unit): String?
```

### Шаг 2. AgentsTalkDelegate.kt, LlmTalkDelegate.kt, PlannerTalkDelegate.kt

В каждой реализации — найти `override suspend fun tryAddUserMessage(...)` и добавить `return null` в конце блока. Возможно, у некоторых уже есть `return` в конце — заменить тип возврата.

### Шаг 3. RagTalkDelegate.kt

Изменить `tryAddUserMessage` по схеме:

```
override suspend fun tryAddUserMessage(chat, inputText, onSuccess): String? {
    // @@debuginfo — без изменений (return null в конце)
    if (inputText.trim() == "@@debuginfo") { ... ; return null }

    // добавить user message
    addChatMessageUseCase.invoke(...)
    onSuccess()

    var lastAnswer: String? = null
    try {
        ragWorker.doWork(
            userPrompt = inputText,
            chat = chat,
            onEvent = { event ->
                if (event is WorkerEvent.RequestSuccess) {
                    lastAnswer = event.result.choices.firstOrNull()?.message?.content
                }
            }
        )
    } catch (e: Throwable) {
        chatTools.addInfoMessage(chat.id, "❌ ${e.stackTraceToString()}")
    }
    return lastAnswer
}
```

### Шаг 4. Проверка компиляции

Убедиться, что все места, где `tryAddUserMessage` вызывается (ViewModel и т.п.), не сломаны изменением возвращаемого типа (return value просто игнорируется — это нормально для Kotlin).
