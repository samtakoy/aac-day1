package com.example.day.features.group_choice.impl.di

import com.example.day.core.core_features.chat.domain.usecase.CreateChatGroupUseCase
import com.example.day.core.core_features.chat.domain.usecase.CreatePlannerGroupWithMainChatUseCase
import com.example.day.core.core_features.chat.domain.usecase.DeleteChatGroupUseCase
import com.example.day.core.core_features.chat.domain.usecase.GetChatGroupsUseCase
import com.example.day.core.core_features.chat.domain.usecase.GetChatTypesUseCase
import com.example.day.core.core_features.chat.domain.usecase.UpdateChatGroupUseCase

interface GroupChoiceFeatureDeps {
    val getChatGroupsUseCase: GetChatGroupsUseCase
    val createChatGroupUseCase: CreateChatGroupUseCase
    val updateChatGroupUseCase: UpdateChatGroupUseCase
    val deleteChatGroupUseCase: DeleteChatGroupUseCase
    val getChatTypesUseCase: GetChatTypesUseCase
    val createPlannerGroupWithMainChatUseCase: CreatePlannerGroupWithMainChatUseCase
}
