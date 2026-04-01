package com.example.day.crmserver.db

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

@Serializable
data class UserRow(val id: Long, val chatId: Long, val name: String)

@Serializable
data class TicketRow(
    val id: Long,
    val chatId: Long,
    val status: String,
    val title: String,
    val description: String,
    val result: String
)

class CrmDatabase {

    fun connect(dbPath: String) {
        Database.connect("jdbc:sqlite:$dbPath", driver = "org.sqlite.JDBC")
        transaction {
            SchemaUtils.createMissingTablesAndColumns(CrmUsersTable, CrmTicketsTable)
        }
        println("CrmDatabase connected: $dbPath")
    }

    fun getUserByChatId(chatId: Long): UserRow? = transaction {
        CrmUsersTable.selectAll()
            .where { CrmUsersTable.chatId eq chatId }
            .map { row ->
                UserRow(
                    id = row[CrmUsersTable.id],
                    chatId = row[CrmUsersTable.chatId],
                    name = row[CrmUsersTable.name]
                )
            }
            .singleOrNull()
    }

    fun createUser(chatId: Long, name: String): UserRow = transaction {
        val insertedId = CrmUsersTable.insert {
            it[CrmUsersTable.chatId] = chatId
            it[CrmUsersTable.name] = name
        }[CrmUsersTable.id]
        UserRow(id = insertedId, chatId = chatId, name = name)
    }

    fun getTicketsByChatId(chatId: Long): List<TicketRow> = transaction {
        CrmTicketsTable.selectAll()
            .where { CrmTicketsTable.chatId eq chatId }
            .map { row ->
                TicketRow(
                    id = row[CrmTicketsTable.id],
                    chatId = row[CrmTicketsTable.chatId],
                    status = row[CrmTicketsTable.status],
                    title = row[CrmTicketsTable.title],
                    description = row[CrmTicketsTable.description],
                    result = row[CrmTicketsTable.result]
                )
            }
    }

    fun createTicket(chatId: Long, title: String, description: String): TicketRow = transaction {
        val insertedId = CrmTicketsTable.insert {
            it[CrmTicketsTable.chatId] = chatId
            it[CrmTicketsTable.status] = "open"
            it[CrmTicketsTable.title] = title
            it[CrmTicketsTable.description] = description
            it[CrmTicketsTable.result] = ""
        }[CrmTicketsTable.id]
        TicketRow(
            id = insertedId,
            chatId = chatId,
            status = "open",
            title = title,
            description = description,
            result = ""
        )
    }

    fun updateTicketStatus(ticketId: Long, status: String, result: String = "") = transaction {
        CrmTicketsTable.update({ CrmTicketsTable.id eq ticketId }) {
            it[CrmTicketsTable.status] = status
            it[CrmTicketsTable.result] = result
        }
    }
}
