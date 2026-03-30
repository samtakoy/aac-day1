# Day 30 — Локальная LLM как приватный сервис

## Цель
Развернуть `rag-server` + `ai-gateway` вместе в Docker, добавить rate limiting и управление max context (num_ctx) для Ollama.

---

## Задача 1: rag-server в docker-compose.yml

### Решение
`RAG_CODE_PATH` задаётся в `.env` файле, монтируется как volume.

**`.env`** (рядом с `docker-compose.yml`):
```
RAG_CODE_PATH=/Users/samtakot/devs/learnings/agent_frameworks/min/koog
RATE_LIMIT_RPM=60
CONCURRENCY_LIMIT=4
```

**`docker-compose.yml`** — добавить сервис:
```yaml
rag-server:
  build:
    context: .
    dockerfile: rag-server/Dockerfile
  container_name: rag-server
  ports:
    - "3001:3001"
  volumes:
    - ${RAG_CODE_PATH}:/workspace/src:ro
    - rag-index:/app/data
  environment:
    - CODE_PATH=/workspace/src
    - OLLAMA_BASE_URL=http://host.docker.internal:11434
    - DB_PATH=/app/data/rag_index.db
    - RAG_SERVER_PORT=3001
  restart: unless-stopped
  healthcheck:
    test: ["CMD", "wget", "-q", "--spider", "http://localhost:3001/search?query=test"]
    interval: 30s
    timeout: 15s
    retries: 3
    start_period: 120s

volumes:
  rag-index:
```

Ai-gateway env дополнить:
```yaml
- RATE_LIMIT_RPM=${RATE_LIMIT_RPM:-60}
- CONCURRENCY_LIMIT=${CONCURRENCY_LIMIT:-4}
```

### Что НЕ меняю
- Dockerfile rag-server (уже multi-stage)
- Код rag-server

---

## Задача 2: Rate limiting в ai-gateway

### Два типа лимитов
1. **RPM (per-IP)** — защита от злоупотреблений. Ktor plugin `ktor-server-rate-limit`.
2. **Concurrency** — защита Ollama от перегрузки. `kotlinx.coroutines.sync.Semaphore`.

Ollama настроим на параллельную обработку 4 запросов → лимит concurrency тоже 4.
Тест: 4 параллельных запроса проходят, 5-й получает 429.

### Принцип чистоты кода
- Два отдельных класса: `RpmRateLimiter` (оборачивает Ktor plugin) и `ConcurrencyLimiter` (Semaphore)
- Применяются только к `/v1/chat/completions` — тяжёлый эндпоинт
- Существующий `ChatRoutes`, `OllamaProvider`, `LlmRouter` — **не меняются**
- `ChatRoutes` принимает `LlmProvider` как сейчас, лимиты применяются на уровне routing в `AiGatewayServer`

### Изменяемые файлы

**`build.gradle.kts`** — одна строка:
```kotlin
implementation("io.ktor:ktor-server-rate-limit:3.2.3")
```

**`config/AiGatewayConfig.kt`** — два новых поля:
```kotlin
val rateLimitRpm: Int = System.getenv("RATE_LIMIT_RPM")?.toIntOrNull() ?: 60
val concurrencyLimit: Int = System.getenv("CONCURRENCY_LIMIT")?.toIntOrNull() ?: 4
```

**Новый файл `middleware/ConcurrencyLimiter.kt`**:
```kotlin
class ConcurrencyLimiter(limit: Int) {
    private val semaphore = Semaphore(limit)

    suspend fun <T> withLimit(block: suspend () -> T): T? {
        if (!semaphore.tryAcquire()) return null   // null → 429
        return try { block() } finally { semaphore.release() }
    }
}
```

**`AiGatewayServer.kt`** — установить RateLimit plugin + передать ConcurrencyLimiter в routing:
```kotlin
install(RateLimit) {
    register(RateLimitName("chat")) {
        rateLimiter(limit = config.rateLimitRpm, refillPeriod = 60.seconds)
    }
}
val concurrencyLimiter = ConcurrencyLimiter(config.concurrencyLimit)
// передать в ChatRoutes вместе с llmProvider
```

**`api/routes/ChatRoutes.kt`** — принять `ConcurrencyLimiter`, применить оба лимита к обоим эндпоинтам:
```kotlin
class ChatRoutes(
    private val llmProvider: LlmProvider,
    private val concurrencyLimiter: ConcurrencyLimiter
) {
    fun Application.configureRoutes() {
        routing {
            rateLimit(RateLimitName("api")) {
                post("/v1/chat/completions") {
                    val request = call.receive<ChatCompletionRequest>()
                    val response = concurrencyLimiter.withLimit { llmProvider.chat(request) }
                        ?: return@post call.respond(HttpStatusCode.TooManyRequests,
                            mapOf("error" to "Too many concurrent requests"))
                    call.respond(response)
                }
                get("/v1/models") {
                    val models = concurrencyLimiter.withLimit { llmProvider.models() }
                        ?: return@get call.respond(HttpStatusCode.TooManyRequests,
                            mapOf("error" to "Too many concurrent requests"))
                    call.respond(mapOf("data" to models))
                }
            }
        }
    }
}
```

---

## Задача 3: Max context (num_ctx) — серверный лимит

### Концепция

