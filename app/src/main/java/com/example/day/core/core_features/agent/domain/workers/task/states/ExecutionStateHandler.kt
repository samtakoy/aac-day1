package com.example.day.core.core_features.agent.domain.workers.task.states

import com.example.day.core.core_features.agent.domain.model.TaskLlmResponse
import com.example.day.core.core_features.agent.domain.model.TaskMemoryKeys
import com.example.day.core.core_features.agent.domain.model.TaskState
import com.example.day.core.core_features.agent.domain.workers.task.TaskContext

/**
 * EXECUTION state handler.
 * Implements current stage with detailed solution.
 */
class ExecutionStateHandler : TaskStateHandler {
    override val state: TaskState = TaskState.EXECUTION

    override fun buildSystemPrompt(context: TaskContext): String {
        val step = context.currentStep
        val stageName = context.getCurrentStageName() ?: "Текущий этап"
        val stageDesc = context.getCurrentStageDesc() ?: ""
        val expert = context.getCurrentStageExpert() ?: "Expert"
        val feedback = context.getCurrentStageFeedback()

        val feedbackSection = if (feedback != null) {
            """

=== ЗАМЕЧАНИЯ ВАЛИДАТОРА ===
$feedback
"""
        } else ""

        return """
Ты — $expert.
Текущее состояние задачи: EXECUTION.
Этап $step: $stageName.
Описание этапа: $stageDesc.$feedbackSection

Инструкции:
1. Реши задачу текущего этапа максимально подробно.
2. Представь результат пользователю.
3. Спроси: "Переходим к проверке этого этапа?"

=== ПРОТОКОЛ ОТВЕТА ===
Формат ответа: Строго JSON.
{
    "thought": "рассуждения",
    "reply_to_user": "текст для пользователя",
    "memory_updates": {
        "exec:stage${step}_result": "результат выполнения этапа"
    },
    "workflow_transition": "VERIFICATION или null"
}

=== ПЕРЕХОДЫ ===
Если пользователь согласен переходить к проверке — workflow_transition: "VERIFICATION"
Иначе — workflow_transition: null (продолжаем уточнять)
""".trimIndent()
    }

    override suspend fun handle(
        context: TaskContext,
        userInput: String,
        llmResponse: TaskLlmResponse
    ): StateResult {
        val nextState = llmResponse.workflowTransition?.let {
            TaskState.fromString(it)
        }

        return StateResult(
            replyToUser = llmResponse.replyToUser,
            nextState = nextState,
            memoryUpdates = llmResponse.memoryUpdates
        )
    }
}
