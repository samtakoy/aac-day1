package com.example.day.core.core_features.agent.domain.workers.concrete

import android.util.Log
import com.example.day.core.core_features.agent.domain.AIAgent
import com.example.day.core.core_features.agent.domain.AIAgentFactory
import com.example.day.core.core_features.agent.domain.AgentContextRepository
import com.example.day.core.core_features.agent.domain.model.AContext
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.AContextParams
import com.example.day.core.core_features.agent.domain.model.AContextState
import com.example.day.core.core_features.agent.domain.repository.AgentMemoryRepository
import com.example.day.core.core_features.agent.domain.strategy.StrategyFactory
import com.example.day.core.core_features.agent.domain.tools.ToolCallOrchestrator
import com.example.day.core.core_features.agent.domain.tools.ToolProvider
import com.example.day.core.core_features.agent.domain.workers.base.AWorker
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.memory.domain.provider.AgentSystemPromptMemoryProvider
import com.example.day.core.core_features.memory.domain.provider.CompositeMemoryProvider
import com.example.day.core.core_features.memory.domain.provider.RagContextMemoryProvider
import com.example.day.core.core_features.memory.domain.provider.RagContextMemoryProviderFactory
import com.example.day.core.core_features.memory.domain.provider.base.MemoryProviderFactory
import kotlinx.collections.immutable.persistentListOf
import javax.inject.Inject

/**
 * Worker для Developer Assistant чата.
 *
 * Особенности:
 * - Системный промпт настраивается через onCreateCallback (как в JustWorkWorker)
 * - MCP-инструменты доступны автоматически через MemoryProviderFactory (без ограничений)
 * - Команда @@help активирует RAG-контекст через RagContextMemoryProvider
 * - Без @@help работает только с MCP-инструментами (git, файлы)
 */
class AssistantWorker @Inject constructor(
    private val aiAgentFactory: AIAgentFactory,
    private val chatTools: ChatTools,
    private val ragContextMemoryProviderFactory: RagContextMemoryProviderFactory,
    private val contextRepository: AgentContextRepository,
    private val llmRequestUseCase: LlmRequestUseCase,
    private val strategyFactory: StrategyFactory,
    private val toolProvider: ToolProvider,
    private val toolCallOrchestrator: ToolCallOrchestrator,
    private val agentMemoryRepository: AgentMemoryRepository,
    private val memoryProviderFactory: MemoryProviderFactory,
) : AWorker {

    companion object {
        const val AGENT_NAME = "assistant_agent"
        const val HELP_PREFIX = "@@help"
        private const val MSG_LIMIT = 8
        private const val EXTRA_LIMIT = 8

        private const val SYSTEM_PROMPT = "Твоя задача помогать пользователю разобираться с кодовой базой проекта. Ты можешь использовать инструменты чтобы запрашивать контекст по вопросам пользователя у RAG базы данных по проекту, а также можешь использовать инструменты для работы с git."
    }

    override suspend fun doWork(
        userPrompt: String,
        chat: Chat,
        userRole: AContextMessage.Role,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        Log.d(AGENT_NAME, "doWork: chat=${chat.id}, prompt='${userPrompt.take(80)}'")

        // 1. Определить isHelpRequest
        val isHelpRequest = userPrompt.trimStart().startsWith(HELP_PREFIX, ignoreCase = true)

        // 2. Вычислить cleanPrompt
        val cleanPrompt = if (isHelpRequest) {
            userPrompt.trimStart().substringAfter(HELP_PREFIX).trimStart()
        } else {
            userPrompt
        }

        // 3. Получить/создать агента с onCreateCallback для systemPrompt
        val baseAgent = aiAgentFactory.getOrCreate(
            systemName = AGENT_NAME,
            chatId = chat.id,
            systemPrompt = "",  // пусто! настраивается через onCreateCallback
            defaultModel = { chat.settings.model },
            defaultContext = {
                AContext(
                    params = AContextParams.Summarization(msgLimit = MSG_LIMIT, extraLimit = EXTRA_LIMIT),
                    data = AContextState.Summary("", persistentListOf())
                )
            },
            onCreateCallback = { agentId ->
                agentMemoryRepository.upsertFact(
                    agentId = agentId,
                    memoryKey = AgentSystemPromptMemoryProvider.MEMORY_KEY,
                    category = AgentSystemPromptMemoryProvider.CATEGORY,
                    fact = SYSTEM_PROMPT
                )
                // allowedTools НЕ сохраняем — все зарегистрированные инструменты разрешены
            }
        )

        val agentId = baseAgent.config.id
        Log.d(AGENT_NAME, "agentId=$agentId")

        // 4. Строим AIAgent вручную (как RagWorker), чтобы подменить memoryProvider.
        // Двойной bindAgentId на синглтонах — намеренно, agentId совпадает.
        val baseMemoryProvider = memoryProviderFactory.create(
            memoryTypes = baseAgent.config.memoryTypes,
            agentId = agentId
        )

        // 5. Построить итоговый memoryProvider
        val ragContextProvider = if (isHelpRequest) {
            ragContextMemoryProviderFactory.create(agentId)
        } else {
            null
        }

        val memoryProvider: CompositeMemoryProvider = if (ragContextProvider != null) {
            CompositeMemoryProvider(listOf(baseMemoryProvider, ragContextProvider))
        } else {
            @Suppress("UNUSED_EXPRESSION")
            baseMemoryProvider as? CompositeMemoryProvider ?: CompositeMemoryProvider(listOf(baseMemoryProvider))
        }

        // 6. Построить strategy
        val strategy = strategyFactory.create(baseAgent.config.contextStrategyType)

        // 7. Построить агента
        val agent = AIAgent(
            config = baseAgent.config,
            contextRepository = contextRepository,
            llmProvider = llmRequestUseCase,
            strategy = strategy,
            memoryProvider = memoryProvider,
            toolProvider = toolProvider,
            orchestrator = toolCallOrchestrator,
        )

        // 8. Вызвать agent.process()
        val result = agent.process(
            prompt = AContextMessage(userRole, cleanPrompt),
            onEvent = onEvent
        )

        // 9. Обработать результат
        result.fold(
            onSuccess = { agentResult ->
                Log.d(AGENT_NAME, "success: response length=${agentResult.responseText.length}")
                agentResult.requestDebugInfo?.let { chatTools.addInfoMessage(chat.id, it) }
                agentResult.reportMessage?.let { chatTools.addInfoMessage(chat.id, it) }
                chatTools.addBotMessage(chat.id, agentResult.responseText)
                ragContextProvider?.postProcess()
            },
            onFailure = { error ->
                Log.e(AGENT_NAME, "error: ${error.message}")
                chatTools.addBotMessage(chat.id, "❌ Ошибка: ${error.message}")
            }
        )
    }
}
