# Этап 5: Логирование (Observability)

## Что решает этот этап
Добавляет логирование ключевых шагов RAG-чата с тегом `(rag)(ktor)`.
Цель: во время тестирования сценариев (10-15 сообщений) понимать что происходит:
- какой запрос ушёл на сервер
- что вернул RAG
- как обновился TaskState
- что сохранилось в short history

## Что получим в итоге
- Все ключевые шаги RAG-пайплайна видны в Logcat с тегом `(rag)(ktor)`
- Можно фильтровать по тегу и видеть полную картину одного запроса

## Зависимости
- Нет жёстких зависимостей — можно добавлять логи параллельно с другими этапами
- Лучше делать в самом конце, когда все компоненты готовы

---

## Пошаговый план реализации

### Шаг 1. Создать TAG-константу

Чтобы не дублировать строку тега — вынести в константу.

**Файл:** `app/.../memory/domain/provider/rag/RagLog.kt`

```kotlin
package com.example.day.core.core_features.memory.domain.provider.rag

object RagLog {
    const val TAG = "(rag)(ktor)"
}
```

Использование: `Log.d(RagLog.TAG, "сообщение")`

---

### Шаг 2. Логи в RagWorker

**Файл:** `app/.../agent/domain/workers/concrete/RagWorker.kt`

```kotlin
Log.d(RagLog.TAG, "doWork: chat=${chat.id}, prompt='${userPrompt.take(80)}'")

// После получения serverUrl:
Log.d(RagLog.TAG, "serverUrl=$serverUrl")

// После result.fold:
result.fold(
    onSuccess = { Log.d(RagLog.TAG, "success: response length=${it.responseText.length}") },
    onFailure = { Log.e(RagLog.TAG, "error: ${it.message}") }
)
```

---

### Шаг 3. Логи в RagContextMemoryProvider

**Файл:** `app/.../memory/domain/provider/RagContextMemoryProvider.kt`

```kotlin
// В appendUserPrompt():
Log.d(RagLog.TAG, "appendUserPrompt: agentId=$agentId")

// После TaskState update:
Log.d(RagLog.TAG, "taskState updated: intent=${taskStateResult?.updatedState?.intent}, focus=${taskStateResult?.updatedState?.currentFocus?.`class`}, switched=${taskStateResult?.updatedState?.contextSwitched}")

// После AutoRag:
Log.d(RagLog.TAG, "autoRag done: prompt length after enrichment=${result.content.length}")

// В postProcess():
Log.d(RagLog.TAG, "postProcess: saved short history entry (summary length=${pendingLastResponseSummary.length})")
```

---

### Шаг 4. Логи в TaskStateRepositoryImpl

**Файл:** `app/.../memory/data/repository/TaskStateRepositoryImpl.kt`

```kotlin
// Перед HTTP запросом:
Log.d(RagLog.TAG, "TaskState update request → $serverUrl/task-state/update, messages=${lastMessages.size}")

// При ошибке:
Log.w(RagLog.TAG, "TaskState update failed: ${e.message}, using cached state")

// После успеха:
Log.d(RagLog.TAG, "TaskState updated: intent=${updatedState.intent}, summary='${result.lastResponseSummary.take(60)}'")
```

---

### Шаг 5. Логи в AutoRagMemoryProvider

**Файл:** `app/.../memory/domain/provider/AutoRagMemoryProvider.kt`

```kotlin
// Перед поиском:
Log.d(RagLog.TAG, "RAG search: query='${prompt.content.take(80)}', preset=reranked_llm, hasTaskState=${!taskStateJson.isNullOrBlank()}, hasHistory=${!shortHistory.isNullOrBlank()}")

// При ошибке:
Log.w(RagLog.TAG, "RAG search failed: ${it.message}")

// После успеха:
Log.d(RagLog.TAG, "RAG search ok: result length=${ragResult.length}")
```

---

### Шаг 6. Логи в ShortHistoryRepositoryImpl

**Файл:** `app/.../memory/data/repository/ShortHistoryRepositoryImpl.kt`

```kotlin
// В append():
Log.d(RagLog.TAG, "ShortHistory: appended entry, total=${current.size}")
```

---

### Шаг 7. Логи на стороне rag-server

**Файл:** `rag-server/.../RagServer.kt` (в обработчике `/search`)

```kotlin
// Уже есть println("[QueryOptimizer] ...") — добавить тег:
println("[rag][ktor][/search] query='$query', preset=${pipelineConfig.rerankStrategy}, hasTaskState=${!taskState.isNullOrBlank()}, hasHistory=${!history.isNullOrBlank()}")
```

**Файл:** `rag-server/.../agent_context/TaskStateUpdaterService.kt`

```kotlin
println("[rag][ktor][task-state] updating, messages=${request.lastMessages.size}")
println("[rag][ktor][task-state] done: intent=${...}, summary length=${response.lastResponseSummary.length}")
```

---

## Формат логов в Logcat

Фильтр в Android Studio: `tag:(rag)(ktor)`

Пример вывода одного запроса:
```
D (rag)(ktor): doWork: chat=42, prompt='Как работает AuthService?'
D (rag)(ktor): serverUrl=http://10.0.2.2:3001
D (rag)(ktor): appendUserPrompt: agentId=7
D (rag)(ktor): TaskState update request → http://10.0.2.2:3001/task-state/update, messages=4
D (rag)(ktor): TaskState updated: intent=debugging, summary='AuthService handles login via JWT tokens...'
D (rag)(ktor): taskState updated: intent=debugging, focus=AuthService, switched=false
D (rag)(ktor): RAG search: query='Как работает AuthService?', preset=reranked_llm, hasTaskState=true, hasHistory=true
D (rag)(ktor): RAG search ok: result length=3421
D (rag)(ktor): autoRag done: prompt length after enrichment=3847
D (rag)(ktor): success: response length=1204
D (rag)(ktor): postProcess: saved short history entry (summary length=87)
D (rag)(ktor): ShortHistory: appended entry, total=3
```

---

## Что проверить после реализации

1. Запустить RAG-чат, написать сообщение
2. В Logcat с фильтром `(rag)(ktor)` должна появиться последовательность логов
3. Проверить что каждый шаг пайплайна залогирован
4. Убедиться что ошибки (недоступный сервер) тоже логируются с уровнем WARN/ERROR

---

## Нюансы

- Логи с `content` обрезаются до 80 символов (`.take(80)`) — чтобы не засорять Logcat
- Для prod-кода логи за `if (BuildConfig.DEBUG)` не ставим — задача учебная, нужна полная видимость
- На стороне rag-server используем `println()` (как везде в проекте), а не SLF4J
