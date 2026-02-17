package com.example.day.core.core_features.chat.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
internal data class ChatEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String
)
