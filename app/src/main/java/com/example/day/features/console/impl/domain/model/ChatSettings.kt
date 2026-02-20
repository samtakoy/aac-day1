package com.example.day.features.console.impl.domain.model

data class ChatSettings(
    val chatId: Long,
    val systemPromt: String,
    val model: ModelSettings
)