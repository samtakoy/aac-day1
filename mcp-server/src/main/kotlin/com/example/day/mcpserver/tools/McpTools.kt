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
import java.io.File
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
    // Day 34: Local file tools
    const val READ_LOCAL_FILE = "read_local_file"
    const val WRITE_LOCAL_FILE = "write_local_file"
    const val LIST_LOCAL_FILES = "list_local_files"
    const val SEARCH_LOCAL_FILES = "search_local_files"
    // Day 32: GitHub PR tools
    const val GET_PR_INFO = "get_pr_info"
    const val GET_PR_DIFF = "get_pr_diff"
    const val GET_PR_FILE_DIFF = "get_pr_file_diff"
    const val ADD_PR_REVIEW_COMMENT = "add_pr_review_comment"
}

object GitHubToolDefaults {
    const val DEFAULT_STATE = "open"
    const val DEFAULT_PER_PAGE = 20
    const val DEFAULT_PAGE = 1
}

/**
 * Converts a glob pattern (using '/' as separator) to a Regex.
 * Supports * (any chars within a segment), ** (any chars including '/'),
 * ? (single char), and {a,b,c} brace expansion (e.g. "*.{kt,java}").
 * Works cross-platform — does not rely on OS-specific PathMatcher.
 */
private fun globToRegex(glob: String): Regex {
    val sb = StringBuilder("^")
    var i = 0
    while (i < glob.length) {
        when {
            glob[i] == '*' && i + 1 < glob.length && glob[i + 1] == '*' -> {
                sb.append(".*")
                i += 2
                if (i < glob.length && glob[i] == '/') i++ // skip trailing slash after **
            }
            glob[i] == '*' -> { sb.append("[^/]*"); i++ }
            glob[i] == '?' -> { sb.append("[^/]"); i++ }
            glob[i] == '{' -> {
                val end = glob.indexOf('}', i)
                if (end == -1) { sb.append(Regex.escape("{")); i++ }
                else {
                    val alternatives = glob.substring(i + 1, end).split(',')
                    sb.append("(?:")
                    alternatives.forEachIndexed { idx, alt ->
                        if (idx > 0) sb.append('|')
                        sb.append(Regex.escape(alt.trim()))
                    }
                    sb.append(')')
                    i = end + 1
                }
            }
            else -> { sb.append(Regex.escape(glob[i].toString())); i++ }
        }
    }
    sb.append("$")
    return Regex(sb.toString())
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
    registerGetGitFileList(server, api, projectPath)
    registerGetFileContent(server, api)
    registerResetGitFileListCache(server, api)
    // Day 31: Developer assistant
    registerGetCurrentGitBranch(server, projectPath)
    // Day 34: Local file tools
    registerReadLocalFile(server, projectPath)
    registerWriteLocalFile(server, projectPath)
    registerListLocalFiles(server, projectPath)
    registerSearchLocalFiles(server, projectPath)
    // Day 32: GitHub PR tools
    registerGetPrInfo(server, api)
    registerGetPrDiff(server, api)
    registerGetPrFileDiff(server, api)
    registerAddPrReviewComment(server, api)
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

private fun registerGetGitFileList(server: Server, api: GitHubApiClient, projectPath: String) {
    server.addTool(
        name = GitHubToolNames.GET_GIT_FILE_LIST,
        description = "Получает список файлов через GitHub API (только запушенные ветки). " +
            "Используй ТОЛЬКО когда нужен список файлов конкретной remote-ветки. " +
            "Для работы с текущими локальными файлами используй list_local_files — он видит незапушенные изменения и работает быстрее.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("pattern", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Glob-паттерн для фильтрации файлов (globstar). Если указать просто имя файла без '*' и '/' — инструмент сам применит '**/*name*' (рекурсивный поиск). Примеры: 'MemoryProviderFactory.kt' → найдёт файл в любой директории; '**/*Worker*' — все файлы с 'Worker' в имени; 'app/src/**/*.kt' — все .kt в поддиректориях пути. Если не указан — возвращаются все файлы."))
                })
            }
        )
    ) { request ->
        val rawPattern = request.arguments?.get("pattern")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        // If pattern looks like a plain filename (no '/' and no '*') — treat as **/*pattern*
        val pattern = rawPattern?.let {
            if (!it.contains('/') && !it.contains('*')) "**/*$it*" else it
        }
        if (pattern != null) {
            // Get all files via GitHub API, then filter locally by glob pattern
            api.listAllFiles().fold(
                onSuccess = { allFiles ->
                    val regex = globToRegex(pattern)
                    val fileList = allFiles.filter { regex.matches(it.trimStart('/')) }
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
        } else {
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
}

private fun registerGetFileContent(server: Server, api: GitHubApiClient) {
    server.addTool(
        name = GitHubToolNames.GET_FILE_CONTENT,
        description = "Скачивает содержимое файла через GitHub API (только запушенные ветки). " +
            "Используй ТОЛЬКО когда нужно получить файл из конкретной remote-ветки (например сравнить с 'main'). " +
            "Для чтения текущих локальных файлов используй read_local_file — он работает без push и быстрее.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("file_path", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Полный путь к файлу (например /path/to/file.kt)"))
                })
                put("branch", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Ветка для получения файла. Если не указана — используется текущая (GIT_BRANCH). Пример: 'main'"))
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
        val branch = request.arguments?.get("branch")?.jsonPrimitive?.contentOrNull
        api.getFileContent(filePath, branch).fold(
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

// Day 34: Local file tools

private fun localFile(projectPath: String, filePath: String): File {
    val root = File(projectPath).canonicalFile
    val target = File(root, filePath.trimStart('/')).canonicalFile
    require(target.path.startsWith(root.path)) { "Access denied: path outside project root" }
    return target
}

private fun registerReadLocalFile(server: Server, projectPath: String) {
    server.addTool(
        name = GitHubToolNames.READ_LOCAL_FILE,
        description = "Читает содержимое файла из локального проекта на устройстве. " +
            "Используй для чтения текущего состояния файла (включая незапушенные изменения). " +
            "file_path — путь относительно корня проекта, например '/app/src/main/java/com/example/Foo.kt'.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("file_path", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Путь к файлу от корня проекта, например /app/src/main/java/com/example/Foo.kt"))
                })
            },
            required = listOf("file_path")
        )
    ) { request ->
        val filePath = request.arguments?.get("file_path")?.jsonPrimitive?.contentOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent(text = "error: file_path is required")),
                isError = true
            )
        try {
            val file = localFile(projectPath, filePath)
            if (!file.exists()) {
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = buildJsonObject {
                        put("status", JsonPrimitive("error"))
                        put("error", JsonPrimitive("File not found: $filePath"))
                    }.toString())),
                    isError = true
                )
            }
            val content = file.readText(Charsets.UTF_8)
            CallToolResult(content = listOf(TextContent(text = buildJsonObject {
                put("status", JsonPrimitive("ok"))
                put("content", JsonPrimitive(content))
            }.toString())))
        } catch (e: IllegalArgumentException) {
            CallToolResult(
                content = listOf(TextContent(text = buildJsonObject {
                    put("status", JsonPrimitive("error"))
                    put("error", JsonPrimitive(e.message ?: "Access denied"))
                }.toString())),
                isError = true
            )
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(TextContent(text = buildJsonObject {
                    put("status", JsonPrimitive("error"))
                    put("error", JsonPrimitive(e.message ?: "Unknown error"))
                }.toString())),
                isError = true
            )
        }
    }
}

