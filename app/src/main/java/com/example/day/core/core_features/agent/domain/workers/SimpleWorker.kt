package com.example.day.core.core_features.agent.domain.workers

import com.example.day.core.core_features.agent.domain.workers.base.AWorker
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.agent.domain.workers.base.askLlm
import com.example.day.core.core_features.agent.domain.workers.tools.AgentTools
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.llm.domain.model.getContent
import javax.inject.Inject

internal class SimpleWorker @Inject constructor(
    private val llmRequestUseCase: LlmRequestUseCase,
    private val agentTools: AgentTools,
    private val chatTools: ChatTools
) : AWorker {
    override suspend fun doWork(
        task: String,
        chat: Chat,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        // Just execute the request and return the result
        llmRequestUseCase.askLlm(
            chatSettings = chat.settings,
            userPrompt = task,
            systemPrompt = "Ответ давай на русском языке.",
            onEvent = onEvent
        )
            .onSuccess { result ->
                chatTools.addBotMessage(chat.id, result.getContent())
            }
            .onFailure { exception ->
                chatTools.addBotMessage(chat.id, exception.stackTraceToString())
            }
    }
}
