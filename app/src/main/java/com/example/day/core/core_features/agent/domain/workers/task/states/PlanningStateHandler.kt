package com.example.day.core.core_features.agent.domain.workers.task.states

import com.example.day.core.core_features.agent.domain.model.TaskLlmResponse
import com.example.day.core.core_features.agent.domain.model.TaskMemoryKeys
import com.example.day.core.core_features.agent.domain.model.TaskState
import com.example.day.core.core_features.agent.domain.workers.task.TaskContext

/**
 * PLANNING state handler.
 * Decomposes task into stages.
 */
class PlanningStateHandler : TaskStateHandler {
    override val state: TaskState = TaskState.PLANNING

    override fun buildSystemPrompt(context: TaskContext): String {
        val expert = context.getInitExpert() ?: "System Architect"
        val title = context.getInitTitle() ?: "Задача"
        val description = context.getInitDescription() ?: ""
        val goal = context.getInitGoal() ?: ""

        return """
Ты — $expert.
Текущее состояние задачи: PLANNING.
Задача: $title
Вводные данные: $description
Цель: $goal

Инструкции:
1. Разбей решение задачи на логические этапы (от 2 до 5).
2. Для каждого этапа укажи: название, краткое описание, требуемую роль эксперта.
3. Представь план пользователю и спроси: "Утверждаем этот план и начинаем первый этап?"

=== ПРОТОКОЛ ОТВЕТА ===
Формат ответа: Строго JSON.
{
    "thought": "рассуждения",
    "reply_to_user": "текст для пользователя",
    "memory_updates": {
        "plan:total_stages": "число этапов",
        "plan:stage1": "название этапа 1",
        "plan:stage1_desc": "описание этапа 1",
        "plan:stage1_expert": "эксперт для этапа 1",
        "plan:stage1_status": "0",
        "plan:stage2": "название этапа 2 (если есть)",
        "plan:stage2_desc": "описание этапа 2",
        "plan:stage2_expert": "эксперт для этапа 2",
        "plan:stage2_status": "0",
        ... и так далее для всех этапов
    },
    "workflow_transition": "EXECUTION или null"
}

=== СТАТУС ЭТАПОВ ===
plan:stageN_status: 0 = не брали, 1 = сделали (верификация), 2 = DONE

=== ПЕРЕХОДЫ ===
Если пользователь утвердил план — workflow_transition: "EXECUTION"
Иначе — workflow_transition: null
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

        val memoryUpdates = llmResponse.memoryUpdates.toMutableMap()

        // Set initial current step and current stage when transitioning to EXECUTION
        if (nextState == TaskState.EXECUTION) {
            memoryUpdates[TaskMemoryKeys.CURRENT_STEP] = "1"
            memoryUpdates[TaskMemoryKeys.PLAN_CURRENT_STAGE] = "1"
        }

        return StateResult(
            replyToUser = llmResponse.replyToUser,
            nextState = nextState,
            memoryUpdates = memoryUpdates,
            newStep = if (nextState == TaskState.EXECUTION) 1 else null
        )
    }
}
