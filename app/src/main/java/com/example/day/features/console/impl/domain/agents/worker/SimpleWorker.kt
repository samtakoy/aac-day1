package com.example.day.features.console.impl.domain.agents.worker

import com.example.day.core.core_features.chat.domain.model.ChatSettings
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelResult
import com.example.day.core.core_features.llm.domain.model.getContent
import com.example.day.features.console.impl.domain.agents.worker.base.AWorker
import com.example.day.features.console.impl.domain.agents.worker.base.WorkerEvent
import com.example.day.features.console.impl.domain.agents.worker.base.askLlm
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

internal class SimpleWorker @Inject constructor(
    private val llmRequestUseCase: LlmRequestUseCase
) : AWorker {
    override suspend fun doWork(
        task: String,
        chatSettings: ChatSettings
    ): Flow<WorkerEvent> {
        return callbackFlow {
            // просто выполним запрос и вернем результат
            with(llmRequestUseCase) {
                askLlm(
                    chatSettings = chatSettings,
                    userPrompt = task,
                    systemPrompt = "Ответ давай на русском языке."
                )
                    .onSuccess { result ->
                        send(WorkerEvent.Speech(result.getContent()))
                    }
                    .onFailure { exception ->
                        send(
                            WorkerEvent.Speech(
                                text = exception.stackTraceToString()
                            )
                        )
                    }
            }
            close()
        }
    }
}