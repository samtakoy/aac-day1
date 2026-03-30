# AI Gateway — Инструкция по настройке и запуску

Прокси-сервер между Android-приложением и локальным Ollama. Принимает запросы в формате OpenAI API (`/v1/chat/completions`) и перенаправляет их в Ollama.

---

## Требования

- JDK 17+
- Ollama запущен локально (порт 11434)
- Gradle 8.5+ (или `./gradlew` из корня проекта)

---

## 1. Настройка Ollama

```bash
# Установить Ollama (macOS)
brew install ollama

# Запустить сервер
ollama serve

# Скачать нужные модели
ollama pull qwen2.5:7b
ollama pull llama3
ollama pull mistral
```

Проверить, что Ollama работает:
```bash
curl http://localhost:11434/api/tags
```

---

## 2. Переменные окружения

| Переменная | По умолчанию | Описание |
|-----------|---|---|
| `OLLAMA_URL` | `http://localhost:11434` | URL Ollama-сервера |
| `PORT` | `8081` | Порт, на котором слушает ai-gateway |
| `RATE_LIMIT_RPM` | `60` | Максимум запросов в минуту на один IP (per-IP, token bucket) |
| `CONCURRENCY_LIMIT` | `4` | Максимум одновременных запросов к Ollama |
| `OLLAMA_MAX_CONTEXT` | `8192` | Административный лимит контекста (токенов). Итоговый `num_ctx` = `min(OLLAMA_MAX_CONTEXT, лимит_модели)` |

> При запуске в Docker через `docker-compose` Ollama недоступна на `localhost` — используется `http://host.docker.internal:11434` (уже задано в `docker-compose.yml`).

### Rate limiting

Оба эндпоинта (`/v1/chat/completions` и `/v1/models`) защищены двумя типами лимитов:

- **RPM per-IP** — при превышении `RATE_LIMIT_RPM` возвращается `429` с заголовками `X-RateLimit-Remaining` и `Retry-After`
- **Concurrency** — при превышении `CONCURRENCY_LIMIT` одновременных запросов возвращается `429` немедленно (не ждёт освобождения)

Настройте Ollama на параллельную обработку (`OLLAMA_NUM_PARALLEL`) в соответствии с `CONCURRENCY_LIMIT`.

### num_ctx (max context)

`num_ctx` — Ollama-специфичный параметр, не передаётся клиентом (OpenAI API его не имеет). ai-gateway вычисляет его автоматически для каждого запроса:

```
num_ctx = min(OLLAMA_MAX_CONTEXT, лимит_модели_из_Ollama)
```

- **`OLLAMA_MAX_CONTEXT`** — административный лимит сервера (память, ресурсы)
- **лимит модели** — запрашивается у Ollama через `/api/show` (поле `llama.context_length`), кешируется

Защита: если запросить `num_ctx` больше лимита модели — Ollama упадёт. `min()` гарантирует, что этого не произойдёт.

---

## 3. Сборка

```bash
./gradlew :ai-gateway:build
```

Jar: `ai-gateway/build/libs/ai-gateway.jar`

---

## 4. Варианты запуска

### Локально (без Docker)

```bash
java -jar ai-gateway/build/libs/ai-gateway.jar
```

Или через Gradle:
```bash
./gradlew :ai-gateway:run
```

### С кастомным URL Ollama

```bash
OLLAMA_URL=http://192.168.1.100:11434 java -jar ai-gateway/build/libs/ai-gateway.jar
```

### Через Docker Compose

```bash
# Собрать JAR перед запуском
./gradlew :ai-gateway:build

# Запустить
docker-compose up ai-gateway

# Или вместе с другими сервисами
docker-compose up
```

---

## 5. API

### POST /v1/chat/completions

Принимает OpenAI-совместимый запрос, перенаправляет в Ollama.

```bash
curl -X POST http://localhost:8081/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "qwen2.5:7b",
    "messages": [
      {"role": "user", "content": "Привет! Кто ты?"}
    ]
  }'
```

### GET /v1/models

Возвращает список моделей, доступных в Ollama (динамически через `/api/tags`).

```bash
curl http://localhost:8081/v1/models
```

---

## 6. Подключение в Android-приложении

По умолчанию приложение использует `http://10.0.2.2:8081` (адрес хоста из эмулятора).

Для **физического устройства** нужно:
1. Узнать IP-адрес компьютера в сети (например `192.168.1.100`)
2. Поменять URL в DataStore через настройки приложения (пока не реализовано в UI — можно поменять дефолт в `AppSettings.DEFAULT_LOCAL_SERVER_URL`)

Включение локальной LLM для чата:
1. Открыть настройки модели в любом чате
2. Указать название модели Ollama (например `qwen2.5:7b`)
3. Поставить галочку **"Локальная LLM (Ollama)"**

---

## 7. Проверка работы

```bash
# Сервер запущен?
curl http://localhost:8081/v1/models

# Тестовый запрос
curl -X POST http://localhost:8081/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{"model":"qwen2.5:7b","messages":[{"role":"user","content":"1+1=?"}]}'
```

Ожидаемый ответ `/v1/models` (реальный список из Ollama):
```json
{"data": ["llama3:latest", "qwen2.5:7b", "nomic-embed-text:latest"]}
```

---

## 8. Связанные сервисы

| Сервис | Порт | Описание | Документация |
|--------|:----:|----------|---|
| **ai-gateway** | `8081` | Прокси Ollama → OpenAI-совместимый API (этот сервис) | `ai-gateway/SETUP.md` |
| **rag-server** | `3001` | Индексация кодовой базы, семантический поиск, MCP-тулы | `rag-server/SETUP.md` |
| **mcp-server** | `3000` | MCP Inspector сервер для отладки MCP-протокола | — |

Для полной локальной среды:
```bash
# Терминал 1 — Ollama
ollama serve

# Терминал 2 — AI Gateway (чат с LLM)
./gradlew :ai-gateway:run

# Терминал 3 — RAG Server (поиск по коду)
CODE_PATH=/path/to/src ./gradlew :rag-server:run
```
