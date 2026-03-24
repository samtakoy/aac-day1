# Orchestrator Refactor + HITL Design

**Date:** 2026-03-24
**Status:** Approved

---

## Problem

1. `ToolCallOrchestratorImpl` имеет двойной трекинг сообщений (`messages` / `newMessages`), которые синхронизируются вручную — источник багов и сложности чтения.
2. Оркестратор содержит цикл, управляет историей И выполняет инструменты — слишком много ответственностей.
3. HITL нужно добавить без усложнения AIAgent: он не должен знать про политику выполнения инструментов.

---

## Design

### Ключевые принципы

- `ToolCallOrchestrator` — **один шаг LLM**, не цикл. Не вызывает инструменты. Stateless.
- Цикл tool calling живёт в `AIAgent`.
- `ToolExecutor` — единая точка входа для всех tool calls. Внутри решает: выполнить сразу или поставить на паузу.
- AIAgent не знает про HITL — только про `ToolExecutionResult`.

---

### 1. OrchestratorRequest

```kotlin
data class OrchestratorRequest(
    val messages: List<ModelRequest.Message>,  // полный контекст для LLM, собранный вызывающим
    val systemPrompt: String?,
    val modelSettings: ModelSettings,
    val tools: List<ModelRequest.Tool>
)
```

AIAgent собирает `messages` сам из: `memoryMessages + initialHistory + contextMessages + prompt`.
Оркестратор не знает что эфемерное — это не его дело.

---

### 2. OrchestratorResult

```kotlin
sealed class OrchestratorResult {
    /** LLM вернул финальный ответ, tool calls нет */
    data class Completed(
        val responseText: String,
        val assistantMessage: ModelRequest.Message
    ) : OrchestratorResult()

    /** LLM запросил выполнение инструментов */
    data class PendingApproval(
        val toolCalls: List<ModelResult.Success.ToolCall>,
        val assistantMessage: ModelRequest.Message  // assistant msg с tool_calls для истории
    ) : OrchestratorResult()
}
```

---

### 3. ToolCallOrchestrator (упрощённый)

```kotlin
interface ToolCallOrchestrator {
    suspend fun execute(
        request: OrchestratorRequest,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ): Result<OrchestratorResult>
}
```

**Реализация** (`ToolCallOrchestratorImpl`):
1. Вызвать LLM с `request.messages`
2. Нет tool calls → вернуть `Completed`
3. Есть tool calls → вернуть `PendingApproval(toolCalls, assistantMessage)`

Никакого цикла. Никакого `toolProvider`. ~30-40 строк.

**Судьба `ToolCallingResult` / `ToolCallSession`:** Тип `ToolCallingResult` упраздняется. Тип `ToolCallSession` сохраняется для debug-логирования, если нужен — AIAgent может его собирать сам по ходу цикла. Это не входит в scope данного рефакторинга.

---

### 4. ToolExecutionResult

```kotlin
sealed class ToolExecutionResult {
    /** Все инструменты выполнены (авто или после подтверждения) */
    data class Completed(val results: List<ToolResult>) : ToolExecutionResult()

    /** Инструменты поставлены в очередь на подтверждение */
    data class AwaitingApproval(val runId: String) : ToolExecutionResult()
}
```

---

### 5. ToolExecutor

```kotlin
interface ToolExecutor {
    suspend fun submit(
        runId: String,
        toolCalls: List<ModelResult.Success.ToolCall>,
        loopMessages: List<ModelRequest.Message>,  // все накопленные newMessages до паузы включительно
        context: ToolCallContext,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ): ToolExecutionResult
}
```

**Две реализации:**

- `AutoToolExecutor` — выполняет все tool calls немедленно через `ToolProvider`, возвращает `Completed`. Игнорирует `loopMessages`.
- `HitlToolExecutor` — сохраняет `loopMessages` и `toolCalls` в `HitlSessionManager`, возвращает `AwaitingApproval(runId)`. Фактическое выполнение через `ToolProvider` — позже, при resume.

AIAgent получает нужную реализацию через DI.

---

