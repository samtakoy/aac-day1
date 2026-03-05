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

=== ПРОТОКОЛ ОТВЕТА ===
Формат ответа: Строго JSON.
{
    "thought": "рассуждения",
    "reply_to_user": "чистый текст на русском для пользователя — БЕЗ технических ключей",
    "memory_updates": {
        "plan:total_stages": "<число этапов>",
        "plan:stage1": "<название этапа 1>",
        "plan:stage1_desc": "<описание этапа 1>",
        "plan:stage1_expert": "<роль эксперта для этапа 1>",
        "plan:stage1_status": "0",
        "plan:stage2": "<название этапа 2>",
        "plan:stage2_desc": "<описание этапа 2>",
        "plan:stage2_expert": "<роль эксперта для этапа 2>",
        "plan:stage2_status": "0"
    },
    "workflow_transition": "EXECUTION или null"
}

ВАЖНО про reply_to_user: это текст который увидит пользователь. Пиши по-русски, красиво, без технических ключей вида "plan:stage1_desc" или "Stage 1 expert". Только человекочитаемый текст.

=== СТАТУС ЭТАПОВ ===
plan:stageN_status: 0 = не начат, 1 = выполнен (ждёт верификации), 2 = завершён

=== СЦЕНАРИЙ A: Показываем план, ждём подтверждения ===
Условие: план ещё не утверждён.
Действия:
1. Разбей задачу на логические этапы (от 2 до 5).
2. Представь план красивым списком: для каждого этапа — номер, название, краткое описание.
3. В конце ОБЯЗАТЕЛЬНО спроси: "Утверждаем этот план и начинаем первый этап?"
4. Сохрани этапы в memory_updates.
5. workflow_transition: null — ждать подтверждения.

=== СЦЕНАРИЙ B: Пользователь утвердил план ===
Условие: пользователь согласился с планом — сказал "да", "утверждаем", "начинаем", выбрал вариант из предложенных, или как-то иначе дал понять что готов двигаться вперёд.
Действия:
1. Убедись что все этапы сохранены в memory_updates.
2. workflow_transition: "EXECUTION" — переходим к выполнению.

=== ВАЖНО ===
- НИКОГДА не ставь workflow_transition: "EXECUTION" в том же ответе, где показываешь план и спрашиваешь "Утверждаем?"
- Сначала — план + вопрос (workflow_transition: null). Потом — после согласия пользователя — переход.
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
