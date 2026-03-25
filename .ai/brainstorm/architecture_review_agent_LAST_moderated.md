# Multi-Agent Brainstorm: Agent Architecture to ai-gateway

## Задача
Проанализировать возможность выноса AIAgent.kt и AWorker.kt с архитектурой агентов на сервер ai-gateway. Предложить чистое расширяемое понятное решение с использованием shared модулей для агентов.

---

## 🤖 AI Systems Architect (включаясь)

Принял задачу. Анализирую архитектуру агентной системы с фокусом на LLM orchestration и multi-agent patterns.

### Анализ текущего состояния

**AIAgent.kt** — центральный компонент:
- Tool loop с max 10 итераций
- HITL (Human-In-The-Loop) через `HitlSessionManager`
- Контекстные стратегии: Full, Summarization, SlidingWindow, StickyFacts, Branching

**Ключевые интерфейсы для extraction:**
```kotlin
// Оркестрация
ToolCallOrchestrator → execute(OrchestratorRequest) → OrchestratorResult
ToolExecutor → submit(runId, toolCalls, ...) → ToolExecutionResult
ToolProvider → getTools(agentId), executeToolCall(call, context)

// Контекст
ContextStrategy → process(config, repository), afterResponse(...)
AgentContextRepository → loadContext, saveContext, clearContext

// HITL
HitlSessionManager → hasActiveSession, createSession, getSession, closeSession, updateSession
```

### Предложение по Shared модулю

**Проблема:** Текущий `shared/simple-chat-api` содержит только OpenAI DTOs. Нам нужен **agent-api** модуль.

```
shared/
├── simple-chat-api/           # Существующий
│   └── dto/OpenAiDtos.kt     # ChatCompletionRequest/Response
│
└── agent-api/                # НОВЫЙ
    └── src/main/kotlin/com/example/day/shared/agent/
        ├── api/
        │   ├── AgentRequest.kt
        │   ├── AgentResponse.kt
        │   └── WorkerEventDto.kt
        ├── model/
        │   ├── AgentConfigDto.kt
        │   ├── ContextMessageDto.kt
        │   └── ToolCallDto.kt
        └── contract/
            ├── AgentRuntimeContext.kt
            ├── AgentContextRepository.kt      # Интерфейс
            ├── ContextStrategy.kt              # Интерфейс
            ├── ToolProvider.kt                 # Интерфейс
            ├── ToolExecutor.kt                 # Интерфейс
            ├── ToolCallOrchestrator.kt        # Интерфейс
            └── HitlSessionManager.kt           # Интерфейс
```

### Критически важно для AI Systems

1. **Model routing** — AIAgentServer должен поддерживать множественные LLM providers
2. **Streaming events** — WorkerEventDto должен передаваться через WebSocket
3. **Tool execution context** — серверные инструменты vs клиентские

### Риски
- HITL через WebSocket добавляет сложность
- Синхронизация состояния агента между клиентом и сервером

---

## 🏗 Senior Architect (включаясь)

Building on AI Systems Architect's module structure analysis. Оцениваю архитектурную чистоту и domain boundaries.

### Анализ domain separation

**Что должно быть в shared/agent-api (контракты):**
- DTOs для comunicación
- Интерфейсы репозиториев и сервисов
- Модели агента (config, messages, tools)

**Что должно быть platform-specific:**
- Android: Room implementation, Android-specific tools, UI layer
- Server: In-memory/Database implementation, server-side tools

### Clean Architecture Layers

```
shared/agent-api/
├── contract/           # Интерфейсы (домен)
│   ├── AgentContextRepository.kt
│   ├── ToolProvider.kt
│   └── ContextStrategy.kt
│
└── model/              # DTOs (transfer objects)
    ├── AgentConfigDto.kt
    └── WorkerEventDto.kt

ai-gateway/agent/       # Server implementation
├── domain/
│   ├── AIAgentServer.kt
│   └── workers/
├── infrastructure/
│   ├── repository/InMemoryAgentContextRepository.kt
│   └── tools/ServerToolProvider.kt
└── di/AgentServerModule.kt

app/agent/              # Client implementation  
├── domain/
│   └── workers/
├── infrastructure/
│   ├── repository/RoomAgentContextRepository.kt
│   └── tools/AndroidToolProvider.kt
└── di/AgentClientModule.kt
```

