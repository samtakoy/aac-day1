# 📋 День 27: Детальный план реализации — Интеграция локальной LLM

**Статус:** План  
**Дата:** 24 марта 2026  
**Автор:** AI Assistant

---

## 🎯 Продуктовая цель

Создать серверное приложение на Kotlin, которое:

1. ✅ Предоставляет **OpenAI-compatible API** для чата
2. ✅ Работает с **локальной Ollama** (без облачных моделей)
3. ✅ Поддерживает **переключение** между Ollama и OpenRouter
4. ✅ Реализует **стриминг ответов** (SSE)
5. ✅ Сохраняет **историю чатов** в SQLite
6. ✅ Интегрируется с **существующим Android-клиентом**

---

## 📊 Анализ текущего состояния

### Что уже есть в проекте

| Компонент | Статус | Расположение |
|-----------|--------|--------------|
| **Android-клиент** | ✅ Готов | `app/src/main/java/...` |
| **RAG MCP Server** | ✅ Готов | `rag-server/` |
| **Ktor HttpClient** | ✅ Настроен | `core/di/NetworkModule.kt` |
| **OpenRouter DTO** | ✅ Готовы | `app/.../llm/data/remote/model/` |
| **Ollama Provider** | ⚠️ Частично | `rag-server/.../OllamaLlmProvider.kt` |
| **БД (Exposed)** | ⚠️ Паттерны есть | `rag-server/.../db/` |

### Что нужно создать

| Компонент | Приоритет | Сложность |
|-----------|-----------|-----------|
| **ai-gateway** (новый модуль) | 🔴 Высокий | Средняя |
| **shared-chat-api** (KMP модуль) | 🔴 Высокий | Низкая |
| **LLM Provider слой** | 🔴 Высокий | Средняя |
| **OpenAI-compatible API** | 🔴 Высокий | Средняя |
| **БД для чатов** | 🟡 Средний | Низкая |
| **Agent Service** | 🟡 Средний | Низкая |

---

## 🏗 Архитектурное решение

### Выбранная архитектура: Монолит с правильными границами

```
┌─────────────────────────────────────────────────────────┐
│                  Android Client                         │
│  (существующее приложение Day)                          │
└────────────────────┬────────────────────────────────────┘
                     │ HTTP (OpenAI-compatible API)
                     ▼
┌─────────────────────────────────────────────────────────┐
│              ai-gateway (Ktor Server)                  │
│  ┌─────────────────────────────────────────────────┐   │
│  │ API Layer (routes)                              │   │
│  │ - POST /v1/chat/completions                     │   │
│  │ - GET  /v1/models                               │   │
│  └───────────────────┬─────────────────────────────┘   │
│                      │                                  │
│  ┌───────────────────▼─────────────────────────────┐   │
│  │ Agent Service                                   │   │
│  │ - загрузка контекста из БД                     │   │
│  │ - обработка запросов                           │   │
│  │ - сохранение истории                           │   │
│  └───────────────────┬─────────────────────────────┘   │
│                      │                                  │
│  ┌───────────────────▼─────────────────────────────┐   │
│  │ LLM Provider (абстракция)                       │   │
│  │ ┌──────────────┐  ┌──────────────────────────┐ │   │
│  │ │ Ollama       │  │ OpenRouter               │ │   │
│  │ │ Provider     │  │ Provider                 │ │   │
│  │ └──────────────┘  └──────────────────────────┘ │   │
│  └─────────────────────────────────────────────────┘   │
│                      │                                  │
│  ┌───────────────────▼─────────────────────────────┐   │
│  │ Database (SQLite + Exposed)                     │   │
│  │ - ChatSessions                                  │   │
│  │ - ChatMessages                                  │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                     │ HTTP
                     ▼
            ┌────────────────┐
            │   Ollama       │
            │ (локально)     │
            └────────────────┘
```

### Почему именно такая архитектура?

#### ❌ Почему НЕ микросервисы сразу?

1. **Сложность разработки ×3** — нужно настраивать gRPC, service discovery, orchestration
2. **Over-engineering для MVP** — один сервер справится с нагрузкой
3. **Быстрее итерации** — легче тестировать и деплоить
4. **Меньше кода** — нет дублирования конфигов, DTO, утилит

#### ✅ Почему монолит с границами?

1. **Четкое разделение ответственности** — API ≠ Business Logic ≠ Data Access
2. **Легкая миграция** — можно вынести любой слой в отдельный сервис
3. **Тестируемость** — каждый слой изолирован интерфейсами
4. **Готовность к росту** — архитектура масштабируется без переписывания

---

## 📁 Структура проекта (Monorepo)

### Решение: Monorepo с shared-модулем

