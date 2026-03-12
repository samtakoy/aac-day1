package com.example.day.core.core_features.agent.domain.tools

import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelResult

interface ToolProvider {
    suspend fun getTools(agentId: Long? = null): List<ModelRequest.Tool>
    suspend fun executeToolCall(
        toolCall: ModelResult.Success.ToolCall,
        context: ToolCallContext
    ): Result<String>
}
