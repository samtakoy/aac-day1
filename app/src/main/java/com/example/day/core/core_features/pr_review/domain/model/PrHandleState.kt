package com.example.day.core.core_features.pr_review.domain.model

import com.example.day.core.core_features.llm.domain.model.ModelSettings

data class PrHandleState(
    val isEnabled: Boolean,
    val chatId: Long,
    val modelSettings: ModelSettings?
)
