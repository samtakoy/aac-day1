package com.example.day.core.core_features.agent.domain.workers.task.states_config.handlers

import com.example.day.core.core_features.agent.domain.model.TaskLlmResponse
import com.example.day.core.core_features.agent.domain.workers.task.TaskMemKeys
import com.example.day.core.core_features.agent.domain.workers.task.states_config.TaskStateConfig
import com.example.day.core.core_features.agent.domain.workers.task.states_config.TaskStateData
import com.example.day.core.core_features.agent.domain.workers.task.states_config.TaskStateMessage
import com.example.day.core.core_features.agent.domain.workers.task.states_config.buildFinalResult
import com.example.day.core.core_features.agent.domain.workers.task.states_config.buildStateTransitionInfoMessage
import com.example.day.core.core_features.agent.domain.workers.task.states_config.continueHistory
import com.example.day.core.core_features.agent.domain.workers.task.states_config.handlers.VerificationStateHandler.Companion.ACTION_DONE
import com.example.day.core.core_features.agent.domain.workers.task.states_config.handlers.VerificationStateHandler.Companion.ACTION_EXEC
import com.example.day.core.core_features.agent.domain.workers.task.states_config.handlers.VerificationStateHandler.Companion.MAX_REVIEW_COUNT
import com.example.day.core.core_features.agent.domain.workers.task.states_config.handlers.VerificationStateHandler.Companion.MIN_PASSING_SCORE
import com.example.day.core.core_features.agent.domain.workers.task.states_config.withBot
import com.example.day.core.core_features.agent.domain.workers.task.states_config.withButtons
import com.example.day.core.core_features.agent.domain.workers.task.states_config.withInfo
import com.example.day.core.core_features.state_machine.domain.HandlerResult
import com.example.day.core.core_features.agent.domain.workers.task.states_store.TaskContext
import com.example.day.core.core_features.chat.domain.model.ChatMessage
import com.example.day.core.core_features.state_machine.domain.TaskStateHandler
import com.example.day.core.core_features.state_machine.domain.model.StateId
import kotlinx.collections.immutable.toPersistentList
import kotlinx.serialization.json.Json

/**
 * VERIFICATION state handler.
 * Quality control for current stage.
 */