### Предлагаю 2 варианта реализации

**Вариант A: Pure Server Agents (мое предложение)**
- Агенты работают ТОЛЬКО на сервере
- Android — тонкий клиент
- Проще, меньше abstraction layers

**Вариант B: Hybrid ( Client + Server agents)**
- Простые агенты работают локально
- Сложные (с RAG, multi-agent) — на сервере
- Риск: код дублируется, синхронизация сложнее

### Trade-offs

| | Вариант A | Вариант B |
|--|-----------|-----------|
| Сложность | Низкая | Высокая |
| Offline mode | Нет | Да |
| Resource usage | Сервер | Клиент |
| Consistency | 100% | Требует sync |

---

## 💻 Kotlin Senior Developer (включаясь)

Оцениваю с точки зрения Kotlin, Ktor, Dagger, Coroutines. Проверяю feasibility предложенной архитектуры.

### Ktor WebSocket Integration

Текущий `ai-gateway` использует Ktor. Для WebSocket HITL нам нужно:

```kotlin
// AgentWebSocketServer.kt
class AgentWebSocketServer(
    private val hitlSessionManager: ServerHitlSessionManager,
    private val eventEmitter: AgentEventEmitter
) {
    fun handleSession(sessionId: String, ws: WebSocketSession) {
        // Bidirectional communication
        // Server → Client: WorkerEventDto
        // Client → Server: Approval decisions
    }
}

// AgentRoutes.kt - добавляем
routing {
    webSocket("/v1/agents/events/{sessionId}") { ... }
    post("/v1/agents/process") { ... }
}
```

### DI Architecture

**Важно:** ai-gateway использует **ручной DI** (не Hilt). Для Agent Server:

```kotlin
// AgentServerModule.kt
class AgentServerModule(
    private val llmProvider: LlmProvider,
    private val config: AiGatewayConfig
) {
    fun provideAgentContextRepository(): AgentContextRepository = 
        InMemoryAgentContextRepository()
    
    fun provideToolProvider(): ToolProvider = 
        ServerToolProvider(...)
    
    // etc.
}
```

### Проблема с AWorker

AWorker зависит от `Chat` (Android domain model). Для server-side:

```kotlin
// Server AWorker interface
interface AWorker {
    suspend fun doWork(
        userPrompt: String,
        context: ServerWorkerContext,  // Not Chat!
        onEvent: (suspend (WorkerEventDto) -> Unit)? = null
    ): WorkerResult
}

data class ServerWorkerContext(
    val sessionId: String,
    val agentConfig: AgentConfigDto,
    val history: List<ContextMessageDto>,
    val metadata: Map<String, String>
)
```

### Gradle Multi-module Setup

```kotlin
// settings.gradle.kts
include(":shared:agent-api")
include(":ai-gateway")

// shared/agent-api/build.gradle.kts
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
            }
        }
    }
}

// ai-gateway/build.gradle.kts
dependencies {
    implementation(project(":shared:agent-api"))
    implementation("io.ktor:ktor-server-websockets")
}
```

---

## 🔍 Quality Reviewer (включаясь)

Проверяю архитектуру на overengineering, SOLID violations, и实用性.

### Issues Found

#### Medium #1: Избыточная абстракция в ContextStrategy

**Проблема:** AIAgent зависит от 5 различных ContextStrategy implementations:
- ContextEmptyStrategy
- ContextFullStrategy
- ContextSlidingWindowStrategy
- ContextStickyFactsStrategy
- ContextBranchingStrategy
- ContextSummaryStrategy

