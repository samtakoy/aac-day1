package com.example.day.core.core_features.memory.domain.provider.rag

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Состояние текущей задачи/диалога пользователя.
 * Обновляется после каждого сообщения через POST /task-state/update.
 * Хранится в AgentMemoryRepository (category="task_state", key="current").
 *
 * Передаётся в QueryOptimizer для умного rewrite поискового запроса.
 */
@Serializable
data class TaskState(
    @SerialName("current_focus")
    val currentFocus: CurrentFocus = CurrentFocus(),
    @SerialName("tech_stack")
    val techStack: String = "",
    @SerialName("intent")
    val intent: String = "general",
    @SerialName("context_switched")
    val contextSwitched: Boolean = false,
    @SerialName("confirmed_decisions")
    val confirmedDecisions: List<String> = emptyList(),
    @SerialName("open_questions")
    val openQuestions: List<String> = emptyList(),
) {
    @Serializable
    data class CurrentFocus(
        @SerialName("file") val file: String = "",
        @SerialName("class") val className: String = "",
        @SerialName("method") val method: String = "",
    )

    companion object {
        val EMPTY = TaskState()
    }
}
