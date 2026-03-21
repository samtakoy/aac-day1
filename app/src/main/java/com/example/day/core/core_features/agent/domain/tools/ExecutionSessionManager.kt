package com.example.day.core.core_features.agent.domain.tools

interface ExecutionSessionManager {
    suspend fun ensure(runId: String, request: LlmExecutionRequest): ExecutionSession
    suspend fun get(runId: String): ExecutionSession?
    suspend fun update(session: ExecutionSession)
    suspend fun close(runId: String)
}
