package com.example.day.features.console.impl.domain.agents.worker

import com.example.day.core.core_features.agent.domain.model.addAssistantMessage
import com.example.day.core.core_features.agent.domain.model.addUserMessage
import com.example.day.core.core_features.agent.domain.model.toModelRequestMessages
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.llm.domain.model.getContent
import com.example.day.features.console.impl.domain.agents.WorkerTools
import com.example.day.features.console.impl.domain.agents.worker.base.AWorker
import com.example.day.features.console.impl.domain.agents.worker.base.WorkerEvent
import com.example.day.features.console.impl.domain.agents.worker.base.askLlm
import javax.inject.Inject

/**
 * Agent with context support (Context Management).
 * Saves message history between requests to database.
 */
internal class TalkWorker @Inject constructor(
    private val llmRequestUseCase: LlmRequestUseCase,
    private val tools: WorkerTools
) : AWorker {

    companion object {
        const val AGENT_NAME = "talk_agent"
    }

    override suspend fun doWork(
        task: String,
        chat: Chat,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        // 0. Get or create agent instance
        val agent = tools.getOrCreateAgent(
            systemName = AGENT_NAME,
            chatId = chat.id,
            isCommonAgent = false
        )

        // 1. Get agent context by agentId
        val context = tools.getContext(agent.id)

        // 2. Prepare message history for LLM
        val history = context.messages.toModelRequestMessages()

        // 3. Request to LLM with context
        llmRequestUseCase.askLlm(
            chatSettings = chat.settings,
            userPrompt = task,
            systemPrompt = chat.settings.systemPromt,
            history = history,
            onEvent = onEvent
        ).onSuccess { result ->
            val content = result.getContent()

            // 4. Save messages to context (using agentId)
            val updatedContext = context
                .addUserMessage(task)
                .addAssistantMessage(content)
            tools.saveContext(agent.id, updatedContext)

            // 5. Send result to chat
            tools.addBotMessage(chat.id, content)
        }.onFailure { exception ->
            tools.addBotMessage(chat.id, exception.stackTraceToString())
        }
    }
}