private fun registerWriteLocalFile(server: Server, projectPath: String) {
    server.addTool(
        name = GitHubToolNames.WRITE_LOCAL_FILE,
        description = "Создаёт или полностью перезаписывает файл в локальном проекте. " +
            "Промежуточные директории создаются автоматически. " +
            "ВАЖНО: передавай полное содержимое файла — существующий файл будет заменён целиком.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("file_path", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Путь к файлу от корня проекта, например /app/src/main/java/com/example/Foo.kt"))
                })
                put("content", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Полное содержимое файла (заменяет существующий файл целиком)"))
                })
            },
            required = listOf("file_path", "content")
        )
    ) { request ->
        val filePath = request.arguments?.get("file_path")?.jsonPrimitive?.contentOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent(text = "error: file_path is required")),
                isError = true
            )
        val content = request.arguments?.get("content")?.jsonPrimitive?.contentOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent(text = "error: content is required")),
                isError = true
            )
        try {
            val file = localFile(projectPath, filePath)
            file.parentFile?.mkdirs()
            file.writeText(content, Charsets.UTF_8)
            CallToolResult(content = listOf(TextContent(text = buildJsonObject {
                put("status", JsonPrimitive("ok"))
                put("file_path", JsonPrimitive(filePath))
                put("bytes_written", JsonPrimitive(content.toByteArray(Charsets.UTF_8).size))
            }.toString())))
        } catch (e: IllegalArgumentException) {
            CallToolResult(
                content = listOf(TextContent(text = buildJsonObject {
                    put("status", JsonPrimitive("error"))
                    put("error", JsonPrimitive(e.message ?: "Access denied"))
                }.toString())),
                isError = true
            )
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(TextContent(text = buildJsonObject {
                    put("status", JsonPrimitive("error"))
                    put("error", JsonPrimitive(e.message ?: "Unknown error"))
                }.toString())),
                isError = true
            )
        }
    }
}

