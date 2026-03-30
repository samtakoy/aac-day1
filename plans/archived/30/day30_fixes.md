# Day 30 — Fixes Plan

Исправления по результатам code review.

---

## Fix 1: Вернуть try-catch в ChatRoutes (регрессия)

**Файл:** `api/routes/ChatRoutes.kt`

**Проблема:** при перезаписи `ChatRoutes` был удалён `try-catch`, который был в оригинале. Если Ollama недоступна или вернула ошибку — исключение улетает необработанным.

**Решение:** обернуть оба handler-а в `try-catch`, возвращать `502 BadGateway` с телом ошибки.

```kotlin
post("/v1/chat/completions") {
    try {
        val request = call.receive<ChatCompletionRequest>()
        val response = concurrencyLimiter.withLimit { llmProvider.chat(request) }
            ?: return@post call.respond(TooManyRequests, mapOf("error" to "Too many concurrent requests"))
        call.respond(response)
    } catch (e: Exception) {
        call.respond(HttpStatusCode.BadGateway, mapOf("error" to (e.message ?: "unknown")))
    }
}

get("/v1/models") {
    try {
        val models = concurrencyLimiter.withLimit { llmProvider.models() }
            ?: return@get call.respond(TooManyRequests, mapOf("error" to "Too many concurrent requests"))
        call.respond(mapOf("data" to models))
    } catch (e: Exception) {
        call.respond(HttpStatusCode.BadGateway, mapOf("error" to (e.message ?: "unknown")))
    }
}
```

---

## Fix 2 + 3: OllamaModelInfoCache — не кешировать ошибки + атомарность

**Файл:** `llm/OllamaModelInfoCache.kt`

**Проблема 1:** `getOrPut { runCatching { }.getOrDefault(4096) }` — в случае ошибки fetch возвращает `4096`, и `getOrPut` кеширует это значение навсегда. Следующие запросы к Ollama уже не делаются.

**Проблема 2:** `ConcurrentHashMap.getOrPut` в Kotlin **не атомарен** (в отличие от Java `computeIfAbsent`). При параллельных запросах к одной модели несколько корутин могут одновременно вызвать `fetchNumCtx`.

**Решение:** `ConcurrentHashMap` для быстрых чтений (без блокировки) + `Mutex` только для сериализации fetch. Ошибки не кешируются — при следующем запросе будет повторная попытка.

```kotlin
class OllamaModelInfoCache(...) {
    private val cache = ConcurrentHashMap<String, Int>()  // reads без lock
    private val mutex = Mutex()

    suspend fun numCtxFor(model: String): Int {
        cache[model]?.let { return it }          // fast path — без mutex
        return mutex.withLock {
            cache[model] ?: try {                // double-check внутри lock
                fetchNumCtx(model).also { cache[model] = it }
            } catch (e: Exception) {
                DEFAULT_NUM_CTX                  // ← не кешируем
            }
        }
    }
}
```

**Почему ConcurrentHashMap + Mutex:**
- `ConcurrentHashMap` — thread-safe reads без блокировки (fast path не мешает другим)
- `Mutex` сериализует fetch: только одна корутина делает сетевой запрос, остальные ждут и получают результат из double-check
- `ConcurrentHashMap.computeIfAbsent` не подходит — не принимает `suspend` lambda

---

## Fix 4: Валидация конфига

**Файл:** `config/AiGatewayConfig.kt`

**Проблема:** `CONCURRENCY_LIMIT=0` или `-1` → `Semaphore(0)` → runtime crash. `RATE_LIMIT_RPM=0` тихо делает rate limit бесполезным.

**Решение:** `init { require(...) }` — падаем при старте с понятным сообщением, не в рантайме.

```kotlin
data class AiGatewayConfig(...) {
    init {
        require(port in 1..65535) { "PORT must be 1-65535, got $port" }
        require(rateLimitRpm > 0) { "RATE_LIMIT_RPM must be > 0, got $rateLimitRpm" }
        require(concurrencyLimit > 0) { "CONCURRENCY_LIMIT must be > 0, got $concurrencyLimit" }
    }
}
```

---

## Fix 5: OllamaProvider — включить тело ошибки

**Файл:** `llm/OllamaProvider.kt`

**Проблема:** `error("Ollama error ${response.status}")` — Ollama возвращает подробное сообщение в теле ответа, но оно теряется.

**Решение:**

```kotlin
if (!response.status.isSuccess()) {
    val body = runCatching { response.bodyAsText() }.getOrDefault("")
    error("Ollama error ${response.status}: $body")
}
```

