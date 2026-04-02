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
    const val SET_REMINDER = "set_reminder"
    
    // Day 19: Git file investigation tools
    const val INVESTIGATE_GIT_FILE = "investigate_git_file"
    const val GET_FILE_ANALYSIS = "get_file_analysis"
    const val ANALYZE_CODE_CONTENT = "analyze_code_content"
    const val GET_GIT_FILE_LIST = "get_git_file_list"
    const val GET_FILE_CONTENT = "get_file_content"
    const val RESET_GIT_FILE_LIST_CACHE = "reset_git_file_list_cache"

    // TODO это как тут оказалось? это инструменты сервера, мы не должны их хардкодить
    val ALLOWED_TOOL_NAMES = setOf(
        "search_codebase",
        "search_codebase_fixed",
        "get_current_git_branch",
        GET_FILE_CONTENT,
        GET_GIT_FILE_LIST,
        "get_pr_info",
        "get_pr_diff",
        "get_pr_file_diff",
        "add_pr_review_comment",
        "get_crm_user_by_chat",
        "create_crm_user",
        "get_crm_user_tickets",
        "create_crm_ticket",
        "update_crm_ticket",
        /*
        GET_ISSUE,
        LIST_ISSUES,
        GET_ISSUE_COMMENTS,
        GET_USER,
        CREATE_ISSUE,
        CREATE_COMMENT,
        SET_REMINDER,
        INVESTIGATE_GIT_FILE,
        GET_FILE_ANALYSIS,
        ANALYZE_CODE_CONTENT,
        GET_GIT_FILE_LIST,
        GET_FILE_CONTENT,
        RESET_GIT_FILE_LIST_CACHE*/
    )
}

object McpToolDefaults {
    const val DEFAULT_PER_PAGE = 20
    const val DEFAULT_PAGE = 1
}
