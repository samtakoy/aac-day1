package com.example.day.core.core_features.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.day.core.core_features.chat.data.local.model.ChatTypeEntity

@Dao
internal interface ChatTypeDao {
    @Query("SELECT * FROM chat_types")
    suspend fun getAllTypes(): List<ChatTypeEntity>
    
    @Query("SELECT * FROM chat_types WHERE id = :id LIMIT 1")
    suspend fun getTypeById(id: Long): ChatTypeEntity?
    
    @Query("SELECT * FROM chat_types WHERE type = :type LIMIT 1")
    suspend fun getTypeByName(type: String): ChatTypeEntity?
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertType(type: ChatTypeEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTypes(types: List<ChatTypeEntity>)
}
