package com.example.day.core.core_features.agent.domain.model

import kotlinx.collections.immutable.ImmutableList

data class AContext(
    val agentName: String,
    val systemPrompt: String,
    val messages: ImmutableList<AContextMessage>
)