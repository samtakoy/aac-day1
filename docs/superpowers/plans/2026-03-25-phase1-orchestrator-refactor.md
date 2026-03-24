# Orchestrator Refactor — Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Устранить двойной трекинг массивов в оркестраторе, перенести цикл tool calling в AIAgent, ввести ToolExecutor как единую точку выполнения инструментов — без изменения внешнего API.

**Architecture:** `ToolCallOrchestrator` становится stateless single-step (один LLM вызов → `OrchestratorResult`). Цикл переезжает в `AIAgent` через `runToolLoop` + `buildLlmContext`. `AutoToolExecutor` заменяет прямые вызовы `toolProvider` из оркестратора. `llmProvider` уходит из `AIAgent` — делегируется оркестратору.

**Tech Stack:** Kotlin, Dagger, Coroutines. Tests: JUnit4 + mockk + kotlinx-coroutines-test (добавляются в Task 1).

---

## File Map

| Действие | Файл |
|---------|------|
| CREATE | `domain/tools/OrchestratorRequest.kt` |
| CREATE | `domain/tools/OrchestratorResult.kt` |
| CREATE | `domain/tools/ToolExecutor.kt` — interface + `ToolExecutionResult` + `ToolResult` |
| CREATE | `domain/tools/impl/AutoToolExecutor.kt` |
| REWRITE | `domain/tools/impl/ToolCallOrchestratorImpl.kt` |
| MODIFY | `domain/tools/ToolCallOrchestrator.kt` |
| MODIFY | `domain/AIAgent.kt` — добавить `buildLlmContext`, `runToolLoop`; убрать `llmProvider` |
| MODIFY | `domain/AIAgentFactory.kt` — убрать `llmProvider`, добавить `ToolExecutor` |
| MODIFY | `di/AgentCoreFeatureModule.kt` |
| DELETE | `domain/tools/ToolCallingResult.kt` |
| DELETE | `domain/tools/ToolCallSession.kt` — `ToolResult` переезжает в `ToolExecutor.kt` |
| CREATE | `test/.../tools/ToolCallOrchestratorImplTest.kt` |
| CREATE | `test/.../tools/AutoToolExecutorTest.kt` |
| CREATE | `test/.../AIAgentLoopTest.kt` |

Базовый путь: `app/src/main/java/com/example/day/core/core_features/agent/`
Тесты: `app/src/test/java/com/example/day/core/core_features/agent/`

---

### Task 1: Добавить тестовые зависимости

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Добавить в libs.versions.toml**

```toml
# [versions]
mockk = "1.13.10"

# [libraries]
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
```

- [ ] **Step 2: Добавить в app/build.gradle.kts**

```kotlin
testImplementation(libs.mockk)
testImplementation(libs.kotlinx.coroutines.test)
```

- [ ] **Step 3: Sync Gradle**

```bash
./gradlew :app:dependencies 2>&1 | tail -5
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "build: add mockk and coroutines-test test dependencies"
```

---

### Task 2: Создать data-классы оркестратора

**Files:**
- Create: `domain/tools/OrchestratorRequest.kt`
- Create: `domain/tools/OrchestratorResult.kt`

- [ ] **Step 1: Создать OrchestratorRequest.kt**

```kotlin
package com.example.day.core.core_features.agent.domain.tools

import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelSettings

data class OrchestratorRequest(
    val messages: List<ModelRequest.Message>,
    val systemPrompt: String?,
    val modelSettings: ModelSettings,
    val tools: List<ModelRequest.Tool>
)
```

- [ ] **Step 2: Создать OrchestratorResult.kt**

```kotlin
package com.example.day.core.core_features.agent.domain.tools

import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelResult

sealed class OrchestratorResult {
    data class Completed(
        val responseText: String,
        val assistantMessage: ModelRequest.Message
    ) : OrchestratorResult()

    data class PendingApproval(
        val toolCalls: List<ModelResult.Success.ToolCall>,
        val assistantMessage: ModelRequest.Message
    ) : OrchestratorResult()
}
```

