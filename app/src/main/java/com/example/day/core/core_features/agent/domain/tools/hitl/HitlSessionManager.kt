package com.example.day.core.core_features.agent.domain.tools.hitl

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

sealed class RecordDecisionResult {
    object AllComplete : RecordDecisionResult()
    object AwaitingMore : RecordDecisionResult()
    data class Error(val message: String) : RecordDecisionResult()
}

interface HitlSessionManager {
    fun createSession(session: HitlSession)
    fun getSession(runId: String): HitlSession?
    fun hasActiveSession(agentId: Long): Boolean
    fun recordDecision(runId: String, toolCallId: String, decision: ToolCallDecision): RecordDecisionResult
    fun getAllDecisions(runId: String): Map<String, ToolCallDecision>
    fun closeSession(runId: String)
}

@Singleton
class HitlSessionManagerImpl @Inject constructor() : HitlSessionManager {

    companion object {
        private const val SESSION_TIMEOUT_MS = 24 * 60 * 60 * 1000L
    }

    private val sessions = ConcurrentHashMap<String, HitlSession>()

    override fun createSession(session: HitlSession) {
        sessions[session.runId] = session
    }

    override fun getSession(runId: String): HitlSession? {
        val session = sessions[runId] ?: return null
        if (System.currentTimeMillis() - session.createdAt > SESSION_TIMEOUT_MS) {
            sessions.remove(runId)
            return null
        }
        return session
    }

    override fun hasActiveSession(agentId: Long): Boolean {
        return sessions.values.any { it.agentId == agentId && getSession(it.runId) != null }
    }

    override fun recordDecision(
        runId: String,
        toolCallId: String,
        decision: ToolCallDecision
    ): RecordDecisionResult {
        val session = getSession(runId) ?: return RecordDecisionResult.Error("Session $runId not found")
        val updated = session.copy(decisions = session.decisions + (toolCallId to decision))
        sessions[runId] = updated

        val allDecided = updated.pendingToolCalls.all { updated.decisions.containsKey(it.id) }
        return if (allDecided) RecordDecisionResult.AllComplete else RecordDecisionResult.AwaitingMore
    }

    override fun getAllDecisions(runId: String): Map<String, ToolCallDecision> {
        return getSession(runId)?.decisions ?: emptyMap()
    }

    override fun closeSession(runId: String) {
        sessions.remove(runId)
    }
}
