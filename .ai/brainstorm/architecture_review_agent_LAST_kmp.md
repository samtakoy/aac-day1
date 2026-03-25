# Multi-Agent Brainstorm v3: Shared Domain Agent Architecture

## Задача (повторные дебаты)
Перенести AIAgent и все его доменные сущности в shared модуль для возможности использования как на клиенте (Android), так и на сервере (ai-gateway). Это **domain слой — чистый Kotlin без Room и клиентских библиотек**. Рассмотреть варианты реализации и описать изменения.

---

## 🤖 AI Systems Architect (включаясь)

Принял задачу. Анализирую AIAgent.kt и его зависимости на предмет возможности extraction в чистый Kotlin/KMP модуль.

### Анализ AIAgent.kt зависимостей

```kotlin
class AIAgent(
    val config: AgentConfig,                    // ✅ Domain model (чистый Kotlin)
    private val contextRepository: AgentContextRepository,  // ⚠️ Interface
    private val strategy: ContextStrategy,                // ✅ Interface (чистый Kotlin)
    private val memoryProvider: MemoryProvider,            // ⚠️ Interface
    private val toolProvider: ToolProvider,               // ✅ Interface (чистый Kotlin)
    private val orchestrator: ToolCallOrchestrator,       // ✅ Interface (чистый Kotlin)
    private val toolExecutor: ToolExecutor,               // ✅ Interface (чистый Kotlin)
    private val hitlSessionManager: HitlSessionManager     // ⚠️ HitlSessionManager
)
```

### Ключевой вопрос: Что есть "чистый Kotlin"?

**Domain models (✅ можно в shared):**
- `AgentConfig` — не зависит от Room, использует `ModelSettings` и `CtxStrategyType`
- `AContextMessage` — domain model для сообщений
- `ProcessResult` — sealed class для результатов
- `AIAgentResult` — result data class
- `ContextStrategyResult` — контекст после обработки

**Interfaces (✅ можно в shared):**
- `AgentContextRepository` — interface, не имеет Room зависимостей в контракте
- `ContextStrategy` — interface
- `MemoryProvider` — interface  
- `ToolProvider` — interface
- `ToolCallOrchestrator` — interface
- `ToolExecutor` — interface
- `HitlSessionManager` — interface

**Infrastructure (⚠️ platform-specific):**
- `AgentContextRepositoryImpl` — Room implementation
- `MemoryProvider` implementations — Android-specific
- `HitlSessionManager` implementation — может быть in-memory на сервере

### HITL Architecture

```kotlin
// HitlSessionManager.kt - interface в shared
interface HitlSessionManager {
    fun hasActiveSession(agentId: String): Boolean
    fun createSession(runId: String, agentId: String, prompt: ContextMessage, loopMessages: List<Message>): HitlSession
    fun getSession(runId: String): HitlSession?
    fun closeSession(runId: String)
    fun updateSession(runId: String, decisions: Map<String, ToolCallDecision>)
}

// HitlSession - data class в shared
data class HitlSession(
    val runId: String,
    val agentId: String,
    val prompt: ContextMessage,
    val loopMessages: List<Message>,
    val pendingToolCalls: List<ToolCall>,
    val decisions: Map<String, ToolCallDecision> = emptyMap()
)
```

**Вывод: HitlSessionManager — это просто in-memory state management**, может быть в shared.

### Agent Core Logic

```kotlin
// AIAgent.kt можно перенести в shared!
class AIAgent(
    val config: AgentConfig,
    private val contextRepository: AgentContextRepository,
    private val strategy: ContextStrategy,
    private val memoryProvider: MemoryProvider,
    private val toolProvider: ToolProvider,
    private val orchestrator: ToolCallOrchestrator,
    private val toolExecutor: ToolExecutor,
    private val hitlSessionManager: HitlSessionManager
) {
    // Весь этот код — платформенно-независимый
    // Использует только интерфейсы и domain модели
}
```

### Риски AI Systems
- **Serialization**: Нужна kotlinx.serialization для cross-platform DTOs
- **Coroutines**: Должны быть common (corkturounds-multplatform)

---

## 🏗 Senior Architect (включаясь)

Building on AI Systems Architect's analysis. Оцениваю архитектурную чистоту и варианты реализации.

### Варианты Shared модуля

