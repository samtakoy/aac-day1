package com.example.day.core.core_features.agent.domain.model

data class AContextMessage(
    val role: Role,
    val content: String,
    val orderNumber: Long
)