```
aac-day1-other/
├── app/                           # Android-клиент (существующий)
│   └── src/main/java/...
│
├── rag-server/                    # RAG MCP сервер (существующий)
│   └── src/main/kotlin/...
│
├── ai-gateway/                   # ← НОВЫЙ модуль
│   ├── src/main/kotlin/
│   │   ├── ChatServer.kt          # Точка входа (main)
│   │   ├── config/
│   │   │   └── ServerConfig.kt    # Конфигурация из env
│   │   ├── api/
│   │   │   ├── routes/
│   │   │   │   ├── ChatRoutes.kt  # /v1/chat/completions
│   │   │   │   └── ModelsRoutes.kt # /v1/models
│   │   │   └── dto/               # Локальные DTO (если нужны)
│   │   ├── llm/
│   │   │   ├── LlmProvider.kt     # Интерфейс
│   │   │   ├── OllamaProvider.kt  # Ollama реализация
│   │   │   ├── OpenRouterProvider.kt # OpenRouter реализация
│   │   │   └── LlmRouter.kt       # Роутинг по модели
│   │   ├── db/
│   │   │   ├── ChatDatabase.kt    # Подключение БД
│   │   │   ├── tables/
│   │   │   │   ├── ChatSessions.kt
│   │   │   │   └── ChatMessages.kt
│   │   │   └── repository/
│   │   │       ├── ChatRepository.kt  # Интерфейс
│   │   │       └── ExposedChatRepositoryImpl.kt
│   │   └── agent/
│   │       └── AgentService.kt    # Бизнес-логика
│   └── build.gradle.kts
│
└── shared-chat-api/               # ← НОВЫЙ KMP модуль
    └── src/commonMain/
        └── kotlin/
            └── dto/
                ├── request/
                │   └── ChatCompletionRequest.kt
                └── response/
                    ├── ChatCompletionResponse.kt
                    └── ChatCompletionChunk.kt
```

### Почему Monorepo?

| Критерий | Monorepo | Separate Repos |
|----------|----------|----------------|
| **Shared код** | ✅ Легко переиспользовать | ❌ Нужно публиковать артефакты |
| **Версионирование** | ✅ Всегда актуально | ❌ Нужно следить за версиями |
| **CI/CD** | ✅ Один пайплайн | ❌ Несколько пайплайнов |
| **Навигация** | ✅ Всё в одном месте | ❌ Переключение между репо |
| **Независимый деплой** | ⚠️ Требует настройки | ✅ Независимые релизы |

**Вывод:** Для MVP и внутренней разработки Monorepo эффективнее.

### Почему shared-chat-api модуль?

1. **Единый источник истины** — DTO определены один раз
2. **Без дублирования** — не нужно копировать DTO в client и server
3. **Type-safe интеграция** — если изменить DTO, клиент не скомпилируется
4. **KMP (Kotlin Multiplatform)** — общие DTO для JVM (сервер) и Android (клиент)

---

## 📝 Поэтапный план реализации

---

### Этап 1: Настройка инфраструктуры (Gradle)

**Цель:** Добавить новые модули в проект

#### 1.1 Обновить `settings.gradle.kts`

```kotlin
include(":app")
include(":mcp-server")
include(":rag-server")
include(":ai-gateway")           // ← Добавить
include(":shared-chat-api")       // ← Добавить
```

**Почему:** Gradle должен знать о новых модулях для компиляции.

---

#### 1.2 Создать `shared-chat-api/build.gradle.kts`

```kotlin
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm()
    androidTarget()
    
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
}
```

**Почему KMP:**
- `commonMain` — общие DTO для всех платформ
- `jvm()` — для сервера (JVM)
- `androidTarget()` — для Android-клиента

---

#### 1.3 Создать `ai-gateway/build.gradle.kts`

```kotlin
plugins {
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.kotlin.serialization)
    application
}

dependencies {
    implementation(project(":shared-chat-api"))
    
    // Ktor Server (версия как в rag-server для совместимости)
    implementation("io.ktor:ktor-server-core:3.2.3")
    implementation("io.ktor:ktor-server-netty:3.2.3")
    implementation("io.ktor:ktor-server-content-negotiation:3.2.3")
    implementation("io.ktor:ktor-server-sse:3.2.3")
    
    // Ktor Client (для вызовов к Ollama/OpenRouter)
    implementation("io.ktor:ktor-client-core:3.2.3")
    implementation("io.ktor:ktor-client-okhttp:3.2.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.2.3")
    implementation("io.ktor:ktor-client-logging:3.2.3")
    
    // Сериализация
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.2.3")
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    
    // Exposed + SQLite (как в rag-server)
    implementation("org.jetbrains.exposed:exposed-core:0.61.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.61.0")
    implementation("org.xerial:sqlite-jdbc:3.49.1.0")
    
    // Логирование
    implementation("org.slf4j:slf4j-simple:2.0.17")
}

application {
    mainClass.set("com.example.day.chatserver.ChatServerKt")
}
```

**Почему такие зависимости:**

| Зависимость | Причина выбора |
|-------------|----------------|
| `ktor-server-netty` | Легковесный, асинхронный, корутины |
| `ktor-server-sse` | Стриминг ответов (Server-Sent Events) |
| `ktor-client-okhttp` | HTTP клиент для вызовов к Ollama |
| `exposed-jdbc` | ORM для работы с SQLite |
| `sqlite-jdbc` | Встраиваемая БД для MVP |