**Вариант 1: Pure Kotlin Multiplatform (KMP)**
```
shared/
└── agent-domain/               # KMP module
    ├── src/
    │   ├── commonMain/kotlin/  # AIAgent, interfaces, models
    │   ├── androidMain/kotlin/ # Android-specific (Room repos)
    │   └── jvmMain/kotlin/    # Server-specific (in-memory repos)
    └── build.gradle.kts
```

**Вариант 2: Simple Kotlin Multiplatform (без android/jvm split)**
```
shared/
└── agent-domain/               # Simple KMP module
    ├── src/commonMain/kotlin/  # AIAgent + interfaces
    └── build.gradle.kts
# Platform-specific: отдельные модули для android/server
```

**Вариант 3: Vanilla Kotlin (JVM-only shared)**
```
shared/
└── agent-domain/               # Pure Kotlin/JVM module
    └── src/main/kotlin/         # Без KMP, для ai-gateway + Android
```

### Анализ текущей структуры Android

```
app/src/main/java/com/example/day/core/core_features/agent/
├── domain/                    # Домен (ЧИСТЫЙ КОТЛИН)
│   ├── AIAgent.kt
│   ├── AgentContextRepository.kt
│   ├── model/
│   │   ├── AgentConfig.kt
│   │   ├── AContextMessage.kt
│   │   └── ProcessResult.kt
│   ├── strategy/
│   │   ├── ContextStrategy.kt
│   │   └── impl/*.kt
│   └── tools/
│       ├── ToolProvider.kt
│       ├── ToolCallOrchestrator.kt
│       └── ...
├── data/                      # Data layer (ROOM + Android)
│   ├── AgentContextRepositoryImpl.kt
│   ├── local/ (Room DAOs, Entities)
│   └── tools/ (Android-specific)
└── di/
```

### Рефакторинг: Move domain → shared

```
shared/agent-domain/
├── src/commonMain/kotlin/com/example/day/shared/agent/
│   ├── AIAgent.kt                          # MOVE
│   ├── AgentContextRepository.kt           # MOVE
│   ├── model/
│   │   ├── AgentConfig.kt                   # MOVE
│   │   ├── AContextMessage.kt              # MOVE
│   │   ├── ProcessResult.kt                # MOVE
│   │   └── ContextStrategyResult.kt        # MOVE
│   ├── strategy/
│   │   ├── ContextStrategy.kt               # MOVE
│   │   └── impl/
│   │       ├── ContextFullStrategy.kt      # MOVE
│   │       └── ... (all strategies)
│   ├── tools/
│   │   ├── ToolProvider.kt                  # MOVE
│   │   ├── ToolCallOrchestrator.kt          # MOVE
│   │   ├── ToolExecutor.kt                  # MOVE
│   │   ├── OrchestratorRequest.kt          # MOVE
│   │   ├── OrchestratorResult.kt           # MOVE
│   │   └── impl/
│   │       └── ToolCallOrchestratorImpl.kt # MOVE
│   └── hitl/
│       ├── HitlSessionManager.kt            # MOVE
│       ├── HitlSession.kt                   # MOVE
│       └── ToolCallDecision.kt             # MOVE
│
└── src/androidMain/kotlin/                 # Platform-specific
    └── repository/
        └── AgentContextRepositoryImpl.kt   # KEEP (Room)

app/src/main/java/.../agent/
├── domain/                    # REMOVE (moved to shared)
├── data/                      # REMOVE most (only Android-specific)
└── di/

ai-gateway/
└── agent/
    └── infrastructure/
        └── repository/
            └── InMemoryAgentContextRepository.kt  # NEW (server impl)
```

### Trade-offs

| Вариант | Сложность | KMP | Android | Server |
|---------|-----------|-----|---------|--------|
| 1: Full KMP | Высокая | ✅ | ✅ | ✅ |
| 2: Simple KMP | Средняя | ✅ | ⚠️ | ✅ |
| 3: Vanilla JVM | Низкая | ❌ | ⚠️ | ✅ |

**Рекомендация: Вариант 2 (Simple KMP)** — domain слой без платформенных split, platform-specific реализации в отдельных модулях.

---

## 💻 Kotlin Senior Developer (включаясь)

Оцениваю с точки зрения Kotlin, KMP, Gradle. Проверяю feasibility и Gradle integration.

### KMP Setup Analysis

Текущий `shared/simple-chat-api` — это **не KMP**, а простой JVM модуль:
```kotlin
// shared/simple-chat-api/build.gradle.kts
plugins {
    kotlin("multiplatform") apply false  // Just declared, not used
    kotlin("jvm")
}
```

### Предложение: Создать agent-domain как Real KMP

