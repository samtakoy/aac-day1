package com.example.day.core.core_features.agent.domain.model

interface AContextOwner {
    fun getContext(agentName: String): AContext
    fun saveContext(context: AContext)
}