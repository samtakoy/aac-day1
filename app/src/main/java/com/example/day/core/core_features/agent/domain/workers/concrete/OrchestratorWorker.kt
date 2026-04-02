package com.example.day.core.core_features.agent.domain.workers.concrete

import android.util.Log
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.strategy.AContextDefaultFactory
import com.example.day.core.core_features.agent.domain.workers.base.AWorker
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import com.mikepenz.markdown.utils.LogCompositions
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

    // endregion

    companion object {
        private const val TAG = "[ORCH]"

        const val AGENT_NAME = "orchestrator_agent"
        const val TASK_PREFIX = "@@task"
        private const val WORKER_AGENT_NAME = "orchestrator_worker"

        private val SYSTEM_PROMPT = """
Ты эксперт по декомпозиции задач.
Твоя цель: разбить задачу на подзадачи, строго соблюдая заданный формат.

# ОСНОВНЫЕ ПРАВИЛА:
1. Проанализируй задачу и доступные инструменты.
2. Составь план реализации.
3. Текст каждой подзадачи будет передан LLM-ассистенту как отдельная самодостаточная инструкция.
4. Каждая подзадача должна содержать четкую инструкцию: что взять из предыдущего шага, какое действие совершить и в каком виде вернуть результат.
5. Инструкция должна исключать догадки: ассистент должен работать только с теми данными, которые получил от инструментов или из предыдущих шагов.
6. Если задача решается в один шаг — создай одну подзадачу.
7. Если инструменты не позволяют выполнить задачу — напиши обоснование вместо подзадач.

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
            Log.e(TAG,"task ${index + 1} systemPrompt: ${step.systemPrompt}")
            Log.e(TAG,"task ${index + 1} userPrompt: ${step.userPrompt}")
            when (val outcome = executeStep(step, chat, onEvent)) {
                is StepOutcome.Success -> {
                    results.add(outcome.result)
                    Log.e(TAG,"task ${index + 1} result: ${outcome.result}")
                }
                is StepOutcome.Failure -> {
                    chatTools.addBotMessage(chat.id, "❌ Ошибка подзадачи ${outcome.index + 1}: ${outcome.message}")
                    Log.e(TAG,"task ${index + 1} failure: ${outcome.message}")
                    return
                }
            }
        }

        chatTools.addInfoMessage(chat.id, "✅ Ok")
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

    // endregion
}
