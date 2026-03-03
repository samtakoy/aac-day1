package com.example.day.features.chats.impl.di

import com.example.day.core.core_features.memory.domain.repository.ArtifactRepository
import com.example.day.core.core_features.chat.domain.usecase.CreateChatUseCase
import com.example.day.core.core_features.chat.domain.usecase.GetChatsByGroupUseCase

interface ChatsFeatureDeps {
    val createChatUseCase: CreateChatUseCase
    val getChatsByGroupUseCase: GetChatsByGroupUseCase
    val artifactRepository: ArtifactRepository
}
