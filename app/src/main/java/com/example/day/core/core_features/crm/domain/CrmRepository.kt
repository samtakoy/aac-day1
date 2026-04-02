package com.example.day.core.core_features.crm.domain

interface CrmRepository {
    suspend fun createCrmUser(chatId: Long, userName: String): Result<CrmUser>
    suspend fun createCrmTicket(chatId: Long, title: String, description: String): Result<CrmTicket>
    suspend fun updateCrmTicket(ticketId: Long, status: String, result: String = ""): Result<Unit>
    suspend fun checkOpenedTicket(chatId: Long, ticketId: Long): Result<Boolean>
}

data class CrmUser(val id: Long, val chatId: Long, val name: String)

data class CrmTicket(
    val id: Long,
    val chatId: Long,
    val status: String,
    val title: String,
    val description: String,
    val result: String
)
