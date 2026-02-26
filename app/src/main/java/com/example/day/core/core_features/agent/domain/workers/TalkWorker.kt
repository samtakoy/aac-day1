package com.example.day.core.core_features.agent.domain.workers

import com.example.day.core.core_features.agent.domain.model.AContext
import com.example.day.core.core_features.agent.domain.model.AContext.Companion.DEFAULT_EXTRA_LIMIT
import com.example.day.core.core_features.agent.domain.model.AContext.Companion.NO_SUMMARY_LIMIT
import com.example.day.core.core_features.agent.domain.model.Agent
import com.example.day.core.core_features.agent.domain.model.ContextParameters
import com.example.day.core.core_features.agent.domain.model.addAssistantMessage
import com.example.day.core.core_features.agent.domain.model.addUserMessage
import com.example.day.core.core_features.agent.domain.model.summarization.SummarizationEnabledState
import com.example.day.core.core_features.agent.domain.workers.base.AWorker
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.agent.domain.workers.base.askLlm
import com.example.day.core.core_features.agent.domain.workers.tools.AgentTools
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import com.example.day.core.core_features.agent.domain.workers.tools.ContextFormatter
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.llm.domain.model.getContent
import javax.inject.Inject

/**
 * Agent with context support and context compression (Context Management).
 * Saves message history between requests to database.
 * 
 * Поддерживает команды:
 * - @@talk(info) - вывести настройки контекста
 * - @@talk(context) - вывести полное содержимое контекста
 * - @@talk(setup --msg X --extra Y) - настроить параметры сжатия
 * - @@talk текст - отправить текст к LLM
 */
