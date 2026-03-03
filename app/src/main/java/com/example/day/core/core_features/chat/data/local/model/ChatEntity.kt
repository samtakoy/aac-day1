package com.example.day.core.core_features.chat.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chats",
    foreignKeys = [
        ForeignKey(
            entity = ChatGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["chat_group_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["chat_group_id"]),
        Index(value = ["parent_id"])
    ]
)
internal data class ChatEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "chat_group_id")
    val chatGroupId: Long,
    
    @ColumnInfo(name = "parent_id")
    val parentId: Long? = null,
    
    @ColumnInfo(name = "working_summary")
    val workingSummary: String? = null,
    
    @ColumnInfo(name = "is_planner_main")
    val isPlannerMain: Boolean = false
)