- [ ] **Step 3: Проверить компиляцию**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -5
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/day/core/core_features/agent/domain/tools/OrchestratorRequest.kt
git add app/src/main/java/com/example/day/core/core_features/agent/domain/tools/OrchestratorResult.kt
git commit -m "feat: add OrchestratorRequest and OrchestratorResult"
```

---

### Task 3: Создать ToolExecutor, удалить старые типы

**Files:**
- Create: `domain/tools/ToolExecutor.kt`
- Delete: `domain/tools/ToolCallSession.kt`
- Delete: `domain/tools/ToolCallingResult.kt`

`ToolResult` переезжает из `ToolCallSession.kt` в `ToolExecutor.kt`.

- [ ] **Step 1: Создать ToolExecutor.kt**

```kotlin
package com.example.day.core.core_features.agent.domain.tools

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelResult

data class ToolResult(
    val toolCallId: String,
    val content: String,
    val isError: Boolean
)

fun List<ToolResult>.toModelRequestMessages(): List<ModelRequest.Message> =
    map { ModelRequest.Message(role = ModelRequest.Role.Tool, content = it.content, toolCallId = it.toolCallId) }

sealed class ToolExecutionResult {
    data class Completed(val results: List<ToolResult>) : ToolExecutionResult()
    data class AwaitingApproval(val runId: String) : ToolExecutionResult()
}

interface ToolExecutor {
    suspend fun submit(
        runId: String,
        toolCalls: List<ModelResult.Success.ToolCall>,
        prompt: AContextMessage,
        loopMessages: List<ModelRequest.Message>,
        context: ToolCallContext,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ): ToolExecutionResult
}
```

- [ ] **Step 2: Удалить устаревшие файлы**

```bash
git rm app/src/main/java/com/example/day/core/core_features/agent/domain/tools/ToolCallSession.kt
git rm app/src/main/java/com/example/day/core/core_features/agent/domain/tools/ToolCallingResult.kt
```

- [ ] **Step 3: Проверить ошибки компиляции**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | grep "error:" | head -20
```

Ожидаемые ошибки только в `ToolCallOrchestratorImpl.kt` и `AIAgent.kt` — ссылки на удалённые типы. Исправляются в следующих задачах. **Других ошибок быть не должно.**

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/day/core/core_features/agent/domain/tools/ToolExecutor.kt
git commit -m "feat: add ToolExecutor interface, ToolResult; remove ToolCallingResult and ToolCallSession"
```

---

### Task 4: Переписать ToolCallOrchestrator интерфейс и реализацию

**Files:**
- Modify: `domain/tools/ToolCallOrchestrator.kt`
- Rewrite: `domain/tools/impl/ToolCallOrchestratorImpl.kt`
- Create: `test/.../tools/ToolCallOrchestratorImplTest.kt`

Новый оркестратор вызывает `llmProvider.exec()` напрямую (без `askLlm` из workers.base — нарушение слоёв). Эмитирует `RequestStart/RequestSuccess/RequestError` инлайн.

- [ ] **Step 1: Написать тест (красный)**

Создай `app/src/test/java/com/example/day/core/core_features/agent/tools/ToolCallOrchestratorImplTest.kt`:

```kotlin
package com.example.day.core.core_features.agent.tools

