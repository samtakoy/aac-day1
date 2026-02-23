package com.example.day.features.console.impl.domain.agents

import com.example.day.core.core_features.chat.domain.model.ChatMessageStatus
import com.example.day.core.core_features.chat.domain.model.ChatSettings
import com.example.day.core.core_features.chat.domain.model.UserType
import com.example.day.core.core_features.chat.domain.usecase.AddChatMessageUseCase
import com.example.day.core.core_features.llm.domain.model.LlmResult
import com.example.day.features.console.impl.domain.agents.model.HandlerResponse
import com.example.day.features.console.impl.domain.agents.worker.AWorker
import com.example.day.features.console.impl.domain.agents.worker.PromptWorker
import com.example.day.features.console.impl.domain.agents.worker.SimpleWorker
import com.example.day.features.console.impl.domain.agents.worker.StepWorker
import com.example.day.features.console.impl.domain.agents.worker.TeamWorker
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class AgMessageHandler @Inject constructor(
    simpleWorker: SimpleWorker,
    stepWorker: StepWorker,
    promptWorker: PromptWorker,
    teamWorker: TeamWorker, // TODO избавиться тут от use case
    private val addChatMessageUseCase: AddChatMessageUseCase
) {

    private val commandToWorker: Map<ChatCommand, AWorker> = mapOf(
        ChatCommand.SimpleWork to simpleWorker,
        ChatCommand.StepWork to stepWorker,
        ChatCommand.PromptWork to promptWorker,
        ChatCommand.TeamWork to teamWorker,
    )

    /** Обработка нового сообщения от пользователя.
     * Находит команду в сообщении [ChatCommand] и в зависимости от нее
     * запускает AWorker на обработку задания.
     *
     * @param userMessage сообщение от пользователя
     * @param chatSettings настройки чата
     * @param createChat функция создания нового чата, принимает имя модели и возвращает Pair<chatId, chatName>
     * @return Flow ответов LLM в чат в виде [HandlerResponse].
     * Например, "Команда не распознана" или "Решение вашей задачи..."
     */
    suspend fun handleUserMessage(
        userMessage: String,
        chatSettings: ChatSettings,
        createChat: suspend (String) -> Pair<Long, String>
    ): Flow<HandlerResponse> {
        val userMessage = userMessage.trim()

        if (userMessage.startsWith(ChatCommand.Compare.title, ignoreCase = true)) {
            val inputWithoutCommand = userMessage
                .substring(ChatCommand.Compare.title.length)
                .trimCmd()
            return handleCompareCommand(inputWithoutCommand, chatSettings, createChat)
        }

        for((command, worker) in commandToWorker.entries) {
            if (userMessage.startsWith(command.title, ignoreCase = true)) {
                return worker.doWork(
                    userMessage.substring(command.title.length).trimCmd(),
                    chatSettings
                ).map { llmResult ->
                    HandlerResponse(
                        chatId = chatSettings.chatId,
                        message = llmResult.text,
                        llmResult = llmResult
                    )
                }
            }
        }
        return flowOf(HandlerResponse(chatId = chatSettings.chatId, message = "Команда не распознана"))
    }


    /**
     * @param userMessage - сообщение очищенное от стартовой [ChatCommand.Compare]
     * @param createChat функция создания нового чата, принимает имя модели и возвращает Pair<chatId, chatName>
     * */
    private suspend fun handleCompareCommand(
        userMessage: String,
        chatSettings: ChatSettings,
        createChat: suspend (String) -> Pair<Long, String>
    ): Flow<HandlerResponse> = callbackFlow {
        val parameters = extractFromStartBrackets(userMessage)
        if (parameters.isNullOrBlank()) {
            // параметров нет - вернемся к обычной обработке
            handleUserMessage(userMessage, chatSettings, createChat).collect { response ->
                send(response)
            }
            close()
            return@callbackFlow
        }

        // Парсим модельки из параметров (формат: "model1, model2, model3")
        val modelNames = parameters.split(INPUT_DELIMITER)
            .map { it.trimCmd() }
            .filter { it.isNotBlank() }

        if (modelNames.isEmpty()) {
            send(HandlerResponse(chatId = chatSettings.chatId, message = "Не указаны модели для сравнения"))
            close()
            return@callbackFlow
        }

        // Извлекаем задачу из сообщения (все что после скобок)
        val taskMessage = userMessage
            .substringAfter(parameters)
            .substringAfter(")")
            .trimCmd()
            .ifBlank { "Сравни эти модели" }

        // Отправляем сообщение о начале сравнения
        send(HandlerResponse(
            chatId = chatSettings.chatId,
            message = "🔄 Начинаю сравнение моделей: ${modelNames.joinToString(", ")}"
        ))

        // Запускаем параллельное выполнение для каждой модели
        coroutineScope {
            val deferredResults = modelNames.map { modelName ->
                async {
                    runComparisonForModel(
                        modelName = modelName,
                        taskMessage = taskMessage,
                        originalChatSettings = chatSettings,
                        createChat = createChat,
                        sendProgress = { message ->
                            // Используем trySend для отправки прогресса
                            trySend(HandlerResponse(
                                chatId = chatSettings.chatId,
                                message = message
                            ))
                        }
                    )
                }.await()
            }

            // TODO подумать
            // Собираем результаты по мере их поступления
            // deferredResults.awaitAll()
        }

        // Отправляем итоговый отчет
        close()
    }

    /**
     * Запускает сравнение для одной модели
     */
    private suspend fun runComparisonForModel(
        modelName: String,
        taskMessage: String,
        originalChatSettings: ChatSettings,
        createChat: suspend (String) -> Pair<Long, String>,
        sendProgress: (String) -> Unit
    ): Unit {

        
        try {
            // Создаем новый чат для этой модели
            val (newChatId, newChatName) = createChat(modelName)

            // Создаем настройки чата с новой моделью
            val newChatSettings = originalChatSettings.copy(
                chatId = newChatId,
                model = originalChatSettings.model.copy(name = modelName)
            )

            // Отправляем сообщение о начале обработки модели
            sendProgress("▶️ Запускаю модель: $modelName в чате \"$newChatName\"")

            // Запускаем SimpleWorker
            val simpleWorker = commandToWorker[ChatCommand.SimpleWork]
            if (simpleWorker != null) {
                // Записываем время начала
                val startTime = System.currentTimeMillis()
                simpleWorker.doWork(taskMessage, newChatSettings).collect { llmResult ->
                    // Записываем время окончания
                    val endTime = System.currentTimeMillis()
                    val durationSeconds = (endTime - startTime) / 1000.0
                    
                    // Добавляем ответ модели в её собственный чат (newChatId)
                    addChatMessageUseCase.invoke(
                        newChatId,
                        System.currentTimeMillis(),
                        UserType.Bot,
                        llmResult.text.replace("<br>", "\n"),
                        ChatMessageStatus.Viewed
                    )
                    
                    // Отправляем уведомление о получении ответа в основной чат
                    sendProgress("✅ Модель $modelName ответила в чате \"$newChatName\": ${llmResult.text.take(30)}...")

                    // Формируем итоговый отчет с использованием данных
                    val report = buildString {
                        appendLine("📊 Отчет по модели: $modelName")
                        appendLine("---")
                        
                        // Добавляем название чата
                        appendLine("💬 Чат: \"$newChatName\"")
                        
                        // Добавляем время ответа
                        appendLine("⏱️ Время ответа: ${String.format("%.2f", durationSeconds)} сек.")
                        
                        // Добавляем информацию об использовании токенов
                        llmResult.source?.let { source ->
                            if (source is com.example.day.core.core_features.llm.domain.model.ModelResult.Success) {
                                source.usage?.let { usage ->
                                    appendLine("📝 Токены:")
                                    appendLine("  - Prompt токены: ${usage.promptTokens}")
                                    appendLine("  - Completion токены: ${usage.completionTokens}")
                                    appendLine("  - Всего токенов: ${usage.totalTokens}")
                                    if (usage.cost != null) {
                                        appendLine("  - Стоимость: ${String.format("%.6f$", usage.cost)}")
                                    }
                                    usage.costDetails?.let { costDetails ->
                                        // TODO проверить формат стоимости
                                        appendLine("  - Детали стоимости:")
                                        if (costDetails.upstreamInferencePromptCost != null) {
                                            appendLine("    - Prompt: ${String.format("%.6f$", costDetails.upstreamInferencePromptCost)}")
                                        }
                                        if (costDetails.upstreamInferenceCompletionsCost != null) {
                                            appendLine("    - Completion: ${String.format("%.6f$", costDetails.upstreamInferenceCompletionsCost)}")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    sendProgress(report)
                }
            }

        } catch (e: Exception) {
            sendProgress("❌ Ошибка при запуске модели $modelName: ${e.message}")
        }
    }

    fun extractFromStartBrackets(input: String): String? {
        // Регулярка: ищет строку, начинающуюся с '(', захватывает всё до ')', и саму ')'
        val regex = """^\(([^)]*)\)""".toRegex()

        return regex.find(input)?.groupValues?.get(1)
    }

    private fun String.trimCmd(): String =
        (this as CharSequence).trim { it.isWhitespace() || it.isISOControl() }.toString()

    private fun String.ifBlank(defaultValue: () -> String): String =
        if (isBlank()) defaultValue() else this

    companion object {
        private const val INPUT_DELIMITER = ","
    }
}
