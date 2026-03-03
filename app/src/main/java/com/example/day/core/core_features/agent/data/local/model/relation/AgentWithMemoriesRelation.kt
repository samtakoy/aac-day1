package com.example.day.core.core_features.agent.data.local.model.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.day.core.core_features.agent.data.local.model.AgentEntity
import com.example.day.core.core_features.agent.data.local.model.AgentToMemoryTypeEntity

internal class AgentWithMemoriesRelation(
    @Embedded
    val agent: AgentEntity,

    @Relation(
        entity = AgentToMemoryTypeEntity::class,
        parentColumn = "id",        // ID из AgentEntity
        entityColumn = "agent_id",  // ID из AgentToMemoryTypeEntity
        projection = ["memory_type"] // Указываем, что нам нужна только колонка с названием
    )
    val memoryTypeNames: List<String>
)