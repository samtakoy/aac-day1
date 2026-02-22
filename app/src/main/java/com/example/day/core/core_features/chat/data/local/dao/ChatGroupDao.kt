package com.example.day.core.core_features.chat.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.day.core.core_features.chat.data.local.model.ChatGroupEntity
import com.example.day.core.core_features.chat.data.local.model.joins.ChatGroupWithType
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ChatGroupDao {
    @Query("SELECT * FROM chat_groups ORDER BY id ASC")
    fun getAllGroupsAsFlow(): Flow<List<ChatGroupEntity>>
    
    @Query("SELECT * FROM chat_groups WHERE id = :groupId LIMIT 1")
    suspend fun getGroupById(groupId: Long): ChatGroupEntity?

    @Transaction
    @Query("SELECT * FROM chat_groups ORDER BY id DESC LIMIT 1")
    suspend fun getGroupWithMaxId(): ChatGroupWithType?

    @Transaction
    @Query("""
        SELECT * FROM chat_groups ORDER BY id ASC
    """)
    fun getAllGroupsWithTypeAsFlow(): Flow<List<ChatGroupWithType>>
    
    @Insert
    suspend fun insertGroup(group: ChatGroupEntity): Long
    
    @Update
    suspend fun updateGroup(group: ChatGroupEntity)
    
    @Delete
    suspend fun deleteGroup(group: ChatGroupEntity)
    
    @Query("DELETE FROM chat_groups WHERE id = :groupId")
    suspend fun deleteGroupById(groupId: Long)
}
