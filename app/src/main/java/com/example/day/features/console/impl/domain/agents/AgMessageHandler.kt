package com.example.day.features.console.impl.domain.agents

import com.example.day.core.core_features.chat.domain.model.ChatSettings
import com.example.day.features.console.impl.domain.agents.worker.AWorker
import com.example.day.features.console.impl.domain.agents.worker.PromptWorker
import com.example.day.features.console.impl.domain.agents.worker.SimpleWorker
import com.example.day.features.console.impl.domain.agents.worker.StepWorker
import com.example.day.features.console.impl.domain.agents.worker.TeamWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

internal class AgMessageHandler @Inject constructor(
    simpleWorker: SimpleWorker,
    stepWorker: StepWorker,
    promptWorker: PromptWorker,
    teamWorker: TeamWorker
) {

    private val commandToWorker: Map<ChatCommand, AWorker> = mapOf(
        ChatCommand.SimpleWork to simpleWorker,
        ChatCommand.StepWork to stepWorker,
        ChatCommand.PromptWork to promptWorker,
        ChatCommand.TeamWork to teamWorker,
    )

    /** Обработка нового сообщения от пользователя.
     * Находит команду в сообщении [ChatCommand] и в зависимости от нее
     * запускает AWorker на обработку задания.
     *
     * @param userMessage сообщение от пользователя
     * @return Flow ответов LLM в чат.
     * Например, "Команда не распознана" или "Решение вашей задачи..."
     */
    suspend fun handleUserMessage(userMessage: String, chatSettings: ChatSettings): Flow<String> {

        val userMessage = userMessage.trim()
        for((command, worker) in commandToWorker.entries) {
            if (userMessage.startsWith(command.title, ignoreCase = true)) {
                return worker.doWork(
                    userMessage.substring(command.title.length),
                    chatSettings
                )
            }
        }
        return flowOf("Команда не распознана")
    }
}