### 6. WorkerEvent (новые события)

```kotlin
/** Один tool call поставлен в очередь на подтверждение */
data class ApprovalRequired(
    val runId: String,
    val toolCallId: String,
    val toolName: String,
    val arguments: String
) : WorkerEvent()

/** Пользователь вынес решение по tool call */
data class ApprovalDecided(
    val runId: String,
    val toolCallId: String,
    val decision: ToolCallDecision   // APPROVED | REJECTED
) : WorkerEvent()
```

`HitlToolExecutor` эмитит `ApprovalRequired` для каждого tool call при вызове `submit()`.
`HitlSessionManager.recordDecision()` эмитит `ApprovalDecided` — либо через callback, либо через отдельный Flow.

---

### 7. AIAgent — общий метод построения контекста

```kotlin
private data class LlmContext(
    val llmMessages: MutableList<ModelRequest.Message>,
    val newMessages: MutableList<ModelRequest.Message>,
    val snapshot: ContextStrategyResult
)

private suspend fun buildLlmContext(prompt: AContextMessage): LlmContext {
    val memoryMessages = memoryProvider.getMemoryContext()
    val promptMessages = memoryProvider.appendUserPrompt(prompt)
    val snapshot = strategy.process(config, contextRepository)

    val llmMessages = buildList {
        addAll(memoryMessages.map { it.toModelRequestMessage() })
        addAll(snapshot.messages.toModelRequestMessages())
        addAll(promptMessages.context.filter { it.content.isNotBlank() }.map { it.toModelRequestMessage() })
        if (prompt.content.isNotBlank()) add(promptMessages.prompt.toModelRequestMessage())
    }.toMutableList()

    // newMessages: сохраняется в DB, без эфемерных memory/context
    val newMessages = buildList {
        addAll(snapshot.messages.toModelRequestMessages())
        if (prompt.content.isNotBlank()) add(promptMessages.prompt.toModelRequestMessage())
    }.toMutableList()

    return LlmContext(llmMessages, newMessages, snapshot)
}
```

---

### 8. AIAgent — цикл

```kotlin
private companion object {
    const val MAX_TOOL_LOOPS = 10
}

suspend fun process(prompt: AContextMessage, onEvent: ...): Result<ProcessResult> {
    if (hitlSessionManager.hasActiveSession(config.id)) {
        return Result.failure(HitlSessionBusyError())
    }
    val runId = UUID.randomUUID().toString()
    val (llmMessages, newMessages, snapshot) = buildLlmContext(prompt)
    return runToolLoop(runId, llmMessages, newMessages, snapshot, onEvent)
}

private suspend fun runToolLoop(
    runId: String,
    llmMessages: MutableList<ModelRequest.Message>,
    newMessages: MutableList<ModelRequest.Message>,
    snapshot: ContextStrategyResult,
    onEvent: (suspend (WorkerEvent) -> Unit)?
): Result<ProcessResult> {
    var loopCount = 0

    while (loopCount < MAX_TOOL_LOOPS) {
        val request = OrchestratorRequest(llmMessages, config.systemPrompt, config.modelSettings, toolProvider.getTools(config.id))
        val result = orchestrator.execute(request, onEvent).getOrElse { return Result.failure(it) }

        when (result) {
            is OrchestratorResult.Completed -> {
                newMessages.add(result.assistantMessage)
                val extendedSnapshot = snapshot.copy(messages = newMessages.toAContextMessages())
                strategy.afterResponse(config, result.responseText, contextRepository, extendedSnapshot)
                return Result.success(ProcessResult.Success(AIAgentResult(result.responseText, ...)))
            }
            is OrchestratorResult.PendingApproval -> {
                // Добавляем assistantMessage в ОБА списка до вызова submit()
                // loopMessages = newMessages включает assistantMessage — именно это хранит сессия
                llmMessages.add(result.assistantMessage)
                newMessages.add(result.assistantMessage)

                when (val exec = toolExecutor.submit(
                    runId = runId,
                    toolCalls = result.toolCalls,
                    loopMessages = newMessages.toList(),  // всё накопленное включая assistantMessage
                    context = ToolCallContext(agentId = config.id),
                    onEvent = onEvent
                )) {
                    is ToolExecutionResult.Completed -> {
                        val toolMessages = exec.results.toModelRequestMessages()
                        llmMessages.addAll(toolMessages)
                        newMessages.addAll(toolMessages)
                    }
                    is ToolExecutionResult.AwaitingApproval -> {
                        return Result.success(ProcessResult.Pending(exec.runId))
                    }
                }
            }
        }
        loopCount++
    }

    // Достигнут лимит итераций — сохраняем накопленное
    val extendedSnapshot = snapshot.copy(messages = newMessages.toAContextMessages())
    strategy.afterResponse(config, responseText = "", contextRepository, extendedSnapshot)
    return Result.success(ProcessResult.Success(AIAgentResult(responseText = "", ...)))
}
```

