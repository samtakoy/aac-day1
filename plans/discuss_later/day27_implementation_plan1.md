# Day 27: Интеграция локальной LLM в приложение - Детальный план реализации

## 📋 Резюме задачи

**Цель:** Интегрировать локальную LLM (Ollama) в существующее Android-приложение чат-ассистентов через backend-сервер.

**Текущее состояние:**
- Android-приложение использует OpenRouter API для LLM взаимодействий
- Существует rag-server для RAG функциональности
- Архитектура построена на Clean Architecture принципах
- Каждый чат уже имеет экран настроек с ModelSettings

**Результат:**
- Новый backend-сервер (ai-gateway) для проксирования запросов к Ollama
- shared-chat-api модуль с общими DTO моделями
- Добавление галочки "Локальная LLM" в существующие настройки чата (ModelSettings)
- Отдельные мапперы для OpenRouter и OpenAI вариантов в data слое

---

## 🏗 Архитектура решения

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Android Application                       │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Chat Settings (ModelSettings)                       │   │
│  │  - name: "llama3"                                    │   │
│  │  - isLocal: true/false  ← НОВОЕ                      │   │
│  │  - localServerUrl: "http://192.168.1.x:8081" ← НОВОЕ│   │
│  │  - temperature, maxTokens, etc.                      │   │
│  └─────────────────────────────────────────────────────┘   │
│                          │                                   │
│                          ▼                                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  LlmRepository                                      │   │
│  │  ├─ if (isLocal) → LocalLlmApiImpl                  │   │
│  │  │   └─ OpenAiModelRequestMapper                    │   │
│  │  │   └─ OpenAiModelResponseMapper                   │   │
│  │  └─ else → RemoteLlmApiImpl (OpenRouter)            │   │
│  │      └─ ModelRequestMapper (existing)               │   │
│  │      └─ ModelResponseMapper (existing)              │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                          │
                          │ HTTP (OpenAI-compatible API)
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    AI Gateway Server (Ktor)                  │
│  Port: 8081 (configurable)                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  POST /v1/chat/completions                           │   │
│  └─────────────────────────────────────────────────────┘   │
│                          │                                   │
│                          ▼                                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  OllamaProvider                                      │   │
│  │  - baseUrl: http://localhost:11434                   │   │
│  │  - OpenAI-compatible API                             │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    Ollama (Local)                            │
│  Port: 11434                                                │
│  Models: llama3, mistral, etc.                              │
└─────────────────────────────────────────────────────────────┘
```

### Project Structure

```
aac-day1-other/
├── shared-chat-api/                          # Новый shared-chat-api модуль
│   ├── build.gradle.kts
│   └── src/main/kotlin/com/example/day/shared/
│       └── model/
│           ├── ChatCompletionRequest.kt
│           ├── ChatCompletionResponse.kt
│           ├── ChatCompletionChunk.kt
│           ├── Message.kt
│           ├── Choice.kt
│           ├── Delta.kt
│           ├── ToolDefinition.kt
│           ├── FunctionDef.kt
│           └── Model.kt
│
├── ai-gateway/                      # Новый сервер
│   ├── build.gradle.kts
│   ├── Dockerfile
│   ├── SETUP.md
│   └── src/main/kotlin/com/example/day/aigateway/
│       ├── AiGatewayServer.kt
│       ├── config/
│       │   └── AiGatewayConfig.kt
│       ├── provider/
│       │   ├── LlmProvider.kt
│       │   └── OllamaProvider.kt
│       └── routes/
│           └── ChatRoutes.kt
│
├── app/                             # Существующий Android app
│   └── src/main/java/com/example/day/
│       ├── core/core_features/llm/
│       │   ├── data/remote/
│       │   │   ├── RemoteLlmApi.kt (interface)
│       │   │   ├── RemoteLlmApiImpl.kt (OpenRouter)
│       │   │   ├── LocalLlmApiImpl.kt (new - OpenAI format)
│       │   │   └── mappers/
│       │   │       ├── ModelRequestMapperImpl.kt (existing - OpenRouter)
│       │   │       ├── ModelResponseMapperImpl.kt (existing - OpenRouter)
│       │   │       ├── OpenAiModelRequestMapperImpl.kt (new - OpenAI format)
│       │   │       └── OpenAiModelResponseMapperImpl.kt (new - OpenAI format)
│       │   └── di/
│       │       └── LlmCoreFeatureModule.kt (update)
│       │
│       └── core/core_features/llm/domain/model/
│           └── ModelSettings.kt (добавить поля isLocal, localServerUrl)
│
└── settings.gradle.kts              # Обновить для включения новых модулей
```

---

## 📝 Детальный план реализации

### Фаза 1: shared-chat-api Module (Общие DTO модели)

**Цель:** Создать shared-chat-api модуль с DTO моделями для OpenAI-compatible API.

#### Шаг 1.1: Создать структуру shared-chat-api модуля

**Файлы для создания:**
- [`shared-chat-api/build.gradle.kts`](shared-chat-api/build.gradle.kts)
- [`shared-chat-api/src/main/kotlin/com/example/day/shared/model/ChatCompletionRequest.kt`](shared-chat-api/src/main/kotlin/com/example/day/shared/model/ChatCompletionRequest.kt)
- [`shared-chat-api/src/main/kotlin/com/example/day/shared/model/ChatCompletionResponse.kt`](shared-chat-api/src/main/kotlin/com/example/day/shared/model/ChatCompletionResponse.kt)
- [`shared-chat-api/src/main/kotlin/com/example/day/shared/model/ChatCompletionChunk.kt`](shared-chat-api/src/main/kotlin/com/example/day/shared/model/ChatCompletionChunk.kt)
- [`shared-chat-api/src/main/kotlin/com/example/day/shared/model/Message.kt`](shared-chat-api/src/main/kotlin/com/example/day/shared/model/Message.kt)
- [`shared-chat-api/src/main/kotlin/com/example/day/shared/model/Choice.kt`](shared-chat-api/src/main/kotlin/com/example/day/shared/model/Choice.kt)
- [`shared-chat-api/src/main/kotlin/com/example/day/shared/model/Delta.kt`](shared-chat-api/src/main/kotlin/com/example/day/shared/model/Delta.kt)
- [`shared-chat-api/src/main/kotlin/com/example/day/shared/model/ToolDefinition.kt`](shared-chat-api/src/main/kotlin/com/example/day/shared/model/ToolDefinition.kt)
- [`shared-chat-api/src/main/kotlin/com/example/day/shared/model/FunctionDef.kt`](shared-chat-api/src/main/kotlin/com/example/day/shared/model/FunctionDef.kt)
- [`shared-chat-api/src/main/kotlin/com/example/day/shared/model/Model.kt`](shared-chat-api/src/main/kotlin/com/example/day/shared/model/Model.kt)

**Ключевые решения:**
- Использовать `@Serializable` с `@SerialName` для всех полей
- Следовать OpenAI API спецификации
- Поддержка `stream: Boolean` параметра
- Поддержка `tools` для future расширения

**Пример ChatCompletionRequest:**
```kotlin
@Serializable
data class ChatCompletionRequest(
    @SerialName("model")
    val model: String,
    @SerialName("messages")
    val messages: List<Message>,
    @SerialName("stream")
    val stream: Boolean = false,
    @SerialName("tools")
    val tools: List<ToolDefinition>? = null,
    @SerialName("temperature")
    val temperature: Double? = null,
    @SerialName("max_tokens")
    val maxTokens: Int? = null
)
```

#### Шаг 1.2: Обновить settings.gradle.kts

**Файл:** [`settings.gradle.kts`](settings.gradle.kts)

**Изменения:**
- Добавить `include(":shared-chat-api")`
- Добавить `include(":ai-gateway")`

---

### Фаза 2: AI Gateway Server (Backend)

**Цель:** Создать Ktor-сервер для проксирования запросов к Ollama.

#### Шаг 2.1: Создать структуру ai-gateway модуля

**Файлы для создания:**
- [`ai-gateway/build.gradle.kts`](ai-gateway/build.gradle.kts)
- [`ai-gateway/Dockerfile`](ai-gateway/Dockerfile)
- [`ai-gateway/SETUP.md`](ai-gateway/SETUP.md)

**Ключевые зависимости:**
```kotlin
dependencies {
    implementation(project(":shared-chat-api"))
    implementation("io.ktor:ktor-server-core:2.3.7")
    implementation("io.ktor:ktor-server-netty:2.3.7")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-okhttp:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}
