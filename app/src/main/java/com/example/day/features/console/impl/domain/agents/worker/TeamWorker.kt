package com.example.day.features.console.impl.domain.agents.worker

import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.model.ChatSettings
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelResult
import com.example.day.core.core_features.llm.domain.model.getContent
import com.example.day.features.console.impl.domain.agents.WorkerTools
import com.example.day.features.console.impl.domain.agents.worker.base.AWorker
import com.example.day.features.console.impl.domain.agents.worker.base.WorkerEvent
import com.example.day.features.console.impl.domain.agents.worker.base.askLlm
import kotlinx.coroutines.delay
import javax.inject.Inject

internal class TeamWorker @Inject constructor(
    private val llmRequestUseCase: LlmRequestUseCase,
    private val tools: WorkerTools
) : AWorker {

    override suspend fun doWork(
        task: String,
        chat: Chat,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        val messageHistory = mutableListOf<ModelRequest.Message>()
        val experts = listOf(
            "АНАЛИТИК" to "определи требования и бизнес-логику",
            "ИНЖЕНЕР" to "предложи технический стек и архитектуру",
            "КРИТИК" to "найди слабые места в предложениях коллег и риски"
        )

        // 1. Опрос экспертов
        experts.forEach { (name, mission) ->
            tools.addBotMessage(chat.id, "--- 🧠 Выступает $name ---")

            val promptText = "Твоя роль: $name. Задача: $mission. " +
                    if (messageHistory.isNotEmpty()) "Учитывай мнение предыдущих экспертов. Контекст: $task" else "Контекст: $task"

            askExpert(chat.settings, promptText, messageHistory, onEvent)
                .onSuccess { llmResult ->
                    val rawAnswer = llmResult.getContent()
                    val result = extractResult(rawAnswer) ?: rawAnswer
                    tools.addBotMessage(chat.id, result)
                    messageHistory.add(ModelRequest.Message(ModelRequest.Role.User, promptText))
                    messageHistory.add(ModelRequest.Message(ModelRequest.Role.Assistant, rawAnswer))
                }
                .onFailure {
                    tools.addBotMessage(chat.id, "что-то пошло не так: ${it.message}")
                }
            delay(2000)
        }

        // 2. Финальное резюме от Менеджера
        tools.addBotMessage(chat.id, "--- 📋 МЕНЕДЖЕР ПРОЕКТА (Сводный план) ---")
        val managerPrompt = "Ты — Менеджер. Собери воедино мнения аналитика и инженера, " +
                "учти замечания критика и выдай финальный пошаговый план реализации задачи: $task"

        val finalResponse = askExpert(chat.settings, managerPrompt, messageHistory, onEvent)

        finalResponse.onSuccess { llmResult ->
            // Логируем для отладки, если пусто
            val raw = llmResult.getContent()
            val result = extractResult(raw)
            if (result != null) {
                tools.addBotMessage(chat.id, result)
            } else {
                // Если совсем ничего не нашли в content, пробуем заглянуть в reasoning (крайний случай)
                tools.addBotMessage(chat.id, "Ошибка парсинга: ${raw.take(100)}...")
            }
        }.onFailure {
            tools.addBotMessage(chat.id, "Ошибка менеджера: ${it.message}")
        }
    }

    private suspend fun askExpert(
        chatSettings: ChatSettings,
        prompt: String,
        history: List<ModelRequest.Message>,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ): Result<ModelResult.Success> {
        return llmRequestUseCase.askLlm(
            chatSettings = chatSettings,
            userPrompt = prompt,
            systemPrompt = TEAM_SYSTEM_PROMPT,
            history = history,
            onEvent = onEvent
        )
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
    }
}
