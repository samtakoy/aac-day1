package com.example.day.core.core_features.agent.domain.workers.innercommand.handler

import com.example.day.core.core_features.agent.domain.AgentRepository
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.repository.AgentMemoryRepository
import com.example.day.core.core_features.agent.domain.strategy.AContextDefaultFactory
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.memory.domain.provider.AutoRagMemoryProvider
import com.example.day.core.core_features.memory.domain.provider.base.MemoryType
import com.example.day.core.core_features.memory.domain.provider.rag.CustomPipelineConfig
import com.example.day.core.core_features.memory.domain.provider.rag.RagSearchRepository
import com.example.day.core.core_features.memory.domain.provider.rag.RuntestResultItem
import com.example.day.core.core_features.memory.domain.provider.rag.TestQueries
import javax.inject.Inject

/**
 * Handles @@talk(rag ...) commands for configuring RAG-based prompt enrichment.
 *
 * Supported sub-commands:
 *   --on                   enable AutoRag for this agent (adds MemoryType.AutoRag)
 *   --off                  disable AutoRag (removes MemoryType.AutoRag)
 *   --state                show current status and configured server URL
 *   --url <url>            set rag-server URL (stored in AgentMemoryRepository)
 *   --gentest [presets]    run evaluation via /evaluate endpoint, save MD reports on server.
 *                          presets: comma-separated preset names or empty for all.
 *                          Examples: --gentest | --gentest baseline | --gentest filtered,reranked_llm
 *   --runtest [preset]     run evaluation + LLM for each question, save MD report on server.
 *                          preset: single preset name, default = baseline.
 *                          Examples: --runtest | --runtest filtered | --runtest reranked_llm
 *   --runparamtest [params] run evaluation with ad-hoc pipeline config (BASELINE as defaults),
 *                          call LLM for each question, save MD report on server.
 *                          params: space-separated key=value pairs (all optional).
 *                          Keys: topk, threshold, rerank (none|heuristic|llm), final,
 *                                qoptimize (true|false|1|0), retrieval (two_stage|hybrid),
 *                                chunking (structural|fixed)
 *                          Examples: --runparamtest topk=20 threshold=0.5 rerank=heuristic
 *                                    --runparamtest retrieval=hybrid chunking=fixed qoptimize=false
 */