---

### 9. HitlSession (что хранить)

```kotlin
data class HitlSession(
    val runId: String,                                         // UUID
    val agentId: Long,
    val prompt: AContextMessage,                               // для buildLlmContext() при resume
    val loopMessages: List<ModelRequest.Message>,              // newMessages накопленные с начала process()
                                                               // включая assistantMessage с tool_calls
    val pendingToolCalls: List<ModelResult.Success.ToolCall>,
    val decisions: Map<String, ToolCallDecision> = emptyMap(),
    val status: HitlStatus = HitlStatus.AWAITING_APPROVAL,
    val createdAt: Long = System.currentTimeMillis()
)
```

**`loopMessages`** — полный delta с момента вызова `process()`: prompt + все предыдущие пары (assistantMsg + toolResults) + текущий assistantMsg с pending tool_calls. Работает для любого числа итераций HITL.

**Что НЕ хранится:**
- DB history — берётся через `strategy.process()` при resume
- Эфемерные memory/context — берутся через `memoryProvider.*`
- `modelSettings`, `systemPrompt`, `tools` — из `AIAgent.config`

**`ToolCallContext`** — реконструируется как `ToolCallContext(agentId = session.agentId)`.

**Детерминизм контекста при resume:** `buildLlmContext(session.prompt)` может дать немного другой эфемерный контекст (если LTM изменился). Это известное ограничение, приемлемое для in-app сценария.

---

### 10. Resume после HITL

```kotlin
suspend fun resumeWithDecisions(runId: String, onEvent: ...): Result<ProcessResult> {
    val session = hitlSessionManager.getSession(runId) ?: return Result.failure(SessionNotFoundError(runId))

    // Реконструировать базу (memory + DB history + context + prompt)
    val (llmMessages, _, snapshot) = buildLlmContext(session.prompt)

    // Дописать все накопленные сообщения цикла (включая assistantMsg с tool_calls)
    llmMessages.addAll(session.loopMessages)
    val newMessages = session.loopMessages.toMutableList()

    // Выполнить approved / отклонить rejected
    val toolResults = session.pendingToolCalls.map { call ->
        when (session.decisions[call.id]) {
            ToolCallDecision.APPROVED -> {
                val result = toolProvider.executeToolCall(call, ToolCallContext(agentId = session.agentId))
                ToolResult(call.id, result.getOrElse { "Error: ${it.message}" }, result.isFailure)
            }
            ToolCallDecision.REJECTED, null ->
                ToolResult(call.id, "Rejected by user", isError = true)
        }
    }

    val toolMessages = toolResults.toModelRequestMessages()
    llmMessages.addAll(toolMessages)
    newMessages.addAll(toolMessages)

    hitlSessionManager.closeSession(runId)
    return runToolLoop(runId, llmMessages, newMessages, snapshot, onEvent)
}
```

---

### 11. HitlSessionManager — хранение

**Реализация:** in-memory (`ConcurrentHashMap`). При уничтожении процесса сессия теряется — это известное ограничение. Пользователь должен повторно отправить сообщение. Persistence через Room — в будущем.

**Timeout:** сессия автоматически инвалидируется через 24 часа.

