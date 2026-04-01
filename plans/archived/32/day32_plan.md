# День 32. Автоматизация ревью кода — Итоговый план

## Продуктовая цель

Автоматический AI-ревью Pull Request-ов в GitHub:
- GitHub Actions уведомляет Android-приложение через Telegram Bot API о новом PR
- Android периодически опрашивает Telegram и запускает многоагентный пайплайн ревью
- Результаты ревью (потенциальные баги, архитектурные проблемы, рекомендации) сохраняются в чат-консоль

---

## Архитектура решения (схема взаимодействия)

```
GitHub PR opened
  → GitHub Actions (.github/workflows/pr_review_notify.yml)
      → POST Telegram Bot API: sendMessage(chat_id, JSON-сообщение)

Android (каждые ~1 мин):
  TelegramPollingWorker (CoroutineWorker)
    → TelegramRepository.getPrUpdates(offset)
      → GET api.telegram.org/bot{token}/getUpdates?offset={id}
    → Если новый PR → StartPrReviewUseCase(prNumber, repo)
        → chatTools.addInfoMessage(chatId, "Начинаем ревью PR #N")
        → Notification "Начинаем ревью PR #N"
        → PrReviewWorker.doWork(prNumber, repo, chatId)
            → JustWorkWorker [pr_info_collector]
                Tools: get_pr_info, get_pr_diff
                → PrInfoResult(title, description, headSha, files)
            → for each file:
                JustWorkWorker [pr_file_reviewer]
                Tools: search_codebase, get_git_file_list, get_file_content,
                       get_pr_file_diff, add_pr_review_comment
                → FileReviewResult(filePath, reportText)
            → JustWorkWorker [pr_summary_agent]
                → финальное резюме
        → chatTools.addBotMessage(chatId, fullReport)
        → Notification "Ревью завершено"
    → saveLastTelegramUpdateId(updateId)
    → Schedule next TelegramPollingWorker in 1 min
```

---

## Формат Telegram-сообщения от GitHub Actions

Чистый JSON в поле `text`:
```
{"event":"pr_opened","repo":"owner/repo","pr_number":42,"title":"Fix authentication bug"}
```

---

## Новые сущности

### 1. GitHub Actions Workflow

**Файл:** `.github/workflows/pr_review_notify.yml`

- Триггер: `pull_request` на события `opened`, `synchronize`
- Секреты репозитория: `TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID`
- Шаг: curl → POST Telegram Bot API sendMessage

---

### 2. MCP Server: новые и изменённые инструменты

**Файл:** `mcp-server/src/main/kotlin/com/example/day/mcpserver/tools/McpTools.kt`

#### Новые инструменты (GitHub Pull Request API)

**`get_pr_info`**
- Параметры: `pr_number: Int`, `repo: String`
- Возвращает JSON: `{number, title, description, state, author, head_sha, files: [{path, status, additions, deletions}]}`
- GitHub API: `GET /repos/{repo}/pulls/{pr_number}` + `GET /repos/{repo}/pulls/{pr_number}/files`

**`get_pr_diff`**
- Параметры: `pr_number: Int`, `repo: String`
- Возвращает: полный diff PR как text
- GitHub API: `GET /repos/{repo}/pulls/{pr_number}` с заголовком `Accept: application/vnd.github.v3.diff`

**`get_pr_file_diff`**
- Параметры: `pr_number: Int`, `repo: String`, `file_path: String`
- Возвращает: patch (diff) конкретного файла
- GitHub API: `GET /repos/{repo}/pulls/{pr_number}/files` → фильтровать по `filename == file_path` → вернуть `patch`

**`add_pr_review_comment`**
- Параметры: `pr_number: Int`, `repo: String`, `file_path: String`, `body: String`, `line: Int`, `commit_id: String`
- Возвращает: JSON подтверждение `{id, status}`
- GitHub API: `POST /repos/{repo}/pulls/{pr_number}/comments`

#### Изменённые инструменты

**`get_git_file_list`**
- Новый опциональный параметр: `pattern: String?`
- Если задан — выполняет `git ls-files "{pattern}"`, иначе — `git ls-files` (поведение без изменений)

