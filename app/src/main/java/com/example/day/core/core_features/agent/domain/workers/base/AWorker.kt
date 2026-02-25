package com.example.day.core.core_features.agent.domain.workers.base

import com.example.day.core.core_features.chat.domain.model.Chat

/**
 * Interface for each type of command handler from ChatCommand.
 * @param onEvent callback for Worker events (nullable)
 * */
interface AWorker {
    suspend fun doWork(
        task: String,
        chat: Chat,
        onEvent: (suspend (WorkerEvent) -> Unit)? = null
    )
}
