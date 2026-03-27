# Day 27: Интеграция локальной LLM (Ollama) в приложение

## Детальный план реализации

**Статус:** План  
**Дата:** 24 марта 2026  
**Цель:** Интегрировать локальную LLM (Ollama) в существующее Android-приложение через backend-сервер

---

## 📋 Резюме решений

| Параметр | Решение |
|----------|---------|
| **Серверный модуль** | `ai-gateway` — отдельный Ktor сервер |
| **Shared модуль** | `shared-chat-api` — KMP модуль с DTO |
| **Стриминг** | Non-streaming MVP, архитектура с учётом SSE |
| **Koog** | Добавляется позже, архитектура должна поддерживать |
| **Клиент API** | Отдельный `LocalLlmApiImpl` для локальной LLM |

---

## 🏗 Архитектура решения

```
┌─────────────────────────────────────────────────────────────────┐
│                    Android Application (Day)                    │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  ModelSettings (per chat)                                  │  │
│  │  - name: "llama3"                                          │  │
│  │  - isLocal: Boolean  ← НОВОЕ                                │  │
│  │  - localServerUrl: String?  ← НОВОЕ                        │  │
│  │  - temperature, maxTokens, etc.                            │  │
│  └───────────────────────────────────────────────────────────┘  │
│                            │                                     │
│                            ▼                                     │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  LlmRepository                                             │  │
│  │  ├── isLocal=true → LocalLlmApiImpl → OpenAI format       │  │
│  │  └── isLocal=false → RemoteLlmApiImpl → OpenRouter format │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                           │ HTTP (OpenAI-compatible)
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    ai-gateway (Ktor Server)                       │
│  Port: 8081 (configurable via env)                               │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  POST /v1/chat/completions  ← OpenAI-compatible endpoint   │  │
│  │  GET  /v1/models                                            │  │
│  └───────────────────────────────────────────────────────────┘  │
│                            │                                     │
│                            ▼                                     │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  LlmProvider (interface)                                    │  │
│  │  ├── OllamaProvider  ← Локальные модели                    │  │
│  │  └── OpenRouterProvider  ← Облачные модели                 │  │
│  └───────────────────────────────────────────────────────────┘  │
│                            │                                     │
│                            ▼                                     │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  LlmRouter                                                 │  │
│  │  - model.startsWith("local/") → OllamaProvider            │  │
│  │  - model.startsWith("openrouter/") → OpenRouterProvider   │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                           │
          ┌────────────────┴────────────────┐
          ▼                                 ▼
┌─────────────────────┐          ┌─────────────────────┐
│      Ollama          │          │     OpenRouter      │
│   (localhost:11434)  │          │   (cloud API)        │
└─────────────────────┘          └─────────────────────┘
```

---

## 📁 Структура проекта

```
aac-day1-other/
├── app/                              # Android клиент (существующий)
│   └── src/main/java/com/example/day/
│       └── core/core_features/llm/
│           ├── data/
│           │   ├── LlmRepositoryImpl.kt  # Обновить: выбор API по isLocal
│           │   ├── remote/
│           │   │   ├── LocalLlmApiImpl.kt    ← НОВОЕ
│           │   │   ├── RemoteLlmApiImpl.kt   # OpenRouter (существует)
│           │   │   └── mappers/
│           │   │       ├── OpenAiModelRequestMapperImpl.kt   ← НОВОЕ
│           │   │       └── OpenAiModelResponseMapperImpl.kt  ← НОВОЕ
│           │   └── di/
│           │       └── LlmCoreFeatureModule.kt  # Обновить
│           └── domain/model/
│               └── ModelSettings.kt  # Добавить isLocal, localServerUrl
│
├── shared-chat-api/                   ← НОВЫЙ KMP модуль
│   ├── build.gradle.kts
│   └── src/
│       └── commonMain/
│           └── kotlin/
│               └── com/example/day/shared/
│                   └── dto/
│                       ├── ChatCompletionRequest.kt
│                       ├── ChatCompletionResponse.kt
│                       ├── ChatCompletionChunk.kt   # Для будущего SSE
│                       ├── Message.kt
│                       ├── Choice.kt
│                       ├── Delta.kt
│                       └── Usage.kt
│
├── ai-gateway/                        ← НОВЫЙ модуль
│   ├── build.gradle.kts
│   ├── Dockerfile
│   ├── SETUP.md
│   └── src/main/kotlin/com/example/day/aigateway/
│       ├── AiGatewayServer.kt        # Точка входа (main)
│       ├── config/
│       │   └── AiGatewayConfig.kt     # Конфигурация из env
│       ├── api/
│       │   └── routes/
│       │       └── ChatRoutes.kt      # /v1/chat/completions, /v1/models
│       ├── llm/
│       │   ├── LlmProvider.kt         # Интерфейс
│       │   ├── OllamaProvider.kt       # Ollama реализация
│       │   ├── OpenRouterProvider.kt  # OpenRouter реализация
│       │   └── LlmRouter.kt           # Роутинг по model name
│       └── dto/
│           └── ServerModels.kt        # Локальные DTO если нужны
│
├── rag-server/                        # Существующий (RAG функциональность)
│
├── mcp-server/                        # Существующий (GitHub MCP)
│
└── settings.gradle.kts               # Обновить: добавить ai-gateway, shared-chat-api
```

