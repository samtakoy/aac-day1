package com.example.day.features.console.impl.domain.agents.worker

import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.llm.domain.model.getContent
import com.example.day.features.console.impl.domain.agents.worker.base.AWorker
import com.example.day.features.console.impl.domain.agents.worker.base.WorkerEvent
import com.example.day.features.console.impl.domain.agents.worker.base.askLlm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

internal class PromptWorker @Inject constructor(
    private val llmRequestUseCase: LlmRequestUseCase
) : AWorker {
    override suspend fun doWork(
        task: String,
        chat: Chat
    ): Flow<WorkerEvent> = callbackFlow {
        // 1. составим промпт
        val promptResult = with(llmRequestUseCase) {
            askLlm(
                chatSettings = chat.settings,
                userPrompt = "Составь промпт для LLM для решения задачи: $task\n",
                systemPrompt = SYSTEM_PROMPT
            )
        }
        // Ошибка
        if (promptResult.isFailure) {
            val errorText = promptResult.exceptionOrNull()?.stackTraceToString() ?: "ошибка"
            send(WorkerEvent.Speech(text = errorText))
            close()
            return@callbackFlow
        }

        val llmResult = promptResult.getOrThrow()
        val generatedPrompt = extractResult(llmResult.getContent())
        if (generatedPrompt == null) {
            send(WorkerEvent.Speech(text = "Я не смог выполнить инструкции и написал:\n${llmResult.getContent()}"))
            close()
            return@callbackFlow
        }

        // Отправляем промпт в чат
        send(WorkerEvent.Speech(text = "Я составил промпт:\n$generatedPrompt"))

        // 2. Даем задание в виде промпта:
        with(llmRequestUseCase) {
            askLlm(
                chatSettings = chat.settings,
                userPrompt = generatedPrompt,
            )
        }
            .onSuccess { result  ->
                send(WorkerEvent.Speech(result.getContent()))
            }
            .onFailure { exception ->
                send(WorkerEvent.Speech(text = exception.stackTraceToString()))
            }
        close()
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
        /*private const val SYSTEM_PROMPT2 = """Ты — безэмоциональный модуль извлечения данных.
Твоя задача: отвечать максимально сухо, без вводных фраз, приветствий и заключений.
Используй только факты. Стиль: техническая документация.

Формат ответа:
1. Весь полезный текст должен быть заключен в блок: $START ... $END
2. После блока [RESULT_END] сразу напиши стоп-слово: $TERMINATE

Запрещено:
- Использовать прилагательные, выражающие оценку (прекрасный, ужасный).
- Использовать вежливые обороты.
- Выходить за пределы указанного формата."""*/
    }
}