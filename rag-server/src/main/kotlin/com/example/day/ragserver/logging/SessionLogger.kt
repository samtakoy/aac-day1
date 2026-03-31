package com.example.day.ragserver.logging

import com.example.day.ragserver.db.SearchResult
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Два файла в день:
 *
 * session_YYYY-MM-DD.md  — компактный лог для человека:
 *   запрос → оптимизированный запрос → найденные файлы → итог
 *
 * debug_YYYY-MM-DD.md    — детальный лог для Claude:
 *   промпты, сырые ответы LLM, полные скоры, контекст
 */
class SessionLogger(logsDir: String = "./logs") {

    private val dir = File(logsDir).also { it.mkdirs() }
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    private fun now() = LocalTime.now().format(timeFormatter)
    private fun sessionFile() = File(dir, "session_${LocalDate.now()}.md")
    private fun debugFile() = File(dir, "debug_${LocalDate.now()}.md")

    @Synchronized private fun session(text: String) = sessionFile().appendText(text)
    @Synchronized private fun debug(text: String) = debugFile().appendText(text)

    // ─── TASK-STATE UPDATE ────────────────────────────────────────────────────

    fun logTaskStateUpdate(
        historyForLog: String,
        rawResponse: String,
        intent: String,
        focusClass: String,
        switched: Boolean,
        summary: String,
        model: String = "?",
    ) {
        val time = now()

        session(buildString {
            appendLine()
            appendLine("## [$time] TASK-STATE `$model`")
            appendLine("intent=$intent | focus=$focusClass | switched=$switched")
            if (summary.isNotBlank()) appendLine("summary: ${summary.take(150)}")
            appendLine()
            appendLine("---")
        })

        debug(buildString {
            appendLine()
            appendLine("## [$time] TASK-STATE UPDATE  `model: $model`")
            appendLine()
            appendLine("### LAST_MESSAGES")
            appendLine("```")
            appendLine(historyForLog)
            appendLine("```")
            appendLine()
            appendLine("### RAW LLM RESPONSE")
            appendLine("```")
            appendLine(rawResponse)
            appendLine("```")
            appendLine()
            appendLine("### PARSED RESULT")
            appendLine("intent=$intent | focus=$focusClass | context_switched=$switched")
            appendLine("summary=\"${summary.take(200)}\"")
            appendLine()
            appendLine("---")
        })
    }

    // ─── SEARCH ───────────────────────────────────────────────────────────────

    fun logSearchStart(
        originalQuery: String,
        taskStateJson: String?,
        history: String?,
    ) {
        val time = now()

        session(buildString {
            appendLine()
            appendLine("## [$time] SEARCH")
            appendLine("Query: \"$originalQuery\"")
        })

        debug(buildString {
            appendLine()
            appendLine("## [$time] SEARCH")
            appendLine("Original query: \"$originalQuery\"")
            if (!taskStateJson.isNullOrBlank() && taskStateJson != "{}") {
                appendLine("TaskState: $taskStateJson")
            }
            if (!history.isNullOrBlank()) {
                appendLine("History:")
                appendLine("```")
                appendLine(history)
                appendLine("```")
            }
        })
    }

    fun logQueryOptimize(
        originalQuery: String,
        prompt: String,
        rawResponse: String,
        optimizedQuery: String,
        model: String = "?",
    ) {
        if (optimizedQuery != originalQuery) {
            session(buildString {
                appendLine("→ Optimized: \"$optimizedQuery\"")
            })
        }

        debug(buildString {
            appendLine()
            appendLine("### QUERY OPTIMIZER  `model: $model`")
            appendLine("Input: \"$originalQuery\"")
            appendLine()
            appendLine("**Prompt:**")
            appendLine("```")
            appendLine(prompt)
            appendLine("```")
            appendLine()
            appendLine("**Raw response:** `$rawResponse`")
            appendLine()
            appendLine("**→ Optimized:** \"$optimizedQuery\"")
        })
    }

    fun logRerank(
        query: String,
        prompt: String,
        rawResponse: String,
        scoredResults: List<SearchResult>,
        model: String = "?",
    ) {
        debug(buildString {
            appendLine()
            appendLine("### RERANK  `model: $model`  (${scoredResults.size} candidates, query: \"${query.take(80)}\")")
            appendLine()
            appendLine("**Prompt:**")
            appendLine("```")
            appendLine(prompt)
            appendLine("```")
            appendLine()
            appendLine("**Raw LLM response:**")
            appendLine("```")
            appendLine(rawResponse)
            appendLine("```")
            appendLine()
            appendLine("**Scored results (sorted by LLM score):**")
            scoredResults.sortedByDescending { it.score }.forEach { r ->
                val score = "%.2f".format(r.score)
                appendLine("- $score — ${r.chunk.fileName}:${r.chunk.startLine} ${r.chunk.declarationName ?: ""}")
            }
        })
    }

