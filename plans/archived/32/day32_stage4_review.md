# Этап 4: PrReviewWorker — многоагентный пайплайн ревью

## Общее описание

Создание domain-воркера `PrReviewWorker`, который последовательно запускает агентов для сбора информации о PR, ревью каждого файла и написания итогового резюме. Использует `JustWorkWorker` для работы с агентами — по аналогии с `InvestigateGitFileTool`.

**Зависимости:** Этап 2 (data layer), Этап 3 (StartPrReviewUseCase — нужно подключить PrReviewWorker к нему)

**Что получим:**
- `PrReviewWorker` — injectable domain-класс, запускает 3 вида агентов
- `PrInfoResult`, `PrFileInfo` — внутренние модели для передачи данных между агентами
- Подключение `PrReviewWorker` в `StartPrReviewUseCase` (убрать заглушку из Этапа 3)
- Промпты для трёх типов агентов

**Критерии успеха:**
- Запуск ревью тестового PR → в чате появляются:
  - инфо-сообщение "👀 Смотрю файл: ..."
  - инфо-сообщения от tool calls (tool_event_start / tool_event_result)
  - финальное Bot-сообщение с полным отчётом
- Если PR содержит потенциальные баги → в GitHub PR появляются review comments

---

## Задача 4.1: Domain-модели для передачи данных

### Файл для создания

**`app/src/main/java/com/example/day/core/core_features/pr_review/domain/model/PrInfoResult.kt`**

```kotlin
import kotlinx.serialization.Serializable

@Serializable
data class PrInfoResult(
    val title: String,
    val description: String,
    val headSha: String,
    val files: List<PrFileInfo>
)

@Serializable
data class PrFileInfo(
    val path: String,
    val status: String  // "added", "modified", "removed", "renamed"
)
```

---

## Задача 4.2: PrReviewWorker

### Файл для создания

**`app/src/main/java/com/example/day/core/core_features/pr_review/domain/worker/PrReviewWorker.kt`**

- Аннотация: `@Inject constructor` (не CoroutineWorker, не AWorker)
- Инжектирует:
  - `JustWorkWorker`
  - `ChatTools`
  - `AgentRepository` (для удаления агентов перед каждым запуском)
  - `Json` (для парсинга JSON-ответов агентов)
  - `PrHandleRepository` — для чтения `ModelSettings` сохранённых при включении галочки

### Метод `doWork(prNumber: Int, repo: String, chatId: Long): Result<String>`

#### Подготовка: получить ModelSettings

В начале `doWork()`, перед запуском агентов:

```kotlin
val prState = prHandleRepository.getPrHandleStateFlow().first()
val modelSettings = prState.modelSettings ?: ModelSettings.default()
```

`modelSettings` передаётся в `JustWorkConfig.defaultModel = { modelSettings }` для **всех** агентов в пайплайне. Таким образом все агенты ревью используют ту же модель, что была настроена в чате на момент включения мониторинга.

#### Шаг 1: Агент-сборщик информации о PR

```
Имя агента: "pr_info_collector"
recreateAgent: true
allowedTools: ["get_pr_info", "get_pr_diff"]
```

**Промпт (userPrompt):**
```
Собери информацию о Pull Request #$prNumber в репозитории $repo.

Используй инструменты get_pr_info и get_pr_diff.

Верни ТОЛЬКО валидный JSON (без markdown, без пояснений) в следующем формате:
{
  "title": "название PR",
  "description": "описание PR (может быть пустым)",
  "headSha": "sha коммита",
  "files": [
    {"path": "путь/к/файлу.kt", "status": "modified"}
  ]
}

Статусы файлов: added, modified, removed, renamed.
Включи все изменённые файлы. Это важно для последующего ревью.
```

**Системный промпт** (задаётся через `JustWorkConfig.systemPrompt`):
```
Ты помощник по сбору данных о Pull Request. 
Твоя задача — вызвать инструменты и вернуть структурированный JSON с информацией о PR.
Не добавляй пояснений, выводи только JSON.
```