private fun registerListLocalFiles(server: Server, projectPath: String) {
    server.addTool(
        name = GitHubToolNames.LIST_LOCAL_FILES,
        description = "Возвращает список файлов из локального проекта на устройстве (включая незапушенные изменения). " +
            "В отличие от get_git_file_list не требует push — читает прямо с диска. " +
            "Опциональный параметр pattern — glob-паттерн (например '**/*.kt'). " +
            "Просто имя файла без '*' и '/' автоматически превращается в '**/*name*'.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("pattern", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Glob-паттерн. Примеры: 'Foo.kt' → найдёт в любой директории; '**/*Worker*' — все файлы с Worker в имени; 'app/src/**/*.kt' — все .kt файлы. Если не указан — все файлы."))
                })
            }
        )
    ) { request ->
        try {
            val rawPattern = request.arguments?.get("pattern")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            val pattern = rawPattern?.let {
                if (!it.contains('/') && !it.contains('*')) "**/*$it*" else it
            }
            val root = File(projectPath)
            val allFiles = root.walk()
                .filter { it.isFile }
                .map { "/" + it.relativeTo(root).path.replace('\\', '/') }
                .toList()
            val filtered = if (pattern != null) {
                val regex = globToRegex(pattern)
                allFiles.filter { regex.matches(it.trimStart('/')) }
            } else {
                allFiles
            }
            CallToolResult(content = listOf(TextContent(text = buildJsonObject {
                put("status", JsonPrimitive("ok"))
                put("content", JsonArray(filtered.map { JsonPrimitive(it) }))
            }.toString())))
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(TextContent(text = buildJsonObject {
                    put("status", JsonPrimitive("error"))
                    put("error", JsonPrimitive(e.message ?: "Unknown error"))
                }.toString())),
                isError = true
            )
        }
    }
}

