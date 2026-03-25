# RAG MCP Server — Инструкция по настройке и запуску

Сервер индексирует Kotlin/Markdown файлы кодовой базы, генерирует эмбеддинги и предоставляет:
- **MCP-тулы** для AI-агента (search_codebase_smart и др.)
- **HTTP REST API** для Android-приложения (`/search`, `/evaluate`)

| Тул | Описание | Когда использовать |
|-----|----------|---|
| `search_codebase` | Hybrid поиск по структурным блокам (функции, классы) | Конкретный класс или метод по имени |
| `search_codebase_fixed` | Hybrid поиск по чанкам фиксированного размера | Широкий контекстный поиск |
| `search_codebase_smart` | 2-Stage: сначала классы по домену, потом методы | Концептуальные вопросы ("как работает X") |
| `get_index_status` | Статус индекса | Проверить готовность перед поиском |

---

## Требования

- JDK 17+
- Gradle 8.5+ (или использовать `./gradlew` из корня проекта)
- Ollama (для локальных эмбеддингов и LLM) **или** ключ OpenRouter (только для embeddings)

---

## 1. Настройка Ollama

```bash
# Установить Ollama (macOS)
brew install ollama

# Запустить сервер Ollama
ollama serve

# Скачать embedding-модель (обязательно)
ollama pull nomic-embed-text

# Скачать LLM (нужна для метаданных / query optimization / reranking)
ollama pull qwen2_5-coder_7b-instruct
```

Проверить embedding-модель:
```bash
curl http://localhost:11434/api/embeddings \
  -d '{"model":"nomic-embed-text","prompt":"hello world"}'
# Ожидаемый ответ: {"embedding":[0.123, ...]} (массив из 768 чисел)
```

---

## 2. Переменные окружения

### Основные

| Переменная | Обязательная | По умолчанию | Описание |
|-----------|:---:|---|---|
| `CODE_PATH` | **да** | — | Путь к директории с исходниками (`.kt`, `.kts`, `.md`) |
| `DB_PATH` | нет | `./rag_index.db` | Путь к SQLite-файлу индекса |
| `EMBEDDING_PROVIDER` | нет | `ollama` | `ollama` или `openrouter` |
| `OLLAMA_BASE_URL` | нет | `http://localhost:11434` | URL Ollama |
| `EMBEDDING_MODEL` | нет | `nomic-embed-text` | Название embedding-модели |
| `OPENROUTER_API_KEY` | если provider=openrouter | — | Ключ OpenRouter API |
| `RAG_SERVER_PORT` | нет | `3001` | Порт HTTP-сервера |
| `FORCE_REINDEX` | нет | `false` | `true` — принудительно перестроить индекс |
| `SEARCH_TOP_K` | нет | `5` | Дефолтный top-K для MCP-тулов |

### Метаданные и 2-Stage поиск

| Переменная | Обязательная | По умолчанию | Описание |
|-----------|:---:|---|---|
| `EXTRACT_METADATA` | нет | `false` | `true` — включить LLM-анализ классов при индексации |
| `LLM_MODEL` | нет | `qwen2.5-coder:7b-instruct` | Ollama-модель для анализа кода |

> Увеличивает время первичной индексации (~1–3 сек на класс). Инкрементальная: уже обработанные пропускаются.

### Query Optimization (rewrite + translate)

| Переменная | Обязательная | По умолчанию | Описание |
|-----------|:---:|---|---|
| `TRANSLATE_QUERIES` | нет | `false` | `true` — включить query optimizer (rewrite + перевод) |
| `TRANSLATE_LLM_MODEL` | нет | значение `LLM_MODEL` | Модель для оптимизации запросов (можно быстрее основной) |

> Включать если запросы на русском. Оптимизирует любые запросы: переводит + добавляет технические ключевые слова.
> Активируется через env (`TRANSLATE_QUERIES=true`) + query param (`enable_query_optimize=true`).
> Если env=false, но query param=true — шаг пропускается с логом в консоль.

### LLM Reranker

