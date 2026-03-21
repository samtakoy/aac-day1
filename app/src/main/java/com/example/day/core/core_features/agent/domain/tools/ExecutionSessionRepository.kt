package com.example.day.core.core_features.agent.domain.tools

interface ExecutionSessionRepository {
    suspend fun get(runId: String): ExecutionSession?
    suspend fun save(session: ExecutionSession)
    suspend fun remove(runId: String)
}
