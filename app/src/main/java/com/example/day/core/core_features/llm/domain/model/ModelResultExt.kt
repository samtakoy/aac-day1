package com.example.day.core.core_features.llm.domain.model

fun ModelResult.Success.getContent(): String {
    val content = choices.firstOrNull()?.message?.content?.trim()
    if (!content.isNullOrEmpty()) return content
    return choices.firstOrNull()?.message?.reasoning?.trim().orEmpty()
}

fun ModelResult.Success.getReasoning(): String? {
    return choices.firstOrNull()?.message?.reasoning?.trim()
}