| Переменная | Обязательная | По умолчанию | Описание |
|-----------|:---:|---|---|
| `RERANKER_LLM_MODEL` | нет | значение `LLM_MODEL` | Модель для LLM reranker. Можно задать быструю: `qwen2.5:3b` |

> Reranker LLM создаётся всегда при старте (cheap операция). Сетевые вызовы только при активном реранке.

### TaskState (память задачи)

| Переменная | Обязательная | По умолчанию | Описание |
|-----------|:---:|---|---|
| `TASK_STATE_LLM_MODEL` | нет | значение `TRANSLATE_LLM_MODEL` | Модель для обновления TaskState (`POST /task-state/update`). Рекомендуется быстрая: `qwen2.5:3b` |

> TaskState обновляется при каждом сообщении в RAG-чате. Отслеживает текущий файл/класс/метод, intent и историю решений.

---

## 3. Сборка

```bash
./gradlew :rag-server:build
```

Jar: `rag-server/build/libs/rag-server.jar`

---

## 4. Варианты запуска

### Минимальный (только embedding поиск)

```bash
export CODE_PATH="/path/to/your/kotlin/project/src"
java -jar rag-server/build/libs/rag-server.jar
```

### С метаданными (рекомендуется)

```bash
export CODE_PATH="/path/to/your/kotlin/project/src"
export EXTRACT_METADATA=true
export LLM_MODEL=qwen2_5-coder_7b-instruct
java -jar rag-server/build/libs/rag-server.jar
```

### Полный (метаданные + query optimization + reranking + TaskState)

```bash
export CODE_PATH="/path/to/your/kotlin/project/src"
export EXTRACT_METADATA=true
export LLM_MODEL=qwen2_5-coder_7b-instruct
export TRANSLATE_QUERIES=true
export TRANSLATE_LLM_MODEL=qwen2.5:3b   # быстрая модель для оптимизации
export RERANKER_LLM_MODEL=qwen2.5:3b    # быстрая модель для reranker
export TASK_STATE_LLM_MODEL=qwen2.5:3b  # быстрая модель для TaskState
java -jar rag-server/build/libs/rag-server.jar
```

### Через Gradle

```bash
export CODE_PATH="/path/to/your/kotlin/project/src"
./gradlew :rag-server:run
```

---

## 5. HTTP REST API

### GET /search — Поиск по кодовой базе

```
GET /search?query=<текст>&[параметры pipeline]
```

#### Pipeline параметры

| Параметр | Тип | По умолчанию | Описание |
|----------|-----|---|---|
| `preset` | String | — | Именованный пресет (базовая конфигурация). Индивидуальные параметры переопределяют поверх. |
| `retrieval_strategy` | String | `two_stage` | `two_stage` — двухэтапный поиск; `hybrid` — embedding+keyword |
| `chunking_strategy` | String | `structural` | `structural` или `fixed` (только для `hybrid`) |
| `retrieval_topK` | Int | `10` | Top-K ДО фильтрации — сколько кандидатов достаём |
| `threshold` | Double | `0.0` | Порог similarity (0.0 = фильтр выключен; рекомендуется 0.5–0.65) |
| `rerank_strategy` | String | `none` | `none`, `heuristic`, `llm` |
| `final_topK` | Int | `5` | Top-K ПОСЛЕ фильтра и реранка — сколько передаём в LLM |
| `enable_query_optimize` | Boolean | `false` | Включить query rewrite + translation (требует `TRANSLATE_QUERIES=true`) |
| `task_state` | String | — | JSON TaskState из Android-клиента — используется QueryOptimizer для точного rewrite абстрактных запросов |
| `history` | String | — | Краткая история диалога (до 3 пар USER/ASSISTANT) — передаётся в QueryOptimizer для контекстного rewrite |

#### Именованные пресеты

| Пресет | retrieval_topK | threshold | rerank | final_topK | Описание |
|--------|:-:|:-:|---|:-:|---|
| `baseline` | 10 | — | none | 5 | Текущий baseline, только двухэтапный поиск |
| `filtered` | 15 | 0.65 | none | 5 | +threshold filter: отсекаем нерелевантных |
| `reranked_heuristic` | 15 | 0.50 | heuristic | 5 | +keyword overlap bonus |
| `reranked_llm` | 15 | 0.50 | llm | 5 | +LLM оценивает релевантность каждого чанка |

