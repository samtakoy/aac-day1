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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.Base64

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

    /**
     * Получает список всех файлов в репозитории через GitHub Tree API (рекурсивно).
     * Возвращает пути в формате /path/to/file.ext
     */
    suspend fun listAllFiles(): Result<List<String>> = runCatching {
        val (resolvedOwner, resolvedRepo) = resolveRepo(null, null)

        // Получаем SHA default branch через repo info
        val repoInfo = client.get("/repos/$resolvedOwner/$resolvedRepo")
            .body<JsonObject>()
        val defaultBranch = repoInfo["default_branch"]?.jsonPrimitive?.content ?: "main"

        // Запрашиваем дерево файлов рекурсивно
        val treeResponse = client.get(
            "/repos/$resolvedOwner/$resolvedRepo/git/trees/$defaultBranch?recursive=1"
        ).body<JsonObject>()

        val tree = treeResponse["tree"]?.jsonArray
            ?: error("Invalid tree response: missing 'tree' field")

        tree.mapNotNull { element ->
            val obj = element.jsonObject
            val type = obj["type"]?.jsonPrimitive?.content
            val path = obj["path"]?.jsonPrimitive?.content
            if (type == "blob" && path != null) "/$path" else null
        }
    }

    /**
     * Скачивает содержимое файла по полному пути.
     * GitHub возвращает содержимое в base64, метод декодирует его.
     */
    suspend fun getFileContent(filePath: String): Result<String> = runCatching {
        val (resolvedOwner, resolvedRepo) = resolveRepo(null, null)
        val cleanPath = filePath.trimStart('/')

        val response = client.get(
            "/repos/$resolvedOwner/$resolvedRepo/contents/$cleanPath"
        ).body<JsonObject>()

        val contentBase64 = response["content"]?.jsonPrimitive?.content
            ?: error("No 'content' field in response for $filePath")

        // GitHub добавляет переносы строк в base64 — убираем их
        val cleanBase64 = contentBase64.replace("\n", "").replace("\r", "")
        String(Base64.getDecoder().decode(cleanBase64), Charsets.UTF_8)
    }

    private fun resolveRepo(owner: String?, repo: String?): Pair<String, String> {
        // TODO всегда наши на данный момент
        val resolvedOwner = defaultOwner // owner ?: defaultOwner
        val resolvedRepo = defaultRepo // repo ?: defaultRepo
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
