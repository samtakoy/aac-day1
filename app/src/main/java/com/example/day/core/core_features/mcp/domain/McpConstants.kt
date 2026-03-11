package com.example.day.core.core_features.mcp.domain

object McpConstants {
    const val COMMAND_PREFIX = "@@mcp"
}

object McpToolNames {
    const val GET_ISSUE = "get_issue"
    const val LIST_ISSUES = "list_issues"
    const val GET_ISSUE_COMMENTS = "get_issue_comments"
    const val GET_USER = "get_user"
    const val CREATE_ISSUE = "create_issue"
    const val CREATE_COMMENT = "create_comment"

    val ALLOWED_TOOL_NAMES = setOf(
        GET_ISSUE,
        LIST_ISSUES,
        GET_ISSUE_COMMENTS,
        GET_USER,
        CREATE_ISSUE,
        CREATE_COMMENT
    )
}

object McpToolDefaults {
    const val DEFAULT_PER_PAGE = 20
    const val DEFAULT_PAGE = 1
}