#### Примеры

```bash
# Baseline (без фильтра и реранка)
curl "http://localhost:3001/search?query=как+работает+ContextPacker"

# Пресет filtered
curl "http://localhost:3001/search?query=how+does+ContextPacker+work&preset=filtered"

# LLM reranking с query optimization
curl "http://localhost:3001/search?query=как+работает+упаковка+контекста&preset=reranked_llm&enable_query_optimize=true"

# Кастомные параметры поверх пресета
curl "http://localhost:3001/search?query=embedding+search&preset=filtered&threshold=0.7&final_topK=3"

# Hybrid retrieval
curl "http://localhost:3001/search?query=ContextPacker&retrieval_strategy=hybrid&chunking_strategy=structural&retrieval_topK=20&final_topK=5"
```

#### Формат ответа

```
Pipeline: RERANKED_LLM | Retrieved: 15 → Filtered: 9 → Final: 5
Timings: retrieve=312ms, filter=0ms, rerank=2100ms, top_k=0ms, pack=1ms

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] ContextPacker
File: search/context/ContextPacker.kt
Score: 0.841
...
```

---

### POST /task-state/update — Обновление памяти задачи

Используется Android-клиентом при каждом сообщении в RAG-чате. Обновляет TaskState через Ollama LLM.

```bash
curl -X POST http://localhost:3001/task-state/update \
  -H "Content-Type: application/json" \
  -d '{
    "currentState": "{\"current_focus\":{\"file\":\"\",\"class\":\"\",\"method\":\"\"},\"tech_stack\":\"\",\"intent\":\"general\",\"context_switched\":false,\"confirmed_decisions\":[],\"open_questions\":[]}",
    "lastMessages": [
      {"role": "user", "content": "Как работает AuthService?"},
      {"role": "assistant", "content": "AuthService использует JWT токены..."}
    ]
  }'
```

#### Ответ

```json
{
  "updatedState": "{\"current_focus\":{\"file\":\"AuthService.kt\",\"class\":\"AuthService\",\"method\":\"\"},\"tech_stack\":\"\",\"intent\":\"general\",\"context_switched\":false,\"confirmed_decisions\":[],\"open_questions\":[]}",
  "lastResponseSummary": "AuthService uses JWT tokens for authentication and stores them in SharedPreferences."
}
```

> `lastResponseSummary` — краткое резюме последнего ответа ассистента. Используется клиентом как short history entry.
> Модель задаётся через `TASK_STATE_LLM_MODEL` (по умолчанию — `TRANSLATE_LLM_MODEL`).

---

### POST /evaluate — Автоматизированное тестирование

Запускает тестовые вопросы через один или несколько pipeline-пресетов. Сохраняет MD-отчёты на сервере.

```bash
# Все 4 пресета
curl -X POST http://localhost:3001/evaluate \
  -H "Content-Type: application/json" \
  -d '{
    "questions": [
      "Как работает двухэтапный поиск?",
      "Где хранятся эмбеддинги?",
      "Как работает ContextPacker?"
    ],
    "presets": ["all"]
  }'

# Отдельные пресеты
curl -X POST http://localhost:3001/evaluate \
  -H "Content-Type: application/json" \
  -d '{
    "questions": ["Как работает ContextPacker?"],
    "presets": ["baseline", "reranked_llm"]
  }'
```

#### Ответ

```json
{
  "savedReports": [
    "reports/eval_BASELINE_2024-03-18_14-30.md",
    "reports/eval_FILTERED_2024-03-18_14-30.md",
    "reports/eval_RERANKED_HEURISTIC_2024-03-18_14-30.md",
    "reports/eval_RERANKED_LLM_2024-03-18_14-30.md"
  ],
  "summary": "4 пресета(ов) × 3 вопросов\nBASELINE avg: 0.72 | FILTERED avg: 0.68 | ..."
}
```

Отчёты сохраняются в `./reports/` относительно рабочей директории сервера.

---

### Из Android-приложения

