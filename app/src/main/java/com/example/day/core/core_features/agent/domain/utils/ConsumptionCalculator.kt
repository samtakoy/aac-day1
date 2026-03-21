package com.example.day.core.core_features.agent.domain.utils

import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.agent.domain.workers.tools.AgentTools
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import com.example.day.core.core_features.llm.domain.utils.ModelConsuption
import com.example.day.core.core_features.llm.domain.utils.ModelReportBuilder
import com.example.day.core.core_features.llm.domain.utils.toModelConsumption
import kotlinx.coroutines.delay
import javax.inject.Inject

class ConsumptionCalculator @Inject constructor(
    private val chatTools: ChatTools,
    private val modelReportBuilder: ModelReportBuilder
) {
    companion object {
        // Map<chatId, ModelConsuption> - суммируется на время жизни приложения
        private val chatConsumptionMap: MutableMap<Long, ModelConsuption> = mutableMapOf()
    }

    suspend fun onWorkerEvent(chat: Chat, event: WorkerEvent) {
        when (event) {
            is WorkerEvent.RequestError -> Unit
            WorkerEvent.RequestStart -> Unit
            is WorkerEvent.RequestSuccess -> {
                // Костыль, чтобы info сообщение не опережало ответ llm
                delay(1)

                // 1. Получить текущее потребление из результата
                val currentConsumption = event.result.toModelConsumption()

                // 2. Обновить суммарное потребление для чата
                val previousTotal = chatConsumptionMap.getOrDefault(chat.id, ModelConsuption.empty())
                val newTotal = previousTotal + currentConsumption
                chatConsumptionMap[chat.id] = newTotal

                // 3. Сформировать сообщение о текущем запросе
                val currentMessage = modelReportBuilder.buildConsuption(currentConsumption)

                // 4. Сформировать сообщение о суммарном потреблении чата
                val totalMessage = modelReportBuilder.buildConsuption(newTotal)

                // 5. Вывести в чат
                val infoMessage = buildString {
                    appendLine("📊 Расходы за текущий запрос:")
                    appendLine(currentMessage)
                    appendLine("📊 Всего за чат:")
                    append(totalMessage)
                }
                chatTools.addInfoMessage(chat.id, infoMessage)
            }
            // Planner-specific events - no consumption tracking needed
            is WorkerEvent.Planner.FactSaved -> Unit
            is WorkerEvent.Planner.StageCompleted -> Unit
            is WorkerEvent.Planner.StageCreationSuggested -> Unit
            is WorkerEvent.Tool.ToolCallStarted -> Unit
            is WorkerEvent.Tool.ToolCallFinished -> Unit
            is WorkerEvent.UserConfirmation.ActionConfirmation -> Unit
            is WorkerEvent.UserConfirmation.ToolConfirmation -> Unit
        }
    }
}