```

#### Шаг 2.2: Реализовать конфигурацию сервера

**Файл:** [`ai-gateway/src/main/kotlin/com/example/day/aigateway/config/AiGatewayConfig.kt`](ai-gateway/src/main/kotlin/com/example/day/aigateway/config/AiGatewayConfig.kt)

**Конфигурация:**
- `serverPort` (default: 8081)
- `ollamaBaseUrl` (default: http://localhost:11434)
- `defaultModel` (default: llama3)

**Источники конфигурации:**
- Environment variables
- application.conf (HOCON)

#### Шаг 2.3: Реализовать LlmProvider интерфейс

**Файл:** [`ai-gateway/src/main/kotlin/com/example/day/aigateway/provider/LlmProvider.kt`](ai-gateway/src/main/kotlin/com/example/day/aigateway/provider/LlmProvider.kt)

**Интерфейс:**
```kotlin
interface LlmProvider {
    suspend fun chat(request: ChatCompletionRequest): ChatCompletionResponse
    suspend fun chatStreaming(request: ChatCompletionRequest): Flow<ChatCompletionChunk>
}
```

#### Шаг 2.4: Реализовать OllamaProvider

**Файл:** [`ai-gateway/src/main/kotlin/com/example/day/aigateway/provider/OllamaProvider.kt`](ai-gateway/src/main/kotlin/com/example/day/aigateway/provider/OllamaProvider.kt)

**Ключевые моменты:**
- Использовать Ktor Client для HTTP запросов
- Базовый URL: `http://localhost:11434`
- OpenAI-compatible endpoint: `/v1/chat/completions`

