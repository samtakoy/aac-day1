package com.example.day.crmserver.db

import org.jetbrains.exposed.sql.Table

object CrmUsersTable : Table("crm_users") {
    val id = long("id").autoIncrement()
    val chatId = long("chat_id").uniqueIndex()
    val name = varchar("name", 255)
    override val primaryKey = PrimaryKey(id)
}
