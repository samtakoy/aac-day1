package com.example.day.ragserver.comparison

import java.io.File

/**
 * Парсит файл с эталонными ответами (answers_v2.md).
 * Формат заголовка: ## Q{N}: "вопрос"
 * Возвращает Map<Int, String> — 1-based индекс → текст ответа.
 */
object ReferenceAnswerParser {

    private val HEADER_REGEX = Regex("""^## Q(\d+):""")

    fun parse(filePath: String): Map<Int, String> {
        val file = File(filePath)
        if (!file.exists()) {
            println("[ReferenceAnswerParser] Файл не найден: $filePath")
            return emptyMap()
        }

        val result = mutableMapOf<Int, String>()
        var currentIndex: Int? = null
        val currentBody = StringBuilder()

        fun flushCurrent() {
            val idx = currentIndex ?: return
            val text = currentBody.toString().trim()
            if (text.isNotEmpty()) result[idx] = text
        }

        file.forEachLine { line ->
            val match = HEADER_REGEX.find(line)
            if (match != null) {
                flushCurrent()
                currentIndex = match.groupValues[1].toIntOrNull()
                currentBody.clear()
            } else if (currentIndex != null) {
                // пропускаем горизонтальный разделитель
                if (line.trim() != "---") {
                    currentBody.appendLine(line)
                }
            }
        }
        flushCurrent()

        println("[ReferenceAnswerParser] Загружено эталонных ответов: ${result.size}")
        return result
    }
}
