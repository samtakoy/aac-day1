package com.example.day.features.console.impl.domain.agents

interface WorkerTools {
    suspend fun createChat(chatTitle: String): Long
    suspend fun addBotMessage(chatId: Long, message: String)
}