package com.example.day.core.core_features.reminder.domain.usecase

import android.util.Log
import com.example.day.core.core_features.agent.domain.AgentRepository
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.workers.concrete.TalkWorker
import com.example.day.core.core_features.chat.domain.ChatRepository
import com.example.day.core.core_features.reminder.domain.ReminderConstants
import com.example.day.core.core_features.reminder.domain.model.Reminder
import com.example.day.core.core_features.reminder.domain.model.ReminderStatus
import com.example.day.core.core_features.reminder.domain.repository.ReminderRepository
import com.example.day.core.core_features.reminder.domain.scheduler.ReminderScheduler
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ExecuteReminderUseCase @Inject constructor(
    private val repository: ReminderRepository,
    private val scheduler: ReminderScheduler,
    private val agentRepository: AgentRepository,
    private val chatRepository: ChatRepository,
    private val talkWorker: TalkWorker
) {
    suspend operator fun invoke(reminderId: String): Result<Unit> = runCatching {
        val reminder = repository.getById(reminderId) ?: error("Reminder not found")
        val chatId = reminder.chatId ?: resolveChatId(reminder)
        val chat = chatRepository.getChatById(chatId) ?: error("Chat not found")

        val prefillMessage = buildPrefillMessage(reminder)
        Log.e(TAG, "WORKER prefill $prefillMessage")
        talkWorker.doWork(prefillMessage, chat, AContextMessage.Role.USER, null)

        val now = System.currentTimeMillis()
        if (reminder.repeatIntervalMinutes != null && reminder.repeatIntervalMinutes > 0) {
            val nextAt = now + reminder.repeatIntervalMinutes * ReminderConstants.MILLIS_IN_MINUTE
            val updated = reminder.copy(
                remindAt = nextAt,
                lastTriggeredAt = now,
                status = ReminderStatus.Pending
            )
            repository.update(updated)
            scheduler.schedule(updated)
        } else {
            val updated = reminder.copy(
                lastTriggeredAt = now,
                status = ReminderStatus.Done
            )
            repository.update(updated)
        }
    }

    private suspend fun resolveChatId(reminder: Reminder): Long {
        val chats = agentRepository.getChatsForAgent(reminder.agentId).first()
        return chats.firstOrNull()?.id
            ?: error("No chat bound to agent ${reminder.agentId}")
    }

    private fun buildPrefillMessage(reminder: Reminder): String {
        val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val remindTime = Instant.ofEpochMilli(reminder.remindAt)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
        val createdTime = Instant.ofEpochMilli(reminder.createdAt)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
        return buildString {
            // append("УВЕДОМЛЕНИЕ: Наступило время $remindTime. Выполни: '${reminder.text}'.")
            // append("Reminder. Сейчас $remindTime и на это время ранее я заводил напоминание (время создания напоминания: $createdTime)")
            // append("Текст напоминания: ${reminder.text}.")
            append(
"""
REMINDER_TRIGGERED
{
  "reminder_id": "${reminder.id}",
  "text": "${reminder.text}"
}
""".trimIndent()
            )
        }
    }

    private companion object {
        const val TAG = "ExecuteReminderUseCase(ktor)"
    }
}
