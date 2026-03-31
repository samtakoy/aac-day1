package com.example.day.core.core_features.pr_review.data

import com.example.day.core.app_settings.AppSettings
import com.example.day.core.core_features.llm.domain.model.ModelSettings
import com.example.day.core.core_features.pr_review.domain.model.PrHandleState
import com.example.day.core.core_features.pr_review.domain.repository.PrHandleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class PrHandleRepositoryImpl @Inject constructor(
    private val appSettings: AppSettings
) : PrHandleRepository {

    override fun getPrHandleStateFlow(): Flow<PrHandleState> =
        appSettings.prHandleStateFlow

    override suspend fun setPrHandleState(isEnabled: Boolean, chatId: Long, modelSettings: ModelSettings?) =
        appSettings.setPrHandleState(isEnabled, chatId, modelSettings)

    override suspend fun getLastTelegramUpdateId(): Long =
        appSettings.getLastTelegramUpdateId()

    override suspend fun saveLastTelegramUpdateId(id: Long) =
        appSettings.saveLastTelegramUpdateId(id)
}
