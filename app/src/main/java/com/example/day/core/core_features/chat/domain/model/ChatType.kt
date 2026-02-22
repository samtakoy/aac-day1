package com.example.day.core.core_features.chat.domain.model

enum class ChatType(val dbType: String, val title: String) {
    SIMPLE_HISTORY("simple_history", "Simple History"),
    AGENT_COMMANDS("agent_commands", "Agent Commands");

    companion object {
        fun fromDbType(dbType: String): ChatType? {
            return enumValues<ChatType>().find { it.dbType == dbType }
        }
    }
}
