package com.example.day.features.console.impl.domain.agents.worker

import com.example.day.core.core_features.chat.domain.model.ChatSettings
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.llm.domain.model.LlmResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

internal class SimpleWorker @Inject constructor(
    private val llmRequestUseCase: LlmRequestUseCase
) : AWorker {
    override suspend fun doWork(
        task: String,
        chatSettings: ChatSettings
    ): Flow<LlmResult> {
        return callbackFlow {
            // просто выполним запрос и вернем результат
            llmRequestUseCase.exec(
                modelSettings = chatSettings.model,
                systemPrompt = "Ответ давай на русском языке.",
                messages = emptyList(),
                promptText = task
            )
                .onSuccess { result ->
                    send(result)
                }
                .onFailure { exception ->
                    send(LlmResult(
                        text = exception.stackTraceToString(),
                        source = null
                    ))
                }
            close()
        }
    }
}