```kotlin
// shared/agent-domain/build.gradle.kts
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm {
        withJava()
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
            }
        }
        val jvmMain by getting {
            dependencies {
                // Нет platform-specific deps для common
            }
        }
    }
}
```

### Ключевые изменения в Android client

**Удалить из `app/agent/domain/`:**
- AIAgent.kt
- AgentContextRepository.kt
- Все model/, strategy/, tools/ (кроме Android-specific)

**Добавить dependency:**
```kotlin
// app/build.gradle.kts
dependencies {
    implementation(project(":shared:agent-domain"))
    // Room repository теперь имплементирует интерфейс из shared
}
```

**Room implementation остаётся в Android:**
```kotlin
// app/src/main/kotlin/.../data/repository/AgentContextRepositoryImpl.kt
// Имплементирует AgentContextRepository из shared
class AgentContextRepositoryImpl(
    private val agentDao: AgentDao,
    private val contextMemoryDao: AgentContextMemoryDao
) : AgentContextRepository {
    // Room-specific implementation
}
```

### ai-gateway server changes

**Добавить dependency:**
```kotlin
// ai-gateway/build.gradle.kts
dependencies {
    implementation(project(":shared:agent-domain"))
}
```

**Server-specific implementations:**
```kotlin
// ai-gateway/src/main/kotlin/.../agent/infrastructure/
// InMemoryAgentContextRepository
// InMemoryHitlSessionManager
// ServerToolExecutor
```

### AIAgentFactory - критический компонент

```kotlin
// AIAgentFactory.kt - ВАЖНО: это factory, остаётся где создаётся
// В Android: AgentCoreFeatureModule создаёт через Dagger
// На Server: AgentServerModule создаёт через manual DI

// Shared: только interface
interface AIAgentFactory {
    fun create(agentId: String): AIAgent
}

// Android: AgentCoreFeatureModule
class AndroidAIAgentFactory(
    private val repository: AgentContextRepositoryImpl,
    // ... other deps
) : AIAgentFactory { ... }

// Server: AgentServerModule  
class ServerAIAgentFactory(
    private val repository: InMemoryAgentContextRepository,
    // ... other deps
) : AIAgentFactory { ... }
```

### Workers - отдельная история

**AWorker** — тоже чистый Kotlin, можно в shared!

```kotlin
// shared/agent-domain/workers/base/AWorker.kt
interface AWorker {
    suspend fun doWork(
        userPrompt: String,
        context: WorkerContext,  // Shared context
        onEvent: (suspend (WorkerEvent) -> Unit)? = null
    ): WorkerResult
}
```

**НО:** Worker implementations могут быть platform-specific:
- `RagWorker` — depends on Android document index → Android only
- `McpWorker` — MCP protocol → может быть server-side
- `SimpleWorker` — чистый → shared

### Gradle Module Structure

```kotlin
// settings.gradle.kts
pluginManagement {
    include(":shared:simple-chat-api")
    include(":shared:agent-domain")  // NEW
    include(":ai-gateway")
    include(":app")
}

// shared/agent-domain/build.gradle.kts
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm()
    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
                implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable")
            }
        }
    }
}
```

### Риски Implementation

| Риск | Mitigation |
|------|------------|
| Circular dependencies | agent-domain не должен зависеть от app или ai-gateway |
| Serialization divergence | Использовать统一的 kotlinx.serialization |
| DI complexity | Factory pattern решает platform-specific creation |

---

## 🔍 Quality Reviewer (включаясь)

Проверяю на overengineering, SOLID, и实用性.

### Issues Found

#### Medium #1: Over-abstraction of Strategies

**Проблема:** 6 реализаций ContextStrategy:
- ContextEmptyStrategy
- ContextFullStrategy
- ContextSlidingWindowStrategy
- ContextStickyFactsStrategy
- ContextBranchingStrategy
- ContextSummaryStrategy

**Вопрос:** Это真的 нужно все? Или можно selector pattern?

```kotlin
// Вместо 6 отдельных классов
class ContextStrategySelector(
    private val strategies: Map<CtxStrategyType, ContextStrategy>
) {
    fun select(type: CtxStrategyType): ContextStrategy = 
        strategies[type] ?: ContextFullStrategy()
}
```

**Рекомендация:** Selector не нужен — лучше 1 interface + 1+N implementations. Но это **не overengineering** — стратегии имеют разную логику.

#### Medium #2: HitlSessionManager - Stateful

