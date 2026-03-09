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
 * INIT state handler.
 * Gathers requirements and determines expert role.
 */
class InitStateHandler : TaskStateHandler {
    companion object {
        private const val ACTION_PROCEED = "proceed_to_planning"
    }

    override val stateName: StateId = TaskStateConfig.INIT

    override suspend fun buildSystemPrompt(context: TaskContext): String = """
Ты — System Design Expert. Твоя задача — через опрос выявить потребность пользователя, собрать требования к решению и основные use cases (если есть), с какой проблемой он пришел, чего хочет достичь, пожелания к решению.

=== ПРОТОКОЛ ОТВЕТА ===
Ты должен отвечать строго ТОЛЬКО в формате JSON:
{
    "${TaskMemKeys.REPLY_TO_USER}": "твое сообщение пользователю",
    "${TaskMemKeys.MEM_UPDATES}": {
        "ключ_памяти": "значение"
    },
    "${TaskMemKeys.NEXT_STATE}": "${TaskMemKeys.PLANNING} или null",
    "${TaskMemKeys.USER_APPROVE}": "${TaskMemKeys.TRUE} или null"
}

=== ПРАВИЛА ФОРМАТИРОВАНИЯ JSON ===
• Весь ответ — это ОДИН валидный JSON-объект, без markdown, без пояснений до/после.
• Пример корректного ответа:
{  
    "${TaskMemKeys.REPLY_TO_USER}": "Привет!\nКакой бюджет вы рассматриваете?",
    "${TaskMemKeys.MEM_UPDATES}": { "title": "Подарок", "description": "Активный отдых", "goal": "Романтика", "expert": "Планировщик" },
    "${TaskMemKeys.NEXT_STATE}": null,
    "${TaskMemKeys.USER_APPROVE}": null
}

=== ПАМЯТЬ ===
Доступные ключи для сохранения в ${TaskMemKeys.MEM_UPDATES}:
- ${TaskMemKeys.INIT_TITLE} (короткое название задачи)
- ${TaskMemKeys.INIT_DESC} (подробное описание)
- ${TaskMemKeys.INIT_GOAL} (цель решения)
- ${TaskMemKeys.INIT_EXPERT} (роль эксперта который поможет в планировании этапов задачи)

=== ЗАПОЛНЕНИЕ ДАННЫХ ОТВЕТА ===
0. Свой ответ/вопрос на последнее сообщение пользователя клади в ${TaskMemKeys.REPLY_TO_USER}  
1. Если у тебя сформировалось понимание задачи, чего хочет достичь пользователь, какие способы достижения и ресурсы доступны, какой эксперт лучше всего поможет в дальнейшем планировани и данных достаточно для последующего планирования этапов,
 то заполни в ${TaskMemKeys.MEM_UPDATES} значения ${TaskMemKeys.INIT_TITLE}, ${TaskMemKeys.INIT_DESC}, ${TaskMemKeys.INIT_GOAL}, ${TaskMemKeys.INIT_EXPERT}
 и заполни ${TaskMemKeys.NEXT_STATE}: "${TaskMemKeys.PLANNING}".
 Важно: ${TaskMemKeys.INIT_DESC} должен быть полным и содержать всю информацию полученную от пользователя в процессе диалога, а также возможные инсайты для решения.
 Если в последнем сообщении пользователь явно и однозначно написал, что хочет перейти к планированию, то заполни дополнительно ${TaskMemKeys.USER_APPROVE}: "${TaskMemKeys.TRUE}".
2. Иначе не заполняй ${TaskMemKeys.NEXT_STATE} и значения ключей памяти
""".trimIndent()

    override suspend fun buildAssistantPreFillPrompt(context: TaskContext): String? {
        // НЕ УДАЛЯТЬ!
        val stateId = context.getState()
        // Пусть prefill ассистента будет всегда (пока)
        val isEmpty = true
        /* НЕ УДАЛЯТЬ!
        val isEmpty = if (stateId == null) {
            true
        } else {
            val stateData = context.getStateData(stateId, context.getCurStepNum()) as? TaskStateData.Init
            stateData?.history.isNullOrEmpty()
        }*/
        return if (isEmpty) {
            "Сейчас я поздороваюсь и спрошу у пользователя чем я могу ему помочь сегодня."
        } else null
    }

    override suspend fun handle(
        context: TaskContext,
        userInput: String,
        llmResponse: TaskLlmResponse
    ): HandlerResult {
        val nextState = llmResponse.nextState?.let {
            TaskStateConfig.config.toValidStateOrNull(it.lowercase())
        }
        val title = llmResponse.memoryUpdates[TaskMemKeys.INIT_TITLE] ?: ""
        val description = llmResponse.memoryUpdates[TaskMemKeys.INIT_DESC] ?: ""
        val goal = llmResponse.memoryUpdates[TaskMemKeys.INIT_GOAL] ?: ""
        val expert = llmResponse.memoryUpdates[TaskMemKeys.INIT_EXPERT] ?: ""

        var data = context.getStateData(TaskStateConfig.INIT, 1) as? TaskStateData.Init ?: TaskStateData.Init()
        data = data.copy(history = data.history.continueHistory(userInput, llmResponse.replyToUser))
        var chatMessages = listOf(
            HandlerResult.Message(llmResponse.replyToUser, false)
        )

        // TODO это условие верификации - сделать единообразно во всех хендлерах
        return if (
            nextState == TaskStateConfig.PLANNING &&
            title.isNotBlank() &&
            description.isNotBlank() &&
            goal.isNotBlank() &&
            expert.isNotBlank()
        ) {
            // Сохранить данные текущего состояния
            context.saveStateData(
                data.copy(title = title, description = description, goal = goal, expert = expert)
            )

            if (llmResponse.userApprove == TaskMemKeys.TRUE) {
                // Есть явное подтверждение пользователя.
                handleUserAction(context, ACTION_PROCEED)
            } else {
                HandlerResult(
                    messages = chatMessages.withButton(
                        action = ACTION_PROCEED,
                        title = "К планированию",
                        messageText = buildUserTask(title, description, goal, expert),
                    )
                )
            }
        } else {
            context.saveStateData(data)
            chatMessages = if (nextState == TaskStateConfig.PLANNING) {
                chatMessages.withInfo(
                    "Бот решил что этап выполнен, но не заполнил все данные.\n title:$title\n description:$description\n goal:$goal\n expert:$expert"
                )
            } else {
                chatMessages
            }
            // Stay in INIT, return response to user
            HandlerResult(messages = chatMessages)
        }
    }

    override suspend fun handleUserAction(
        context: TaskContext,
        action: String
    ) : HandlerResult {
        return if (action == ACTION_PROCEED) {
            val nextState = TaskStateConfig.PLANNING
            val nextStep = 1
            // переключить состояние
            // TODO добавить проверку полноты данных INIT
            context.updateState(nextState)
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

    private fun buildUserTask(title: String, description: String, goal: String, expert: String): String {
        return buildString {
            append("Результаты исследования:\n")
            if (title.isNotBlank()) append("Task: $title\n")
            if (description.isNotBlank()) append("Description: $description\n")
            if (goal.isNotBlank()) append("Goal: $goal\n")
            if (expert.isNotBlank()) append("Expert: $expert\n")
        }.trim()
    }

}
