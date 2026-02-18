package com.example.day.core.core_features.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.day.core.core_features.chat.data.local.model.ChatEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ChatDao {
    @Insert
    suspend fun insert(chat: ChatEntity): Long

    @Query("SELECT * FROM chats ORDER BY id ASC")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :chatId")
    suspend fun getChatById(chatId: Long): ChatEntity?

    @Delete
    suspend fun delete(chat: ChatEntity)
}
