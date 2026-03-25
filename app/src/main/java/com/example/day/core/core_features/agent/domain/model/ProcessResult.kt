package com.example.day.core.core_features.agent.domain.model

import com.example.day.core.core_features.agent.domain.model.AIAgentResult

sealed class ProcessResult {
    data class Success(val result: AIAgentResult) : ProcessResult()
    data class Pending(val runId: String) : ProcessResult()
}
