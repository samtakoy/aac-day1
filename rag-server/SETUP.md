# RAG MCP Server — Инструкция по настройке и запуску

Сервер индексирует Kotlin/Markdown файлы кодовой базы, генерирует эмбеддинги и предоставляет MCP-тулы для AI-агента:

| Тул | Описание | Когда использовать |
|-----|----------|--------------------|
| `search_codebase` | Hybrid поиск по структурным блокам (функции, классы) | Конкретный класс или метод по имени |
| `search_codebase_fixed` | Hybrid поиск по чанкам фиксированного размера | Широкий контекстный поиск |
| `search_codebase_smart` | 2-Stage: сначала классы по домену, потом методы | Концептуальные вопросы ("как работает X") |
| `get_index_status` | Статус индекса | Проверить готовность перед поиском |

---

## Требования

- JDK 17+
- Gradle 8.5+ (или использовать `./gradlew` из корня проекта)
- Ollama (для локальных эмбеддингов) **или** ключ OpenRouter

---

## 1. Настройка Ollama

```bash
# Установить Ollama (macOS)
brew install ollama

# Запустить сервер Ollama
ollama serve

# Скачать embedding-модель (обязательно)
ollama pull nomic-embed-text

# Скачать LLM для метаданных (нужна только если EXTRACT_METADATA=true)
ollama pull qwen2.5-coder:7b
```

Проверить что embedding-модель работает:
```bash
curl http://localhost:11434/api/embeddings \
  -d '{"model":"nomic-embed-text","prompt":"hello world"}'
```

Ожидаемый ответ: `{"embedding":[0.123, ...]}` (массив из 768 чисел)

---

## 2. Переменные окружения

### Основные (как раньше)

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
| `SEARCH_TOP_K` | нет | `5` | Сколько результатов возвращать при поиске |

### Новые (для метаданных и 2-Stage поиска)

| Переменная | Обязательная | По умолчанию | Описание |
|-----------|:---:|---|---|
| `EXTRACT_METADATA` | нет | `false` | `true` — включить LLM-анализ классов при индексации |
| `LLM_MODEL` | нет | `qwen2.5-coder:7b` | Ollama-модель для анализа кода (только если `EXTRACT_METADATA=true`) |

> **Примечание:** `EXTRACT_METADATA=true` увеличивает время первичной индексации (~1–3 сек на класс).
> Для проекта из 100 классов — ~3–5 минут дополнительно.
> Индексация **инкрементальная**: при повторном запуске уже обработанные классы пропускаются.
> После индексации сервер работает как обычно, скорость поиска не меняется.

### Новые (для перевода запросов)

| Переменная | Обязательная | По умолчанию | Описание |
|-----------|:---:|---|---|
| `TRANSLATE_QUERIES` | нет | `false` | `true` — переводить запросы на английский перед поиском |
| `TRANSLATE_LLM_MODEL` | нет | значение `LLM_MODEL` | Ollama-модель для перевода (можно указать отдельную, более быструю) |

> **Когда включать:** если поисковые запросы приходят на русском или другом языке.
> Определение языка — эвристика: если >30% букв кириллица → запрос переводится.
> Перевод выполняется перед каждым поиском, логируется: `[Translate] 'запрос' → 'query'`.
> Требует работающего Ollama (`EMBEDDING_PROVIDER=ollama`).

---

## 3. Сборка

```bash
# Из корня проекта
./gradlew :rag-server:build
```

Jar находится в `rag-server/build/libs/rag-server.jar`

---

## 4. Варианты запуска

### Базовый (как раньше — только embedding поиск + keyword + context packing)

```bash
export CODE_PATH="/path/to/your/kotlin/project/src"

java -jar rag-server/build/libs/rag-server.jar
```

Доступны тулы: `search_codebase`, `search_codebase_fixed`, `get_index_status`

---

### С метаданными (+ тул `search_codebase_smart`)

```bash
export CODE_PATH="/path/to/your/kotlin/project/src"
export EXTRACT_METADATA=true
export LLM_MODEL=qwen2.5-coder:7b-instruct   # или другая локальная модель

java -jar rag-server/build/libs/rag-server.jar
```

Доступны все 4 тула, включая `search_codebase_smart`.

> При первом запуске генерируются метаданные и embedding-векторы для каждого класса.
> При последующих запусках уже обработанные классы **пропускаются автоматически**.

---

### С метаданными и переводом запросов (рекомендуется для русскоязычных запросов)

```bash
export CODE_PATH="/path/to/your/kotlin/project/src"
export EXTRACT_METADATA=true
export LLM_MODEL=qwen2.5-coder:7b-instruct
export TRANSLATE_QUERIES=true
# Опционально: отдельная модель для перевода (быстрее чем coder-модель)
# export TRANSLATE_LLM_MODEL=qwen2.5:3b

java -jar rag-server/build/libs/rag-server.jar
```

---

### С OpenRouter вместо Ollama (только embeddings, без метаданных)

