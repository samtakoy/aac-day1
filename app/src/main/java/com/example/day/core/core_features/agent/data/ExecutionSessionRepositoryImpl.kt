package com.example.day.core.core_features.agent.data

import com.example.day.core.core_features.agent.domain.tools.ExecutionSession
import com.example.day.core.core_features.agent.domain.tools.ExecutionSessionRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ExecutionSessionRepositoryImpl @Inject constructor() : ExecutionSessionRepository {

    private val sessions = ConcurrentHashMap<String, ExecutionSession>()

    override suspend fun get(runId: String): ExecutionSession? {
        return sessions[runId]
    }

    override suspend fun save(session: ExecutionSession) {
        // TODO: replace in-memory storage with Room-backed persistence to survive process death.
        sessions[session.runId] = session
    }

    override suspend fun remove(runId: String) {
        sessions.remove(runId)
    }
}
