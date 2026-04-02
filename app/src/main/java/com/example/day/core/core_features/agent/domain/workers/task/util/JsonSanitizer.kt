package com.example.day.core.core_features.agent.domain.workers.task.util

/**
 * Fixes unescaped characters inside JSON string values produced by LLM.
 *
 * LLMs sometimes emit literal newlines, tabs, or unescaped double-quotes inside
 * string values, making the JSON unparseable. This scanner walks the raw JSON
 * character by character and escapes such characters only when inside a string.
 *
 * To distinguish a closing `"` from an in-value `"` we use two signals:
 * 1. Lookahead: the next non-whitespace character must be `:`, `,`, `}`, or `]`.
 * 2. innerDepth == 0: we track `{`/`[` and `}`/`]` seen inside the current string.
 *    While inside an embedded object or array the string cannot end yet, so even
 *    a `"` followed by a structural token is treated as an unescaped quote.
 *    This correctly handles values like `"Данные: {"name": "Иван"}"`.
 */
fun sanitizeJsonStringValues(json: String): String {
    val sb = StringBuilder(json.length)
    var i = 0
    var inString = false
    var escape = false
    var innerDepth = 0  // depth of {[ seen inside the current string value

    while (i < json.length) {
        val c = json[i]
        when {
            escape -> {
                sb.append(c)
                escape = false
            }
            inString && c == '\\' -> {
                sb.append(c)
                escape = true
            }
            inString && c == '"' -> {
                if (innerDepth == 0 && isJsonStringTerminator(json, i + 1)) {
                    sb.append(c)
                    inString = false
                } else {
                    sb.append("\\\"")
                }
            }
            inString && (c == '{' || c == '[') -> {
                innerDepth++
                sb.append(c)
            }
            inString && (c == '}' || c == ']') -> {
                if (innerDepth > 0) innerDepth--
                sb.append(c)
            }
            inString && c == '\n' -> sb.append("\\n")
            inString && c == '\r' -> sb.append("\\r")
            inString && c == '\t' -> sb.append("\\t")
            c == '"' -> {
                sb.append(c)
                inString = true
                innerDepth = 0
            }
            else -> sb.append(c)
        }
        i++
    }
    return sb.toString()
}

/**
 * Returns true if the `"` before [fromIndex] is a legitimate JSON string
 * terminator, i.e. the next non-whitespace character is a structural token.
 */
private fun isJsonStringTerminator(json: String, fromIndex: Int): Boolean {
    var j = fromIndex
    while (j < json.length && json[j].isWhitespace()) j++
    if (j >= json.length) return true
    return json[j] == ':' || json[j] == ',' || json[j] == '}' || json[j] == ']'
}
