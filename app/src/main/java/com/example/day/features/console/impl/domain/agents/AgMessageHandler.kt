package com.example.day.features.console.impl.domain.agents

import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.features.console.impl.domain.agents.worker.base.AWorker
import com.example.day.features.console.impl.domain.agents.worker.PromptWorker
import com.example.day.features.console.impl.domain.agents.worker.SimpleWorker
import com.example.day.features.console.impl.domain.agents.worker.StepWorker
import com.example.day.features.console.impl.domain.agents.worker.TeamWorker
import com.example.day.features.console.impl.domain.agents.worker.TalkWorker
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
    compareWorker: CompareWorker
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
     * @param tools доступные операции с чатом
     * Например, "Команда не распознана" или "Решение вашей задачи..."
     */
    suspend fun handleUserMessage(
        userMessage: String,
        chat: Chat,
        tools: WorkerTools
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

        tools.addBotMessage(chat.id, "Команда не распознана")
    }
}
