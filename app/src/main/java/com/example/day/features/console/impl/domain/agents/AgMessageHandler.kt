package com.example.day.features.console.impl.domain.agents

import com.example.day.core.core_features.chat.domain.model.ChatSettings
import com.example.day.features.console.impl.domain.agents.worker.AWorker
import com.example.day.features.console.impl.domain.agents.worker.PromptWorker
import com.example.day.features.console.impl.domain.agents.worker.SimpleWorker
import com.example.day.features.console.impl.domain.agents.worker.StepWorker
import com.example.day.features.console.impl.domain.agents.worker.TeamWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

internal class AgMessageHandler @Inject constructor(
    simpleWorker: SimpleWorker,
    stepWorker: StepWorker,
    promptWorker: PromptWorker,
    teamWorker: TeamWorker
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
     * @return Flow ответов LLM в чат.
     * Например, "Команда не распознана" или "Решение вашей задачи..."
     */
    suspend fun handleUserMessage(
        userMessage: String,
        chatSettings: ChatSettings
    ): Flow<String> {
        val userMessage = userMessage.trim()

        if (userMessage.startsWith(ChatCommand.Compare.title, ignoreCase = true)) {
            val inputWithoutCommand = userMessage
                .substring(ChatCommand.Compare.title.length)
                .trimCmd()
            return handleCompareCommand(inputWithoutCommand, chatSettings)
        }

        for((command, worker) in commandToWorker.entries) {
            if (userMessage.startsWith(command.title, ignoreCase = true)) {
                return worker.doWork(
                    userMessage.substring(command.title.length).trimCmd(),
                    chatSettings
                )
            }
        }
        return flowOf("Команда не распознана")
    }


    /**
     * @param userMessage - сообщение очищенное от стартовой [ChatCommand.Compare]
     * */
    private suspend fun handleCompareCommand(
        userMessage: String,
        chatSettings: ChatSettings
    ): Flow<String> {
        val parameters = extractFromStartBrackets(userMessage)
        if (parameters.isNullOrBlank()) {
            // параметров нет - вернемся к обычной обработке
            return handleUserMessage(userMessage, chatSettings)
        }
        val inputs = parameters.split(INPUT_DELIMITER)
        return flowOf("не реализовано")
    }

    fun extractFromStartBrackets(input: String): String? {
        // Регулярка: ищет строку, начинающуюся с '(', захватывает всё до ')', и саму ')'
        val regex = """^\(([^)]*)\)""".toRegex()

        return regex.find(input)?.groupValues?.get(1)
    }

    private fun String.trimCmd(): String =
        (this as CharSequence).trim { it.isWhitespace() || it.isISOControl() }.toString()

    companion object {
        private const val INPUT_DELIMITER = ","
    }
}