**Проблема:** `HitlSessionManager` хранит состояние в памяти. На сервере это ок. На клиенте... он уже используется?

```kotlin
// AIAgent.process()
if (hitlSessionManager.hasActiveSession(config.id)) {
    return Result.failure(HitlSessionBusyError())
}
```

**Ответ:** HITL нужен для client-server architecture. В pure Android (offline) HITL не используется. На сервере — нужен.

**Рекомендация:** HitlSessionManager — **обязателен для shared** (для server use case), но Android client может не использовать.

#### High #3: Прямое использование ModelRequest в Agent

**Проблема:** AIAgent использует `ModelRequest.Tool` напрямую:

```kotlin
// OrchestratorRequest
data class OrchestratorRequest(
    val messages: List<ModelRequest.Message>,
    val systemPrompt: String?,
    val modelSettings: ModelSettings,
    val tools: List<ModelRequest.Tool>  // ⚠️ LLM-specific
)
```

**Вопрос:** Это leak abstraction. Agent domain не должен знать про LLM specifics.

**Рекомендация:** Создать generic `ToolDefinition` в shared:

```kotlin
// shared/agent-domain/tools/ToolDefinition.kt
data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: JsonObject,
    val handler: ToolHandler  // Platform-specific
)
```

#### Low #4: AWorker зависит от "Chat"

```kotlin
// Текущий AWorker
interface AWorker {
    suspend fun doWork(
        userPrompt: String,
        chat: Chat,  // ⚠️ Android/Room specific
        ...
    )
}
```

**Факт:** В shared proposal это уже исправлено — `WorkerContext` вместо `Chat`.

### Veto Check

- **More than 3 abstraction layers?** ✅ 3 слоя: Domain (shared) → Infrastructure (platform) → Data (impl). Нормально.
- **AI/LLM used where rule-based suffices?** ✅ Tool calling justified.
- **Circular dependencies?** ❌ Не обнаружено.
- **Cost estimate missing?** ⚠️ Добавить.

### Final Assessment

**Medium Issues:** 3 (Strategies overkill, HitlStateful, Tool leak)
**Recommendation:** Accept with Medium issues addressed in implementation.

---

## ⚔️ Дебаты: AI Systems Architect vs Quality Reviewer

**QA:** ToolProvider возвращает `ModelRequest.Tool` — это leak abstraction. Agent domain не должен знать про LLM модели.

**AI Systems:** Согласен. Но `ModelRequest.Tool` — это DTO для serialization. Мы можем создать generic `ToolDefinition` и маппить в `ModelRequest.Tool` в infrastructure layer.

**QA:** Это добавляет mapping layer.

**AI Systems:** Это нормально для Clean Architecture. Domain не должен знать про LLM DTOs.

**QA:** Ок, but let's track this as Medium priority refactoring. Не must-fix для initial migration.

**Решение:** Принято. Tool abstraction будет улучшена post-migration.

---

## 📝 Промежуточный итог v2

### Принятые решения:
1. ✅ **Shared KMP module** `shared/agent-domain` с pure Kotlin domain
2. ✅ **AIAgent** — переносится в shared как есть (чистый Kotlin)
3. ✅ **Interfaces** — AgentContextRepository, ToolProvider, ContextStrategy идут в shared
4. ✅ **Platform-specific implementations:**
   - Android: Room AgentContextRepositoryImpl, AndroidMemoryProvider
   - Server: InMemoryAgentContextRepository, ServerHitlSessionManager
5. ✅ **Workers** — AWorker interface в shared, implementations в platform modules

### Изменения в Android client:
- Удалить `app/core/core_features/agent/domain/`
- Удалить `app/core/core_features/agent/data/` (кроме Android-specific)
- Добавить dependency на `shared:agent-domain`
- Оставить `AgentContextRepositoryImpl` (Room implementation)

### Изменения в ai-gateway server:
- Добавить dependency на `shared:agent-domain`
- Создать `ai-gateway/agent/infrastructure/` с server implementations
- `AIAgent` используется напрямую из shared

### Открытые вопросы:
1. Что делать с LLM domain models (ModelRequest, ModelSettings)?
2. Workers implementations — где именно?

---

## 💻 Kotlin Senior Developer (второй раунд)

**Вопрос от Moderator:** Workers implementations — где?

### Worker Distribution

