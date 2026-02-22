package com.example.day.core.core_features.chat.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_groups",
    foreignKeys = [
        ForeignKey(
            entity = ChatTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["type_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["type_id"])]
)
internal data class ChatGroupEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "type_id")
    val typeId: Long,
    
    @ColumnInfo(name = "color_index")
    val colorIndex: Int = 0
)
