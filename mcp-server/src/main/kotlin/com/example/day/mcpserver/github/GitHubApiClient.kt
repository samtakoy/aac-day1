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
import io.ktor.client.request.headers
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
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
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

        // Определяем ветку: из env GIT_BRANCH, иначе default_branch репозитория
        val repoInfo = client.get("/repos/$resolvedOwner/$resolvedRepo").body<JsonObject>()
        val defaultBranch = repoInfo["default_branch"]?.jsonPrimitive?.content ?: "main"
        val branch = System.getenv("GIT_BRANCH")?.takeIf { it.isNotBlank() } ?: defaultBranch

        val branchResp = client.get("/repos/$resolvedOwner/$resolvedRepo/git/ref/heads/$branch")
            .body<JsonObject>()
        val sha = branchResp["object"]?.jsonObject?.get("sha")?.jsonPrimitive?.content
            ?: error("Cannot resolve SHA for branch '$branch'")

        // Запрашиваем дерево файлов рекурсивно по SHA коммита
        val treeResponse = client.get(
            "/repos/$resolvedOwner/$resolvedRepo/git/trees/$sha?recursive=1"
        ).body<JsonObject>()

        val truncated = treeResponse["truncated"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
        if (truncated) error("GitHub tree response is truncated — repository is too large for Trees API")

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
    suspend fun getFileContent(filePath: String, branch: String? = null): Result<String> = runCatching {
        val (resolvedOwner, resolvedRepo) = resolveRepo(null, null)
        val cleanPath = filePath.trimStart('/')
        val refSuffix = branch?.takeIf { it.isNotBlank() }?.let { "?ref=$it" } ?: ""

        val response = client.get(
            "/repos/$resolvedOwner/$resolvedRepo/contents/$cleanPath$refSuffix"
        ).body<JsonObject>()

        val contentBase64 = response["content"]?.jsonPrimitive?.content
            ?: error("No 'content' field in response for $filePath")

        // GitHub добавляет переносы строк в base64 — убираем их
        val cleanBase64 = contentBase64.replace("\n", "").replace("\r", "")
        String(Base64.getDecoder().decode(cleanBase64), Charsets.UTF_8)
    }

    /**
     * Получает информацию о Pull Request: метаданные + список изменённых файлов.
     * @param repo в формате "owner/repo"
     */
    suspend fun getPrInfo(repo: String, prNumber: Int): String {
        val prResponse = client.get("/repos/$repo/pulls/$prNumber").body<JsonObject>()
        val filesResponse = client.get("/repos/$repo/pulls/$prNumber/files").body<JsonArray>()

        val files = filesResponse.map { element ->
            val obj = element.jsonObject
            buildJsonObject {
                put("path", JsonPrimitive(obj["filename"]?.jsonPrimitive?.content ?: ""))
                put("status", JsonPrimitive(obj["status"]?.jsonPrimitive?.content ?: ""))
                put("additions", JsonPrimitive(obj["additions"]?.jsonPrimitive?.intOrNull ?: 0))
                put("deletions", JsonPrimitive(obj["deletions"]?.jsonPrimitive?.intOrNull ?: 0))
            }
        }

        val result = buildJsonObject {
            put("number", JsonPrimitive(prResponse["number"]?.jsonPrimitive?.intOrNull ?: prNumber))
            put("title", JsonPrimitive(prResponse["title"]?.jsonPrimitive?.content ?: ""))
            put("description", JsonPrimitive(prResponse["body"]?.jsonPrimitive?.content ?: ""))
            put("state", JsonPrimitive(prResponse["state"]?.jsonPrimitive?.content ?: ""))
            put("author", JsonPrimitive(prResponse["user"]?.jsonObject?.get("login")?.jsonPrimitive?.content ?: ""))
            put("head_sha", JsonPrimitive(prResponse["head"]?.jsonObject?.get("sha")?.jsonPrimitive?.content ?: ""))
            put("files", JsonArray(files))
        }
        return result.toString()
    }

    /**
     * Получает полный unified diff Pull Request.
     * @param repo в формате "owner/repo"
     */
    suspend fun getPrDiff(repo: String, prNumber: Int): String {
        val response = client.get("/repos/$repo/pulls/$prNumber") {
            headers {
                remove(HttpHeaders.Accept)
                append(HttpHeaders.Accept, "application/vnd.github.v3.diff")
            }
        }
        return response.body()
    }

    /**
     * Получает diff конкретного файла из Pull Request (поле "patch").
     * @param repo в формате "owner/repo"
     */
    suspend fun getPrFileDiff(repo: String, prNumber: Int, filePath: String): String {
        val filesResponse = client.get("/repos/$repo/pulls/$prNumber/files").body<JsonArray>()
        val fileObj = filesResponse
            .map { it.jsonObject }
            .firstOrNull { it["filename"]?.jsonPrimitive?.content == filePath }
            ?: return "File $filePath not found in PR #$prNumber"
        return fileObj["patch"]?.jsonPrimitive?.content ?: "No patch available for $filePath"
    }

    /**
     * Добавляет review-комментарий к конкретной строке файла в Pull Request.
     * @param repo в формате "owner/repo"
     */
    suspend fun addPrReviewComment(
        repo: String,
        prNumber: Int,
        filePath: String,
        body: String,
        line: Int,
        commitId: String
    ): String {
        val response = client.post("/repos/$repo/pulls/$prNumber/comments") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("body", JsonPrimitive(body))
                put("commit_id", JsonPrimitive(commitId))
                put("path", JsonPrimitive(filePath))
                put("line", JsonPrimitive(line))
                put("side", JsonPrimitive("RIGHT"))
            })
        }
        val responseObj = response.body<JsonObject>()
        val id = responseObj["id"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
        return buildJsonObject {
            put("id", JsonPrimitive(id))
            put("status", JsonPrimitive("created"))
        }.toString()
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
