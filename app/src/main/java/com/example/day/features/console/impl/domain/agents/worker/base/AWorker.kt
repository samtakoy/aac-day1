package com.example.day.features.console.impl.domain.agents.worker.base

import com.example.day.core.core_features.chat.domain.model.ChatSettings
import kotlinx.coroutines.flow.Flow

/** Интерфейс для каждого типа обработчика команды из [com.example.day.features.console.impl.domain.agents.ChatCommand]
 * @return Flow ответов LLM
 * */
internal interface AWorker {
    suspend fun doWork(task: String, chatSettings: ChatSettings): Flow<WorkerEvent>
}