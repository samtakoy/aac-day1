package com.example.day.features.console.impl.domain.agents.worker

import com.example.day.features.console.impl.domain.model.ChatSettings
import kotlinx.coroutines.flow.Flow

/** Интерфейс для каждого типа обработчика команды из [com.example.day.features.console.impl.domain.agents.ChatCommand]
 * @return Flow ответов LLM
 * */
interface AWorker {
    suspend fun doWork(task: String, chatSettings: ChatSettings): Flow<String>
}