package com.example.day.core.core_features.agent.domain.workers.base

import com.example.day.core.core_features.chat.domain.model.ChatSettings
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelResult

/**
 * Extension function for LlmRequestUseCase that sends a request to LLM 
 * and notifies about events via onEvent.
 */
internal suspend fun LlmRequestUseCase.askLlm(
    chatSettings: ChatSettings,
    userPrompt: String,
    systemPrompt: String? = null,
    history: List<ModelRequest.Message> = emptyList(),
    onEvent: (suspend (WorkerEvent) -> Unit)? = null
): Result<ModelResult.Success> {
    onEvent?.invoke(WorkerEvent.RequestStart)
    return exec(
        modelSettings = chatSettings.model,
        systemPrompt = systemPrompt,
        messages = history,
        promptText = userPrompt,
    ).onSuccess {
        onEvent?.invoke(WorkerEvent.RequestSuccess(it))
    }.onFailure {
        onEvent?.invoke(WorkerEvent.RequestError(it.message ?: "some error"))
    }
}