```bash
export CODE_PATH="/path/to/project/src"
export EMBEDDING_PROVIDER=openrouter
export OPENROUTER_API_KEY=sk-or-...
export EMBEDDING_MODEL=openai/text-embedding-3-small

java -jar rag-server/build/libs/rag-server.jar
```

> `EXTRACT_METADATA` работает только с Ollama (использует `/api/generate`).

---

### Через Gradle

```bash
export CODE_PATH="/path/to/your/kotlin/project/src"

./gradlew :rag-server:run
```

---

## 5. Выбор тула — когда что использовать

```
Запрос агента
     │
     ├─ "найди метод createUser"          → search_codebase
     ├─ "где определён ChatRepository"    → search_codebase
     ├─ "как работает авторизация"        → search_codebase_smart (если EXTRACT_METADATA=true)
     │                                      search_codebase (если без метаданных)
     ├─ "покажи код вокруг ошибки X"      → search_codebase_fixed
     └─ "индекс готов?"                   → get_index_status
```

---

## 6. Проверка работы (curl)

### Шаг 1 — инициализация сессии

```bash
curl -N -X POST http://localhost:3001/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -D- \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "initialize",
    "params": {
      "protocolVersion": "2024-11-05",
      "capabilities": {},
      "clientInfo": {"name": "test", "version": "1.0"}
    }
  }'
```

В ответе в заголовках будет `Mcp-Session-Id: <session_id>`.

### Шаг 2 — вызов тула (с session ID)

```bash
# Статус индекса
curl -N -X POST http://localhost:3001/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "Mcp-Session-Id: <session_id>" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/call",
    "params": {
      "name": "get_index_status",
      "arguments": {}
    }
  }'

# Hybrid поиск
curl -N -X POST http://localhost:3001/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "Mcp-Session-Id: <session_id>" \
  -d '{
    "jsonrpc": "2.0",
    "id": 3,
    "method": "tools/call",
    "params": {
      "name": "search_codebase",
      "arguments": { "query": "createUser" }
    }
  }'

# 2-Stage Smart поиск
curl -N -X POST http://localhost:3001/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "Mcp-Session-Id: <session_id>" \
  -d '{
    "jsonrpc": "2.0",
    "id": 4,
    "method": "tools/call",
    "params": {
      "name": "search_codebase_smart",
      "arguments": { "query": "как работает авторизация" }
    }
  }'
```

---

## 7. Подключение в Android-приложении

В настройках MCP-серверов добавить новый сервер:
- **URL**: `http://10.0.2.2:3001` (из эмулятора) или `http://localhost:3001` (физическое устройство через adb forward)

После подключения агент увидит все доступные тулы.

---

## 8. Повторная индексация

По умолчанию сервер **не** переиндексирует если индекс уже есть. Для принудительного пересоздания:

```bash
FORCE_REINDEX=true java -jar rag-server/build/libs/rag-server.jar
```

Или удалить файл базы данных:
```bash
rm rag_index.db
```

> `FORCE_REINDEX=true` пересобирает чанки и эмбеддинги, но **не** затрагивает таблицу метаданных.
> Для полного пересоздания метаданных и их векторов — удалите `rag_index.db` и запустите с `EXTRACT_METADATA=true`.

### Когда нужен FORCE_REINDEX

| Ситуация | Что делать |
|----------|-----------|
| Добавились новые файлы в проект | Ничего — при запуске новые классы добавятся автоматически |
| Изменился код существующих классов | `FORCE_REINDEX=true` — пересоздать чанки и эмбеддинги |
| Сменилась embedding-модель | Удалить `rag_index.db`, переиндексировать полностью |
| Нужно обновить метаданные классов | `FORCE_REINDEX=true` + `EXTRACT_METADATA=true` |

---

## 9. Индексируемые файлы

Сервер сканирует `CODE_PATH` рекурсивно и индексирует файлы с расширениями:
- `.kt` — Kotlin source files
- `.kts` — Kotlin scripts
- `.md` — Markdown документация

Игнорируются директории: `build/`, `.git/`, `.gradle/`, `generated/`, `.idea/`

---

## 10. Что улучшилось (vs. предыдущей версии)

| Улучшение | Эффект |
|-----------|--------|
| **Hybrid Retrieval** | Точные имена методов/классов теперь находятся надёжно |
| **Context Packing** | Результаты сгруппированы по классам, нет дублей, виден Big Picture |
| **Structured Metadata** | LLM описывает каждый класс — responsibility, dependencies, domain |
| **2-Stage Smart Search** | Концептуальные вопросы находят правильные классы, не случайные методы |
| **Embedding-based Stage 1** | Stage 1 использует семантическое сходство вместо keyword — работает на любом языке |
| **Incremental Metadata** | Повторный запуск не регенерирует уже обработанные классы — быстрый старт |
| **Query Translation** | Русскоязычные запросы переводятся на английский перед поиском (`TRANSLATE_QUERIES=true`) |