**Почему Ktor 3.2.3 (а не последняя 3.4+):**
- Совместимость с `rag-server`
- Стабильная версия (без breaking changes)
- MCP SDK 0.8.4 требует 3.2.x

---

#### 1.4 Обновить `libs.versions.toml` (опционально)

Если используется каталог версий, добавить:

```toml
[versions]
ktor = "3.2.3"
exposed = "0.61.0"

[libraries]
ktor-server-core = { group = "io.ktor", name = "ktor-server-core", version.ref = "ktor" }
ktor-server-netty = { group = "io.ktor", name = "ktor-server-netty", version.ref = "ktor" }
# ... и т.д.
```

**Почему:** Централизованное управление версиями зависимостей.

---

### Этап 2: Shared DTO модуль

**Цель:** Создать общие DTO для OpenAI-compatible API

---

#### 2.1 Создать `ChatCompletionRequest.kt`

**Путь:** `shared-chat-api/src/commonMain/kotlin/dto/request/ChatCompletionRequest.kt`

```kotlin
@Serializable
data class ChatCompletionRequest(
    @SerialName("model")
    val model: String,
    
    @SerialName("messages")
    val messages: List<Message>,
    
    @SerialName("tools")
    val tools: List<ToolDefinition>? = null,
    
    @SerialName("stream")
    val stream: Boolean = false,
    
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
    
    @SerialName("temperature")
    val temperature: Double? = null,
    
    // Metadata для внутреннего использования
    @SerialName("metadata")
    val metadata: Map<String, String>? = null
)
```

**Почему такие поля:**

| Поле | Обязательное | Причина |
|------|--------------|---------|
| `model` | ✅ | Выбор модели (local/llama3, openrouter/gpt-4) |
| `messages` | ✅ | История чата + текущий запрос |
| `tools` | ❌ | Для будущего tool calling |
| `stream` | ❌ | Включение SSE стриминга |
| `max_tokens` | ❌ | Ограничение длины ответа |
| `temperature` | ❌ | Креативность генерации |
| `metadata` | ❌ | Внутренние данные (session_id, user_id) |

**Почему `@SerialName`:**
- Явное указание имен полей для JSON
- Совместимость с OpenAI API (snake_case)
- Защита от обфускации

---

#### 2.2 Создать `Message.kt`

**Путь:** `shared-chat-api/src/commonMain/kotlin/dto/Message.kt`

```kotlin
@Serializable
data class Message(
    @SerialName("role")
    val role: String,  // "system", "user", "assistant", "tool"
    
    @SerialName("content")
    val content: String? = null,
    
    @SerialName("tool_calls")
    val toolCalls: List<ToolCall>? = null,
    
    @SerialName("tool_call_id")
    val toolCallId: String? = null
)
```

**Почему 4 поля:**

1. **`role`** — определяет тип сообщения:
   - `system` — инструкция для модели
   - `user` — сообщение от пользователя
   - `assistant` — ответ от модели
   - `tool` — результат выполнения инструмента

2. **`content`** — текст сообщения (nullable для tool_calls)

3. **`toolCalls`** — для вызова функций (будущее расширение)

4. **`toolCallId`** — связь с tool call (для role=tool)

---

#### 2.3 Создать `ChatCompletionResponse.kt` (non-streaming)

**Путь:** `shared-chat-api/src/commonMain/kotlin/dto/response/ChatCompletionResponse.kt`

```kotlin
@Serializable
data class ChatCompletionResponse(
    @SerialName("id")
    val id: String,
    
    @SerialName("choices")
    val choices: List<Choice>,
    
    @SerialName("usage")
    val usage: Usage? = null,
    
    @SerialName("created")
    val created: Long,
    
    @SerialName("model")
    val model: String,
    
    @SerialName("object")
    val objectType: String = "chat.completion"
)

@Serializable
data class Choice(
    @SerialName("index")
    val index: Int = 0,
    
    @SerialName("message")
    val message: Message,
    
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class Usage(
    @SerialName("prompt_tokens")
    val promptTokens: Int,
    
    @SerialName("completion_tokens")
    val completionTokens: Int,
    
    @SerialName("total_tokens")
    val totalTokens: Int
)
```

**Почему такая структура:**

- **`id`** — уникальный идентификатор запроса (для отладки)
- **`choices`** — список вариантов (OpenAI поддерживает multiple choices)
- **`usage`** — статистика токенов (важно для контроля расходов)
- **`created`** — timestamp (Unix time)
- **`object`** — тип объекта (для парсинга клиентами)

---

#### 2.4 Создать `ChatCompletionChunk.kt` (streaming)

**Путь:** `shared-chat-api/src/commonMain/kotlin/dto/response/ChatCompletionChunk.kt`

```kotlin
@Serializable
data class ChatCompletionChunk(
    @SerialName("id")
    val id: String,
    
    @SerialName("choices")
    val choices: List<ChunkChoice>,
    
    @SerialName("created")
    val created: Long = System.currentTimeMillis() / 1000,
    
    @SerialName("model")
    val model: String,
    
    @SerialName("object")
    val objectType: String = "chat.completion.chunk"
)

@Serializable
data class ChunkChoice(
    @SerialName("index")
    val index: Int = 0,
    
    @SerialName("delta")
    val delta: Delta,
    
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class Delta(
    @SerialName("role")
    val role: String? = null,
    
    @SerialName("content")
    val content: String? = null
)
```

