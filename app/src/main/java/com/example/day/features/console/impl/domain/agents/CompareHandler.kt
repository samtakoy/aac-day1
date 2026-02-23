package com.example.day.features.console.impl.domain.agents

import com.example.day.core.core_features.chat.domain.model.ChatSettings
import com.example.day.core.core_features.llm.domain.model.getContent
import com.example.day.features.console.impl.domain.agents.utils.ModelReportBuilder
import com.example.day.features.console.impl.domain.agents.worker.SimpleWorker
import com.example.day.features.console.impl.domain.agents.worker.base.WorkerEvent
import javax.inject.Inject

/**
 * Обработчик команды сравнения моделей (@@compare)
 * Отвечает за парсинг параметров, запуск параллельного сравнения моделей
 * и формирование отчетов.
 */
internal class CompareHandler @Inject constructor(
    private val simpleWorker: SimpleWorker,
    private val reportBuilder: ModelReportBuilder
) {

    /**
     * Обрабатывает команду сравнения
     *
     * @param inputWithoutCommand - сообщение очищенное от стартовой команды
     * @param chatSettings настройки чата
     * @param tools инструменты для работы с чатом
     */
    suspend fun handle(
        inputWithoutCommand: String,
        chatSettings: ChatSettings,
        tools: WorkerTools
    ) {
        val parameters = inputWithoutCommand.extractFromStartBrackets()
        if (parameters.isNullOrBlank()) {
            // параметров нет - вернемся к обычной обработке
            // Здесь можно вызвать обратно основной обработчик
            return
        }

        // Парсим модельки из параметров (формат: "model1, model2, model3")
        val modelNames = parameters.split(INPUT_DELIMITER)
            .map { it.trimCmd() }
            .filter { it.isNotBlank() }

        if (modelNames.isEmpty()) {
            tools.addBotMessage(chatId = chatSettings.chatId, message = "Не указаны модели для сравнения")
            return
        }

        // Извлекаем задачу из сообщения (все что после скобок)
        val taskMessage = inputWithoutCommand
            .substringAfter(parameters)
            .substringAfter(")")
            .trimCmd()
            .ifBlank { "Сравни эти модели" }

        // Отправляем сообщение о начале сравнения
        tools.addBotMessage(
            chatId = chatSettings.chatId,
            message = "🔄 Начинаю сравнение моделей: ${modelNames.joinToString(", ")}"
        )

        // Запускаем параллельное выполнение для каждой модели
        modelNames.forEach { modelName ->
            runComparisonForModel(
                modelName = modelName,
                taskMessage = taskMessage,
                originalChatSettings = chatSettings,
                tools = tools
            )
        }
    }

    /**
     * Запускает сравнение для одной модели
     */
    private suspend fun runComparisonForModel(
        modelName: String,
        taskMessage: String,
        originalChatSettings: ChatSettings,
        tools: WorkerTools,
    ) {
        try {
            // Создаем новый чат для этой модели
            val newChatName = modelName
            // TODO если чат с таким именем существует - то не создавать его а переиспользовать
            val newChatId = tools.createChat(newChatName)

            // Создаем настройки чата с новой моделью
            val newChatSettings = originalChatSettings.copy(
                chatId = newChatId,
                model = originalChatSettings.model.copy(name = modelName)
            )

            // Отправляем сообщение о начале обработки модели
            tools.addBotMessage(
                originalChatSettings.chatId,
                "▶️ Запускаю модель: $modelName в чате \"$newChatName\""
            )

            // Запускаем SimpleWorker
            var startTime = 0L
            simpleWorker.doWork(taskMessage, newChatSettings).collect { workerEvent ->
                when (workerEvent) {
                    is WorkerEvent.Speech -> {
                        // Добавляем ответ модели в её собственный чат (newChatId)
                        tools.addBotMessage(newChatId, workerEvent.text.replace("<br>", "\n"))
                    }
                    // TODO тут будет инфо сообщение в чат
                    is WorkerEvent.RequestError -> Unit
                    WorkerEvent.RequestStart -> {
                        // Время старта запроса
                        startTime = System.currentTimeMillis()
                    }
                    is WorkerEvent.RequestSuccess -> {
                        // Записываем время окончания
                        val endTime = System.currentTimeMillis()
                        val durationSeconds = (endTime - startTime) / 1000.0

                        // Отправляем уведомление о получении ответа в основной чат
                        val shortModelAnswer = workerEvent.result.getContent().take(30) + "..."
                        tools.addBotMessage(
                            originalChatSettings.chatId,
                            "✅ Модель $modelName ответила в чате \"$newChatName\": $shortModelAnswer"
                        )

                        // Формируем итоговый отчет с использованием данных
                        val report = reportBuilder.build(
                            modelName = modelName,
                            durationSeconds = durationSeconds,
                            modelResult = workerEvent.result
                        )
                        tools.addBotMessage(originalChatSettings.chatId, report)
                    }
                }
            }
        } catch (e: Exception) {
            tools.addBotMessage(
                originalChatSettings.chatId,
                "❌ Ошибка при запуске модели $modelName: ${e.message}"
            )
        }
    }

    companion object {
        private const val INPUT_DELIMITER = ","
    }
}
