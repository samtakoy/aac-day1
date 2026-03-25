package com.example.day.ragserver.comparison

import com.example.day.ragserver.indexing.OllamaLlmProvider
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Сервис сравнительного анализа двух runtest-отчётов (LOCAL vs CLOUD).
 *
 * Алгоритм [tryRunComparison]:
 * 1. Найти последний LOCAL и последний CLOUD отчёт в [reportsDir]
 * 2. Если оба есть — распарсить, загрузить эталонные ответы
 * 3. Для каждого вопроса отправить тройку в Ollama (ответ LOCAL / ответ CLOUD / эталон)
 * 4. Сохранить итоговый отчёт comparison_{timestamp}.md
 */
class ComparisonService(
    private val ollamaProvider: OllamaLlmProvider,
    private val reportsDir: String,
    private val answersPath: String,
) {

    private data class EvalResult(
        val question: String,
        val scoreLocal: Int?,
        val scoreCloud: Int?,
        val rawEvaluation: String,
    )

    suspend fun tryRunComparison() {
        println("[ComparisonService] (${ollamaProvider.modelName}) Запуск сравнительного анализа...")

        val localFile = findLastReport("LOCAL")
        val cloudFile = findLastReport("CLOUD")

        if (localFile == null || cloudFile == null) {
            println("[ComparisonService] Нет пары LOCAL/CLOUD для сравнения (LOCAL=$localFile, CLOUD=$cloudFile)")
            return
        }

        println("[ComparisonService] LOCAL: ${localFile.name}, CLOUD: ${cloudFile.name}")

        val localItems = ReportParser.parse(localFile.absolutePath)
        val cloudItems = ReportParser.parse(cloudFile.absolutePath)
        val refs = ReferenceAnswerParser.parse(answersPath)

        if (refs.isEmpty()) {
            println("[ComparisonService] Эталонные ответы не загружены, сравнение пропущено")
            return
        }

        val count = minOf(localItems.size, cloudItems.size, refs.size)
        if (count == 0) {
            println("[ComparisonService] Нет данных для сравнения")
            return
        }

        println("[ComparisonService] (${ollamaProvider.modelName}) Сравниваю $count вопросов через Ollama...")

        val evalResults = mutableListOf<EvalResult>()
        for (i in 0 until count) {
            val question = localItems[i].first
            val answerLocal = localItems[i].second
            val answerCloud = cloudItems[i].second
            val refAnswer = refs[i + 1] ?: continue

            val prompt = buildComparisonPrompt(question, answerLocal, answerCloud, refAnswer)
            // т.к. временный тест, используется для сравнения моделька из переменной окружения RERANKER_LLM_MODEL
            val evaluation = runCatching { ollamaProvider.generate(prompt) }.getOrElse { e ->
                println("[ComparisonService] Ошибка Ollama на вопросе ${i + 1}: ${e.message}")
                "Ошибка оценки: ${e.message}"
            }

            val scoreLocal = extractScore(evaluation, "Оценка 1")
            val scoreCloud = extractScore(evaluation, "Оценка 2")
            evalResults.add(EvalResult(question, scoreLocal, scoreCloud, evaluation))
            println("[ComparisonService] Q${i + 1}: LOCAL=$scoreLocal, CLOUD=$scoreCloud")
        }

        saveReport(evalResults, localFile.name, cloudFile.name)
    }

    private fun findLastReport(modelLabel: String): File? =
        File(reportsDir)
            .listFiles { f -> f.name.startsWith("runtest_RAG_WORKER_${modelLabel}_") && f.name.endsWith(".md") }
            ?.sortedBy { it.name }
            ?.lastOrNull()

    private fun extractScore(text: String, label: String): Int? =
        Regex("""$label:\s*(\d+)/10""").find(text)?.groupValues?.get(1)?.toIntOrNull()

    private fun saveReport(results: List<EvalResult>, localFileName: String, cloudFileName: String) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"))
        val reportPath = "$reportsDir/comparison_$timestamp.md"
        File(reportsDir).mkdirs()
        File(reportPath).writeText(buildReportText(results, localFileName, cloudFileName, timestamp))
        println("[ComparisonService] Отчёт сохранён: $reportPath")
    }

    private fun buildReportText(
        results: List<EvalResult>,
        localFileName: String,
        cloudFileName: String,
        timestamp: String,
    ): String = buildString {
        appendLine("# Сравнительный отчёт: LOCAL vs CLOUD")
        appendLine("**Дата:** $timestamp")
        appendLine("**LOCAL-отчёт:** $localFileName")
        appendLine("**CLOUD-отчёт:** $cloudFileName")
        appendLine()
        appendLine("---")
        appendLine()

        results.forEachIndexed { i, r ->
            appendLine("## Q${i + 1}: \"${r.question}\"")
            appendLine("**Оценка LOCAL:** ${r.scoreLocal?.let { "$it/10" } ?: "—"}")
            appendLine("**Оценка CLOUD:** ${r.scoreCloud?.let { "$it/10" } ?: "—"}")
            appendLine()
            appendLine(r.rawEvaluation.lines()
                .firstOrNull { it.startsWith("Комментарий:") }
                ?: r.rawEvaluation.take(200))
            appendLine()
            appendLine("---")
            appendLine()
        }

        val validLocal = results.mapNotNull { it.scoreLocal }
        val validCloud = results.mapNotNull { it.scoreCloud }
        val avgLocal = if (validLocal.isNotEmpty()) validLocal.average() else null
        val avgCloud = if (validCloud.isNotEmpty()) validCloud.average() else null

        appendLine("## Итого")
        appendLine()
        appendLine("| | LOCAL | CLOUD |")
        appendLine("|--|--|--|")
        appendLine("| Средний балл | ${avgLocal?.let { "%.1f".format(it) } ?: "—"} | ${avgCloud?.let { "%.1f".format(it) } ?: "—"} |")
        appendLine("| Оценено вопросов | ${validLocal.size}/${results.size} | ${validCloud.size}/${results.size} |")
    }
}

private fun buildComparisonPrompt(
    question: String,
    answerLocal: String,
    answerCloud: String,
    refAnswer: String,
): String = """
Ты — независимый эксперт, оценивающий качество ответов на технические вопросы.

**Вопрос:** $question

**Ответ 1 (LOCAL):**
$answerLocal

**Ответ 2 (CLOUD):**
$answerCloud

**Эталонный ответ:**
$refAnswer

Оцени каждый ответ по шкале от 0 до 10, сравнивая с эталонным.
Критерии: точность, полнота, конкретность, наличие примеров кода, не забывает указывать источники. Есть ли выдуманные классы и методы?

Ответь строго в формате:
Оценка 1: X/10
Оценка 2: X/10
Комментарий: (краткое сравнение, 1-2 предложения)
""".trimIndent()
