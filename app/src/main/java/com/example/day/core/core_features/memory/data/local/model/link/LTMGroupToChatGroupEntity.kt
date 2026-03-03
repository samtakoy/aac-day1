package com.example.day.core.core_features.memory.data.local.model.link

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.example.day.core.core_features.chat.data.local.model.ChatGroupEntity
import com.example.day.core.core_features.memory.data.local.model.LTMGroupEntity

/**
 * Links an LTM group to a chat group (one-to-one relationship).
 * When a chat group is created, an LTM group can be lazily created and linked.
 * Deleting the chat group will cascade delete this link and the LTM group.
 */
@Entity(
    tableName = "ltm_group_to_chat_group",
    primaryKeys = ["ltm_group_id"],
    foreignKeys = [
        ForeignKey(
            entity = LTMGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["ltm_group_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChatGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["chat_group_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["ltm_group_id"]),
        Index(value = ["chat_group_id"], unique = true)
    ]
)
internal data class LTMGroupToChatGroupEntity(
    @ColumnInfo(name = "ltm_group_id")
    val ltmGroupId: Long,

    @ColumnInfo(name = "chat_group_id")
    val chatGroupId: Long
)