**Почему отдельный класс для стриминга:**

1. **`delta` вместо `message`** — содержит только изменения
2. **`role` в delta** — передается только в первом чанке
3. **`content` в delta** — часть текста (не всё сообщение)
4. **`finish_reason`** — сигнализирует о завершении

**Формат SSE ответа:**

```
data: {"id":"1","choices":[{"delta":{"role":"assistant"}}]}
data: {"id":"1","choices":[{"delta":{"content":"Hello"}}]}
data: {"id":"1","choices":[{"delta":{"content":"!"}}]}
data: {"id":"1","choices":[],"finish_reason":"stop"}
data: [DONE]
```

---

#### 2.5 Создать `ToolDefinition.kt` (для будущего расширения)

**Путь:** `shared-chat-api/src/commonMain/kotlin/dto/ToolDefinition.kt`

```kotlin
@Serializable
data class ToolDefinition(
    @SerialName("type")
    val type: String = "function",
    
    @SerialName("function")
    val function: FunctionDefinition
)

@Serializable
data class FunctionDefinition(
    @SerialName("name")
    val name: String,
    
    @SerialName("description")
    val description: String? = null,
    
    @SerialName("parameters")
    val parameters: JsonObject
)

@Serializable
data class ToolCall(
    @SerialName("id")
    val id: String,
    
    @SerialName("type")
    val type: String = "function",
    
    @SerialName("function")
    val function: FunctionCall
)

@Serializable
data class FunctionCall(
    @SerialName("name")
    val name: String,
    
    @SerialName("arguments")
    val arguments: String  // JSON STRING!
)
```

**Почему `arguments` как String:**

- OpenAI API возвращает аргументы как JSON-строку
- Нужно парсить отдельно: `Json.parseToJsonElement(arguments)`
- Это не ошибка, а спецификация API

---

### Этап 3: LLM Provider слой

**Цель:** Абстракция для переключения между провайдерами

---

#### 3.1 Создать `LlmProvider.kt` (интерфейс)

**Путь:** `ai-gateway/src/main/kotlin/llm/LlmProvider.kt`

```kotlin
interface LlmProvider {
    /**
     * Отправляет запрос к LLM и возвращает поток чанков
     */
    fun chat(request: ChatCompletionRequest): Flow<ChatCompletionChunk>
}
```

**Почему `Flow<ChatCompletionChunk>`:**

1. **Реактивный стриминг** — клиент получает токены по мере генерации
2. **Backpressure** — клиент контролирует скорость потребления
3. **Отмена** — можно отменить генерацию (coroutine cancellation)
4. **Единый контракт** — одинаково для SSE и WebSocket

**Почему интерфейс:**

- **Dependency Inversion** — бизнес-логика зависит от абстракции
- **Легкое тестирование** — можно замокать провайдер
- **Переключение** — смена реализации без изменения кода

---

#### 3.2 Создать `OllamaProvider.kt`

**Путь:** `ai-gateway/src/main/kotlin/llm/OllamaProvider.kt`

```kotlin
class OllamaProvider(
    private val baseUrl: String,
    private val httpClient: HttpClient
) : LlmProvider {
    
    override fun chat(request: ChatCompletionRequest): Flow<ChatCompletionChunk> = flow {
        // 1. Маппинг OpenAI request → Ollama request
        val ollamaRequest = request.toOllamaRequest()
        
        // 2. POST к Ollama /api/chat
        val response = httpClient.post("$baseUrl/api/chat") {
            contentType(ContentType.Application.Json)
            setBody(ollamaRequest)
        }
        
        // 3. Парсинг NDJSON потока
        response.bodyAsChannel().toFlow().collect { rawLine ->
            val ollamaChunk = parseOllamaChunk(rawLine)
            // 4. Маппинг Ollama chunk → OpenAI chunk
            emit(ollamaChunk.toOpenAiChunk(request.model))
        }
    }
}
```

**Почему маппинг:**

| OpenAI | Ollama |
|--------|--------|
| `/v1/chat/completions` | `/api/chat` |
| `messages: [{role, content}]` | `messages: [{role, content}]` |
| `stream: true` | `stream: true` |
| Ответ: SSE `data: {...}` | Ответ: NDJSON `{...}\n{...}` |

**Почему NDJSON парсинг:**

Ollama возвращает:
```json
{"message":{"role":"assistant","content":"Hello"},"done":false}
{"message":{"role":"assistant","content":"!"},"done":true}
```

Нужно:
1. Разделить по строкам
2. Распарсить каждый JSON
3. Собрать в Flow

---

#### 3.3 Создать `OpenRouterProvider.kt`

**Путь:** `ai-gateway/src/main/kotlin/llm/OpenRouterProvider.kt`