    fun logSearchFinish(
        finalResults: List<SearchResult>,
        droppedResults: List<SearchResult>,
        contextText: String,
    ) {
        session(buildString {
            if (finalResults.isNotEmpty()) {
                appendLine("Results (${finalResults.size} kept, ${droppedResults.size} dropped):")
                finalResults.forEach { r ->
                    appendLine("  ✓ ${"%.2f".format(r.score)} ${r.chunk.fileName}:${r.chunk.startLine} ${r.chunk.declarationName ?: ""}")
                }
            } else {
                appendLine("Results: none")
            }
            appendLine()
            appendLine("**Context sent to LLM:**")
            appendLine("```")
            appendLine(contextText)
            appendLine("```")
            appendLine()
            appendLine("---")
        })

        debug(buildString {
            appendLine()
            appendLine("### FINAL RESULTS (kept ${finalResults.size}, dropped ${droppedResults.size})")
            appendLine()
            if (finalResults.isNotEmpty()) {
                appendLine("**KEPT:**")
                finalResults.forEach { r ->
                    appendLine("  ✓ ${"%.2f".format(r.score)} — ${r.chunk.fileName}:${r.chunk.startLine} ${r.chunk.declarationName ?: ""}")
                }
            }
            if (droppedResults.isNotEmpty()) {
                appendLine()
                appendLine("**DROPPED by TopK:**")
                droppedResults.forEach { r ->
                    appendLine("  ✗ ${"%.2f".format(r.score)} — ${r.chunk.fileName}:${r.chunk.startLine} ${r.chunk.declarationName ?: ""}")
                }
            }
            appendLine()
            appendLine("**Context sent to LLM (${contextText.length} chars):**")
            appendLine("```")
            appendLine(contextText)  // полный контекст без усечения
            appendLine("```")
            appendLine()
            appendLine("---")
        })
    }

    // ─── TWO-STAGE RETRIEVAL internals ───────────────────────────────────────

    data class Stage2ChunkDetail(
        val className: String,
        val exactMatches: Int,
        val softMatches: Int,
        val topChunks: List<Triple<String, Int, Double>>, // fileName, startLine, score
    )

    /**
     * Логирует внутреннее состояние TwoStageSearchService в debug-файл.
     * Вызывается из TwoStageSearchService после Stage 1 и Stage 2.
     */
    fun logTwoStageSearch(
        query: String,
        metadataCount: Int,
        metadataVectorCount: Int,
        fallback: Boolean,
        stage1Classes: List<Pair<String, Double>>,   // className → finalScore
        stage2Details: List<Stage2ChunkDetail>,
        finalChunks: List<Pair<String, Double>>,     // "fileName:line decl" → score
    ) {
        debug(buildString {
            appendLine()
            appendLine("### TWO-STAGE RETRIEVAL  query=\"${query.take(80)}\"")
            appendLine("metadata=$metadataCount  metadataVectors=$metadataVectorCount  fallback=$fallback")
            appendLine()
            if (fallback) {
                appendLine("**FALLBACK to standardSearch** (no metadata)")
            } else {
                appendLine("**Stage 1 — selected classes (top ${stage1Classes.size}):**")
                stage1Classes.forEach { (cls, score) ->
                    appendLine("  ${"%.3f".format(score)} — $cls")
                }
                appendLine()
                appendLine("**Stage 2 — drill-down per class:**")
                stage2Details.forEach { d ->
                    appendLine("  ${d.className}: exactMatch=${d.exactMatches} softMatch=${d.softMatches}")
                    d.topChunks.forEach { (file, line, score) ->
                        appendLine("    ${"%.3f".format(score)} — $file:$line")
                    }
                }
            }
            appendLine()
            appendLine("**Final ${finalChunks.size} chunks:**")
            finalChunks.forEach { (label, score) ->
                appendLine("  ${"%.3f".format(score)} — $label")
            }
        })
    }

    // ─── INDEXING ─────────────────────────────────────────────────────────────

    /**
     * Лог индексации одного файла: какие чанки были созданы.
     * Пишет в index_YYYY-MM-DD.md (отдельный файл, т.к. индексация — разовая операция).
     */
    fun logIndexingFile(
        fileName: String,
        filePath: String,
        strategy: String,
        chunks: List<IndexedChunkLog>,
    ) {
        val indexFile = File(dir, "index_${LocalDate.now()}.md")
        indexFile.appendText(buildString {
            appendLine()
            appendLine("### $fileName  [$strategy]  ${chunks.size} chunks")
            appendLine("path: $filePath")
            chunks.forEachIndexed { i, c ->
                val decl = buildString {
                    if (c.declarationName != null) append(c.declarationName)
                    if (c.parentScope != null) append(" (in ${c.parentScope})")
                    if (c.nodeType != null) append(" [${c.nodeType}]")
                }
                appendLine("  #$i  line=${c.startLine}  ${decl.ifEmpty { "<no-decl>" }}  len=${c.contentLength}")
            }
        })
    }

    data class IndexedChunkLog(
        val declarationName: String?,
        val parentScope: String?,
        val nodeType: String?,
        val startLine: Int,
        val contentLength: Int,
    )

    // ─── LLM RESPONSE (от Android) ───────────────────────────────────────────

    fun logLlmResponse(
        userMessage: String,
        assistantResponse: String,
        taskStateJson: String?,
    ) {
        val time = now()

        session(buildString {
            appendLine()
            appendLine("## [$time] RESPONSE")
            appendLine("User: \"${userMessage.take(150)}\"")
            appendLine("Response: ${assistantResponse.length} chars")
            appendLine()
            appendLine("---")
        })

        debug(buildString {
            appendLine()
            appendLine("## [$time] LLM RESPONSE")
            appendLine("User: \"${userMessage.take(200)}\"")
            if (!taskStateJson.isNullOrBlank()) {
                appendLine("TaskState: $taskStateJson")
            }
            appendLine()
            appendLine("**Response (${assistantResponse.length} chars):**")
            appendLine("```")
            appendLine(assistantResponse)
            appendLine("```")
            appendLine()
            appendLine("---")
        })
    }
}
