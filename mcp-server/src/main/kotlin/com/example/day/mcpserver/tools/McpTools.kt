package com.example.day.mcpserver.tools

import com.example.day.mcpserver.github.GitHubApiClient
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.TimeUnit

object GitHubToolNames {
    const val GET_ISSUE = "get_issue"
    const val LIST_ISSUES = "list_issues"
    const val GET_ISSUE_COMMENTS = "get_issue_comments"
    const val GET_USER = "get_user"
    const val CREATE_ISSUE = "create_issue"
    const val CREATE_COMMENT = "create_comment"
    // Day 19: Git file investigation
    const val GET_GIT_FILE_LIST = "get_git_file_list"
    const val GET_FILE_CONTENT = "get_file_content"
    const val RESET_GIT_FILE_LIST_CACHE = "reset_git_file_list_cache"
    // Day 31: Developer assistant
    const val GET_CURRENT_GIT_BRANCH = "get_current_git_branch"
}

object GitHubToolDefaults {
    const val DEFAULT_STATE = "open"
    const val DEFAULT_PER_PAGE = 20
    const val DEFAULT_PAGE = 1
}

fun registerMcpTools(server: Server, api: GitHubApiClient, projectPath: String) {
    registerEcho(server)
    registerGetIssue(server, api)
    registerListIssues(server, api)
    registerGetIssueComments(server, api)
    registerGetUser(server, api)
    registerCreateIssue(server, api)
    registerCreateComment(server, api)
    // Day 19: Git file investigation tools
    registerGetGitFileList(server, api)
    registerGetFileContent(server, api)
    registerResetGitFileListCache(server, api)
    // Day 31: Developer assistant
    registerGetCurrentGitBranch(server, projectPath)
}

private fun registerEcho(server: Server) {
    server.addTool(
        name = "echo",
        description = "Echo back the input text (test/debug tool)",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("text", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Text to echo back"))
                })
            },
            required = listOf("text")
        )
    ) { request ->
        val text = request.arguments?.get("text")?.jsonPrimitive?.content ?: "(empty)"
        CallToolResult(content = listOf(TextContent(text = "Echo: $text")))
    }
}

private fun registerGetIssue(server: Server, api: GitHubApiClient) {
    server.addTool(
        name = GitHubToolNames.GET_ISSUE,
        description = "Get issue by number from a GitHub repository",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                // put("owner", buildJsonObject { put("type", JsonPrimitive("string")) })
                // put("repo", buildJsonObject { put("type", JsonPrimitive("string")) })
                put("issueNumber", buildJsonObject { put("type", JsonPrimitive("integer")) })
            },
            required = listOf("issueNumber")
        )
    ) { request ->
        val issueNumber = request.arguments?.get("issueNumber")?.jsonPrimitive?.intOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent(text = "issueNumber is required")),
                isError = true
            )
        val text = api.getIssue(
            owner = request.arguments?.get("owner")?.jsonPrimitive?.contentOrNull,
            repo = request.arguments?.get("repo")?.jsonPrimitive?.contentOrNull,
            issueNumber = issueNumber
        )
        CallToolResult(content = listOf(TextContent(text = text)))
    }
}

private fun registerListIssues(server: Server, api: GitHubApiClient) {
    server.addTool(
        name = GitHubToolNames.LIST_ISSUES,
        description = "List issues for a repository (PRs excluded by default)",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                // put("owner", buildJsonObject { put("type", JsonPrimitive("string")) })
                // put("repo", buildJsonObject { put("type", JsonPrimitive("string")) })
                put("state", buildJsonObject { put("type", JsonPrimitive("string")) })
                put("labels", buildJsonObject { put("type", JsonPrimitive("array")) })
                put("per_page", buildJsonObject { put("type", JsonPrimitive("integer")) })
                put("page", buildJsonObject { put("type", JsonPrimitive("integer")) })
                put("include_prs", buildJsonObject { put("type", JsonPrimitive("boolean")) })
            }
        )
    ) { request ->
        val args = request.arguments
        val perPage = args?.get("per_page")?.jsonPrimitive?.intOrNull ?: GitHubToolDefaults.DEFAULT_PER_PAGE
        val page = args?.get("page")?.jsonPrimitive?.intOrNull ?: GitHubToolDefaults.DEFAULT_PAGE
        val includePrs = args?.get("include_prs")?.jsonPrimitive?.booleanOrNull ?: false
        val labels = when (val labelsElement = args?.get("labels")) {
            is JsonArray -> labelsElement.mapNotNull { it.jsonPrimitive.contentOrNull }
            is JsonPrimitive -> labelsElement.content.split(",").map { it.trim() }.filter { it.isNotBlank() }
            else -> null
        }
        val text = api.listIssues(
            owner = args?.get("owner")?.jsonPrimitive?.contentOrNull,
            repo = args?.get("repo")?.jsonPrimitive?.contentOrNull,
            state = args?.get("state")?.jsonPrimitive?.contentOrNull ?: GitHubToolDefaults.DEFAULT_STATE,
            labels = labels,
            perPage = perPage,
            page = page,
            includePrs = includePrs
        )
        CallToolResult(content = listOf(TextContent(text = text)))
    }
}

