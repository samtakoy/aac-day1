package com.example.day.features.chats.impl.di

import com.example.day.core.core_features.chat.domain.usecase.CreateChatUseCase
import com.example.day.core.core_features.chat.domain.usecase.GetChatListAsFlowUseCase
import kotlin.properties.Delegates.notNull

interface ChatsFeatureDeps {
    val createChatUseCase: CreateChatUseCase
    val getChatsUseCase: GetChatListAsFlowUseCase
}

interface ChatsFeatureDepsProvider {
    val deps: ChatsFeatureDeps

    companion object: ChatsFeatureDepsProvider by ChatsFeatureDepsStore
}

object ChatsFeatureDepsStore : ChatsFeatureDepsProvider {
    override var deps: ChatsFeatureDeps by notNull()
}