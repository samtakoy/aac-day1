package com.example.day.core.core_features.agent.domain.utils

import com.example.day.core.core_features.agent.domain.model.AContextMessage

/**
 * Утилита для построения промпта суммаризации контекста.
 * 
 * Проблема "Глухого телефона": При циклической суммаризации смысл может искажаться.
 * Решение: В промпте указано "Не теряй ключевые факты из предыдущего summary"
 */
object SummarizationPrompt {

    /**
     * Построить промпт для LLM с запросом на создание summary.
     *
     * @param previousSummary предыдущее summary (может быть null при первом сжатии)
     * @param messagesToSummarize сообщения для сжатия (от старых к новым)
     * @return промпт для LLM
     */
    fun buildSummarizationPrompt(
        previousSummary: String?,
        messagesToSummarize: List<AContextMessage>
    ): String {
        return buildString {
            appendLine("Ты — эксперт по ведению протоколов встреч и аналитик данных.")
            appendLine("Твоя задача: обновить краткое содержание (Summary) диалога, добавив в него информацию из новых сообщений.")
            appendLine()
            appendLine("### СТРОГИЕ ПРАВИЛА:")
            appendLine("1. СОХРАНЯЙ КОНКРЕТИКУ: имена, даты, названия проектов, технические параметры, принятые решения и текущие задачи.")
            appendLine("2. ИЗБЕГАЙ ОБОБЩЕНИЙ: Вместо 'обсудили планы' пиши 'решили запустить проект X в понедельник'.")
            appendLine("3. АККУМУЛЯЦИЯ: Новое summary должно включать ВСЮ важную информацию из предыдущего summary и новых сообщений.")
            appendLine("4. ФОРМАТ: Используй маркированный список по категориям (Текущий контекст, Важные данные, Решения).")
            appendLine()

            if (!previousSummary.isNullOrBlank()) {
                appendLine("### ПРЕДЫДУЩЕЕ SUMMARY (ТЕКУЩЕЕ СОСТОЯНИЕ):")
                appendLine(previousSummary)
                appendLine()
            }

            appendLine("### НОВЫЕ СООБЩЕНИЯ ДЛЯ АНАЛИЗА:")
            messagesToSummarize.forEach { msg ->
                // Увеличиваем лимит обрезки или убираем её для важных ролей
                appendLine("[${msg.role.name}]: ${msg.content}")
            }
            appendLine()
            appendLine("Сгенерируй обновленное summary на основе этих данных. Отвечай только текстом summary.")
        }
    }
}
