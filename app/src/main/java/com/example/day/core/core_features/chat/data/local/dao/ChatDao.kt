package com.example.day.core.core_features.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.day.core.core_features.chat.data.local.model.ChatEntity
import com.example.day.core.core_features.chat.data.local.model.joins.ChatWithGroupAndSettings
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ChatDao {
    @Insert
    suspend fun insert(chat: ChatEntity): Long

    @Transaction
    @Query("SELECT * FROM chats WHERE id = :chatId")
    suspend fun getChatById(chatId: Long): ChatWithGroupAndSettings?

    @Transaction
    @Query("SELECT * FROM chats WHERE id = :chatId")
    fun getChatByIdAsFlow(chatId: Long): Flow<ChatWithGroupAndSettings?>

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun delete(chatId: Long)

    @Transaction
    @Query("SELECT * FROM chats WHERE chat_group_id = :groupId ORDER BY id ASC")
    fun getChatsByGroupAsFlow(groupId: Long): Flow<List<ChatWithGroupAndSettings>>
    
    @Query("SELECT COUNT(*) FROM chats WHERE chat_group_id = :groupId")
    suspend fun getChatsCountInGroup(groupId: Long): Int

    @Transaction
    @Query("SELECT * FROM chats WHERE title = :title AND chat_group_id = :groupId")
    suspend fun getChatByTitleAndGroupId(title: String, groupId: Long): ChatWithGroupAndSettings?

    @Query("UPDATE chats SET title = :title WHERE id = :chatId")
    suspend fun updateChatTitle(chatId: Long, title: String)

    @Query("SELECT * FROM chats WHERE parent_id = :parentId ORDER BY id ASC")
    fun getSubChats(parentId: Long): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE parent_id = :parentId ORDER BY id ASC")
    suspend fun getSubChatsOnce(parentId: Long): List<ChatEntity>

    @Query("UPDATE chats SET working_summary = :summary WHERE id = :chatId")
    suspend fun updateWorkingSummary(chatId: Long, summary: String)

    @Query("UPDATE chats SET is_planner_main = :isMain WHERE id = :chatId")
    suspend fun updateIsPlannerMain(chatId: Long, isMain: Boolean)

    @Query("SELECT * FROM chats WHERE chat_group_id = :groupId AND is_planner_main = 1 LIMIT 1")
    suspend fun getMainPlannerChat(groupId: Long): ChatEntity?
}
