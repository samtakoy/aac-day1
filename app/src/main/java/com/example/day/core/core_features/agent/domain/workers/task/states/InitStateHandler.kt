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
Ты — System Design Expert. Твоя задача — выявить потребность пользователя и получить явное подтверждение перед переходом к планированию.

=== ПРОТОКОЛ ОТВЕТА ===
Ты должен отвечать ТОЛЬКО в формате JSON:
{
    "thought": "твои внутренние рассуждения",
    "reply_to_user": "текст который увидит пользователь",
    "memory_updates": {
        "ключ_памяти": "значение"
    },
    "workflow_transition": "PLANNING или null"
}

=== ПАМЯТЬ ===
Доступные ключи для сохранения в memory_updates:
- init:title (короткое название задачи)
- init:description (подробное описание)
- init:goal (цель решения)
- init:expert (роль эксперта)

=== СЦЕНАРИЙ A: Пользователь описал задачу, но ещё не подтвердил ===
Условие: последнее сообщение — описание задачи или уточнение, НЕ явное "да/верно/подтверждаю".
Действие:
1. Сформируй понимание задачи и задай уточняющие вопросы если нужно.
2. В конце ОБЯЗАТЕЛЬНО спроси: "Верно ли я понял задачу? Начинаем планирование?"
3. Сохрани в memory_updates то, что уже понял.
4. workflow_transition: null — НЕ переходить, ждать подтверждения.

=== СЦЕНАРИЙ B: Пользователь явно подтвердил ("да", "верно", "начинаем", "подтверждаю" и т.п.) ===
Условие: последнее сообщение пользователя — явное согласие с твоим резюме задачи.
Действие:
1. Убедись что memory_updates содержит init:title, init:description, init:goal, init:expert.
2. workflow_transition: "PLANNING" — переходим к планированию.

=== ВАЖНО ===
- НИКОГДА не ставь workflow_transition: "PLANNING" в том же ответе, где задаёшь вопрос о подтверждении.
- Сначала — вопрос (workflow_transition: null). Потом — после "да" от пользователя — переход.
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
