package com.example.day.core.core_features.agent.domain.strategy

import com.example.day.core.core_features.agent.domain.model.AContext
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.AContextParams
import com.example.day.core.core_features.agent.domain.model.AContextState
import kotlinx.collections.immutable.toPersistentList

object AContextDefaultFactory {
    fun createEmpty(): AContext = AContext(
        params = AContextParams.Empty,
        data = AContextState.Empty
    )

    fun createFull(): AContext = AContext(
        params = AContextParams.Full,
        data = AContextState.Full(listOf<AContextMessage>().toPersistentList())
    )
}