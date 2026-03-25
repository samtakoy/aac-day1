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
                    toolCalls = if (toolCalls.isEmpty()) null else toolCalls.toPersistentList()
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