**Пример:**
```kotlin
class OllamaProvider(
    private val client: HttpClient,
    private val baseUrl: String
) : LlmProvider {
    override suspend fun chat(request: ChatCompletionRequest): ChatCompletionResponse {
        val response = client.post("$baseUrl/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return response.body()
    }
}
```

#### Шаг 2.5: Реализовать ChatRoutes

**Файл:** [`ai-gateway/src/main/kotlin/com/example/day/aigateway/routes/ChatRoutes.kt`](ai-gateway/src/main/kotlin/com/example/day/aigateway/routes/ChatRoutes.kt)

**Endpoints:**
- `POST /v1/chat/completions` - Chat completion (non-streaming для MVP)

**Пример:**
```kotlin
fun Route.chatRoutes(provider: LlmProvider) {
    post("/v1/chat/completions") {
        val request = call.receive<ChatCompletionRequest>()
        val response = provider.chat(request)
        call.respond(response)
    }
}
```

#### Шаг 2.6: Реализовать главный файл сервера

**Файл:** [`ai-gateway/src/main/kotlin/com/example/day/aigateway/AiGatewayServer.kt`](ai-gateway/src/main/kotlin/com/example/day/aigateway/AiGatewayServer.kt)

**Ключевые компоненты:**
- Инициализация Ktor сервера
- Установка ContentNegotiation с JSON
- Создание HttpClient для Ollama
- Инициализация OllamaProvider
- Регистрация routes

**Пример:**
```kotlin
fun main() {
    val config = AiGatewayConfig.from()
    
    embeddedServer(Netty, port = config.serverPort) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        
        val httpClient = HttpClient(OkHttp) {
            install(ContentNegotiation) { json() }
        }
        
        val ollamaProvider = OllamaProvider(httpClient, config.ollamaBaseUrl)
        
        routing {
            chatRoutes(ollamaProvider)
        }
    }.start(wait = true)
}
```

---

### Фаза 3: Android App - Обновление ModelSettings

**Цель:** Добавить галочку "Локальная LLM" в существующие настройки чата.

#### Шаг 3.1: Обновить ModelSettings domain модель

