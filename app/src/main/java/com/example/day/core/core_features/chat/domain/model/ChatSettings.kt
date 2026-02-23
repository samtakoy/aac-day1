package com.example.day.core.core_features.chat.domain.model

import com.example.day.core.core_features.llm.domain.model.ModelSettings

// TODO
data class ChatSettings(
    val chatId: Long,
    val systemPromt: String,
    val model: ModelSettings
)