---

## 📝 Поэтапный план реализации

---

### Этап 1: Shared Module (shared-chat-api)

**Цель:** Создать общие DTO для OpenAI-compatible API (используются и клиентом, и сервером)

#### 1.1 Создать структуру модуля `shared-chat-api`

**Файлы для создания:**

| Файл | Описание |
|------|----------|
| `shared-chat-api/build.gradle.kts` | KMP конфигурация (jvm, android) |
| `shared-chat-api/src/commonMain/kotlin/com/example/day/shared/dto/ChatCompletionRequest.kt` | OpenAI Chat Completion Request |
| `shared-chat-api/src/commonMain/kotlin/com/example/day/shared/dto/ChatCompletionResponse.kt` | OpenAI Chat Completion Response (non-streaming) |
| `shared-chat-api/src/commonMain/kotlin/com/example/day/shared/dto/ChatCompletionChunk.kt` | OpenAI Chunk (для SSE future) |
| `shared-chat-api/src/commonMain/kotlin/com/example/day/shared/dto/Message.kt` | Role + content message |
| `shared-chat-api/src/commonMain/kotlin/com/example/day/shared/dto/Choice.kt` | Choice в ответе |
| `shared-chat-api/src/commonMain/kotlin/com/example/day/shared/dto/Delta.kt` | Delta для chunk |
| `shared-chat-api/src/commonMain/kotlin/com/example/day/shared/dto/Usage.kt` | Token usage stats |

#### 1.2 Обновить `settings.gradle.kts`

```kotlin
include(":app")
include(":mcp-server")
include(":rag-server")
include(":ai-gateway")           // ← Добавить
include(":shared-chat-api")     // ← Добавить
```

**Ключевые решения:**
- Использовать `@Serializable` с `@SerialName` для всех полей
- Следовать OpenAI API спецификации точно
- Поддержка `stream: Boolean` параметра (дляfuture SSE)
- KMP: `commonMain` общий, `jvm()` для сервера, `androidTarget()` для клиента

---

### Этап 2: AI Gateway Server (ai-gateway)

**Цель:** Создать Ktor-сервер с OpenAI-compatible API

#### 2.1 Создать структуру модуля `ai-gateway`

**build.gradle.kts зависимости:**
```kotlin
dependencies {
    implementation(project(":shared-chat-api"))
    
    // Ktor Server
    implementation("io.ktor:ktor-server-core:2.3.7")
    implementation("io.ktor:ktor-server-netty:2.3.7")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-server-sse:2.3.7")  // Для future SSE
    
    // Ktor Client (для вызовов к Ollama/OpenRouter)
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-okhttp:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    
    // Сериализация
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}
```

#### 2.2 Реализовать конфигурацию

