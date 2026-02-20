package com.example.day.features.console.impl.domain.agents.worker

import com.example.day.features.console.impl.domain.model.ChatSettings
import com.example.day.features.console.impl.domain.LlmRequestUseCase
import com.example.day.features.console.impl.domain.model.ModelRequest
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
    ): Flow<String> = callbackFlow {

        // История сообщений для поддержания контекста
        val messageHistory = mutableListOf<ModelRequest.Message>()

        var isFinished = false
        var cycleCount = 1
        var currentStep = 1

        // Первый запрос: ставим задачу
        var nextUserMessage = "Реши задачу пошагово. Каждый шаг пиши отдельно. " +
                "Если задача решена, напиши $DONE. \nЗадача: $task"

        while (!isFinished && cycleCount <= MAX_STEPS) {
            send("--- Думаю над шагом №$currentStep ---")

            val response = llmRequestUseCase.exec(
                modelSettings = chatSettings.model,
                systemPrompt = STEP_BY_STEP_SYSTEM_PROMPT,
                messages = messageHistory,
                promptText = nextUserMessage
            )

            response.onSuccess { rawAnswer ->
                val results = extractResults(rawAnswer)

                if (results.isEmpty()) {
                    send("Вот мой финальный ответ:\n$rawAnswer")
                    close()
                    return@callbackFlow
                }

                // Отправляем промежуточные результаты в UI
                results.forEachIndexed { index, content ->
                    send(content)
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
                    send("✅ Задача завершена")
                } else {
                    nextUserMessage = "Выполни следующий шаг или заверши работу тегом $DONE"
                    cycleCount++
                    send("--- Думаю над шагом №$currentStep ---")
                    // на всякий случай уменьшим частоту ddos-атаки
                    delay(1000)
                }
            }.onFailure {
                send("Ошибка на шаге $cycleCount: ${it.message}")
                isFinished = true
            }
        }

        if (cycleCount > MAX_STEPS) send("⚠️ Превышен лимит шагов")
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
