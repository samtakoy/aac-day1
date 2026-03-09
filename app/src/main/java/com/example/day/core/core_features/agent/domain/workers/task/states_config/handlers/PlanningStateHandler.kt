package com.example.day.core.core_features.agent.domain.workers.task.states_config.handlers

import com.example.day.core.core_features.agent.domain.model.TaskLlmResponse
import com.example.day.core.core_features.agent.domain.workers.task.TaskMemKeys
import com.example.day.core.core_features.agent.domain.workers.task.states_config.TaskStateConfig
import com.example.day.core.core_features.agent.domain.workers.task.states_config.TaskStateData
import com.example.day.core.core_features.agent.domain.workers.task.states_config.buildStateTransitionInfoMessage
import com.example.day.core.core_features.agent.domain.workers.task.states_config.continueHistory
import com.example.day.core.core_features.agent.domain.workers.task.states_config.withBot
import com.example.day.core.core_features.agent.domain.workers.task.states_config.withButton
import com.example.day.core.core_features.agent.domain.workers.task.states_config.withInfo
import com.example.day.core.core_features.agent.domain.workers.task.states_config.withTitle
import com.example.day.core.core_features.agent.domain.workers.task.states_store.TaskContext
import com.example.day.core.core_features.state_machine.domain.HandlerResult
import com.example.day.core.core_features.state_machine.domain.TaskStateHandler
import com.example.day.core.core_features.state_machine.domain.model.StateId

/**
 * PLANNING state handler.
 * Decomposes task into stages.
 */
class PlanningStateHandler : TaskStateHandler {
    companion object {
        private const val ACTION_PROCEED = "proceed_to_execution"
    }

    override val stateName: StateId = TaskStateConfig.PLANNING

    override suspend fun buildSystemPrompt(context: TaskContext): String {
        // Читаем результаты предыдущего этапа
        val initData = context.getStateData(TaskStateConfig.INIT, 1) as? TaskStateData.Init ?: return "Что-то пошло не так"
        val expert = initData.expert ?: "Эксперт"
        val title = initData.title ?: "Не поставлена"
        val description = initData.description ?: "Описание отсутствует"
        val goal = initData.goal ?: "Еще не поставлена"

        return """
Ты — $expert.
К тебе пришел человек с потребностью решить свою задачу. Ты должен в процессе опроса и уточнения деталей определить основные этапы решения задачи пользователя:
 количество этапов, название каждого этапа, описание каждого этапа, роль эксперта который лучше всего поможет в проработке деталей каждого этапа.
Задача: $title
Вводные данные: $description
Цель: $goal

=== ПРОТОКОЛ ОТВЕТА ===
Ты должен отвечать строго ТОЛЬКО в формате JSON:
{
    "${TaskMemKeys.REPLY_TO_USER}": "твой ответ - чистый текст на русском для пользователя — БЕЗ технических ключей",
    "${TaskMemKeys.MEM_UPDATES}": {
        "ключ_памяти": "значение"
    },
    "${TaskMemKeys.NEXT_STATE}": "${TaskMemKeys.EXECUTION} или null",
    "${TaskMemKeys.USER_APPROVE}": "${TaskMemKeys.TRUE} или null"
}

=== ПРАВИЛА ФОРМАТИРОВАНИЯ JSON ===
• Весь ответ — это ОДИН валидный JSON-объект, без markdown, без пояснений до/после.
• Пример корректного ответа (количество этапов не регламентировано - тут 3 только для примера):
{
    "${TaskMemKeys.REPLY_TO_USER}": "Отлично, я понял задачу.\nПлан включает 3 этапа:\n1. Анализ требований\n2. Проектирование\n3. Реализация",
    "${TaskMemKeys.MEM_UPDATES}": {
        "${TaskMemKeys.PLAN_TOTAL_STAGES}": "1",
        "${TaskMemKeys.planStageName(1)}": "Анализ",
        "${TaskMemKeys.planStageDesc(1)}": "Сбор и анализ требований",
        "${TaskMemKeys.planStageExpert(1)}": "Business Analyst"
    },
    "${TaskMemKeys.NEXT_STATE}": null,
    "${TaskMemKeys.USER_APPROVE}": null
}

=== ПАМЯТЬ ===
- ${TaskMemKeys.PLAN_TOTAL_STAGES} (сколько этапов нужно для решения задачи)
- ${TaskMemKeys.PLAN_TASK_DETAILS}  (дополнительные детали задачи выясненные во время диалога, подробности, пожелания от пользователя)
Для каждого этапа тройка ключей:
- ${TaskMemKeys.planStageName(1)} (название этапа 1)
- ${TaskMemKeys.planStageDesc(1)} (описание этапа 1)
- ${TaskMemKeys.planStageExpert(1)} (роль эксперта для этапа 1)
...
- ${TaskMemKeys.planStageNameN()} (название этапа N)
- ${TaskMemKeys.planStageDescN()} (описание этапа N)
- ${TaskMemKeys.planStageExpertN()} (роль эксперта для этапа N)

=== ЗАПОЛНЕНИЕ ДАННЫХ ОТВЕТА ===
0. Свой ответ/вопрос на последнее сообщение пользователя клади в ${TaskMemKeys.REPLY_TO_USER}
1. Если у тебя сформировалось полное понимание задачи, чего хочет достичь пользователь и с помощью каких этапов это можно реализовать,
 то заполни в ${TaskMemKeys.MEM_UPDATES} количество этапов ${TaskMemKeys.PLAN_TOTAL_STAGES} а также для каждого этапа значения его ключей ${TaskMemKeys.planStageNameN()}, ${TaskMemKeys.planStageDescN()}, ${ TaskMemKeys.planStageExpertN()}
 и заполни ${TaskMemKeys.NEXT_STATE}: "${TaskMemKeys.EXECUTION}".
 Если во время диалога выяснились дополнительные детали задачи, то помести их в  в ${TaskMemKeys.MEM_UPDATES} с ключом ${TaskMemKeys.PLAN_TASK_DETAILS}
  Важно: ${TaskMemKeys.planStageDescN()} должен быть полным и содержать ПОДРОБНУЮ информацию, а также возможные инсайты для решения, включая пожелания пользователя по решению (если были).
 Если в последнем сообщении пользователь явно и однозначно дал понять что план его устраивает и он хочет перейти к деталям первого этапа, то заполни дополнительно ${TaskMemKeys.USER_APPROVE}: "${TaskMemKeys.TRUE}".
2. Иначе (пока план не сформирован) не заполняй ${TaskMemKeys.NEXT_STATE} и значения ключей памяти
""".trimIndent()
    }