```
@@talk(rag --gentest)                          # все 4 пресета
@@talk(rag --gentest baseline)                 # один пресет
@@talk(rag --gentest filtered,reranked_llm)    # несколько через запятую
```

---

## 6. Отчёты Evaluation

Файлы сохраняются в `./reports/eval_{PRESET}_{yyyy-MM-dd_HH-mm}.md`.

Каждый отчёт содержит:
- Конфигурацию пресета
- Для каждого вопроса: оптимизированный запрос, метрики pipeline, RAG-контекст
- Summary таблицу с avg scores

Для сравнения пресетов — смотреть строку `Summary` в каждом файле или `summary` в JSON-ответе `/evaluate`.

---

## 7. MCP API (для AI-агента)

### Инициализация сессии

```bash
curl -N -X POST http://localhost:3001/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -D- \
  -d '{
    "jsonrpc": "2.0", "id": 1, "method": "initialize",
    "params": {
      "protocolVersion": "2024-11-05",
      "capabilities": {},
      "clientInfo": {"name": "test", "version": "1.0"}
    }
  }'
```

В ответе в заголовках: `Mcp-Session-Id: <session_id>`

### Вызов тула

```bash
curl -N -X POST http://localhost:3001/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "Mcp-Session-Id: <session_id>" \
  -d '{
    "jsonrpc": "2.0", "id": 2, "method": "tools/call",
    "params": {"name": "search_codebase_smart", "arguments": {"query": "как работает авторизация"}}
  }'
```

---

## 8. Подключение в Android-приложении

- **URL эмулятор:** `http://10.0.2.2:3001`
- **URL физическое устройство:** `http://localhost:3001` (через `adb forward tcp:3001 tcp:3001`)

Настройка через команды:
```
@@talk(rag --url http://10.0.2.2:3001)
@@talk(rag --on)
@@talk(rag --state)
```

---

## 9. Повторная индексация

```bash
# Пересоздать чанки и эмбеддинги (метаданные не трогает)
FORCE_REINDEX=true java -jar rag-server/build/libs/rag-server.jar

# Полное пересоздание (удалить БД)
rm rag_index.db
```

| Ситуация | Что делать |
|----------|---|
| Добавились новые файлы | Ничего — новые добавятся автоматически |
| Изменился код существующих файлов | `FORCE_REINDEX=true` |
| Сменилась embedding-модель | Удалить `rag_index.db`, переиндексировать |
| Обновить метаданные классов | `FORCE_REINDEX=true` + `EXTRACT_METADATA=true` |

---

## 10. Что улучшилось (vs. предыдущей версии)

| Улучшение | Эффект |
|-----------|--------|
| **Настраиваемый Pipeline** | Фильтр + реранк управляются query params или именованными пресетами |
| **Threshold Filter** | Отсечение нерелевантных по similarity score — меньше шума в контексте LLM |
| **Heuristic Reranker** | Keyword overlap boost — поднимает точные совпадения выше |
| **LLM Reranker** | Локальная модель оценивает релевантность каждого чанка — самый точный реранк |
| **Query Optimizer** | Rewrite + translation: запросы становятся самодостаточными, добавляются технические ключевые слова |
| **Context-Aware QueryOptimizer** | `task_state` + `history` → абстрактные запросы ("как это работает?") rewrite-ятся с учётом текущего класса/intent |
| **TaskState endpoint** | `POST /task-state/update` — отслеживает фокус диалога (файл/класс/intent), возвращает резюме ответа для short history |
| **Top-K управление** | `retrieval_topK` (до фильтра) и `final_topK` (после) — видны в debug-заголовке ответа |
| **Evaluation endpoint** | `/evaluate` прогоняет вопросы через все пресеты, сохраняет MD-отчёты для сравнения |
| **@@talk(rag --gentest)** | Автоматизированный запуск тестов из Android-приложения |
| **Hybrid Retrieval** | Embedding (0.6) + Keyword (0.4) — альтернатива двухэтапному поиску |
| **2-Stage Smart Search** | Классы → чанки — концептуальные вопросы находят правильные классы |
| **Incremental Metadata** | Повторный запуск не регенерирует уже обработанные классы |
