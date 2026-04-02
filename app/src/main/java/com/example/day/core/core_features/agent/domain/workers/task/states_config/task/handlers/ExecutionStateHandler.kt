package com.example.day.core.core_features.agent.domain.workers.task.states_config.task.handlers

import com.example.day.core.core_features.agent.domain.model.TaskLlmResponse
import com.example.day.core.core_features.agent.domain.workers.task.states_config.task.TaskMemKeys
import com.example.day.core.core_features.agent.domain.workers.task.TaskResponseParser
import com.example.day.core.core_features.agent.domain.workers.task.states_config.task.TaskStateConfig
import com.example.day.core.core_features.agent.domain.workers.task.states_config.task.TaskStateData
import com.example.day.core.core_features.agent.domain.workers.task.states_config.buildStateTransitionInfoMessage
import com.example.day.core.core_features.agent.domain.workers.task.states_config.continueHistory
import com.example.day.core.core_features.agent.domain.workers.task.states_config.withBot
import com.example.day.core.core_features.agent.domain.workers.task.states_config.withButton
import com.example.day.core.core_features.agent.domain.workers.task.states_config.withInfo
import com.example.day.core.core_features.agent.domain.workers.task.states_config.withTitle
import com.example.day.core.core_features.state_machine.domain.StateContext
import com.example.day.core.core_features.state_machine.domain.HandlerResult
import com.example.day.core.core_features.state_machine.domain.TaskStateHandler
import com.example.day.core.core_features.state_machine.domain.model.StateId

/**
 * EXECUTION state handler.
 * Implements current stage with detailed solution.
 */
class ExecutionStateHandler : TaskStateHandler {
    companion object {
        private const val ACTION_PROCEED = "proceed_to_next"
    }

    override val stateName: StateId = TaskStateConfig.EXECUTION

