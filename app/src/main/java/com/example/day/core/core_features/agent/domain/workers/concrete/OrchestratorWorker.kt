package com.example.day.core.core_features.agent.domain.workers.concrete

import android.util.Log
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.strategy.AContextDefaultFactory
import com.example.day.core.core_features.agent.domain.workers.base.AWorker
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import javax.inject.Inject

class OrchestratorWorker @Inject constructor(
    private val justWorkWorker: JustWorkWorker,
    private val chatTools: ChatTools,
) : AWorker {

    // region — inner types

    private data class SubTaskResult(
        val index: Int,
        val subtask: String,
        val result: String,
    )

    private data class PipelineStep(
        val index: Int,
        val total: Int,
        val subtask: String,
        val systemPrompt: String,
        val userPrompt: String,
    )

    private sealed interface StepOutcome {
        data class Success(val result: SubTaskResult) : StepOutcome
        data class Failure(val index: Int, val message: String) : StepOutcome
    }

    private sealed interface VerifyOutcome {
        data object Ok : VerifyOutcome
        data class Fail(val reason: String) : VerifyOutcome
        data class Retry(val revisedPrompt: String) : VerifyOutcome
    }

    // endregion

    companion object {
        private const val TAG = "[ORCH][Only]"

        const val AGENT_NAME = "orchestrator_agent"
        const val TASK_PREFIX = "@@task"
        private const val WORKER_AGENT_NAME = "orchestrator_worker"
        private const val VERIFIER_AGENT_NAME = "orchestrator_verifier"
        private const val SYNTHESIZER_AGENT_NAME = "orchestrator_synthesizer"

        const val isVerifyOn = true

        private val SYSTEM_PROMPT = """
Ты эксперт по декомпозиции задач.
Твоя цель: разбить задачу на подзадачи, строго соблюдая заданный формат.

# ОСНОВНЫЕ ПРАВИЛА:
1. Проанализируй задачу и доступные инструменты.
2. Если что-то не понятно, уточни у пользователя или используй инструменты для поиска по проекту или git.
3. Составь план реализации.
4. Текст каждой подзадачи будет передан LLM-ассистенту как отдельная самодостаточная инструкция.
5. Каждая подзадача должна содержать четкую инструкцию: что взять из предыдущего шага, какое действие совершить и в каком виде вернуть результат.
6. Инструкция должна исключать догадки: ассистент должен работать только с теми данными, которые получил от инструментов или из предыдущих шагов.
7. Если задача решается в один шаг — создай одну подзадачу.
8. Если инструменты не позволяют выполнить задачу — напиши обоснование вместо подзадач.
9. Если для выполнения подзадачи нужен инструмент — укажи его имя явно в тексте подзадачи.
10. Если шаг читает данные только чтобы передать их следующему шагу — объедини чтение и обработку в один шаг. Промежуточные сырые данные (содержимое файлов и т.п.) не должны быть результатом отдельного шага.

# ТРЕБОВАНИЯ К ФОРМАТУ:
- Запрещено добавлять любой текст (приветствия, пояснения) до тега [TASK_START] или после последнего тега [SUBTASKS_END].
- Каждая подзадача ДОЛЖНА быть обернута в отдельный блок [SUBTASKS_START] / [SUBTASKS_END].

# ПРИМЕР ЭТАЛОННОГО ОТВЕТА:
[TASK_START]
Описание решения: сначала найдем файлы, затем отберем нужные и прочитаем их.
[TASK_END]
[SUBTASKS_START]
Подзадача 1: Найди файлы... Ожидаемый результат: список путей.
[SUBTASKS_END]
[SUBTASKS_START]
Подзадача 2: Из списка в Подзадаче 1 выбери... Ожидаемый результат: список из 3 строк.
[SUBTASKS_END]
        """.trimIndent()

        private val SYNTHESIZE_SYSTEM_PROMPT = """
Ты финальный верификатор пайплайна.
Тебе переданы описание задачи и результаты всех подзадач.

Твоя цель — сформировать итоговый ответ пользователю:
- Если задача запрашивала данные или информацию — верни их в удобном виде согласно условиям задачи.
- Если задача предполагала выполнение действий — подтверди что все шаги выполнены и кратко резюмируй результат.
- Если какой-то шаг вернул пустой или некорректный результат — сообщи об этом явно.
        """.trimIndent()

        private val VERIFY_SYSTEM_PROMPT = """
Ты верификатор результата подзадачи в пайплайне.

Тебе передаётся полный контекст пайплайна (общая задача, список всех шагов, результаты предыдущих шагов, текущая подзадача) и фактический результат исполнителя текущего шага.
Твоя задача — оценить, выполнена ли текущая подзадача корректно. Предыдущие шаги не оценивай.

Если результат пустой из-за неверного паттерна/параметра инструмента, а не из-за логической ошибки — предпочитай [RETRY] с исправленным параметром.
Если данные корректны, но нарушен только формат (лишние поля, неверный порядок, неверное обёртывание) — предпочитай [RETRY] с инструкцией исправить формат, а не [FAIL].

Отвечай ТОЛЬКО одной из трёх форм — никакого другого текста:

[OK]
— результат соответствует ожиданиям подзадачи

[FAIL] <причина>
— результат некорректен и исправить его невозможно (объективный факт: ошибка инструмента, данных нет и т.п.)

[RETRY] <исправленная инструкция для исполнителя>
— результат некорректен, но задачу можно переформулировать и решить после корректировки инструкции. Напиши конкретную исправленную инструкцию.
        """.trimIndent()
    }

    // region — doWork

    override suspend fun doWork(
        userPrompt: String,
        chat: Chat,
        userRole: AContextMessage.Role,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        val cleanPrompt = userPrompt.trimStart().substringAfter(TASK_PREFIX).trimStart()

        // Phase 1: декомпозиция
        val rawText = decompose(cleanPrompt, chat, userRole, onEvent) ?: return

        val orchestratorResults = OrchestratorResultParser.parse(rawText)
        if (orchestratorResults == null) {
            chatTools.addBotMessage(chat.id, rawText)
            return
        }

        orchestratorResults.taskDescription?.let { chatTools.addInfoMessage(chat.id, it) }
        orchestratorResults.subtasks.forEach { chatTools.addInfoMessage(chat.id, it) }

        if (orchestratorResults.subtasks.isEmpty()) {
            chatTools.addBotMessage(chat.id, "Не получилось создать подзаадачи")
            return
        }

        // Phase 2: пайплайн воркеров
        val results = mutableListOf<SubTaskResult>()

        for ((index, subtask) in orchestratorResults.subtasks.withIndex()) {
            val step = buildStep(
                index = index,
                total = orchestratorResults.subtasks.size,
                subtask = subtask,
                orchestratorData = orchestratorResults,
                previousResults = results
            )
            Log.e(TAG, "task ${index + 1} systemPrompt: ${step.systemPrompt}")
            Log.e(TAG, "task ${index + 1} userPrompt: ${step.userPrompt}")

            when (val outcome = executeStep(step, chat, onEvent)) {
                is StepOutcome.Failure -> {
                    chatTools.addBotMessage(chat.id, "❌ Ошибка подзадачи ${outcome.index + 1}: ${outcome.message}")
                    Log.e(TAG, "task ${index + 1} failure: ${outcome.message}")
                    return
                }
                is StepOutcome.Success -> {
                    Log.e(TAG, "task ${index + 1} result: ${outcome.result}")
                    val finalResult = if (isVerifyOn) {
                        when (val verify = verifyStep(step, outcome.result, chat, onEvent)) {
                            is VerifyOutcome.Ok -> outcome.result
                            is VerifyOutcome.Fail -> {
                                chatTools.addBotMessage(chat.id, "❌ Верификация подзадачи ${step.index + 1}: ${verify.reason}")
                                return
                            }
                            is VerifyOutcome.Retry -> {
                                chatTools.addInfoMessage(chat.id, "🔁 Повтор подзадачи ${step.index + 1}...")
                                val retryStep = step.copy(userPrompt = verify.revisedPrompt)
                                when (val retryOutcome = executeStep(retryStep, chat, onEvent)) {
                                    is StepOutcome.Success -> retryOutcome.result
                                    is StepOutcome.Failure -> {
                                        chatTools.addInfoMessage(chat.id, "❌ Повтор подзадачи ${step.index + 1}: ${retryOutcome.message}")
                                        return
                                    }
                                }
                            }
                        }
                    } else {
                        outcome.result
                    }
                    results.add(finalResult)
                }
            }
        }

        // Phase 3: синтез
        synthesize(cleanPrompt, orchestratorResults, results, chat, onEvent)
    }

    // endregion

    // region — Phase 1

    private suspend fun decompose(
        cleanPrompt: String,
        chat: Chat,
        userRole: AContextMessage.Role,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ): String? {
        val config = JustWorkConfig(
            agentName = AGENT_NAME,
            chatId = chat.id,
            systemPrompt = SYSTEM_PROMPT,
            allowedTools = emptyList(),
            defaultModel = { chat.settings.model },
            defaultContext = { AContextDefaultFactory.createFull() },
            recreateAgent = true,
            memoryTypes = emptyList(),
        )
        return justWorkWorker.doWork(config, "Декомпозируй такую задачу:\n$cleanPrompt", userRole, onEvent)
            .onFailure { chatTools.addInfoMessage(chat.id, "❌ OrchestratorWorker: ${it.message}") }
            .getOrNull()
    }

    // endregion

    // region — Phase 2

    private fun buildStep(
        index: Int,
        total: Int,
        subtask: String,
        orchestratorData: OrchestratorParsedResult,
        previousResults: List<SubTaskResult>,
    ): PipelineStep = PipelineStep(
        index = index,
        total = total,
        subtask = subtask,
        systemPrompt = buildWorkerSystemPrompt(orchestratorData, index),
        userPrompt = buildWorkerUserPrompt(orchestratorData.subtasks, previousResults, index),
    )

    private suspend fun executeStep(
        step: PipelineStep,
        chat: Chat,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ): StepOutcome {
        chatTools.addInfoMessage(chat.id, "⚙️ Подзадача ${step.index + 1}/${step.total}...")

        val config = JustWorkConfig(
            agentName = "${WORKER_AGENT_NAME}_${chat.id}",
            chatId = chat.id,
            systemPrompt = step.systemPrompt,
            allowedTools = emptyList(),
            defaultModel = { chat.settings.model },
            defaultContext = { AContextDefaultFactory.createFull() },
            recreateAgent = true,
            memoryTypes = emptyList(),
        )

        return justWorkWorker.doWork(config, step.userPrompt, onEvent = onEvent)
            .fold(
                onSuccess = { text ->
                    chatTools.addInfoMessage(chat.id, "✅ Подзадача ${step.index + 1}:\n$text")
                    StepOutcome.Success(SubTaskResult(step.index + 1, step.subtask, text))
                },
                onFailure = { error ->
                    StepOutcome.Failure(step.index + 1, error.message ?: "unknown error")
                }
            )
    }

    // endregion

    // region — Phase 3: Synthesizer

    private suspend fun synthesize(
        cleanPrompt: String,
        orchestratorData: OrchestratorParsedResult,
        results: List<SubTaskResult>,
        chat: Chat,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        val config = JustWorkConfig(
            agentName = "${SYNTHESIZER_AGENT_NAME}_${chat.id}",
            chatId = chat.id,
            systemPrompt = SYNTHESIZE_SYSTEM_PROMPT,
            allowedTools = emptyList(),
            defaultModel = { chat.settings.model },
            defaultContext = { AContextDefaultFactory.createFull() },
            recreateAgent = true,
            memoryTypes = emptyList(),
        )

        justWorkWorker.doWork(config, buildSynthesizerUserPrompt(cleanPrompt, orchestratorData, results), onEvent = onEvent)
            .fold(
                onSuccess = { text -> chatTools.addBotMessage(chat.id, text) },
                onFailure = { chatTools.addBotMessage(chat.id, "✅ Что-то пошло не так при формировании отчета: ${it.stackTraceToString()}") }
            )
    }

    // endregion

    // region — Phase 2: Verifier

    private suspend fun verifyStep(
        step: PipelineStep,
        result: SubTaskResult,
        chat: Chat,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ): VerifyOutcome {
        val config = JustWorkConfig(
            agentName = "${VERIFIER_AGENT_NAME}_${chat.id}",
            chatId = chat.id,
            systemPrompt = buildVerifierSystemPrompt(step),
            allowedTools = emptyList(),
            defaultModel = { chat.settings.model },
            defaultContext = { AContextDefaultFactory.createFull() },
            recreateAgent = true,
            memoryTypes = emptyList(),
        )

        val text = justWorkWorker.doWork(config, buildVerifierUserPrompt(step, result), onEvent = onEvent)
            .getOrElse { return VerifyOutcome.Ok }

        Log.e(TAG, "verify ${step.index + 1} outcome: $text")
        return parseVerifyOutcome(text)
    }

    private fun parseVerifyOutcome(text: String): VerifyOutcome {
        val trimmed = text.trim()
        return when {
            trimmed.startsWith("[OK]")    -> VerifyOutcome.Ok
            trimmed.startsWith("[FAIL]")  -> VerifyOutcome.Fail(trimmed.removePrefix("[FAIL]").trim())
            trimmed.startsWith("[RETRY]") -> VerifyOutcome.Retry(trimmed.removePrefix("[RETRY]").trim())
            else -> VerifyOutcome.Ok
        }
    }

    // endregion

    // region — prompt builders

    private fun buildWorkerSystemPrompt(parsed: OrchestratorParsedResult, currentIndex: Int): String = buildString {
        appendLine("Общая задача:")
        parsed.taskDescription?.let { taskDescription ->
            appendLine(taskDescription)
            appendLine()
        }
        appendLine("Ты работаешь как часть пайплайна из ${parsed.subtasks.size} шагов:")
        parsed.subtasks.forEachIndexed { i, subtask ->
            appendLine("${i + 1}. $subtask")
        }
        appendLine()
        append("Выполняй только свою подзадачу (${currentIndex + 1}). Результаты предыдущих шагов переданы в запросе.")
        // TODO test
        append("Если инструмент вернул пустой результат — попробуй более широкий паттерн альтернативный путь перед тем как вернуть пустой ответ (но не вызывай слишком много инструментов).")
    }

    private fun buildWorkerUserPrompt(
        subtasks: List<String>,
        previousResults: List<SubTaskResult>,
        currentIndex: Int,
    ): String = buildString {
        if (previousResults.isNotEmpty()) {
            appendLine("Результаты выполненных подзадач:")
            previousResults.forEach { r ->
                appendLine()
                appendLine(r.subtask)
                appendLine("Результат: ${r.result}")
            }
            appendLine()
            appendLine("---")
            appendLine()
        }
        appendLine("Твоя текущая задача (подзадача ${currentIndex + 1}):")
        append(subtasks[currentIndex])
    }

    private fun buildSynthesizerUserPrompt(
        cleanPrompt: String,
        orchestratorData: OrchestratorParsedResult,
        results: List<SubTaskResult>,
    ): String = buildString {
        appendLine("Задача:")
        appendLine(orchestratorData.taskDescription ?: cleanPrompt)
        appendLine()
        appendLine("Результаты подзадач:")
        results.forEach { r ->
            appendLine()
            appendLine("Подзадача ${r.index}: ${r.subtask}")
            appendLine("Результат: ${r.result}")
        }
    }

    private fun buildVerifierSystemPrompt(step: PipelineStep): String = buildString {
        appendLine(step.systemPrompt)
        appendLine()
        appendLine("---")
        appendLine()
        append(VERIFY_SYSTEM_PROMPT)
    }

    private fun buildVerifierUserPrompt(step: PipelineStep, result: SubTaskResult): String = buildString {
        appendLine(step.userPrompt)
        appendLine()
        appendLine("---")
        appendLine()
        appendLine("Фактический результат исполнителя:")
        append(result.result)
    }

    // endregion
}