class TalkWorker @Inject constructor(
    private val llmRequestUseCase: LlmRequestUseCase,
    private val agentTools: AgentTools,
    private val chatTools: ChatTools,
    private val contextFormatter: ContextFormatter
) : AWorker {

    companion object {
        const val AGENT_NAME = "talk_agent"
        
        // Паттерн для распознавания команды в формате (command_name --param value)
        // Проверяем, что строка начинается с открывающей скобки
        private val COMMAND_PATTERN = Regex("""^\s*\((.*)\)\s*$""")

        private const val INFO = "info"
        private const val CONTEXT = "context"
        private const val SETUP = "setup"
        private const val MSG_PARAM = "msg"
        private const val EXTRA_PARAM = "extra"
        // Зарегистрированные команды
        private val REGISTERED_COMMANDS = setOf(INFO, CONTEXT, SETUP)
    }

    override suspend fun doWork(
        task: String,
        chat: Chat,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        // 0. Проверка на управляющие команды
        val commandHandled = processCommand(task, chat)
        if (commandHandled) {
            return  // Команда обработана
        }

        // Обычная обработка сообщения
        processMessage(task, chat, onEvent)
    }

    /**
     * Обработать управляющую команду, если это она.
     * Поддерживает формат: (command_name --param value)
     * @return true если команда обработана, false если это обычное сообщение
     */
    private suspend fun processCommand(task: String, chat: Chat): Boolean {
        val content = task.trim()
        
        // Проверяем, начинается ли строка с открывающей скобки
        if (!content.startsWith("(")) {
            return false // Нет скобок - обычная обработка сообщения
        }
        
        // Извлекаем содержимое внутри скобок
        val matchResult = COMMAND_PATTERN.matchEntire(content)
        if (matchResult == null) {
            chatTools.addBotMessage(chat.id, "неверный формат команды - нет закрывающей скобки")
            return true
        }
        
        val innerContent = matchResult.groupValues[1].trim()
        
        // Разбиваем на куски по символу --
        val parts = innerContent.split("--")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        
        if (parts.isEmpty()) {
            chatTools.addBotMessage(chat.id, "параметры команды не указаны или неверный формат")
            return true
        }
        
        // Индекс 0 - это имя команды
        val commandName = parts[0].lowercase().trim()
        
        // Проверяем, зарегистрирована ли команда
        if (commandName !in REGISTERED_COMMANDS) {
            chatTools.addBotMessage(chat.id, "неизвестные параметры: $commandName")
            return true
        }
        
        // Остальные индексы - это параметры (пока без значений или с значениями --key value)
        val params = parts.drop(1).mapNotNull { parseParameter(it) }
        
        // Выполняем команду
        when (commandName) {
            INFO -> showContextInfo(chat)
            CONTEXT -> showFullContext(chat)
            SETUP -> executeSetupCommand(params, chat)
            else -> chatTools.addBotMessage(chat.id, "неизвестные параметры: $commandName")
        }
        return true
    }
    
    /**
     * Разобрать параметр в формате "key value" или просто "flag"
     * @return Pair(key, value) или null если параметр без значения
     */
    private fun parseParameter(param: String): Pair<String, String?>? {
        val pair = param.trim().split(" ").filter { it.isNotBlank() }
        return when {
            pair.isEmpty() -> null
            pair.size > 1 -> Pair(pair[0], pair[1])
            else -> Pair(pair[0], null)
        }
    }
    
    /**
     * Выполнить команду setup с параметрами
     */
    private suspend fun executeSetupCommand(params: List<Pair<String, String?>>, chat: Chat) {
        var msgParam: Int? = null
        var extraParam: Int? = null
        
        for ((key, value) in params) {
            when (key.lowercase()) {
                MSG_PARAM -> {
                    msgParam = value?.toIntOrNull()
                }
                EXTRA_PARAM -> {
                    extraParam = value?.toIntOrNull()
                }
            }
        }
        
        // Проверяем, что оба параметра предоставлены
        if (msgParam == null || extraParam == null) {
            chatTools.addBotMessage(chat.id, "неизвестные параметры: требуется --$MSG_PARAM и --$EXTRA_PARAM")
            return
        }
        
        setupContextParameters(chat, msgParam, extraParam)
    }

    /**
     * Показать информацию о текущих настройках контекста
     */
    private suspend fun showContextInfo(chat: Chat) {
        val agent = agentTools.getOrCreateAgent(
            systemName = AGENT_NAME,
            chatId = chat.id,
            isCommonAgent = false
        )
        val context = agentTools.getContext(agent.id)
        val state = context.summarizationState
        val params = ContextParameters(
            msgLimit = if (state is SummarizationEnabledState) state.msgLimit else NO_SUMMARY_LIMIT,
            extraLimit = if (state is SummarizationEnabledState) state.extraLimit else DEFAULT_EXTRA_LIMIT,
            strategy = state.strategyName
        )
        
        val message = contextFormatter.formatContextInfo(params)
        chatTools.addInfoMessage(chat.id, message)
    }

    /**
     * Показать полное содержимое контекста
     */
    private suspend fun showFullContext(chat: Chat) {
        val agent = agentTools.getOrCreateAgent(
            systemName = AGENT_NAME,
            chatId = chat.id,
            isCommonAgent = false
        )
        
        val context = agentTools.getContext(agent.id)
        val messageCount = context.messages.size

        val message = contextFormatter.formatFullContext(agent.systemName, context, messageCount)
        chatTools.addInfoMessage(chat.id, message)
    }

    /**
     * Настроить параметры контекста
     */
    private suspend fun setupContextParameters(chat: Chat, msgLimit: Int, compactLimit: Int) {
        val agent = agentTools.getOrCreateAgent(
            systemName = AGENT_NAME,
            chatId = chat.id,
            isCommonAgent = false
        )
        
        agentTools.saveContextParameters(
            agentId = agent.id,
            msgLimit = msgLimit,
            extraLimit = compactLimit,
            strategy = "summarization"  // Пока только одна стратегия
        )
        
        val message = contextFormatter.formatSettingsSaved(msgLimit, compactLimit)
        chatTools.addInfoMessage(chat.id, message)
    }

    /**
     * Обработать обычное сообщение
     */
    private suspend fun processMessage(
        task: String,
        chat: Chat,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        // 1. Get or create agent instance
        val agent = agentTools.getOrCreateAgent(
            systemName = AGENT_NAME,
            chatId = chat.id,
            isCommonAgent = false
        )

        // 2. Получаем контекст
        val context = agentTools.getContext(agent.id)

        // 3. Подготавливаем историю сообщений для LLM (с учётом summary)
        val history = agentTools.getFullContext(context)

        // 4. Запрос к LLM с контекстом
        llmRequestUseCase.askLlm(
            chatSettings = chat.settings,
            userPrompt = task,
            systemPrompt = chat.settings.systemPromt,
            history = history,
            onEvent = onEvent
        ).onSuccess { result ->
            val resultText = result.getContent()

            // 5. Добавляем сообщения в контекст
            var updatedContext = context
                .addUserMessage(task)
                .addAssistantMessage(resultText)

            // 5.1. Сжимаем контекст при необходимости
            updatedContext = checkCompression(agent, chat, updatedContext)

            agentTools.saveContext(agent.id, updatedContext)

            // 8. Отправляем результат в чат
            chatTools.addBotMessage(chat.id, resultText)
        }.onFailure { exception ->
            chatTools.addBotMessage(chat.id, exception.stackTraceToString())
        }
    }

    private suspend fun checkCompression(
        agent: Agent,
        chat: Chat,
        context: AContext
    ): AContext {
        return if (agentTools.shouldCompress(agent.id)) {
            val startMessage = contextFormatter.formatCompressionStarted()
            chatTools.addInfoMessage(chat.id, startMessage)
            
            // 6. Проверяем и выполняем оптимизацию контекста (сжатие)
            val optimizedContext = agentTools.processContextOptimization(
                context = context,
                modelSettings = chat.settings.model
            )

            val resultMessage = contextFormatter.formatCompressionResult(
                oldMessageCount = context.messages.size,
                newMessageCount = optimizedContext.messages.size,
                oldSummaryLength = context.summarizationState.retrieveSummary()?.length ?: 0,
                newSummaryLength = optimizedContext.summarizationState.retrieveSummary()?.length ?: 0
            )
            chatTools.addInfoMessage(chat.id, resultMessage)
            optimizedContext
        } else {
            context
        }
    }
}
