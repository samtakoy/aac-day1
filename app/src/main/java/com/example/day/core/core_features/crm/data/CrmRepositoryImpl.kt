package com.example.day.core.core_features.crm.data

import com.example.day.core.core_features.crm.domain.CrmRepository
import com.example.day.core.core_features.crm.domain.CrmTicket
import com.example.day.core.core_features.crm.domain.CrmUser
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
private data class CreateCrmUserRequest(val chatId: Long, val userName: String)

@Serializable
private data class CreateCrmTicketRequest(val chatId: Long, val title: String, val description: String)

@Serializable
private data class UpdateCrmTicketRequest(val ticketId: Long, val status: String, val result: String = "")

@Serializable
private data class UserResponse(val id: Long, val chatId: Long, val name: String)

@Serializable
private data class TicketResponse(
    val id: Long,
    val chatId: Long,
    val status: String,
    val title: String,
    val description: String,
    val result: String
)

@Serializable
private data class CheckOpenedTicketResponse(val found: Boolean)

class CrmRepositoryImpl @Inject constructor(
    private val client: HttpClient
) : CrmRepository {

    override suspend fun createCrmUser(chatId: Long, userName: String): Result<CrmUser> = runCatching {
        val response = client.post("$BASE_URL/crm/create_crm_user") {
            contentType(ContentType.Application.Json)
            setBody(CreateCrmUserRequest(chatId, userName))
        }.body<UserResponse>()
        CrmUser(id = response.id, chatId = response.chatId, name = response.name)
    }

    override suspend fun createCrmTicket(chatId: Long, title: String, description: String): Result<CrmTicket> = runCatching {
        val response = client.post("$BASE_URL/crm/create_crm_ticket") {
            contentType(ContentType.Application.Json)
            setBody(CreateCrmTicketRequest(chatId, title, description))
        }.body<TicketResponse>()
        CrmTicket(
            id = response.id,
            chatId = response.chatId,
            status = response.status,
            title = response.title,
            description = response.description,
            result = response.result
        )
    }

    override suspend fun updateCrmTicket(ticketId: Long, status: String, result: String): Result<Unit> = runCatching {
        client.post("$BASE_URL/crm/update_crm_ticket") {
            contentType(ContentType.Application.Json)
            setBody(UpdateCrmTicketRequest(ticketId, status, result))
        }
        Unit
    }

    override suspend fun checkOpenedTicket(chatId: Long, ticketId: Long): Result<Boolean> = runCatching {
        val response = client.get("$BASE_URL/crm/check_opened_ticket") {
            parameter("chatId", chatId)
            parameter("ticketId", ticketId)
        }.body<CheckOpenedTicketResponse>()
        response.found
    }

    companion object {
        private const val BASE_URL = "http://10.0.2.2:3002"
    }
}