private fun registerGetIssueComments(server: Server, api: GitHubApiClient) {
    server.addTool(
        name = GitHubToolNames.GET_ISSUE_COMMENTS,
        description = "Get comments for an issue",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                // put("owner", buildJsonObject { put("type", JsonPrimitive("string")) })
                // put("repo", buildJsonObject { put("type", JsonPrimitive("string")) })
                put("issueNumber", buildJsonObject { put("type", JsonPrimitive("integer")) })
            },
            required = listOf("issueNumber")
        )
    ) { request ->
        val issueNumber = request.arguments?.get("issueNumber")?.jsonPrimitive?.intOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent(text = "issueNumber is required")),
                isError = true
            )
        val text = api.getIssueComments(
            owner = request.arguments?.get("owner")?.jsonPrimitive?.contentOrNull,
            repo = request.arguments?.get("repo")?.jsonPrimitive?.contentOrNull,
            issueNumber = issueNumber
        )
        CallToolResult(content = listOf(TextContent(text = text)))
    }
}

private fun registerGetUser(server: Server, api: GitHubApiClient) {
    server.addTool(
        name = GitHubToolNames.GET_USER,
        description = "Get GitHub user information by username",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("username", buildJsonObject { put("type", JsonPrimitive("string")) })
            },
            required = listOf("username")
        )
    ) { request ->
        val username = request.arguments?.get("username")?.jsonPrimitive?.contentOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent(text = "username is required")),
                isError = true
            )
        val text = api.getUser(username)
        CallToolResult(content = listOf(TextContent(text = text)))
    }
}

private fun registerCreateIssue(server: Server, api: GitHubApiClient) {
    server.addTool(
        name = GitHubToolNames.CREATE_ISSUE,
        description = "Create a new issue in a repository",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                // put("owner", buildJsonObject { put("type", JsonPrimitive("string")) })
                // put("repo", buildJsonObject { put("type", JsonPrimitive("string")) })
                put("title", buildJsonObject { put("type", JsonPrimitive("string")) })
                put("body", buildJsonObject { put("type", JsonPrimitive("string")) })
                put("labels", buildJsonObject { put("type", JsonPrimitive("array")) })
            },
            required = listOf("title")
        )
    ) { request ->
        val title = request.arguments?.get("title")?.jsonPrimitive?.contentOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent(text = "title is required")),
                isError = true
            )
        val labels = when (val labelsElement = request.arguments?.get("labels")) {
            is JsonArray -> labelsElement.mapNotNull { it.jsonPrimitive.contentOrNull }
            is JsonPrimitive -> labelsElement.content.split(",").map { it.trim() }.filter { it.isNotBlank() }
            else -> null
        }
        val text = api.createIssue(
            owner = request.arguments?.get("owner")?.jsonPrimitive?.contentOrNull,
            repo = request.arguments?.get("repo")?.jsonPrimitive?.contentOrNull,
            title = title,
            body = request.arguments?.get("body")?.jsonPrimitive?.contentOrNull,
            labels = labels
        )
        CallToolResult(content = listOf(TextContent(text = text)))
    }
}

private fun registerCreateComment(server: Server, api: GitHubApiClient) {
    server.addTool(
        name = GitHubToolNames.CREATE_COMMENT,
        description = "Create a comment for an issue",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                // put("owner", buildJsonObject { put("type", JsonPrimitive("string")) })
                // put("repo", buildJsonObject { put("type", JsonPrimitive("string")) })
                put("issueNumber", buildJsonObject { put("type", JsonPrimitive("integer")) })
                put("body", buildJsonObject { put("type", JsonPrimitive("string")) })
            },
            required = listOf("issueNumber", "body")
        )
    ) { request ->
        val issueNumber = request.arguments?.get("issueNumber")?.jsonPrimitive?.intOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent(text = "issueNumber is required")),
                isError = true
            )
        val body = request.arguments?.get("body")?.jsonPrimitive?.contentOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent(text = "body is required")),
                isError = true
            )
        val text = api.createComment(
            owner = request.arguments?.get("owner")?.jsonPrimitive?.contentOrNull,
            repo = request.arguments?.get("repo")?.jsonPrimitive?.contentOrNull,
            issueNumber = issueNumber,
            body = body
        )
        CallToolResult(content = listOf(TextContent(text = text)))
    }
}

