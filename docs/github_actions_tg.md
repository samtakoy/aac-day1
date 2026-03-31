# День 32: Настройка автоматического AI-ревью PR

Описание того, как настроить и проверить пайплайн автоматического ревью Pull Request-ов через GitHub Actions + Telegram + Android.

---

## Как это работает

```
GitHub PR открыт
  → GitHub Actions отправляет JSON в Telegram бот
    → Android (WorkManager, каждую ~1 мин) опрашивает Telegram
      → Находит новый PR → запускает агентный пайплайн ревью
        → Агенты анализируют diff, ищут баги, оставляют комментарии в PR
          → Результат сохраняется в чат-консоль приложения
```

---

## Требования

- GitHub репозиторий (публичный или приватный с токеном)
- Telegram бот (создать через [@BotFather](https://t.me/BotFather))
- Telegram группа или личный чат, где бот является участником
- Android-приложение собрано и запущено
- MCP-сервер запущен локально (или доступен по сети)

---

## Шаг 1: Создать Telegram бота

1. Открыть [@BotFather](https://t.me/BotFather) в Telegram
2. Отправить `/newbot`, задать имя и username (например `pr_review_bot`)
3. Скопировать токен вида `123456789:AABBccDD...` — это `TELEGRAM_BOT_TOKEN`
4. Добавить бота в нужный чат/группу
5. Узнать `TELEGRAM_CHAT_ID`:
   - Написать боту любое сообщение, затем открыть в браузере:
     ```
     https://api.telegram.org/bot{TELEGRAM_BOT_TOKEN}/getUpdates
     ```
     В ответе найти `"chat":{"id": 452474658, ..., "type":"private"}` — это и есть chat_id
   - Для **личного чата** (`"type":"private"`) — ID положительный, например `452474658`
   - Для **группы/супергруппы** (`"type":"supergroup"`) — ID отрицательный, например `-1001234567890`

---

## Шаг 2: Настроить local.properties (Android)

В файле `local.properties` (в корне проекта, **не коммитить в git**) добавить:

```properties
TELEGRAM_BOT_TOKEN=123456789:AABBccDD...
TELEGRAM_CHAT_ID=-100123456789
```

> `TELEGRAM_CHAT_ID` — это Telegram chat_id (не путать с chatId чата в Android-приложении).

После этого пересобрать приложение — токены попадут в `BuildConfig`.

---

## Шаг 3: Настроить GitHub Secrets

В настройках GitHub репозитория: **Settings → Secrets and variables → Actions → New repository secret**

Добавить два секрета:

| Имя | Значение |
|-----|----------|
| `TELEGRAM_BOT_TOKEN` | токен бота из BotFather |
| `TELEGRAM_CHAT_ID` | chat_id чата, куда бот отправит уведомление |

---

## Шаг 4: Настроить GitHub Actions

Файл `.github/workflows/pr_review_notify.yml` уже находится в репозитории.

Убедиться что workflow включён: GitHub → вкладка **Actions** → если workflow отключён, нажать **Enable workflow**.

---

## Шаг 5: Настроить MCP-сервер

MCP-сервер должен знать GitHub токен для вызова GitHub API (инструменты `get_pr_info`, `get_pr_diff` и т.д.):

**Windows (PowerShell):**
```powershell
$env:GITHUB_TOKEN="ghp_..."
$env:GITHUB_OWNER="your-org-or-username"   # опционально
$env:GITHUB_REPO="your-repo-name"          # опционально
```

**macOS/Linux:**
```bash
export GITHUB_TOKEN="ghp_..."
```

Затем запустить сервер обычным способом.

> Токен GitHub должен иметь права: `repo` (для приватных репозиториев) или `public_repo` (для публичных). Для добавления комментариев к PR — право `pull_requests: write`.

---

## Шаг 6: Включить мониторинг в приложении

1. Открыть любую **чат-консоль** в приложении (например AgentConsole)
2. Нажать **⚙️ настройки** чата (иконка в правом верхнем углу)
3. В открывшемся окне настроек найти переключатель **"Мониторинг PR"**
4. Включить переключатель
5. Нажать **OK**

После включения:
- В DataStore сохраняется `chatId` этого чата и модель (используется для агентов ревью)
- Запускается `TelegramPollingWorker` — раз в минуту опрашивает Telegram

> Только один чат может быть привязан к мониторингу. Если включить в другом чате — предыдущий отвязывается автоматически.

---

## Шаг 7: Проверка работы

### 7.1 Проверить что поллинг запущен

В logcat (фильтр по тегу `TelegramPolling`):
```
D/TelegramPolling: пусто
```
Это сообщение должно появляться каждую минуту. Если тишина — поллинг не запущен (проверить шаги 2, 6).

### 7.2 Отправить тестовое сообщение вручную

Для проверки парсинга без создания PR — отправить боту сообщение вручную (из того же чата):

```json
{"event":"pr_opened","repo":"owner/repo","pr_number":1,"title":"Test PR"}
```

В logcat должно появиться:
```
D/TelegramPolling: новый pr: 1
```

И в чат-консоли приложения появятся сообщения о начале ревью.

### 7.3 Создать реальный Pull Request

1. Создать ветку, внести любые изменения, открыть PR в GitHub
2. Через несколько секунд: во вкладке **Actions** репозитория появится выполненный workflow `pr_review_notify`
3. В Telegram чате появится сообщение формата: `{"event":"pr_opened","repo":"...","pr_number":N,"title":"..."}`
4. В течение минуты (следующий цикл поллинга) в чат-консоли приложения появятся сообщения:
   - `🔍 Начинаем ревью PR #N: repo`
   - `📋 PR: название | Файлов: X`
   - `👀 Смотрю файл: path/to/File.kt` (для каждого файла)
   - Инфо-сообщения о tool calls агентов
5. По завершении — большое Bot-сообщение с полным отчётом ревью
6. В самом PR на GitHub появятся inline-комментарии к файлам (если агент нашёл проблемы)

---

## Возможные проблемы

| Симптом | Причина | Решение |
|---------|---------|---------|
| Нет `TelegramPolling: пусто` в logcat | Поллинг не запустился | Открыть настройки чата, убедиться что переключатель включён |
| Поллинг работает, но `{"event":...}` не парсится | Неверный `TELEGRAM_CHAT_ID` | Проверить chat_id через `getUpdates` API |
| `get_pr_info` возвращает 401 | Нет или неверный `GITHUB_TOKEN` | Установить `$env:GITHUB_TOKEN` перед запуском mcp-server |
| `add_pr_review_comment` возвращает 422 | Строка не входит в diff PR | Агент указал строку вне изменённого диапазона — это нормально для первых тестов |
| Агент не находит инструменты `get_pr_info` | MCP-сервер не перезапущен | Перезапустить mcp-server после изменений |
| Workflow в GitHub Actions не запускается | Workflow отключён | Actions → включить workflow |

---

## Архитектура компонентов

```
.github/workflows/pr_review_notify.yml    — GitHub Actions триггер
mcp-server/McpTools.kt                    — инструменты get_pr_info, get_pr_diff,
                                            get_pr_file_diff, add_pr_review_comment

app/core/core_features/pr_review/
  domain/
    model/        PrHandleState, TelegramPrEvent, PrInfoResult
    repository/   PrHandleRepository, TelegramRepository
    usecase/      GetPrHandleStateUseCase, SetPrHandleEnabledUseCase, StartPrReviewUseCase
    worker/       PrReviewWorker (domain, multi-agent pipeline)
  data/
    PrHandleRepositoryImpl  — DataStore
    TelegramRepositoryImpl  — Telegram Bot API (getUpdates)
    PrModelSettingsDto      — сериализация ModelSettings
    worker/
      TelegramPollingWorker — CoroutineWorker, цепочка 1 мин

app/features/console/
  ChatSettingsUiModel       — поле handlePr: Boolean
  ChatSettingsView          — Switch "Мониторинг PR"
  ConsoleViewModelImpl      — flow подписка + HandlePrToggled event
```

--

## Мониторинг в GitHub

Для события pull_request GitHub Actions берёт workflow из базовой ветки PR (та, в которую мержишь).

Если открываешь PR feature → main, то .github/workflows/pr_review_notify.yml должен лежать в main.

Порядок действий:

Смержи (или черзнапрямую запушь) файл workflow в main:


git checkout main
git merge day32  # или скопировать файл вручную
git push origin main
После этого любой новый PR, открытый в main, будет триггерить workflow.

Проверить что всё ок:

GitHub → вкладка Actions → должен появиться workflow pr_review_notify в списке слева
Открыть тестовый PR из любой ветки в main → во вкладке Actions появится запуск
Если workflow лежит только в day32 — он не сработает на PR, потому что GitHub смотрит на файлы базовой ветки.