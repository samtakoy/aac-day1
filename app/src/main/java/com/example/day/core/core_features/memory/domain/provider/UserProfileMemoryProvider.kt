package com.example.day.core.core_features.memory.domain.provider

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.memory.domain.provider.base.MemoryProvider

class UserProfileMemoryProvider : MemoryProvider {
    override suspend fun getMemoryContext(): List<AContextMessage> {
        // нужно взять userprofile
        // если он не пуст достать - привязанную память
        // если она не пуста - заполнить сообщения
        //
        TODO("Not yet implemented")
    }
}