`num_ctx` — чисто Ollama-специфичный параметр (не часть OpenAI API).
`ChatCompletionRequest` — OpenAI-совместимая модель, `num_ctx` туда не добавляем.

**Две границы:**
1. **`OLLAMA_MAX_CONTEXT`** — административный лимит сервера (ресурсы, память)
2. **`modelInfoCache.numCtxFor(model)`** — жёсткий лимит модели из `/api/show`, выше которого Ollama упадёт

Итоговое значение: `min(OLLAMA_MAX_CONTEXT, model_max)`

Клиент не знает про `num_ctx` — это внутренняя деталь ai-gateway.

```
Клиент → ChatCompletionRequest (OpenAI, без num_ctx)
                    ↓
              ai-gateway
    min(OLLAMA_MAX_CONTEXT, modelInfoCache.numCtxFor(model))
                    ↓
    OllamaCompletionRequest (с num_ctx) → Ollama
```

### Принцип чистоты кода
- `ChatCompletionRequest` (shared) — **не трогаем**, остаётся OpenAI-совместимым
- `OllamaModelInfoCache` — жёсткий лимит модели, защита от краша Ollama
- `OllamaCompletionRequest` — внутренний DTO только для ai-gateway
- `LlmProvider` interface и `LlmRouter` — **не меняются**

### Новые/изменяемые файлы

**Новый `llm/OllamaModelInfoCache.kt`**:
```kotlin
class OllamaModelInfoCache(private val client: HttpClient, private val ollamaUrl: String) {
    private val cache = ConcurrentHashMap<String, Int>()

    suspend fun numCtxFor(model: String): Int = cache.getOrPut(model) {
        runCatching { fetchNumCtx(model) }.getOrDefault(4096)
    }

    private suspend fun fetchNumCtx(model: String): Int {
        val response: JsonObject = client.post("$ollamaUrl/api/show") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("name" to model))
        }.body()
        return response["modelinfo"]
            ?.jsonObject?.get("llama.context_length")
            ?.jsonPrimitive?.int ?: 4096
    }
}
```

**Новый `llm/OllamaCompletionRequest.kt`** (внутренний DTO, не shared):
```kotlin
@Serializable
data class OllamaCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val stream: Boolean = false,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    val temperature: Double? = null,
    @SerialName("num_ctx") val numCtx: Int
)
```

**`llm/OllamaProvider.kt`** — принять `OllamaModelInfoCache`, маппить запрос:
```kotlin
class OllamaProvider(
    private val client: HttpClient,
    private val ollamaUrl: String,
    private val modelInfoCache: OllamaModelInfoCache
) : LlmProvider {
    override suspend fun chat(request: ChatCompletionRequest): ChatCompletionResponse {
        val numCtx = modelInfoCache.numCtxFor(request.model)
        val ollamaRequest = OllamaCompletionRequest(
            model = request.model,
            messages = request.messages,
            stream = request.stream,
            maxTokens = request.maxTokens,
            temperature = request.temperature,
            numCtx = numCtx
        )
        val response = client.post("$ollamaUrl/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            setBody(ollamaRequest)
        }
        if (!response.status.isSuccess()) error("Ollama error ${response.status}")
        return response.body()
    }
}
```

**`AiGatewayServer.kt`** — создать `OllamaModelInfoCache`, передать в `OllamaProvider`.

---

## Порядок реализации

| # | Файл | Действие |
|---|------|---------|
| 1 | `docker-compose.yml` | Добавить rag-server, env для ai-gateway |
| 2 | `.env` | Создать с RAG_CODE_PATH |
| 3 | `build.gradle.kts` | `ktor-server-rate-limit` |
| 4 | `config/AiGatewayConfig.kt` | `rateLimitRpm`, `concurrencyLimit` |
| 5 | `middleware/ConcurrencyLimiter.kt` | Новый файл |
| 6 | `llm/OllamaModelInfoCache.kt` | Новый файл |
| 7 | `llm/OllamaCompletionRequest.kt` | Новый файл |
| 8 | `llm/LlmProvider.kt` | Добавить `models()` |
| 9 | `llm/OllamaProvider.kt` | `models()` + `modelInfoCache` + `OllamaCompletionRequest` |
| 10 | `llm/LlmRouter.kt` | Делегировать `models()` |
| 11 | `api/routes/ChatRoutes.kt` | `ConcurrencyLimiter` + оба лимита на оба эндпоинта |
| 12 | `AiGatewayServer.kt` | Сборка всех зависимостей |

### `/v1/models` — динамический через Ollama `/api/tags`

**`llm/LlmProvider.kt`** — добавить `models()`:
```kotlin
interface LlmProvider {
    suspend fun chat(request: ChatCompletionRequest): ChatCompletionResponse
    suspend fun models(): List<String>
}
```

**`llm/OllamaProvider.kt`** — реализовать `models()`:
```kotlin
override suspend fun models(): List<String> {
    val response: JsonObject = client.get("$ollamaUrl/api/tags").body()
    return response["models"]?.jsonArray
        ?.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.content }
        ?: emptyList()
}
```

**`llm/LlmRouter.kt`** — делегировать:
```kotlin
override suspend fun models(): List<String> = ollamaProvider.models()
```

---

## Что НЕ меняю
- `shared/` — никаких изменений в shared DTO
- Android app — никаких изменений
- rag-server код — только docker-compose
