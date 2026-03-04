package com.example.day.core.core_features.memory.data.local.model.link

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.example.day.core.core_features.agent.data.local.model.AgentEntity
import com.example.day.core.core_features.memory.data.local.model.LTMGroupEntity

/**
 * Links an LTM group to an agent (one-to-one relationship).
 * Each agent has exactly one LTM group for storing facts.
 * Deleting the agent will cascade delete this link.
 */
@Entity(
    tableName = "ltm_group_to_agent",
    primaryKeys = ["ltm_group_id"],
    foreignKeys = [
        ForeignKey(
            entity = LTMGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["ltm_group_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AgentEntity::class,
            parentColumns = ["id"],
            childColumns = ["agent_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["ltm_group_id"]),
        Index(value = ["agent_id"], unique = true)
    ]
)
internal data class LTMGroupToAgentEntity(
    @ColumnInfo(name = "ltm_group_id")
    val ltmGroupId: Long,

    @ColumnInfo(name = "agent_id")
    val agentId: Long
)
