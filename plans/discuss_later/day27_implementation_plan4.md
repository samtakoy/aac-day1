# Day 27: Интеграция локальной LLM (Ollama) в приложение Day

**Статус:** Design  
**Дата:** 24 марта 2026  
**Цель:** Интегрировать локальную LLM (Ollama) в существующее Android-приложение через backend-сервер

---

## 📋 Резюме требований

| Параметр | Решение                                             |
|----------|-----------------------------------------------------|
| **Scope** | MVP - только Ollama работает                        |
| **Shared Module** | shared/simple-chat-api (не KMP)                     |
| **Streaming** | Non-streaming сейчас, SSE потом                     |
| **Koog** | Архитектура должна поддерживать позже, но не сейчас |
| **Android API** | Отдельный LocalLlmApiImpl для локальной LLM         |

---

## 🏗 Архитектура решения

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Android Application (Day)                     │
│  ┌───────────────────────────────────────────────────────────┐   │
│  │  ModelSettings (per chat)                                  │
│  │  - name: "llama3"                                          │
│  │  - isLocal: Boolean  ← НОВОЕ                               │
│  │  - localServerUrl: String?  ← НОВОЕ                        │
│  │  - temperature, maxTokens, etc.                            │
│  └───────────────────────────────────────────────────────────┘   │
│                            │                                     │
│                            ▼                                     │
│  ┌───────────────────────────────────────────────────────────┐   │
│  │  LlmRepository                                             │   │
│  │  ├── isLocal=true → LocalLlmApiImpl → OpenAI format       │
│  │  └── isLocal=false → RemoteLlmApiImpl → OpenRouter format │
│  └───────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                            │ HTTP (OpenAI-compatible)
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    ai-gateway (Ktor Server)                       │
│  Port: 8081 (configurable via env)                              │
│  ┌───────────────────────────────────────────────────────────┐   │
│  │  POST /v1/chat/completions  ← OpenAI-compatible endpoint │
│  │  GET  /v1/models                                             │
│  └───────────────────────────────────────────────────────────┘   │
│                            │                                     │
│                            ▼                                     │
│  ┌───────────────────────────────────────────────────────────┐   │
│  │  LlmRouter (interface-based)                               │
│  │  ├── OllamaProvider  ← Локальные модели                   │
│  │  └── OpenRouterProvider  ← Облачные модели               │
│  └───────────────────────────────────────────────────────────┘   │
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
│           │   ├── LlmRepositoryImpl.kt      # Обновить: выбор API по isLocal
│           │   ├── remote/
│           │   │   ├── LocalLlmApiImpl.kt     ← НОВОЕ
│           │   │   ├── RemoteLlmApiImpl.kt    # OpenRouter (существует)
│           │   │   └── mappers/
│           │   │       ├── ModelRequestMapperImpl.kt      # OpenRouter
│           │   │       ├── ModelResponseMapperImpl.kt     # OpenRouter
│           │   │       ├── OpenAiModelRequestMapperImpl.kt   ← НОВОЕ
│           │   │       └── OpenAiModelResponseMapperImpl.kt  ← НОВОЕ
│           │   └── di/
│           │       └── LlmCoreFeatureModule.kt  # Обновить
│           └── domain/model/
│               └── ModelSettings.kt  # Добавить isLocal, localServerUrl
│
├── shared/simple-chat-api/                  ← НОВЫЙ модуль (shared DTO)
│   ├── build.gradle.kts
│   └── src/main/kotlin/com/example/day/shared/
│       └── dto/
│           ├── ChatCompletionRequest.kt
│           ├── ChatCompletionResponse.kt
│           ├── Message.kt
│           ├── Choice.kt
│           └── Usage.kt
│
├── ai-gateway/                       ← НОВЫЙ модуль
│   ├── build.gradle.kts
│   ├── Dockerfile
│   ├── SETUP.md
│   └── src/main/kotlin/com/example/day/aigateway/
│       ├── AiGatewayServer.kt       # Точка входа (main)
│       ├── config/
│       │   └── AiGatewayConfig.kt    # Конфигурация из env
│       ├── api/
│       │   └── routes/
│       │       └── ChatRoutes.kt     # /v1/chat/completions
│       └── llm/
│           ├── LlmProvider.kt       # Интерфейс
│           ├── OllamaProvider.kt    # Ollama реализация
│           ├── OpenRouterProvider.kt # OpenRouter реализация
│           └── LlmRouter.kt         # Роутинг по model name
│
├── rag-server/                       # Существующий (RAG)
├── mcp-server/                        # Существующий (GitHub MCP)
└── settings.gradle.kts              # Обновить: добавить ai-gateway, shared/simple-chat-api
```

---

## 🔑 Ключевые решения

### 1. shared/simple-chat-api Module (не KMP)

**Почему не KMP:**
- DTO простые (data classes с @Serializable)
- KMP добавил бы complexity без пользы для MVP
- Можно легко скопировать DTO в оба проекта или использовать симлинки

**Структура:**
```kotlin
@Serializable
data class ChatCompletionRequest(
    @SerialName("model") val model: String,
    @SerialName("messages") val messages: List<Message>,
    @SerialName("stream") val stream: Boolean = false,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    @SerialName("temperature") val temperature: Double? = null,
    @SerialName("tools") val tools: List<ToolDefinition>? = null
)
```

### 2. ai-gateway Server

**Ktor версия:** 2.3.7 (совместимость с rag-server)

**Endpoints:**
- `POST /v1/chat/completions` - non-streaming chat completion
- `GET /v1/models` - список доступных моделей

**LlmRouter логика:**
```kotlin
class LlmRouter(...) {
    suspend fun chat(request: ChatCompletionRequest): ChatCompletionResponse {
        return when {
            request.model.startsWith("local/") -> ollama.chat(request)
            else -> openRouter.chat(request)
        }
    }
}
```

### 3. Android Client Changes

**ModelSettings additions:**
```kotlin
data class ModelSettings(
    // ... existing fields ...
    val isLocal: Boolean = false,           // ← НОВОЕ
    val localServerUrl: String? = null      // ← НОВОЕ
)
```

**LlmRepositoryImpl updated:**
```kotlin
class LlmRepositoryImpl(
    private val openRouterApi: RemoteLlmApi,
    private val localLlmApi: LocalLlmApi
) : LlmRepository {
    override suspend fun sendRequest(request: ModelRequest): ModelResult {
        return if (request.isLocal) {
            localLlmApi.sendRequest(request, localServerUrl)
        } else {
            openRouterApi.sendRequest(request, apiKey)
        }
    }
}
```

### 4. Separate Mappers

| Mapper | Format | Usage |
|--------|--------|-------|
| `ModelRequestMapperImpl` | OpenRouter | Existing |
| `ModelResponseMapperImpl` | OpenRouter | Existing |
| `OpenAiModelRequestMapperImpl` | OpenAI | Local LLM |
| `OpenAiModelResponseMapperImpl` | OpenAI | Local LLM |

**Почему отдельные:**
- OpenRouter и OpenAI API немного отличаются
- Изоляция изменений
- Легче тестировать

---

## 📝 Поэтапный план реализации

### Этап 1: shared/simple-chat-api Module

1. Создать `shared/simple-chat-api/build.gradle.kts`
2. Создать DTO модели (ChatCompletionRequest, ChatCompletionResponse, Message, Choice, Usage)
3. Обновить `settings.gradle.kts`

### Этап 2: ai-gateway Server

1. Создать `ai-gateway/build.gradle.kts`
2. Создать `AiGatewayConfig.kt`
3. Создать `LlmProvider.kt` interface
4. Создать `OllamaProvider.kt`
5. Создать `OpenRouterProvider.kt`
6. Создать `LlmRouter.kt`
7. Создать `ChatRoutes.kt`
8. Создать `AiGatewayServer.kt`
9. Создать Dockerfile и SETUP.md

### Этап 3: Android App - ModelSettings

1. Обновить `ModelSettings.kt` domain model
2. Обновить `ModelSettingsEntity.kt`
3. Обновить `ModelSettingsMapper.kt`
4. Обновить UI настроек чата

### Этап 4: Android App - LLM Integration

1. Создать `LocalLlmApiImpl.kt`
2. Создать `OpenAiModelRequestMapperImpl.kt`
3. Создать `OpenAiModelResponseMapperImpl.kt`
4. Обновить `LlmCoreFeatureModule.kt`
5. Обновить `LlmRepositoryImpl.kt`

### Этап 5: Docker Compose

1. Добавить ai-gateway в docker-compose.yml

---

## 🔜 Следующие шаги (после MVP)

### Future: SSE Streaming
1. Добавить `ktor-server-sse` dependency
2. В `LlmProvider` добавить: `fun chatStreaming(request): Flow<ChatCompletionChunk>`
3. В `OllamaProvider`: использовать `stream: true` и парсить NDJSON
4. В `ChatRoutes`: использовать SSE response

### Future: Koog Integration
1. Добавить Koog dependency в `ai-gateway`
2. Создать `AgentService` который использует `LlmRouter`
3. Endpoint `/v1/chat/completions` будет вызывать `AgentService` вместо прямого `LlmRouter`

---

## ✅ Критерии приёмки

1. ✅ Android app может отправлять запросы к локальной Ollama
2. ✅ Android app получает и отображает ответы от Ollama
3. ✅ Работает без облачных моделей (isLocal = true)
4. ✅ OpenRouter по-прежнему работает (isLocal = false)
5. ✅ UI позволяет включить "Локальная LLM" и указать URL
6. ✅ ai-gateway собирается и запускается в Docker
7. ✅ Архитектура готова к SSE и Koog без переписывания
