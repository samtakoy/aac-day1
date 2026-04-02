package com.example.day.core.core_features.agent.domain.workers.task

import org.junit.Assert.*
import org.junit.Test

class TaskResponseParserTest {

    // region — happy path

    @Test
    fun `parses valid JSON with all fields`() {
        val raw = """{"reply_to_user": "Привет!", "memory_updates": {"user_name": "Иван"}, "next_state": "planning"}"""
        val result = TaskResponseParser.parse(raw)
        assertNotNull(result)
        assertEquals("Привет!", result!!.replyToUser)
        assertEquals("Иван", result.memoryUpdates["user_name"])
        assertEquals("planning", result.nextState)
    }

    @Test
    fun `parses valid JSON without optional fields`() {
        val raw = """{"reply_to_user": "ok", "memory_updates": {}}"""
        val result = TaskResponseParser.parse(raw)
        assertNotNull(result)
        assertEquals("ok", result!!.replyToUser)
        assertNull(result.nextState)
        assertNull(result.userApprove)
    }

    @Test
    fun `parses JSON wrapped in markdown code block`() {
        val raw = """
            ```json
            {"reply_to_user": "Готово", "memory_updates": {}, "next_state": "done"}
            ```
        """.trimIndent()
        val result = TaskResponseParser.parse(raw)
        assertNotNull(result)
        assertEquals("Готово", result!!.replyToUser)
        assertEquals("done", result.nextState)
    }

    @Test
    fun `parses JSON wrapped in markdown code block without language tag`() {
        val raw = """
            ```
            {"reply_to_user": "ok", "memory_updates": {}}
            ```
        """.trimIndent()
        val result = TaskResponseParser.parse(raw)
        assertNotNull(result)
        assertEquals("ok", result!!.replyToUser)
    }

    // endregion

    // region — sanitization applied end-to-end

    @Test
    fun `parses reply_to_user with unescaped quotes`() {
        val raw = """{"reply_to_user": "Нажмите "ОК"", "memory_updates": {}}"""
        val result = TaskResponseParser.parse(raw)
        assertNotNull(result)
        assertEquals("""Нажмите "ОК"""", result!!.replyToUser)
    }

    @Test
    fun `parses reply_to_user with literal newline`() {
        val raw = "{\"reply_to_user\": \"Line 1\nLine 2\", \"memory_updates\": {}}"
        val result = TaskResponseParser.parse(raw)
        assertNotNull(result)
        assertEquals("Line 1\nLine 2", result!!.replyToUser)
    }

    @Test
    fun `parses reply_to_user with embedded JSON object`() {
        val raw = """{"reply_to_user": "Данные: {"name": "Иван"}", "memory_updates": {}}"""
        val result = TaskResponseParser.parse(raw)
        assertNotNull(result)
        assertEquals("""Данные: {"name": "Иван"}""", result!!.replyToUser)
    }

    @Test
    fun `parses reply_to_user with embedded JSON and all other fields intact`() {
        val raw = """{"reply_to_user": "Ответ: {"id": 5}", "memory_updates": {"ticket_id": "5"}, "next_state": "execution"}"""
        val result = TaskResponseParser.parse(raw)
        assertNotNull(result)
        assertEquals("""Ответ: {"id": 5}""", result!!.replyToUser)
        assertEquals("5", result.memoryUpdates["ticket_id"])
        assertEquals("execution", result.nextState)
    }

    @Test
    fun `parses memory_updates value with unescaped quotes`() {
        val raw = """{"reply_to_user": "ok", "memory_updates": {"summary": "Fix "login" bug"}}"""
        val result = TaskResponseParser.parse(raw)
        assertNotNull(result)
        assertEquals("""Fix "login" bug""", result!!.memoryUpdates["summary"])
    }

    // endregion

    // endregion

    // region — code blocks inside reply_to_user (regression: extractJson must not match inner ```)

    @Test
    fun `extractJson returns raw JSON unchanged when response starts with brace`() {
        val json = """{"reply_to_user": "hello", "memory_updates": {}}"""
        assertEquals(json, TaskResponseParser.extractJson(json))
    }

    @Test
    fun `extractJson returns raw JSON unchanged when reply_to_user contains kotlin code block`() {
        val json = "{\"reply_to_user\": \"Example:\\n```kotlin\\nval x = 1\\n```\", \"memory_updates\": {}}"
        assertEquals(json, TaskResponseParser.extractJson(json))
    }

    @Test
    fun `parse succeeds when reply_to_user contains kotlin code block`() {
        val raw = "{\"reply_to_user\": \"Пример:\\n```kotlin\\nval x = 1\\n```\\nГотово.\", \"memory_updates\": {}, \"next_state\": null}"
        val result = TaskResponseParser.parse(raw)
        assertNotNull(result)
        assertTrue(result!!.replyToUser.contains("```kotlin"))
    }

    @Test
    fun `parse succeeds when reply_to_user contains multiple code blocks`() {
        val raw = "{\"reply_to_user\": \"Шаг 1:\\n```kotlin\\nfun a(){}\\n```\\nШаг 2:\\n```kotlin\\nfun b(){}\\n```\", \"memory_updates\": {}}"
        val result = TaskResponseParser.parse(raw)
        assertNotNull(result)
        assertTrue(result!!.replyToUser.contains("fun a()"))
        assertTrue(result.replyToUser.contains("fun b()"))
    }

    @Test
    fun `extractJson still extracts from outer markdown code block when not raw JSON`() {
        val raw = "Вот ответ:\n```json\n{\"reply_to_user\": \"ok\", \"memory_updates\": {}}\n```"
        val extracted = TaskResponseParser.extractJson(raw)
        assertEquals("{\"reply_to_user\": \"ok\", \"memory_updates\": {}}", extracted)
    }

    // endregion

    // region — failure cases

    @Test
    fun `returns null for completely invalid input`() {
        assertNull(TaskResponseParser.parse("это не json"))
    }

    @Test
    fun `returns null for empty string`() {
        assertNull(TaskResponseParser.parse(""))
    }

    @Test
    fun `returns null for plain text without JSON structure`() {
        assertNull(TaskResponseParser.parse("Привет, как дела?"))
    }

    // endregion
}
