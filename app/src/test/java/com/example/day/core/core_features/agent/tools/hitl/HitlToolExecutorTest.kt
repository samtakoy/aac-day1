package com.example.day.core.core_features.agent.tools.hitl

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.tools.ToolCallContext
import com.example.day.core.core_features.agent.domain.tools.ToolExecutionResult
import com.example.day.core.core_features.agent.domain.tools.hitl.HitlSession
import com.example.day.core.core_features.agent.domain.tools.hitl.HitlSessionManager
import com.example.day.core.core_features.agent.domain.tools.hitl.HitlToolExecutor
import com.example.day.core.core_features.llm.domain.model.ModelResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HitlToolExecutorTest {

    private val sessionManager = mockk<HitlSessionManager>(relaxed = true)
    private val executor = HitlToolExecutor(sessionManager)
    private val context = ToolCallContext(agentId = 1L)

    private fun makeToolCall(id: String) = ModelResult.Success.ToolCall(
        id = id, type = "function",
        function = ModelResult.Success.FunctionCall("tool_$id", "{}")
    )

    private val testPrompt = AContextMessage(AContextMessage.Role.USER, "test")

    @Test
    fun `submit creates session and returns AwaitingApproval`() = runTest {
        val toolCalls = listOf(makeToolCall("c1"), makeToolCall("c2"))

        val result = executor.submit(
            runId = "run1",
            toolCalls = toolCalls,
            prompt = testPrompt,
            loopMessages = emptyList(),
            context = context,
            onEvent = null
        )

        assertTrue(result is ToolExecutionResult.AwaitingApproval)
        assertEquals("run1", (result as ToolExecutionResult.AwaitingApproval).runId)
    }

    @Test
    fun `submit stores all pending tool calls in session`() = runTest {
        val sessionSlot = slot<HitlSession>()
        every { sessionManager.createSession(capture(sessionSlot)) } returns Unit

        executor.submit(
            runId = "run1",
            toolCalls = listOf(makeToolCall("c1"), makeToolCall("c2")),
            prompt = testPrompt,
            loopMessages = emptyList(),
            context = context,
            onEvent = null
        )

        assertEquals(2, sessionSlot.captured.pendingToolCalls.size)
        assertEquals("run1", sessionSlot.captured.runId)
        assertEquals(1L, sessionSlot.captured.agentId)
    }

    @Test
    fun `submit emits ApprovalRequired event for each tool call`() = runTest {
        val events = mutableListOf<Any>()

        executor.submit(
            runId = "run1",
            toolCalls = listOf(makeToolCall("c1"), makeToolCall("c2")),
            prompt = testPrompt,
            loopMessages = emptyList(),
            context = context,
            onEvent = { events.add(it) }
        )

        assertEquals(2, events.filterIsInstance<com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent.ApprovalRequired>().size)
    }
}
