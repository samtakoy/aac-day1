package com.example.day.core.core_features.agent.domain.workers

import com.example.day.core.core_features.agent.domain.AIAgent
import com.example.day.core.core_features.agent.domain.AIAgentFactory
import com.example.day.core.core_features.agent.domain.AgentContextRepository
import com.example.day.core.core_features.agent.domain.model.TaskLlmResponse
import com.example.day.core.core_features.agent.domain.model.TaskMemoryKeys
import com.example.day.core.core_features.agent.domain.model.TaskState
import com.example.day.core.core_features.agent.domain.strategy.StrategyFactory
import com.example.day.core.core_features.agent.domain.usecase.ClearAgentContextUseCase
import com.example.day.core.core_features.agent.domain.workers.base.AWorker
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.agent.domain.workers.task.TaskContext
import com.example.day.core.core_features.agent.domain.workers.task.TaskResponseParser
import com.example.day.core.core_features.agent.domain.workers.task.TaskStateMachine
import com.example.day.core.core_features.agent.domain.workers.task.validation.MemoryUpdateValidator
import com.example.day.core.core_features.agent.domain.workers.task.validation.TransitionValidator
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.memory.domain.provider.CompositeMemoryProvider
import com.example.day.core.core_features.memory.domain.provider.TaskStateMemoryProvider
import com.example.day.core.core_features.memory.domain.provider.base.MemoryProviderFactory
import com.example.day.core.core_features.memory.domain.usecase.ClearTaskMemoryForAgentUseCase
import com.example.day.core.core_features.memory.domain.usecase.GetFactsByAgentUseCase
import com.example.day.core.core_features.memory.domain.usecase.UpsertFactWithCategoryForAgentUseCase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * Worker for task state machine.
 * Manages task lifecycle: INIT → PLANNING → EXECUTION → VERIFICATION → DONE
 *
 * Features:
 * - State persistence in LTM (Long-Term Memory)
 * - Uses AIAgent.process() for automatic context management
 * - Dynamic system prompts via TaskStateMemoryProvider
 * - Feedback loop for rework (VERIFICATION → EXECUTION)
 * - Pause and resume without losing context
 */
