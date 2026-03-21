# Phase 4: Future Enhancements

> **Статус**: Future / Backlog  
> **Приоритет**: P3 (Low)  
> **Оценка сложности**: Medium

---

## Overview

Данный этап содержит улучшения, которые **не являются критичными** для запуска UITL, но могут быть полезны в будущем.

---

## 1. RemoteAgentProtocol

### Мотивация

Когда agent логика будет вынесена на сервер, `LocalAgentProtocol` нужно заменить на `RemoteAgentProtocol`, при этом **UI и UITLInteractor не должны меняться**.

### Архитектура

```kotlin
interface AgentProtocol {
    fun execute(request: AgentRequest): Flow<AgentEvent>
    suspend fun submitApproval(approval: ToolApproval): Result<Unit>
    suspend fun cancel(): Result<Unit>
}

// Local: работает в том же процессе
class LocalAgentProtocol(...) : AgentProtocol { ... }

// Remote: работает через HTTP/WebSocket
class RemoteAgentProtocol(
    private val httpClient: HttpClient,
    private val webSocketClient: WebSocketClient
) : AgentProtocol {
    override fun execute(request: AgentRequest): Flow<AgentEvent> = flow {
        // HTTP POST to start execution
        val response = httpClient.post("/api/agent/execute") {
            setBody(request)
        }
        
        // WebSocket для событий
        webSocketClient.connect("/ws/agent/${response.requestId}") { event ->
            emit(event.toAgentEvent())
        }
    }
    
    override suspend fun submitApproval(approval: ToolApproval): Result<Unit> {
        return httpClient.post("/api/agent/approval") {
            setBody(approval)
        }
    }
}
```

### Файлы для создания

```
agent/data/protocol/
├── LocalAgentProtocol.kt      # существует
└── RemoteAgentProtocol.kt     # СОЗДАТЬ
```

---

## 2. Retry Policy для Tool Execution

### Мотивация

При transient errors (network timeout, server busy) полезно автоматически retry.

### Архитектура

```kotlin
data class RetryPolicy(
    val maxRetries: Int = 1,
    val backoffMillis: Long = 1000,
    val transientOnly: Boolean = true  // retry только для transient errors
)

enum class ToolErrorType {
    TRANSIENT,    // network timeout, 5xx
    PERMANENT,    // 4xx, invalid arguments
    UNKNOWN
}

class RetryableToolExecutor(
    private val toolProvider: ToolProvider,
    private val retryPolicy: RetryPolicy
) {
    suspend fun executeWithRetry(
        call: ModelResult.Success.ToolCall,
        context: ToolCallContext
    ): ToolResult {
        repeat(retryPolicy.maxRetries + 1) { attempt ->
            val result = toolProvider.executeToolCall(call, context)
            
            if (result.isSuccess) return ToolResult.from(result)
            
            val errorType = classifyError(result.exceptionOrNull())
            
            if (!retryPolicy.transientOnly || errorType == ToolErrorType.TRANSIENT) {
                if (attempt < retryPolicy.maxRetries) {
                    delay(retryPolicy.backoffMillis * (attempt + 1))
                    continue
                }
            }
            
            return ToolResult.fromFailed(result, errorType)
        }
    }
    
    private fun classifyError(error: Throwable?): ToolErrorType {
        return when (error) {
            is HttpException -> {
                if (error.code() in 500..599) ToolErrorType.TRANSIENT
                else ToolErrorType.PERMANENT
            }
            is SocketTimeoutException -> ToolErrorType.TRANSIENT
            else -> ToolErrorType.UNKNOWN
        }
    }
}
```

### Файлы для создания

```
agent/domain/tools/
├── RetryPolicy.kt
└── RetryableToolExecutor.kt
```

---

## 3. Parallel Tool Execution

### Мотивация

Когда LLM возвращает несколько independent tool calls (например, `get_weather(city=A)` и `get_weather(city=B)`), их можно выполнить параллельно для уменьшения latency.

### Архитектура

```kotlin
class ParallelToolExecutor {
    suspend fun executeParallel(
        calls: List<ModelResult.Success.ToolCall>,
        context: ToolCallContext,
        toolProvider: ToolProvider,
        maxParallelism: Int = 3
    ): List<ToolResult> = coroutineScope {
        calls.map { call ->
            async {
                executeToolCall(call, context, toolProvider)
            }
        }.awaitAll()
    }
}

// Использование в ToolCallOrchestratorImpl:
if (config.parallelExecution) {
    val results = parallelExecutor.executeParallel(toolCalls, context, toolProvider)
    // Добавить все results к history
} else {
    // Существующий sequential loop
}
```

### Предостережение

- **Не все tools можно выполнять параллельно** — некоторые имеют side effects
- **Сложность с ordering** — LLM может ожидать определённый порядок результатов
- **Рекомендация**: Оставить `parallelExecution = false` по умолчанию

---

## 4. Cancellation Support

### Мотивация

Пользователь может захотеть отменить long-running tool execution.

### Архитектура

```kotlin
class ToolCallOrchestratorImpl {
    private var currentJob: Job? = null
    
    override suspend fun execute(...): Result<ToolCallingResult> {
        currentJob = coroutineContext.job
        // ... execution
    }
    
    fun cancel() {
        currentJob?.cancel()
    }
}

interface AgentProtocol {
    suspend fun cancel(): Result<Unit>
}

class LocalAgentProtocol {
    override suspend fun cancel(): Result<Unit> {
        currentJob?.cancel()
        return Result.success(Unit)
    }
}
```

---

## 5. Tool Execution Metrics

### Мотивация

Для debugging и optimization полезно знать:
- Сколько раз каждый tool был вызван
- Среднее время выполнения
- Error rate

### Архитектура

```kotlin
data class ToolMetrics(
    val toolName: String,
    val callCount: Int,
    val totalDurationMs: Long,
    val errorCount: Int,
    val lastCalledAt: Long
)

class ToolMetricsCollector {
    private val metrics = ConcurrentHashMap<String, ToolMetrics>()
    
    fun recordCall(toolName: String, durationMs: Long, isError: Boolean) {
        metrics.compute(toolName) { _, existing ->
            val count = (existing?.callCount ?: 0) + 1
            val totalDuration = (existing?.totalDurationMs ?: 0) + durationMs
            val errors = (existing?.errorCount ?: 0) + if (isError) 1 else 0
            ToolMetrics(toolName, count, totalDuration, errors, System.currentTimeMillis())
        }
    }
    
    fun getMetrics(): Map<String, ToolMetrics> = metrics.toMap()
    
    fun getAverageDuration(toolName: String): Long {
        val m = metrics[toolName] ?: return 0
        return m.totalDurationMs / m.callCount
    }
}
```

---

## Priority Order для Future Enhancements

| Enhancement | Priority | Notes |
|-------------|----------|-------|
| RemoteAgentProtocol | High | Для cloud deployment |
| Cancellation Support | Medium | User experience |
| Retry Policy | Medium | Reliability |
| Tool Metrics | Low | Debugging |
| Parallel Execution | Low | Performance (may not help) |

---

## Notes

Эти enhancements могут быть реализованы независимо друг от друга. Рекомендуется:

1. **RemoteAgentProtocol** — планировать когда появится cloud backend
2. **Cancellation** — относительно просто, улучшает UX
3. **Retry Policy** — только для важных production scenarios
4. **Metrics** — debugging tool, не для MVP
5. **Parallel** — profiling-driven decision
