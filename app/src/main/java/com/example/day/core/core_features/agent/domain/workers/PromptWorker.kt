package com.example.day.core.core_features.agent.domain.workers

import com.example.day.core.core_features.agent.domain.workers.base.AWorker
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.agent.domain.workers.base.askLlm
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.llm.domain.model.getContent
import javax.inject.Inject

internal class PromptWorker @Inject constructor(
    private val llmRequestUseCase: LlmRequestUseCase,
    private val chatTools: ChatTools
) : AWorker {
    override suspend fun doWork(
        task: String,
        chat: Chat,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        // 1. Compose the prompt
        val promptResult = llmRequestUseCase.askLlm(
            chatSettings = chat.settings,
            userPrompt = "Составь промпт для LLM для решения задачи: $task\n",
            systemPrompt = SYSTEM_PROMPT,
            onEvent = onEvent
        )
        // Error
        if (promptResult.isFailure) {
            val errorText = promptResult.exceptionOrNull()?.stackTraceToString() ?: "ошибка"
            chatTools.addBotMessage(chat.id, errorText)
            return
        }

        val llmResult = promptResult.getOrThrow()
        val generatedPrompt = extractResult(llmResult.getContent())
        if (generatedPrompt == null) {
            chatTools.addBotMessage(chat.id, "Я не смог выполнить инструкции и написал:\n${llmResult.getContent()}")
            return
        }

        // Send the prompt to chat
        chatTools.addBotMessage(chat.id, "Я составил промпт:\n$generatedPrompt")

        // 2. Give the task as a prompt:
        llmRequestUseCase.askLlm(
            chatSettings = chat.settings,
            userPrompt = generatedPrompt,
            onEvent = onEvent
        )
            .onSuccess { result  ->
                chatTools.addBotMessage(chat.id, result.getContent())
            }
            .onFailure { exception ->
                chatTools.addBotMessage(chat.id, exception.stackTraceToString())
            }
    }

    private fun extractResult(answer: String): String? {
        return if (answer.contains(START) && answer.contains(END)) {
            answer.substringAfter(START)
                .substringBefore(END)
                .trim()
        } else {
            null
        }
    }

    companion object {
        private const val START = "[RESULT_START]"
        private const val END = "[RESULT_END]"
        private const val TERMINATE = "[TERMINATE_STREAM]"
        private const val SYSTEM_PROMPT = """Ты — автономный скрипт обработки данных.
 ЗАПРЕЩЕНО: Использовать внутренние блоки рассуждений (thought/reasoning), писать приветствия или пояснения. 
 ОБЯЗАТЕЛЬНО: Весь вывод должен быть направлен строго в поле основного ответа (content).
 Ответ давай на русском языке.

 Формат ответа:
 $START
 (Твой текст)
 $END
 $TERMINATE"""
    }
}
