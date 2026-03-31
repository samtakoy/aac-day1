package com.example.day.core.core_features.pr_review.domain.usecase

import com.example.day.core.core_features.pr_review.domain.model.PrHandleState
import com.example.day.core.core_features.pr_review.domain.repository.PrHandleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPrHandleStateUseCase @Inject constructor(
    private val prHandleRepository: PrHandleRepository
) {
    operator fun invoke(): Flow<PrHandleState> = prHandleRepository.getPrHandleStateFlow()
}
