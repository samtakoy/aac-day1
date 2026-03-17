# RAG MCP Server — Инструкция по настройке и запуску

Сервер индексирует Kotlin/Markdown файлы кодовой базы, генерирует эмбеддинги и предоставляет три MCP-тула для AI-агента:
- `search_codebase` — поиск по структурным блокам (функции, классы)
- `search_codebase_fixed` — поиск по чанкам фиксированного размера
- `get_index_status` — статус индекса

## Требования

- JDK 17+
- Gradle 8.5+ (или использовать `./gradlew` из корня проекта)
- Ollama (для локальных эмбеддингов) **или** ключ OpenRouter

---

## 1. Настройка Ollama (рекомендуется)

```bash
# Установить Ollama (macOS)
brew install ollama

# Запустить сервер Ollama
ollama serve

# Скачать embedding-модель (в отдельном терминале)
ollama pull nomic-embed-text
```

Проверить что модель работает:
```bash
curl http://localhost:11434/api/embeddings \
  -d '{"model":"nomic-embed-text:v1.5","prompt":"hello world"}'
```

Ожидаемый ответ: `{"embedding":[0.123, ...]}` (массив из 768 чисел)

---

## 2. Переменные окружения

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

---

## 3. Сборка

```bash
# Из корня проекта
./gradlew :rag-server:build
```

Jar находится в `rag-server/build/libs/rag-server.jar`

---

## 4. Запуск

### Вариант A: Локально через Gradle

```bash
export CODE_PATH="/path/to/your/kotlin/project/src"
export DB_PATH="./rag_index.db"

./gradlew :rag-server:run
```

### Вариант B: Через Java напрямую

```bash
export CODE_PATH="/path/to/your/kotlin/project/src"

java -jar rag-server/build/libs/rag-server.jar
```

### Вариант C: Docker

```bash
docker build -f rag-server/Dockerfile -t rag-server .

docker run -p 3001:3001 \
  -e CODE_PATH="/code" \
  -v "/path/to/your/project:/code:ro" \
  -v "$(pwd)/rag_index.db:/app/rag_index.db" \
  rag-server
```

### С OpenRouter вместо Ollama

```bash
export CODE_PATH="/path/to/project/src"
export EMBEDDING_PROVIDER=openrouter
export OPENROUTER_API_KEY=sk-or-...
export EMBEDDING_MODEL=openai/text-embedding-3-small

java -jar rag-server/build/libs/rag-server.jar
```

---

## 5. Проверка работы

```
Отлично, сервер отвечает! Но нужен session ID из заголовков ответа. Запусти с -D- чтобы увидеть заголовки:


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
  
  В ответе в заголовках будет Mcp-Session-Id: <session_id>.

Шаг 2 — вызов тула (с session ID):


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
```

### Статус индекса

```bash
curl -X POST http://localhost:3001/mcp/message \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/call",
    "params": {
      "name": "get_index_status",
      "arguments": {}
    }
  }'
```

### Поиск по кодовой базе

```bash
curl -X POST http://localhost:3001/mcp/message \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/call",
    "params": {
      "name": "search_codebase",
      "arguments": { "query": "как работает MCP сервер" }
    }
  }'
```

---

## 6. Подключение в Android-приложении

В настройках MCP-серверов добавить новый сервер:
- **URL**: `http://10.0.2.2:3001` (из эмулятора) или `http://localhost:3001` (физическое устройство через adb forward)

После подключения агент увидит три новых тула.

---

## 7. Повторная индексация

По умолчанию сервер **не** переиндексирует если индекс уже есть. Для принудительного пересоздания:

```bash
FORCE_REINDEX=true java -jar rag-server/build/libs/rag-server.jar
```

Или удалить файл базы данных:
```bash
rm rag_index.db
```

---

## 8. Индексируемые файлы

Сервер сканирует `CODE_PATH` рекурсивно и индексирует файлы с расширениями:
- `.kt` — Kotlin source files
- `.kts` — Kotlin scripts
- `.md` — Markdown документация

Игнорируются директории: `build/`, `.git/`, `.gradle/`, `generated/`, `.idea/`