import com.example.day.core.core_features.agent.domain.tools.OrchestratorRequest
import com.example.day.core.core_features.agent.domain.tools.OrchestratorResult
import com.example.day.core.core_features.agent.domain.tools.impl.ToolCallOrchestratorImpl
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelResult
import com.example.day.core.core_features.llm.domain.model.ModelSettings
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolCallOrchestratorImplTest {

    private val llmProvider = mockk<LlmRequestUseCase>()
    private val orchestrator = ToolCallOrchestratorImpl(llmProvider)

    private fun makeRequest() = OrchestratorRequest(
        messages = listOf(ModelRequest.Message(ModelRequest.Role.User, "hello")),
        systemPrompt = null,
        modelSettings = ModelSettings(name = "test-model"),
        tools = emptyList()
    )

    private fun makeLlmSuccess(
        content: String,
        toolCalls: List<ModelResult.Success.ToolCall> = emptyList()
    ) = ModelResult.Success(
        id = "id",
        model = "test-model",
        choices = persistentListOf(
            ModelResult.Success.Choice(
                message = ModelResult.Success.Message(
                    role = "assistant",
                    content = content,
                    reasoning = null,
                    toolCalls = toolCalls.toPersistentList()
                ),
                finishReason = "stop"
            )
        )
    )

    // llmProvider.exec has 5 params: modelSettings, systemPrompt, messages, prompt, tools
    private fun stubExec(result: ModelResult.Success) {
        coEvery { llmProvider.exec(any(), any(), any(), any(), any()) } returns Result.success(result)
    }

    @Test
    fun `returns Completed when LLM has no tool calls`() = runTest {
        stubExec(makeLlmSuccess("Hello world"))

        val result = orchestrator.execute(makeRequest(), onEvent = null).getOrThrow()

        assertTrue(result is OrchestratorResult.Completed)
        assertEquals("Hello world", (result as OrchestratorResult.Completed).responseText)
    }

    @Test
    fun `returns PendingApproval when LLM requests tool calls`() = runTest {
        val toolCall = ModelResult.Success.ToolCall(
            id = "call_1", type = "function",
            function = ModelResult.Success.FunctionCall("get_weather", "{}")
        )
        stubExec(makeLlmSuccess("", listOf(toolCall)))

        val result = orchestrator.execute(makeRequest(), onEvent = null).getOrThrow()

        assertTrue(result is OrchestratorResult.PendingApproval)
        assertEquals(1, (result as OrchestratorResult.PendingApproval).toolCalls.size)
    }

    @Test
    fun `propagates LLM failure`() = runTest {
        coEvery { llmProvider.exec(any(), any(), any(), any(), any()) } returns
            Result.failure(RuntimeException("LLM down"))

        assertTrue(orchestrator.execute(makeRequest(), null).isFailure)
    }
}
```

Примечание: `toolCalls.toPersistentList()` — импорт из `kotlinx.collections.immutable.toPersistentList`. Если список пустой, передавай `null` вместо пустого `persistentListOf()` т.к. сервер не возвращает пустой массив.

- [ ] **Step 2: Запустить тест (красный)**

```bash
./gradlew :app:testDebugUnitTest --tests "*.ToolCallOrchestratorImplTest" 2>&1 | tail -10
```

Expected: FAILED

- [ ] **Step 3: Обновить ToolCallOrchestrator.kt**

```kotlin
package com.example.day.core.core_features.agent.domain.tools

import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent

interface ToolCallOrchestrator {
    suspend fun execute(
        request: OrchestratorRequest,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ): Result<OrchestratorResult>
}
```

- [ ] **Step 4: Переписать ToolCallOrchestratorImpl.kt**

```kotlin
package com.example.day.core.core_features.agent.domain.tools.impl

import android.util.Log
import com.example.day.core.core_features.agent.domain.tools.OrchestratorRequest
import com.example.day.core.core_features.agent.domain.tools.OrchestratorResult
import com.example.day.core.core_features.agent.domain.tools.ToolCallOrchestrator
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.getContent
import javax.inject.Inject

class ToolCallOrchestratorImpl @Inject constructor(
    private val llmProvider: LlmRequestUseCase
) : ToolCallOrchestrator {

    companion object {
        private const val TAG = "ToolCallOrchestrator"
    }

    override suspend fun execute(
        request: OrchestratorRequest,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ): Result<OrchestratorResult> {
        onEvent?.invoke(WorkerEvent.RequestStart)

        val llmResult = llmProvider.exec(
            modelSettings = request.modelSettings,
            systemPrompt = request.systemPrompt,
            messages = request.messages,
            prompt = null,
            tools = request.tools.ifEmpty { null }
        ).onSuccess { onEvent?.invoke(WorkerEvent.RequestSuccess(it)) }
         .onFailure { onEvent?.invoke(WorkerEvent.RequestError(it.message ?: "error")) }
         .getOrElse { return Result.failure(it) }

        val choice = llmResult.choices.firstOrNull()
        val toolCalls = choice?.message?.toolCalls

        if (toolCalls.isNullOrEmpty()) {
            val responseText = llmResult.getContent()
            Log.d(TAG, "No tool calls — final response")
            return Result.success(
                OrchestratorResult.Completed(
                    responseText = responseText,
                    assistantMessage = ModelRequest.Message(ModelRequest.Role.Assistant, responseText)
                )
            )
        }

        Log.d(TAG, "${toolCalls.size} tool calls requested")
        val assistantMessage = ModelRequest.Message(
            role = ModelRequest.Role.Assistant,
            content = choice.message.content.orEmpty(),
            toolCalls = toolCalls.map { call ->
                ModelRequest.ToolCall(
                    id = call.id, type = call.type,
                    function = ModelRequest.FunctionCall(call.function.name, call.function.arguments)
                )
            }
        )
        return Result.success(OrchestratorResult.PendingApproval(toolCalls, assistantMessage))
    }
}
```

- [ ] **Step 5: Запустить тест (зелёный)**

```bash
./gradlew :app:testDebugUnitTest --tests "*.ToolCallOrchestratorImplTest" 2>&1 | tail -10
```

Expected: 3 tests passed

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/day/core/core_features/agent/domain/tools/ToolCallOrchestrator.kt
git add app/src/main/java/com/example/day/core/core_features/agent/domain/tools/impl/ToolCallOrchestratorImpl.kt
git add app/src/test/java/com/example/day/core/core_features/agent/tools/ToolCallOrchestratorImplTest.kt
git commit -m "refactor: ToolCallOrchestratorImpl — single LLM step, no loop, no tool execution"
```

