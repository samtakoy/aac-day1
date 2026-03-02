package com.example.day.core.core_features.agent.domain.workers

import com.example.day.core.core_features.agent.domain.AgentContextRepository
import com.example.day.core.core_features.agent.domain.AgentRepository
import com.example.day.core.core_features.agent.domain.AIAgentFactory
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.AContextParams
import com.example.day.core.core_features.agent.domain.model.AContextState
import com.example.day.core.core_features.agent.domain.model.AgentConfig
import com.example.day.core.core_features.agent.domain.strategy.CtxStrategyType
import com.example.day.core.core_features.agent.domain.strategy.ContextStrategyConstants
import com.example.day.core.core_features.agent.domain.strategy.impl.ContextBranchingStrategy
import com.example.day.core.core_features.agent.domain.workers.base.AWorker
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.agent.domain.workers.innercommand.InnerCommand
import com.example.day.core.core_features.agent.domain.workers.innercommand.InnerCommandParser
import com.example.day.core.core_features.agent.domain.workers.innercommand.InnerCommandParserResult
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import javax.inject.Inject

/**
 * Agent with context support and context compression (Context Management).
 * Saves message history between requests to database.
 *
 * Delegates all context logic to [AIAgentFactory] → [AIAgent] → [ContextStrategy].
 *
 * Поддерживает команды:
 * - @@talk(info) - вывести настройки контекста
 * - @@talk(context) - вывести полное содержимое контекста
 * - @@talk(setup --msg X --extra Y) - настроить Summarization стратегию
 * - @@talk(setup_sliding --msg X) - настроить SlidingWindow стратегию
 * - @@talk(setup_sticky --msg X --facts Y) - настроить StickyFacts стратегию
 * - @@talk текст - отправить текст к LLM
 */
