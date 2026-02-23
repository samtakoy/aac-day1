package com.example.day.features.console.impl.domain.agents.worker

import com.example.day.core.core_features.chat.domain.model.ChatSettings
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.llm.domain.model.LlmResult
import com.example.day.core.core_features.llm.domain.model.ModelRequest
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
    ): Flow<LlmResult> = callbackFlow {

        // История сообщений для поддержания контекста
        val messageHistory = mutableListOf<ModelRequest.Message>()

        var isFinished = false
        var cycleCount = 1
        var currentStep = 1

        // Первый запрос: ставим задачу
        var nextUserMessage = "Реши задачу пошагово. Каждый шаг пиши отдельно. " +
                "Если задача решена, напиши $DONE. \nЗадача: $task"

        while (!isFinished && cycleCount <= MAX_STEPS) {
            send(LlmResult(text = "--- Думаю над шагом №$currentStep ---", source = null))

            val response = llmRequestUseCase.exec(
                modelSettings = chatSettings.model,
                systemPrompt = STEP_BY_STEP_SYSTEM_PROMPT,
                messages = messageHistory,
                promptText = nextUserMessage
            )

            response.onSuccess { llmResult ->
                val rawAnswer = llmResult.text
                val results = extractResults(rawAnswer)

                if (results.isEmpty()) {
                    send(LlmResult(text = "Вот мой финальный ответ:\n$rawAnswer", source = llmResult.source))
                    close()
                    return@callbackFlow
                }

                // Отправляем промежуточные результаты в UI
                results.forEachIndexed { index, content ->
                    send(LlmResult(text = content, source = llmResult.source))
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
                    send(LlmResult(text = "✅ Задача завершена", source = llmResult.source))
                } else {
                    nextUserMessage = "Выполни следующий шаг или заверши работу тегом $DONE"
                    cycleCount++
                    send(LlmResult(text = "--- Думаю над шагом №$currentStep ---", source = null))
                    // на всякий случай уменьшим частоту ddos-атаки
                    delay(1000)
                }
            }.onFailure {
                send(LlmResult(text = "Ошибка на шаге $cycleCount: ${it.message}", source = null))
                isFinished = true
            }
        }

        if (cycleCount > MAX_STEPS) send(LlmResult(text = "⚠️ Превышен лимит шагов", source = null))
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
