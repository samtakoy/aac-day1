package com.example.day.core.core_features.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.day.core.core_features.chat.data.local.model.ChatEntity
import com.example.day.core.core_features.chat.data.local.model.joins.ChatWithGroup
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ChatDao {
    @Insert
    suspend fun insert(chat: ChatEntity): Long

    @Transaction
    @Query("SELECT * FROM chats WHERE id = :chatId")
    suspend fun getChatById(chatId: Long): ChatWithGroup?

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun delete(chatId: Long)

    @Transaction
    @Query("SELECT * FROM chats WHERE chat_group_id = :groupId ORDER BY id ASC")
    fun getChatsByGroupAsFlow(groupId: Long): Flow<List<ChatWithGroup>>
    
    @Query("SELECT COUNT(*) FROM chats WHERE chat_group_id = :groupId")
    suspend fun getChatsCountInGroup(groupId: Long): Int
}
