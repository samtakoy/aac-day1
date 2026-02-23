package com.example.day.core.core_features.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.day.core.core_features.chat.data.local.model.ChatSettingsEntity

@Dao
internal interface ChatSettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: ChatSettingsEntity): Long

    @Query("SELECT * FROM chat_settings WHERE chat_id = :chatId")
    suspend fun getSettingsByChatId(chatId: Long): ChatSettingsEntity?

    @Query("SELECT * FROM chat_settings WHERE chat_id = :chatId")
    fun getSettingsByChatIdAsFlow(chatId: Long): kotlinx.coroutines.flow.Flow<ChatSettingsEntity?>

    @Update
    suspend fun update(settings: ChatSettingsEntity)

    @Query("DELETE FROM chat_settings WHERE chat_id = :chatId")
    suspend fun deleteByChatId(chatId: Long)
}
