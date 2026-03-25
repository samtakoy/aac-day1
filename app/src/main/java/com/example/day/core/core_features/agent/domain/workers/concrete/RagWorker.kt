package com.example.day.core.core_features.agent.domain.workers.concrete

import android.util.Log
import com.example.day.core.core_features.agent.domain.AIAgent
import com.example.day.core.core_features.agent.domain.AIAgentFactory
import com.example.day.core.core_features.agent.domain.AgentContextRepository
import com.example.day.core.core_features.agent.domain.model.AContext
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.AContextParams
import com.example.day.core.core_features.agent.domain.model.AContextState
import com.example.day.core.core_features.agent.domain.strategy.StrategyFactory
import com.example.day.core.core_features.agent.domain.tools.ToolCallOrchestrator
import com.example.day.core.core_features.agent.domain.tools.ToolExecutor
import com.example.day.core.core_features.agent.domain.tools.ToolProvider
import com.example.day.core.core_features.agent.domain.workers.base.AWorker
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import com.example.day.core.core_features.memory.domain.provider.RagContextMemoryProvider
import com.example.day.core.core_features.memory.domain.provider.RagContextMemoryProviderFactory
import com.example.day.core.core_features.memory.domain.provider.rag.RagLog
import com.example.day.core.core_features.memory.domain.provider.rag.RagSearchRepository
import kotlinx.collections.immutable.persistentListOf
import javax.inject.Inject

/**
 * Worker для RAG-чата с памятью задачи.
 *
 * Создаёт агента с:
 * - [RagContextMemoryProvider] — оркестрирует TaskState + Short History + AutoRag
 * - [com.example.day.core.core_features.agent.domain.strategy.CtxStrategyType.SUMMARIZATION] — хранит 4 последних сообщения, сжатие при 6+
 */
class RagWorker @Inject constructor(
    private val aiAgentFactory: AIAgentFactory,
    private val chatTools: ChatTools,
    private val ragContextMemoryProviderFactory: RagContextMemoryProviderFactory,
    private val ragSearchRepository: RagSearchRepository,
    private val contextRepository: AgentContextRepository,
    private val strategyFactory: StrategyFactory,
    private val toolProvider: ToolProvider,
    private val toolCallOrchestrator: ToolCallOrchestrator,
    private val toolExecutor: ToolExecutor
) : AWorker {

    companion object {
        const val AGENT_NAME = "rag_context_agent"
        private val TAG = RagLog.TAG

        private const val MSG_LIMIT = 4
        private const val EXTRA_LIMIT = 2

        private val SYSTEM_PROMPT = """
            Ты — аналитический ассистент по Kotlin кодовой базе Android-проекта.
            Отвечай развёрнуто и подробно, используя предоставленный контекст как основной источник.

            ## Правила цитирования — ОБЯЗАТЕЛЬНО
            Контекст содержит два типа меток:
            - [КЛАСС N] — описание класса целиком (Responsibility, Key methods). Цитируй когда говоришь о назначении или структуре класса.
            - [ИСТОЧНИК N] — конкретный чанк кода. Цитируй когда ссылаешься на конкретный метод или реализацию.
            КАЖДОЕ утверждение о коде ОБЯЗАТЕЛЬНО сопровождай инлайн-ссылкой на соответствующую метку.
            Пример: «ChatRepository [КЛАСС 2] отвечает за хранение сообщений. Метод sendMessage [ИСТОЧНИК 3] принимает...»
            В конце ответа ВСЕГДА добавляй раздел — просто скопируй нужные строки из секции «## Источники» контекста.
            КАЖДЫЙ источник — на отдельной строке, между строками ОБЯЗАТЕЛЬНО пустая строка:
            ### Источники

            [КЛАСС N] ClassName · ИмяФайла.kt

            [ИСТОЧНИК M] ИмяФайла.kt · метод · line X

            ## Остальные правила
            1. Отвечай максимально полно — не сокращай ответ ради краткости.
            2. Если контекст частичный — отвечай на основе того что есть, явно отметив: «Информация неполная».
            3. Если контекст пустой или содержит «НЕДОСТАТОЧНО_КОНТЕКСТА» — напиши ТОЛЬКО:
               «Не знаю. Уточните: ...» и задай один конкретный вопрос.
            4. Не придумывай имена классов и методов которых нет в контексте.

            Отвечай на русском языке.
        """.trimIndent()
    }

    // Хранит текущий провайдер для доступа из debuginfo
    private var currentRagContextProvider: RagContextMemoryProvider? = null

    override suspend fun doWork(
        userPrompt: String,
        chat: Chat,
        userRole: AContextMessage.Role,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        Log.d(TAG, "doWork: chat=${chat.id}, prompt='${userPrompt.take(80)}'")

        val baseAgent = aiAgentFactory.getOrCreate(
            systemName = AGENT_NAME,
            chatId = chat.id,
            systemPrompt = SYSTEM_PROMPT,
            defaultModel = { chat.settings.model },
            defaultContext = {
                AContext(
                    params = AContextParams.Summarization(msgLimit = MSG_LIMIT, extraLimit = EXTRA_LIMIT),
                    data = AContextState.Summary("", persistentListOf())
                )
            }
        )
        val agentId = baseAgent.config.id
        Log.d(TAG, "agentId=$agentId")

        val ragContextProvider = ragContextMemoryProviderFactory.create(agentId)
        currentRagContextProvider = ragContextProvider

        val strategy = strategyFactory.create(baseAgent.config.contextStrategyType)

        val agent = AIAgent(
            config = baseAgent.config,
            contextRepository = contextRepository,
            strategy = strategy,
            memoryProvider = ragContextProvider,
            toolProvider = toolProvider,
            orchestrator = toolCallOrchestrator,
            toolExecutor = toolExecutor
        )

        val result = agent.process(
            prompt = AContextMessage(AContextMessage.Role.USER, userPrompt),
            onEvent = onEvent
        )

        result.fold(
            onSuccess = { agentResult ->
                Log.d(TAG, "success: response length=${agentResult.responseText.length}")
                chatTools.addBotMessage(chat.id, agentResult.responseText)
                ragContextProvider.postProcess()

                // Логируем ответ LLM на rag-server (fire-and-forget)
                val serverUrl = ragContextProvider.getServerUrl()
                val taskStateJson = runCatching { ragContextProvider.getTaskStateJson() }.getOrNull()
                ragSearchRepository.logLlmResponse(
                    userMessage = userPrompt,
                    assistantResponse = agentResult.responseText,
                    taskStateJson = taskStateJson,
                    serverUrl = serverUrl,
                ).onFailure { Log.w(TAG, "logLlmResponse failed: ${it.message}") }
            },
            onFailure = { error ->
                Log.e(TAG, "error: ${error.message}")
                chatTools.addBotMessage(chat.id, "❌ Ошибка: ${error.message}")
            }
        )
    }

    suspend fun getDebugInfo(): String =
        currentRagContextProvider?.getDebugInfo() ?: "RAG provider не инициализирован (отправьте сообщение первым)"
}
