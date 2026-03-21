package com.example.day.core.core_features.agent.domain.workers.concrete

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.workers.base.AWorker
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.agent.domain.utils.extractFromStartBrackets
import com.example.day.core.core_features.agent.domain.utils.ifBlank
import com.example.day.core.core_features.agent.domain.utils.trimCmd
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import com.example.day.core.core_features.llm.domain.model.getContent
import com.example.day.core.core_features.llm.domain.utils.ModelReportBuilder
import com.example.day.core.core_features.llm.domain.utils.toModelConsumption
import javax.inject.Inject

/**
 * Handler for model comparison command (@@compare)
 * Responsible for parsing parameters, launching parallel model comparison
 * and generating reports.
 */
class CompareWorker @Inject constructor(
    private val simpleWorker: SimpleWorker,
    private val reportBuilder: ModelReportBuilder,
    private val chatTools: ChatTools
) : AWorker {

    /**
     * Handles comparison command
     *
     * @param userPrompt - message cleaned from the start command
     * @param chat chat settings
     * @param chatTools tools for working with chat
     */
    override suspend fun doWork(
        userPrompt: String,
        chat: Chat,
        userRole: AContextMessage.Role,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        val inputWithoutCommand = userPrompt
        val chatSettings = chat.settings
        val parameters = inputWithoutCommand.extractFromStartBrackets()
        if (parameters.isNullOrBlank()) {
            // No parameters - return to normal processing
            return
        }

        // Parse models from parameters (format: "model1, model2, model3")
        val modelNames = parameters.split(INPUT_DELIMITER)
            .map { it.trimCmd() }
            .filter { it.isNotBlank() }

        if (modelNames.isEmpty()) {
            chatTools.addBotMessage(chatId = chatSettings.chatId, message = "Не указаны модели для сравнения")
            return
        }

        // Extract task from message (everything after brackets)
        val taskMessage = inputWithoutCommand
            .substringAfter(parameters)
            .substringAfter(")")
            .trimCmd()
            .ifBlank { "Сравни эти модели" }

        // Send message about start of comparison
        chatTools.addBotMessage(
            chatId = chatSettings.chatId,
            message = "🔄 Начинаю сравнение моделей: ${modelNames.joinToString(", ")}"
        )

        // Launch parallel execution for each model
        modelNames.forEach { modelName ->
            runComparisonForModel(
                modelName = modelName,
                taskMessage = taskMessage,
                originalChat = chat
            )
        }
    }

    /**
     * Launches comparison for one model
     */
    private suspend fun runComparisonForModel(
        modelName: String,
        taskMessage: String,
        originalChat: Chat,
    ) {
        try {
            // Get or create chat for this model
            val newChatName = modelName
            val newChat = chatTools.getOrCreateChat(newChatName, originalChat.chatGroup.id)
            val newChatId = newChat.id

            // Create chat settings with new model
            val newChatSettings = originalChat.settings.copy(
                chatId = newChatId,
                model = originalChat.settings.model.copy(name = modelName)
            )

            // Create temporary Chat for passing to Worker
            val tempChat = originalChat.copy(
                id = newChatId,
                settings = newChatSettings
            )

            // Send message about start of model processing
            chatTools.addBotMessage(
                originalChat.settings.chatId,
                "▶️ Запускаю модель: $modelName в чате \"$newChatName\""
            )

            // Launch SimpleWorker - it sends messages to chat itself via chatTools
            var startTime = 0L
            simpleWorker.doWork(taskMessage, tempChat) { workerEvent ->
                when (workerEvent) {
                    is WorkerEvent.RequestError -> Unit
                    WorkerEvent.RequestStart -> {
                        // Request start time
                        startTime = System.currentTimeMillis()
                    }
                    is WorkerEvent.RequestSuccess -> {
                        // Record end time
                        val endTime = System.currentTimeMillis()
                        val durationSeconds = (endTime - startTime) / 1000.0

                        // Send notification about receiving response to main chat
                        val shortModelAnswer = workerEvent.result.getContent().take(30) + "..."
                        chatTools.addBotMessage(
                            originalChat.settings.chatId,
                            "✅ Модель $modelName ответила в чате \"$newChatName\": $shortModelAnswer"
                        )

                        // Generate final report using data
                        val report = reportBuilder.build(
                            modelName = workerEvent.result.model,
                            durationSeconds = durationSeconds,
                            consumption = workerEvent.result.toModelConsumption()
                        )
                        chatTools.addBotMessage(originalChat.settings.chatId, report)
                    }
                    // Planner-specific events - ignore in CompareWorker
                    is WorkerEvent.Planner.FactSaved -> Unit
                    is WorkerEvent.Planner.StageCompleted -> Unit
                    is WorkerEvent.Planner.StageCreationSuggested -> Unit
                    is WorkerEvent.Tool.ToolCallStarted -> Unit
                    is WorkerEvent.Tool.ToolCallFinished -> Unit
                    is WorkerEvent.UserConfirmation.Requested -> Unit
                }
            }
        } catch (e: Exception) {
            chatTools.addBotMessage(
                originalChat.settings.chatId,
                "❌ Ошибка при запуске модели $modelName: ${e.message}"
            )
        }
    }

    companion object Companion {
        private const val INPUT_DELIMITER = ","
    }
}

