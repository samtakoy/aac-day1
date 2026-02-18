package com.example.day.core.core_features.chat.domain.model

import com.example.day.features.console.impl.domain.model.ModelRequest

data class ChatSettings(
    val chatId: Long,
    val systemPromt: String,
    val stopWord: String,
    val maxTokens: Int,
    val jsonFormat: Boolean
)