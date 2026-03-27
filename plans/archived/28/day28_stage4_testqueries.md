# Этап 4. Команда @@testqueries + RagWorker.saveTestResults

## Описание
Реализуем основную логику: обработчик `@@testqueries` в `RagTalkDelegate` и вспомогательный метод `saveTestResults` в `RagWorker`.
Зависит от Этапа 2 (answer capture) и Этапа 3 (расширенный протокол).

## Задачи

### 4.1 RagWorker — новый метод saveTestResults

Файл: `app/src/.../workers/concrete/RagWorker.kt`

Метод: `suspend fun saveTestResults(items: List<Pair<String, String>>, executionTimeMs: Long, chat: Chat): Result<String>`

Логика:
- Получить server URL из agent memory (тот же ключ `AUTO_RAG_URL_KEY`, агент `AGENT_NAME`)
- Определить `isLocalLlm` из `chat.settings`
- Вызвать `ragSearchRepository.saveRuntestResults(preset="rag_worker", items→RuntestResultItem, executionTimeMs, isLocalLlm, serverUrl)`
- Вернуть `Result<String>` с путём к сохранённому отчёту

### 4.2 RagTalkDelegate — обработчик @@testqueries

Файл: `app/src/.../delegates/RagTalkDelegate.kt`

Добавить ветку обработки в начало `tryAddUserMessage`:

```
if (inputText.trim() == "@@testqueries") {
    onSuccess()
    handleTestQueries(chat)
    return null
}
```

Метод `handleTestQueries(chat: Chat)`:
1. Показать info: "🔄 Запускаю тест: N вопросов..."
2. Измерить `startTime = System.currentTimeMillis()`
3. Цикл по `TestQueries.list`:
   - `val answer = tryAddUserMessage(chat, question, onSuccess = {})`
   - Если `answer == null` → показать info "❌ Ошибка на вопросе #${i+1}. Тест остановлен." → выйти
   - Добавить `question to answer` в `results`
4. Вычислить `executionTimeMs = System.currentTimeMillis() - startTime`
5. `ragWorker.saveTestResults(results, executionTimeMs, chat)`
   - При Success → info "✅ Тест завершён: ${results.size} вопросов за ${executionTimeMs}мс\nОтчёт: ${report}"
   - При Failure → info "⚠️ Тест прошёл, но отчёт не сохранён: ${error}"

---

## Резюме

**Что получим:** полностью рабочая команда `@@testqueries` в RAG_CONTEXT чате. Запускает 29 вопросов последовательно, отображает прогресс, отправляет отчёт на сервер. Тест с локальной моделью и с облачной сохранит соответствующие файлы (LOCAL/CLOUD).

**Критерии успеха:**
- В RAG_CONTEXT чате с локальной LLM: `@@testqueries` → 29 вопросов в чате → файл `runtest_RAG_WORKER_LOCAL_*.md` на сервере
- В RAG_CONTEXT чате с облачной LLM: аналогично → файл `runtest_RAG_WORKER_CLOUD_*.md`
- При ошибке на N-м вопросе: отчёт НЕ отправляется, info-сообщение с номером вопроса
- Обычные вопросы в RAG_CONTEXT чате работают без изменений

---

## Подробный план реализации

### Шаг 1. Найти как RagWorker получает server URL

Изучить `RagWorker.doWork()` — найти, откуда берётся URL для `ragSearchRepository`. Вероятно, через `agentMemoryRepository` с ключом `AUTO_RAG_URL_KEY` и `agentId` агента `AGENT_NAME`.

Если `RagWorker` не имеет прямого доступа к URL — посмотреть, хранится ли он в `currentRagContextProvider` или в памяти агента.

### Шаг 2. Определить isLocalLlm из chat.settings

Найти поле в `Chat.settings` или `ModelSettings`, которое отвечает за использование локальной LLM. Вероятно, это флаг `useLocalLlm: Boolean` или `isLocal: Boolean` в настройках модели.

### Шаг 3. Реализовать RagWorker.saveTestResults

```kotlin
suspend fun saveTestResults(
    items: List<Pair<String, String>>,
    executionTimeMs: Long,
    chat: Chat
): Result<String> {
    val serverUrl = // получить из agent memory или ragSearchRepository
    val isLocalLlm = chat.settings.model.isLocal  // уточнить поле
    return ragSearchRepository.saveRuntestResults(
        preset = "rag_worker",
        items = items.map { (q, a) -> RuntestResultItem(question = q, llmAnswer = a) },
        serverUrl = serverUrl,
        executionTimeMs = executionTimeMs,
        isLocalLlm = isLocalLlm,
    ).map { it.savedReport }
}
```

### Шаг 4. Добавить handleTestQueries в RagTalkDelegate

Новый приватный метод. Порядок:
1. `chatTools.addInfoMessage(chat.id, "🔄 Запускаю тест: ${TestQueries.list.size} вопросов...")`
2. `val startTime = System.currentTimeMillis()`
3. `val results = mutableListOf<Pair<String, String>>()`
4. `TestQueries.list.forEachIndexed { i, question -> ... }`
   - `val answer = tryAddUserMessage(chat, question, onSuccess = {})`
   - `if (answer == null) { chatTools.addInfoMessage(...); return }`
   - `results.add(question to answer)`
5. `val executionTimeMs = System.currentTimeMillis() - startTime`
6. `ragWorker.saveTestResults(results, executionTimeMs, chat).fold(onSuccess, onFailure)`

### Шаг 5. Подключить @@testqueries в tryAddUserMessage

В начало `tryAddUserMessage` (после `@@debuginfo`):
```kotlin
if (inputText.trim() == "@@testqueries") {
    onSuccess()
    handleTestQueries(chat)
    return null
}
```

### Шаг 6. Ручное тестирование

1. Запустить сервер
2. Открыть RAG_CONTEXT чат с ОБЛАЧНОЙ моделью
3. Ввести `@@testqueries` → наблюдать прогресс → проверить файл `_CLOUD_` на сервере
4. Открыть RAG_CONTEXT чат с ЛОКАЛЬНОЙ моделью (Ollama)
5. Ввести `@@testqueries` → наблюдать прогресс → проверить файл `_LOCAL_` на сервере
6. Убедиться, что обычный вопрос в RAG_CONTEXT чате работает без регрессии