#### Новые константы в McpTools или McpToolNames

- `GET_PR_INFO`, `GET_PR_DIFF`, `GET_PR_FILE_DIFF`, `ADD_PR_REVIEW_COMMENT`

---

### 3. Android App: новые сущности

#### BuildConfig (app/build.gradle.kts)

Новые поля из `local.properties`:
- `TELEGRAM_BOT_TOKEN: String`
- `TELEGRAM_CHAT_ID: String` (Telegram chat_id, может быть отрицательным для групп)

#### Domain Models

**`PrHandleState`**
- Поля: `isEnabled: Boolean`, `chatId: Long`, `modelSettings: ModelSettings?`

**`TelegramPrEvent`**
- Поля: `updateId: Long`, `prNumber: Int`, `repo: String`, `title: String`

#### DataStore (расширение AppSettings)

Новые ключи в `AppSettings`:
- `HANDLE_PR_ENABLED: Boolean` (default: false)
- `HANDLE_PR_CHAT_ID: Long` (default: -1)
- `TELEGRAM_LAST_UPDATE_ID: Long` (default: 0)
- `HANDLE_PR_MODEL_SETTINGS: String` (JSON, default: null) — `PrModelSettingsDto` сериализованный

`ModelSettings` не `@Serializable` (содержит `ImmutableList`), поэтому для DataStore используется DTO `PrModelSettingsDto` (только примитивные поля, без `stopSequence`).

Новые методы в `AppSettings`:
- `getPrHandleStateFlow(): Flow<PrHandleState>`
- `setPrHandleState(isEnabled: Boolean, chatId: Long)`
- `getLastTelegramUpdateId(): Long` (suspend)
- `saveLastTelegramUpdateId(id: Long)` (suspend)

#### Repositories

**`PrHandleRepository`** (interface)
- `getPrHandleStateFlow(): Flow<PrHandleState>`
- `setPrHandleState(isEnabled: Boolean, chatId: Long)`
- `getLastTelegramUpdateId(): Long`
- `saveLastTelegramUpdateId(id: Long)`

**`PrHandleRepositoryImpl`** (data, @Singleton) — делегирует в `AppSettings`

**`TelegramRepository`** (interface)
- `getPrUpdates(offset: Long): Result<List<TelegramPrEvent>>`

**`TelegramRepositoryImpl`** (data, @Singleton)
- Инжектирует: `HttpClient`, `AppSettings` (для чтения `TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID`)
- GET `https://api.telegram.org/bot{token}/getUpdates?offset={offset}&limit=10`
- Парсит ответ, фильтрует по `chat_id == TELEGRAM_CHAT_ID`, десериализует JSON текст сообщения в `TelegramPrEvent`

#### Use Cases

**`GetPrHandleStateUseCase`**
- Возвращает: `Flow<PrHandleState>`
- Делегирует в `PrHandleRepository.getPrHandleStateFlow()`

**`SetPrHandleEnabledUseCase`**
- Параметры: `isEnabled: Boolean`, `chatId: Long`, `modelSettings: ModelSettings? = null`
- Записывает состояние в `PrHandleRepository`
- Если `isEnabled == true` → запускает `TelegramPollingWorker` через `WorkManager.enqueueUniqueWork(KEEP)`
- Если `isEnabled == false` → `WorkManager.cancelUniqueWork("telegram_polling")`

**`StartPrReviewUseCase`**
- Параметры: `prNumber: Int`, `repo: String`
- Читает `chatId` из `PrHandleRepository`
- Добавляет инфо-сообщение в чат: "🔍 Начинаем ревью PR #N: title"
- Показывает Android Notification: "Начинаем ревью PR #N"
- Вызывает `PrReviewWorker.doWork(prNumber, repo, chatId)`
- По завершении добавляет инфо-сообщение и notification о результате

#### TelegramPollingWorker (CoroutineWorker)

**Класс:** `TelegramPollingWorker : CoroutineWorker`
**Расположение:** `core/core_features/pr_review/data/worker/`

