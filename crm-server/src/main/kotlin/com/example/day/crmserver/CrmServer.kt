package com.example.day.crmserver

import com.example.day.crmserver.config.CrmConfig
import com.example.day.crmserver.db.CrmDatabase
import com.example.day.crmserver.tools.registerCrmTools
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.serialization.kotlinx.json.json
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonPrimitive

fun main() {
    val db = CrmDatabase()
    db.connect(CrmConfig.dbPath)

    val server = Server(
        serverInfo = Implementation(name = "crm-mcp", version = "1.0.0"),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = true)
            )
        )
    )

    registerCrmTools(server, db)

    val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    embeddedServer(Netty, port = CrmConfig.serverPort, host = "0.0.0.0") {
        install(ContentNegotiation) { json(json) }

        mcpStreamableHttp { server }

        routing {
            get("/health") {
                call.respond(buildJsonObject { put("status", JsonPrimitive("ok")) })
            }
        }
    }.start(wait = true)
}
