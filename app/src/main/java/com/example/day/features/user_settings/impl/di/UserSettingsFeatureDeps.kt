package com.example.day.features.user_settings.impl.di

import com.example.day.core.core_features.memory.domain.usecase.BindUserProfileUseCase
import com.example.day.core.core_features.memory.domain.usecase.CreateUserProfileUseCase
import com.example.day.core.core_features.memory.domain.usecase.DeleteProfileFactUseCase
import com.example.day.core.core_features.memory.domain.usecase.GenerateProfileAvatarUseCase
import com.example.day.core.core_features.memory.domain.usecase.GetAllProfilesUseCase
import com.example.day.core.core_features.memory.domain.usecase.GetCurrentUserProfileUseCase
import com.example.day.core.core_features.memory.domain.usecase.GetProfileFactsFlowUseCase
import com.example.day.core.core_features.memory.domain.usecase.UnbindUserProfileUseCase
import com.example.day.core.core_features.memory.domain.usecase.UpdateProfileAvatarUseCase
import com.example.day.core.core_features.memory.domain.usecase.UpsertFactWithCategoryUseCase

interface UserSettingsFeatureDeps {
    val getCurrentUserProfileUseCase: GetCurrentUserProfileUseCase
    val getAllProfilesUseCase: GetAllProfilesUseCase
    val getProfileFactsFlowUseCase: GetProfileFactsFlowUseCase
    val createUserProfileUseCase: CreateUserProfileUseCase
    val bindUserProfileUseCase: BindUserProfileUseCase
    val unbindUserProfileUseCase: UnbindUserProfileUseCase
    val upsertFactWithCategoryUseCase: UpsertFactWithCategoryUseCase
    val deleteProfileFactUseCase: DeleteProfileFactUseCase
    val generateProfileAvatarUseCase: GenerateProfileAvatarUseCase
    val updateAvatar: UpdateProfileAvatarUseCase
}
