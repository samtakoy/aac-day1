package com.example.day.core.core_features.mcp.domain

import com.example.day.core.core_features.mcp.domain.model.TransportType

object McpLocalConstants {
    const val LOCAL_SERVER_ID = "local_mcp"
    const val LOCAL_SERVER_NAME = "LocalMcp"
    const val LOCAL_SERVER_URL = "local://mcp"
    val LOCAL_TRANSPORT: TransportType = TransportType.LOCAL
}