private fun registerGetGitFileList(server: Server, api: GitHubApiClient) {
    server.addTool(
        name = GitHubToolNames.GET_GIT_FILE_LIST,
        description = "Получает список всех полных имён файлов из git репозитория. " +
            "Возвращает массив путей в формате /path/to/file.ext. " +
            "owner и repo берутся из переменных окружения.",
        inputSchema = ToolSchema(properties = buildJsonObject {})
    ) { _ ->
        api.listAllFiles().fold(
            onSuccess = { fileList ->
                val responseJson = buildJsonObject {
                    put("status", JsonPrimitive("ok"))
                    put("content", JsonArray(fileList.map { JsonPrimitive(it) }))
                }
                CallToolResult(content = listOf(TextContent(text = responseJson.toString())))
            },
            onFailure = { error ->
                val responseJson = buildJsonObject {
                    put("status", JsonPrimitive("error"))
                    put("content", JsonArray(emptyList()))
                    put("error", JsonPrimitive(error.message ?: "Unknown error"))
                }
                CallToolResult(
                    content = listOf(TextContent(text = responseJson.toString())),
                    isError = true
                )
            }
        )
    }
}

private fun registerGetFileContent(server: Server, api: GitHubApiClient) {
    server.addTool(
        name = GitHubToolNames.GET_FILE_CONTENT,
        description = "Скачивает содержимое файла из git репозитория по полному пути.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("file_path", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Полный путь к файлу (например /path/to/file.kt)"))
                })
            },
            required = listOf("file_path")
        )
    ) { request ->
        val filePath = request.arguments?.get("file_path")?.jsonPrimitive?.contentOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent(text = "file_path is required")),
                isError = true
            )
        api.getFileContent(filePath).fold(
            onSuccess = { content ->
                val responseJson = buildJsonObject {
                    put("status", JsonPrimitive("ok"))
                    put("content", JsonPrimitive(content))
                }
                CallToolResult(content = listOf(TextContent(text = responseJson.toString())))
            },
            onFailure = { error ->
                val responseJson = buildJsonObject {
                    put("status", JsonPrimitive("error"))
                    put("error", JsonPrimitive(error.message ?: "Unknown error"))
                }
                CallToolResult(
                    content = listOf(TextContent(text = responseJson.toString())),
                    isError = true
                )
            }
        )
    }
}

private fun registerResetGitFileListCache(server: Server, @Suppress("UNUSED_PARAMETER") api: GitHubApiClient) {
    // Кеш живёт на стороне приложения (GitFileCacheRepository).
    // Этот tool служит сигналом для приложения сбросить кеш.
    // Само по себе просто возвращает ok — логика сброса на стороне app.
    server.addTool(
        name = GitHubToolNames.RESET_GIT_FILE_LIST_CACHE,
        description = "Сбрасывает кеш списка файлов git. Используйте для принудительного обновления.",
        inputSchema = ToolSchema(properties = buildJsonObject {})
    ) { _ ->
        val responseJson = buildJsonObject {
            put("status", JsonPrimitive("ok"))
            put("content", JsonArray(emptyList()))
        }
        CallToolResult(content = listOf(TextContent(text = responseJson.toString())))
    }
}

private fun registerGetCurrentGitBranch(server: Server, projectPath: String) {
    server.addTool(
        name = GitHubToolNames.GET_CURRENT_GIT_BRANCH,
        description = "Возвращает текущую ветку git-репозитория проекта",
        inputSchema = ToolSchema(properties = buildJsonObject {})
    ) { _ ->
        try {
            val process = ProcessBuilder("git", "-C", projectPath, "branch", "--show-current").start()
            val completed = process.waitFor(5, TimeUnit.SECONDS)

            if (!completed) {
                process.destroyForcibly()
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Timeout: git не ответил за 5 секунд")),
                    isError = true
                )
            }

            val branchName = process.inputStream.bufferedReader().readText().trim()
            val exitValue = process.exitValue()

            if (exitValue != 0 || branchName.isBlank()) {
                val errorText = process.errorStream.bufferedReader().readText().trim()
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Не удалось получить ветку: $errorText")),
                    isError = true
                )
            }

            CallToolResult(content = listOf(TextContent(text = branchName)))
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(TextContent(text = e.message ?: "Unknown error")),
                isError = true
            )
        }
    }
}
