package com.example.day.core.core_features.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.day.core.core_features.chat.data.local.model.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface MessageDao {
    @Insert
    suspend fun insert(message: MessageEntity): Long

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesByChatId(chatId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: Long): MessageEntity?

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateStatus(messageId: Long, status: Int)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteById(messageId: Long)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteByChatId(chatId: Long)

    @Query("DELETE FROM messages WHERE chatId = :chatId AND status != :viewedStatus")
    suspend fun deleteByChatIdAndStatusNotViewed(chatId: Long, viewedStatus: Int)
}
