package com.example.day.core.core_features.agent.domain.workers.concrete

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.workers.base.AWorker
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.agent.domain.workers.base.askLlm
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.getContent
import kotlinx.coroutines.delay
import javax.inject.Inject

class StepWorker @Inject constructor(
    private val llmRequestUseCase: LlmRequestUseCase,
    private val chatTools: ChatTools
) : AWorker {
    override suspend fun doWork(
        userPrompt: String,
        chat: Chat,
        userRole: AContextMessage.Role,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        // Message history for maintaining context
        val messageHistory = mutableListOf<ModelRequest.Message>()

        var isFinished = false
        var cycleCount = 1
        var currentStep = 1

        // First request: set the task
        var nextUserMessage = "Реши задачу пошагово. Каждый шаг пиши отдельно. " +
                "Если задача решена, напиши $DONE. \nЗадача: $userPrompt"

        while (!isFinished && cycleCount <= MAX_STEPS) {
            chatTools.addBotMessage(chat.id, "--- Думаю над шагом №$currentStep ---")
            val response = llmRequestUseCase.askLlm(
                model = chat.settings.model,
                prompt = AContextMessage(AContextMessage.Role.USER, nextUserMessage),
                systemPrompt = STEP_BY_STEP_SYSTEM_PROMPT,
                history = messageHistory,
                onEvent = onEvent
            )

            response.onSuccess { llmResult ->
                val rawAnswer = llmResult.getContent()
                val results = extractResults(rawAnswer)

                if (results.isEmpty()) {
                    chatTools.addBotMessage(chat.id, "Вот мой финальный ответ:\n$rawAnswer")
                    return
                }

                // Send intermediate results to chat
                results.forEach { content ->
                    chatTools.addBotMessage(chat.id, content)
                    currentStep++
                }

                // Save to history so LLM remembers what it answered
                messageHistory.add(
                    ModelRequest.Message(role = ModelRequest.Role.User, content = nextUserMessage)
                )
                messageHistory.add(
                    ModelRequest.Message(role = ModelRequest.Role.Assistant, content = rawAnswer)
                )

                if (rawAnswer.contains(DONE)) {
                    isFinished = true
                    chatTools.addBotMessage(chat.id, "✅ Задача завершена")
                } else {
                    nextUserMessage = "Выполни следующий шаг или заверши работу тегом $DONE"
                    cycleCount++
                    chatTools.addBotMessage(chat.id, "--- Думаю над шагом №$currentStep ---")
                    // Just in case reduce the frequency of ddos-attack
                    delay(1000)
                }
            }.onFailure {
                isFinished = true
            }
        }

        if (cycleCount > MAX_STEPS) chatTools.addBotMessage(chat.id, "⚠️ Превышен лимит шагов")
    }

    private fun extractResults(answer: String): List<String> {
        val regex = Regex("\\[RESULT_START\\](.*?)\\[RESULT_END\\]", RegexOption.DOT_MATCHES_ALL)
        return regex.findAll(answer).map { it.groupValues[1].trim() }.toList()
    }

    companion object {
        private const val START = "[RESULT_START]"
        private const val END = "[RESULT_END]"
        private const val DONE = "[TASK_DONE]"
        private const val MAX_STEPS = 5

        private const val STEP_BY_STEP_SYSTEM_PROMPT = """Ты — технический исполнитель.
 Решай задачу строго пошагово. Один ответ = один логический шаг.
 Ответ давай на русском языке.
 Если результат финальный — обязательно добавь тег $DONE.

 Формат:
 $START
 (описание шага или результат)
 $END"""
    }
}
