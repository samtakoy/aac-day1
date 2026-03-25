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
import com.example.day.core.core_features.agent.domain.tools.hitl.HitlSessionManager
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
    private val hitlSessionManager = mockk<HitlSessionManager>(relaxed = true)

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
        toolExecutor = toolExecutor,
        hitlSessionManager = hitlSessionManager
    )

    private fun setupBase() {
        coEvery { memoryProvider.getMemoryContext() } returns emptyList()
        coEvery { memoryProvider.appendUserPrompt(any()) } returns mockk(relaxed = true)
        coEvery { strategy.process(any(), any()) } returns ContextStrategyResult(messages = emptyList())
        coEvery { strategy.afterResponse(any(), any(), any(), any()) } returns mockk(relaxed = true)
        coEvery { toolProvider.getTools(any()) } returns emptyList()
        coEvery { hitlSessionManager.hasActiveSession(any()) } returns false
    }

    @Test
    fun `process returns success on Completed`() = runTest {
        setupBase()
        coEvery { orchestrator.execute(any(), any()) } returns Result.success(
            OrchestratorResult.Completed("Hello!", ModelRequest.Message(ModelRequest.Role.Assistant, "Hello!"))
        )

        val result = agent.process(AContextMessage(AContextMessage.Role.USER, "Hi"), null)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow() is ProcessResult.Success)
        assertEquals("Hello!", (result.getOrThrow() as ProcessResult.Success).result.responseText)
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
