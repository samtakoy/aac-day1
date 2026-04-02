package com.example.day.core.core_features.agent.domain.workers.task.util

import org.junit.Assert.assertEquals
import org.junit.Test

class JsonSanitizerTest {

    // region — valid JSON, no changes expected

    @Test
    fun `valid JSON passes through unchanged`() {
        val input = """{"reply_to_user": "Привет!", "next_state": "done"}"""
        assertEquals(input, sanitizeJsonStringValues(input))
    }

    @Test
    fun `nested object with valid JSON passes through unchanged`() {
        val input = """{"reply_to_user": "ok", "memory_updates": {"key": "value"}, "next_state": "done"}"""
        assertEquals(input, sanitizeJsonStringValues(input))
    }

    @Test
    fun `empty string value passes through unchanged`() {
        val input = """{"reply_to_user": ""}"""
        assertEquals(input, sanitizeJsonStringValues(input))
    }

    @Test
    fun `already escaped quotes are not double-escaped`() {
        val input = """{"reply_to_user": "He said \"hello\""}"""
        assertEquals(input, sanitizeJsonStringValues(input))
    }

    @Test
    fun `already escaped backslash is not mangled`() {
        val input = """{"reply_to_user": "path: C:\\Users\\foo"}"""
        assertEquals(input, sanitizeJsonStringValues(input))
    }

    // endregion

    // region — unescaped control characters inside string values

    @Test
    fun `literal newline in string value gets escaped`() {
        val input    = "{\"reply_to_user\": \"Line 1\nLine 2\"}"
        val expected = "{\"reply_to_user\": \"Line 1\\nLine 2\"}"
        assertEquals(expected, sanitizeJsonStringValues(input))
    }

    @Test
    fun `literal carriage return in string value gets escaped`() {
        val input    = "{\"reply_to_user\": \"Line 1\rLine 2\"}"
        val expected = "{\"reply_to_user\": \"Line 1\\rLine 2\"}"
        assertEquals(expected, sanitizeJsonStringValues(input))
    }

    @Test
    fun `literal tab in string value gets escaped`() {
        val input    = "{\"reply_to_user\": \"col1\tcol2\"}"
        val expected = "{\"reply_to_user\": \"col1\\tcol2\"}"
        assertEquals(expected, sanitizeJsonStringValues(input))
    }

    @Test
    fun `crlf in string value gets escaped`() {
        val input    = "{\"reply_to_user\": \"Line 1\r\nLine 2\"}"
        val expected = "{\"reply_to_user\": \"Line 1\\r\\nLine 2\"}"
        assertEquals(expected, sanitizeJsonStringValues(input))
    }

    // endregion

    // region — unescaped double-quotes inside string values

    @Test
    fun `unescaped quotes in string value get escaped`() {
        val input    = """{"reply_to_user": "Он сказал "привет" ей"}"""
        val expected = """{"reply_to_user": "Он сказал \"привет\" ей"}"""
        assertEquals(expected, sanitizeJsonStringValues(input))
    }

    @Test
    fun `multiple unescaped quote pairs in same value`() {
        val input    = """{"reply_to_user": "Нажмите "ОК" или "Отмена""}"""
        val expected = """{"reply_to_user": "Нажмите \"ОК\" или \"Отмена\""}"""
        assertEquals(expected, sanitizeJsonStringValues(input))
    }

    @Test
    fun `unescaped quotes in memory_updates value get escaped`() {
        val input    = """{"reply_to_user": "ok", "memory_updates": {"summary": "Fix "button" issue"}}"""
        val expected = """{"reply_to_user": "ok", "memory_updates": {"summary": "Fix \"button\" issue"}}"""
        assertEquals(expected, sanitizeJsonStringValues(input))
    }

    // endregion

    // region — embedded JSON inside string values (main LLM pattern)

    @Test
    fun `embedded JSON object in string value - all inner quotes escaped`() {
        val input    = """{"reply_to_user": "Данные: {"name": "Иван"}", "memory_updates": {}}"""
        val expected = """{"reply_to_user": "Данные: {\"name\": \"Иван\"}", "memory_updates": {}}"""
        assertEquals(expected, sanitizeJsonStringValues(input))
    }

    @Test
    fun `deeply nested embedded JSON - all inner quotes escaped`() {
        val input    = """{"reply_to_user": "Result: {"data": {"status": "ok"}}"}"""
        val expected = """{"reply_to_user": "Result: {\"data\": {\"status\": \"ok\"}}"}"""
        assertEquals(expected, sanitizeJsonStringValues(input))
    }

    @Test
    fun `embedded JSON array in string value - all inner quotes escaped`() {
        val input    = """{"reply_to_user": "Items: ["first", "second"]"}"""
        val expected = """{"reply_to_user": "Items: [\"first\", \"second\"]"}"""
        assertEquals(expected, sanitizeJsonStringValues(input))
    }

    @Test
    fun `literal newline before embedded JSON - both sanitized`() {
        val input    = """{"reply_to_user": "Ответ:""" + "\n" + """{"key": "val"}"}"""
        val expected = """{"reply_to_user": "Ответ:\n{\"key\": \"val\"}"}"""
        assertEquals(expected, sanitizeJsonStringValues(input))
    }

    @Test
    fun `embedded JSON with int value - no false quote escaping on number`() {
        val input    = """{"reply_to_user": "Ticket: {"id": 42}"}"""
        val expected = """{"reply_to_user": "Ticket: {\"id\": 42}"}"""
        assertEquals(expected, sanitizeJsonStringValues(input))
    }

    // endregion
}