**Обработка результата:**
- Попытаться `json.decodeFromString<PrInfoResult>(responseText.trim())`
- Если парсинг неудачен — вернуть `Result.failure(Exception("Не удалось получить данные о PR: $responseText"))`
- Добавить инфо в чат: "📋 PR: ${prInfoResult.title} | Файлов: ${prInfoResult.files.size}"

#### Шаг 2: Цикл ревью по каждому файлу

Для каждого `PrFileInfo` из `prInfoResult.files`:

**Инфо-сообщение до запуска агента:**
```kotlin
chatTools.addInfoMessage(chatId, "👀 Смотрю файл: ${file.path}")
```

**Агент:**
```
Имя агента: "pr_file_reviewer"
recreateAgent: true  ← ВАЖНО: каждый раз новый агент с чистой историей
allowedTools: [
    "search_codebase",
    "get_git_file_list",
    "get_file_content",
    "get_pr_file_diff",
    "add_pr_review_comment"
]
```

**Промпт (userPrompt):**
```
Проведи ревью изменений в файле "${file.path}" из Pull Request #$prNumber в репозитории $repo.

Информация о PR:
- Название: ${prInfoResult.title}
- Описание: ${prInfoResult.description}
- HEAD SHA: ${prInfoResult.headSha}

Что нужно сделать:
1. Получи diff этого файла с помощью get_pr_file_diff (pr_number=$prNumber, repo=$repo, file_path="${file.path}")
2. По необходимости изучи контекст: используй search_codebase для поиска связанных классов/методов, get_file_content для просмотра зависимостей
3. Если видишь конкретную проблему в конкретной строке — оставь комментарий через add_pr_review_comment (commit_id="${prInfoResult.headSha}")

НЕЗАВИСИМО от результатов вызова инструментов — обязательно выдай структурированный отчёт:

## Файл: ${file.path}
### Потенциальные баги
(список или "не обнаружено")
### Архитектурные проблемы  
(список или "не обнаружено")
### Рекомендации
(список или "нет рекомендаций")
### Что было сделано
(краткое описание твоих действий)
```

**Системный промпт:**
```
Ты опытный Kotlin-разработчик, проводишь ревью кода Android-приложения.
Проект использует Clean Architecture, Dagger, Jetpack Compose, Room, Kotlin Coroutines.
Будь конкретен: указывай строки кода, имена методов, объясняй почему это проблема.
Если что-то выглядит нормально — скажи об этом явно, не придумывай проблемы.
```

**Накопление результатов:**
```kotlin
val fileReports = mutableListOf<String>()
// ...внутри цикла:
result.onSuccess { report -> fileReports.add("### ${file.path}\n$report") }
result.onFailure { error -> fileReports.add("### ${file.path}\nОшибка ревью: ${error.message}") }
```

#### Шаг 3: Агент-резюмировщик

```
Имя агента: "pr_summary_agent"
recreateAgent: true
allowedTools: []  ← без инструментов
```

**Промпт (userPrompt):**
```
По результатам ревью Pull Request #$prNumber "${prInfoResult.title}" в репозитории $repo напиши краткое резюме.

Результаты ревью файлов:
${fileReports.joinToString("\n\n---\n\n")}

Структура резюме:
## Резюме ревью PR #$prNumber: ${prInfoResult.title}
**Изменено файлов:** ${prInfoResult.files.size}

### 🐛 Потенциальные баги
(список наиболее критичных проблем из всех файлов, или "не обнаружено")

### 🏗️ Архитектурные проблемы
(список, или "не обнаружено")

### 💡 Рекомендации
(список наиболее важных рекомендаций)

### Общая оценка
(1-2 предложения: можно ли мержить, что нужно исправить)
```

**Системный промпт:**
```
Ты технический лид, пишешь итоговое резюме code review.
Будь лаконичен. Выдели самое важное. Не повторяй всё что сказано в ревью — только ключевое.
```

#### Шаг 4: Финальный отчёт в чат