`doWork()`:
1. Получает `appComponent` из `MyApp`
2. Вызывает `TelegramRepository.getPrUpdates(lastUpdateId + 1)`
3. `Log.d("TelegramPolling", "пусто" | "новый pr: N")` — heartbeat
4. Если есть новые PR события: вызывает `StartPrReviewUseCase(prNumber, repo)`
5. Сохраняет `lastUpdateId` через `PrHandleRepository`
6. Планирует следующий `TelegramPollingWorker` через `WorkManager.enqueueUniqueWork("telegram_polling", REPLACE, OneTimeWorkRequest с InitialDelay(1 min))`
7. Возвращает `Result.success()`

Константа имени: `WORK_NAME = "telegram_polling"`

#### PrReviewWorker (domain, injectable, НЕ CoroutineWorker)

**Класс:** `PrReviewWorker`
**Расположение:** `core/core_features/pr_review/domain/worker/`
**Инжектирует:** `JustWorkWorker`, `ChatTools`, `AgentRepository`, `ModelSettings provider`

**`doWork(prNumber: Int, repo: String, chatId: Long): Result<String>`**

Внутренняя логика:
1. **Агент 1: `pr_info_collector`** — собирает данные о PR (файлы, title, description, head_sha). Allowed tools: `get_pr_info`, `get_pr_diff`. Ожидается JSON-ответ.
2. **Парсинг ответа** → `PrInfoResult(title, description, headSha, files: List<PrFileInfo>)`
3. **Цикл по файлам** — для каждого `PrFileInfo`:
   - Агент `pr_file_reviewer` (пересоздаётся — `recreateAgent = true`)
   - Allowed tools: `search_codebase`, `get_git_file_list`, `get_file_content`, `get_pr_file_diff`, `add_pr_review_comment`
   - Добавляет инфо-сообщение до старта: "👀 Смотрю файл: path/to/File.kt"
   - Результат накапливается в `List<String>`
4. **Агент: `pr_summary_agent`** — пишет итоговое резюме из всех накопленных результатов. Нет tools.
5. Собирает полный отчёт и сохраняет в чат как Bot-сообщение

**Внутренняя модель:**

`PrInfoResult`
- Поля: `title: String`, `description: String`, `headSha: String`, `files: List<PrFileInfo>`

`PrFileInfo`
- Поля: `path: String`, `status: String` (added/modified/removed)

#### DI: новые компоненты/модули

Новый feature package: `core/core_features/pr_review/`
- `domain/` — `PrHandleRepository`, `TelegramRepository`, `PrReviewWorker`, use cases, models
- `data/` — `PrHandleRepositoryImpl`, `TelegramRepositoryImpl`, `worker/TelegramPollingWorker`
- `di/` — `PrReviewModule` (bindings), extensions для `AppComponent`

AppComponent экспозиции (новые методы):
- `telegramRepository(): TelegramRepository`
- `prHandleRepository(): PrHandleRepository`
- `startPrReviewUseCase(): StartPrReviewUseCase`

#### UI Changes

**`ChatSettingsUiModel`** — добавить поле:
- `handlePr: Boolean`

**`ChatSettingsView`** — добавить переключатель (Switch) для `handlePr`

**`ConsoleViewModelImpl`** — изменения:
- Новый flow: `GetPrHandleStateUseCase().invoke()` → обновляет `settings.handlePr`
- Новый Event: `HandlePrToggled(isEnabled: Boolean)`
- Обработчик: `SetPrHandleEnabledUseCase(isEnabled, chatId)`

---

## Этапы реализации

| # | Название | Файл |
|---|----------|------|
| 1 | GitHub Actions + MCP Server GitHub API tools | [day32_stage1_infra.md](day32_stage1_infra.md) |
| 2 | Android Data Layer: BuildConfig, DataStore, Repositories, DI | [day32_stage2_data.md](day32_stage2_data.md) |
| 3 | TelegramPollingWorker + StartPrReviewUseCase | [day32_stage3_polling.md](day32_stage3_polling.md) |
| 4 | PrReviewWorker: многоагентный пайплайн ревью | [day32_stage4_review.md](day32_stage4_review.md) |
| 5 | UI: ChatSettings + ConsoleViewModelImpl | [day32_stage5_ui.md](day32_stage5_ui.md) |