**Файл:** [`app/src/main/java/com/example/day/core/core_features/llm/domain/model/ModelSettings.kt`](app/src/main/java/com/example/day/core/core_features/llm/domain/model/ModelSettings.kt)

**Изменения:**
- Добавить поле `isLocal: Boolean = false`
- Добавить поле `localServerUrl: String? = null` (URL локального сервера)

**Пример:**
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
    val isLocal: Boolean = false,  // НОВОЕ: галочка "Локальная LLM"
    val localServerUrl: String? = null  // НОВОЕ: URL локального сервера
) {
    companion object {
        fun default(): ModelSettings = ModelSettings(ModelConst.DEFAULT_MODEL)
    }
}
```

#### Шаг 3.2: Обновить ModelSettingsEntity

**Файл:** [`app/src/main/java/com/example/day/core/core_features/llm/data/local/model/ModelSettingsEntity.kt`](app/src/main/java/com/example/day/core/core_features/llm/data/local/model/ModelSettingsEntity.kt)

**Изменения:**
- Добавить поля `isLocal` и `localServerUrl`

#### Шаг 3.3: Обновить ModelSettingsMapper

**Файл:** [`app/src/main/java/com/example/day/core/core_features/llm/data/local/mapper/ModelSettingsMapper.kt`](app/src/main/java/com/example/day/core/core_features/llm/data/local/mapper/ModelSettingsMapper.kt)

**Изменения:**
- Добавить маппинг новых полей в методах `toDomain` и `toEntity`

#### Шаг 3.4: Обновить UI настроек чата

**Файлы для обновления:**
- UI компоненты экрана настроек чата (где уже есть выбор модели)
- Добавить чекбокс "Локальная LLM"
- Добавить поле ввода URL сервера (показывается только если isLocal = true)

**Пример UI:**
```kotlin
// В существующем экране настроек чата
Checkbox(
    checked = settings.isLocal,
    onCheckedChange = { onIsLocalChanged(it) }
)
Text("Локальная LLM")

if (settings.isLocal) {
    TextField(
        value = settings.localServerUrl ?: "",
        onValueChange = { onLocalServerUrlChanged(it) },
        label = { Text("URL сервера") },
        placeholder = { Text("http://192.168.1.100:8081") }
    )
}
```

---

### Фаза 4: Android App - LLM Provider Integration

**Цель:** Интегрировать выбор провайдера в существующую LLM архитектуру с отдельными мапперами.

#### Шаг 4.1: Создать LocalLlmApiImpl

**Файл:** [`app/src/main/java/com/example/day/core/core_features/llm/data/remote/LocalLlmApiImpl.kt`](app/src/main/java/com/example/day/core/core_features/llm/data/remote/LocalLlmApiImpl.kt)

**Ключевые моменты:**
- Использовать существующий `RemoteLlmApi` интерфейс
- URL берется из ModelSettings (localServerUrl)
- Формат запроса/ответа совместим с OpenAI API (отличается от OpenRouter)

**Пример:**
```kotlin
class LocalLlmApiImpl(
    private val client: HttpClient,
    private val requestMapper: OpenAiModelRequestMapper,
    private val responseMapper: OpenAiModelResponseMapper
) : RemoteLlmApi {
    
    override suspend fun sendRequest(
        request: ChatRequestDto,
        apiKey: String
    ): ChatResultDto {
        // URL будет передан через request или выбран на основе настроек
        val localServerUrl = "http://192.168.1.100:8081" // TODO: брать из настроек
        
        val response = client.post("$localServerUrl/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return if (response.status.isSuccess()) {
            response.body<ChatResponseDto>()
        } else {
            response.body<ErrorResponseDto>()
        }
    }
}
```

#### Шаг 4.2: Создать отдельные мапперы для OpenAI формата

**Файлы для создания:**
- [`app/src/main/java/com/example/day/core/core_features/llm/data/remote/mappers/OpenAiModelRequestMapperImpl.kt`](app/src/main/java/com/example/day/core/core_features/llm/data/remote/mappers/OpenAiModelRequestMapperImpl.kt)
- [`app/src/main/java/com/example/day/core/core_features/llm/data/remote/mappers/OpenAiModelResponseMapperImpl.kt`](app/src/main/java/com/example/day/core/core_features/llm/data/remote/mappers/OpenAiModelResponseMapperImpl.kt)

**OpenAiModelRequestMapperImpl:**
- Маппинг ModelRequest → OpenAI ChatCompletionRequest
- Отличия от OpenRouter:
  - Нет поля `transforms`
  - Нет поля `reasoning`
  - Упрощенный формат

**OpenAiModelResponseMapperImpl:**
- Маппинг OpenAI ChatCompletionResponse → ModelResult
- Отличия от OpenRouter:
  - Нет поля `reasoning` в ответе
  - Упрощенный формат usage (без cost, costDetails)

#### Шаг 4.3: Обновить LlmCoreFeatureModule

**Файл:** [`app/src/main/java/com/example/day/core/core_features/llm/di/LlmCoreFeatureModule.kt`](app/src/main/java/com/example/day/core/core_features/llm/di/LlmCoreFeatureModule.kt)

**Изменения:**
- Добавить `LocalLlmApiImpl` в DI graph
- Добавить `OpenAiModelRequestMapperImpl` и `OpenAiModelResponseMapperImpl`
- Создать `LlmProviderSelector` для выбора провайдера на основе `ModelSettings.isLocal`

**Пример:**
```kotlin
@Module
class LlmCoreFeatureModule {
    