---

### Task 5: Создать AutoToolExecutor

**Files:**
- Create: `domain/tools/impl/AutoToolExecutor.kt`
- Create: `test/.../tools/AutoToolExecutorTest.kt`

- [ ] **Step 1: Написать тест (красный)**

Создай `app/src/test/java/com/example/day/core/core_features/agent/tools/AutoToolExecutorTest.kt`:

```kotlin
package com.example.day.core.core_features.agent.tools

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.tools.ToolCallContext
import com.example.day.core.core_features.agent.domain.tools.ToolExecutionResult
import com.example.day.core.core_features.agent.domain.tools.ToolProvider
import com.example.day.core.core_features.agent.domain.tools.impl.AutoToolExecutor
import com.example.day.core.core_features.llm.domain.model.ModelResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoToolExecutorTest {

    private val toolProvider = mockk<ToolProvider>()
    private val executor = AutoToolExecutor(toolProvider)
    private val context = ToolCallContext(agentId = 1L)
    private val prompt = AContextMessage(AContextMessage.Role.USER, "test")

    private fun makeToolCall(id: String) = ModelResult.Success.ToolCall(
        id = id, type = "function",
        function = ModelResult.Success.FunctionCall("tool_$id", "{}")
    )

    @Test
    fun `executes all tool calls and returns Completed`() = runTest {
        coEvery { toolProvider.executeToolCall(any(), any()) } returns Result.success("result")

        val result = executor.submit("run1", listOf(makeToolCall("c1"), makeToolCall("c2")), prompt, emptyList(), context, null)

        assertTrue(result is ToolExecutionResult.Completed)
        assertEquals(2, (result as ToolExecutionResult.Completed).results.size)
    }

    @Test
    fun `marks result as error when tool fails`() = runTest {
        coEvery { toolProvider.executeToolCall(any(), any()) } returns Result.failure(RuntimeException("boom"))

        val result = executor.submit("run1", listOf(makeToolCall("c1")), prompt, emptyList(), context, null)
            as ToolExecutionResult.Completed

        assertTrue(result.results.first().isError)
    }

    @Test
    fun `never returns AwaitingApproval`() = runTest {
        coEvery { toolProvider.executeToolCall(any(), any()) } returns Result.success("ok")

        val result = executor.submit("run1", listOf(makeToolCall("c1")), prompt, emptyList(), context, null)

        assertFalse(result is ToolExecutionResult.AwaitingApproval)
    }
}
```

- [ ] **Step 2: Запустить тест (красный)**

```bash
./gradlew :app:testDebugUnitTest --tests "*.AutoToolExecutorTest" 2>&1 | tail -10
```

- [ ] **Step 3: Создать AutoToolExecutor.kt**