Собрать полный отчёт:
```kotlin
val fullReport = buildString {
    appendLine("# Ревью PR #$prNumber: ${prInfoResult.title}")
    appendLine("**Репозиторий:** $repo | **Файлов:** ${prInfoResult.files.size}")
    appendLine()
    fileReports.forEach { appendLine(it); appendLine() }
    appendLine("---")
    appendLine(summaryText)
}
chatTools.addBotMessage(chatId, fullReport)
```

Вернуть `Result.success(fullReport)`

---

## Задача 4.3: Константы имён агентов

### Файл для создания (опционально, можно inline)

**`app/src/main/java/com/example/day/core/core_features/pr_review/domain/worker/PrAgentNames.kt`**

```kotlin
object PrAgentNames {
    const val INFO_COLLECTOR = "pr_info_collector"
    const val FILE_REVIEWER = "pr_file_reviewer"
    const val SUMMARY_AGENT = "pr_summary_agent"
}
```

---

## Задача 4.4: Подключение в StartPrReviewUseCase

### Файл для изменения

**`app/src/main/java/com/example/day/core/core_features/pr_review/domain/usecase/StartPrReviewUseCase.kt`**

Убрать заглушку из Этапа 3. Добавить `PrReviewWorker` в конструктор и заменить заглушку на реальный вызов (код уже описан в Этапе 3, задача 3.3).

---

## Задача 4.5: DI — добавить PrReviewWorker

### Файл для изменения

**`app/src/main/java/com/example/day/core/core_features/pr_review/di/PrReviewModule.kt`**

`PrReviewWorker` использует `@Inject constructor`, так что Dagger создаст его автоматически. Явного биндинга не нужно. Убедиться что все зависимости `PrReviewWorker` (в первую очередь `JustWorkWorker`) доступны в scope.

`JustWorkWorker` уже имеет `@Inject constructor` и использует зависимости из AppComponent. Должна работать без дополнительных биндингов.

---

## Задача 4.6: ModelSettings для агентов ревью

`JustWorkConfig.defaultModel: () -> ModelSettings` передаётся как `{ modelSettings }`, где `modelSettings` — переменная из начала `doWork()` (прочитана из `PrHandleRepository`, fallback — `ModelSettings.default()`).

Инжектировать через конструктор `ModelSettings` **не нужно** — значение читается динамически из DataStore при каждом запуске ревью.

---

## Структура файлов этапа

```
app/src/main/java/com/example/day/core/core_features/pr_review/
└── domain/
    ├── model/
    │   └── PrInfoResult.kt                  (новый: PrInfoResult + PrFileInfo)
    └── worker/
        ├── PrReviewWorker.kt                (новый)
        └── PrAgentNames.kt                  (новый, опционально)

app/src/.../pr_review/domain/usecase/StartPrReviewUseCase.kt  (изменить — убрать заглушку)
```

---

## Примечания

### Про recreateAgent = true
Каждый агент в цикле должен начинать с чистой историей. `JustWorkConfig.recreateAgent = true` вызывает `agentRepository.deleteAgent(systemName, chatId)` перед созданием. Это критично — иначе агент "помнит" предыдущий файл и путается.

### Про обработку ошибок
Даже если агент по одному файлу вернул ошибку — продолжать цикл, добавить запись об ошибке в `fileReports`. Финальный отчёт всё равно собирать и сохранять в чат.

### Про размер контекста
PR с 20+ файлами может вызвать проблемы с длиной итогового промпта для резюмировщика. На данном этапе ограничений не вводить — это демо. В продакшне нужно ограничивать.

### Про MCP tools names
Константы имён MCP инструментов для `allowedTools` должны совпадать с названиями в `McpTools.kt` сервера. Проверить актуальные имена в:
- `mcp-server/src/main/kotlin/com/example/day/mcpserver/tools/McpTools.kt` — `get_pr_info`, `get_pr_diff`, `get_pr_file_diff`, `add_pr_review_comment` (новые из Этапа 1)
- `rag-server/src/main/kotlin/.../tools/RagTools.kt` — `search_codebase`
- Существующие: `get_git_file_list`, `get_file_content`