    override suspend fun buildAssistantPreFillPrompt(context: TaskContext): String {
        return "Итак, ко мне пришел пользователь с постановкой задачи, которую мне нужно разбить на этапы и определить эксперта для каждого этапа. Сейчас я напишу, что я думаю об этом на основе имеющейся информации и буду задавать вопросы для полного понимания, с помощью каких этапов и кем решается задача пользователя, чтобы пользователь могу достичь своей цели."
    }

    override suspend fun handle(
        context: TaskContext,
        userInput: String,
        llmResponse: TaskLlmResponse
    ): HandlerResult {
        val nextState = llmResponse.nextState?.let {
            TaskStateConfig.config.toValidStateOrNull(it.lowercase())
        }

        var data = context.getStateData(TaskStateConfig.PLANNING, 1) as? TaskStateData.Planning ?: TaskStateData.Planning()
        data = tryFillResultData(llmResponse).copy(
            history = data.history.continueHistory(userInput, llmResponse.replyToUser)
        )
        var chatMessages = listOf(
            HandlerResult.Message(llmResponse.replyToUser, false)
        )

        return if (nextState == TaskStateConfig.EXECUTION && data.steps.isNotEmpty() && data.steps.size == data.totalSteps) {

            // Сохранить данные текущего состояния
            context.saveStateData(data)
            context.setTotalSteps(data.steps.size)

            if (llmResponse.userApprove == TaskMemKeys.TRUE) {
                // Есть явное подтверждение пользователя.
                handleUserAction(context, ACTION_PROCEED)
            } else {
                HandlerResult(
                    messages = chatMessages.withButton(
                        action = ACTION_PROCEED,
                        title = "К этапу 1",
                        messageText = buildUserTask(data)
                    )
                )
            }

        } else {
            context.saveStateData(data)
            chatMessages = if (nextState == TaskStateConfig.EXECUTION) {
                chatMessages.withInfo("Бот решил что этап выполнен, но не заполнил все данные.\n $data")
            } else {
                chatMessages
            }
            // Stay in INIT, return response to user
            HandlerResult(messages = chatMessages)
        }
    }

    private fun tryFillResultData(llmResponse: TaskLlmResponse): TaskStateData.Planning {
        val totalSteps = llmResponse.memoryUpdates[TaskMemKeys.PLAN_TOTAL_STAGES]?.toIntOrNull() ?: return TaskStateData.Planning(
            taskDetails = llmResponse.memoryUpdates[TaskMemKeys.PLAN_TASK_DETAILS] ?: ""
        )

        val steps: List<TaskStateData.TaskStep> = buildList {
            for (i in 1..totalSteps) {
                val name = llmResponse.memoryUpdates[TaskMemKeys.planStageName(i)] ?: ""
                val desc = llmResponse.memoryUpdates[TaskMemKeys.planStageDesc(i)] ?: ""
                val expert = llmResponse.memoryUpdates[TaskMemKeys.planStageExpert(i)] ?: ""
                if (name.isNotBlank() && desc.isNotBlank() && expert.isNotBlank()) {
                    add(TaskStateData.TaskStep(
                        stepNumber = i,
                        title = name,
                        description = desc,
                        expert = expert
                    ))
                } else {
                    break
                }
            }
        }
        return TaskStateData.Planning(totalSteps, steps)
    }

    override suspend fun handleUserAction(
        context: TaskContext,
        action: String
    ): HandlerResult {
        return if (action == ACTION_PROCEED) {
            val nextState = TaskStateConfig.EXECUTION
            val nextStep = 1
            // переключить состояние
            // TODO добавить проверку полноты данных
            context.updateState(nextState)
            // и шага для Execution
            context.setCurStepNum(nextStep)
            // сообщить о переходе и разбудить Llm
            HandlerResult(
                messages = emptyList<HandlerResult.Message>()
                    .withTitle(context.buildStateTransitionInfoMessage(nextStep, nextState)),
                // Будим Llm
                llmRequest = HandlerResult.LlmRequest()
            )
        } else {
            HandlerResult(
                messages = emptyList<HandlerResult.Message>().withBot("Неизвестная команда: $action")
            )
        }
    }

    private fun buildUserTask(data: TaskStateData.Planning): String {
        return buildString {
            append("Результаты планирования:\n")
            append("Steps: ${data.totalSteps}\n")

            data.steps.forEach { step ->
                append("${step.stepNumber}. Step\n")
                append("    Title: ${step.title}\n")
                append("    Description: ${step.description}\n")
                append("    Expert: ${step.expert}\n")
            }

            append("Add details: ${data.taskDetails}\n")
        }.trim()
    }
}
