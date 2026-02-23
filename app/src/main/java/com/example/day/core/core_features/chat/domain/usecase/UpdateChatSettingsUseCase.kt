package com.example.day.core.core_features.chat.domain.usecase

import com.example.day.core.core_features.chat.domain.ChatRepository
import com.example.day.core.core_features.chat.domain.model.ChatSettings
import javax.inject.Inject

class UpdateChatSettingsUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(settings: ChatSettings) {
        repository.updateChatSettings(settings)
    }
}
