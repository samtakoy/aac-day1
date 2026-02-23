package com.example.day.features.console.impl.domain.agents.worker

import com.example.day.core.core_features.chat.domain.model.ChatSettings
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.llm.domain.model.LlmResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

internal class PromptWorker @Inject constructor(
    private val llmRequestUseCase: LlmRequestUseCase
) : AWorker {
    override suspend fun doWork(
        task: String,
        chatSettings: ChatSettings
    ): Flow<LlmResult> {
        return callbackFlow {
            // 1. составим промпт
            val promptResult = llmRequestUseCase.exec(
                modelSettings = chatSettings.model,
                systemPrompt = SYSTEM_PROMPT,
                messages = emptyList(),
                promptText = "Составь промпт для LLM для решения задачи: $task\n",
                // текущая LLM со стоп словом перестает отвечать - пока без него
                //stopSequence = listOf(TERMINATE)
            )
            // Ошибка
            if (promptResult.isFailure) {
                val errorText = promptResult.exceptionOrNull()?.stackTraceToString() ?: "ошибка"
                send(LlmResult(text = errorText, source = null))
                close()
                return@callbackFlow
            }

            val llmResult = promptResult.getOrThrow()
            val generatedPrompt = extractResult(llmResult.text)
            if (generatedPrompt == null) {
                send(LlmResult(text = "Я не смог выполнить инструкции и написал:\n${llmResult.text}", source = llmResult.source))
                close()
                return@callbackFlow
            }

            // Отправляем промпт в чат
            send(LlmResult(text = "Я составил промпт:\n$generatedPrompt", source = llmResult.source))

            // 2. Даем задание в виде промпта:
            val result = llmRequestUseCase.exec(
                modelSettings = chatSettings.model,
                systemPrompt = null,
                messages = emptyList(),
                promptText = generatedPrompt,
            )
                .onSuccess { result  ->
                    send(result)
                }
                .onFailure { exception ->
                    send(LlmResult(text = exception.stackTraceToString(), source = null))
                }
            close()
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