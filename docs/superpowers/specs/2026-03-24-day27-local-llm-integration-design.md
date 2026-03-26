# Day 27: Интеграция локальной LLM (Ollama) — Design Spec

**Дата:** 2026-03-24
**Статус:** Approved
**Ветка:** day27

---

## Цель

Интегрировать локальную LLM (Ollama) в существующее Android-приложение через backend ai-gateway сервер. Приложение должно работать без облачных моделей при `isLocal=true`, при этом OpenRouter продолжает работать при `isLocal=false`.

---

## Архитектура

```
Android App
├── ModelSettings (per-chat)
│   ├── isLocal: Boolean           ← НОВОЕ
│   └── name: "llama3"            ← имя модели Ollama, per-chat
│
├── LlmRepositoryImpl
│   ├── isLocal=true  → LocalLlmApiImpl  → ai-gateway:8081
│   └── isLocal=false → RemoteLlmApiImpl → OpenRouter
│
└── AppSettings (global, DataStore "app_settings")
    └── localServerUrl: String     ← дефолт: http://10.0.2.2:8081

shared/simple-chat-api             ← новый JVM Kotlin модуль
└── OpenAI-compatible DTOs

ai-gateway (Ktor 3.2.3)           ← новый модуль
├── POST /v1/chat/completions
├── GET  /v1/models                (stub: возвращает хардкод список)
└── OllamaProvider → Ollama:11434/v1/chat/completions
```

### Ключевые решения

- **ai-gateway как посредник**: Android → ai-gateway → Ollama. Android не знает про Ollama напрямую.
- **isLocal per-chat**: переключатель в настройках каждого чата. Каждый чат независимо локальный или облачный.
- **localServerUrl глобально**: URL ai-gateway хранится в DataStore (AppSettings), не в ModelSettings.
- **Ktor 3.2.3**: как в rag-server. Консистентность.
- **shared/simple-chat-api**: только DTOs (не KMP). rag-server использует свои провайдеры (нативный Ollama API — другой протокол).
- **Маперы без интерфейсов**: `OpenAiModelRequestMapperImpl`, `OpenAiModelResponseMapperImpl` инжектятся напрямую в `LocalLlmApiImpl`.
- **MVP ограничение**: `shared/simple-chat-api` не содержит полей `reasoning`/`toolCalls` — локальная LLM не вернёт эти данные в `ModelResult`.

---

## Модули

### settings.gradle.kts (обновление)

```kotlin
include(":app")
include(":mcp-server")
include(":rag-server")
include(":shared:simple-chat-api")   // ← НОВОЕ
include(":ai-gateway")               // ← НОВОЕ
```

---

### shared/simple-chat-api

Обычный JVM Kotlin модуль. Зависят: `app` и `ai-gateway`.

```kotlin
@Serializable data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val stream: Boolean = false,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    val temperature: Double? = null
)

@Serializable data class ChatCompletionResponse(
    val id: String,
    val choices: List<Choice>,
    val usage: Usage? = null
)

@Serializable data class Message(val role: String, val content: String)

@Serializable data class Choice(
    val index: Int,
    val message: Message,
    @SerialName("finish_reason") val finishReason: String? = null
)

// Int? — Ollama может не возвращать usage поля
@Serializable data class Usage(
    @SerialName("prompt_tokens") val promptTokens: Int? = null,
    @SerialName("completion_tokens") val completionTokens: Int? = null,
    @SerialName("total_tokens") val totalTokens: Int? = null
)
```

---

### ai-gateway

```
ai-gateway/
├── build.gradle.kts              (Ktor 3.2.3, ktor-server-netty, kotlinx-serialization)
├── Dockerfile
└── src/main/kotlin/.../aigateway/
    ├── AiGatewayServer.kt        (main, embeddedServer на Netty)
    ├── config/
    │   └── AiGatewayConfig.kt    (OLLAMA_URL=env default http://localhost:11434, PORT=8081)
    ├── api/routes/
    │   └── ChatRoutes.kt         (POST /v1/chat/completions, GET /v1/models)
    └── llm/
        ├── LlmProvider.kt        (interface: suspend fun chat(req: ChatCompletionRequest): ChatCompletionResponse)
        ├── OllamaProvider.kt     (HttpClient → {ollamaUrl}/v1/chat/completions, без маппинга)
        └── LlmRouter.kt         (MVP: всегда OllamaProvider)
```

**GET /v1/models** — stub, возвращает хардкод список `["llama3", "mistral", "qwen2.5"]`. Не обращается к Ollama.

**Обработка ошибок в ChatRoutes**: HTTP non-2xx от OllamaProvider → `respond(HttpStatusCode.BadGateway, "Ollama error: ...")`.

`OllamaProvider` форвардит запрос напрямую — Ollama поддерживает OpenAI-совместимый API, маппинг не нужен.

**docker-compose.yml добавление:**
```yaml
ai-gateway:
  build:
    context: .
    dockerfile: ai-gateway/Dockerfile
  container_name: ai-gateway
  ports:
    - "8081:8081"
  environment:
    - OLLAMA_URL=http://host.docker.internal:11434
    - PORT=8081
  restart: unless-stopped
```

---

## Android — изменения

### ModelSettings.kt
```kotlin
data class ModelSettings(
    val name: String,
    // ... existing fields ...
    val isLocal: Boolean = false   // ← НОВОЕ
)
```

### ModelSettingsEntity.kt + маппер

Добавить `isLocal: Boolean = false`.

> **Важно**: `ModelSettingsEntity` — это `@Serializable` data class, хранящийся как JSON-строка в колонке `model_settings_json` в Room. Это **не** Room entity — отдельная таблица для него не существует. **Room migration не нужна** — Json-декодер с `ignoreUnknownKeys = true` корректно десериализует старые записи с дефолтом `isLocal = false`.

