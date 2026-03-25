package com.example.day.core.core_features.agent.tools.hitl

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.tools.hitl.HitlSessionManager
import com.example.day.core.core_features.agent.domain.tools.hitl.HitlSessionManagerImpl
import com.example.day.core.core_features.agent.domain.tools.hitl.HitlStatus
import com.example.day.core.core_features.agent.domain.tools.hitl.RecordDecisionResult
import com.example.day.core.core_features.agent.domain.tools.hitl.ToolCallDecision
import com.example.day.core.core_features.agent.domain.tools.hitl.HitlSession
import com.example.day.core.core_features.llm.domain.model.ModelResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HitlSessionManagerTest {

    private val manager: HitlSessionManager = HitlSessionManagerImpl()

    private fun makeSession(runId: String, agentId: Long = 1L) = HitlSession(
        runId = runId,
        agentId = agentId,
        prompt = AContextMessage(AContextMessage.Role.USER, "test"),
        loopMessages = emptyList(),
        pendingToolCalls = listOf(
            ModelResult.Success.ToolCall("c1", "function", ModelResult.Success.FunctionCall("tool", "{}"))
        )
    )

    @Test
    fun `createSession stores session retrievable by runId`() {
        val session = makeSession("run1")
        manager.createSession(session)
        assertNotNull(manager.getSession("run1"))
    }

    @Test
    fun `hasActiveSession returns true after create`() {
        manager.createSession(makeSession("run1", agentId = 42L))
        assertTrue(manager.hasActiveSession(agentId = 42L))
    }

    @Test
    fun `hasActiveSession returns false after closeSession`() {
        manager.createSession(makeSession("run1", agentId = 42L))
        manager.closeSession("run1")
        assertFalse(manager.hasActiveSession(agentId = 42L))
    }

    @Test
    fun `recordDecision returns AwaitingMore when not all decided`() {
        val session = makeSession("run1").copy(
            pendingToolCalls = listOf(
                ModelResult.Success.ToolCall("c1", "function", ModelResult.Success.FunctionCall("t1", "{}")),
                ModelResult.Success.ToolCall("c2", "function", ModelResult.Success.FunctionCall("t2", "{}"))
            )
        )
        manager.createSession(session)

        val result = manager.recordDecision("run1", "c1", ToolCallDecision.APPROVED)

        assertTrue(result is RecordDecisionResult.AwaitingMore)
    }

    @Test
    fun `recordDecision returns AllComplete when all decided`() {
        manager.createSession(makeSession("run1"))

        val result = manager.recordDecision("run1", "c1", ToolCallDecision.APPROVED)

        assertTrue(result is RecordDecisionResult.AllComplete)
    }

    @Test
    fun `getSession returns null for unknown runId`() {
        assertNull(manager.getSession("nonexistent"))
    }

    @Test
    fun `getAllDecisions returns all recorded decisions`() {
        manager.createSession(makeSession("run1"))
        manager.recordDecision("run1", "c1", ToolCallDecision.REJECTED)

        val decisions = manager.getAllDecisions("run1")
        assertEquals(ToolCallDecision.REJECTED, decisions["c1"])
    }
}