---

## Fix 6: Тест для пути 429 (concurrency)

**Файл:** `test/.../ChatRoutesTest.kt`

**Проблема:** нет теста, который проверяет что `ConcurrencyLimiter` действительно возвращает 429 при превышении лимита.

**Решение:** slow fake provider + `ConcurrencyLimiter(1)` + 2 параллельных запроса через `async`.

```kotlin
@Test
fun `POST chat completions returns 429 when concurrency limit exceeded`() = testApplication {
    application {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(RateLimit) {
            register(RateLimitName("api")) { rateLimiter(limit = 1000, refillPeriod = 60.seconds) }
        }
        val slowProvider = object : LlmProvider {
            override suspend fun chat(request: ChatCompletionRequest): ChatCompletionResponse {
                delay(500)
                return ChatCompletionResponse("id", request.model,
                    listOf(Choice(message = Message("assistant", "ok"))))
            }
            override suspend fun models() = emptyList<String>()
        }
        ChatRoutes(slowProvider, ConcurrencyLimiter(1)).apply { configureRoutes() }
    }
    val results = (1..2).map {
        async {
            client.post("/v1/chat/completions") {
                contentType(ContentType.Application.Json)
                setBody("""{"model":"llama3","messages":[{"role":"user","content":"Hi"}]}""")
            }.status
        }
    }.awaitAll()
    assertTrue(results.contains(HttpStatusCode.TooManyRequests))
}
```

---

## Порядок реализации

| # | Файл | Fix |
|---|------|-----|
| 1 | `config/AiGatewayConfig.kt` | `init { require(...) }` |
| 2 | `llm/OllamaModelInfoCache.kt` | Mutex + не кешировать ошибки |
| 3 | `llm/OllamaProvider.kt` | Тело ошибки в сообщении |
| 4 | `api/routes/ChatRoutes.kt` | Вернуть try-catch |
| 5 | `test/.../ChatRoutesTest.kt` | Тест для 429 |

---

## Fix 6: ForwardedHeaders — реальный IP за реверс-прокси

**Файл:** `AiGatewayServer.kt`

**Проблема:** Docker bridge NAT скрывает реальный IP клиента. Ktor видит `172.17.0.1` для всех — per-IP rate limit превращается в глобальный. Проблема есть и локально, и на сервере с default bridge networking.

**Решение:** установить плагин `XForwardedHeaders` — читает `X-Forwarded-For`, который устанавливают nginx/caddy/traefik. Устанавливается **до** `RateLimit` — порядок важен.

```kotlin
install(XForwardedHeaders)  // читает X-Forwarded-For: client_ip
install(RateLimit) { ... }
```

**Зависимость** — отдельный артефакт, добавить в `build.gradle.kts`:
```kotlin
implementation("io.ktor:ktor-server-forwarded-header:3.2.3")
```

**Инфраструктура на сервере** — nginx/caddy перед ai-gateway должен пробрасывать заголовок:
```nginx
proxy_set_header X-Forwarded-For $remote_addr;
```

**Локально** (без реверс-прокси) плагин безвреден — если заголовок отсутствует, Ktor fallback-ится на реальный IP соединения.

> ⚠️ Важно: `XForwardedHeaders` доверяет заголовку от любого клиента. На публичном сервере нужно ограничить доверие только до IP реверс-прокси, иначе клиент сам может подделать `X-Forwarded-For`. Для MVP — приемлемо.

---

## Порядок реализации

| # | Файл | Fix |
|---|------|-----|
| 1 | `build.gradle.kts` | `ktor-server-forwarded-header:3.2.3` |
| 2 | `config/AiGatewayConfig.kt` | `init { require(...) }` |
| 3 | `llm/OllamaModelInfoCache.kt` | ConcurrentHashMap + Mutex + не кешировать ошибки |
| 4 | `llm/OllamaProvider.kt` | Тело ошибки в сообщении |
| 5 | `api/routes/ChatRoutes.kt` | Вернуть try-catch |
| 6 | `AiGatewayServer.kt` | `install(XForwardedHeaders)` перед `RateLimit` |
| 7 | `test/.../ChatRoutesTest.kt` | Тест для 429 (импорты: async, awaitAll, delay) |

## Что НЕ меняем
- `ConcurrencyLimiter.kt` — `T?` семантика формально корректна (chat/models не возвращают null)
- `AiGatewayServer.kt` — HttpClient leak приемлем для long-running сервера
