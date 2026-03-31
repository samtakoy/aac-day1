package com.example.day.core.core_features.memory.domain.provider

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.memory.domain.provider.base.MemoryProvider
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ToolCallHelperMemoryProvider @Inject constructor() : MemoryProvider {
    override suspend fun getMemoryContext(): List<AContextMessage> {
        val currentTime = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        // return listOf()
        return listOf(
            AContextMessage(
                role = AContextMessage.Role.SYSTEM,
"""Ты можешь использовать внешние инструменты.
    |Когда пользователь попросит выполнить какое-либо действие вызови соответствующий инструмент, если он есть и явно соответствует запросу пользователя.
    |
    |ВАЖНО: После вызова инструмента и получения результата:
    |1. Вызывай инструменты только при необходимости
    |2. Вызывай каждый инструмент не более одного раза за запрос
    |3. Покажи пользователю результат выполнения инструмента
    |4. Если инструмент возвращает результат успешно, задача выполнена
    |5. Не повторяйте вызов одного и того же инструмента с одинаковыми аргументами
    |6. Заверши свой ответ
    |
    |Текущее время пользователя в формате ISO-8601: $currentTime"""
    .trimMargin()
            )
        )
    }
}