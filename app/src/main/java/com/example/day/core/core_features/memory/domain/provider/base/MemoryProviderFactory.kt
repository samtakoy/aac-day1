package com.example.day.core.core_features.memory.domain.provider.base

import com.example.day.core.core_features.memory.domain.provider.CompositeMemoryProvider
import com.example.day.core.core_features.memory.domain.provider.EmptyMemoryProvider
import com.example.day.core.core_features.memory.domain.provider.UserProfileMemoryProvider
import javax.inject.Inject

class MemoryProviderFactory @Inject constructor(
    private val userProfileMemoryProvider: UserProfileMemoryProvider
) {
    fun create(
        memoryTypes: List<MemoryType>,
        chatId: Long,
        chatGroupId: Long
    ): MemoryProvider {
        if (memoryTypes.isNotEmpty()) {
            return CompositeMemoryProvider(
                memoryTypes.mapNotNull { createProviderByType(it) }
            )
        }
        return EmptyMemoryProvider()
    }

    private fun createProviderByType(type: MemoryType): MemoryProvider? {
        return when (type) {
            MemoryType.UserProfile -> userProfileMemoryProvider
            MemoryType.Chat -> null
            MemoryType.ChatGroup -> null
        }
    }
}