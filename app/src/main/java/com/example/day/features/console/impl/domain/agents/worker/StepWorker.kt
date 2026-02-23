package com.example.day.features.console.impl.domain.agents.worker

import com.example.day.core.core_features.chat.domain.model.ChatSettings
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.getContent
import com.example.day.features.console.impl.domain.agents.worker.base.AWorker
import com.example.day.features.console.impl.domain.agents.worker.base.WorkerEvent
import com.example.day.features.console.impl.domain.agents.worker.base.askLlm
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

internal class StepWorker @Inject constructor(
    private val llmRequestUseCase: LlmRequestUseCase
) : AWorker {
    override suspend fun doWork(
        task: String,
        chatSettings: ChatSettings
    ): Flow<WorkerEvent> = callbackFlow {

        // История сообщений для поддержания контекста
        val messageHistory = mutableListOf<ModelRequest.Message>()

        var isFinished = false
        var cycleCount = 1
        var currentStep = 1

        // Первый запрос: ставим задачу
        var nextUserMessage = "Реши задачу пошагово. Каждый шаг пиши отдельно. " +
                "Если задача решена, напиши $DONE. \nЗадача: $task"

        while (!isFinished && cycleCount <= MAX_STEPS) {
            send(WorkerEvent.Speech("--- Думаю над шагом №$currentStep ---"))
            val response = with(llmRequestUseCase) {
                askLlm(
                    chatSettings = chatSettings,
                    userPrompt = nextUserMessage,
                    systemPrompt = STEP_BY_STEP_SYSTEM_PROMPT,
                    history = messageHistory
                )
            }

            response.onSuccess { llmResult ->
                val rawAnswer = llmResult.getContent()
                val results = extractResults(rawAnswer)

                if (results.isEmpty()) {
                    send(WorkerEvent.Speech(text = "Вот мой финальный ответ:\n$rawAnswer"))
                    close()
                    return@callbackFlow
                }

                // Отправляем промежуточные результаты в UI
                results.forEach { content ->
                    send(WorkerEvent.Speech(text = content))
                    currentStep++
                }

                // Сохраняем в историю, чтобы LLM помнила, что она ответила
                messageHistory.add(
                    ModelRequest.Message(role = ModelRequest.Role.User, content = nextUserMessage)
                )
                messageHistory.add(
                    ModelRequest.Message(role = ModelRequest.Role.Assistant, content = rawAnswer)
                )

                if (rawAnswer.contains(DONE)) {
                    isFinished = true
                    send(WorkerEvent.Speech(text = "✅ Задача завершена"))
                } else {
                    nextUserMessage = "Выполни следующий шаг или заверши работу тегом $DONE"
                    cycleCount++
                    send(WorkerEvent.Speech(text = "--- Думаю над шагом №$currentStep ---"))
                    // на всякий случай уменьшим частоту ddos-атаки
                    delay(1000)
                }
            }.onFailure {
                isFinished = true
            }
        }

        if (cycleCount > MAX_STEPS) send(WorkerEvent.Speech(text = "⚠️ Превышен лимит шагов"))
        close()
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
