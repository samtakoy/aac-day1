package com.example.day.features.console.impl.domain.agents

/**
 * Утилитные функции для обработки строк команд
 */
internal fun String.trimCmd(): String =
    (this as CharSequence).trim { it.isWhitespace() || it.isISOControl() }.toString()

/**
 * Возвращает значение по умолчанию, если строка пустая
 */
internal fun String.ifBlank(defaultValue: () -> String): String =
    if (isBlank()) defaultValue() else this

/**
 * Извлекает текст из скобок в начале строки.
 * Например: "(model1, model2) task" -> "model1, model2"
 */
internal fun String.extractFromStartBrackets(): String? {
    val regex = """^\(([^)]*)\)""".toRegex()
    return regex.find(this)?.groupValues?.get(1)
}