class RagCommandHandler @Inject constructor(
    private val agentRepository: AgentRepository,
    private val agentMemoryRepository: AgentMemoryRepository,
    private val ragSearchRepository: RagSearchRepository,
    private val llmRequestUseCase: LlmRequestUseCase,
) : CommandHandler {

    override val commandName = "rag"

    override suspend fun handle(params: List<Pair<String, String?>>, chat: Chat): CommandResult {
        val paramsMap = params.toMap()
        val agentConfig = agentRepository.getOrCreateAgent(
            systemName = AGENT_NAME,
            chatId = chat.id,
            systemPrompt = chat.settings.systemPromt,
            defaultModel = { chat.settings.model },
            defaultContext = { AContextDefaultFactory.createFull() }
        )
        return when {
            "on"      in paramsMap -> handleOn(agentConfig.id)
            "off"     in paramsMap -> handleOff(agentConfig.id)
            "state"   in paramsMap -> handleState(agentConfig.id)
            "url"     in paramsMap -> handleUrl(agentConfig.id, paramsMap["url"])
            "gentest"      in paramsMap -> handleGentest(agentConfig.id, paramsMap["gentest"])
            "runtest"      in paramsMap -> handleRuntest(agentConfig.id, paramsMap["runtest"], chat)
            "runparamtest" in paramsMap -> handleRunparamtest(agentConfig.id, paramsMap["runparamtest"], chat)
            else -> CommandResult.Error(
                "Неизвестная команда rag.\nДоступные: --on | --off | --state | --url <url> | --gentest [presets] | --runtest [preset] | --runparamtest [params]"
            )
        }
    }

    private suspend fun handleOn(agentId: Long): CommandResult {
        agentRepository.addMemoryType(agentId, MemoryType.AutoRag)
        val url = currentUrl(agentId)
        return CommandResult.Success("RAG включён. Сервер: $url")
    }

    private suspend fun handleOff(agentId: Long): CommandResult {
        agentRepository.removeMemoryType(agentId, MemoryType.AutoRag)
        return CommandResult.Success("RAG выключен")
    }

    private suspend fun handleState(agentId: Long): CommandResult {
        val types = agentRepository.getMemoryTypes(agentId)
        val isOn = MemoryType.AutoRag in types
        val url = currentUrl(agentId)
        return CommandResult.Success(
            "RAG: ${if (isOn) "включён ✓" else "выключен"}\nURL: $url"
        )
    }

    private suspend fun handleUrl(agentId: Long, url: String?): CommandResult {
        if (url.isNullOrBlank()) return CommandResult.Error("Укажите URL сервера")
        agentMemoryRepository.upsertFact(
            agentId,
            AutoRagMemoryProvider.MEMORY_KEY,
            AutoRagMemoryProvider.CATEGORY_URL,
            url
        )
        return CommandResult.Success("URL сохранён: $url")
    }

    /**
     * Запускает evaluation: отправляет TestQueries на /evaluate, сервер сохраняет MD-отчёты.
     *
     * @param presetsArg null или "" → все пресеты; "baseline" → один; "filtered,reranked_llm" → несколько
     */
    private suspend fun handleGentest(agentId: Long, presetsArg: String?): CommandResult {
        val serverUrl = currentUrl(agentId)
        val presets = when {
            presetsArg.isNullOrBlank() -> listOf("all")
            else -> presetsArg.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }

        val response = ragSearchRepository.evaluate(
            questions = TestQueries.list,
            presets = presets,
            serverUrl = serverUrl,
        ).getOrElse { return CommandResult.Error("Ошибка evaluation: ${it.message}") }

        return CommandResult.Success(
            buildString {
                appendLine(response.summary)
                appendLine()
                appendLine("Отчёты сохранены на сервере:")
                response.savedReports.forEach { appendLine("  $it") }
            }.trim()
        )
    }

    /**
     * Прогоняет TestQueries через RAG pipeline (один пресет), затем для каждого вопроса
     * отправляет вопрос + RAG-контекст в LLM. Показывает компактный Q&A в чате.
     * MD-отчёты сохраняются на сервере как side effect /evaluate.
     *
     * @param presetArg null или "" → baseline; "filtered" → конкретный пресет
     */
    private suspend fun handleRuntest(agentId: Long, presetArg: String?, chat: Chat): CommandResult {
        val serverUrl = currentUrl(agentId)
        val presetName = presetArg?.trim()?.takeIf { it.isNotBlank() } ?: "baseline"

        val evalResult = ragSearchRepository.evaluate(
            questions = TestQueries.list,
            presets = listOf(presetName),
            serverUrl = serverUrl,
        ).getOrElse { return CommandResult.Error("Ошибка evaluate: ${it.message}") }

        val items = evalResult.items[presetName.uppercase()]
            ?: return CommandResult.Error("Нет данных для пресета '$presetName'")

        val answers = mutableListOf<Pair<String, String>>()
        items.forEachIndexed { i, item ->
            val prompt = "${item.question}\n\nКонтекст по вопросу:\n${item.ragContext}"
            val result = llmRequestUseCase.exec(
                modelSettings = chat.settings.model,
                systemPrompt = """
Ты — аналитический ассистент. Твоя задача — отвечать на вопросы, используя предоставленный контекст.
Правила:
Если в контексте нет прямого ответа, напиши "Недостаточно данных для ответа".
Для каждого утверждения в ответе укажи источник.
Если источник - файл, то укажи имя, полный путь, и номер строки в файле если есть.
Отвечай на русском языке.
                """.trimMargin(),
                // systemPrompt = "",
                messages = emptyList(),
                prompt = AContextMessage(AContextMessage.Role.USER, prompt),
            ).getOrElse {
                return CommandResult.Error("Ошибка LLM на вопросе ${i + 1}: ${it.message}")
            }
            val answer = result.choices.firstOrNull()?.message?.content ?: "(нет ответа)"
            answers.add(item.question to answer)
        }

        val saveResult = ragSearchRepository.saveRuntestResults(
            preset = presetName,
            items = answers.map { (question, answer) -> RuntestResultItem(question, answer) },
            serverUrl = serverUrl,
        ).getOrElse {
            return CommandResult.Success(
                "Runtest: ${presetName.uppercase()} | ${answers.size} вопросов\n⚠️ Не удалось сохранить отчёт: ${it.message}"
            )
        }

        return CommandResult.Success(
            "Runtest: ${presetName.uppercase()} | ${answers.size} вопросов\nОтчёт сохранён: ${saveResult.savedReport}"
        )
    }

    private suspend fun handleRunparamtest(agentId: Long, paramsArg: String?, chat: Chat): CommandResult {
        val serverUrl = currentUrl(agentId)
        val kvMap = parseKeyValues(paramsArg)

        val customConfig = CustomPipelineConfig(
            retrievalTopK = kvMap["topk"]?.toIntOrNull(),
            threshold = kvMap["threshold"]?.toDoubleOrNull(),
            rerankStrategy = kvMap["rerank"],
            finalTopK = kvMap["final"]?.toIntOrNull(),
            enableQueryOptimize = kvMap["qoptimize"]?.let { it == "true" || it == "1" },
            retrievalStrategy = kvMap["retrieval"],
            chunkingStrategy = kvMap["chunking"],
        )

        val evalResult = ragSearchRepository.evaluateCustom(
            customConfig = customConfig,
            questions = TestQueries.list,
            serverUrl = serverUrl,
        ).getOrElse { return CommandResult.Error("Ошибка evaluate: ${it.message}") }

        val items = evalResult.items["CUSTOM"]
            ?: return CommandResult.Error("Нет данных для кастомного пресета")

        val answers = mutableListOf<Pair<String, String>>()
        items.forEachIndexed { i, item ->
            val prompt = "${item.question}\n\nКонтекст по вопросу:\n${item.ragContext}"
            val result = llmRequestUseCase.exec(
                modelSettings = chat.settings.model,
                systemPrompt = "",
                messages = emptyList(),
                prompt = AContextMessage(AContextMessage.Role.USER, prompt),
            ).getOrElse {
                return CommandResult.Error("Ошибка LLM на вопросе ${i + 1}: ${it.message}")
            }
            val answer = result.choices.firstOrNull()?.message?.content ?: "(нет ответа)"
            answers.add(item.question to answer)
        }

        val presetLabel = buildParamLabel(kvMap)
        val saveResult = ragSearchRepository.saveRuntestResults(
            preset = presetLabel,
            items = answers.map { (question, answer) -> RuntestResultItem(question, answer) },
            serverUrl = serverUrl,
        ).getOrElse {
            return CommandResult.Success(
                "Runparamtest: $presetLabel | ${answers.size} вопросов\n⚠️ Не удалось сохранить отчёт: ${it.message}"
            )
        }

        return CommandResult.Success(
            "Runparamtest: $presetLabel | ${answers.size} вопросов\nОтчёт сохранён: ${saveResult.savedReport}"
        )
    }

    private fun parseKeyValues(input: String?): Map<String, String> {
        if (input.isNullOrBlank()) return emptyMap()
        return input.trim()
            .split("\\s+".toRegex())
            .mapNotNull { pair ->
                val idx = pair.indexOf('=')
                if (idx > 0) pair.substring(0, idx).lowercase() to pair.substring(idx + 1).lowercase()
                else null
            }
            .toMap()
    }

    private fun buildParamLabel(kvMap: Map<String, String>): String {
        val parts = listOfNotNull(
            kvMap["topk"]?.let { "topk$it" },
            kvMap["threshold"]?.let { "thr$it" },
            kvMap["rerank"]?.let { "rerank-$it" },
            kvMap["final"]?.let { "final$it" },
            kvMap["qoptimize"]?.let { "qopt$it" },
            kvMap["retrieval"]?.let { "ret-$it" },
            kvMap["chunking"]?.let { "chunk-$it" },
        )
        return if (parts.isEmpty()) "CUSTOM" else "CUSTOM_${parts.joinToString("_")}"
    }

    private suspend fun currentUrl(agentId: Long): String =
        agentMemoryRepository
            .getFact(agentId, AutoRagMemoryProvider.MEMORY_KEY, AutoRagMemoryProvider.CATEGORY_URL)
            ?.fact ?: AutoRagMemoryProvider.DEFAULT_URL

    companion object {
        private const val AGENT_NAME = "talk_agent"
    }
}
