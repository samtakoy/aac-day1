# Phase 3: Tool Calling Config + Idempotency

> **Статус**: TODO  
> **Приоритет**: P2  
> **Оценка сложности**: Low (1-2 дня)

---

## Мотивация

### Проблема 1: Magic Constant `MAX_TOOL_LOOPS`

В коде `ToolCallOrchestratorImpl` есть захардкоженное значение:

```kotlin
private companion object {
    private const val MAX_TOOL_LOOPS = 3
}
```

Это не позволяет:
- Динамически менять лимит в зависимости от типа задачи
- Конфигурировать через настройки пользователя
- Адаптировать для разных моделей (у некоторых проблемы с длинными tool chains)

### Проблема 2: No Idempotency

При LLM reasoning errors или network retries один tool call может выполниться несколько раз:

```
LLM → get_user(id=1) → get_user(id=1)  // оба выполнятся
```

Хотя OpenRouter возвращает структурированный JSON (не raw text), всё равно возможны дубликаты в рамках одной сессии.

---

## Цели

1. Вынести `MAX_TOOL_LOOPS` в конфигурацию
2. Добавить `ToolCallingConfig` data class
3. Реализовать simple exact-match cache для предотвращения duplicate execution
4. Обеспечить backward compatibility (defaults работают как раньше)

---

## Пошаговый план

### Шаг 1: Создать ToolCallingConfig

**Файл**: `app/src/main/java/com/example/day/core/core_features/agent/domain/tools/ToolCallingConfig.kt`

```kotlin
package com.example.day.core.core_features.agent.domain.tools

data class ToolCallingConfig(
    val maxLoops: Int = DEFAULT_MAX_LOOPS,
    val enableIdempotencyCache: Boolean = true,
    val enableSessionCache: Boolean = true
) {
    companion object {
        const val DEFAULT_MAX_LOOPS = 3
        const val MIN_MAX_LOOPS = 1
        const val MAX_MAX_LOOPS = 10
    }
}
```

### Шаг 2: Добавить Idempotency Cache

**Файл**: `app/src/main/java/com/example/day/core/core_features/agent/domain/tools/impl/ToolCallOrchestratorImpl.kt`

```kotlin
class ToolCallOrchestratorImpl @Inject constructor(
    private val llmProvider: LlmRequestUseCase,
    private val toolProvider: ToolProvider,
    private val sessionApprovalCache: SessionApprovalCache,
    private val config: ToolCallingConfig = ToolCallingConfig()  // ← ДОБАВИТЬ
) : ToolCallOrchestrator {

    // Idempotency cache — key: "functionName:argumentsHash", value: ToolResult
    private val executionCache = mutableMapOf<String, ToolResult>()
    
    override suspend fun execute(...): Result<ToolCallingResult> {
        var loopIndex = 0
        
        while (loopIndex < config.maxLoops) {  // ← Использовать config
            // ... existing code ...
        }
    }
    
    private suspend fun executeWithIdempotency(
        call: ModelResult.Success.ToolCall,
        context: ToolCallContext
    ): ToolResult {
        if (!config.enableIdempotencyCache) {
            return executeToolCallDirect(call, context)
        }
        
        val cacheKey = computeCacheKey(call)
        
        return executionCache.getOrPut(cacheKey) {
            executeToolCallDirect(call, context)
        }
    }
    
    private fun computeCacheKey(call: ModelResult.Success.ToolCall): String {
        // Use hash of arguments for consistent key
        val argsHash = call.function.arguments.hashCode()
        return "${call.function.name}:$argsHash"
    }
    
    private suspend fun executeToolCallDirect(
        call: ModelResult.Success.ToolCall,
        context: ToolCallContext
    ): ToolResult {
        val result = toolProvider.executeToolCall(call, context)
        return ToolResult(
            toolCallId = call.id,
            content = result.getOrElse { it.message ?: "error" },
            isError = result.isFailure
        )
    }
    
    fun clearExecutionCache() {
        executionCache.clear()
    }
}
```

### Шаг 3: Обновить ToolCallContext

**Файл**: `app/src/main/java/com/example/day/core/core_features/agent/domain/tools/ToolCallContext.kt`

```kotlin
data class ToolCallContext(
    val agentId: Long,
    val toolToServer: Map<String, String> = emptyMap(),
    val config: ToolCallingConfig = ToolCallingConfig()  // ← ДОБАВИТЬ
)
```

### Шаг 4: DI Registration

**Файл**: `app/src/main/java/com/example/day/core/core_features/agent/di/AgentCoreFeatureModule.kt`

```kotlin
@Provides
@Singleton
fun provideToolCallingConfig(
    // Можно брать из настроек пользователя
): ToolCallingConfig = ToolCallingConfig()
```

---

## Критерии завершения

- [ ] `ToolCallingConfig` создан с defaults
- [ ] `MAX_TOOL_LOOPS` вынесен в конфиг
- [ ] Idempotency cache работает
- [ ] Cache очищается между запросами (или использовать scoped cache)
- [ ] Проект компилируется

---

## Файлы для создания/изменения

| Файл | Изменение |
|------|-----------|
| `ToolCallingConfig.kt` | СОЗДАТЬ |
| `ToolCallOrchestratorImpl.kt` | Добавить idempotency cache |
| `ToolCallContext.kt` | Добавить config field |
| `AgentCoreFeatureModule.kt` | Зарегистрировать config |

---

## Notes

### Почему не нужен SemanticToolCallDeduplicator

OpenRouter уже парсит JSON из raw LLM output. Это означает:
- `arguments` — это нормализованная JSON string
- `"{id:1}"` vs `"{id: 1}"` — разные строки, но после JSON parsing дадут `{id: 1}`
- Нам достаточно **exact match** по хешу аргументов

### Idempotency vs Session Cache

| Cache | Назначение | Scope |
|-------|------------|-------|
| `SessionApprovalCache` | Запоминает решения пользователя (approve/reject) | session (chat) |
| `executionCache` | Предотвращает duplicate tool execution | single request |

### Потенциальные улучшения (future)

1. **TTL для executionCache** —防止 memory leak при долгих сессиях
2. **LRU eviction** — ограничить размер cache
3. **Persistence** — сохранять cache между app restarts (для resume functionality)