```kotlin
class OpenRouterProvider(
    private val apiKey: String,
    private val httpClient: HttpClient
) : LlmProvider {
    
    override fun chat(request: ChatCompletionRequest): Flow<ChatCompletionChunk> = flow {
        val response = httpClient.post("https://openrouter.ai/api/v1/chat/completions") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $apiKey")
                append("HTTP-Referer", "https://github.com")
                append("X-Title", "Day Chat Server")
            }
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        
        // OpenRouter возвращает SSE
        response.bodyAsChannel().toFlow().collect { line ->
            if (line.startsWith("data: ")) {
                val json = line.removePrefix("data: ")
                if (json != "[DONE]") {
                    emit(parseChunk(json))
                }
            }
        }
    }
}
```

**Почему заголовки:**

- **`Authorization`** — API ключ (обязательно)
- **`HTTP-Referer`** — требуется OpenRouter
- **`X-Title`** — название приложения (для статистики OpenRouter)

---

#### 3.4 Создать `LlmRouter.kt` (роутинг по модели)

**Путь:** `ai-gateway/src/main/kotlin/llm/LlmRouter.kt`

```kotlin
class LlmRouter(
    private val ollamaProvider: OllamaProvider,
    private val openRouterProvider: OpenRouterProvider
) : LlmProvider {
    
    override fun chat(request: ChatCompletionRequest): Flow<ChatCompletionChunk> {
        return when {
            // Локальные модели (префикс "local/")
            request.model.startsWith("local/") -> {
                val ollamaModel = request.model.removePrefix("local/")
                ollamaProvider.chat(request.copy(model = ollamaModel))
            }
            
            // OpenRouter модели (префикс "openrouter/")
            request.model.startsWith("openrouter/") -> {
                val orModel = request.model.removePrefix("openrouter/")
                openRouterProvider.chat(request.copy(model = orModel))
            }
            
            // По умолчанию → Ollama
            else -> ollamaProvider.chat(request)
        }
    }
}
```

**Почему префиксы:**

| Префикс | Пример | Куда идет |
|---------|--------|-----------|
| `local/` | `local/llama3` | Ollama (localhost:11434) |
| `openrouter/` | `openrouter/gpt-4` | OpenRouter API |
| (нет) | `llama3` | Ollama (по умолчанию) |

**Преимущества:**

1. **Явный выбор** — клиент контролирует, куда идет запрос
2. **Гибкость** — можно тестировать разные модели
3. **Fallback** — если Ollama недоступна, можно переключиться

---

### Этап 4: Ktor API (OpenAI-compatible endpoints)

**Цель:** Реализовать HTTP endpoints для чата

---

#### 4.1 Создать `ChatRoutes.kt`

**Путь:** `ai-gateway/src/main/kotlin/api/routes/ChatRoutes.kt`

```kotlin
fun Route.chatRoutes(agentService: AgentService) {
    
    /**
     * POST /v1/chat/completions
     * OpenAI-compatible Chat API
     */
    post("/v1/chat/completions") {
        val request = call.receive<ChatCompletionRequest>()
        
        if (request.stream) {
            // SSE стриминг
            call.respondSseChat(agentService.process(request))
        } else {
            // Обычный JSON ответ
            call.respondChatCompletion(agentService.processBlocking(request))
        }
    }
}

/**
 * Extension для SSE ответа
 */
private suspend fun PipelineContext.respondSseChat(flow: Flow<ChatCompletionChunk>) {
    call.respondTextWriter(ContentType.Text.EventStream) {
        flow.collect { chunk ->
            val json = Json.encodeToString(chunk)
            write("data: $json\n\n")
            flush()  // Важно! Отправляем сразу
        }
        write("data: [DONE]\n\n")
    }
}

/**
 * Extension для non-streaming ответа
 */
private suspend fun PipelineContext.respondChatCompletion(chunks: List<ChatCompletionChunk>) {
    // Агрегация чанков в полный ответ
    val fullResponse = aggregateChunks(chunks)
    call.respond(fullResponse)
}
```

**Почему `respondTextWriter`:**

- Прямая запись в HTTP поток
- Контроль над flushing (отправка чанков сразу)
- Минимальные накладные расходы

**Почему `flush()` после каждого чанка:**

- Без flush данные буферизуются
- Клиент не получит токены в реальном времени
- SSE требует немедленной отправки

---

#### 4.2 Создать `ModelsRoutes.kt`

**Путь:** `ai-gateway/src/main/kotlin/api/routes/ModelsRoutes.kt`

```kotlin
fun Route.modelsRoutes(availableModels: List<String>) {
    
    /**
     * GET /v1/models
     * Список доступных моделей
     */
    get("/v1/models") {
        call.respond(
            mapOf(
                "object" to "list",
                "data" to availableModels.map { model ->
                    mapOf(
                        "id" to model,
                        "object" to "model",
                        "created" to System.currentTimeMillis() / 1000,
                        "owned_by" to (if (model.startsWith("local")) "ollama" else "openrouter")
                    )
                }
            )
        )
    }
}
```

**Почему endpoint `/v1/models`:**

- Требуется OpenAI spec
- Клиент может запросить список доступных моделей
- Полезно для UI (выбор модели в настройках)

---

### Этап 5: База данных (Exposed + SQLite)

