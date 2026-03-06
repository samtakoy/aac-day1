package com.example.day.core.core_features.agent.domain.workers.task.states

import com.example.day.core.core_features.agent.domain.model.TaskLlmResponse
import com.example.day.core.core_features.agent.domain.model.TaskMemoryKeys
import com.example.day.core.core_features.agent.domain.model.TaskState
import com.example.day.core.core_features.agent.domain.workers.task.TaskContext

/**
 * DONE state handler.
 * Finalizes task and generates summary report.
 */
class DoneStateHandler : TaskStateHandler {
    override val state: TaskState = TaskState.DONE

    override fun buildSystemPrompt(context: TaskContext): String {
        val title = context.getInitTitle() ?: "Задача"
        val description = context.getInitDescription() ?: ""
        val goal = context.getInitGoal() ?: ""
        val artifacts = context.getAllStageArtifacts()

        val artifactsText = artifacts.joinToString("\n\n") { artifact ->
            """
            | Этап ${artifact.step}: ${artifact.name}
            | Описание: ${artifact.description}
            | Результат: ${artifact.artifact}
            """.trimMargin()
        }

        return """
Ты — Project Manager.
Текущее состояние задачи: DONE.

=== АРТЕФАКТЫ ЗАДАЧИ ===
Название: $title
Описание: $description
Цель: $goal

Результаты по этапам:
$artifactsText

Инструкции:
1. Собери итоговый отчёт на основе всех артефактов.
2. Выведи пользователю красивое резюме всей проделанной работы.
3. Предложи начать новую задачу.

=== ПРОТОКОЛ ОТВЕТА ===
Формат ответа: Строго JSON.
{
    "thought": "рассуждения",
    "reply_to_user": "итоговый отчёт для пользователя",
    "memory_updates": {},
    "workflow_transition": null
}

=== ОЧИСТКА ===
При ответе workflow_transition должен быть null (задача завершена).
""".trimIndent()
    }

    override suspend fun handleLegacy(
        context: TaskContext,
        userInput: String,
        llmResponse: TaskLlmResponse
    ): StateResult {
        // DONE state - no transition, task is complete
        return StateResult(
            replyToUser = llmResponse.replyToUser,
            nextState = null,
            memoryUpdates = emptyMap()
        )
    }
}
