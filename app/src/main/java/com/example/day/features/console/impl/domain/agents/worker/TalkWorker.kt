package com.example.day.features.console.impl.domain.agents.worker

import com.example.day.core.core_features.agent.domain.model.AContextOwner
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

/**
 * Агент с поддержкой контекста (Context Management).
 * Сохраняет историю сообщений между запросами.
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
        chat: Chat
    ): Flow<WorkerEvent> = callbackFlow {
        // 1. Получить контекст агента
        val context = tools.getContext(AGENT_NAME)

        // 2. Подготовить историю сообщений для LLM
        val history = context.messages.toModelRequestMessages()

        // 3. Запрос к LLM с контекстом
        with(llmRequestUseCase) {
            askLlm(
                chatSettings = chat.settings,
                userPrompt = task,
                systemPrompt = chat.settings.systemPromt, // из ChatSettings TODO - не правильно - надо из контекста
                history = history
            )
        }.onSuccess { result ->
            val content = result.getContent()

            // 4. Сохранить сообщения в контекст
            val updatedContext = context
                .addUserMessage(task)
                .addAssistantMessage(content)
            tools.saveContext(updatedContext)

            // 5. Отправить результат
            send(WorkerEvent.Speech(content))
        }.onFailure { exception ->
            send(WorkerEvent.Speech(exception.stackTraceToString()))
        }

        close()
    }
}
