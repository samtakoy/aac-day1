package com.example.day.core.core_features.memory.domain.provider.base

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.chat.domain.model.Chat

/**
 * UserProfileMemoryProvider: Тянет только LTM (скиллы, имя).
 * TaskWorkingMemoryProvider: Тянет только workingSummary (текущий проект).
 * CompositeMemoryProvider: Собирает данные из нескольких источников.
 *
 * */
interface MemoryProvider {
    /**
     * Возвращает список системных сообщений или инструкций,
     * сформированных на основе различных слоев памяти.
     * TODO пока возвращает [AContextMessage] но он из модуля агентов - надо подумать, возможно перенести контекст в memory, тогда все сложится
     */
    // suspend fun getMemoryContext(params: MemoryParams): List<ChatMessage>
    suspend fun getMemoryContext(): List<AContextMessage>
}

/*
data class MemoryParams(
    val metadata: Map<String, String> = emptyMap()
)*/
