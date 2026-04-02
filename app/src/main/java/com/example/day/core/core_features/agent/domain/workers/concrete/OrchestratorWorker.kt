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
        val dependsOn: List<Int>?,  // null = legacy (все предыдущие), [] = независимый, [1,2] = только эти
    )

    private sealed interface StepOutcome {
        data class Success(val result: SubTaskResult) : StepOutcome
        data class Failure(val index: Int, val message: String) : StepOutcome
    }

    private sealed interface VerifyOutcome {
        data object Ok : VerifyOutcome
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
        const val isDependsOnEnabled = true
        private const val MAX_RESULT_IN_CONTEXT = 3000

        private val SYSTEM_PROMPT = """
Ты эксперт по декомпозиции задач.
Твоя цель: разбить задачу на подзадачи, строго соблюдая заданный формат.
Сначала проанализируй задачу и доступные инструменты, составь план, затем оформи подзадачи.
Если что-то не понятно — уточни у пользователя или используй инструменты для поиска по проекту или git.

# ФОРМАТ ОТВЕТА

ОБЯЗАТЕЛЬНО — первая строка каждой подзадачи: depends_on: [номера шагов]
Исполнитель получит ТОЛЬКО результаты шагов из этого списка — никаких других данных.
Примеры: depends_on: [] — шаг независим; depends_on: [1] — нужен шаг 1; depends_on: [1, 3] — нужны шаги 1 и 3.

Правила структуры:
- Каждая подзадача обёрнута в [SUBTASKS_START] / [SUBTASKS_END]
- Подробное описание задачи пользователя с комментариями для исполнителей обернуто в [TASK_START] / [TASK_END]
- Весь ответ — между [TASK_START] и последним [SUBTASKS_END], без лишнего текста снаружи

# ИНСТРУМЕНТЫ

[FIND FILE]  Найти файл по имени → list_local_files с паттерном: "**/*MyClass*", "**/*Factory*.kt". Всегда используй `**/*` префикс — иначе файл в поддиректории не найдётся.
[SEARCH]     Найти по содержимому строк → search_local_files: возвращает только совпадающие строки, не весь файл. Для нескольких паттернов — несколько вызовов.
[READ]       Читать файл целиком → read_local_file: только для заведомо небольшого файла с уже известным путём
[COMBINE]    Если шаг читает файл только чтобы передать содержимое дальше — объедини чтение и обработку в один шаг
[NO LOOP]    Запрещено поручать одному шагу "для каждого из N файлов прочитай содержимое" — это взрывает контекст

# КАЧЕСТВО ПОДЗАДАЧ

- Каждая подзадача самодостаточна: [ЧТО ВЗЯТЬ из depends_on] + [ДЕЙСТВИЕ] + [ФОРМАТ РЕЗУЛЬТАТА]
- Указывай инструмент явно по имени, если он нужен для выполнения шага
- Если шаг записывает файл — явно укажи ОТКУДА берётся содержимое: "возьми данные из шага N и запиши в файл X". Плейсхолдеры и шаблонные заготовки без реальных данных — недопустимы.
- Если результат инструмента может быть пустым — добавь: "Если результат пуст — попробуй [альтернатива]"
- При сравнении двух источников (файл A vs B, ветка X vs Y) — явно укажи BASE (эталон) и CURRENT (изменения):
  "Присутствует в CURRENT, отсутствует в BASE: ..." и "Присутствует в BASE, отсутствует в CURRENT: ..."
- Если задача решается в один шаг — создай одну подзадачу
- Если инструменты не позволяют выполнить задачу — напиши обоснование вместо подзадач

# ЧЕК-ЛИСТ (проверь мысленно перед генерацией ответа)
□ У каждой подзадачи есть depends_on: [...] в первой строке?
□ depends_on ссылается только на реально существующие шаги (не на N+1)?
□ Ни один шаг не читает файл только чтобы передать его содержимое следующему шагу?
□ При сравнении двух источников явно указаны BASE и CURRENT?

# ПРИМЕР 1 — поиск по содержимому

[TASK_START]
Описание: найдём все файлы с аннотацией через search_local_files.
[TASK_END]
[SUBTASKS_START]
depends_on: []
Найди все строки с аннотацией @Composable в проекте с помощью search_local_files с паттерном "@Composable". Если результат пуст — попробуй паттерн "Composable". Ожидаемый результат: список строк вида "путь:номер_строки:содержимое".
[SUBTASKS_END]
[SUBTASKS_START]
depends_on: [1]
Из результатов шага 1 извлеки уникальные пути файлов (часть строки до первого ":"). Ожидаемый результат: список путей без дублей.
[SUBTASKS_END]

# ПРИМЕР 2 — сравнение файла с веткой (чтение + сравнение = один шаг)

[TASK_START]
Описание: найдём путь к файлу, затем в одном шаге прочитаем оба варианта и сравним.
[TASK_END]
[SUBTASKS_START]
depends_on: []
Найди путь к файлу UserRepository.kt с помощью list_local_files с паттерном "*UserRepository.kt". Ожидаемый результат: строка-путь, например "/app/src/.../UserRepository.kt".
[SUBTASKS_END]
[SUBTASKS_START]
depends_on: [1]
Используя путь из шага 1:
1. Прочитай локальный файл через read_local_file — это CURRENT.
2. Прочитай файл из ветки main через get_file_content(branch="main") — это BASE.
3. Сравни содержимое и выведи два списка:
   - "Присутствует в CURRENT, отсутствует в BASE: [перечисление]"
   - "Присутствует в BASE, отсутствует в CURRENT: [перечисление]"
Ожидаемый результат: два списка различий.
[SUBTASKS_END]
        """.trimIndent()

        private val SYNTHESIZE_SYSTEM_PROMPT = """
Ты финальный верификатор пайплайна.
Тебе переданы описание задачи и результаты всех подзадач.

Твоя цель — сформировать детальный итоговый ответ пользователю на русском языке:
- Если задача запрашивала данные или информацию — верни их в удобном виде согласно условиям задачи.
- Если задача предполагала выполнение действий — подтверди что все шаги выполнены и кратко резюмируй результат.
- Если какой-то шаг вернул пустой или некорректный результат — сообщи об этом явно.
        """.trimIndent()

        private val VERIFY_SYSTEM_PROMPT = """
Ты верификатор результата подзадачи в пайплайне.

Тебе передаётся полный контекст пайплайна (общая задача, список всех шагов, результаты предыдущих шагов, текущая подзадача) и фактический результат исполнителя текущего шага.
Твоя задача — оценить, выполнена ли текущая подзадача корректно. Предыдущие шаги не оценивай.

Отвечай ТОЛЬКО одной из двух форм — никакого другого текста:

[OK]
— результат достаточно хорош для продолжения пайплайна. Используй [OK] широко: если данные получены, даже если формат чуть отличается от ожидаемого или есть лишний текст.

[RETRY] <конкретная исправленная инструкция для исполнителя>
— используй ТОЛЬКО когда результат явно неверен и можно дать конкретную инструкцию для исправления. Примеры: инструмент вернул пустой список из-за неверного паттерна (укажи правильный), ответ в принципиально неверном формате (укажи точный формат). Не используй [RETRY] ради мелких улучшений — только когда следующий шаг точно не сможет использовать этот результат.

Если ожидаемый результат — словарь или список данных, а получен пустой словарь {} или пустой список [] — это скорее всего ошибка стратегии поиска, а не реальное отсутствие данных. Верни [RETRY] с предложением альтернативного подхода.
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
        orchestratorResults.subtasks.forEach { chatTools.addInfoMessage(chat.id, it.text) }

        if (orchestratorResults.subtasks.isEmpty()) {
            chatTools.addBotMessage(chat.id, "Не получилось создать подзаадачи")
            return
        }

        // Phase 2: пайплайн воркеров
        val results = mutableListOf<SubTaskResult>()

        for ((index, subtaskDef) in orchestratorResults.subtasks.withIndex()) {
            val step = buildStep(
                index = index,
                total = orchestratorResults.subtasks.size,
                subtaskDef = subtaskDef,
                orchestratorData = orchestratorResults,
                previousResults = results,
                cleanPrompt = cleanPrompt,
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
                            is VerifyOutcome.Retry -> {
                                chatTools.addInfoMessage(chat.id, "🔁 Повтор подзадачи ${step.index + 1}...")
                                val retryUserPrompt = buildString {
                                    appendLine(step.userPrompt)
                                    appendLine()
                                    appendLine("---")
                                    appendLine()
                                    appendLine("Верификатор отклонил предыдущий результат. Исправленная инструкция:")
                                    append(verify.revisedPrompt)
                                }
                                val retryStep = step.copy(userPrompt = retryUserPrompt)
                                when (val retryOutcome = executeStep(retryStep, chat, onEvent)) {
                                    is StepOutcome.Success -> retryOutcome.result
                                    is StepOutcome.Failure -> {
                                        chatTools.addInfoMessage(chat.id, "⚠️ Повтор подзадачи ${step.index + 1} не удался, берём исходный результат")
                                        outcome.result
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
        subtaskDef: SubtaskDef,
        orchestratorData: OrchestratorParsedResult,
        previousResults: List<SubTaskResult>,
        cleanPrompt: String,
    ): PipelineStep {
        val relevantResults = selectRelevantResults(previousResults, subtaskDef.dependsOn)
        return PipelineStep(
            index = index,
            total = total,
            subtask = subtaskDef.text,
            systemPrompt = buildWorkerSystemPrompt(orchestratorData, index, cleanPrompt),
            userPrompt = buildWorkerUserPrompt(orchestratorData.subtasks.map { it.text }, relevantResults, index),
            dependsOn = subtaskDef.dependsOn,
        )
    }

    private fun selectRelevantResults(
        previousResults: List<SubTaskResult>,
        dependsOn: List<Int>?,
    ): List<SubTaskResult> = when {
        !isDependsOnEnabled -> previousResults
        dependsOn == null -> previousResults           // depends_on не указан → все предыдущие (backward compat)
        dependsOn.isEmpty() -> emptyList()             // depends_on: [] → независимый шаг
        else -> dependsOn.mapNotNull { depIdx ->       // depends_on: [1, 3] → только нужные
            previousResults.find { it.index == depIdx }
        }
    }

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
            trimmed.startsWith("[RETRY]") -> VerifyOutcome.Retry(trimmed.removePrefix("[RETRY]").trim())
            else -> VerifyOutcome.Ok  // [OK], [FAIL] (игнорируем), или любой другой ответ → принимаем
        }
    }

    // endregion

    // region — prompt builders

    private fun buildWorkerSystemPrompt(parsed: OrchestratorParsedResult, currentIndex: Int, cleanPrompt: String): String = buildString {
        appendLine("Общая задача:")
        appendLine(cleanPrompt)
        appendLine()
        appendLine("Ты работаешь как часть пайплайна из ${parsed.subtasks.size} шагов:")
        parsed.subtasks.forEachIndexed { i, subtask ->
            appendLine("${i + 1}. ${subtask.text}")
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
                val resultText = if (r.result.length > MAX_RESULT_IN_CONTEXT)
                    r.result.take(MAX_RESULT_IN_CONTEXT) + "\n... [truncated, ${r.result.length} chars total]"
                else
                    r.result
                appendLine("Результат: $resultText")
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