| Worker | Location | Причина |
|--------|----------|---------|
| AWorker interface | shared/agent-domain | Pure Kotlin |
| SimpleWorker | shared/agent-domain | Pure Kotlin, stateless |
| StepWorker | shared/agent-domain | Pure Kotlin |
| TeamWorker | shared/agent-domain | Pure Kotlin |
| PromptWorker | shared/agent-domain | Pure Kotlin |
| RagWorker | app/agent/workers | Android-specific (document index) |
| TaskWorker | shared/agent-domain | Stateless, но имеет Android deps в impl |
| McpWorker | ? | MCP protocol — может быть server-side |
| CompareWorker | shared/agent-domain | Pure Kotlin |

**Решение:**
1. **Simple, Step, Team, Prompt, Compare** — в shared (stateless, pure Kotlin)
2. **RagWorker** — Android-only (непереносим)
3. **TaskWorker** — сложный, требует анализа deps
4. **McpWorker** — Server-only (или отдельный server module)

### LLM Models Question

**Q:** ModelRequest, ModelSettings — где?

**A:** Они уже в `app/core/core_features/llm/domain/model/`. Это **LLM-specific**, не Agent-specific. Есть два варианта:

**Вариант A:** Оставить LLM models в app, создать mapper в shared
```kotlin
// shared/agent-domain/tools/ToolDefinition.kt
data class ToolDefinition(...) // Generic

// app/llm/data/mapper/ToolMapper.kt  
fun ToolDefinition.toModelRequestTool(): ModelRequest.Tool = ...
```

**Вариант B:** Перенести LLM models тоже в shared
```kotlin
// shared/llm-domain/  # NEW separate module
// Но это overkill — LLM models не используются на server
```

**Решение: Вариант A.** LLM models остаются в app, shared использует generic ToolDefinition.

---

## 🗳️ Голосование: Все согласны с архитектурой?

- 🤖 AI Systems Architect: ✅ Согласен
- 🏗 Senior Architect: ✅ Согласен  
- 💻 Kotlin Senior Developer: ✅ Согресен (с уточнением по Workers)
- 🔍 Quality Reviewer: ✅ Согласен (Medium issues noted)

---

## ✅ Итоговое решение v3

### Что решено

1. **Создать `shared/agent-domain` KMP module** — чистый Kotlin
   - AIAgent, все interfaces, models, strategies
   - Workers: Simple, Step, Team, Prompt, Compare
   - HITL: HitlSessionManager, HitlSession

2. **Platform-specific implementations:**
   - Android: Room AgentContextRepositoryImpl, AndroidMemoryProvider, RagWorker
   - Server: InMemoryAgentContextRepository, ServerHitlSessionManager

3. **Gradle integration:**
   - settings.gradle.kts: include shared:agent-domain
   - app: dependency на shared:agent-domain
   - ai-gateway: dependency на shared:agent-domain

4. **LLM models остаются в app** — shared использует generic ToolDefinition

### Архитектура модулей

```
shared/
├── simple-chat-api/          # Существующий (DTOs)
└── agent-domain/            # НОВЫЙ KMP module
    └── src/commonMain/kotlin/com/example/day/shared/agent/
        ├── AIAgent.kt
        ├── AgentContextRepository.kt
        ├── model/
        │   ├── AgentConfig.kt
        │   ├── AContextMessage.kt
        │   ├── ProcessResult.kt
        │   └── ContextStrategyResult.kt
        ├── strategy/
        │   ├── ContextStrategy.kt
        │   └── impl/ (6 strategies)
        ├── tools/
        │   ├── ToolProvider.kt
        │   ├── ToolCallOrchestrator.kt
        │   ├── ToolExecutor.kt
        │   ├── ToolDefinition.kt
        │   └── OrchestratorRequest.kt
        ├── hitl/
        │   ├── HitlSessionManager.kt
        │   └── HitlSession.kt
        └── workers/
            ├── base/AWorker.kt
            ├── base/WorkerEvent.kt
            └── concrete/
                ├── SimpleWorker.kt
                ├── StepWorker.kt
                ├── TeamWorker.kt
                ├── PromptWorker.kt
                └── CompareWorker.kt

app/
└── src/main/kotlin/com/example/day/
    └── core/core_features/agent/
        ├── data/                    # KEEP (Room + Android-specific)
        │   ├── AgentContextRepositoryImpl.kt
        │   └── tools/AndroidToolProvider.kt
        ├── di/
        │   └── AgentCoreFeatureModule.kt  # ADAPT: use shared interfaces
        └── workers/
            └── concrete/
                ├── RagWorker.kt     # Android-specific
                └── TaskWorker.kt    # DEPRECATED: переписать на shared

ai-gateway/
└── src/main/kotlin/com/example/day/aigateway/
    └── agent/
        ├── AIAgentServer.kt        # REMOVE: используем shared.AIAgent
        ├── infrastructure/
        │   ├── repository/
        │   │   └── InMemoryAgentContextRepository.kt  # NEW
        │   └── tools/
        │       └── ServerToolProvider.kt  # IMPLEMENT ToolProvider
        └── di/
            └── AgentServerModule.kt  # NEW: wire shared AIAgent
```

