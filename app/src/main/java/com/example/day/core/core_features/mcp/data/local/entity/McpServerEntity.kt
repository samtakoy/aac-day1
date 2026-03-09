package com.example.day.core.core_features.mcp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mcp_servers")
internal data class McpServerEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "url")
    val url: String,

    /** Alias for SecretsVault — not the actual token */
    @ColumnInfo(name = "auth_token_alias")
    val authTokenAlias: String? = null,

    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true,

    /** "HTTP", "SSE", "STREAMABLE_HTTP", or "STDIO" */
    @ColumnInfo(name = "transport_type")
    val transportType: String = "HTTP",

    /** URL path appended to base URL (e.g. /message, /sse, /mcp) */
    @ColumnInfo(name = "url_path")
    val urlPath: String = "/message",

    /** Shell command for STDIO transport */
    @ColumnInfo(name = "stdio_command")
    val stdioCommand: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