**Цель:** Сохранение истории чатов

---

#### 5.1 Создать `ChatDatabase.kt`

**Путь:** `ai-gateway/src/main/kotlin/db/ChatDatabase.kt`

```kotlin
object ChatDatabase {
    
    private var database: Database? = null
    
    fun connect(dbPath: String = "chat.db") {
        database = Database.connect(
            url = "jdbc:sqlite:$dbPath",
            driver = "org.sqlite.JDBC"
        )
        
        // Создание таблиц
        transaction {
            SchemaUtils.create(ChatSessions, ChatMessages)
        }
    }
    
    fun close() {
        database?.disconnect()
    }
}
```

**Почему SQLite:**

| Критерий | SQLite | PostgreSQL |
|----------|--------|------------|
| **Настройка** | ✅ Файл, без сервера | ❌ Нужен отдельный сервер |
| **MVP** | ✅ Идеально | ❌ Overkill |
| **Конкурентность** | ⚠️ Ограничена | ✅ Высокая |
| **Миграция** | ✅ Exposed абстрагирует | ✅ Exposed абстрагирует |

**Почему Exposed:**

- **ORM** — работа с таблицами как с Kotlin-объектами
- **Type-safe** — компилятор проверяет запросы
- **Миграция** — смена БД изменением конфига (не кода)

---

#### 5.2 Создать `ChatSessions.kt` (таблица)

**Путь:** `ai-gateway/src/main/kotlin/db/tables/ChatSessions.kt`

```kotlin
object ChatSessions : Table() {
    val id = varchar("id", 64)          // UUID
    val userId = varchar("user_id", 64) // ID пользователя
    val title = varchar("title", 256)   // Заголовок чата
    val createdAt = long("created_at")  // Timestamp
    val updatedAt = long("updated_at")  // Timestamp
    
    override val primaryKey = PrimaryKey(id)
}
```

**Почему такие поля:**

| Поле | Тип | Причина |
|------|-----|---------|
| `id` | UUID | Уникальный идентификатор |
| `userId` | String | Привязка к пользователю |
| `title` | String | Заголовок (для UI списка чатов) |
| `createdAt` | Long | Время создания |
| `updatedAt` | Long | Время последнего сообщения (для сортировки) |

---

#### 5.3 Создать `ChatMessages.kt` (таблица)

**Путь:** `ai-gateway/src/main/kotlin/db/tables/ChatMessages.kt`

```kotlin
object ChatMessages : Table() {
    val id = varchar("id", 64)
    val sessionId = varchar("session_id", 64)  // FK к ChatSessions
    val role = varchar("role", 16)             // "user", "assistant", "system"
    val content = text("content")              // Текст сообщения
    val createdAt = long("created_at")         // Timestamp
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        foreignKey(sessionId to ChatSessions.id, onDelete = ReferenceOption.CASCADE)
        index("session_id_index", false, sessionId)
    }
}
```

**Почему foreign key с CASCADE:**

- При удалении чата удаляются все сообщения
- Автоматическая очистка (без ручных запросов)

**Почему индекс на `session_id`:**

- Частый запрос: "все сообщения для сессии"
- Без индекса — полный скан таблицы
- С индексом — быстрый lookup

---

#### 5.4 Создать `ChatRepository.kt` (интерфейс)

**Путь:** `ai-gateway/src/main/kotlin/db/repository/ChatRepository.kt`

```kotlin
interface ChatRepository {
    
    /**
     * Получить последние N сообщений для сессии
     */
    suspend fun getMessages(sessionId: String, limit: Int = 20): List<Message>
    
    /**
     * Сохранить сообщение
     */
    suspend fun saveMessage(sessionId: String, message: Message)
    
    /**
     * Создать новую сессию
     */
    suspend fun createSession(userId: String, title: String): String
    
    /**
     * Получить сессию по ID
     */
    suspend fun getSession(sessionId: String): ChatSession?
}
```

**Почему интерфейс:**

- **Абстракция** — бизнес-логика не знает про Exposed
- **Тестирование** — можно использовать fake-реализацию
- **Миграция** — смена ORM без изменения бизнес-логики

---

#### 5.5 Создать `ExposedChatRepositoryImpl.kt`

**Путь:** `ai-gateway/src/main/kotlin/db/repository/ExposedChatRepositoryImpl.kt`

```kotlin
class ExposedChatRepositoryImpl : ChatRepository {
    
    override suspend fun getMessages(sessionId: String, limit: Int): List<Message> =
        newSuspendedTransaction {
            ChatMessages
                .select { ChatMessages.sessionId eq sessionId }
                .orderBy(ChatMessages.createdAt to SortOrder.ASC)
                .limit(limit)
                .map { row ->
                    Message(
                        role = row[ChatMessages.role],
                        content = row[ChatMessages.content]
                    )
                }
        }
    
    override suspend fun saveMessage(sessionId: String, message: Message) {
        newSuspendedTransaction {
            ChatMessages.insert {
                it[id] = generateUuid()
                it[sessionId] = sessionId
                it[role] = message.role
                it[content] = message.content ?: ""
                it[createdAt] = System.currentTimeMillis()
            }
        }
    }
    
    // ... остальные методы
}
```

