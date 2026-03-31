package com.example.day.mcpserver

import com.example.day.mcpserver.github.GitHubApiClient
import com.example.day.mcpserver.tools.registerMcpTools
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.serialization.kotlinx.json.json
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp
import kotlinx.serialization.json.Json

object ServerConstants {
    const val DEFAULT_PORT = 3000
    const val API_BASE_URL = "https://api.github.com"
}

fun main() {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
        explicitNulls = false
    }

    val token = System.getenv("GITHUB_TOKEN")
        ?: error("GITHUB_TOKEN environment variable not set")
    val defaultOwner = System.getenv("GITHUB_OWNER")
        ?: error("GITHUB_OWNER environment variable not set")
    val defaultRepo = System.getenv("GITHUB_REPO")
        ?: error("GITHUB_REPO environment variable not set")

    val githubClient = GitHubApiClient(
        baseUrl = ServerConstants.API_BASE_URL,
        token = token,
        defaultOwner = defaultOwner,
        defaultRepo = defaultRepo,
        json = json
    )

    val server = Server(
        serverInfo = Implementation(name = "github-issues-mcp", version = "1.0.0"),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = true)
            )
        )
    )

    val projectPath = System.getenv("GIT_PROJECT_PATH") ?: "."
    registerMcpTools(server, githubClient, projectPath)

    embeddedServer(Netty, port = ServerConstants.DEFAULT_PORT, host = "0.0.0.0") {
        install(ContentNegotiation) { json(json) }

        mcpStreamableHttp { server }

        routing {
            post("/message") {
                call.respondRedirect("/mcp/message", permanent = false)
            }
        }
    }.start(wait = true)
}
