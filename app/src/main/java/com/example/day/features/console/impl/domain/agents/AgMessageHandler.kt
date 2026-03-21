package com.example.day.features.console.impl.domain.agents

import com.example.day.core.core_features.agent.domain.utils.ConsumptionCalculator
import com.example.day.core.core_features.agent.domain.utils.trimCmd
import com.example.day.core.core_features.agent.domain.workers.base.AWorker
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.agent.domain.workers.concrete.CompareWorker
import com.example.day.core.core_features.agent.domain.workers.concrete.McpWorker
import com.example.day.core.core_features.agent.domain.workers.concrete.PromptWorker
import com.example.day.core.core_features.agent.domain.workers.concrete.RejectWorker
import com.example.day.core.core_features.agent.domain.workers.concrete.SimpleWorker
import com.example.day.core.core_features.agent.domain.workers.concrete.StepWorker
import com.example.day.core.core_features.agent.domain.workers.concrete.TalkWorker
import com.example.day.core.core_features.agent.domain.workers.concrete.TeamWorker
import com.example.day.core.core_features.chat.domain.model.Chat
import javax.inject.Inject

/**
 * Routes user commands to the corresponding [AWorker] and applies shared event post-processing.
 */
internal class AgMessageHandler @Inject constructor(
    simpleWorker: SimpleWorker,
    stepWorker: StepWorker,
    promptWorker: PromptWorker,
    teamWorker: TeamWorker,
    private val talkWorker: TalkWorker,
    mcpWorker: McpWorker,
    compareWorker: CompareWorker,
    private val rejectWorker: RejectWorker,
    private val consumptionCalculator: ConsumptionCalculator
) {

    private val commandToWorker: Map<ChatCommand, AWorker> = mapOf(
        ChatCommand.SimpleWork to simpleWorker,
        ChatCommand.StepWork to stepWorker,
        ChatCommand.PromptWork to promptWorker,
        ChatCommand.TeamWork to teamWorker,
        ChatCommand.Talk to talkWorker,
        ChatCommand.Mcp to mcpWorker,
        ChatCommand.Compare to compareWorker,
    )

    suspend fun handleUserMessage(
        userMessage: String,
        chat: Chat,
        onEvent: (suspend (WorkerEvent) -> Unit)? = null
    ) {
        val trimmedMessage = userMessage.trim()

        for ((command, worker) in commandToWorker.entries) {
            if (trimmedMessage.startsWith(command.title, ignoreCase = true)) {
                val postProcessingEvents = mutableListOf<WorkerEvent>()
                worker.doWork(
                    userPrompt = trimmedMessage.substring(command.title.length).trimCmd(),
                    chat = chat,
                    onEvent = { workerEvent ->
                        postProcessingEvents.add(workerEvent)
                        onEvent?.invoke(workerEvent)
                    }
                )
                postProcessingEvents.forEach { workerEvent ->
                    consumptionCalculator.onWorkerEvent(chat, workerEvent)
                }
                return
            }
        }

        rejectWorker.doWork(
            userPrompt = trimmedMessage,
            chat = chat,
            onEvent = null
        )
    }

    suspend fun handleConfirmation(
        chat: Chat,
        runId: String,
        confirmationId: String,
        approved: Boolean,
        onEvent: (suspend (WorkerEvent) -> Unit)? = null
    ) {
        val postProcessingEvents = mutableListOf<WorkerEvent>()
        when (runId.substringBefore(':')) {
            TalkWorker.AGENT_NAME -> {
                talkWorker.handleConfirmation(
                    chat = chat,
                    runId = runId,
                    confirmationId = confirmationId,
                    approved = approved,
                    onEvent = { workerEvent ->
                        postProcessingEvents.add(workerEvent)
                        onEvent?.invoke(workerEvent)
                    }
                )
            }
            else -> error("Unsupported confirmation runId: $runId")
        }

        postProcessingEvents.forEach { workerEvent ->
            consumptionCalculator.onWorkerEvent(chat, workerEvent)
        }
    }
}
