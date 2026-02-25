package com.example.day.features.console.impl.domain.agents

import com.example.day.core.core_features.agent.domain.workers.CompareWorker
import com.example.day.core.core_features.agent.domain.workers.PromptWorker
import com.example.day.core.core_features.agent.domain.workers.RejectWorker
import com.example.day.core.core_features.agent.domain.workers.SimpleWorker
import com.example.day.core.core_features.agent.domain.workers.StepWorker
import com.example.day.core.core_features.agent.domain.workers.TeamWorker
import com.example.day.core.core_features.agent.domain.workers.TalkWorker
import com.example.day.core.core_features.agent.domain.utils.trimCmd
import com.example.day.core.core_features.agent.domain.workers.base.AWorker
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
    compareWorker: CompareWorker,
    private val rejectWorker: RejectWorker
) {

    private val commandToWorker: Map<ChatCommand, AWorker> = mapOf(
        ChatCommand.SimpleWork to simpleWorker,
        ChatCommand.StepWork to stepWorker,
        ChatCommand.PromptWork to promptWorker,
        ChatCommand.TeamWork to teamWorker,
        ChatCommand.Talk to talkWorker,
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
                worker.doWork(
                    task = trimmedMessage.substring(command.title.length).trimCmd(),
                    chat = chat,
                    onEvent = null // Технические события (RequestStart, RequestSuccess, RequestError) можно обрабатывать при необходимости
                )
                return
            }
        }

        rejectWorker.doWork(
            task = trimmedMessage,
            chat = chat,
            onEvent = null
        )
    }
}