    override suspend fun buildSystemPrompt(context: StateContext): String {
        val initData = context.getStateData(TaskStateConfig.INIT, 1) as? TaskStateData.Init ?: return "Что-то пошло не так"
        val commonTitle = initData.title ?: "Не поставлена"
        val commonDescription = initData.description ?: "Описание отсутствует"
        val commonGoal = initData.goal ?: "Еще не поставлена"

        val step = context.getCurStepNum()
        val planData = context.getStateData(TaskStateConfig.PLANNING, 1) as? TaskStateData.Planning ?: return "Что-то пошло не так"
        val stepData = planData.steps.getOrNull(context.getCurStepIdx()) ?: return "Что-то пошло не так"

        val stageName = stepData.title
        val stageDesc = stepData.description
        val expert = stepData.expert ?: "Expert"

        val verifData = context.getStateData(TaskStateConfig.VERIFICATION, 1) as? TaskStateData.Verification
        val feedback = verifData?.stepResults?.find { it.stepNumber == step }?.feedback

        val feedbackSection = if (feedback != null) {
            """

=== ЗАМЕЧАНИЯ ВАЛИДАТОРА ===
${feedback}
"""
        } else ""

        return """
Ты — $expert.
К нам в компанию пришел человек с потребностью решить конкретный ЭТАП его задачи.
Ты должен в процессе опроса пользователя и уточнения деталей сформировать и описать подробное решение текущего этапа его задачи.

Общая задача: $commonTitle.
Описание общей задачи: $commonDescription. ${planData.taskDetails}
Общая цель: $commonGoal.
Общий план: ${planData.steps.joinToString(", ") { it.title }}}.

Текущий Этап $step: $stageName (его решение - твоя ответственность).
Описание этапа: $stageDesc
$feedbackSection

=== ПРОТОКОЛ ОТВЕТА ===
Ты должен отвечать строго ТОЛЬКО в формате JSON:
{
    "${TaskMemKeys.REPLY_TO_USER}": "текст для пользователя",
    "${TaskMemKeys.MEM_UPDATES}": {
        "${TaskMemKeys.EXECUTION_RESULT}": "описание твоего решения текущего эатапа задачи"
    },
    "${TaskMemKeys.NEXT_STATE}": "${TaskMemKeys.EXECUTION} или null",
    "${TaskMemKeys.USER_APPROVE}": "${TaskMemKeys.TRUE} или null"
}

=== ПРАВИЛА ФОРМАТИРОВАНИЯ JSON ===
• Весь ответ — это ОДИН валидный JSON-объект, без markdown, без пояснений до/после.
• Пример корректного ответа:
{
    "${TaskMemKeys.REPLY_TO_USER}": "Вот решение для этапа \"Название\":\n1. Подготовь инструменты\n2. Выполни действия\n3. Проверь результат",
    "${TaskMemKeys.MEM_UPDATES}": {
        "${TaskMemKeys.EXECUTION_RESULT}": "Пошаговое решение:\n- Шаг 1: ...\n- Шаг 2: ...\nКритерии успеха: ..."
    },
    "${TaskMemKeys.NEXT_STATE}": null,
    "${TaskMemKeys.USER_APPROVE}": null
}

=== ПАМЯТЬ ===
- "${TaskMemKeys.EXECUTION_RESULT}" (твоей подробное описание как пользователю решить текущий этап общей задачи)

=== ЗАПОЛНЕНИЕ ДАННЫХ ОТВЕТА ===
0. Свой ответ/вопрос на последнее сообщение пользователя клади в ${TaskMemKeys.REPLY_TO_USER}
1. Если у тебя сформировалось полное понимание задачи, как реализовать решение текущего этапа задачи,
 то занеси в ${TaskMemKeys.MEM_UPDATES} подробное описание решения в значение с ключом ${TaskMemKeys.EXECUTION_RESULT}
 и заполни ${TaskMemKeys.NEXT_STATE}: "${TaskMemKeys.EXECUTION}".
 Если в последнем сообщении пользователь явно и однозначно дал понять что решение текущего этапа его устраивает и он хочет перейти к следующему этапу, то заполни дополнительно ${TaskMemKeys.USER_APPROVE}: "${TaskMemKeys.TRUE}".
2. Иначе (пока решение не сформировано) не заполняй ${TaskMemKeys.NEXT_STATE} и значения ключей памяти


""".trimIndent()
    }

    override suspend fun buildAssistantPreFillPrompt(context: StateContext): String {
        val currentStep = context.getCurStepNum()
        val planData = context.getStateData(TaskStateConfig.PLANNING, currentStep) as? TaskStateData.Planning
        val stepData = planData?.steps?.getOrNull(context.getCurStepIdx())

        return "Хорошо. Я понимаю, что сейчас я должен описать решение этапа ${stepData?.title}. Приступаю к выяснению и уточнению деталей. И вот мой первый вопрос."
    }

    override suspend fun handle(
        context: StateContext,
        userInput: String,
        rawResponse: String
    ): HandlerResult {
        val llmResponse = TaskResponseParser.parse(rawResponse)
            ?: return HandlerResult(
                messages = listOf(HandlerResult.Message(
                    "⚠️ Не удалось разобрать ответ ассистента. Попробуйте переформулировать запрос.\n\nRaw: $rawResponse",
                    isInfo = true
                ))
            )
        val nextState = llmResponse.nextState?.let {
            TaskStateConfig.config.toValidStateOrNull(it.lowercase())
        }

        val curStep = context.getCurStepNum()
        var data = context.getStateData(TaskStateConfig.EXECUTION, curStep) as? TaskStateData.Execution ?: TaskStateData.Execution(curStep)
        data = tryFillResultData(llmResponse, curStep).copy(
            history = data.history.continueHistory(userInput, llmResponse.replyToUser)
        )
        var chatMessages = listOf(
            HandlerResult.Message(llmResponse.replyToUser, false)
        )

        return if (nextState == TaskStateConfig.EXECUTION && data.result?.isNotBlank() == true) {
            // Сохранить данные текущего состояния
            context.saveStateData(data, curStep)

            if (llmResponse.userApprove == TaskMemKeys.TRUE) {
                // Есть явное подтверждение пользователя.
                handleUserAction(context, ACTION_PROCEED)
            } else {
                HandlerResult(
                    messages = chatMessages.withButton(
                        action = ACTION_PROCEED,
                        title = "К следующему этапу",
                        messageText = buildUserTask(data)
                    )
                )
            }

        } else {
            context.saveStateData(data, curStep)
            chatMessages = if (nextState == TaskStateConfig.EXECUTION) {
                chatMessages.withInfo("Бот решил что этап выполнен, но не заполнил все данные.\n $data")
            } else {
                chatMessages
            }
            // Stay in INIT, return response to user
            HandlerResult(messages = chatMessages)
        }
    }