```kotlin
package com.example.day.core.core_features.agent.domain.tools.impl

import android.util.Log
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.tools.ToolCallContext
import com.example.day.core.core_features.agent.domain.tools.ToolCallingConstants
import com.example.day.core.core_features.agent.domain.tools.ToolExecutionResult
import com.example.day.core.core_features.agent.domain.tools.ToolExecutor
import com.example.day.core.core_features.agent.domain.tools.ToolProvider
import com.example.day.core.core_features.agent.domain.tools.ToolResult
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelResult
import javax.inject.Inject

class AutoToolExecutor @Inject constructor(
    private val toolProvider: ToolProvider
) : ToolExecutor {

    companion object { private const val TAG = "AutoToolExecutor" }

    override suspend fun submit(
        runId: String,
        toolCalls: List<ModelResult.Success.ToolCall>,
        prompt: AContextMessage,
        loopMessages: List<ModelRequest.Message>,
        context: ToolCallContext,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ): ToolExecutionResult {
        val results = toolCalls.map { call ->
            onEvent?.invoke(WorkerEvent.ToolCallStarted(call.id, call.function.name, call.function.arguments))
            Log.d(TAG, "Executing: ${call.function.name}")

            val toolResult = toolProvider.executeToolCall(call, context)
            val content = toolResult.getOrElse { err ->
                "${ToolCallingConstants.MCP_TOOL_ERROR_PREFIX}: ${err.message ?: ToolCallingConstants.UNKNOWN_TOOL_ERROR}"
            }

            onEvent?.invoke(WorkerEvent.ToolCallFinished(call.id, call.function.name, content, toolResult.isFailure))
            ToolResult(toolCallId = call.id, content = content, isError = toolResult.isFailure)
        }
        return ToolExecutionResult.Completed(results)
    }
}
```

- [ ] **Step 4: Запустить тест (зелёный)**

```bash
./gradlew :app:testDebugUnitTest --tests "*.AutoToolExecutorTest" 2>&1 | tail -10
```

Expected: 3 tests passed

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/day/core/core_features/agent/domain/tools/impl/AutoToolExecutor.kt
git add app/src/test/java/com/example/day/core/core_features/agent/tools/AutoToolExecutorTest.kt
git commit -m "feat: AutoToolExecutor — immediate tool execution"
```

---

### Task 6: Переписать AIAgent

**Files:**
- Modify: `domain/AIAgent.kt`
- Create: `test/.../AIAgentLoopTest.kt`

`AIAgent` теряет `llmProvider` (делегируется оркестратору) и получает `toolExecutor`. Метод `process()` сохраняет return type `Result<AIAgentResult>` — внешний API не меняется. Поле `hitlSessionManager` будет добавлено в Phase 2; сейчас добавляем заглушку через параметр с дефолтом.

- [ ] **Step 1: Написать тест (красный)**

Создай `app/src/test/java/com/example/day/core/core_features/agent/AIAgentLoopTest.kt`:

```kotlin
package com.example.day.core.core_features.agent

