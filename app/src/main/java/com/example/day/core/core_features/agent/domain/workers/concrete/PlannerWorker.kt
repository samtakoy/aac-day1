package com.example.day.core.core_features.agent.domain.workers.concrete

import com.example.day.core.core_features.agent.domain.AIAgentFactory
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.prompt.PlannerPromptBuilder
import com.example.day.core.core_features.agent.domain.strategy.AContextDefaultFactory
import com.example.day.core.core_features.agent.domain.tools.ToolResponseParser
import com.example.day.core.core_features.agent.domain.usecase.CompleteStageUseCase
import com.example.day.core.core_features.agent.domain.workers.base.AWorker
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import com.example.day.core.core_features.memory.domain.usecase.GetFactsByChatGroupUseCase
import com.example.day.core.core_features.memory.domain.usecase.UpsertFactWithCategoryUseCase
import javax.inject.Inject

/**
 * Worker for PLANNER-type chat groups.
 * Implements three-layer memory architecture with hierarchical chat structure.
 *
 * Features:
 * - Long-term memory (LTM) retrieval and injection
 * - Working memory (parent context) for stage chats
 * - Pattern-based tool calling (pseudo-tool calling)
 * - Human-in-the-loop for stage creation
 *
 * Pattern Syntax:
 * - SAVE_FACT[key:category:fact] - Saves fact to long-term memory
 * - CREATE_STAGE[title:context] - Suggests creating a new stage chat (requires user confirmation)
 * - COMPLETE_STAGE[outcome] - Marks stage as completed and saves artifact
 */
class PlannerWorker @Inject constructor(
    private val aiAgentFactory: AIAgentFactory,
    private val getFactsByChatGroupUseCase: GetFactsByChatGroupUseCase,
    private val upsertFactWithCategoryUseCase: UpsertFactWithCategoryUseCase,
    // TODO то юзкейсы то репозитории - непонятно - все перемешано
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
        val chatId: Long = chat.id
        val chatTitle: String = chat.title
        val groupId: Long = chat.chatGroup.id  // Get groupId for memory isolation

        // Use domain model fields directly (no need to fetch entity)
        val parentId: Long? = chat.parentId
        val isStageChat: Boolean = chat.isStageChat
        val workingSummary: String? = chat.workingSummary

        // Retrieve Long-Term Memory for this group only
        // Uses GetFactsByChatGroupUseCase which coordinates LTMGroup lookup and fact retrieval
        val ltmFacts = getFactsByChatGroupUseCase(groupId)

        // Build system prompt with memory injection
        val systemPrompt = if (isStageChat) {
            PlannerPromptBuilder.buildStagePrompt(
                stageTitle = chatTitle,
                parentSummary = workingSummary,
                ltmFacts = ltmFacts
            )
        } else {
            PlannerPromptBuilder.buildMainPlannerPrompt(
                ltmFacts = ltmFacts,
                projectContext = workingSummary
            )
        }

        // Enrich task with system context
        val enrichedTask = "[System context: $systemPrompt]\n\n$userPrompt"

        // Notify that request is starting
        onEvent?.invoke(WorkerEvent.RequestStart)

        // Get agent and process message
        val agent = aiAgentFactory.getOrCreate(
            AGENT_NAME,
            chat.id,
            chat.settings.systemPromt,
            defaultModel = { chat.settings.model },
            defaultContext = { AContextDefaultFactory.createFull() }
        )

        // Process the message
        agent.process(chat.settings, prompt = AContextMessage(AContextMessage.Role.USER, enrichedTask), onEvent)
            .onSuccess { result ->
                val responseText = result.responseText

                // Parse response for tool patterns
                val parsedResponse = ToolResponseParser.parse(responseText)

                // Handle SAVE_FACT commands (auto-save)
                // Uses UpsertFactWithCategoryUseCase which coordinates category resolution and fact storage
                for (cmd in parsedResponse.saveFactCommands) {
                    upsertFactWithCategoryUseCase(
                        chatGroupId = groupId,
                        memoryKey = cmd.memoryKey,
                        category = cmd.category,
                        fact = cmd.fact
                    )
                    onEvent?.invoke(WorkerEvent.FactSaved(cmd.memoryKey, cmd.category, cmd.fact))
                }

                // Handle COMPLETE_STAGE command — only the first occurrence per response
                // (LLM may echo the pattern multiple times; processing all would duplicate working memory)
                val completeCmd = parsedResponse.completeStageCommands.firstOrNull()
                if (isStageChat && parentId != null && completeCmd != null) {
                    completeStageUseCase(
                        stageChatId = chatId,
                        stageTitle = chatTitle,
                        parentId = parentId,
                        outcome = completeCmd.outcome
                    ).onSuccess {
                        onEvent?.invoke(WorkerEvent.StageCompleted(chatId, completeCmd.outcome))
                    }.onFailure {
                        it.printStackTrace()
                        // TODO инао сообщение в чат
                        chatTools.addBotMessage(chatId, it.message ?: "Что-то пошло не так")
                    }
                }

                // Handle CREATE_STAGE commands (human-in-the-loop!)
                if (!isStageChat) {
                    for (cmd in parsedResponse.createStageCommands) {
                        onEvent?.invoke(WorkerEvent.StageCreationSuggested(
                            stageTitle = cmd.stageTitle,
                            workingSummary = cmd.workingSummary
                        ))
                    }
                }

                // Save cleaned response as bot message
                chatTools.addBotMessage(chatId, parsedResponse.cleanedResponse)
            }
            .onFailure { exception ->
                val errorMessage = "Error: ${exception.message}"
                onEvent?.invoke(WorkerEvent.RequestError(errorMessage))
                chatTools.addBotMessage(chatId, errorMessage)
            }
    }
}
