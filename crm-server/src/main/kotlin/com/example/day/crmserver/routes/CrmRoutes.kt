package com.example.day.crmserver.routes

import com.example.day.crmserver.db.CrmDatabase
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

@Serializable
data class CreateCrmUserRequest(val chatId: Long, val userName: String)

@Serializable
data class CreateCrmTicketRequest(val chatId: Long, val title: String, val description: String)

@Serializable
data class UpdateCrmTicketRequest(val ticketId: Long, val status: String, val result: String = "")

@Serializable
data class CheckOpenedTicketResponse(val found: Boolean)

fun Route.crmRoutes(db: CrmDatabase) {
    route("/debug") {
        // GET /debug/users?name=Иван  — поиск по подстроке имени (case-insensitive)
        // GET /debug/users?chatId=123 — найти пользователя по chatId
        get("/users") {
            val name = call.request.queryParameters["name"]
            val chatId = call.request.queryParameters["chatId"]?.toLongOrNull()
            when {
                name != null -> call.respond(db.getUsersByName(name))
                chatId != null -> {
                    val user = db.getUserByChatId(chatId)
                    if (user != null) call.respond(user)
                    else call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                }
                else -> call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Provide ?name=... or ?chatId=...")
                )
            }
        }

        // GET /debug/tickets?chatId=123 — тикеты пользователя
        // GET /debug/tickets?status=open — все тикеты с заданным статусом
        // GET /debug/tickets         — все тикеты
        get("/tickets") {
            val chatId = call.request.queryParameters["chatId"]?.toLongOrNull()
            val status = call.request.queryParameters["status"]
            val tickets = when {
                chatId != null -> db.getTicketsByChatId(chatId)
                status != null -> db.getAllTickets().filter { it.status == status }
                else -> db.getAllTickets()
            }
            call.respond(tickets)
        }
    }

    route("/crm") {
        post("/create_crm_user") {
            val req = call.receive<CreateCrmUserRequest>()
            val existing = db.getUserByChatId(req.chatId)
            val user = existing ?: db.createUser(req.chatId, req.userName)
            call.respond(user)
        }

        post("/create_crm_ticket") {
            val req = call.receive<CreateCrmTicketRequest>()
            val ticket = db.createTicket(req.chatId, req.title, req.description)
            call.respond(ticket)
        }

        post("/update_crm_ticket") {
            val req = call.receive<UpdateCrmTicketRequest>()
            val validStatuses = setOf("open", "closed", "operator")
            if (req.status !in validStatuses) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Invalid status '${req.status}'. Must be one of: open, closed, operator")
                )
                return@post
            }
            db.updateTicketStatus(req.ticketId, req.status, req.result)
            call.respond(mapOf("success" to true, "ticketId" to req.ticketId, "status" to req.status))
        }

        get("/check_opened_ticket") {
            val chatId = call.request.queryParameters["chatId"]?.toLongOrNull()
            val ticketId = call.request.queryParameters["ticketId"]?.toLongOrNull()
            if (chatId == null || ticketId == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "chatId and ticketId are required")
                )
                return@get
            }
            val tickets = db.getTicketsByChatId(chatId)
            val found = tickets.any { it.id == ticketId && it.status == "open" }
            call.respond(CheckOpenedTicketResponse(found = found))
        }
    }
}
