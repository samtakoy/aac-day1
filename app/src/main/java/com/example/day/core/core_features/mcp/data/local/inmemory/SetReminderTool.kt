package com.example.day.core.core_features.mcp.data.local.inmemory

import com.example.day.core.core_features.agent.domain.AgentRepository
import com.example.day.core.core_features.mcp.domain.McpToolNames
import com.example.day.core.core_features.mcp.domain.model.McpToolCallContext
import com.example.day.core.core_features.reminder.domain.model.Reminder
import com.example.day.core.core_features.reminder.domain.model.ReminderStatus
import com.example.day.core.core_features.reminder.domain.usecase.ScheduleReminderUseCase
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

internal class SetReminderTool @Inject constructor(
    private val scheduleReminderUseCase: ScheduleReminderUseCase,
    private val agentRepository: AgentRepository
) : LocalMcpTool {
        override val name: String = McpToolNames.SET_REMINDER
        override val description: String =
            // "Create a reminder to run a task at a specific time."
            // "Создать напоминание для выполнения задачи в указанное время. Когда напоминание сработает - я смогу выполнить задачу."
            // "Schedule a reminder to execute a task at a specific time. Returns a JSON object containing reminder_id (string) which uniquely identifies the created reminder. After calling this tool successfully, do not call it again for the same request."
            """Schedule a reminder to execute a task at a specific time.

Use this tool when the user asks to perform an action later (for example: "in 10 minutes", "tomorrow at 18:00").

Arguments:
- text: task description to execute when the reminder triggers
- remind_at: ISO-8601 datetime with timezone

Returns JSON:
{
  "reminder_id": string,
  "status": "scheduled"
}

The reminder_id uniquely identifies the created reminder.

If status="scheduled", the reminder was successfully created.

Important:
When the scheduled time arrives, the system will automatically trigger a reminder event and send it to the assistant with the task to execute.

The assistant does not need to monitor time or create another reminder.

Example:

User: "Remind me in 1 minute to say hello"

Assistant tool call:
{
  "text": "say hello",
  "remind_at": "2026-03-12T12:01:00+02:00"
}

Respond to User: "Напоминание запланировано."

System event example when reminder triggers:

REMINDER_TRIGGERED
{
  "reminder_id": "rem_84291",
  "text": "say hello"
}

""".trimIndent()

        override val inputSchema: JsonObject = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put(
                "properties",
                buildJsonObject {
                    put(
                        "text",
                        buildJsonObject {
                            put("type", JsonPrimitive("string"))
                            put("description", JsonPrimitive("Task description to execute at reminder time"))
                        }
                    )
                    put(
                        "remind_at",
                        buildJsonObject {
                            put("type", JsonPrimitive("string"))
                            put("description", JsonPrimitive("ISO-8601 datetime with timezone when the reminder should trigger. Example: 2026-03-11T12:50:00+03:00"))
                        }
                    )
                }
            )
            put(
                "required",
                kotlinx.serialization.json.JsonArray(
                    listOf(JsonPrimitive("text"), JsonPrimitive("remind_at"))
                )
            )
        }

        override suspend fun call(
            arguments: JsonObject,
            context: McpToolCallContext?
        ): Result<String> = runCatching {
            val agentId = context?.agentId ?: error("agentId is required")
            val text = arguments["text"]?.jsonPrimitive?.content?.trim().orEmpty()
            if (text.isBlank()) error("text is required")

            val remindAt = parseRemindAt(arguments)
            val repeatMinutes = arguments["repeat_interval_minutes"]?.jsonPrimitive?.longOrNull
            val chatId = resolveChatId(agentId) ?: error("No chat bound to agent $agentId")

            val createdAt = System.currentTimeMillis()
            val reminder = Reminder(
                id = UUID.randomUUID().toString(),
                agentId = agentId,
                chatId = chatId,
                text = text,
                remindAt = remindAt,
                createdAt = createdAt,
                status = ReminderStatus.Pending,
                repeatIntervalMinutes = repeatMinutes,
                lastTriggeredAt = null
            )
            scheduleReminderUseCase(reminder)
            val timeText = Instant.ofEpochMilli(remindAt)
                .atZone(ZoneId.systemDefault())
                .format(DISPLAY_TIME_FORMAT)
            val createdText = Instant.ofEpochMilli(createdAt)
                .atZone(ZoneId.systemDefault())
                .format(DISPLAY_TIME_FORMAT)
            "Reminder scheduled for $timeText (created at $createdText)"
            buildJsonObject {
                put("reminder_id", JsonPrimitive(reminder.id))
                put("status", JsonPrimitive("scheduled"))
                put("remind_at", JsonPrimitive(remindAt))
                put("text", JsonPrimitive(text))
            }.toString()
        }

    private suspend fun resolveChatId(agentId: Long): Long? {
        val chats = agentRepository.getChatsForAgent(agentId).first()
        return chats.firstOrNull()?.id
    }

        private fun parseRemindAt(arguments: JsonObject): Long {
            //val epochMs = arguments["remind_at_epoch_ms"]?.jsonPrimitive?.longOrNull
            //if (epochMs != null) return epochMs
            val raw = arguments["remind_at"]?.jsonPrimitive?.content?.trim().orEmpty()
            if (raw.isBlank()) error("remind_at is required")

            return runCatching { Instant.parse(raw).toEpochMilli() }
                .recoverCatching { OffsetDateTime.parse(raw).toInstant().toEpochMilli() }
                .recoverCatching {
                    LocalDateTime.parse(raw, LOCAL_DATETIME_FORMAT)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                }
                .recoverCatching {
                    LocalDateTime.parse(raw, LOCAL_DATETIME_ALT_FORMAT)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                }
                .getOrElse { throw IllegalArgumentException("Invalid remind_at format: $raw") }
        }

    private companion object {
        val DISPLAY_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val LOCAL_DATETIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val LOCAL_DATETIME_ALT_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
    }
}
