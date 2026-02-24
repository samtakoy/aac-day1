package com.example.day.features.console.impl.domain.agents.worker

import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.llm.domain.model.getContent
import com.example.day.features.console.impl.domain.agents.WorkerTools
import com.example.day.features.console.impl.domain.agents.worker.base.AWorker
import com.example.day.features.console.impl.domain.agents.worker.base.WorkerEvent
import com.example.day.features.console.impl.domain.agents.worker.base.askLlm
import javax.inject.Inject

internal class SimpleWorker @Inject constructor(
    private val llmRequestUseCase: LlmRequestUseCase,
    private val tools: WorkerTools
) : AWorker {
    override suspend fun doWork(
        task: String,
        chat: Chat,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        // просто выполним запрос и вернем результат
        llmRequestUseCase.askLlm(
            chatSettings = chat.settings,
            userPrompt = task,
            systemPrompt = "Ответ давай на русском языке.",
            onEvent = onEvent
        )
            .onSuccess { result ->
                tools.addBotMessage(chat.id, result.getContent())
            }
            .onFailure { exception ->
                tools.addBotMessage(chat.id, exception.stackTraceToString())
            }
    }
}