**Вопрос:** Все ли они нужны для MVP server-side агента?

**Рекомендация:** Начать с 1-2 стратегий (Full + SlidingWindow), остальные добавлять по мере необходимости.

#### Medium #2: Tool system — abstraction leak

**Проблема:** ToolProvider.getTools() возвращает `ModelRequest.Tool` — это LLM-specific, не generic.

```kotlin
// Текущее (проблема)
interface ToolProvider {
    fun getTools(agentId: String): List<ModelRequest.Tool>  // Привязано к LLM
}

// Нужно
interface ToolProvider {
    fun getTools(agentId: String): List<AgentTool>  // Generic
}

data class AgentTool(
    val name: String,
    val description: String,
    val parameters: JsonObject,
    val handler: ToolHandler  // Server-side implementation
)
```

#### Low #3: HITLSessionManager — неясный lifecycle

**Проблема:** HitlSessionManager создаёт сессии, но нет TTL или cleanup policy.

**Рекомендация:** Добавить `sessionTimeoutMinutes` в конфиг.

### Veto Check (High Severity)

- **More than 3 abstraction layers?** ❌ Нет. Contract → DTO → Implementation = 3 слоя, нормально.
- **AI/LLM used where rule-based suffices?** ✅ Tool calling действительно требует LLM.
- **Circular dependencies?** ❌ Не обнаружено.
- **Cost estimate missing?** ⚠️ Нужно добавить.

### Cost Estimate

| Компонент | Ресурс | Оценка |
|-----------|--------|--------|
| LLM calls | OpenAI/Ollama | $0.01-0.10/request |
| WebSocket connections | RAM | 1KB/s per session |
| HITL sessions | Storage | 10KB per session |
| Agent context | RAM/DB | 100KB-1MB per agent |

---

## 📝 Промежуточный итог

### Принятые решения:
1. ✅ Создать `shared/agent-api` модуль с контрактами и DTOs
2. ✅ AIAgentServer — server-side implementation
3. ✅ WebSocket для HITL events
4. ✅ REST API: `/v1/agents/process`, `/v1/agents/{id}/resume`
5. ✅ Client-side AWorker → AgentClient proxy

### Открытые вопросы:
1. Какие ContextStrategy реализации критичны для MVP?
2. Offline mode — нужен или нет?
3. Authentication — API key или JWT?

### Следующий шаг:
Утвердить предложение и перейти к детальному design.

---

## ⚔️ Дебаты: Kotlin Senior Developer vs Senior Architect

**Kotlin Senior:** В варианте B (Hybrid) мы можем переиспользовать AIAgent на клиенте через KMP.

**Senior Architect:** Нет, это заблуждение. AIAgent зависит от Room, Android-specific memory providers. KMP не решит эти зависимости без significant refactoring.

**Kotlin Senior:** Но ContextStrategy, ToolProvider — это уже интерфейсы. Мы можем сделать expect/actual для platform-specific implementations.

**Senior Architect:** Это добавляет complexity. Давайте сначала сделаем чистый server-side подход (Вариант A), а потом, если нужен offline, добавим KMP agent-runtime.

**Решение:** Вариант A (Pure Server) принят для initial implementation.

---

## 🗳️ Голосование: Все согласны с предложенным решением?

- 🤖 AI Systems Architect: ✅ Согласен
- 🏗 Senior Architect: ✅ Согласен  
- 💻 Kotlin Senior Developer: ✅ Согласен
- 🔍 Quality Reviewer: ✅ Согласен (с оговорками по Medium issues)

---

## ✅ Итоговое решение

### Что решено

1. **Создать `shared/agent-api`** — общий модуль для контрактов и DTOs
2. **AIAgentServer** — server-side implementation без Android dependencies  
3. **WebSocket endpoint** `/v1/agents/events/{sessionId}` — для HITL real-time events
4. **REST endpoints** — `/v1/agents/process`, `/v1/agents/{id}/resume`, `/v1/agents/{id}/context`
5. **AgentClient** — Android proxy для вызова server-side agents
6. **Single ContextStrategy** (Full) для MVP, добавлять по необходимости

