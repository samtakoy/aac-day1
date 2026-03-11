package com.example.day.core.core_features.agent.domain.prompt

object McpSystemPrompt {
    private const val TOOLS_BLOCK = """
MCP Tools (GitHub Issues):
- get_issue(owner?, repo?, issueNumber)
- list_issues(owner?, repo?, state?, labels?, per_page?, page?, include_prs?)
- get_issue_comments(owner?, repo?, issueNumber)
- get_user(username)
- create_issue(owner?, repo?, title, body?, labels?)
- create_comment(owner?, repo?, issueNumber, body)

Notes:
- owner/repo are optional if default repo is configured on the server.
- include_prs defaults to false.
"""

    private const val FORMAT_BLOCK = """
If you need to use a tool, respond with ONLY a JSON object in this format:
{
  "tool": "tool_name",
  "arguments": {
    "param1": "value1",
    "param2": "value2"
  }
}
No additional text around the JSON.
"""

    fun appendTo(systemPrompt: String?): String {
        val base = systemPrompt?.trim().orEmpty()
        return if (base.isBlank()) {
            "$TOOLS_BLOCK\n$FORMAT_BLOCK"
        } else {
            "$base\n\n$TOOLS_BLOCK\n$FORMAT_BLOCK"
        }
    }
}
