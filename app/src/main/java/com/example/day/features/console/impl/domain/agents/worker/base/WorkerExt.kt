package com.example.day.features.console.impl.domain.agents.worker.base

import com.example.day.core.core_features.chat.domain.model.ChatSettings
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelResult
import kotlinx.coroutines.channels.ProducerScope

context(requestUseCase: LlmRequestUseCase)
internal suspend fun ProducerScope<WorkerEvent>.askLlm(
    chatSettings: ChatSettings,
    userPrompt: String,
    systemPrompt: String? = null,
    history: List<ModelRequest.Message> = emptyList()
): Result<ModelResult.Success> {
    send(WorkerEvent.RequestStart)
    return requestUseCase.exec(
        modelSettings = chatSettings.model,
        systemPrompt = systemPrompt,
        messages = history,
        promptText = userPrompt,
    ).onSuccess {
        send(WorkerEvent.RequestSuccess(it))
    }.onFailure {
        send(WorkerEvent.RequestError(it.message ?: "some error"))
    }
}
