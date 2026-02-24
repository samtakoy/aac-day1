package com.example.day.features.console.impl.domain.agents.worker.base

import com.example.day.core.core_features.chat.domain.model.Chat

/** Интерфейс для каждого типа обработчика команды из [com.example.day.features.console.impl.domain.agents.ChatCommand]
 * @param onEvent колбэк для событий Worker (nullable)
 * */
internal interface AWorker {
    suspend fun doWork(
        task: String,
        chat: Chat,
        onEvent: (suspend (WorkerEvent) -> Unit)? = null
    )
}