### Архитектура модулей

```
shared/
└── agent-api/                      # НОВЫЙ
    └── src/main/kotlin/com/example/day/shared/agent/
        ├── dto/
        │   ├── AgentRequest.kt
        │   ├── AgentResponse.kt
        │   └── WorkerEventDto.kt
        └── contract/
            ├── AgentRuntimeContext.kt
            ├── AgentContextRepository.kt
            ├── ToolProvider.kt
            └── ContextStrategy.kt

ai-gateway/
└── src/main/kotlin/com/example/day/aigateway/
    ├── agent/                       # НОВЫЙ
    │   ├── AIAgentServer.kt
    │   ├── domain/
    │   │   └── workers/
    │   ├── infrastructure/
    │   │   ├── repository/
    │   │   └── tools/
    │   └── di/
    ├── websocket/
    │   └── AgentWebSocketServer.kt  # НОВЫЙ
    └── api/
        └── routes/
            └── AgentRoutes.kt       # НОВЫЙ
```

### Приоритеты (P0-P2)

| P | Задача | Файлы |
|---|--------|-------|
| P0 | Shared agent-api module | `shared/agent-api/build.gradle.kts`, DTOs |
| P0 | AIAgentServer core | `ai-gateway/agent/AIAgentServer.kt` |
| P0 | AgentRoutes REST | `ai-gateway/agent/api/routes/AgentRoutes.kt` |
| P1 | WebSocket HITL | `ai-gateway/websocket/AgentWebSocketServer.kt` |
| P1 | AgentClient (Android) | `app/agent/client/AgentClient.kt` |
| P2 | Full ContextStrategy suite | ContextSummaryStrategy, etc. |
| P2 | Server-side tools | `ai-gateway/agent/infrastructure/tools/` |

### Действия

1. **Создать `shared/agent-api`** с gradle module и base DTOs
2. **Определить contract interfaces** — AgentContextRepository, ToolProvider, etc.
3. **Добавить `ai-gateway/agent/`** package structure
4. **Реализовать AIAgentServer** — адаптировать AIAgent.kt без Android deps
5. **Добавить AgentRoutes.kt** — REST endpoints
6. **Реализовать WebSocket** — для HITL approval flow
7. **Создать AgentClient** — Android proxy для server calls
8. **Добавить tests** — AIAgentServerTest, AgentRoutesTest

### Риски и mitigation

| Риск | Вероятность | Mitigation |
|------|-------------|------------|
| HITL WebSocket complexity | Medium | Использовать существующий Ktor websocket pattern |
| LLM provider abstraction | Low | Уже есть LlmProvider interface |
| Context persistence | Medium | Начать с in-memory, добавить Redis later |

---

## Файлы для создания/модификации

### Новые файлы
- `shared/agent-api/build.gradle.kts`
- `shared/agent-api/src/main/kotlin/com/example/day/shared/agent/dto/*.kt` (5 файлов)
- `shared/agent-api/src/main/kotlin/com/example/day/shared/agent/contract/*.kt` (6 интерфейсов)
- `ai-gateway/src/main/kotlin/.../agent/AIAgentServer.kt`
- `ai-gateway/src/main/kotlin/.../agent/api/routes/AgentRoutes.kt`
- `ai-gateway/src/main/kotlin/.../websocket/AgentWebSocketServer.kt`
- `ai-gateway/src/main/kotlin/.../agent/infrastructure/repository/*.kt`
- `ai-gateway/src/main/kotlin/.../agent/infrastructure/tools/*.kt`

### Модифицируемые файлы
- `settings.gradle.kts` — include shared/agent-api
- `ai-gateway/build.gradle.kts` — add shared/agent-api dependency
- `ai-gateway/src/main/kotlin/.../AiGatewayServer.kt` — wire AgentRoutes