```kotlin
interface HitlSessionManager {
    fun createSession(session: HitlSession)
    fun getSession(runId: String): HitlSession?
    fun hasActiveSession(agentId: Long): Boolean
    fun recordDecision(runId: String, toolCallId: String, decision: ToolCallDecision): RecordDecisionResult
    fun getAllDecisions(runId: String): Map<String, ToolCallDecision>
    fun closeSession(runId: String)
}

sealed class RecordDecisionResult {
    object AllComplete : RecordDecisionResult()
    object AwaitingMore : RecordDecisionResult()
    data class Error(val message: String) : RecordDecisionResult()
}
```

---

### 12. ProcessResult

```kotlin
sealed class ProcessResult {
    data class Success(val result: AIAgentResult) : ProcessResult()
    data class Pending(val runId: String) : ProcessResult()
}
```

`AIAgent.process()` возвращает `Result<ProcessResult>`.

---

### 13. AgentConfig

```kotlin
data class AgentConfig(
    // ... existing fields ...
    val hitlEnabled: Boolean = false
)
```

`hitlEnabled = true` → DI инжектирует `HitlToolExecutor` вместо `AutoToolExecutor`. Или: фабрика на уровне AIAgent выбирает реализацию.

---

## Зависимости

```
AIAgent
  → ToolCallOrchestrator   (один шаг LLM)
  → ToolExecutor           (submit tool calls → Completed | AwaitingApproval)
  → HitlSessionManager     (только для resume: getSession, closeSession)
  → ToolProvider           (только в resumeWithDecisions для выполнения одобренных tools)
  → ContextStrategy, MemoryProvider, AgentContextRepository  (без изменений)

ToolExecutor (HitlToolExecutor)
  → HitlSessionManager

ToolExecutor (AutoToolExecutor)
  → ToolProvider

ToolCallOrchestrator
  → LlmRequestUseCase
```

**Примечание:** `ToolProvider` остаётся в `AIAgent` только для `resumeWithDecisions`. `toolProvider` уходит из основного пути `process()`.

---

## Изменяемые файлы

| Файл | Изменение |
|------|-----------|
| `domain/tools/ToolCallOrchestrator.kt` | Упростить: один метод, `OrchestratorRequest` |
| `domain/tools/OrchestratorRequest.kt` | NEW |
| `domain/tools/OrchestratorResult.kt` | NEW — sealed: Completed / PendingApproval |
| `domain/tools/ToolExecutor.kt` | NEW — interface + ToolExecutionResult |
| `domain/tools/impl/ToolCallOrchestratorImpl.kt` | Переписать (~30-40 строк) |
| `domain/tools/impl/AutoToolExecutor.kt` | NEW |
| `domain/tools/hitl/HitlToolExecutor.kt` | NEW |
| `domain/tools/hitl/HitlSession.kt` | NEW |
| `domain/tools/hitl/HitlSessionManager.kt` | NEW — interface + in-memory impl |
| `domain/model/ProcessResult.kt` | NEW — Success / Pending |
| `domain/workers/base/WorkerEvent.kt` | Add: ApprovalRequired, ApprovalDecided |
| `domain/model/AgentConfig.kt` | Add: `hitlEnabled: Boolean = false` |
| `domain/AIAgent.kt` | Добавить цикл, resumeWithDecisions, runToolLoop |
| `di/AgentCoreFeatureModule.kt` | Bind ToolExecutor, HitlSessionManager |

---

## Порядок реализации

**Шаг 1 — Рефакторинг оркестратора**
`OrchestratorRequest`, `OrchestratorResult`, новый `ToolCallOrchestratorImpl` (~30 строк), `AutoToolExecutor`, цикл в `AIAgent`. `ProcessResult` без `Pending`. Всё работает как раньше, без HITL.

**Шаг 2 — HITL слой**
`HitlToolExecutor`, `HitlSessionManager`, `HitlSession`, `resumeWithDecisions` в AIAgent, `ProcessResult.Pending`, новые `WorkerEvent`. AIAgent не меняется структурно — только DI и новый метод `resumeWithDecisions`.
