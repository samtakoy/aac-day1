# Локальный AI-стек — запуск через Docker

Три сервиса в одном `docker-compose`:

| Сервис | Порт | Описание |
|--------|------|----------|
| `ai-gateway` | 8081 | Прокси Ollama с rate limiting и auto num_ctx |
| `rag-server` | 3001 | RAG-поиск по кодовой базе |
| `mcp-server` | 3000 | MCP-сервер для GitHub |

---

## Шаг 1 — Установить и настроить Ollama

```bash
brew install ollama
ollama serve
```

Скачать модели (минимум для работы):
```bash
ollama pull nomic-embed-text        # эмбеддинги для rag-server
ollama pull qwen2.5:7b              # LLM для ai-gateway
```

Включить параллельную обработку (соответствует `CONCURRENCY_LIMIT` в ai-gateway):
```bash
# В отдельном терминале или в systemd/launchd:
OLLAMA_NUM_PARALLEL=4 ollama serve
```

Проверить:
```bash
curl http://localhost:11434/api/tags
```

---

## Шаг 2 — Настроить .env

Создать `.env` в корне проекта (или отредактировать существующий):

```bash
cp .env .env.local  # опционально — для локальных переопределений
```

Обязательно задать `RAG_CODE_PATH` — путь к кодовой базе для индексации:

```
# .env
RAG_CODE_PATH=/path/to/your/project/src

RATE_LIMIT_RPM=60        # запросов в минуту на IP
CONCURRENCY_LIMIT=4      # одновременных запросов к Ollama (= OLLAMA_NUM_PARALLEL)
OLLAMA_MAX_CONTEXT=8192  # лимит токенов контекста; итоговый num_ctx = min(это, лимит_модели)
```

---

## Шаг 3 — Собрать JAR для ai-gateway

`rag-server` и `mcp-server` собираются внутри Docker (multi-stage). `ai-gateway` требует pre-built JAR:

```bash
./gradlew :ai-gateway:build
```

Убедиться что JAR создан:
```bash
ls ai-gateway/build/libs/ai-gateway.jar
```

---

## Шаг 4 — Запустить Docker Compose

```bash
# Все три сервиса
docker-compose up -d

# Только ai-gateway (без rag-server и mcp-server)
docker-compose up -d ai-gateway

# ai-gateway + rag-server
docker-compose up -d ai-gateway rag-server

# с пересборкой
docker-compose up -d --build ai-gateway rag-server
```

Посмотреть логи:
```bash
docker-compose logs -f ai-gateway
docker-compose logs -f rag-server
```

---

## Шаг 5 — Проверить работу

### ai-gateway

```bash
# Список моделей (динамически из Ollama)
curl http://localhost:8081/v1/models

# Тестовый чат-запрос
curl -X POST http://localhost:8081/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{"model":"qwen2.5-coder:7b-instruct","messages":[{"role":"user","content":"1+1=?"}]}'
```

### rag-server

```bash
# Статус индекса
curl "http://localhost:3001/search?query=test"
```

---

## Шаг 6 — Тест rate limiting и concurrency

### Проверить concurrency limit (4 параллельных → ок, 5-й → 429)

```bash
# Запустить 5 параллельных запросов, вывести только HTTP-коды
seq 5 | xargs -P5 -I{} curl -s -o /dev/null -w "%{http_code}\n" \
  -X POST http://localhost:8081/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{"model":"qwen2.5-coder:7b-instruct","messages":[{"role":"user","content":"напиши одно слово"}]}'
```

Ожидаемый вывод (порядок может отличаться):
```
200
200
200
200
429
```

### Проверить RPM rate limit

```bash
# Быстро отправить 65 запросов — последние 5 должны получить 429
seq 65 | xargs -P1 -I{} curl -s -o /dev/null -w "%{http_code}\n" \
  -X POST http://localhost:8081/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{"model":"qwen2.5-coder:7b-instruct","messages":[{"role":"user","content":"1+1"}]}'
```

### Посмотреть rate limit заголовки

```bash
curl -i http://localhost:8081/v1/models | grep -i "ratelimit\|retry"
# X-RateLimit-Limit: 60
# X-RateLimit-Remaining: 59
# X-RateLimit-Reset: <timestamp>
```

---

## Остановка

```bash
docker-compose down
```

> Индекс rag-server хранится в `rag-server/rag_index.db` на хосте — не теряется при остановке контейнера. Для полной переиндексации удалить файл или запустить с `FORCE_REINDEX=true`.

---

## Детальная настройка сервисов

- [ai-gateway/SETUP.md](ai-gateway/SETUP.md) — переменные окружения, rate limiting, num_ctx
- [rag-server/SETUP.md](rag-server/SETUP.md) — pipeline, пресеты, MCP API
- [mcp-server/SETUP.md](mcp-server/SETUP.md) — GitHub интеграция