**Почему `newSuspendedTransaction`:**

- Exposed требует транзакции для любых операций с БД
- `newSuspendedTransaction` — обертка для корутин
- Гарантирует закрытие соединения

**Почему `limit = 20`:**

- Не загружать всю историю (дорого для LLM)
- Достаточно контекста для осмысленного ответа
- Контроль размера prompt (токены = деньги)

---

### Этап 6: Agent Service

**Цель:** Бизнес-логика обработки запросов

---

#### 6.1 Создать `AgentService.kt`

**Путь:** `ai-gateway/src/main/kotlin/agent/AgentService.kt`

```kotlin
class AgentService(
    private val llmProvider: LlmProvider,
    private val chatRepository: ChatRepository
) {
    
    /**
     * Обработка запроса со стримингом
     */
    fun process(request: ChatCompletionRequest): Flow<ChatCompletionChunk> = flow {
        // 1. Получаем session_id из metadata
        val sessionId = request.metadata?.get("session_id")
            ?: run {
                // Создаем новую сессию
                val userId = request.metadata?.get("user_id") ?: "anonymous"
                chatRepository.createSession(userId, "New Chat")
            }
        
        // 2. Загружаем контекст (историю чата)
        val history = chatRepository.getMessages(sessionId, limit = 20)
        
        // 3. Объединяем историю с текущим запросом
        val fullRequest = request.copy(
            messages = history + request.messages
        )
        
        // 4. Буфер для агрегации ответа
        val responseBuffer = StringBuilder()
        
        // 5. Стримим ответ от LLM
        llmProvider.chat(fullRequest).collect { chunk ->
            emit(chunk)
            
            // Собираем текст для сохранения
            chunk.choices.firstOrNull()?.delta?.content?.let { content ->
                responseBuffer.append(content)
            }
        }
        
        // 6. Сохраняем сообщения в БД
        chatRepository.saveMessage(sessionId, request.messages.last())
        chatRepository.saveMessage(sessionId, Message(
            role = "assistant",
            content = responseBuffer.toString()
        ))
    }
    
    /**
     * Обработка без стриминга (для совместимости)
     */
    suspend fun processBlocking(request: ChatCompletionRequest): List<ChatCompletionChunk> =
        process(request).toList()
}
```

**Почему такая последовательность:**

1. **Session management** — каждый чат имеет свою сессию
2. **Context loading** — LLM нужна история для контекста
3. **Limit 20** — защита от огромных prompt
4. **Buffer** — агрегация токенов для сохранения
5. **Save после стрима** — не сохраняем каждый токен (слишком часто)

---

### Этап 7: Конфигурация и точка входа

**Цель:** Настройка и запуск сервера

---

#### 7.1 Создать `ServerConfig.kt`

**Путь:** `ai-gateway/src/main/kotlin/config/ServerConfig.kt`

```kotlin
data class ServerConfig(
    val host: String,
    val port: Int,
    val dbPath: String,
    val ollamaBaseUrl: String,
    val openRouterApiKey: String,
    val availableModels: List<String>
) {
    companion object {
        fun fromEnv(): ServerConfig {
            return ServerConfig(
                host = System.getenv("CHAT_SERVER_HOST") ?: "0.0.0.0",
                port = System.getenv("CHAT_SERVER_PORT")?.toIntOrNull() ?: 8080,
                dbPath = System.getenv("CHAT_DB_PATH") ?: "chat.db",
                ollamaBaseUrl = System.getenv("OLLAMA_BASE_URL") ?: "http://localhost:11434",
                openRouterApiKey = System.getenv("OPENROUTER_API_KEY") ?: "",
                availableModels = System.getenv("AVAILABLE_MODELS")
                    ?.split(",")
                    ?.map { it.trim() }
                    ?: listOf("local/llama3", "local/qwen2.5")
            )
        }
    }
}
```

**Почему env-переменные:**

- **Безопасность** — API ключи не в коде
- **Гибкость** — разные конфиги для dev/prod
- **Docker** — легко передавать в контейнер

---

#### 7.2 Создать `ChatServer.kt` (точка входа)

**Путь:** `ai-gateway/src/main/kotlin/ChatServer.kt`

```kotlin
fun main() {
    // 1. Загрузка конфигурации
    val config = ServerConfig.fromEnv()
    
    println("=== Day Chat Server ===")
    println("Port: ${config.port}")
    println("Ollama: ${config.ollamaBaseUrl}")
    println("Models: ${config.availableModels.joinToString()}")
    
    // 2. Инициализация БД
    ChatDatabase.connect(config.dbPath)
    
    // 3. Создание HTTP клиента
    val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) { json() }
        install(Logging) { level = LogLevel.INFO }
        install(HttpTimeout) { requestTimeoutMillis = 120_000 }
    }
    
    // 4. Создание провайдеров
    val ollamaProvider = OllamaProvider(config.ollamaBaseUrl, httpClient)
    val openRouterProvider = OpenRouterProvider(config.openRouterApiKey, httpClient)
    val llmRouter = LlmRouter(ollamaProvider, openRouterProvider)
    
    // 5. Создание репозитория и сервиса
    val chatRepository = ExposedChatRepositoryImpl()
    val agentService = AgentService(llmRouter, chatRepository)
    
    // 6. Запуск Ktor сервера
    embeddedServer(Netty, port = config.port, host = config.host) {
        install(ContentNegotiation) { json() }
        install(SSE)
        
        routing {
            chatRoutes(agentService)
            modelsRoutes(config.availableModels)
            
            // Health check
            get("/health") {
                call.respondText("OK")
            }
        }
    }.start(wait = true)
}
```

