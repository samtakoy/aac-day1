package com.example.day.core.core_features.llm.domain.model

data class LlmResult(
    val text: String,
    val source: ModelResult?
)