class TaskWorker @Inject constructor(
    private val aiAgentFactory: AIAgentFactory,
    private val getFactsByAgentUseCase: GetFactsByAgentUseCase,
    private val upsertFactUseCase: UpsertFactWithCategoryForAgentUseCase,
    private val clearAgentContextUseCase: ClearAgentContextUseCase,
    private val clearTaskMemoryUseCase: ClearTaskMemoryForAgentUseCase,
    private val chatTools: ChatTools,
    private val memoryProviderFactory: MemoryProviderFactory,
    private val contextRepository: AgentContextRepository,
    private val llmRequestUseCase: LlmRequestUseCase,
    private val strategyFactory: StrategyFactory
) : AWorker {

    companion object {
        const val AGENT_NAME = "task_state_agent"
    }

    private val stateMachine = TaskStateMachine()
    private val mutex = Mutex()
    private val transitionValidator = TransitionValidator()
    private val memoryUpdateValidator = MemoryUpdateValidator()

    override suspend fun doWork(
        userPrompt: String,
        chat: Chat,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) = mutex.withLock {
        // Get or create agent. System prompt is empty — TaskStateMemoryProvider manages it dynamically.
        val agent = aiAgentFactory.getOrCreate(
            AGENT_NAME,
            chat.id,
            "",
            chat.settings.model
        )

        // Load current state from LTM
        val facts = loadFacts(agent.config.id)
        val currentState = determineCurrentState(facts)
        val currentStep = facts[TaskMemoryKeys.CURRENT_STEP]?.toIntOrNull() ?: 1

        // Build context
        val context = TaskContext(
            chat = chat,
            agentId = agent.config.id,
            facts = facts,
            currentState = currentState,
            currentStep = currentStep
        )

        // Create TaskStateMemoryProvider for dynamic system prompts
        val taskStateProvider = TaskStateMemoryProvider(
            getFactsByAgentUseCase = getFactsByAgentUseCase,
            chat = chat
        ).withAgentId(agent.config.id)

        // Base memory from agent config (e.g. UserProfile if configured)
        val baseMemoryProvider = memoryProviderFactory.create(agent.config.memoryTypes)

        // Create composite provider: task state prompt + base memory
        val compositeProvider = CompositeMemoryProvider(
            listOf(taskStateProvider, baseMemoryProvider)
        )

        // Create strategy for the agent
        val strategy = strategyFactory.create(agent.config.contextStrategyType)

        // Create new agent instance with composite memory provider
        val agentWithTaskState = AIAgent(
            config = agent.config,
            contextRepository = contextRepository,
            llmProvider = llmRequestUseCase,
            strategy = strategy,
            memoryProvider = compositeProvider
        )

        // Notify request start
        onEvent?.invoke(WorkerEvent.RequestStart)

        // Report current state to chat before LLM call
        reportCurrentState(chat.id, currentState, currentStep, facts)

        // Call agent.process() for automatic context management
        val result = agentWithTaskState.process(
            chat = chat.settings,
            userPrompt = userPrompt,
            onEvent = onEvent
        )

        result.fold(
            onSuccess = { agentResult ->
                val llmResponse = TaskResponseParser.parse(agentResult.responseText)
                if (llmResponse != null) {
                    processSuccessResponse(context, userPrompt, llmResponse, agent.config.id, onEvent)
                } else {
                    handleParseError(chat.id, agentResult.responseText, onEvent)
                }
            },
            onFailure = { error ->
                handleLlmError(chat.id, error, onEvent)
            }
        )
    }

    private suspend fun reportCurrentState(
        chatId: Long,
        state: TaskState,
        step: Int,
        facts: Map<String, String>
    ) {
        val totalStages = facts[TaskMemoryKeys.PLAN_TOTAL_STAGES]?.toIntOrNull() ?: 0
        val stageName = if (totalStages > 0) facts[TaskMemoryKeys.planStageName(step)] else null

        val stateDescription = when (state) {
            TaskState.INIT -> "Сбор требований и определение задачи"
            TaskState.PLANNING -> "Декомпозиция задачи на этапы"
            TaskState.EXECUTION -> buildString {
                append("Выполнение этапа $step")
                if (totalStages > 0) append(" из $totalStages")
                if (stageName != null) append(": $stageName")
            }
            TaskState.VERIFICATION -> buildString {
                append("Проверка этапа $step")
                if (totalStages > 0) append(" из $totalStages")
                if (stageName != null) append(": $stageName")
            }
            TaskState.DONE -> "Формирование итогового отчёта"
        }

        chatTools.addInfoMessage(chatId, "📍 Было Состояние: ${state.name} — $stateDescription")
    }

    private suspend fun loadFacts(agentId: Long): Map<String, String> {
        return getFactsByAgentUseCase(agentId)
            .associate { it.memoryKey to it.fact }
    }

    private fun determineCurrentState(facts: Map<String, String>): TaskState {
        val stateStr = facts[TaskMemoryKeys.CURRENT_STATE]
        return when {
            stateStr == null || stateStr.isBlank() -> TaskState.INIT
            // DONE state is preserved so DoneStateHandler can run on next doWork() call
            else -> TaskState.fromString(stateStr) ?: TaskState.INIT
        }
    }

    private suspend fun processSuccessResponse(
        context: TaskContext,
        userInput: String,
        llmResponse: TaskLlmResponse,
        agentId: Long,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        // Get handler for current state
        val handler = stateMachine.getHandler(context.currentState)

        // Handle state logic
        val result = handler.handle(context, userInput, llmResponse)

        // Validate state transition before applying
        val nextState = result.nextState
        if (nextState != null && nextState != context.currentState) {
            val validation = transitionValidator.validate(context.currentState, nextState)
            if (validation is TransitionValidator.ValidationResult.Invalid) {
                chatTools.addBotMessage(
                    context.chat.id,
                    "⚠️ Некорректный переход состояний: ${validation.error}"
                )
                return
            }
        }

        // Save memory updates to LTM
        saveMemoryUpdates(agentId, result.memoryUpdates)

        // Save current state
        val newState = result.nextState ?: context.currentState
        val newStep = result.newStep ?: context.currentStep
        saveCurrentState(agentId, newState, newStep)

        // Notify about state transition
        if (result.nextState != null && result.nextState != context.currentState) {
            val totalStages = context.getTotalStages()
            val newStageName = context.facts[TaskMemoryKeys.planStageName(newStep)]
            val stateLabel = when (result.nextState) {
                TaskState.PLANNING -> "PLANNING — Планирование"
                TaskState.EXECUTION -> buildString {
                    append("EXECUTION — Этап $newStep")
                    if (totalStages > 0) append(" из $totalStages")
                    if (newStageName != null) append(": $newStageName")
                }
                TaskState.VERIFICATION -> buildString {
                    append("VERIFICATION — Проверка этапа $newStep")
                    if (newStageName != null) append(": $newStageName")
                }
                TaskState.DONE -> "DONE — Финализация задачи"
                TaskState.INIT -> "INIT — Новая задача"
            }
            chatTools.addInfoMessage(context.chat.id, "🔄 Переход: $stateLabel")
        }

        // Send reply to user
        chatTools.addBotMessage(context.chat.id, result.replyToUser)

        // Handle state transition side-effects
        when {
            context.currentState == TaskState.DONE -> {
                // DoneStateHandler just ran — clear everything and reset
                clearAgentContextUseCase(agentId)
                clearTaskMemoryUseCase(agentId)
            }
            newState == TaskState.DONE -> {
                // Transitioned TO DONE from VERIFICATION — clear only dialog history.
                // Task memory is preserved so DoneStateHandler can read artifacts on next doWork() call.
                clearAgentContextUseCase(agentId)
            }
            result.nextState != null && result.nextState != context.currentState -> {
                // Transitioned to a new non-DONE state — clear history for fresh context
                clearAgentContextUseCase(agentId)
            }
        }
    }

    private suspend fun saveMemoryUpdates(agentId: Long, updates: Map<String, String>) {
        val validUpdates = memoryUpdateValidator.filterValid(updates)

        val invalidKeys = updates.keys - validUpdates.keys
        if (invalidKeys.isNotEmpty()) {
            println("Warning: Ignoring invalid memory keys: $invalidKeys")
        }

        validUpdates.forEach { (key, value) ->
            val category = when {
                key.startsWith("workflow:") -> TaskMemoryKeys.CAT_WORKFLOW
                key.startsWith("init:") -> TaskMemoryKeys.CAT_INIT
                key.startsWith("plan:") -> TaskMemoryKeys.CAT_PLAN
                key.startsWith("verif:") -> TaskMemoryKeys.CAT_VERIF
                key.startsWith("exec:") -> TaskMemoryKeys.CAT_EXEC
                else -> "general"
            }
            upsertFactUseCase(agentId, key, category, value)
        }
    }

    private suspend fun saveCurrentState(agentId: Long, state: TaskState, step: Int) {
        upsertFactUseCase(agentId, TaskMemoryKeys.CURRENT_STATE, TaskMemoryKeys.CAT_WORKFLOW, state.name)
        upsertFactUseCase(agentId, TaskMemoryKeys.CURRENT_STEP, TaskMemoryKeys.CAT_WORKFLOW, step.toString())
    }

    private suspend fun handleParseError(
        chatId: Long,
        rawResponse: String,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        onEvent?.invoke(WorkerEvent.RequestError("Parse error"))
        chatTools.addBotMessage(
            chatId,
            "⚠️ Не удалось обработать ответ от LLM. Пожалуйста, попробуйте переформулировать запрос."
        )
    }

    private suspend fun handleLlmError(
        chatId: Long,
        error: Throwable,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        val errorMessage = "❌ Ошибка при обращении к LLM: ${error.message}"
        onEvent?.invoke(WorkerEvent.RequestError(errorMessage))
        chatTools.addBotMessage(chatId, errorMessage)
    }
}
