package com.example.day.core.core_features.pr_review.domain.repository

import com.example.day.core.core_features.llm.domain.model.ModelSettings
import com.example.day.core.core_features.pr_review.domain.model.PrHandleState
import kotlinx.coroutines.flow.Flow

interface PrHandleRepository {
    fun getPrHandleStateFlow(): Flow<PrHandleState>
    suspend fun setPrHandleState(isEnabled: Boolean, chatId: Long, modelSettings: ModelSettings?)
    suspend fun getLastTelegramUpdateId(): Long
    suspend fun saveLastTelegramUpdateId(id: Long)
}