    @Provides
    @Singleton
    fun provideOpenAiModelRequestMapper(): OpenAiModelRequestMapperImpl {
        return OpenAiModelRequestMapperImpl()
    }
    
    @Provides
    @Singleton
    fun provideOpenAiModelResponseMapper(): OpenAiModelResponseMapperImpl {
        return OpenAiModelResponseMapperImpl()
    }
    
    @Provides
    @Singleton
    fun provideLocalLlmApi(
        client: HttpClient,
        requestMapper: OpenAiModelRequestMapperImpl,
        responseMapper: OpenAiModelResponseMapperImpl
    ): LocalLlmApiImpl {
        return LocalLlmApiImpl(client, requestMapper, responseMapper)
    }
    
    @Provides
    @Singleton
    fun provideLlmRepository(
        openRouterApi: RemoteLlmApiImpl,
        localLlmApi: LocalLlmApiImpl
    ): LlmRepository {
        return LlmRepositoryImpl(openRouterApi, localLlmApi)
    }
}
```

#### Шаг 4.4: Обновить LlmRepositoryImpl

**Файл:** [`app/src/main/java/com/example/day/core/core_features/llm/data/LlmRepositoryImpl.kt`](app/src/main/java/com/example/day/core/core_features/llm/data/LlmRepositoryImpl.kt)

**Изменения:**
- Добавить зависимость от `LocalLlmApiImpl`
- В методе `sendRequest` выбирать провайдер на основе `ModelSettings.isLocal`

**Пример:**
```kotlin
class LlmRepositoryImpl(
    private val openRouterApi: RemoteLlmApiImpl,
    private val localLlmApi: LocalLlmApiImpl
) : LlmRepository {
    
    override suspend fun sendRequest(request: ModelRequest): ModelResult {
        val api = if (request.modelSettings.isLocal) {
            localLlmApi
        } else {
            openRouterApi
        }
        
        val requestDto = request.toDto()
        return api.sendRequest(requestDto, request.modelSettings.apiKey)
    }
}
```

#### Шаг 4.5: Обновить AppComponent

**Файлы для обновления:**
- [`app/src/main/java/com/example/day/app/di/AppComponent.kt`](app/src/main/java/com/example/day/app/di/AppComponent.kt) - убедиться что все зависимости доступны

---

### Фаза 5: Тестирование и интеграция

**Цель:** Протестировать полный путь от Android-приложения до Ollama.

#### Шаг 5.1: Настройка Ollama

**Документация:** [`ai-gateway/SETUP.md`](ai-gateway/SETUP.md)

**Шаги:**
1. Установить Ollama: `brew install ollama` (macOS) или скачать с ollama.ai
2. Запустить Ollama: `ollama serve`
3. Скачать модель: `ollama pull llama3`
4. Проверить API: `curl http://localhost:11434/api/tags`

