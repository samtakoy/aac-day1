package com.example.day.ragserver.comparison

import java.io.File

/**
 * Парсит runtest-отчёт в формате buildRuntestReport.
 * Формат вопроса: ## Q{N}: "{question}"
 * Текст ответа — всё до следующего заголовка ## Q или конца файла.
 * Возвращает List<Pair<question, answer>>.
 */
object ReportParser {

    private val QUESTION_HEADER_REGEX = Regex("""^## Q\d+: "(.+)"""")

    fun parse(filePath: String): List<Pair<String, String>> {
        val file = File(filePath)
        if (!file.exists()) {
            println("[ReportParser] Файл не найден: $filePath")
            return emptyList()
        }

        val result = mutableListOf<Pair<String, String>>()
        var currentQuestion: String? = null
        val currentAnswer = StringBuilder()

        fun flushCurrent() {
            val q = currentQuestion ?: return
            val a = currentAnswer.toString().trim()
            if (a.isNotEmpty()) result.add(q to a)
        }

        file.forEachLine { line ->
            val match = QUESTION_HEADER_REGEX.find(line)
            if (match != null) {
                flushCurrent()
                currentQuestion = match.groupValues[1]
                currentAnswer.clear()
            } else if (currentQuestion != null) {
                currentAnswer.appendLine(line)
            }
        }
        flushCurrent()

        return result
    }
}
