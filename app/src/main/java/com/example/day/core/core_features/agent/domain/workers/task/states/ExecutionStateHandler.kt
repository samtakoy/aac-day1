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

=== СЦЕНАРИЙ A: Выполняем этап, ждём согласия пользователя ===
Условие: пользователь ещё не дал явного согласия на проверку.
Действия:
1. Реши задачу текущего этапа максимально подробно.
2. Сохрани результат в memory_updates: exec:stage${step}_result.
3. В конце ОБЯЗАТЕЛЬНО спроси: "Переходим к проверке этого этапа?"
4. workflow_transition: null — ждать согласия.

=== СЦЕНАРИЙ B: Пользователь явно согласился ("да", "переходим", "проверяй" и т.п.) ===
Условие: последнее сообщение пользователя — явное согласие перейти к проверке.
Действия:
1. workflow_transition: "VERIFICATION" — переходим к проверке.

=== ВАЖНО ===
- НИКОГДА не ставь workflow_transition: "VERIFICATION" в том же ответе, где представляешь результат и задаёшь вопрос.
- Сначала — результат + вопрос (workflow_transition: null). Потом — после "да" — переход.
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