    override suspend fun handleUserAction(
        context: StateContext,
        action: String
    ): HandlerResult {
        // Execu TODO("в конце сбросить фидбэк. и посмотреть в списке этапв - кто следующий")
        return if (action == ACTION_PROCEED) {
            makeCurStepCompleted(context)
            val nextStepNum = findNextUncompletedStep(context)
            val nextState = if (nextStepNum <= 0) TaskStateConfig.VERIFICATION else TaskStateConfig.EXECUTION
            // переключить состояние
            // TODO добавить проверку полноты данных
            context.updateState(nextState)
            // и шага для Execution
            context.setCurStepNum(nextStepNum)
            // сообщить о переходе и разбудить Llm
            HandlerResult(
                messages = emptyList<HandlerResult.Message>()
                    .withTitle(context.buildStateTransitionInfoMessage(nextStepNum, nextState)),
                // Будим Llm
                llmRequest = HandlerResult.LlmRequest()
            )
        } else {
            HandlerResult(
                messages = emptyList<HandlerResult.Message>().withBot("Неизвестная команда: $action")
            )
        }
    }

    private suspend fun findNextUncompletedStep(context: StateContext): Int {
        val planData = context.getStateData(TaskStateConfig.PLANNING, 1) as? TaskStateData.Planning ?: return 0
        planData.steps.forEach { step ->
            if (!step.isCompleted) {
                return step.stepNumber
            }
        }
        return 0
    }

    /** Отметить текущий шаг как выполненный (и убрат фидбэк, т.к. он больше не актуален) */
    private suspend fun makeCurStepCompleted(context: StateContext) {
        val planData = context.getStateData(TaskStateConfig.PLANNING, 1) as? TaskStateData.Planning ?: return
        val currentStep = context.getCurStepNum()
        context.saveStateData(
            planData.copy(
                steps = planData.steps.map { step ->
                    if (step.stepNumber == currentStep) {
                        step.copy(isCompleted = true)
                    } else {
                        step
                    }
                }
            )
        )
        val verifData = context.getStateData(TaskStateConfig.VERIFICATION, 1) as? TaskStateData.Verification
        if (verifData != null && verifData.stepResults.isNotEmpty()) {
            context.saveStateData(
                verifData.copy(
                    stepResults = verifData.stepResults.map { step ->
                        if (step.stepNumber == currentStep) {
                            step.copy(feedback = "")
                        } else {
                            step
                        }
                    }
                )
            )
        }
    }

    private fun tryFillResultData(llmResponse: TaskLlmResponse, curStep: Int): TaskStateData.Execution {
        return TaskStateData.Execution(
            stepNumber = curStep,
            result = llmResponse.memoryUpdates[TaskMemKeys.EXECUTION_RESULT]
        )
    }

    private fun buildUserTask(data: TaskStateData.Execution): String {
        return buildString {
            append("Результаты этапа ${data.stepNumber}:")
            append("${data.result}\n")
        }.trim()
    }
}
