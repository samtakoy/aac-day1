package com.example.day.ragserver.evaluation

import com.example.day.ragserver.pipeline.PipelineConfig
import com.example.day.ragserver.pipeline.PipelineExecutor
import com.example.day.ragserver.pipeline.PipelineMetrics
import com.example.day.ragserver.pipeline.PipelinePreset
import com.example.day.ragserver.search.context.ContextFormatter
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class EvalItem(
    val question: String,
    val optimizedQuery: String?,
    val ragContext: String,
)

data class EvaluationSummary(
    val savedReports: List<String>,
    val summary: String,
    val items: Map<String, List<EvalItem>>,  // preset name → items (для будущего --runtest)
)

class EvaluationService(
    private val reportsDir: String = "./reports",
    private val buildPipeline: (PipelineConfig) -> PipelineExecutor,
) {

    suspend fun evaluate(
        questions: List<String>,
        presets: List<PipelinePreset>,
    ): EvaluationSummary {
        File(reportsDir).mkdirs()

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"))
        val savedReports = mutableListOf<String>()
        val allItems = mutableMapOf<String, List<EvalItem>>()
        val presetSummaries = mutableListOf<String>()

        for (preset in presets) {
            val (reportPath, items, avgTopScore) = runNamedConfig(preset.name, preset.config, questions, timestamp)
            savedReports.add(reportPath)
            allItems[preset.name] = items
            presetSummaries.add("${preset.name} avg: ${"%.2f".format(avgTopScore)}")
        }

        val summary = "${presets.size} пресета(ов) × ${questions.size} вопросов\n" +
            presetSummaries.joinToString(" | ")

        return EvaluationSummary(
            savedReports = savedReports,
            summary = summary,
            items = allItems,
        )
    }

    suspend fun evaluateCustom(
        name: String,
        config: PipelineConfig,
        questions: List<String>,
    ): EvaluationSummary {
        File(reportsDir).mkdirs()
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"))
        val (reportPath, items, avgTopScore) = runNamedConfig(name, config, questions, timestamp)
        return EvaluationSummary(
            savedReports = listOf(reportPath),
            summary = "CUSTOM | ${questions.size} вопросов | avg: ${"%.2f".format(avgTopScore)}",
            items = mapOf(name to items),
        )
    }

    private suspend fun runNamedConfig(
        name: String,
        config: PipelineConfig,
        questions: List<String>,
        timestamp: String,
    ): Triple<String, List<EvalItem>, Double> {
        val pipeline = buildPipeline(config)
        val items = mutableListOf<EvalItem>()
        val report = StringBuilder()

        report.appendLine("# Evaluation: $name | $timestamp")
        report.appendLine("Config: retrievalTopK=${config.retrievalTopK}, " +
            "threshold=${config.threshold}, " +
            "rerank=${config.rerankStrategy}, " +
            "finalTopK=${config.finalTopK}")
        report.appendLine()

        var totalTopScore = 0.0

        questions.forEachIndexed { index, question ->
            val ctx = pipeline.execute(question)
            val ragContext = ctx.packed?.let { ContextFormatter.format(it) } ?: "Ничего не найдено"
            val topScore = ctx.results.maxOfOrNull { it.score.toDouble() } ?: 0.0
            val avgScore = if (ctx.results.isNotEmpty())
                ctx.results.map { it.score.toDouble() }.average() else 0.0

            totalTopScore += topScore

            items.add(EvalItem(
                question = question,
                optimizedQuery = if (ctx.query != ctx.originalQuery) ctx.query else null,
                ragContext = ragContext,
            ))

            report.appendLine("---")
            report.appendLine("## Query ${index + 1}: \"$question\"")
            if (ctx.query != ctx.originalQuery) {
                report.appendLine("**Optimized:** \"${ctx.query}\"")
            }
            report.appendLine(formatMetrics(ctx.metrics, config, ctx.results.size))
            report.appendLine("**Top score:** ${"%.2f".format(topScore)} | Avg score: ${"%.2f".format(avgScore)}")
            report.appendLine()
            report.appendLine("### RAG Context:")
            report.appendLine(ragContext)
            report.appendLine()
        }

        val avgTopScore = if (questions.isNotEmpty()) totalTopScore / questions.size else 0.0
        appendSummaryTable(report, questions.size, avgTopScore, config)

        val reportPath = "$reportsDir/eval_${name}_$timestamp.md"
        File(reportPath).writeText(report.toString())
        println("[Evaluation] Saved: $reportPath")

        return Triple(reportPath, items, avgTopScore)
    }

    private fun formatMetrics(
        metrics: PipelineMetrics,
        config: PipelineConfig,
        finalCount: Int,
    ): String = buildString {
        append("**Metrics:** Retrieved: ${metrics.countAfterRetrieval}")
        if (config.threshold > 0.0) append(" → Filtered: ${metrics.countAfterFilter}")
        if (config.rerankStrategy != com.example.day.ragserver.pipeline.RerankStrategy.NONE)
            append(" → Reranked: ${metrics.countAfterRerank}")
        append(" → Final: $finalCount")
        appendLine()
        val timings = metrics.timings.entries.joinToString(", ") { (k, v) -> "$k=${v}ms" }
        if (timings.isNotEmpty()) append("**Timings:** $timings")
    }

    private fun appendSummaryTable(
        report: StringBuilder,
        questionCount: Int,
        avgTopScore: Double,
        config: PipelineConfig,
    ) {
        report.appendLine("---")
        report.appendLine("## Summary")
        report.appendLine("| Метрика | Значение |")
        report.appendLine("|---------|---------|")
        report.appendLine("| Всего вопросов | $questionCount |")
        report.appendLine("| Avg top score | ${"%.2f".format(avgTopScore)} |")
        report.appendLine("| retrievalTopK | ${config.retrievalTopK} |")
        report.appendLine("| threshold | ${config.threshold} |")
        report.appendLine("| rerankStrategy | ${config.rerankStrategy} |")
        report.appendLine("| finalTopK | ${config.finalTopK} |")
    }
}
