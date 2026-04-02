package com.example.day.core.core_features.agent.domain.workers.concrete

import android.util.Log
import com.example.day.core.core_features.agent.domain.AIAgent
import com.example.day.core.core_features.state_machine.domain.SM_TAG

import com.example.day.core.core_features.agent.domain.AIAgentFactory
import com.example.day.core.core_features.agent.domain.AgentContextRepository
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.strategy.AContextDefaultFactory
import com.example.day.core.core_features.agent.domain.strategy.StrategyFactory
import com.example.day.core.core_features.agent.domain.tools.ToolCallOrchestrator
import com.example.day.core.core_features.agent.domain.workers.base.AWorker
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.state_machine.domain.StateContext
import com.example.day.core.core_features.agent.domain.tools.ToolProvider
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.memory.domain.provider.CompositeMemoryProvider
import com.example.day.core.core_features.memory.domain.provider.StateMemoryProviderFactory
import com.example.day.core.core_features.memory.domain.provider.base.MemoryProviderFactory
import com.example.day.core.core_features.state_machine.domain.StateStore

/**
 * Worker for task state machine.
 * Manages task lifecycle: INIT → PLANNING → EXECUTION → VERIFICATION → DONE
 */
class TaskWorker constructor(
    private val aiAgentFactory: AIAgentFactory,
    private val chatTools: ChatTools,
    private val memoryProviderFactory: MemoryProviderFactory,
    private val stateMemoryProviderFactory: StateMemoryProviderFactory,
    private val contextRepository: AgentContextRepository,
    private val llmRequestUseCase: LlmRequestUseCase,
    private val strategyFactory: StrategyFactory,
    private val stateStore: StateStore,
    private val toolProvider: ToolProvider,
    private val toolCallOrchestrator: ToolCallOrchestrator,
    private val agentName: String
) : AWorker {

    /**
     * TODO синхронизировать взаимодействие с воркерами через каналы.
     * */
    override suspend fun doWork(
        userPrompt: String,
        chat: Chat,
        userRole: AContextMessage.Role,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        // Get or create agent. System prompt is empty — TaskStateMemoryProvider manages it dynamically.
        val agent = aiAgentFactory.getOrCreate(
            agentName,
            chat.id,
            "",
            defaultModel = { chat.settings.model.copy(jsonFormat = true, temperature = 0.0) },
            defaultContext = { AContextDefaultFactory.createEmpty() }
        )
        val taskContext = StateContext(
            chatId = chat.id,
            agentId = agent.config.id,
            store = stateStore
        )
        Log.d(SM_TAG, "[${agent.config.id}] doWork: state=${taskContext.getState()?.value ?: "null"}, prompt='$userPrompt'")
        if (userPrompt.isNotBlank()) {
            clearTaskDataIfNeeded(taskContext)
        }
        validateInitialState(taskContext)
        Log.d(SM_TAG, "[${agent.config.id}] after init: state=${taskContext.getState()?.value}, step=${taskContext.getCurStepNum()}")
        val compositeProvider = buildMemoryProvider(chat, agent)
        val strategy = strategyFactory.create(agent.config.contextStrategyType)
        // Create new agent instance with composite memory provider
        val agentWithTaskState = AIAgent(
            config = agent.config,
            contextRepository = contextRepository,
            llmProvider = llmRequestUseCase,
            strategy = strategy,
            memoryProvider = compositeProvider,
            toolProvider = toolProvider,
            orchestrator = toolCallOrchestrator
        )

        //  current state to chat before LLM call
        reportCurrentState(chat.id, taskContext)

        // Call agent.process() for automatic context management
        val result = agentWithTaskState.process(
            prompt = AContextMessage(AContextMessage.Role.USER, userPrompt),
            onEvent = onEvent
        )

        // debug message
        result.getOrNull()?.requestDebugInfo?.let { debugString ->
            chatTools.addInfoMessage(chat.id, debugString)
        }

        // process result
        result.fold(
            onSuccess = { agentResult ->
                processSuccessResponse(chat, taskContext, userPrompt, agentResult.responseText, onEvent)
            },
            onFailure = { error ->
                handleLlmError(chat.id, error)
            }
        )
    }

    suspend fun handleAction(chat: Chat, action: String, onEvent: (suspend (WorkerEvent) -> Unit)?) {
        val agent = aiAgentFactory.getOrCreate(
            agentName, chat.id, "",
            defaultModel = { chat.settings.model.copy(jsonFormat = true) },
            defaultContext = { AContextDefaultFactory.createEmpty() }
        )
        val taskContext = StateContext(chatId = chat.id, agentId = agent.config.id, store = stateStore)
        val currentState = taskContext.getState()
        val handler = taskContext.store.getStateConfig().handlers[currentState]

        val result = handler?.handleUserAction(taskContext, action)
        result?.messages?.forEach { msg ->
            if (msg.isTitle) chatTools.addTitleMessage(chat.id, msg.message, msg.buttons)
            else if (msg.isInfo) chatTools.addInfoMessage(chat.id, msg.message, msg.buttons)
            else chatTools.addBotMessage(chat.id, msg.message, msg.buttons)
        }

        // Если handler не найден - покажем Info сообщение
        if (result == null) {
            chatTools.addInfoMessage(chat.id, "Обработчик не найден", emptyList())
            return
        }

        if (result.llmRequest != null) {
            doWork(result.llmRequest.userPrompt.orEmpty(), chat, AContextMessage.Role.USER, onEvent)
        }
    }

    /** Все MemoryProvider для агента: стандартные + для выдачи промптов текущего стейта */
    private fun buildMemoryProvider(
        chat: Chat,
        agent: AIAgent
    ): CompositeMemoryProvider {
        // Create TaskStateMemoryProvider using factory
        val taskStateProvider = stateMemoryProviderFactory.create(
            chat = chat,
            agentId = agent.config.id,
            stateStore = stateStore
        )

        // Base memory from agent config (e.g. UserProfile if configured)
        val baseMemoryProvider = memoryProviderFactory.create(agent.config.memoryTypes)

        // Create composite provider: task state prompt + base memory
        val compositeProvider = CompositeMemoryProvider(
            listOf(taskStateProvider, baseMemoryProvider)
        )
        return compositeProvider
    }

    private suspend fun reportCurrentState(
        chatId: Long,
        taskContext: StateContext
    ) {
        val stateDescription = taskContext.getStateDescription()
        chatTools.addInfoMessage(chatId, "📍 Было Состояние: ${taskContext.getState()?.value} — $stateDescription")
    }

    private suspend fun processSuccessResponse(
        chat: Chat,
        context: StateContext,
        userInput: String,
        rawResponse: String,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        val stateConfig = context.store.getStateConfig()
        val currentState = context.getState()!!
        val handler =  stateConfig.handlers[currentState]!!

        Log.d(SM_TAG, "[${context.agentId}] processSuccessResponse: state=${currentState.value}, raw=$rawResponse")
        // Handle state logic
        val result = handler.handle(context, userInput, rawResponse)
        Log.d(SM_TAG, "[${context.agentId}] handler result: msgs=${result.messages.size}, nextLlm=${result.llmRequest?.userPrompt}, err=${result.errorMessage}")

        // 1. Сообщения в чат
        result.messages.forEach { msg ->
            when {
                msg.isTitle -> chatTools.addTitleMessage(chat.id, msg.message, msg.buttons)
                msg.isInfo -> chatTools.addInfoMessage(chat.id, msg.message, msg.buttons)
                else -> chatTools.addBotMessage(chat.id, msg.message, msg.buttons)
            }
        }
        // Отладочное при ошибке
        if (result.errorMessage != null) {
            chatTools.addBotMessage(chat.id, "Что-то пошло не так: ${result.errorMessage}\n$rawResponse")
        }

        // Loop back
        if (result.llmRequest != null) {
            doWork(
                userPrompt = result.llmRequest.userPrompt.orEmpty(),
                chat = chat,
                onEvent = onEvent
            )
        }
    }

    private suspend fun validateInitialState(context: StateContext) {
        val currentState = context.getState()
        when {
            currentState == null -> {
                context.updateState(context.store.getStateConfig().fallbackState)
                context.setCurStepNum(1)
                context.saveStateData(context.store.getStateConfig().fallbackStateData)
            }
        }
    }

    private suspend fun clearTaskDataIfNeeded(context: StateContext) {
        val currentState = context.getState()
        when {
            currentState == null || context.store.getStateConfig().isFinalState(currentState)  -> {
                context.clearTaskMemory()
            }
        }
    }

    private suspend fun handleLlmError(
        chatId: Long,
        error: Throwable,
    ) {
        val errorMessage = "❌ Ошибка при обращении к LLM: ${error.message}"
        chatTools.addBotMessage(chatId, errorMessage)
    }
}
