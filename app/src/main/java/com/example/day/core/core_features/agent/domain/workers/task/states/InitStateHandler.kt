package com.example.day.core.core_features.agent.domain.workers.task.states

import com.example.day.core.core_features.agent.domain.model.TaskLlmResponse
import com.example.day.core.core_features.agent.domain.model.TaskMemoryKeys
import com.example.day.core.core_features.agent.domain.model.TaskState
import com.example.day.core.core_features.agent.domain.workers.task.TaskContext

/**
 * INIT state handler.
 * Gathers requirements and determines expert role.
 */
class InitStateHandler : TaskStateHandler {
    override val state: TaskState = TaskState.INIT

    override fun buildSystemPrompt(context: TaskContext): String = """
Ты — System Design Expert. Твоя задача — выявить потребность пользователя.

Инструкции:
1. Проанализируй сообщения и последнее сообщение пользователя.
2. Если информации мало — задай уточняющие вопросы (о цели, юзкейсах, ограничениях).
3. Если информации достаточно — сформируй резюме задачи.
4. В ответе обязательно спроси подтверждение: "Верно ли я понял задачу? Переходим к планированию?"

=== ПРОТОКОЛ ОТВЕТА ===
Ты должен отвечать ТОЛЬКО в формате JSON следующей структуры:
{
    "thought": "твои внутренние рассуждения",
    "reply_to_user": "текст который увидит пользователь",
    "memory_updates": {
        "ключ_памяти": "значение"
    },
    "workflow_transition": "NEXT_STATE или null"
}

=== ПАМЯТЬ ===
Доступные ключи для сохранения в memory_updates:
- init:title (короткое название задачи)
- init:description (подробное описание)
- init:goal (цель решения)
- init:expert (роль эксперта)

=== ПЕРЕХОДЫ ===
Если пользователь подтвердил понимание задачи — в workflow_transition укажи "PLANNING"
Иначе укажи null

=== ПРАВИЛА ===
- Если понял задачу — заполни memory_updates и укажи workflow_transition: "PLANNING"
- Если нужны уточнения — memory_updates оставь пустым, workflow_transition: null
""".trimIndent()

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
