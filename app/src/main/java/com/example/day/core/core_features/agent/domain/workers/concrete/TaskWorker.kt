package com.example.day.core.core_features.agent.domain.workers.concrete

import android.util.Log
import com.example.day.core.core_features.agent.domain.AIAgent
import com.example.day.core.core_features.agent.domain.AIAgentFactory
import com.example.day.core.core_features.agent.domain.AgentContextRepository
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.TaskLlmResponse
import com.example.day.core.core_features.agent.domain.strategy.AContextDefaultFactory
import com.example.day.core.core_features.agent.domain.strategy.StrategyFactory
import com.example.day.core.core_features.agent.domain.tools.ExecutionSessionManager
import com.example.day.core.core_features.agent.domain.tools.ToolCallOrchestrator
import com.example.day.core.core_features.agent.domain.tools.ToolRegistry
import com.example.day.core.core_features.agent.domain.workers.base.AWorker
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.agent.domain.workers.task.TaskResponseParser
import com.example.day.core.core_features.agent.domain.workers.task.states_config.TaskStateConfig
import com.example.day.core.core_features.agent.domain.workers.task.states_config.TaskStateData
import com.example.day.core.core_features.agent.domain.workers.task.states_store.TaskContext
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.memory.domain.provider.CompositeMemoryProvider
import com.example.day.core.core_features.memory.domain.provider.TaskStateMemoryProviderFactory
import com.example.day.core.core_features.memory.domain.provider.base.MemoryProviderFactory
import com.example.day.core.core_features.state_machine.domain.StateStore
import java.util.UUID
import javax.inject.Inject

/**
 * Worker for task state machine.
 * Manages task lifecycle: INIT -> PLANNING -> EXECUTION -> VERIFICATION -> DONE
 */