import com.example.day.core.core_features.agent.domain.AIAgent
import com.example.day.core.core_features.agent.domain.AgentContextRepository
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.AgentConfig
import com.example.day.core.core_features.agent.domain.strategy.ContextStrategy
import com.example.day.core.core_features.agent.domain.strategy.ContextStrategyResult
import com.example.day.core.core_features.agent.domain.strategy.CtxStrategyType
import com.example.day.core.core_features.agent.domain.tools.OrchestratorResult
import com.example.day.core.core_features.agent.domain.tools.ToolCallOrchestrator
import com.example.day.core.core_features.agent.domain.tools.ToolExecutionResult
import com.example.day.core.core_features.agent.domain.tools.ToolExecutor
import com.example.day.core.core_features.agent.domain.tools.ToolProvider
import com.example.day.core.core_features.agent.domain.tools.ToolResult
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelResult
import com.example.day.core.core_features.llm.domain.model.ModelSettings
import com.example.day.core.core_features.memory.domain.provider.base.MemoryProvider
import com.example.day.core.core_features.memory.domain.provider.base.MemoryType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AIAgentLoopTest {

    private val orchestrator = mockk<ToolCallOrchestrator>()
    private val toolExecutor = mockk<ToolExecutor>()
    private val memoryProvider = mockk<MemoryProvider>()
    private val strategy = mockk<ContextStrategy>()
    private val contextRepository = mockk<AgentContextRepository>()
    private val toolProvider = mockk<ToolProvider>()

    private val config = AgentConfig(
        id = 1L,
        systemName = "test-agent",
        title = "Test Agent",
        chatUserId = 0L,
        isCommon = false,
        modelSettings = ModelSettings(name = "test-model"),
        systemPrompt = "",
        contextStrategyType = CtxStrategyType.FULL_CONTEXT,
        memoryTypes = emptyList()
    )

    private val agent = AIAgent(
        config = config,
        contextRepository = contextRepository,
        strategy = strategy,
        memoryProvider = memoryProvider,
        toolProvider = toolProvider,
        orchestrator = orchestrator,
        toolExecutor = toolExecutor
    )

    private fun setupBase() {
        coEvery { memoryProvider.getMemoryContext() } returns emptyList()
        coEvery { memoryProvider.appendUserPrompt(any()) } returns mockk(relaxed = true)
        coEvery { strategy.process(any(), any()) } returns ContextStrategyResult(messages = emptyList())
        coEvery { strategy.afterResponse(any(), any(), any(), any()) } returns mockk(relaxed = true)
        coEvery { toolProvider.getTools(any()) } returns emptyList()
    }

    @Test
    fun `process returns success on Completed`() = runTest {
        setupBase()
        coEvery { orchestrator.execute(any(), any()) } returns Result.success(
            OrchestratorResult.Completed("Hello!", ModelRequest.Message(ModelRequest.Role.Assistant, "Hello!"))
        )

        val result = agent.process(AContextMessage(AContextMessage.Role.USER, "Hi"), null)

        assertTrue(result.isSuccess)
        assertEquals("Hello!", result.getOrThrow().responseText)
    }

    @Test
    fun `process calls toolExecutor then continues loop`() = runTest {
        setupBase()
        val toolCall = ModelResult.Success.ToolCall("c1", "function", ModelResult.Success.FunctionCall("tool", "{}"))
        val assistantToolMsg = ModelRequest.Message(ModelRequest.Role.Assistant, "")
        val finalMsg = ModelRequest.Message(ModelRequest.Role.Assistant, "Done!")

        coEvery { orchestrator.execute(any(), any()) } returnsMany listOf(
            Result.success(OrchestratorResult.PendingApproval(listOf(toolCall), assistantToolMsg)),
            Result.success(OrchestratorResult.Completed("Done!", finalMsg))
        )
        coEvery { toolExecutor.submit(any(), any(), any(), any(), any(), any()) } returns
            ToolExecutionResult.Completed(listOf(ToolResult("c1", "result", false)))

        val result = agent.process(AContextMessage(AContextMessage.Role.USER, "Do it"), null)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { toolExecutor.submit(any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 2) { orchestrator.execute(any(), any()) }
    }

    @Test
    fun `process calls strategy afterResponse on success`() = runTest {
        setupBase()
        coEvery { orchestrator.execute(any(), any()) } returns Result.success(
            OrchestratorResult.Completed("Hi", ModelRequest.Message(ModelRequest.Role.Assistant, "Hi"))
        )

        agent.process(AContextMessage(AContextMessage.Role.USER, "test"), null)

        coVerify(exactly = 1) { strategy.afterResponse(any(), any(), any(), any()) }
    }
}
```

- [ ] **Step 2: Запустить тест (красный)**

```bash
./gradlew :app:testDebugUnitTest --tests "*.AIAgentLoopTest" 2>&1 | tail -15
```

- [ ] **Step 3: Переписать AIAgent.kt**

```kotlin
package com.example.day.core.core_features.agent.domain

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.AIAgentResult
import com.example.day.core.core_features.agent.domain.model.AgentConfig
import com.example.day.core.core_features.agent.domain.model.toModelRequestMessages
import com.example.day.core.core_features.agent.domain.strategy.ContextStrategy
import com.example.day.core.core_features.agent.domain.strategy.ContextStrategyResult
import com.example.day.core.core_features.agent.domain.tools.OrchestratorRequest
import com.example.day.core.core_features.agent.domain.tools.OrchestratorResult
import com.example.day.core.core_features.agent.domain.tools.ToolCallContext
import com.example.day.core.core_features.agent.domain.tools.ToolCallOrchestrator
import com.example.day.core.core_features.agent.domain.tools.ToolExecutionResult
import com.example.day.core.core_features.agent.domain.tools.ToolExecutor
import com.example.day.core.core_features.agent.domain.tools.ToolProvider
import com.example.day.core.core_features.agent.domain.tools.toModelRequestMessages
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.memory.domain.provider.base.MemoryProvider
import java.util.UUID

class AIAgent(
    val config: AgentConfig,
    private val contextRepository: AgentContextRepository,
    private val strategy: ContextStrategy,
    private val memoryProvider: MemoryProvider,
    private val toolProvider: ToolProvider,
    private val orchestrator: ToolCallOrchestrator,
    private val toolExecutor: ToolExecutor
) {
    companion object {
        private const val MAX_TOOL_LOOPS = 10
    }

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

        val newMessages = buildList {
            addAll(snapshot.messages.toModelRequestMessages())
            if (prompt.content.isNotBlank()) add(promptMessages.prompt.toModelRequestMessage())
        }.toMutableList()

        return LlmContext(llmMessages, newMessages, snapshot)
    }

    suspend fun process(
        prompt: AContextMessage,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ): Result<AIAgentResult> {
        val runId = UUID.randomUUID().toString()
        val (llmMessages, newMessages, snapshot) = buildLlmContext(prompt)
        return runToolLoop(runId, prompt, llmMessages, newMessages, snapshot, onEvent)
    }

    internal suspend fun runToolLoop(
        runId: String,
        prompt: AContextMessage,
        llmMessages: MutableList<ModelRequest.Message>,
        newMessages: MutableList<ModelRequest.Message>,
        snapshot: ContextStrategyResult,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ): Result<AIAgentResult> {
        var loopCount = 0

        while (loopCount < MAX_TOOL_LOOPS) {
            val request = OrchestratorRequest(
                messages = llmMessages.toList(),
                systemPrompt = config.systemPrompt,
                modelSettings = config.modelSettings,
                tools = toolProvider.getTools(config.id)
            )
            val result = orchestrator.execute(request, onEvent)
                .getOrElse { return Result.failure(it) }

            when (result) {
                is OrchestratorResult.Completed -> {
                    newMessages.add(result.assistantMessage)
                    val extendedSnapshot = snapshot.copy(messages = newMessages.toAContextMessages())
                    val strategyResult = strategy.afterResponse(
                        agent = config,
                        response = result.responseText,
                        store = contextRepository,
                        fullContext = extendedSnapshot
                    )
                    return Result.success(AIAgentResult(result.responseText, strategyResult.reportMessage, debugInfo = ""))
                }
                is OrchestratorResult.PendingApproval -> {
                    llmMessages.add(result.assistantMessage)
                    newMessages.add(result.assistantMessage)

                    when (val exec = toolExecutor.submit(
                        runId = runId,
                        toolCalls = result.toolCalls,
                        prompt = prompt,
                        loopMessages = newMessages.toList(),
                        context = ToolCallContext(agentId = config.id),
                        onEvent = onEvent
                    )) {
                        is ToolExecutionResult.Completed -> {
                            val toolMessages = exec.results.toModelRequestMessages()
                            llmMessages.addAll(toolMessages)
                            newMessages.addAll(toolMessages)
                        }
                        is ToolExecutionResult.AwaitingApproval -> {
                            // Phase 2: HITL — handled in subclass/override
                            return Result.failure(UnsupportedOperationException("HITL not implemented in Phase 1"))
                        }
                    }
                }
            }
            loopCount++
        }

        val extendedSnapshot = snapshot.copy(messages = newMessages.toAContextMessages())
        strategy.afterResponse(config, "", contextRepository, extendedSnapshot)
        return Result.success(AIAgentResult("", null, debugInfo = ""))
    }

    suspend fun getInfo(): String = strategy.getInfoReport(config, contextRepository)
    suspend fun getFullContext(): String = strategy.getFullContextReport(config, contextRepository)
    suspend fun setupParams(params: Map<String, String>): String =
        strategy.updateParams(config, params, contextRepository)

    private fun List<ModelRequest.Message>.toAContextMessages() = map { msg ->
        AContextMessage(
            role = when (msg.role) {
                ModelRequest.Role.System -> AContextMessage.Role.SYSTEM
                ModelRequest.Role.User -> AContextMessage.Role.USER
                ModelRequest.Role.Assistant -> AContextMessage.Role.ASSISTANT
                ModelRequest.Role.Tool -> AContextMessage.Role.TOOL
            },
            content = msg.content,
            toolCallId = msg.toolCallId,
            toolCalls = msg.toolCalls?.map { call ->
                AContextMessage.ToolCallRef(call.id, call.type, call.function.name, call.function.arguments)
            }
        )
    }
}
```

- [ ] **Step 4: Запустить тест (зелёный)**

```bash
./gradlew :app:testDebugUnitTest --tests "*.AIAgentLoopTest" 2>&1 | tail -15
```

Expected: 3 tests passed

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/day/core/core_features/agent/domain/AIAgent.kt
git add app/src/test/java/com/example/day/core/core_features/agent/AIAgentLoopTest.kt
git commit -m "refactor: AIAgent with buildLlmContext + runToolLoop, remove llmProvider dependency"
```

