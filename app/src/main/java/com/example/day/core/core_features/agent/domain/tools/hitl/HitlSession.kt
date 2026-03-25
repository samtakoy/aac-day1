package com.example.day.core.core_features.agent.domain.tools.hitl

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelResult

enum class HitlStatus { AWAITING_APPROVAL, COMPLETED, CANCELLED }

enum class ToolCallDecision { APPROVED, REJECTED }

data class HitlSession(
    val runId: String,
    val agentId: Long,
    val prompt: AContextMessage,
    val loopMessages: List<ModelRequest.Message>,
    val pendingToolCalls: List<ModelResult.Success.ToolCall>,
    val decisions: Map<String, ToolCallDecision> = emptyMap(),
    val status: HitlStatus = HitlStatus.AWAITING_APPROVAL,
    val createdAt: Long = System.currentTimeMillis()
)
