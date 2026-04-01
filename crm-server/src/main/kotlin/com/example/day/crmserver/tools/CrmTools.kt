package com.example.day.crmserver.tools

import com.example.day.crmserver.db.CrmDatabase
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

fun registerCrmTools(server: Server, db: CrmDatabase) {
    registerGetCrmUserByChat(server, db)
    registerCreateCrmUser(server, db)
    registerGetCrmUserTickets(server, db)
    registerCreateCrmTicket(server, db)
    registerUpdateCrmTicket(server, db)
}

private fun registerGetCrmUserByChat(server: Server, db: CrmDatabase) {
    server.addTool(
        name = CrmToolNames.GET_CRM_USER_BY_CHAT,
        description = "Get CRM user by Telegram chat ID. Returns user object or null if not found.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("chatId", buildJsonObject {
                    put("type", JsonPrimitive("integer"))
                    put("description", JsonPrimitive("Telegram chat ID"))
                })
            },
            required = listOf("chatId")
        )
    ) { request ->
        try {
            val chatId = request.arguments?.get("chatId")?.jsonPrimitive?.longOrNull
                ?: return@addTool CallToolResult(
                    content = listOf(TextContent(text = "chatId is required")),
                    isError = true
                )
            val user = db.getUserByChatId(chatId)
            val responseText = if (user != null) {
                json.encodeToString(user)
            } else {
                """{"found": false}"""
            }
            CallToolResult(content = listOf(TextContent(text = responseText)))
        } catch (e: Throwable) {
            CallToolResult(content = listOf(TextContent(text = "Error: ${e.message}")), isError = true)
        }
    }
}

private fun registerCreateCrmUser(server: Server, db: CrmDatabase) {
    server.addTool(
        name = CrmToolNames.CREATE_CRM_USER,
        description = "Create a new CRM user. Use when user is not found by get_crm_user_by_chat.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("chatId", buildJsonObject {
                    put("type", JsonPrimitive("integer"))
                    put("description", JsonPrimitive("Telegram chat ID"))
                })
                put("userName", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("User display name"))
                })
            },
            required = listOf("chatId", "userName")
        )
    ) { request ->
        try {
            val chatId = request.arguments?.get("chatId")?.jsonPrimitive?.longOrNull
                ?: return@addTool CallToolResult(
                    content = listOf(TextContent(text = "chatId is required")),
                    isError = true
                )
            val userName = request.arguments?.get("userName")?.jsonPrimitive?.content
                ?: return@addTool CallToolResult(
                    content = listOf(TextContent(text = "userName is required")),
                    isError = true
                )
            val existing = db.getUserByChatId(chatId)
            val user = existing ?: db.createUser(chatId, userName)
            CallToolResult(content = listOf(TextContent(text = json.encodeToString(user))))
        } catch (e: Throwable) {
            CallToolResult(content = listOf(TextContent(text = "Error: ${e.message}")), isError = true)
        }
    }
}

private fun registerGetCrmUserTickets(server: Server, db: CrmDatabase) {
    server.addTool(
        name = CrmToolNames.GET_CRM_USER_TICKETS,
        description = "Get all tickets for a user by chat ID. Returns list of tickets with id, status, title, description.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("chatId", buildJsonObject {
                    put("type", JsonPrimitive("integer"))
                    put("description", JsonPrimitive("Telegram chat ID"))
                })
            },
            required = listOf("chatId")
        )
    ) { request ->
        try {
            val chatId = request.arguments?.get("chatId")?.jsonPrimitive?.longOrNull
                ?: return@addTool CallToolResult(
                    content = listOf(TextContent(text = "chatId is required")),
                    isError = true
                )
            val tickets = db.getTicketsByChatId(chatId)
            CallToolResult(content = listOf(TextContent(text = json.encodeToString(tickets))))
        } catch (e: Throwable) {
            CallToolResult(content = listOf(TextContent(text = "Error: ${e.message}")), isError = true)
        }
    }
}

private fun registerCreateCrmTicket(server: Server, db: CrmDatabase) {
    server.addTool(
        name = CrmToolNames.CREATE_CRM_TICKET,
        description = "Create a new support ticket for user. Status is set to 'open' automatically.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("chatId", buildJsonObject {
                    put("type", JsonPrimitive("integer"))
                    put("description", JsonPrimitive("Telegram chat ID"))
                })
                put("title", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Short ticket title"))
                })
                put("description", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Detailed problem description"))
                })
            },
            required = listOf("chatId", "title", "description")
        )
    ) { request ->
        try {
            val chatId = request.arguments?.get("chatId")?.jsonPrimitive?.longOrNull
                ?: return@addTool CallToolResult(
                    content = listOf(TextContent(text = "chatId is required")),
                    isError = true
                )
            val title = request.arguments?.get("title")?.jsonPrimitive?.content
                ?: return@addTool CallToolResult(
                    content = listOf(TextContent(text = "title is required")),
                    isError = true
                )
            val description = request.arguments?.get("description")?.jsonPrimitive?.content
                ?: return@addTool CallToolResult(
                    content = listOf(TextContent(text = "description is required")),
                    isError = true
                )
            val ticket = db.createTicket(chatId, title, description)
            CallToolResult(content = listOf(TextContent(text = json.encodeToString(ticket))))
        } catch (e: Throwable) {
            CallToolResult(content = listOf(TextContent(text = "Error: ${e.message}")), isError = true)
        }
    }
}

private fun registerUpdateCrmTicket(server: Server, db: CrmDatabase) {
    val validStatuses = setOf("open", "closed", "operator")
    server.addTool(
        name = CrmToolNames.UPDATE_CRM_TICKET,
        description = "Update ticket status. status can be: 'open', 'closed', 'operator'. Use result to summarize resolution.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("ticketId", buildJsonObject {
                    put("type", JsonPrimitive("integer"))
                    put("description", JsonPrimitive("Ticket ID"))
                })
                put("status", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("New status: open, closed, or operator"))
                })
                put("result", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Resolution summary (optional)"))
                })
            },
            required = listOf("ticketId", "status")
        )
    ) { request ->
        try {
            val ticketId = request.arguments?.get("ticketId")?.jsonPrimitive?.longOrNull
                ?: return@addTool CallToolResult(
                    content = listOf(TextContent(text = "ticketId is required")),
                    isError = true
                )
            val status = request.arguments?.get("status")?.jsonPrimitive?.content
                ?: return@addTool CallToolResult(
                    content = listOf(TextContent(text = "status is required")),
                    isError = true
                )
            if (status !in validStatuses) {
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Invalid status '$status'. Must be one of: open, closed, operator")),
                    isError = true
                )
            }
            val result = request.arguments?.get("result")?.jsonPrimitive?.content ?: ""
            db.updateTicketStatus(ticketId, status, result)
            val responseText = buildJsonObject {
                put("success", JsonPrimitive(true))
                put("ticketId", JsonPrimitive(ticketId))
                put("status", JsonPrimitive(status))
            }.toString()
            CallToolResult(content = listOf(TextContent(text = responseText)))
        } catch (e: Throwable) {
            CallToolResult(content = listOf(TextContent(text = "Error: ${e.message}")), isError = true)
        }
    }
}