class TalkWorker @Inject constructor(
    private val aiAgentFactory: AIAgentFactory,
    private val agentRepository: AgentRepository,
    private val contextRepository: AgentContextRepository,
    private val chatTools: ChatTools
) : AWorker {

    private val innerCommandParser = InnerCommandParser()

    companion object {
        const val AGENT_NAME = "talk_agent"

        private const val INFO = "info"
        private const val CONTEXT = "context"
        private const val SETUP = "setup"
        private const val SETUP_SLIDING = "setup_sliding"
        private const val SETUP_STICKY = "setup_sticky"
        private const val SETUP_BRANCHES = "setup_branches"
        private const val NEW_BRANCH = "new_branch"
        private const val SWITCH_BRANCH = "switch_branch"
        private const val LIST_BRANCHES = "list_branches"
        private const val DELETE_BRANCH = "delete_branch"
        private val REGISTERED_COMMANDS = setOf(
            INFO, CONTEXT, SETUP, SETUP_SLIDING, SETUP_STICKY,
            SETUP_BRANCHES, NEW_BRANCH, SWITCH_BRANCH, LIST_BRANCHES, DELETE_BRANCH
        )
    }

    override suspend fun doWork(
        task: String,
        chat: Chat,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        val commandHandled = processCommand(task, chat)
        if (commandHandled) return
        processMessage(task, chat, onEvent)
    }

    /**
     * Обработать управляющую команду, если это она.
     * @return true если команда обработана
     */
    private suspend fun processCommand(task: String, chat: Chat): Boolean {
        val parseResult = innerCommandParser.tryExtractCommand(task)

        return when {
            parseResult.command == null && parseResult.error == null -> false // No command in input
            parseResult.error != null -> {
                chatTools.addBotMessage(chat.id, parseResult.error)
                true
            }
            parseResult.command != null -> {
                val command = parseResult.command
                if (command.name !in REGISTERED_COMMANDS) {
                    chatTools.addBotMessage(chat.id, "неизвестные параметры: ${command.name}")
                    return true
                }

                when (command.name) {
                    INFO -> showContextInfo(chat)
                    CONTEXT -> showFullContext(chat)
                    SETUP -> executeSetupCommand(command.parameters, chat)
                    SETUP_SLIDING -> executeSetupSlidingCommand(command.parameters, chat)
                    SETUP_STICKY -> executeSetupStickyCommand(command.parameters, chat)
                    SETUP_BRANCHES -> executeSetupBranchesCommand(command.parameters, chat)
                    NEW_BRANCH -> executeNewBranchCommand(command.parameters, chat)
                    SWITCH_BRANCH -> executeSwitchBranchCommand(command.parameters, chat)
                    LIST_BRANCHES -> executeListBranchesCommand(chat)
                    DELETE_BRANCH -> executeDeleteBranchCommand(command.parameters, chat)
                    else -> chatTools.addBotMessage(chat.id, "неизвестные параметры: ${command.name}")
                }
                true
            }
            else -> false
        }
    }

    private suspend fun executeSetupCommand(params: List<Pair<String, String?>>, chat: Chat) {
        val paramsMap = params.toMap()
        val msgParam = paramsMap[ContextStrategyConstants.PARAM_MSG_LIMIT]?.toIntOrNull()
        val extraParam = paramsMap[ContextStrategyConstants.PARAM_EXTRA_LIMIT]?.toIntOrNull()

        if (msgParam == null || extraParam == null) {
            chatTools.addBotMessage(
                chat.id,
                "неизвестные параметры: требуется --${ContextStrategyConstants.PARAM_MSG_LIMIT} и --${ContextStrategyConstants.PARAM_EXTRA_LIMIT}"
            )
            return
        }
        setupSummarizationStrategy(chat, msgParam, extraParam)
    }

    private suspend fun executeSetupSlidingCommand(params: List<Pair<String, String?>>, chat: Chat) {
        val paramsMap = params.toMap()
        val msgParam = paramsMap[ContextStrategyConstants.PARAM_MSG_LIMIT]?.toIntOrNull()

        if (msgParam == null) {
            chatTools.addBotMessage(
                chat.id,
                "неизвестные параметры: требуется --${ContextStrategyConstants.PARAM_MSG_LIMIT}"
            )
            return
        }
        setupSlidingWindowStrategy(chat, msgParam)
    }

    private suspend fun executeSetupStickyCommand(params: List<Pair<String, String?>>, chat: Chat) {
        val paramsMap = params.toMap()
        val msgParam = paramsMap[ContextStrategyConstants.PARAM_MSG_LIMIT]?.toIntOrNull()
        val factsParam = paramsMap[ContextStrategyConstants.PARAM_MAX_FACTS]?.toIntOrNull()

        if (msgParam == null) {
            chatTools.addBotMessage(
                chat.id,
                "неизвестные параметры: требуется --${ContextStrategyConstants.PARAM_MSG_LIMIT}"
            )
            return
        }
        setupStickyFactsStrategy(chat, msgParam, factsParam ?: 20) // 20 - значение по умолчанию
    }

    private suspend fun executeSetupBranchesCommand(params: List<Pair<String, String?>>, chat: Chat) {
        val paramsMap = params.toMap()
        val mainBranchParam = paramsMap[ContextStrategyConstants.PARAM_DEFAULT_BRANCH]
        setupBranchingStrategy(chat, mainBranchParam ?: ContextBranchingStrategy.DEFAULT_BRANCH_ID)
    }

    private suspend fun executeNewBranchCommand(params: List<Pair<String, String?>>, chat: Chat) {
        val paramsMap = params.toMap()
        val branchId = paramsMap[ContextStrategyConstants.PARAM_BRANCH_ID]

        if (branchId == null) {
            chatTools.addBotMessage(
                chat.id,
                "Требуется указать ID ветки: @@talk(new_branch --id myBranch)"
            )
            return
        }

        val agent = getOrCreateAgent(chat)

        // Check if strategy is Branching
        if (agent.contextStrategyType != CtxStrategyType.BRANCHING) {
            chatTools.addBotMessage(
                chat.id,
                "Стратегия ветвления не активирована. Используйте: @@talk(setup_branches)"
            )
            return
        }

        val strategy = ContextBranchingStrategy()
        val success = strategy.createBranch(agent.id, branchId, contextRepository)

        if (success) {
            chatTools.addInfoMessage(chat.id, "Создана новая ветка: $branchId")
        } else {
            chatTools.addBotMessage(chat.id, "Ветка $branchId уже существует")
        }
    }

    private suspend fun executeSwitchBranchCommand(params: List<Pair<String, String?>>, chat: Chat) {
        val paramsMap = params.toMap()
        val branchId = paramsMap[ContextStrategyConstants.PARAM_BRANCH_ID]

        if (branchId == null) {
            chatTools.addBotMessage(
                chat.id,
                "Требуется указать ID ветки: @@talk(switch_branch --id myBranch)"
            )
            return
        }

        val agent = getOrCreateAgent(chat)

        // Check if strategy is Branching
        if (agent.contextStrategyType != CtxStrategyType.BRANCHING) {
            chatTools.addBotMessage(
                chat.id,
                "Стратегия ветвления не активирована. Используйте: @@talk(setup_branches)"
            )
            return
        }

        val strategy = ContextBranchingStrategy()
        val success = strategy.switchBranch(agent.id, branchId, contextRepository)

        if (success) {
            chatTools.addInfoMessage(chat.id, "Переключено на ветку: $branchId")
        } else {
            chatTools.addBotMessage(chat.id, "Ветка $branchId не существует")
        }
    }

    private suspend fun executeListBranchesCommand(chat: Chat) {
        val agent = getOrCreateAgent(chat)

        // Check if strategy is Branching
        if (agent.contextStrategyType != CtxStrategyType.BRANCHING) {
            chatTools.addBotMessage(
                chat.id,
                "Стратегия ветвления не активирована. Используйте: @@talk(setup_branches)"
            )
            return
        }

        val strategy = ContextBranchingStrategy()
        val branchList = strategy.listBranches(agent.id, contextRepository)
        chatTools.addInfoMessage(chat.id, branchList)
    }

    private suspend fun executeDeleteBranchCommand(params: List<Pair<String, String?>>, chat: Chat) {
        val paramsMap = params.toMap()
        val branchId = paramsMap[ContextStrategyConstants.PARAM_BRANCH_ID]

        if (branchId == null) {
            chatTools.addBotMessage(
                chat.id,
                "Требуется указать ID ветки: @@talk(delete_branch --id myBranch)"
            )
            return
        }

        val agent = getOrCreateAgent(chat)

        // Check if strategy is Branching
        if (agent.contextStrategyType != CtxStrategyType.BRANCHING) {
            chatTools.addBotMessage(
                chat.id,
                "Стратегия ветвления не активирована. Используйте: @@talk(setup_branches)"
            )
            return
        }

        val strategy = ContextBranchingStrategy()
        val success = strategy.deleteBranch(agent.id, branchId, contextRepository)

        if (success) {
            chatTools.addInfoMessage(chat.id, "Ветка удалена: $branchId")
        } else {
            chatTools.addBotMessage(chat.id, "Не удалось удалить ветку $branchId (возможно, это текущая ветка)")
        }
    }

    /**
     * Переключает стратегию на StickyFacts.
     * Если текущая стратегия уже StickyFacts - просто обновляет параметры.
     * Иначе - мигрирует сообщения из текущей стратегии.
     */
    private suspend fun setupStickyFactsStrategy(chat: Chat, windowSize: Int, maxFacts: Int) {
        val agent = getOrCreateAgent(chat)
        val currentStrategyType = agent.contextStrategyType

        if (currentStrategyType == CtxStrategyType.STICKY_FACTS) {
            // Уже StickyFacts - просто обновляем параметры
            contextRepository.saveContextParams(
                agent.id,
                AContextParams.StickyFacts(windowSize, maxFacts)
            )
            chatTools.addInfoMessage(chat.id, "Настройки StickyFacts обновлены: окно=$windowSize, макс.фактов=$maxFacts")
        } else {
            // Нужно переключить стратегию
            val messages = extractMessagesFromCurrentStrategy(agent)

            // Обновляем тип стратегии в агенте
            val updatedAgent = agent.copy(contextStrategyType = CtxStrategyType.STICKY_FACTS)
            agentRepository.updateAgent(updatedAgent)

            // Сохраняем новые параметры и состояние
            contextRepository.saveContextParams(
                agent.id,
                AContextParams.StickyFacts(windowSize, maxFacts)
            )
            // Берем только последние windowSize сообщений для окна
            val windowMessages = if (messages.size > windowSize) {
                messages.takeLast(windowSize)
            } else {
                messages
            }
            contextRepository.saveContextState(
                agent.id,
                AContextState.StickyFacts(facts = emptyMap(), messages = windowMessages.toPersistentList())
            )

            chatTools.addInfoMessage(
                chat.id,
                "Стратегия переключена на StickyFacts. " +
                "Окно: $windowSize сообщений, макс.фактов: $maxFacts. " +
                "Мигрировано сообщений: ${windowMessages.size} из ${messages.size}"
            )
        }
    }

    /**
     * Переключает стратегию на Summarization.
     * Если текущая стратегия уже Summarization - просто обновляет параметры.
     * Иначе - мигрирует сообщения из текущей стратегии.
     */
    private suspend fun setupSummarizationStrategy(chat: Chat, msgLimit: Int, extraLimit: Int) {
        val agent = getOrCreateAgent(chat)
        val currentStrategyType = agent.contextStrategyType

        if (currentStrategyType == CtxStrategyType.SUMMARIZATION) {
            // Уже Summarization - просто обновляем параметры
            contextRepository.saveContextParams(
                agent.id,
                AContextParams.Summarization(msgLimit, extraLimit)
            )
            chatTools.addInfoMessage(chat.id, "Настройки обновлены: msg=$msgLimit, extra=$extraLimit")
        } else {
            // Нужно переключить стратегию
            val messages = extractMessagesFromCurrentStrategy(agent)

            // Обновляем тип стратегии в агенте
            val updatedAgent = agent.copy(contextStrategyType = CtxStrategyType.SUMMARIZATION)
            agentRepository.updateAgent(updatedAgent)

            // Сохраняем новые параметры и состояние
            contextRepository.saveContextParams(
                agent.id,
                AContextParams.Summarization(msgLimit, extraLimit)
            )
            contextRepository.saveContextState(
                agent.id,
                AContextState.Summary(summary = "", messages = messages.toPersistentList())
            )

            chatTools.addInfoMessage(
                chat.id,
                "Стратегия переключена на Summarization. " +
                "msg=$msgLimit, extra=$extraLimit. " +
                "Мигрировано сообщений: ${messages.size}"
            )
        }
    }

    /**
     * Переключает стратегию на SlidingWindow.
     * Если текущая стратегия уже SlidingWindow - просто обновляет параметры.
     * Иначе - мигрирует сообщения из текущей стратегии.
     */
    private suspend fun setupSlidingWindowStrategy(chat: Chat, windowSize: Int) {
        val agent = getOrCreateAgent(chat)
        val currentStrategyType = agent.contextStrategyType

        if (currentStrategyType == CtxStrategyType.SLIDING_WINDOW) {
            // Уже SlidingWindow - просто обновляем параметры
            contextRepository.saveContextParams(
                agent.id,
                AContextParams.SlidingWindow(windowSize)
            )
            chatTools.addInfoMessage(chat.id, "Размер окна обновлён: $windowSize сообщений")
        } else {
            // Нужно переключить стратегию
            val messages = extractMessagesFromCurrentStrategy(agent)

            // Обновляем тип стратегии в агенте
            val updatedAgent = agent.copy(contextStrategyType = CtxStrategyType.SLIDING_WINDOW)
            agentRepository.updateAgent(updatedAgent)

            // Сохраняем новые параметры и состояние
            contextRepository.saveContextParams(
                agent.id,
                AContextParams.SlidingWindow(windowSize)
            )
            // Берем только последние windowSize сообщений для окна
            val windowMessages = if (messages.size > windowSize) {
                messages.takeLast(windowSize)
            } else {
                messages
            }
            contextRepository.saveContextState(
                agent.id,
                AContextState.SlidingWindow(messages = windowMessages.toPersistentList())
            )

            chatTools.addInfoMessage(
                chat.id,
                "Стратегия переключена на SlidingWindow. " +
                "Окно: $windowSize сообщений. " +
                "Мигрировано сообщений: ${windowMessages.size} из ${messages.size}"
            )
        }
    }

    /**
     * Переключает стратегию на Branching (ветвление).
     * Если текущая стратегия уже Branching - просто обновляет параметры.
     * Иначе - мигрирует сообщения из текущей стратегии.
     */
    private suspend fun setupBranchingStrategy(chat: Chat, defaultBranch: String) {
        val agent = getOrCreateAgent(chat)
        val currentStrategyType = agent.contextStrategyType

        if (currentStrategyType == CtxStrategyType.BRANCHING) {
            // Уже Branching - просто обновляем параметры
            contextRepository.saveContextParams(
                agent.id,
                AContextParams.Branching(defaultBranchId = defaultBranch)
            )
            chatTools.addInfoMessage(chat.id, "Настройки Branching обновлены: основная ветка=$defaultBranch")
        } else {
            // Нужно переключить стратегию
            val messages = extractMessagesFromCurrentStrategy(agent)

            // Обновляем тип стратегии в агенте
            val updatedAgent = agent.copy(contextStrategyType = CtxStrategyType.BRANCHING)
            agentRepository.updateAgent(updatedAgent)

            // Сохраняем новые параметры и состояние
            contextRepository.saveContextParams(
                agent.id,
                AContextParams.Branching(defaultBranchId = defaultBranch)
            )
            contextRepository.saveContextState(
                agent.id,
                AContextState.Branching(
                    branches = kotlinx.collections.immutable.persistentMapOf(
                        defaultBranch to messages.toPersistentList()
                    ),
                    currentBranchId = defaultBranch,
                    defaultBranchId = defaultBranch
                )
            )

            chatTools.addInfoMessage(
                chat.id,
                "Стратегия переключена на Branching (ветвление). " +
                "Основная ветка: $defaultBranch. " +
                "Мигрировано сообщений: ${messages.size}"
            )
        }
    }

    /**
     * Извлекает список сообщений из текущего состояния контекста независимо от типа стратегии.
     */
    private suspend fun extractMessagesFromCurrentStrategy(agent: AgentConfig): List<AContextMessage> {
        val state = contextRepository.getContextState(agent.id)
        return when (state) {
            is AContextState.Summary -> state.messages
            is AContextState.SlidingWindow -> state.messages
            is AContextState.StickyFacts -> state.messages
            is AContextState.Branching -> state.branches[state.currentBranchId] ?: emptyList()
            is AContextState.Full -> state.messages
            AContextState.Empty,
            null -> emptyList()
        }
    }

    private suspend fun getOrCreateAgent(chat: Chat): AgentConfig {
        return agentRepository.getOrCreateAgent(
            systemName = AGENT_NAME,
            isCommon = false,
            chatId = chat.id,
            chatSettings = chat.settings
        )
    }

    private suspend fun showContextInfo(chat: Chat) {
        val agent = aiAgentFactory.getOrCreate(AGENT_NAME, chat.id, false, chat.settings)
        chatTools.addInfoMessage(chat.id, agent.getInfo())
    }

    private suspend fun showFullContext(chat: Chat) {
        val agent = aiAgentFactory.getOrCreate(AGENT_NAME, chat.id, false, chat.settings)
        chatTools.addInfoMessage(chat.id, agent.getFullContext())
    }

    private suspend fun setupContextParameters(chat: Chat, msgLimit: Int, extraLimit: Int) {
        val agent = aiAgentFactory.getOrCreate(AGENT_NAME, chat.id, false, chat.settings)
        val params = mapOf(
            ContextStrategyConstants.PARAM_MSG_LIMIT to msgLimit.toString(),
            ContextStrategyConstants.PARAM_EXTRA_LIMIT to extraLimit.toString()
        )
        chatTools.addInfoMessage(chat.id, agent.setupParams(params))
    }

    private suspend fun processMessage(
        task: String,
        chat: Chat,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        val agent = aiAgentFactory.getOrCreate(AGENT_NAME, chat.id, false, chat.settings)
        agent.process(chat.settings, task, onEvent)
            .onSuccess { result ->
                result.reportMessage?.let { chatTools.addInfoMessage(chat.id, it) }
                chatTools.addBotMessage(chat.id, result.responseText)
            }
            .onFailure { exception ->
                chatTools.addBotMessage(chat.id, exception.stackTraceToString())
            }
    }
}