private fun registerSearchLocalFiles(server: Server, projectPath: String) {
    server.addTool(
        name = GitHubToolNames.SEARCH_LOCAL_FILES,
        description = "Ищет строки, содержащие заданный текст, во всех локальных файлах проекта. " +
            "Возвращает ТОЛЬКО совпадающие строки с указанием файла и номера строки — не читает файлы целиком. " +
            "Используй вместо read_local_file когда нужно найти что-то по всему проекту. " +
            "Опциональный glob ограничивает поиск по типу файлов (например '**/*.kt').",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("pattern", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Текст для поиска (поддерживается regex). Например: '// Day', 'registerMcpTools', 'addTool'"))
                })
                put("glob", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Glob-фильтр файлов. Например: '**/*.kt', '**/*.md'. Если не указан — ищет во всех файлах."))
                })
                put("max_results", buildJsonObject {
                    put("type", JsonPrimitive("integer"))
                    put("description", JsonPrimitive("Максимальное число совпадений (по умолчанию 200)"))
                })
            },
            required = listOf("pattern")
        )
    ) { request ->
        val patternStr = request.arguments?.get("pattern")?.jsonPrimitive?.contentOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent(text = "error: pattern is required")),
                isError = true
            )
        val rawGlob = request.arguments?.get("glob")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        val glob = rawGlob?.let {
            if (!it.contains('/') && !it.contains('*')) "**/*$it*" else it
        }
        val maxResults = request.arguments?.get("max_results")?.jsonPrimitive?.intOrNull ?: 200

        try {
            val regex = Regex(patternStr)
            val globRegex = glob?.let { globToRegex(it) }
            val root = File(projectPath)

            data class Match(val path: String, val line: Int, val text: String)

            val matches = mutableListOf<Match>()
            root.walk().filter { it.isFile }.forEach { file ->
                if (matches.size >= maxResults) return@forEach
                val relativePath = "/" + file.relativeTo(root).path.replace('\\', '/')
                if (globRegex != null && !globRegex.matches(relativePath.trimStart('/'))) return@forEach
                runCatching {
                    file.bufferedReader(Charsets.UTF_8).useLines { lines ->
                        lines.forEachIndexed { idx, line ->
                            if (matches.size < maxResults && regex.containsMatchIn(line)) {
                                matches.add(Match(relativePath, idx + 1, line.trim()))
                            }
                        }
                    }
                }
            }

            val resultLines = matches.joinToString("\n") { "${it.path}:${it.line}: ${it.text}" }
            CallToolResult(content = listOf(TextContent(text = buildJsonObject {
                put("status", JsonPrimitive("ok"))
                put("count", JsonPrimitive(matches.size))
                put("results", JsonPrimitive(resultLines))
            }.toString())))
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(TextContent(text = buildJsonObject {
                    put("status", JsonPrimitive("error"))
                    put("error", JsonPrimitive(e.message ?: "Unknown error"))
                }.toString())),
                isError = true
            )
        }
    }
}

// Day 32: GitHub PR tools

private fun registerGetPrInfo(server: Server, api: GitHubApiClient) {
    server.addTool(
        name = GitHubToolNames.GET_PR_INFO,
        description = "Получить информацию о Pull Request: заголовок, описание, статус, автор, список изменённых файлов и SHA коммита. Используй для получения обзора PR перед ревью.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("pr_number", buildJsonObject {
                    put("type", JsonPrimitive("integer"))
                    put("description", JsonPrimitive("Номер Pull Request"))
                })
                put("repo", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Репозиторий в формате owner/repo"))
                })
            },
            required = listOf("pr_number", "repo")
        )
    ) { request ->
        val prNumber = request.arguments?.get("pr_number")?.jsonPrimitive?.intOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent(text = "error: pr_number is required")),
                isError = true
            )
        val repo = request.arguments?.get("repo")?.jsonPrimitive?.contentOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent(text = "error: repo is required")),
                isError = true
            )
        try {
            val text = api.getPrInfo(repo = repo, prNumber = prNumber)
            CallToolResult(content = listOf(TextContent(text = text)))
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(TextContent(text = "error: ${e.message ?: "Unknown error"}")),
                isError = true
            )
        }
    }
}

private fun registerGetPrDiff(server: Server, api: GitHubApiClient) {
    server.addTool(
        name = GitHubToolNames.GET_PR_DIFF,
        description = "Получить полный diff Pull Request. Возвращает текст в формате unified diff.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("pr_number", buildJsonObject {
                    put("type", JsonPrimitive("integer"))
                    put("description", JsonPrimitive("Номер Pull Request"))
                })
                put("repo", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Репозиторий в формате owner/repo"))
                })
            },
            required = listOf("pr_number", "repo")
        )
    ) { request ->
        val prNumber = request.arguments?.get("pr_number")?.jsonPrimitive?.intOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent(text = "error: pr_number is required")),
                isError = true
            )
        val repo = request.arguments?.get("repo")?.jsonPrimitive?.contentOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent(text = "error: repo is required")),
                isError = true
            )
        try {
            val diff = api.getPrDiff(repo = repo, prNumber = prNumber)
            CallToolResult(content = listOf(TextContent(text = diff)))
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(TextContent(text = "error: ${e.message ?: "Unknown error"}")),
                isError = true
            )
        }
    }
}

