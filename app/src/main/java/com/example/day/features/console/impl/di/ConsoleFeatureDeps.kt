package com.example.day.features.console.impl.di

import com.example.day.core.core_features.chat.domain.usecase.AddChatMessageUseCase
import com.example.day.core.core_features.chat.domain.usecase.ChangeMessageStatusUseCase
import com.example.day.core.core_features.chat.domain.usecase.ClearChatNotViewedMessageUseCase
import com.example.day.core.core_features.chat.domain.usecase.GetChatMessagesAsFlowUseCase
import com.example.day.core.core_features.chat.domain.usecase.GetChatMessagesWithStatusUseCase
import com.example.day.features.console.impl.domain.LlmRequestUseCase
import io.ktor.client.HttpClient
import kotlin.properties.Delegates.notNull

interface ConsoleFeatureDeps {
    fun httpClient(): HttpClient
    val getMessagesUseCase: GetChatMessagesAsFlowUseCase
    val getMessagesWithStatusUseCase: GetChatMessagesWithStatusUseCase
    val clearUnviewedUseCase: ClearChatNotViewedMessageUseCase
    val addChatMessageUseCase: AddChatMessageUseCase
    val changeMessageUseCase: ChangeMessageStatusUseCase
}

interface ConsoleFeatureDepsProvider {
    val deps: ConsoleFeatureDeps

    companion object: ConsoleFeatureDepsProvider by ConsoleFeatureDepsStore
}

object ConsoleFeatureDepsStore : ConsoleFeatureDepsProvider {
    override var deps: ConsoleFeatureDeps by notNull()
}