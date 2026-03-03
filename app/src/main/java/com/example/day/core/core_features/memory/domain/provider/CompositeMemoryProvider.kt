package com.example.day.core.core_features.memory.domain.provider

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.memory.domain.provider.base.MemoryProvider

class CompositeMemoryProvider(
    private val providers: List<MemoryProvider>
) : MemoryProvider {

    override suspend fun getMemoryContext(): List<AContextMessage> {
        return providers.map { it.getMemoryContext() }.flatten()
    }
}