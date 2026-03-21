package com.example.day.core.core_features.agent.data

import com.example.day.core.core_features.agent.domain.tools.ExecutionSession
import com.example.day.core.core_features.agent.domain.tools.ExecutionSessionManager
import com.example.day.core.core_features.agent.domain.tools.ExecutionSessionRepository
import com.example.day.core.core_features.agent.domain.tools.LlmExecutionRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ExecutionSessionManagerImpl @Inject constructor(
    private val repository: ExecutionSessionRepository
) : ExecutionSessionManager {

    override suspend fun ensure(
        runId: String,
        request: LlmExecutionRequest
    ): ExecutionSession {
        val existing = repository.get(runId)
        if (existing != null) {
            return existing
        }

        val session = ExecutionSession(
            runId = runId,
            requestSnapshot = request
        )
        repository.save(session)
        return session
    }

    override suspend fun get(runId: String): ExecutionSession? {
        return repository.get(runId)
    }

    override suspend fun update(session: ExecutionSession) {
        repository.save(session)
    }

    override suspend fun close(runId: String) {
        repository.remove(runId)
    }
}