**`AiGatewayConfig.kt`:**
```kotlin
data class AiGatewayConfig(
    val serverPort: Int = 8081,
    val ollamaBaseUrl: String = "http://localhost:11434",
    val openrouterApiKey: String? = null,
    val defaultModel: String = "local/llama3"
) {
    companion object {
        fun fromEnvironment(): AiGatewayConfig = AiGatewayConfig(
            serverPort = System.getenv("AI_GATEWAY_PORT")?.toIntOrNull() ?: 8081,
            ollamaBaseUrl = System.getenv("OLLAMA_BASE_URL") ?: "http://localhost:11434",
            openrouterApiKey = System.getenv("OPENROUTER_API_KEY"),
            defaultModel = System.getenv("DEFAULT_MODEL") ?: "local/llama3"
        )
    }
}
```

#### 2.3 Реализовать LLM Provider слой

**`LlmProvider.kt` (интерфейс):**
```kotlin
interface LlmProvider {
    suspend fun chat(request: ChatCompletionRequest): ChatCompletionResponse
    // Future: fun chatStreaming(request: ChatCompletionRequest): Flow<ChatCompletionChunk>
}
```

**`OllamaProvider.kt`:**
- Использовать Ollama `/api/chat` endpoint (НЕ `/api/generate`)
- Маппить OpenAI request → Ollama format
- Маппить Ollama response → OpenAI format
- Non-streaming для MVP

**`OpenRouterProvider.kt`:**
- Полностью OpenAI-compatible (не требует маппинга)
- Добавить `Authorization: Bearer $apiKey` header

**`LlmRouter.kt`:**
```kotlin
class LlmRouter(
    private val ollama: OllamaProvider,
    private val openRouter: OpenRouterProvider
) : LlmProvider {
    
    override suspend fun chat(request: ChatCompletionRequest): ChatCompletionResponse {
        return when {
            request.model.startsWith("local/") -> {
                val localRequest = request.copy(model = request.model.removePrefix("local/"))
                ollama.chat(localRequest)
            }
            else -> openRouter.chat(request)
        }
    }
}
```

#### 2.4 Реализовать API Routes

**`ChatRoutes.kt`:**
```kotlin
fun Route.chatRoutes(llmRouter: LlmRouter) {
    
    post("/v1/chat/completions") {
        val request = call.receive<ChatCompletionRequest>()
        val response = llmRouter.chat(request)
        call.respond(response)
    }
    
    get("/v1/models") {
        call.respond(
            mapOf(
                "data" to listOf(
                    mapOf("id" to "local/llama3"),
                    mapOf("id" to "local/qwen2.5-coder"),
                    mapOf("id" to "openrouter/gpt-4")
                )
            )
        )
    }
}
```

#### 2.5 Создать Dockerfile и SETUP.md

