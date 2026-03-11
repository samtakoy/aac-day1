package com.example.day.core.core_features.mcp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.day.core.core_features.mcp.data.local.entity.McpServerEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface McpServerDao {

    @Query("SELECT * FROM mcp_servers ORDER BY created_at DESC")
    fun getAllServers(): Flow<List<McpServerEntity>>

    @Query("SELECT * FROM mcp_servers WHERE id = :serverId")
    suspend fun getServerById(serverId: String): McpServerEntity?

    @Query("SELECT * FROM mcp_servers WHERE name = :name LIMIT 1")
    suspend fun getServerByName(name: String): McpServerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: McpServerEntity)

    @Update
    suspend fun updateServer(server: McpServerEntity)

    @Delete
    suspend fun deleteServer(server: McpServerEntity)

    @Query("SELECT COUNT(*) FROM mcp_servers")
    suspend fun getCount(): Int
}
