package com.example.day.core.core_features.agent.domain.workers.concrete

import com.example.day.core.core_features.agent.domain.AIAgent
import com.example.day.core.core_features.agent.domain.AIAgentFactory
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.AIAgentResult
import com.example.day.core.core_features.agent.domain.prompt.PlannerPromptBuilder
import com.example.day.core.core_features.agent.domain.strategy.AContextDefaultFactory
import com.example.day.core.core_features.agent.domain.tools.ToolCallingConstants
import com.example.day.core.core_features.agent.domain.tools.ToolResponseParser
import com.example.day.core.core_features.agent.domain.usecase.CompleteStageUseCase
import com.example.day.core.core_features.agent.domain.workers.base.AWorker
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import com.example.day.core.core_features.memory.domain.model.LongTermMemoryFact
import com.example.day.core.core_features.memory.domain.usecase.GetFactsByChatGroupUseCase
import com.example.day.core.core_features.memory.domain.usecase.UpsertFactWithCategoryUseCase
import javax.inject.Inject

/**
 * Worker for planner-type chat groups.
 */
class PlannerWorker @Inject constructor(
    private val aiAgentFactory: AIAgentFactory,
    private val getFactsByChatGroupUseCase: GetFactsByChatGroupUseCase,
    private val upsertFactWithCategoryUseCase: UpsertFactWithCategoryUseCase,
    private val completeStageUseCase: CompleteStageUseCase,
    private val chatTools: ChatTools
) : AWorker {

    companion object {
        const val AGENT_NAME = "planner_agent"
    }

    override suspend fun doWork(
        userPrompt: String,
        chat: Chat,
        userRole: AContextMessage.Role,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        val groupId = chat.chatGroup.id
        val ltmFacts = getFactsByChatGroupUseCase(groupId)
        val enrichedTask = buildEnrichedTask(chat, ltmFacts, userPrompt)

        onEvent?.invoke(WorkerEvent.RequestStart)

        val result = getAgent(chat).process(
            prompt = AContextMessage(AContextMessage.Role.USER, enrichedTask),
            onEvent = onEvent
        )

        handleAgentResult(
            chat = chat,
            groupId = groupId,
            result = result,
            onEvent = onEvent
        )
    }

    suspend fun handleConfirmation(
        chat: Chat,
        runId: String,
        confirmationId: String,
        approved: Boolean,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        val result = getAgent(chat).resume(
            runId = runId,
            confirmationId = confirmationId,
            approved = approved,
            onEvent = onEvent
        )

        handleAgentResult(
            chat = chat,
            groupId = chat.chatGroup.id,
            result = result,
            onEvent = onEvent
        )
    }

    private suspend fun handleAgentResult(
        chat: Chat,
        groupId: Long,
        result: Result<AIAgentResult>,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        val chatId = chat.id
        val chatTitle = chat.title
        val parentId = chat.parentId
        val isStageChat = chat.isStageChat

        result.onSuccess { agentResult ->
            if (agentResult.isPaused) {
                chatTools.addInfoMessage(chatId, ToolCallingConstants.WAITING_CONFIRMATION_MESSAGE)
                return@onSuccess
            }

            val parsedResponse = ToolResponseParser.parse(agentResult.responseText)

            for (cmd in parsedResponse.saveFactCommands) {
                upsertFactWithCategoryUseCase(
                    chatGroupId = groupId,
                    memoryKey = cmd.memoryKey,
                    category = cmd.category,
                    fact = cmd.fact
                )
                onEvent?.invoke(WorkerEvent.Planner.FactSaved(cmd.memoryKey, cmd.category, cmd.fact))
            }

            val completeCmd = parsedResponse.completeStageCommands.firstOrNull()
            if (isStageChat && parentId != null && completeCmd != null) {
                completeStageUseCase(
                    stageChatId = chatId,
                    stageTitle = chatTitle,
                    parentId = parentId,
                    outcome = completeCmd.outcome
                ).onSuccess {
                    onEvent?.invoke(WorkerEvent.Planner.StageCompleted(chatId, completeCmd.outcome))
                }.onFailure {
                    it.printStackTrace()
                    chatTools.addBotMessage(chatId, it.message ?: "Something went wrong")
                }
            }

            if (!isStageChat) {
                for (cmd in parsedResponse.createStageCommands) {
                    onEvent?.invoke(
                        WorkerEvent.Planner.StageCreationSuggested(
                            stageTitle = cmd.stageTitle,
                            workingSummary = cmd.workingSummary
                        )
                    )
                }
            }

            chatTools.addBotMessage(chatId, parsedResponse.cleanedResponse)
        }.onFailure { exception ->
            val errorMessage = "Error: ${exception.message}"
            onEvent?.invoke(WorkerEvent.RequestError(errorMessage))
            chatTools.addBotMessage(chatId, errorMessage)
        }
    }

    private suspend fun getAgent(chat: Chat): AIAgent {
        return aiAgentFactory.getOrCreate(
            AGENT_NAME,
            chat.id,
            chat.settings.systemPromt,
            defaultModel = { chat.settings.model },
            defaultContext = { AContextDefaultFactory.createFull() }
        )
    }

    private fun buildEnrichedTask(
        chat: Chat,
        ltmFacts: List<LongTermMemoryFact>,
        userPrompt: String
    ): String {
        val systemPrompt = if (chat.isStageChat) {
            PlannerPromptBuilder.buildStagePrompt(
                stageTitle = chat.title,
                parentSummary = chat.workingSummary,
                ltmFacts = ltmFacts
            )
        } else {
            PlannerPromptBuilder.buildMainPlannerPrompt(
                ltmFacts = ltmFacts,
                projectContext = chat.workingSummary
            )
        }

        return "[System context: $systemPrompt]\n\n$userPrompt"
    }
}