**Почему такая структура:**

1. **Конфигурация** — все настройки из env
2. **DI вручную** — без Dagger/Hilt (сервер проще)
3. **Health check** — для мониторинга и Docker
4. **SSE plugin** — поддержка Server-Sent Events

---

### Этап 8: Docker-compose и запуск

**Цель:** Контейнеризация для простого запуска

---

#### 8.1 Создать `docker-compose.yml`

**Путь:** `ai-gateway/docker-compose.yml`

```yaml
version: '3.8'

services:
  ai-gateway:
    build: .
    ports:
      - "8080:8080"
    environment:
      - CHAT_SERVER_PORT=8080
      - CHAT_DB_PATH=/data/chat.db
      - OLLAMA_BASE_URL=http://ollama:11434
      - OPENROUTER_API_KEY=${OPENROUTER_API_KEY:-}
      - AVAILABLE_MODELS=local/llama3,local/qwen2.5
    volumes:
      - chat-data:/data
    depends_on:
      - ollama
  
  ollama:
    image: ollama/ollama:latest
    ports:
      - "11434:11434"
    volumes:
      - ollama-data:/root/.ollama
    # Если Ollama уже запущен локально, можно закомментировать
    # и использовать host.docker.internal:11434

volumes:
  chat-data:
  ollama-data:
```

**Почему Docker:**

- **Воспроизводимость** — одинаковая среда везде
- **Простота** — `docker-compose up` и всё работает
- **Изоляция** — не загрязняет систему

---

#### 8.2 Создать `Dockerfile`

**Путь:** `ai-gateway/Dockerfile`

```dockerfile
FROM gradle:8.5-jdk17 AS build

WORKDIR /app
COPY build.gradle.kts .
COPY src src

RUN gradle build --no-daemon

FROM openjdk:17-slim

WORKDIR /app
COPY --from=build /app/build/libs/ai-gateway.jar .

EXPOSE 8080

CMD ["java", "-jar", "ai-gateway.jar"]
```

**Почему multi-stage:**

- **Первый этап** — сборка с Gradle
- **Второй этап** — только JRE (меньше образ)
- **Итог** — ~200MB вместо ~1GB

---

#### 8.3 Создать `.env.example`

**Путь:** `ai-gateway/.env.example`

```bash
# Порт сервера
CHAT_SERVER_PORT=8080

# Путь к БД
CHAT_DB_PATH=./chat.db

# Ollama URL
OLLAMA_BASE_URL=http://localhost:11434

# OpenRouter API key (опционально)
OPENROUTER_API_KEY=

# Доступные модели
AVAILABLE_MODELS=local/llama3,local/qwen2.5,openrouter/gpt-4
```

---

## 🔧 Интеграция с Android-клиентом

### Что нужно изменить в приложении

#### 1. Добавить переключатель провайдера

**Файл:** `app/src/main/java/.../settings/`

```kotlin
enum class LlmProvider {
    OPENROUTER,
    LOCAL_SERVER
}
```

#### 2. Обновить `RemoteLlmApi`

Добавить реализацию `LocalServerLlmApi` с URL локального сервера.

#### 3. Обновить `NetworkModule`

Добавить предоставление клиента для локального сервера.

---

## ✅ Критерии завершения

| Критерий | Статус |
|----------|--------|
| Сервер запускается | ⬜ |
| `POST /v1/chat/completions` работает | ⬜ |
| SSE стриминг работает | ⬜ |
| Ollama провайдер работает | ⬜ |
| OpenRouter провайдер работает | ⬜ |
| Переключение по модели работает | ⬜ |
| История сохраняется в SQLite | ⬜ |
| Docker-compose запускает всё | ⬜ |
| Android переключается на сервер | ⬜ |

---

## 🚀 Следующие шаги (после MVP)

1. **Koog интеграция** — агенты, tools, multi-step reasoning
2. **Redis** — кэширование контекста, статусы
3. **WebSocket** — двусторонняя связь
4. **PostgreSQL** — для продакшена
5. **Микросервисы** — разделение Gateway и Agent Service

---

## 📚 Используемые ресурсы

- [Ktor Documentation](https://ktor.io/docs/welcome.html)
- [OpenAI API Reference](https://platform.openai.com/docs/api-reference/chat)
- [Ollama API Documentation](https://github.com/ollama/ollama/blob/main/docs/api.md)
- [Exposed Documentation](https://github.com/JetBrains/Exposed/wiki)
- [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization)
