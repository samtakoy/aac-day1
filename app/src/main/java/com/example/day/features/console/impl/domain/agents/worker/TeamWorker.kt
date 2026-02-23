package com.example.day.features.console.impl.domain.agents.worker

import com.example.day.core.core_features.chat.domain.model.ChatSettings
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelResult
import com.example.day.core.core_features.llm.domain.model.getContent
import com.example.day.features.console.impl.domain.agents.worker.base.AWorker
import com.example.day.features.console.impl.domain.agents.worker.base.WorkerEvent
import com.example.day.features.console.impl.domain.agents.worker.base.askLlm
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

internal class TeamWorker @Inject constructor(
    private val llmRequestUseCase: LlmRequestUseCase
) : AWorker {

    override suspend fun doWork(
        task: String,
        chatSettings: ChatSettings
    ): Flow<WorkerEvent> = callbackFlow {

        val messageHistory = mutableListOf<ModelRequest.Message>()
        val experts = listOf(
            "АНАЛИТИК" to "определи требования и бизнес-логику",
            "ИНЖЕНЕР" to "предложи технический стек и архитектуру",
            "КРИТИК" to "найди слабые места в предложениях коллег и риски"
        )

        // 1. Опрос экспертов
        experts.forEach { (name, mission) ->
            send(WorkerEvent.Speech(text = "--- 🧠 Выступает $name ---"))

            val promptText = "Твоя роль: $name. Задача: $mission. " +
                    if (messageHistory.isNotEmpty()) "Учитывай мнение предыдущих экспертов. Контекст: $task" else "Контекст: $task"

            askExpert(chatSettings, promptText, messageHistory)
                .onSuccess { llmResult ->
                    val rawAnswer = llmResult.getContent()
                    val result = extractResult(rawAnswer) ?: rawAnswer
                    send(WorkerEvent.Speech(text = result))
                    messageHistory.add(ModelRequest.Message(ModelRequest.Role.User, promptText))
                    messageHistory.add(ModelRequest.Message(ModelRequest.Role.Assistant, rawAnswer))
                }
                .onFailure {
                    send(WorkerEvent.Speech("что-то пошло не так: ${it.message}"))
                    close()
                }
            delay(2000)
        }

        // 2. Финальное резюме от Менеджера
        send(WorkerEvent.Speech(text = "--- 📋 МЕНЕДЖЕР ПРОЕКТА (Сводный план) ---"))
        val managerPrompt = "Ты — Менеджер. Собери воедино мнения аналитика и инженера, " +
                "учти замечания критика и выдай финальный пошаговый план реализации задачи: $task"

        val finalResponse = askExpert(chatSettings, managerPrompt, messageHistory)

        finalResponse.onSuccess { llmResult ->
            // Логируем для отладки, если пусто
            val raw = llmResult.getContent()
            val result = extractResult(raw)
            if (result != null) {
                send(WorkerEvent.Speech(text = result))
            } else {
                // Если совсем ничего не нашли в content, пробуем заглянуть в reasoning (крайний случай)
                send(WorkerEvent.Speech(text = "Ошибка парсинга: ${raw.take(100)}..."))
            }
        }.onFailure {
            send(WorkerEvent.Speech(text = "Ошибка менеджера: ${it.message}"))
        }
        close()
    }

    private suspend fun ProducerScope<WorkerEvent>.askExpert(
        chatSettings: ChatSettings,
        prompt: String,
        history: List<ModelRequest.Message>
    ): Result<ModelResult.Success> {
        return with(llmRequestUseCase) {
            askLlm(
                chatSettings = chatSettings,
                userPrompt = prompt,
                systemPrompt = TEAM_SYSTEM_PROMPT,
                history = history
            )
        }
    }

    private fun extractResult(answer: String): String? {
        val regex = Regex("\\[RESULT_START\\](.*)\\[RESULT_END\\]", RegexOption.DOT_MATCHES_ALL)
        val match = regex.find(answer)?.groupValues?.get(1)?.trim()

        // Если тегов нет, но текст явно полезный (не пустой), возвращаем как есть
        return match ?: if (answer.isNotBlank() && !answer.contains("reasoning")) answer.trim() else null
    }

    companion object {
        private const val START = "[RESULT_START]"
        private const val END = "[RESULT_END]"

        private const val TEAM_SYSTEM_PROMPT = """Ты — узкопрофильный эксперт.
ЗАПРЕЩЕНО: Рассуждать вслух, объяснять ход своих мыслей или использовать блоки типа <thought> или reasoning.
ЗАПРЕЩЕНО: Повторять одно и то же описание более одного раза.
ОБЯЗАТЕЛЬНО: Выдавай только конечный технический результат.
Ответ давай на русском языке.

Формат:
$START
(конкретный список технологий или шагов)
$END"""

            /*"""Ты — эксперт в составе команды.
Твоя задача: дать максимально краткий, технический и сухой ответ.
Никаких вводных слов типа "Как инженер, я считаю...". Только суть.
Формат вывода:
$START
(текст ответа)
$END""" */
    }
}
