package com.example.day.features.console.impl.di

import com.example.day.core.core_features.agent.domain.workers.tools.AgentTools
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import com.example.day.core.core_features.chat.domain.usecase.AddChatMessageUseCase
import com.example.day.core.core_features.chat.domain.usecase.ChangeMessageStatusUseCase
import com.example.day.core.core_features.chat.domain.usecase.ClearChatNotViewedMessageUseCase
import com.example.day.core.core_features.chat.domain.usecase.CreateChatUseCase
import com.example.day.core.core_features.chat.domain.usecase.GetChatByIdAsFlowUseCase
import com.example.day.core.core_features.chat.domain.usecase.GetChatMessagesAsFlowUseCase
import com.example.day.core.core_features.chat.domain.usecase.GetChatMessagesWithStatusUseCase
import com.example.day.core.core_features.chat.domain.usecase.GetOrCreateChatUseCase
import com.example.day.core.core_features.chat.domain.usecase.UpdateChatSettingsUseCase
import com.example.day.core.core_features.chat.domain.usecase.UpdateChatTitleUseCase
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase

interface ConsoleFeatureDeps {
    val getMessagesUseCase: GetChatMessagesAsFlowUseCase
    val getMessagesWithStatusUseCase: GetChatMessagesWithStatusUseCase
    val clearUnviewedUseCase: ClearChatNotViewedMessageUseCase
    val addChatMessageUseCase: AddChatMessageUseCase
    val changeMessageUseCase: ChangeMessageStatusUseCase
    val llmRequestUseCase: LlmRequestUseCase
    val getChatByIdAsFlowUseCase: GetChatByIdAsFlowUseCase
    val updateChatSettingsUseCase: UpdateChatSettingsUseCase
    val createChatUseCase: CreateChatUseCase
    val getOrCreateChatUseCase: GetOrCreateChatUseCase
    val updateChatTitleUseCase: UpdateChatTitleUseCase
    // Tools для агентов
    val agentTools: AgentTools
    val chatTools: ChatTools
}