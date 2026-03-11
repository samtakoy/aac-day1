package com.example.day.features.console.impl.domain.agents

import com.example.day.core.core_features.agent.domain.utils.ConsumptionCalculator
import com.example.day.core.core_features.agent.domain.workers.concrete.CompareWorker
import com.example.day.core.core_features.agent.domain.workers.concrete.McpWorker
import com.example.day.core.core_features.agent.domain.workers.concrete.PromptWorker
import com.example.day.core.core_features.agent.domain.workers.concrete.RejectWorker
import com.example.day.core.core_features.agent.domain.workers.concrete.SimpleWorker
import com.example.day.core.core_features.agent.domain.workers.concrete.StepWorker
import com.example.day.core.core_features.agent.domain.workers.concrete.TeamWorker
import com.example.day.core.core_features.agent.domain.workers.concrete.TalkWorker
import com.example.day.core.core_features.agent.domain.utils.trimCmd
import com.example.day.core.core_features.agent.domain.workers.base.AWorker
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.chat.domain.model.Chat
import javax.inject.Inject

/**
 * Основная цель: обработать команду пользователя - перенаправив ее к [AWorker]
 * Результаты [AWorker] выдать в чат
 * */
internal class AgMessageHandler @Inject constructor(
    simpleWorker: SimpleWorker,
    stepWorker: StepWorker,
    promptWorker: PromptWorker,
    teamWorker: TeamWorker,
    talkWorker: TalkWorker,
    mcpWorker: McpWorker,
    compareWorker: CompareWorker,
    private val rejectWorker: RejectWorker,
    private val consumptionCalculator: ConsumptionCalculator
) {

    private val commandToWorker: Map<ChatCommand, AWorker> = mapOf(
        ChatCommand.SimpleWork to simpleWorker,
        ChatCommand.StepWork to stepWorker,
        ChatCommand.PromptWork to promptWorker,
        ChatCommand.TeamWork to teamWorker,
        ChatCommand.Talk to talkWorker,
        ChatCommand.Mcp to mcpWorker,
        ChatCommand.Compare to compareWorker,
    )

    /** Обработка нового сообщения от пользователя.
     * Находит команду в сообщении [ChatCommand] и в зависимости от нее
     * запускает AWorker на обработку задания.
     *
     * @param userMessage сообщение от пользователя
     * @param chat настройки чата
     */
    suspend fun handleUserMessage(
        userMessage: String,
        chat: Chat
    ) {
        val trimmedMessage = userMessage.trim()

        // Маршрутизация к соответствующему worker
        for ((command, worker) in commandToWorker.entries) {
            if (trimmedMessage.startsWith(command.title, ignoreCase = true)) {
                val postProcessingEvents = mutableListOf<WorkerEvent>()
                worker.doWork(
                    userPrompt = trimmedMessage.substring(command.title.length).trimCmd(),
                    chat = chat,
                    // Технические события (RequestStart, RequestSuccess, RequestError) можно обрабатывать при необходимости
                    onEvent = { workerEvent ->
                        postProcessingEvents.add(workerEvent)
                    }
                )
                // Пост обработка событий
                postProcessingEvents.forEach { workerEvent -> consumptionCalculator.onWorkerEvent(chat, workerEvent) }
                return
            }
        }

        rejectWorker.doWork(
            userPrompt = trimmedMessage,
            chat = chat,
            onEvent = null
        )
    }
}