**Dockerfile:**
```dockerfile
FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle :ai-gateway:shadowJar

FROM eclipse-temurin:17-jre
COPY --from=build /app/ai-gateway/build/libs/*-all.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**SETUP.md:** Инструкции по запуску, env variables, примеры запросов

---

### Этап 3: Android App — ModelSettings Update

**Цель:** Добавить поля `isLocal` и `localServerUrl` в настройки модели

#### 3.1 Обновить `ModelSettings.kt` (domain)

```kotlin
data class ModelSettings(
    val name: String,
    val stopSequence: ImmutableList<String> = emptyList<String>().toImmutableList(),
    val maxTokens: Int? = null,
    val maxCompletionTokens: Int? = null,
    val jsonFormat: Boolean = false,
    val temperature: Double? = null,
    val topP: Double? = null,
    val topK: Int? = null,
    val presencePenalty: Double? = null,
    val frequencyPenalty: Double? = null,
    val seed: Int? = null,
    val reasoningEffort: String? = null,
    val isLocal: Boolean = false,           // ← НОВОЕ
    val localServerUrl: String? = null      // ← НОВОЕ
) {
    companion object {
        fun default(): ModelSettings = ModelSettings(ModelConst.DEFAULT_MODEL)
    }
}
```

#### 3.2 Обновить `ModelSettingsEntity.kt` (data layer)

Добавить поля `is_local` (INTEGER) и `local_server_url` (TEXT) в Room entity.

#### 3.3 Обновить `ModelSettingsMapper.kt`

Маппинг в обоих направлениях: domain ↔ entity.

#### 3.4 Обновить UI настроек чата

В существующем экране настроек чата:
- Добавить `Checkbox` "Локальная LLM"
- Если `isLocal = true`, показать `TextField` для `localServerUrl`
- Валидация: если `isLocal = true`, `localServerUrl` не может быть пустым

---

### Этап 4: Android App — LocalLlmApiImpl

**Цель:** Создать отдельный API имплементацию для локальной LLM

#### 4.1 Создать `LocalLlmApiImpl.kt`

```kotlin
internal class LocalLlmApiImpl(
    private val client: HttpClient,
    private val requestMapper: OpenAiModelRequestMapper,
    private val responseMapper: OpenAiModelResponseMapper
) : RemoteLlmApi {
    
    override suspend fun sendRequest(
        request: ChatRequestDto,
        apiKey: String,
        localServerUrl: String
    ): ChatResultDto {
        val response = client.post("$localServerUrl/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return response.body<ChatResultDto>()
    }
}
```

#### 4.2 Создать мапперы для OpenAI формата

**`OpenAiModelRequestMapperImpl.kt`:**
- Маппит `ModelRequest` → `ChatCompletionRequest` (OpenAI format)
- Отличия от OpenRouter: нет поля `transforms`, `reasoning`

**`OpenAiModelResponseMapperImpl.kt`:**
- Маппит `ChatCompletionResponse` → `ModelResult`
- Отличия: нет `reasoning`, упрощённый `usage` без cost details

#### 4.3 Обновить `LlmCoreFeatureModule.kt`

```kotlin
@Module
class LlmCoreFeatureModule {
    
    @Provides
    @Singleton
    fun provideOpenAiModelRequestMapper(): OpenAiModelRequestMapper = OpenAiModelRequestMapperImpl()
    
    @Provides
    @Singleton
    fun provideOpenAiModelResponseMapper(): OpenAiModelResponseMapper = OpenAiModelResponseMapperImpl()
    
    @Provides
    @Singleton
    fun provideLocalLlmApi(
        client: HttpClient,
        requestMapper: OpenAiModelRequestMapper,
        responseMapper: OpenAiModelResponseMapper
    ): LocalLlmApiImpl = LocalLlmApiImpl(client, requestMapper, responseMapper)
    
    @Provides
    @Singleton
    fun provideLlmRepository(
        openRouterApi: RemoteLlmApiImpl,
        localLlmApi: LocalLlmApiImpl
    ): LlmRepository = LlmRepositoryImpl(openRouterApi, localLlmApi)
}
```

#### 4.4 Обновить `LlmRepositoryImpl.kt`

```kotlin
internal class LlmRepositoryImpl @Inject constructor(
    private val openRouterApi: RemoteLlmApi,
    private val localLlmApi: LocalLlmApi,
    private val settingsRepository: ChatSettingsRepository  // для получения localServerUrl
) : LlmRepository {
    
    override suspend fun sendRequest(request: ModelRequest): ModelResult {
        val settings = settingsRepository.getSettings(request.chatId)
        
        return if (settings.isLocal) {
            val localUrl = settings.localServerUrl ?: error("localServerUrl required for local LLM")
            localLlmApi.sendRequest(
                request = requestMapper.toDto(request),
                apiKey = "",  // не нужен для локальной Ollama
                localServerUrl = localUrl
            )
        } else {
            openRouterApi.sendRequest(
                request = requestMapper.toDto(request),
                apiKey = BuildConfig.LLM_API_KEY
            )
        }
    }
}
```

---

### Этап 5: Docker Compose обновление

Добавить `ai-gateway` в `docker-compose.yml`:

```yaml
services:
  ai-gateway:
    build:
      context: .
      dockerfile: ai-gateway/Dockerfile
    container_name: ai-gateway
    ports:
      - "8081:8081"
    environment:
      - OLLAMA_BASE_URL=http://host.docker.internal:11434
      - OPENROUTER_API_KEY=${OPENROUTER_API_KEY}
      - DEFAULT_MODEL=local/llama3
    restart: unless-stopped
    extra_hosts:
      - "host.docker.internal:host-gateway"
```

---

## 🔜 Следующие шаги (после MVP)

### Future: SSE Streaming

1. Добавить `ktor-server-sse` dependency
2. В `LlmProvider` добавить: `fun chatStreaming(request): Flow<ChatCompletionChunk>`
3. В `OllamaProvider`: использовать `stream: true` и парсить NDJSON линии
4. В `ChatRoutes`: использовать `call.respondSse { }` вместо `call.respond()`

### Future: Koog Integration

1. Добавить Koog dependency в `ai-gateway`
2. Создать `AgentService` который использует `LlmRouter`
3. В `AgentService` добавить tool calling, reasoning, memory
4. Endpoint `/v1/chat/completions` будет вызывать `AgentService` вместо прямого `LlmRouter`

### Future: Chat History

1. Добавить SQLite/PostgreSQL в `ai-gateway`
2. Создать `ChatRepository` интерфейс
3. `AgentService` загружает историю перед вызовом LLM

---

## ✅ Чеклист перед началом реализации

- [ ] Подтвердить структуру проекта
- [ ] Проверить совместимость Ktor версий (2.3.7)
- [ ] Определить default port для ai-gateway (8081)
- [ ] Определить default Ollama URL (http://localhost:11434)
- [ ] Решить: использовать ли окружение или захардкодить для MVP

---

## ⚠️ Известные риски

| Риск | Mitigation |
|------|------------|
| Ollama не поддерживает точный OpenAI API | Маппинг в OllamaProvider |
| Android устройство не имеет доступа к локальному серверу | localServerUrl должен быть reachable (LAN IP) |
| Разные версии Ktor на клиенте и сервере | Использовать одинаковые версии |

---

## 📚 Ключевые файлы для изменения

### Создаваемые файлы (NEW)

| Путь | Описание |
|------|----------|
| `shared-chat-api/build.gradle.kts` | KMP build config |
| `shared-chat-api/src/commonMain/kotlin/.../dto/*.kt` | 8 DTO файлов |
| `ai-gateway/build.gradle.kts` | Server build config |
| `ai-gateway/Dockerfile` | Container definition |
| `ai-gateway/SETUP.md` | Setup instructions |
| `ai-gateway/src/main/kotlin/.../AiGatewayServer.kt` | Main entry point |
| `ai-gateway/src/main/kotlin/.../config/AiGatewayConfig.kt` | Config from env |
| `ai-gateway/src/main/kotlin/.../api/routes/ChatRoutes.kt` | API endpoints |
| `ai-gateway/src/main/kotlin/.../llm/LlmProvider.kt` | Interface |
| `ai-gateway/src/main/kotlin/.../llm/OllamaProvider.kt` | Ollama implementation |
| `ai-gateway/src/main/kotlin/.../llm/OpenRouterProvider.kt` | OpenRouter implementation |
| `ai-gateway/src/main/kotlin/.../llm/LlmRouter.kt` | Runtime routing |
| `app/.../llm/data/remote/LocalLlmApiImpl.kt` | Client local API |
| `app/.../llm/data/remote/mappers/OpenAiModelRequestMapperImpl.kt` | Request mapper |
| `app/.../llm/data/remote/mappers/OpenAiModelResponseMapperImpl.kt` | Response mapper |

### Изменяемые файлы (MODIFY)

| Путь | Изменения |
|------|-----------|
| `settings.gradle.kts` | Add include for new modules |
| `app/.../llm/domain/model/ModelSettings.kt` | Add isLocal, localServerUrl |
| `app/.../llm/data/local/model/ModelSettingsEntity.kt` | Add is_local, local_server_url |
| `app/.../llm/data/local/mapper/ModelSettingsMapper.kt` | Map new fields |
| `app/.../llm/data/LlmRepositoryImpl.kt` | Add local API selection |
| `app/.../llm/di/LlmCoreFeatureModule.kt` | Add LocalLlmApiImpl, mappers |
| `docker-compose.yml` | Add ai-gateway service |

---

**Далее:** После подтверждения плана — переход в Code режим для реализации.