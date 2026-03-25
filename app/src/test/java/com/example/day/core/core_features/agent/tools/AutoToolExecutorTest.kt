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
