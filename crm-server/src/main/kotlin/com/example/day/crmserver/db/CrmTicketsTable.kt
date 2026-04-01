package com.example.day.crmserver.db

import org.jetbrains.exposed.sql.Table

object CrmTicketsTable : Table("crm_tickets") {
    val id = long("id").autoIncrement()
    val chatId = long("chat_id")
    val status = varchar("status", 50)
    val title = varchar("title", 500)
    val description = text("description")
    val result = text("result").default("")
    override val primaryKey = PrimaryKey(id)
}