#### Шаг 5.2: Запуск AI Gateway

**Команды:**
```bash
cd ai-gateway
./gradlew run
```

**Проверка:**
```bash
# Тестовый запрос
curl -X POST http://localhost:8081/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "llama3",
    "messages": [{"role": "user", "content": "Hello!"}]
  }'
```

#### Шаг 5.3: Настройка Android-приложения

**Шаги:**
1. Запустить Android-приложение
2. Открыть настройки существующего чата (или создать новый)
3. В настройках модели поставить галочку "Локальная LLM"
4. Ввести URL: `http://192.168.1.100:8081` (IP компьютера с Ollama)
5. Сохранить настройки

#### Шаг 5.4: Тестирование чата

**Шаги:**
1. Открыть чат с включенной галочкой "Локальная LLM"
2. Отправить сообщение
3. Проверить что ответ приходит от локальной LLM
4. Убрать галочку "Локальная LLM" в настройках
5. Проверить что ответ приходит от OpenRouter

---

## 🔧 Технические детали

### OpenAI-compatible API контракт

#### Request (POST /v1/chat/completions)
```json
{
  "model": "llama3",
  "messages": [
    {"role": "system", "content": "You are a helpful assistant."},
    {"role": "user", "content": "Hello!"}
  ],
  "stream": false,
  "temperature": 0.7,
  "max_tokens": 1000
}
```

#### Response (non-stream)
```json
{
  "id": "chatcmpl-123",
  "object": "chat.completion",
  "created": 1677652288,
  "model": "llama3",
  "choices": [{
    "index": 0,
    "message": {
      "role": "assistant",
      "content": "Hello! How can I help you today?"
    },
    "finish_reason": "stop"
  }],
  "usage": {
    "prompt_tokens": 9,
    "completion_tokens": 12,
    "total_tokens": 21
  }
}
```

#### Response (stream - для future расширения)
```
data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1677652288,"model":"llama3","choices":[{"index":0,"delta":{"role":"assistant","content":""},"finish_reason":null}]}

data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1677652288,"model":"llama3","choices":[{"index":0,"delta":{"content":"Hello"},"finish_reason":null}]}

data: [DONE]
```

### Ollama API mapping

Ollama предоставляет OpenAI-совместимый API:
- `POST /v1/chat/completions` - работает как OpenAI

### Отличия OpenRouter и OpenAI форматов

#### OpenRouter (текущий)
- Поле `transforms` - трансформации для ответа модели
- Поле `reasoning` - reasoning effort
- Поле `logprobs` - лог-вероятности
- Поле `top_logprobs` - количество top logprobs

#### OpenAI (новый)
- Нет поля `transforms`
- Нет поля `reasoning`
- Нет поля `logprobs`
- Нет поля `top_logprobs`
- Упрощенный формат usage (без cost, costDetails)

**Маппинг мапперов:**
- `ModelRequestMapperImpl` → OpenRouter формат (существующий)
- `OpenAiModelRequestMapperImpl` → OpenAI формат (новый)
- `ModelResponseMapperImpl` → OpenRouter формат (существующий)
- `OpenAiModelResponseMapperImpl` → OpenAI формат (новый)

---

## 📊 Оценка сложности и приоритеты

### Приоритет 1 (MVP - Must Have)
- [ ] shared-chat-api модуль с DTO моделями
- [ ] AI Gateway сервер с OllamaProvider
- [ ] POST /v1/chat/completions endpoint
- [ ] Обновление ModelSettings (добавить isLocal, localServerUrl)
- [ ] Обновление UI настроек чата (галочка "Локальная LLM")
- [ ] LocalLlmApiImpl
- [ ] OpenAiModelRequestMapperImpl и OpenAiModelResponseMapperImpl
- [ ] Интеграция выбора провайдера в LlmRepository

### Приоритет 2 (Should Have)
- [ ] Обработка ошибок и retry логика
- [ ] Логирование запросов/ответов
- [ ] Валидация URL локального сервера

