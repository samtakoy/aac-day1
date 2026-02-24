package com.example.day.features.console.impl.domain.agents

import com.example.day.core.core_features.agent.domain.model.AContextOwner

/**
 * Интерфейс инструментов для взаимодействия агента с внешним миром.
 * Наследует [AContextOwner] для управления контекстом агента.
 */
interface WorkerTools : AContextOwner {
    suspend fun createChat(chatTitle: String, groupId: Long): Long
    suspend fun addBotMessage(chatId: Long, message: String)
}
