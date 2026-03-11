package com.example.day.mcpserver.github

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.put

class GitHubApiClient(
    private val baseUrl: String,
    private val token: String,
    private val defaultOwner: String?,
    private val defaultRepo: String?,
    private val json: Json
) {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
        defaultRequest {
            url(baseUrl)
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.Accept, "application/vnd.github+json")
            header("X-GitHub-Api-Version", "2022-11-28")
        }
    }

    suspend fun getIssue(owner: String?, repo: String?, issueNumber: Int): String {
        val (resolvedOwner, resolvedRepo) = resolveRepo(owner, repo)
        val response = client.get("/repos/$resolvedOwner/$resolvedRepo/issues/$issueNumber")
        return response.body()
    }

    suspend fun listIssues(
        owner: String?,
        repo: String?,
        state: String?,
        labels: List<String>?,
        perPage: Int,
        page: Int,
        includePrs: Boolean
    ): String {
        val (resolvedOwner, resolvedRepo) = resolveRepo(owner, repo)
        val response = client.get("/repos/$resolvedOwner/$resolvedRepo/issues") {
            url {
                parameters.append("state", state ?: "open")
                parameters.append("per_page", perPage.toString())
                parameters.append("page", page.toString())
                labels?.takeIf { it.isNotEmpty() }?.let {
                    parameters.append("labels", it.joinToString(","))
                }
            }
        }
        val raw = response.body<String>()
        return if (includePrs) raw else filterOutPullRequests(raw)
    }

    suspend fun getIssueComments(owner: String?, repo: String?, issueNumber: Int): String {
        val (resolvedOwner, resolvedRepo) = resolveRepo(owner, repo)
        val response = client.get("/repos/$resolvedOwner/$resolvedRepo/issues/$issueNumber/comments")
        return response.body()
    }

    suspend fun getUser(username: String): String {
        val response = client.get("/users/$username")
        return response.body()
    }

    suspend fun createIssue(
        owner: String?,
        repo: String?,
        title: String,
        body: String?,
        labels: List<String>?
    ): String {
        val (resolvedOwner, resolvedRepo) = resolveRepo(owner, repo)
        val response = client.post("/repos/$resolvedOwner/$resolvedRepo/issues") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("title", JsonPrimitive(title))
                body?.let { put("body", JsonPrimitive(it)) }
                labels?.takeIf { it.isNotEmpty() }?.let {
                    put("labels", JsonArray(it.map { label -> JsonPrimitive(label) }))
                }
            })
        }
        return response.body()
    }

    suspend fun createComment(
        owner: String?,
        repo: String?,
        issueNumber: Int,
        body: String
    ): String {
        val (resolvedOwner, resolvedRepo) = resolveRepo(owner, repo)
        val response = client.post("/repos/$resolvedOwner/$resolvedRepo/issues/$issueNumber/comments") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("body", JsonPrimitive(body)) })
        }
        return response.body()
    }

    private fun resolveRepo(owner: String?, repo: String?): Pair<String, String> {
        val resolvedOwner = owner ?: defaultOwner
        val resolvedRepo = repo ?: defaultRepo
        if (resolvedOwner.isNullOrBlank() || resolvedRepo.isNullOrBlank()) {
            error("Repository not configured. Provide owner/repo arguments or set GITHUB_OWNER/GITHUB_REPO.")
        }
        return resolvedOwner to resolvedRepo
    }

    private fun filterOutPullRequests(rawJson: String): String {
        val parsed = json.parseToJsonElement(rawJson)
        if (parsed !is JsonArray) return rawJson
        val filtered = parsed.filterNot { element ->
            val obj = element as? JsonObject ?: return@filterNot false
            obj.containsKey("pull_request")
        }
        return json.encodeToString(JsonElement.serializer(), JsonArray(filtered))
    }
}
