package com.example.day.core.core_features.llm.domain.model

fun ModelResult.Success.getContent(): String {
    return choices.firstOrNull()?.message?.content.orEmpty().trim()
}

fun ModelResult.Success.getReasoning(): String? {
    return choices.firstOrNull()?.message?.reasoning?.trim()
}