private fun registerGetPrFileDiff(server: Server, api: GitHubApiClient) {
    server.addTool(
        name = GitHubToolNames.GET_PR_FILE_DIFF,
        description = "Получить diff конкретного файла из Pull Request. Используй для детального анализа изменений в одном файле.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("pr_number", buildJsonObject {
                    put("type", JsonPrimitive("integer"))
                    put("description", JsonPrimitive("Номер Pull Request"))
                })
                put("repo", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Репозиторий в формате owner/repo"))
                })
                put("file_path", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Путь к файлу, например app/src/main/java/com/example/Foo.kt"))
                })
            },
            required = listOf("pr_number", "repo", "file_path")
        )
    ) { request ->
        val prNumber = request.arguments?.get("pr_number")?.jsonPrimitive?.intOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent(text = "error: pr_number is required")),
                isError = true
            )
        val repo = request.arguments?.get("repo")?.jsonPrimitive?.contentOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent(text = "error: repo is required")),
                isError = true
            )
        val filePath = request.arguments?.get("file_path")?.jsonPrimitive?.contentOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent(text = "error: file_path is required")),
                isError = true
            )
        try {
            val patch = api.getPrFileDiff(repo = repo, prNumber = prNumber, filePath = filePath)
            CallToolResult(content = listOf(TextContent(text = patch)))
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(TextContent(text = "error: ${e.message ?: "Unknown error"}")),
                isError = true
            )
        }
    }
}

private fun registerAddPrReviewComment(server: Server, api: GitHubApiClient) {
    server.addTool(
        name = GitHubToolNames.ADD_PR_REVIEW_COMMENT,
        description = "Добавить review-комментарий к конкретной строке файла в Pull Request на GitHub. Используй когда нашёл конкретную проблему в коде.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("pr_number", buildJsonObject {
                    put("type", JsonPrimitive("integer"))
                    put("description", JsonPrimitive("Номер Pull Request"))
                })
                put("repo", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Репозиторий в формате owner/repo"))
                })
                put("file_path", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Путь к файлу"))
                })
                put("body", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Текст комментария"))
                })
                put("line", buildJsonObject {
                    put("type", JsonPrimitive("integer"))
                    put("description", JsonPrimitive("Номер строки в новой версии файла (должна входить в diff)"))
                })
                put("commit_id", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("SHA коммита (head_sha из get_pr_info)"))
                })
            },
            required = listOf("pr_number", "repo", "file_path", "body", "line", "commit_id")
        )
    ) { request ->
        val prNumber = request.arguments?.get("pr_number")?.jsonPrimitive?.intOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent(text = "error: pr_number is required")),
                isError = true
            )
        val repo = request.arguments?.get("repo")?.jsonPrimitive?.contentOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent(text = "error: repo is required")),
                isError = true
            )
        val filePath = request.arguments?.get("file_path")?.jsonPrimitive?.contentOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent(text = "error: file_path is required")),
                isError = true
            )
        val body = request.arguments?.get("body")?.jsonPrimitive?.contentOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent(text = "error: body is required")),
                isError = true
            )
        val line = request.arguments?.get("line")?.jsonPrimitive?.intOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent(text = "error: line is required")),
                isError = true
            )
        val commitId = request.arguments?.get("commit_id")?.jsonPrimitive?.contentOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent(text = "error: commit_id is required")),
                isError = true
            )
        try {
            val result = api.addPrReviewComment(
                repo = repo,
                prNumber = prNumber,
                filePath = filePath,
                body = body,
                line = line,
                commitId = commitId
            )
            CallToolResult(content = listOf(TextContent(text = result)))
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(TextContent(text = "error: ${e.message ?: "Unknown error"}")),
                isError = true
            )
        }
    }
}
