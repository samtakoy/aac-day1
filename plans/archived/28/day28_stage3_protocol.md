# Этап 3. Расширение сетевого протокола (Android + Server DTO)

## Описание
Добавляем два новых поля в цепочку: Android-repository → HTTP-тело → Server-endpoint.
Этот этап изолирован от логики @@testqueries и ComparisonService — он только расширяет протокол.

## Задачи

### 3.1 Android: RagSearchRepository (domain)
Файл: `app/src/.../repository/RagSearchRepository.kt` (или аналогичный)

Метод `saveRuntestResults` получает два новых параметра:
- `executionTimeMs: Long`
- `isLocalLlm: Boolean`

### 3.2 Android: RagSearchRepositoryImpl (data)
Файл: `app/src/.../repository/RagSearchRepositoryImpl.kt`

- Пробросить `executionTimeMs` и `isLocalLlm` в тело HTTP-запроса

### 3.3 Android: RuntestSaveRequest (network DTO)
Файл: network DTO для `/runtest/save`

Добавить поля: `executionTimeMs: Long`, `isLocalLlm: Boolean`

### 3.4 Server: RuntestSaveRequest (DTO в RagServer.kt)
Файл: `rag-server/src/.../RagServer.kt`

В `private data class RuntestSaveRequest` добавить поля:
- `isLocalLlm: Boolean = false`
- `executionTimeMs: Long = 0`

### 3.5 Server: изменить формирование имени файла
В обработчике `POST /runtest/save` изменить логику построения `reportPath`:

```
val modelLabel = if (request.isLocalLlm) "LOCAL" else "CLOUD"
val reportPath = "./reports/runtest_RAG_WORKER_${modelLabel}_$timestamp.md"
```

### 3.6 Server: добавить executionTimeMs в отчёт
В функции `buildRuntestReport` добавить в заголовок отчёта строку с временем выполнения.

---

## Резюме

**Что получим:** сервер принимает расширенный запрос, сохраняет отчёты с суффиксом LOCAL/CLOUD в имени файла. Android-сторона готова к передаче этих полей.

**Критерии успеха:**
- Существующий `@@talk(rag --runtest ...)` продолжает работать (обратная совместимость — поля optional на сервере)
- При вызове с `isLocalLlm=true` файл создаётся как `runtest_RAG_WORKER_LOCAL_*.md`
- При вызове с `isLocalLlm=false` — `runtest_RAG_WORKER_CLOUD_*.md`
- `executionTimeMs` виден в заголовке отчёта

---

## Подробный план реализации

### Шаг 1. Найти RagSearchRepository

Определить текущую сигнатуру `saveRuntestResults`. Найти domain-интерфейс и impl-класс.

### Шаг 2. Обновить domain-интерфейс

```
suspend fun saveRuntestResults(
    preset: String,
    items: List<RuntestResultItem>,
    serverUrl: String,
    executionTimeMs: Long = 0,
    isLocalLlm: Boolean = false,
): Result<RuntestSaveResult>
```

Default values обеспечивают обратную совместимость с существующими вызовами в `RagCommandHandler`.

### Шаг 3. Обновить impl

Включить `executionTimeMs` и `isLocalLlm` в тело HTTP POST-запроса (JSON).

### Шаг 4. Обновить Android network DTO

Добавить поля в serializable DTO (Retrofit/Ktor/etc), которое используется для `/runtest/save`.

### Шаг 5. Обновить RagServer.kt

В `RuntestSaveRequest`:
```kotlin
@Serializable
private data class RuntestSaveRequest(
    val preset: String,
    val items: List<RuntestItemDto>,
    val isLocalLlm: Boolean = false,
    val executionTimeMs: Long = 0,
)
```

### Шаг 6. Изменить buildReportPath (в обработчике POST /runtest/save)

```kotlin
val modelLabel = if (request.isLocalLlm) "LOCAL" else "CLOUD"
val reportPath = "./reports/runtest_RAG_WORKER_${modelLabel}_$timestamp.md"
```

### Шаг 7. Обновить buildRuntestReport

Добавить в markdown-заголовок:
```
**Время выполнения:** ${request.executionTimeMs} мс
**Модель:** ${if (request.isLocalLlm) "Локальная (Ollama)" else "Облачная"}
```

### Шаг 8. Проверка существующих вызовов

Убедиться, что `RagCommandHandler.handleRuntest` и `handleRunparamtest` компилируются без изменений (используют default values).
