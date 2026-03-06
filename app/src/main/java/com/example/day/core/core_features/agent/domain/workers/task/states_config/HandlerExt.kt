package com.example.day.core.core_features.agent.domain.workers.task.states_config

import com.example.day.core.core_features.agent.domain.workers.task.states_store.TaskContext
import com.example.day.core.core_features.chat.domain.model.ChatMessage
import com.example.day.core.core_features.state_machine.domain.HandlerResult
import com.example.day.core.core_features.state_machine.domain.model.StateId

fun List<TaskStateMessage>.continueHistory(
    userInput: String,
    llmAnswer: String
): List<TaskStateMessage> = buildList {
    addAll(this@continueHistory)
    if (userInput.isNullOrBlank() == false) {
        add(TaskStateMessage(TaskStateMessage.Role.USER, userInput))
    }
    add(TaskStateMessage(TaskStateMessage.Role.ASSISTANT, llmAnswer))
}


fun List<HandlerResult.Message>.withButton(
    action: String,
    title: String,
    description: String = "",
    replyMessage: String = ""
): List<HandlerResult.Message> {
    return this + HandlerResult.Message(
        message = "",
        isInfo = false,
        buttons = listOf(
            ChatMessage.Button(
                actionId = action,
                title = title,
                description = description,
                replyMessage = replyMessage,
                isEnabled = true,
                isPressed = false
            )
        )
    )
}

fun List<HandlerResult.Message>.withButtons(
    buttons: List<ChatMessage.Button>
): List<HandlerResult.Message> {
    return this + HandlerResult.Message(
        message = "",
        isInfo = false,
        buttons = buttons
    )
}

fun List<HandlerResult.Message>.withInfo(title: String): List<HandlerResult.Message> {
    return this + HandlerResult.Message(
        message = title,
        isInfo = true
    )
}

fun List<HandlerResult.Message>.withBot(title: String): List<HandlerResult.Message> {
    return this + HandlerResult.Message(
        message = title,
        isInfo = false
    )
}

suspend fun TaskContext.buildStateTransitionInfoMessage(
    newStep: Int,
    nextState: StateId?
): String {
    val totalStages = getTotalSteps()
    val newStageName = getCurStepNum()

    val stateLabel = when (nextState) {
        TaskStateConfig.PLANNING -> "PLANNING — Планирование"
        TaskStateConfig.EXECUTION -> buildString {
            append("EXECUTION — Этап $newStep")
            if (totalStages > 0) append(" из $totalStages")
            append(": $newStageName")
        }
        TaskStateConfig.VERIFICATION -> buildString {
            append("VERIFICATION — Верификация результатов")
        }
        TaskStateConfig.DONE -> "DONE — Финализация задачи"
        TaskStateConfig.INIT -> "INIT — Новая задача"
        null -> "Задачи нет"
        else -> nextState.value.uppercase()
    }
    return stateLabel
}

suspend fun TaskContext.buildFinalResult(withFeedbacks: Boolean): String {
    val initData = getStateData(TaskStateConfig.INIT, 1) as? TaskStateData.Init ?: return "Что-то пошло не так"
    val commonTitle = initData.title ?: "Не поставлена"
    val commonDescription = initData.description ?: "Описание отсутствует"
    val commonGoal = initData.goal ?: "Еще не поставлена"

    val totalSteps = getTotalSteps()
    val planData = getStateData(TaskStateConfig.PLANNING, 1) as? TaskStateData.Planning ?: TaskStateData.Planning()
    val verifData = getStateData(TaskStateConfig.VERIFICATION, 1) as? TaskStateData.Verification ?: TaskStateData.Verification()
    val stageDescriptionsAndResume: String = buildString {
        planData.steps.forEach { step ->
            append("Этап 1. ${step.title}\n")
            append("${step.description}\n")
            val exeData = getStateData(TaskStateConfig.EXECUTION, step.stepNumber) as? TaskStateData.Execution ?: TaskStateData.Execution(step.stepNumber)
            if (exeData.result?.isNotBlank() == true) {
                append("Решение этапа: \n${exeData.result}\n")
            }
            if (withFeedbacks) {
                val reviewItem = verifData.stepResults.find { it.stepNumber == step.stepNumber }
                if (reviewItem?.feedback?.isNotBlank() == true) {
                    append("Ревью этапа: \n${exeData.result}\n")
                }
            }
        }
        if (withFeedbacks && verifData.resume?.isNotBlank() == true) {
            append("Резюме ревьювера: \n${verifData.resume}\n")
            append("Оценка решения: ${verifData.score}/10")
        }
    }
    return """
Задача: $commonTitle.
Описание задачи: $commonDescription. 
Дополнительные детали: ${planData.taskDetails}.
Цель задачи: $commonGoal.
Количество этапов: $totalSteps
Поэтапное решение:\n$stageDescriptionsAndResume.\n
""".trimIndent() + stageDescriptionsAndResume
}