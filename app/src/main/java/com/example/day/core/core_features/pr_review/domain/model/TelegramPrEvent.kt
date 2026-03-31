package com.example.day.core.core_features.pr_review.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TelegramPrEvent(
    val updateId: Long,
    val prNumber: Int,
    val repo: String,
    val title: String
)