### Приоритет 3 (Nice to Have - Future)
- [ ] Streaming поддержка (SSE)
- [ ] Tool calling поддержка
- [ ] Аутентификация
- [ ] Мониторинг и метрики
- [ ] Docker Compose для запуска всего стека
- [ ] GET /v1/models endpoint (если понадобится)

---

## 🚀 Порядок выполнения

### Неделя 1: Фундамент
1. **День 1-2:** shared-chat-api модуль + DTO модели
2. **День 3-4:** AI Gateway сервер (базовая структура)
3. **День 5:** OllamaProvider реализация

### Неделя 2: Интеграция
4. **День 6-7:** Обновление ModelSettings + UI настроек чата
5. **День 8-9:** LocalLlmApiImpl + OpenAI мапперы + интеграция в LlmRepository
6. **День 10:** Тестирование полного пути

### Неделя 3: Полировка
7. **День 11-12:** Обработка ошибок + retry логика
8. **День 13-14:** Валидация + логирование
9. **День 15:** Документация + SETUP.md

---

## ⚠️ Риски и митигации

### Риск 1: Ollama не установлен у пользователя
**Митигация:** 
- Clear error message в настройках чата
- Link на инструкцию по установке
- Fallback на OpenRouter если Local недоступен

### Риск 2: Сетевые проблемы (Android → Server)
**Митигация:**
- Timeout handling
- Retry с exponential backoff
- Offline detection

### Риск 3: Несовместимость форматов (OpenRouter vs OpenAI)
**Митигация:**
- Отдельные мапперы для каждого формата
- Тщательное тестирование с реальной Ollama
- Логирование запросов/ответов
- Graceful degradation

### Риск 4: Производительность локальной LLM
**Митигация:**
- Индикация "думаю..." в UI
- Timeout для долгих запросов
- Возможность отмены запроса

---

## 📚 Ссылки и ресурсы

### Документация
- [OpenAI API Reference](https://platform.openai.com/docs/api-reference)
- [Ollama Documentation](https://github.com/ollama/ollama)
- [Ktor Documentation](https://ktor.io/)
- [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore)

### Существующий код
- [`app/src/main/java/com/example/day/core/core_features/llm/data/remote/RemoteLlmApiImpl.kt`](app/src/main/java/com/example/day/core/core_features/llm/data/remote/RemoteLlmApiImpl.kt) - пример OpenRouter интеграции
- [`rag-server/src/main/kotlin/com/example/day/ragserver/RagServer.kt`](rag-server/src/main/kotlin/com/example/day/ragserver/RagServer.kt) - пример Ktor сервера
- [`rag-server/src/main/kotlin/com/example/day/ragserver/indexing/OllamaLlmProvider.kt`](rag-server/src/main/kotlin/com/example/day/ragserver/indexing/OllamaLlmProvider.kt) - пример Ollama интеграции

---

## ✅ Критерии готовности (Definition of Done)

- [ ] AI Gateway сервер запускается и принимает запросы
- [ ] POST /v1/chat/completions работает с Ollama
- [ ] Android-приложение может переключаться между OpenRouter и Local через галочку в настройках чата
- [ ] Настройки (isLocal, localServerUrl) сохраняются в ModelSettings
- [ ] Отдельные мапперы для OpenRouter и OpenAI форматов работают корректно
- [ ] Ошибки обрабатываются gracefully
- [ ] Документация SETUP.md обновлена
- [ ] Код соответствует Clean Architecture принципам
- [ ] Все @SerialName и @ColumnInfo анотации добавлены
- [ ] Нет magic constants - все в objects

---

## 🎯 Следующие шаги после Day 27

1. **Streaming поддержка** - добавить SSE для real-time ответов
2. **Tool calling** - интегрировать существующую tool систему
3. **Аутентификация** - добавить API key или JWT
4. **Мониторинг** - добавить метрики и логирование
5. **Docker Compose** - упростить запуск всего стека
6. **shared-chat-api module расширение** - вынести больше общей логики

---

*План создан: 2026-03-24*
*Версия: 1.1 (с учетом корректировок)*
*Автор: AI Assistant*