### Приоритеты (P0-P2)

| P | Задача | Файлы |
|---|--------|-------|
| P0 | Create shared/agent-domain KMP module | `shared/agent-domain/build.gradle.kts` |
| P0 | Move domain files to shared | `shared/agent-domain/src/commonMain/...` |
| P0 | Update app/build.gradle.kts | Add shared dependency |
| P0 | Update ai-gateway/build.gradle.kts | Add shared dependency |
| P1 | Create Server implementations | `ai-gateway/agent/infrastructure/` |
| P1 | Adapt Android AgentCoreFeatureModule | Use shared interfaces |
| P1 | Create Server AgentServerModule | Wire AIAgent |
| P2 | Migrate Workers | Split to shared vs platform |
| P2 | Refactor ToolDefinition | Address QA Medium #3 |

### Детальные изменения

#### Android Client

**Удалить:**
```
app/src/main/java/com/example/day/core/core_features/agent/domain/
├── AIAgent.kt
├── AgentContextRepository.kt
├── AIAgentFactory.kt
├── model/ (все кроме Android-specific)
├── strategy/ (все)
├── tools/ (interfaces и implementations)
└── workers/ (кроме RagWorker и TaskWorker)
```

**Сохранить:**
```
app/src/main/java/.../agent/
├── data/
│   ├── AgentContextRepositoryImpl.kt  # IMPLEMENT shared.AgentContextRepository
│   ├── local/ (Room DAOs, Entities)
│   └── tools/McpToolProvider.kt      # Android-specific
├── di/
│   └── AgentCoreFeatureModule.kt     # ADAPT: import from shared
└── workers/
    └── concrete/
        ├── RagWorker.kt              # Android-specific
        └── TaskWorker.kt             # DEPRECATED
```

#### ai-gateway Server

**Создать:**
```
ai-gateway/src/main/kotlin/com/example/day/aigateway/agent/
├── AIAgentRunner.kt                  # NEW: Wrapper around shared AIAgent
├── infrastructure/
│   ├── repository/
│   │   └── InMemoryAgentContextRepository.kt  # IMPLEMENT shared interface
│   └── tools/
│       ├── ServerToolProvider.kt     # IMPLEMENT shared interface
│       ├── ServerToolExecutor.kt     # IMPLEMENT shared interface
│       └── ServerToolCallOrchestrator.kt  # IMPLEMENT shared interface
└── di/
    └── AgentServerModule.kt          # NEW: DI for server agent
```

**Удалить (или переиспользовать):**
```
ai-gateway/agent/AIAgentServer.kt     # REUSE: shared.AIAgent
```

#### Gradle Changes

**settings.gradle.kts:**
```kotlin
include(":shared:agent-domain")
```

**shared/agent-domain/build.gradle.kts:**
```kotlin
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm()
    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
                implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable")
            }
        }
    }
}
```

**app/build.gradle.kts:**
```kotlin
dependencies {
    implementation(project(":shared:agent-domain"))
    // Room и другие Android deps...
}
```

**ai-gateway/build.gradle.kts:**
```kotlin
dependencies {
    implementation(project(":shared:agent-domain"))
    // Ktor, OkHttp...
}
```

### Риски и mitigation

| Риск | Вероятность | Mitigation |
|------|-------------|------------|
| Circular deps | Low | agent-domain не зависит от app/ai-gateway |
| Migration effort | Medium | Делать постепенно, P0→P1→P2 |
| Serialization issues | Low | Единая kotlinx.serialization |
| DI complexity | Medium | Factory pattern решает |

### Cost Estimate

| Компонент | Ресурс | Оценка |
|-----------|--------|--------|
| Shared module creation | Dev time | 1-2 days |
| Android adaptation | Dev time | 2-3 days |
| Server adaptation | Dev time | 2-3 days |
| Testing | Dev time | 1-2 days |
| **Total** | | **6-10 days** |