class TaskWorker @Inject constructor(
    private val aiAgentFactory: AIAgentFactory,
    private val chatTools: ChatTools,
    private val memoryProviderFactory: MemoryProviderFactory,
    private val taskStateMemoryProviderFactory: TaskStateMemoryProviderFactory,
    private val contextRepository: AgentContextRepository,
    private val llmRequestUseCase: LlmRequestUseCase,
    private val strategyFactory: StrategyFactory,
    private val stateStore: StateStore,
    private val toolRegistry: ToolRegistry,
    private val executionSessionManager: ExecutionSessionManager,
    private val toolCallOrchestrator: ToolCallOrchestrator
) : AWorker {

    companion object {
        const val AGENT_NAME = "task_state_agent"
    }

    override suspend fun doWork(
        userPrompt: String,
        chat: Chat,
        userRole: AContextMessage.Role,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        val agent = aiAgentFactory.getOrCreate(
            AGENT_NAME,
            chat.id,
            "",
            defaultModel = { chat.settings.model.copy(jsonFormat = true) },
            defaultContext = { AContextDefaultFactory.createEmpty() }
        )
        val taskContext = TaskContext(
            chat = chat,
            agentId = agent.config.id,
            store = stateStore
        )
        if (userPrompt.isNotBlank()) {
            clearTaskDataIfNeeded(taskContext)
        }
        validateInitialState(taskContext)
        val compositeProvider = buildMemoryProvider(chat, agent)
        val strategy = strategyFactory.create(agent.config.contextStrategyType)

        Log.e("ktor", "init agent: ${agent.config.id}")
        val agentWithTaskState = AIAgent(
            config = agent.config,
            contextRepository = contextRepository,
            llmProvider = llmRequestUseCase,
            strategy = strategy,
            memoryProvider = compositeProvider,
            toolRegistry = toolRegistry,
            executionSessionManager = executionSessionManager,
            runIdProvider = { "${agent.config.id}:${UUID.randomUUID()}" },
            orchestrator = toolCallOrchestrator
        )

        reportCurrentState(chat.id, taskContext)

        val result = agentWithTaskState.process(
            prompt = AContextMessage(AContextMessage.Role.USER, userPrompt),
            onEvent = onEvent
        )

        result.getOrNull()?.requestDebugInfo?.let { debugString ->
            chatTools.addInfoMessage(chat.id, debugString)
        }

        handleAgentResult(
            chat = chat,
            taskContext = taskContext,
            userInput = userPrompt,
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
        val session = executionSessionManager.get(runId)
        if (session == null) {
            chatTools.addInfoMessage(chat.id, "Confirmation session not found", emptyList())
            return
        }

        val agent = aiAgentFactory.getOrCreate(
            AGENT_NAME,
            chat.id,
            "",
            defaultModel = { chat.settings.model.copy(jsonFormat = true) },
            defaultContext = { AContextDefaultFactory.createEmpty() }
        )
        val taskContext = TaskContext(chat = chat, agentId = agent.config.id, store = stateStore)
        val userInput = session.requestSnapshot.prompt.content.orEmpty()

        val result = agent.resume(
            runId = runId,
            confirmationId = confirmationId,
            approved = approved,
            onEvent = onEvent
        )

        handleAgentResult(
            chat = chat,
            taskContext = taskContext,
            userInput = userInput,
            result = result,
            onEvent = onEvent
        )
    }

    suspend fun handleAction(chat: Chat, action: String, onEvent: (suspend (WorkerEvent) -> Unit)?) {
        val agent = aiAgentFactory.getOrCreate(
            AGENT_NAME, chat.id, "",
            defaultModel = { chat.settings.model.copy(jsonFormat = true) },
            defaultContext = { AContextDefaultFactory.createEmpty() }
        )
        val taskContext = TaskContext(chat = chat, agentId = agent.config.id, store = stateStore)
        val currentState = taskContext.getState()
        val handler = TaskStateConfig.config.handlers[currentState]

        val result = handler?.handleUserAction(taskContext, action)
        result?.messages?.forEach { msg ->
            if (msg.isTitle) chatTools.addTitleMessage(chat.id, msg.message, msg.buttons)
            else if (msg.isInfo) chatTools.addInfoMessage(chat.id, msg.message, msg.buttons)
            else chatTools.addBotMessage(chat.id, msg.message, msg.buttons)
        }

        if (result == null) {
            chatTools.addInfoMessage(chat.id, "Handler not found", emptyList())
            return
        }

        if (result.llmRequest != null) {
            doWork(result.llmRequest.userPrompt.orEmpty(), chat, AContextMessage.Role.USER, onEvent)
        }
    }

    private suspend fun handleAgentResult(
        chat: Chat,
        taskContext: TaskContext,
        userInput: String,
        result: Result<com.example.day.core.core_features.agent.domain.model.AIAgentResult>,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        result.fold(
            onSuccess = { agentResult ->
                if (agentResult.isPaused) {
                    return@fold
                }
                val llmResponse = TaskResponseParser.parse(agentResult.responseText)
                if (llmResponse != null) {
                    processSuccessResponse(taskContext, userInput, llmResponse, agentResult.responseText, onEvent)
                } else {
                    handleParseError(chat.id, agentResult.responseText)
                }
            },
            onFailure = { error ->
                handleLlmError(chat.id, error)
            }
        )
    }

    private fun buildMemoryProvider(
        chat: Chat,
        agent: AIAgent
    ): CompositeMemoryProvider {
        val taskStateProvider = taskStateMemoryProviderFactory.create(
            chat = chat,
            agentId = agent.config.id
        )

        val baseMemoryProvider = memoryProviderFactory.create(agent.config.memoryTypes)

        return CompositeMemoryProvider(
            listOf(taskStateProvider, baseMemoryProvider)
        )
    }

    private suspend fun reportCurrentState(
        chatId: Long,
        taskContext: TaskContext
    ) {
        val step = taskContext.getCurStepNum()
        val totalStages = taskContext.getTotalSteps()

        val stateDescription = when (taskContext.getState()) {
            TaskStateConfig.INIT -> "Collecting requirements and defining the task"
            TaskStateConfig.PLANNING -> "Decomposing task into steps"
            TaskStateConfig.EXECUTION -> buildString {
                append("Executing step $step")
                if (totalStages > 0) append(" of $totalStages")
            }
            TaskStateConfig.VERIFICATION -> "Verifying task result"
            TaskStateConfig.DONE -> "Preparing final report"
            else -> "Unknown"
        }

        chatTools.addInfoMessage(chatId, "Current state: ${taskContext.getState()?.value} - $stateDescription")
    }

    private suspend fun processSuccessResponse(
        context: TaskContext,
        userInput: String,
        llmResponse: TaskLlmResponse,
        rawResponse: String,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        val stateConfig = TaskStateConfig.config
        val currentState = context.getState()!!
        val handler = stateConfig.handlers[currentState]!!

        val result = handler.handle(context, userInput, llmResponse)

        result.messages.forEach { msg ->
            when {
                msg.isTitle -> chatTools.addTitleMessage(context.chat.id, msg.message, msg.buttons)
                msg.isInfo -> chatTools.addInfoMessage(context.chat.id, msg.message, msg.buttons)
                else -> chatTools.addBotMessage(context.chat.id, msg.message, msg.buttons)
            }
        }
        if (result.errorMessage != null) {
            chatTools.addBotMessage(context.chat.id, "Something went wrong: ${result.errorMessage}\n$rawResponse")
        }

        if (result.llmRequest != null) {
            doWork(
                userPrompt = result.llmRequest.userPrompt.orEmpty(),
                chat = context.chat,
                onEvent = onEvent
            )
        }
    }

    private suspend fun validateInitialState(context: TaskContext) {
        val currentState = context.getState()
        when {
            currentState == null -> {
                val newState = TaskStateConfig.INIT
                context.updateState(newState)
                context.saveStateData(TaskStateData.Init())
            }
        }
    }

    private suspend fun clearTaskDataIfNeeded(context: TaskContext) {
        val currentState = context.getState()
        when {
            currentState == TaskStateConfig.DONE || currentState == null -> {
                context.clearTaskMemory()
            }
        }
    }

    private suspend fun handleParseError(
        chatId: Long,
        rawResponse: String
    ) {
        chatTools.addInfoMessage(
            chatId,
            "Could not parse LLM response. Please try to reformulate the request.\n\nrawResponse: $rawResponse"
        )
    }

    private suspend fun handleLlmError(
        chatId: Long,
        error: Throwable,
    ) {
        val errorMessage = "LLM request error: ${error.message}"
        chatTools.addBotMessage(chatId, errorMessage)
    }
}