### AppSettings (новый)

DataStore name: **`"app_settings"`** (не пересекается с `"mcp_secrets"` из `SecretsVault`).

```kotlin
class AppSettings @Inject constructor(@ApplicationContext context: Context) {
    private val dataStore = context.createDataStore("app_settings")
    // или PreferencesDataStore делегат

    val localServerUrl: Flow<String>  // default: "http://10.0.2.2:8081"
    suspend fun setLocalServerUrl(url: String)
}
```

Провайдится через новый `AppSettingsModule` в `AppComponent` (`@Singleton`). `LlmCoreFeatureComponent` получает `AppSettings` через `LlmCoreFeatureDeps`.

```kotlin
@Module
object AppSettingsModule {
    @Provides @Singleton
    fun provideAppSettings(@ApplicationContext context: Context): AppSettings = AppSettings(context)
}
```

### LocalLlmApi (интерфейс)

```kotlin
internal interface LocalLlmApi {
    suspend fun sendRequest(request: ChatCompletionRequest, serverUrl: String): ChatCompletionResponse
}
```

### Новые классы в data/remote/

```
LocalLlmApiImpl.kt
  — реализует LocalLlmApi
  — @Inject constructor(client: HttpClient, requestMapper: OpenAiModelRequestMapperImpl, responseMapper: OpenAiModelResponseMapperImpl)
  — POST {serverUrl}/v1/chat/completions, без Authorization header
  — HTTP non-2xx → бросает исключение (перехватывается в LlmRepositoryImpl → ModelResult.RuntimeError)

mappers/
  OpenAiModelRequestMapperImpl.kt   — ModelRequest → ChatCompletionRequest
  OpenAiModelResponseMapperImpl.kt  — ChatCompletionResponse → ModelResult
```

### LlmRepositoryImpl (обновлённый)

```kotlin
internal class LlmRepositoryImpl @Inject constructor(
    private val remoteApi: RemoteLlmApi,
    private val remoteRequestMapper: ModelRequestMapper,   // existing
    private val remoteResponseMapper: ModelResponseMapper, // existing
    private val localApi: LocalLlmApi,
    private val localRequestMapper: OpenAiModelRequestMapperImpl,
    private val localResponseMapper: OpenAiModelResponseMapperImpl,
    private val appSettings: AppSettings
) : LlmRepository {
    override suspend fun sendRequest(request: ModelRequest): ModelResult {
        return try {
            if (request.settings.isLocal) {
                val serverUrl = appSettings.localServerUrl.first()
                val dto = localRequestMapper.toDto(request)
                val response = localApi.sendRequest(dto, serverUrl)
                localResponseMapper.toDomain(response)
            } else {
                val dto = remoteRequestMapper.toDto(request)
                val result = remoteApi.sendRequest(dto, BuildConfig.LLM_API_KEY)
                remoteResponseMapper.toDomain(result)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ModelResult.RuntimeError(e.stackTraceToString())
        }
    }
}
```

### DI — LlmCoreFeatureModule

Только одно новое `@Binds` — для `LocalLlmApi`. Маперы без интерфейсов инжектятся через `@Inject` напрямую.

```kotlin
@Module
internal interface LlmCoreFeatureModule {
    // existing (без изменений)
    @Binds fun bindsRequestMapper(impl: ModelRequestMapperImpl): ModelRequestMapper
    @Binds fun bindsResponseMapper(impl: ModelResponseMapperImpl): ModelResponseMapper
    @Binds fun bindsApi(impl: RemoteLlmApiImpl): RemoteLlmApi
    @Binds fun bindsRepository(impl: LlmRepositoryImpl): LlmRepository
    @Binds fun bindsLlmRequestUseCase(impl: LlmRequestUseCaseImpl): LlmRequestUseCase

    // discuss_later
    @Binds fun bindsLocalApi(impl: LocalLlmApiImpl): LocalLlmApi
}
```

### UI изменения
- В настройках чата: `Switch` "Использовать локальную LLM (Ollama)" → устанавливает `isLocal`
- В глобальных настройках приложения: текстовое поле для URL ai-gateway сервера

---

## Что НЕ меняем (MVP scope)

- rag-server провайдеры — остаются как есть (нативный Ollama API, другой протокол)
- SSE / streaming — не в этой задаче
- Koog integration — не в этой задаче
- OpenRouterProvider в ai-gateway — не реализуется
- `reasoning` / `toolCalls` для локальной LLM — не поддерживаются в MVP

---

## Этапы реализации

| Этап | Что делаем | Зависимости |
|------|-----------|-------------|
| 1 | `shared/simple-chat-api` DTO + `settings.gradle.kts` | — |
| 2 | `ai-gateway` Ktor сервер (config, LlmProvider, OllamaProvider, routes, Dockerfile) | Этап 1 |
| 3 | `ModelSettings` + `ModelSettingsEntity` + маппер (без Room migration) | — |
| 4 | `AppSettings` + `AppSettingsModule` + AppComponent + LlmCoreFeatureDeps | — |
| 5 | `LocalLlmApi` + `LocalLlmApiImpl` + OpenAI маперы + `LlmRepositoryImpl` + DI | 1, 3, 4 |
| 6 | UI: Switch per-chat + глобальный URL в настройках | 3, 4 |
| 7 | `docker-compose.yml` — ai-gateway секция | Этап 2 |

---

## Критерии готовности

- [ ] Android с `isLocal=true` получает ответ от Ollama через ai-gateway
- [ ] OpenRouter с `isLocal=false` продолжает работать без изменений
- [ ] URL сервера меняется в настройках без пересборки
- [ ] ai-gateway собирается и запускается в Docker
- [ ] Архитектура готова к SSE и Koog без переписывания
