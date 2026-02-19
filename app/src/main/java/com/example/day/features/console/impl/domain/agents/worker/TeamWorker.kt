package com.example.day.features.console.impl.domain.agents.worker

import com.example.day.core.core_features.chat.domain.model.ChatSettings
import com.example.day.features.console.impl.domain.LlmRequestUseCase
import com.example.day.features.console.impl.domain.ModelConst
import com.example.day.features.console.impl.domain.model.ModelRequest
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
    ): Flow<String> = callbackFlow {

        val messageHistory = mutableListOf<ModelRequest.Message>()
        val experts = listOf(
            "АНАЛИТИК" to "определи требования и бизнес-логику",
            "ИНЖЕНЕР" to "предложи технический стек и архитектуру",
            "КРИТИК" to "найди слабые места в предложениях коллег и риски"
        )

        // 1. Опрос экспертов
        experts.forEach { (name, mission) ->
            send("--- 🧠 Выступает $name ---")

            val promptText = "Твоя роль: $name. Задача: $mission. " +
                    if (messageHistory.isNotEmpty()) "Учитывай мнение предыдущих экспертов. Контекст: $task" else "Контекст: $task"

            val response = askExpert(promptText, messageHistory)

            response.onSuccess { rawAnswer ->
                val result = extractResult(rawAnswer) ?: rawAnswer
                send(result)
                messageHistory.add(ModelRequest.Message(ModelRequest.Role.User, promptText))
                messageHistory.add(ModelRequest.Message(ModelRequest.Role.Assistant, rawAnswer))
            }
            delay(2000)
        }

        // 2. Финальное резюме от Менеджера
        send("--- 📋 МЕНЕДЖЕР ПРОЕКТА (Сводный план) ---")
        val managerPrompt = "Ты — Менеджер. Собери воедино мнения аналитика и инженера, " +
                "учти замечания критика и выдай финальный пошаговый план реализации задачи: $task"

        val finalResponse = askExpert(managerPrompt, messageHistory)

        finalResponse.onSuccess { raw ->
            // Логируем для отладки, если пусто
            val result = extractResult(raw)
            if (result != null) {
                send(result)
            } else {
                // Если совсем ничего не нашли в content, пробуем заглянуть в reasoning (крайний случай)
                send("Ошибка парсинга: ${raw.take(100)}...")
            }
        }.onFailure {
            send("Ошибка менеджера: ${it.message}")
        }
        close()
    }

    private suspend fun askExpert(prompt: String, history: List<ModelRequest.Message>) =
        llmRequestUseCase.exec(
            model = ModelConst.DEFAULT_MODEL,
            systemPrompt = TEAM_SYSTEM_PROMPT,
            messages = history,
            promptText = prompt
        )

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