class VerificationStateHandler : TaskStateHandler {
    companion object {
        const val ACTION_DONE = "proceed_to_done"
        const val ACTION_EXEC = "proceed_to_execution"
        const val MIN_PASSING_SCORE = 8
        const val MAX_REVIEW_COUNT = 2

        private val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
            prettyPrint = true
        }
    }

    override val stateName: StateId = TaskStateConfig.VERIFICATION

    override suspend fun buildSystemPrompt(context: TaskContext): String {
        context.getCurStepNum()
        context.getTotalSteps()


        return """
Ты — Senior QA & Architect.
К нам пришел человек с потребностью решить его задачу.
Задача была рассмотрена, разбита на этапы и описано ее решение.
Ты должен провести ревью сделанного описания и:
 1. Оценить общее решение по шкале от 0 до 10.
 2. Оценить решение каждого этапа.
 3. Выдать фидбэк по каждому этапу.
 4. Выдать общее небольшое резюме и рекомендации.

Описание задачи: ${context.buildFinalResult(withFeedbacks = false)}

=== ПРОТОКОЛ ОТВЕТА ===
Ты должен отвечать строго ТОЛЬКО в формате JSON:
{
    "${TaskMemKeys.REPLY_TO_USER}": "твой ответ - чистый текст на русском для пользователя — БЕЗ технических ключей",
    "${TaskMemKeys.MEM_UPDATES}": {
        "ключ_памяти": "значение"
    }
}

=== ПРАВИЛА ФОРМАТИРОВАНИЯ JSON ===
• Весь ответ — это ОДИН валидный JSON-объект, без markdown, без пояснений до/после.
• Пример корректного ответа:
{
    "${TaskMemKeys.REPLY_TO_USER}": "Ревью завершено.\nОбщая оценка: 8/10.\nРекомендую доработать этап 2.",
    "${TaskMemKeys.MEM_UPDATES}": {
        "${TaskMemKeys.VERIF_SCORE}": "8",
        "${TaskMemKeys.VERIF_RESUME}": "Решение качественное, но требует уточнения критериев тестирования.",
        "${TaskMemKeys.verifStageScore(1)}": "9",
        "${TaskMemKeys.verifStageFeedback(1)}": "Отлично проработано",
        "${TaskMemKeys.verifStageScore(2)}": "6",
        "${TaskMemKeys.verifStageFeedback(2)}": "Не хватает деталей по интеграции"
    }
}

=== ПАМЯТЬ ===
- ${TaskMemKeys.VERIF_SCORE} (оценка (цифра) решения - по шкале от 1 до 10)
- ${TaskMemKeys.VERIF_RESUME}  (общее резюме по решению)
Для каждого этапа тройка ключей:
- ${TaskMemKeys.verifStageScore(1)} (оценка этапа 1)
- ${TaskMemKeys.verifStageFeedback(1)} (резюме/критика по этапу 1)
...
- ${TaskMemKeys.verifStageScoreN()} (оценка этапа N)
- ${TaskMemKeys.verifStageFeedbackN()} (резюме/критика по этапу N)

=== Инструкции ==
1. Оцени результат по шкале 1-10 и занеси результат в ${TaskMemKeys.VERIF_SCORE}.
2. Если оценка < $MIN_PASSING_SCORE:
    Напиши конкретные замечания, что нужно доработать и занеси в ${TaskMemKeys.VERIF_RESUME}. 
3. Если оценка >= $MIN_PASSING_SCORE:
    Оставь короткое резюму по решению в ${TaskMemKeys.VERIF_RESUME}.
4. Заполни значения ключей памяти ${TaskMemKeys.verifStageScoreN()} и ${TaskMemKeys.verifStageFeedbackN()} для каждого этапа без исключения. 

""".trimIndent()
    }

    override suspend fun buildAssistantPreFillPrompt(context: TaskContext): String {
        return "Хорошо. Я получил задание провести ревью задачи и ее этапов. Вот мои выводы и оценки."
    }

    override suspend fun handle(
        context: TaskContext,
        userInput: String,
        llmResponse: TaskLlmResponse
    ): HandlerResult {
        var data = context.getStateData(TaskStateConfig.VERIFICATION, 1) as? TaskStateData.Verification ?: TaskStateData.Verification()
        data = tryFillResultData(llmResponse, context.getTotalSteps(), data.retryCount).copy(
            history = data.history.continueHistory(userInput, llmResponse.replyToUser)
        )
        val chatMessages = listOf(
            HandlerResult.Message(llmResponse.replyToUser, false)
        )

        // return if (data.stepResults.isNotEmpty()  && data.stepResults.size == context.getTotalSteps() && data.resume != null && data.score != null) {
        return if (data.stepResults.isNotEmpty() && data.resume != null && data.score != null) {
            val userTask = buildUserTask(data)
            // Сохранить данные текущего состояния
            context.saveStateData(data)

            HandlerResult(
                messages = chatMessages.addButtons(data).withInfo(userTask)
            )
        } else {
            context.saveStateData(data)
            // Stay in INIT, return response to user
            HandlerResult(messages = chatMessages.addButtons(data).withInfo("rawResult: ${json.encodeToString(llmResponse)}"))
        }
    }

    override suspend fun handleUserAction(
        context: TaskContext,
        action: String
    ): HandlerResult {
        val actionStep = action.split(":").getOrNull(1)?.toIntOrNull() ?: 0
        return if (action == ACTION_DONE) {
            val nextState = TaskStateConfig.DONE
            val nextStep = 1
            // TODO добавить проверку полноты данных
            context.updateState(nextState)
            // сообщить о переходе и разбудить Llm
            HandlerResult(
                messages = emptyList<HandlerResult.Message>()
                    .withInfo(context.buildStateTransitionInfoMessage(nextStep, nextState)),
                // Будим Llm
                llmRequest = HandlerResult.LlmRequest()
            )
        } else if (action.startsWith(ACTION_EXEC) && actionStep > 0) {
            // отметить этап как незаконченный и добавть feedback
            makeExecutionStateUncompleted(context, actionStep)

            val nextState = TaskStateConfig.EXECUTION
            context.updateState(nextState)
            // сообщить о переходе и разбудить Llm
            HandlerResult(
                messages = emptyList<HandlerResult.Message>()
                    .withInfo(context.buildStateTransitionInfoMessage(actionStep, nextState)),
                // Будим Llm
                llmRequest = HandlerResult.LlmRequest()
            )
        } else {
            HandlerResult(
                messages = emptyList<HandlerResult.Message>().withBot("Неизвестная команда: $action")
            )
        }
    }

    private suspend fun makeExecutionStateUncompleted(context: TaskContext, executionStep: Int) {
        val verifData = context.getStateData(TaskStateConfig.VERIFICATION, 1) as? TaskStateData.Verification ?: return
        context.saveStateData(
            verifData.copy(
                retryCount = verifData.retryCount + 1,
            )
        )
        val planData = context.getStateData(TaskStateConfig.PLANNING, 1) as? TaskStateData.Planning ?: return
        context.saveStateData(
            planData.copy(
                steps = planData.steps.map { step ->
                    if (step.stepNumber == executionStep) {
                        step.copy(isCompleted = false)
                    } else {
                        step
                    }
                }
            )
        )
        // prefill для стадии выполнения с фидбэком
        val feedback = verifData.stepResults.find { it.stepNumber == executionStep }?.feedback ?: return
        val execData = context.getStateData(TaskStateConfig.VERIFICATION, executionStep) as? TaskStateData.Verification ?: return
        context.saveStateData(
            execData.copy(
                history = (execData.history + TaskStateMessage(
                    role = TaskStateMessage.Role.ASSISTANT,
                    content = "Хорошо. Я получил фидбэк от эксперта по проделанной работе: $feedback.\nИ сейчас я продолжу диалог с пользователем, чтобы улучшить решение."
                )).toPersistentList()
            )
        )
    }

    private fun buildUserTask(data: TaskStateData.Verification): String {
        return buildString {
            append("Результаты верификации:")
            data.stepResults.forEach { step ->
                append("Ревью этапа ${step.stepNumber}\n${step.feedback}\n")
                append("    Оценка: ${step.score}/1-\n")
            }
            append("Резюме ревьювера: \n${data.resume}\n")
            append("Оценка решения: ${data.score}/10")
        }.trim()
    }

    private fun tryFillResultData(
        llmResponse: TaskLlmResponse,
        totalSteps: Int,
        retryCount: Int
    ): TaskStateData.Verification {
        val score = llmResponse.memoryUpdates[TaskMemKeys.VERIF_SCORE]?.toIntOrNull()
        val resume = llmResponse.memoryUpdates[TaskMemKeys.VERIF_RESUME]

        val steps: List<TaskStateData.ReviewItem> = buildList {
            for (i in 1..totalSteps) {
                val stepScore = llmResponse.memoryUpdates[TaskMemKeys.verifStageScore(i)]?.toIntOrNull()
                val stepResume = llmResponse.memoryUpdates[TaskMemKeys.verifStageFeedback(i)]
                if (stepScore != null && stepResume != null) {
                    add(
                        TaskStateData.ReviewItem(
                            stepNumber = i,
                            score = stepScore,
                            feedback = stepResume
                        )
                    )
                }
            }
        }
        return TaskStateData.Verification(
            retryCount = retryCount,
            stepResults = steps,
            score = score,
            resume = resume
        )
    }
}

private fun List<HandlerResult.Message>.addButtons(data: TaskStateData.Verification): List<HandlerResult.Message> {
    return this.withButtons(
        buttons = buildList {
            data.stepResults.forEach { step ->
                if (step.score < MIN_PASSING_SCORE && data.retryCount < MAX_REVIEW_COUNT) {
                    add(
                        ChatMessage.Button(
                            actionId = "$ACTION_EXEC:${step.stepNumber}",
                            title = "К этапу ${step.stepNumber}",
                            description = "",
                            replyMessage = "К этапу ${step.stepNumber}"
                        )
                    )
                }
            }
            add(
                ChatMessage.Button(
                    actionId = ACTION_DONE,
                    title = "К результатам",
                    description = "",
                    replyMessage = "К результатам"
                )
            )
        }
    )
}