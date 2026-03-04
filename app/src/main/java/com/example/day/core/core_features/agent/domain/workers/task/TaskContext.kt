package com.example.day.core.core_features.agent.domain.workers.task

import com.example.day.core.core_features.agent.domain.model.TaskMemoryKeys
import com.example.day.core.core_features.agent.domain.model.TaskState
import com.example.day.core.core_features.chat.domain.model.Chat

/**
 * Context for task state machine execution.
 * Holds all necessary data for current task state.
 */
data class TaskContext(
    val chat: Chat,
    val agentId: Long,
    val facts: Map<String, String>,
    val currentState: TaskState,
    val currentStep: Int = 1
) {
    // Init artifact helpers
    fun getInitTitle(): String? = facts[TaskMemoryKeys.INIT_TITLE]
    fun getInitDescription(): String? = facts[TaskMemoryKeys.INIT_DESCRIPTION]
    fun getInitGoal(): String? = facts[TaskMemoryKeys.INIT_GOAL]
    fun getInitExpert(): String? = facts[TaskMemoryKeys.INIT_EXPERT]

    // Planning artifact helpers
    fun getTotalStages(): Int = facts[TaskMemoryKeys.PLAN_TOTAL_STAGES]?.toIntOrNull() ?: 0

    // Current stage info helpers
    fun getCurrentStageName(): String? = facts[TaskMemoryKeys.planStageName(currentStep)]
    fun getCurrentStageDesc(): String? = facts[TaskMemoryKeys.planStageDesc(currentStep)]
    fun getCurrentStageExpert(): String? = facts[TaskMemoryKeys.planStageExpert(currentStep)]
    fun getCurrentStageStatus(): Int = facts[TaskMemoryKeys.planStageStatus(currentStep)]?.toIntOrNull() ?: 0

    // Execution artifact helpers
    fun getCurrentStageArtifact(): String? = facts[TaskMemoryKeys.planStageArtifact(currentStep)]
    fun getCurrentStageResult(): String? = facts[TaskMemoryKeys.execStageResult(currentStep)]

    // Verification artifact helpers
    fun getCurrentStageFeedback(): String? = facts[TaskMemoryKeys.verifStageFeedback(currentStep)]
    fun getCurrentStageScore(): Int? = facts[TaskMemoryKeys.verifStageScore(currentStep)]?.toIntOrNull()

    // State checks
    fun isLastStage(): Boolean = currentStep >= getTotalStages()
    fun hasFeedbackForCurrentStage(): Boolean = getCurrentStageFeedback() != null

    /**
     * Get all stage artifacts for final report.
     */
    fun getAllStageArtifacts(): List<StageArtifact> {
        val total = getTotalStages()
        return (1..total).mapNotNull { step ->
            val name = facts[TaskMemoryKeys.planStageName(step)] ?: return@mapNotNull null
            val desc = facts[TaskMemoryKeys.planStageDesc(step)] ?: ""
            val artifact = facts[TaskMemoryKeys.execStageResult(step)] ?: ""
            StageArtifact(step, name, desc, artifact)
        }
    }

    data class StageArtifact(
        val step: Int,
        val name: String,
        val description: String,
        val artifact: String
    )
}
