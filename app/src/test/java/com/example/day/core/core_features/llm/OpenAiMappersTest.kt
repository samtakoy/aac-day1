package com.example.day.core.core_features.llm

import com.example.day.core.core_features.llm.data.remote.mappers.OpenAiModelRequestMapperImpl
import com.example.day.core.core_features.llm.data.remote.mappers.OpenAiModelResponseMapperImpl
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelResult
import com.example.day.shared.dto.ChatCompletionResponse
import com.example.day.shared.dto.Choice
import com.example.day.shared.dto.Message
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenAiMappersTest {

    private val requestMapper = OpenAiModelRequestMapperImpl()
    private val responseMapper = OpenAiModelResponseMapperImpl()

    @Test
    fun `request mapper maps model and messages`() {
        val request = ModelRequest(
            model = "llama3",
            messages = listOf(
                ModelRequest.Message(ModelRequest.Role.System, "You are helpful"),
                ModelRequest.Message(ModelRequest.Role.User, "Hello")
            ),
            responseFormat = ModelRequest.ResponseFormat.None,
            temperature = 0.7,
            isLocal = true
        )
        val dto = requestMapper.toDto(request)
        assertEquals("llama3", dto.model)
        assertEquals(2, dto.messages.size)
        assertEquals("system", dto.messages[0].role)
        assertEquals("You are helpful", dto.messages[0].content)
        assertEquals("user", dto.messages[1].role)
        assertEquals(0.7, dto.temperature)
    }

    @Test
    fun `response mapper maps assistant message to ModelResult Success`() {
        val response = ChatCompletionResponse(
            id = "resp-123",
            model = "llama3",
            choices = listOf(
                Choice(message = Message("assistant", "Hi there!"), finishReason = "stop")
            )
        )
        val result = responseMapper.toDomain(response)
        assertTrue(result is ModelResult.Success)
        val success = result as ModelResult.Success
        assertEquals("resp-123", success.id)
        assertEquals("Hi there!", success.choices.first().message.content)
    }

    @Test
    fun `response mapper returns RuntimeError when choices empty`() {
        val response = ChatCompletionResponse(id = "x", choices = emptyList())
        val result = responseMapper.toDomain(response)
        assertTrue(result is ModelResult.RuntimeError)
    }
}
