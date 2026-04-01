package com.example.day.core.core_features.agent.domain.workers.task.states_config.handlers

import com.example.day.core.core_features.agent.domain.model.TaskLlmResponse
import com.example.day.core.core_features.agent.domain.workers.task.TaskMemKeys
import com.example.day.core.core_features.agent.domain.workers.task.TaskResponseParser
import com.example.day.core.core_features.agent.domain.workers.task.states_config.TaskStateConfig
import com.example.day.core.core_features.agent.domain.workers.task.states_config.TaskStateData
import com.example.day.core.core_features.agent.domain.workers.task.states_config.buildFinalResult
import com.example.day.core.core_features.agent.domain.workers.task.states_config.continueHistory
import com.example.day.core.core_features.agent.domain.workers.task.states_config.withBot
import com.example.day.core.core_features.agent.domain.workers.task.states_config.withButton
import com.example.day.core.core_features.state_machine.domain.StateContext
import com.example.day.core.core_features.state_machine.domain.HandlerResult
import com.example.day.core.core_features.state_machine.domain.TaskStateHandler
import com.example.day.core.core_features.state_machine.domain.model.StateId

/**
 * DONE state handler.
 * Finalizes task and generates summary report.
 */
class DoneStateHandler : TaskStateHandler {
    companion object {
        const val ACTION_PROCEED = "proceed_to_complete"
    }

    override val stateName: StateId = TaskStateConfig.DONE

    override suspend fun buildSystemPrompt(context: StateContext): String {

        return """Ты — Project Manager.
К нам пришел человек с потребностью решить его задачу. Артефакты решения ниже. 

=== АРТЕФАКТЫ ЗАДАЧИ ===
${context.buildFinalResult(withFeedbacks = true)}

== Инструкции ==
1. Собери итоговый отчёт на основе всех артефактов.
2. Выведи пользователю красивое полное резюме всей проделанной работы.
3. Предложи начать новую задачу.

=== ПРОТОКОЛ ОТВЕТА ===
Ты должен отвечать строго ТОЛЬКО в формате JSON:
{
    "${TaskMemKeys.REPLY_TO_USER}": "итоговый отчёт для пользователя"
}

=== ПРАВИЛА ФОРМАТИРОВАНИЯ JSON ===
• Весь ответ — это ОДИН валидный JSON-объект, без markdown, без пояснений до/после.
• Пример корректного ответа:
{
    "${TaskMemKeys.REPLY_TO_USER}": "## Итоговый отчёт\n\n### Задача:\nНазвание\n\n### Решение:\n[подробности]\n\n### Рекомендации:\n[советы]\n\nГотов помочь с новой задачей!"
}
""".trimIndent()
    }

    override suspend fun buildAssistantPreFillPrompt(context: StateContext): String? {
        return "Хорошо. Сейчас я соберу итоговое решение на основе артефактов решения задачи и отдам его пользователю в ключе ${TaskMemKeys.REPLY_TO_USER}"
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
        var data = context.getStateData(TaskStateConfig.DONE, 1) as? TaskStateData.Done ?: TaskStateData.Done()
        data = data.copy(
            history = data.history.continueHistory(userInput, llmResponse.replyToUser)
        )
        val chatMessages = listOf(
            HandlerResult.Message(llmResponse.replyToUser, false)
        )

        // Сохранить данные текущего состояния
        context.saveStateData(data)

        return HandlerResult(
            messages = chatMessages.withButton(
                action = ACTION_PROCEED,
                title = "Завершить",
                messageText = buildUserTask()
            )
        )
    }

    private suspend fun buildUserTask(): String {
        return "Задача решена"
    }

    override suspend fun handleUserAction(
        context: StateContext,
        action: String
    ): HandlerResult {
        return if (action == ACTION_PROCEED) {
            // Очищаем все про задачу
            context.clearTaskMemory()
            // сообщить о переходе и разбудить Llm
            HandlerResult(
                messages = emptyList<HandlerResult.Message>().withBot("Задача выполнена!")
            )
        } else {
            HandlerResult(
                messages = emptyList<HandlerResult.Message>().withBot("Неизвестная команда: $action")
            )
        }
    }
}