---

### Task 7: Обновить AIAgentFactory и DI

**Files:**
- Modify: `domain/AIAgentFactory.kt`
- Modify: `di/AgentCoreFeatureModule.kt`

- [ ] **Step 1: Проверить текущие ошибки компиляции**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | grep "error:" | head -20
```

Ожидаются ошибки в `AIAgentFactory` (старые параметры) и `AgentCoreFeatureModule` (оркестратор с `toolProvider`).

- [ ] **Step 2: Обновить AIAgentFactory.kt**

Убрать `llmProvider`, добавить `toolExecutor`:

```kotlin
class AIAgentFactory @Inject constructor(
    private val getOrCreateAgentUseCase: GetOrCreateAgentUseCase,
    private val strategyFactory: StrategyFactory,
    private val memoryProviderFactory: MemoryProviderFactory,
    private val contextRepository: AgentContextRepository,
    private val toolProvider: ToolProvider,
    private val toolCallOrchestrator: ToolCallOrchestrator,
    private val toolExecutor: ToolExecutor
) {
    suspend fun getOrCreate(
        systemName: String,
        chatId: Long,
        systemPrompt: String,
        defaultModel: () -> ModelSettings,
        defaultContext: () -> AContext,
        onCreateCallback: (suspend (Long) -> Unit)? = null
    ): AIAgent {
        val config = getOrCreateAgentUseCase(
            systemName = systemName,
            chatId = chatId,
            systemPrompt = systemPrompt,
            defaultModel = defaultModel,
            defaultContext = defaultContext,
            onCreateCallback = onCreateCallback
        )
        val strategy = strategyFactory.create(config.contextStrategyType)
        val memoryProvider = memoryProviderFactory.create(
            memoryTypes = config.memoryTypes,
            agentId = config.id
        )
        return AIAgent(
            config = config,
            contextRepository = contextRepository,
            strategy = strategy,
            memoryProvider = memoryProvider,
            toolProvider = toolProvider,
            orchestrator = toolCallOrchestrator,
            toolExecutor = toolExecutor
        )
    }
}
```

- [ ] **Step 3: Обновить AgentCoreFeatureModule.kt**

Добавить `bindsToolExecutor`, убрать `toolProvider` из провайдера оркестратора:

```kotlin
@Binds
@Singleton
fun bindsToolExecutor(impl: AutoToolExecutor): ToolExecutor

// В companion object:
@Provides
@Singleton
internal fun provideToolCallOrchestrator(
    llmRequestUseCase: LlmRequestUseCase
): ToolCallOrchestrator = ToolCallOrchestratorImpl(llmRequestUseCase)
// toolProvider убран — оркестратор инструменты не вызывает
```

- [ ] **Step 4: Полная компиляция**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Все тесты**

```bash
./gradlew :app:testDebugUnitTest 2>&1 | tail -10
```

- [ ] **Step 6: Сборка**

```bash
./gradlew :app:assembleDebug 2>&1 | tail -5
```

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/day/core/core_features/agent/domain/AIAgentFactory.kt
git add app/src/main/java/com/example/day/core/core_features/agent/di/AgentCoreFeatureModule.kt
git commit -m "refactor: wire ToolExecutor in factory and DI — Phase 1 complete"
```

---

## Следующий шаг

После завершения Phase 1 → реализовать HITL: `docs/superpowers/plans/2026-03-25-phase2-hitl.md`
