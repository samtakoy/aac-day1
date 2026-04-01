package com.example.day.crmserver.config

object CrmConfig {
    val dbPath: String = System.getenv("CRM_DB_PATH") ?: "crm.db"
    val serverPort: Int = System.getenv("CRM_SERVER_PORT")?.toIntOrNull() ?: 3002
}
