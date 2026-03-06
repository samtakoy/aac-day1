package com.example.day.core.core_features.agent.domain.workers.concrete

import com.example.day.core.core_features.agent.domain.workers.base.AWorker
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import javax.inject.Inject

/**
 * Worker that handles unrecognized commands.
 * Responds with "Команда не распознана" message.
 */
class RejectWorker @Inject constructor(
    private val chatTools: ChatTools
) : AWorker {
    override suspend fun doWork(
        userPrompt: String,
        chat: Chat,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        chatTools.addBotMessage(chat.id, "Команда не распознана")
    }
}
