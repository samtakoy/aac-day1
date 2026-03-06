package com.example.day.core.core_features.agent.domain.workers.task.states

import com.example.day.core.core_features.agent.domain.model.TaskLlmResponse
import com.example.day.core.core_features.agent.domain.model.TaskMemoryKeys
import com.example.day.core.core_features.agent.domain.model.TaskState
import com.example.day.core.core_features.agent.domain.workers.task.TaskContext

/**
 * VERIFICATION state handler.
 * Quality control for current stage.
 */
class VerificationStateHandler : TaskStateHandler {
    override val state: TaskState = TaskState.VERIFICATION

    companion object {
        const val MIN_PASSING_SCORE = 8
    }

    override fun buildSystemPrompt(context: TaskContext): String {
        val step = context.currentStep
        val totalStages = context.getTotalStages()
        val stageName = context.getCurrentStageName() ?: "Текущий этап"
        val result = context.getCurrentStageResult() ?: ""
        val goal = context.getInitGoal() ?: ""

        return """
Ты — Senior QA & Architect.
Текущее состояние задачи: VERIFICATION.
Проверяемый этап $step из $totalStages: $stageName.
Результат этапа: $result
Общая цель задачи: $goal

Инструкции:
1. Оцени результат по шкале 1-10.
2. Если оценка < $MIN_PASSING_SCORE: Напиши конкретные замечания, что нужно доработать.
   -> В workflow_transition укажи "EXECUTION" (повтор того же шага).
   -> В memory_updates запиши verif:stage${step}_feedback.
3. Если оценка >= $MIN_PASSING_SCORE:
   -> Похвали за результат.
   -> Если это последний этап ($step == $totalStages): workflow_transition: "DONE".
   -> Если есть следующие этапы: workflow_transition: "EXECUTION" (увеличь workflow:current_step в memory_updates).

=== ПРОТОКОЛ ОТВЕТА ===
Формат ответа: Строго JSON.
{
    "thought": "рассуждения",
    "reply_to_user": "текст для пользователя",
    "memory_updates": {
        "verif:stage${step}_score": "оценка 1-10",
        "verif:stage${step}_feedback": "замечания или пусто",
        "plan:stage${step}_status": "1"
    },
    "workflow_transition": "EXECUTION/DONE/null"
}
""".trimIndent()
    }

    override suspend fun handleLegacy(
        context: TaskContext,
        userInput: String,
        llmResponse: TaskLlmResponse
    ): StateResult {
        val nextState = llmResponse.workflowTransition?.let {
            TaskState.fromString(it)
        }

        val memoryUpdates = llmResponse.memoryUpdates.toMutableMap()
        var newStep: Int? = null

        // Determine next step based on score
        when (nextState) {
            TaskState.EXECUTION -> {
                val score = memoryUpdates[TaskMemoryKeys.verifStageScore(context.currentStep)]?.toIntOrNull() ?: 0
                if (score >= MIN_PASSING_SCORE && !context.isLastStage()) {
                    // Move to next stage
                    newStep = context.currentStep + 1
                    memoryUpdates[TaskMemoryKeys.CURRENT_STEP] = newStep.toString()
                    memoryUpdates[TaskMemoryKeys.PLAN_CURRENT_STAGE] = newStep.toString()
                    // Set previous stage status to DONE (2)
                    memoryUpdates[TaskMemoryKeys.planStageStatus(context.currentStep)] = "2"
                }
                // If score < MIN_PASSING_SCORE - stay on current step for rework
            }
            TaskState.DONE -> {
                // Set current stage status to DONE
                memoryUpdates[TaskMemoryKeys.planStageStatus(context.currentStep)] = "2"
            }
            else -> { /* no transition */ }
        }

        return StateResult(
            replyToUser = llmResponse.replyToUser,
            nextState = nextState,
            memoryUpdates = memoryUpdates,
            newStep = newStep
        )
    }
}
