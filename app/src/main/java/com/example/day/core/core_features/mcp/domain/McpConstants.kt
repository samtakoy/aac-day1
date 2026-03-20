package com.example.day.core.core_features.mcp.domain

object McpConstants {
    const val COMMAND_PREFIX = "@@mcp"
}

/**
 * Tool name constants for reference.
 * NOTE: These are for documentation/reference only. The actual available tools
 * are determined dynamically from connected MCP servers.
 * Access control is managed per-agent via AgentMemoryRepository.
 */
object McpToolNames {
    // Issue tracking tools
    const val GET_ISSUE = "get_issue"
    const val LIST_ISSUES = "list_issues"
    const val GET_ISSUE_COMMENTS = "get_issue_comments"
    const val GET_USER = "get_user"
    const val CREATE_ISSUE = "create_issue"
    const val CREATE_COMMENT = "create_comment"
    const val SET_REMINDER = "set_reminder"
    
    // Git file investigation tools
    const val INVESTIGATE_GIT_FILE = "investigate_git_file"
    const val GET_FILE_ANALYSIS = "get_file_analysis"
    const val ANALYZE_CODE_CONTENT = "analyze_code_content"
    const val GET_GIT_FILE_LIST = "get_git_file_list"
    const val GET_FILE_CONTENT = "get_file_content"
    const val RESET_GIT_FILE_LIST_CACHE = "reset_git_file_list_cache"

    // Code search tools
    const val SEARCH_CODEBASE = "search_codebase"
    const val SEARCH_CODEBASE_FIXED = "search_codebase_fixed"

    // NOTE: Global ALLOWED_TOOL_NAMES removed - tool access is now controlled
    // per-agent via AgentMemoryRepository. If no restrictions are set for an agent,
    // all tools from connected MCP servers are available.
}

object McpToolDefaults {
    const val DEFAULT_PER_PAGE = 20
    const val DEFAULT_PAGE = 1
}
