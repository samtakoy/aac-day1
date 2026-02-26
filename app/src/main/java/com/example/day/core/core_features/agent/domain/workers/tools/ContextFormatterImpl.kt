package com.example.day.core.core_features.agent.domain.workers.tools

import com.example.day.core.core_features.agent.domain.model.AContext
import com.example.day.core.core_features.agent.domain.model.AContext.Companion.NO_SUMMARY_LIMIT
import com.example.day.core.core_features.agent.domain.model.ContextParameters
import com.example.day.core.core_features.agent.domain.model.Role
import com.example.day.core.core_features.agent.domain.model.summarization.SummarizationEnabledState
import javax.inject.Inject

/**
 * Implementation of [ContextFormatter] for formatting context-related messages.
 * 
 * This class contains all presentation/formating logic that was previously
 * embedded in TalkWorker, extracted for better separation of concerns.
 */
internal class ContextFormatterImpl @Inject constructor() : ContextFormatter {
    
    companion object {
        private const val MSG_PARAM = "msg"
        private const val EXTRA_PARAM = "extra"
    }
    
    override fun formatContextInfo(params: ContextParameters): String {
        val compressionEnabled = params.msgLimit < NO_SUMMARY_LIMIT
        return buildString {
            appendLine("📊 Настройки контекста:")
            appendLine("• $MSG_PARAM: ${params.msgLimit} (сообщений хранится как есть)")
            appendLine("• $EXTRA_PARAM: ${params.extraLimit} (порог для сжатия)")
            appendLine("• strategy: ${params.strategy}")
            appendLine("• Сжатие: ${if (compressionEnabled) "✅ Включено" else "❌ Отключено"}")
            
            if (compressionEnabled) {
                val threshold = params.msgLimit + params.extraLimit
                appendLine("• Сжатие произойдёт при накоплении $threshold сообщений")
            }
        }
    }
    
    override fun formatFullContext(
        agentName: String,
        context: AContext,
        messageCount: Int
    ): String {
        val state = context.summarizationState
        val summary = state.retrieveSummary()
        val msgLimit = if (state is SummarizationEnabledState) state.msgLimit else Int.MAX_VALUE
        val threshold = state.getCompressionThreshold()
        
        return buildContextInfoString(
            agentName = agentName,
            context = context,
            summary = summary,
            msgLimit = msgLimit,
            threshold = threshold,
            messageCount = messageCount
        )
    }
    
    override fun formatSettingsSaved(msgLimit: Int, extraLimit: Int): String {
        return buildString {
            appendLine("✅ Настройки сохранены:")
            appendLine("• $MSG_PARAM: $msgLimit")
            appendLine("• $EXTRA_PARAM: $extraLimit")
            appendLine("• Сжатие включено - будет работать при накоплении ${msgLimit + extraLimit} сообщений")
        }
    }
    
    override fun formatCompressionStarted(): String {
        return buildString {
            appendLine("⚠️ Лимит сообщений превышен - начинаем сжатие контекста")
        }
    }
    
    override fun formatCompressionResult(
        oldMessageCount: Int,
        newMessageCount: Int,
        oldSummaryLength: Int,
        newSummaryLength: Int
    ): String {
        return buildString {
            appendLine("🔄 Контекст сжат:")
            appendLine("• Сообщений стало: $newMessageCount")
            appendLine("• Предыдущий summary: $oldSummaryLength символов")
            appendLine("• Новый summary: $newSummaryLength символов")
        }
    }
    
    /**
     * Build the full context info string (extracted from TalkWorker)
     */
    private fun buildContextInfoString(
        agentName: String,
        context: AContext,
        summary: String?,
        msgLimit: Int,
        threshold: Int,
        messageCount: Int
    ): String = buildString {
        val systemPrompt = context.systemPrompt
        val messagesUntilCompression = (threshold - messageCount).coerceAtLeast(0)
        appendLine("📋 Полное содержимое контекста:")
        appendLine("┌─ Agent: ${agentName}")
        appendLine("├─ System Prompt (${systemPrompt.length} симв.):")
        if (systemPrompt.isNotBlank()) {
            appendLine("│ ${systemPrompt.take(200)}${if (systemPrompt.length > 200) "..." else ""}")
        } else {
            appendLine("│ (пусто)")
        }
        appendLine("├─ Сообщений в контексте: ${context.messages.size}")

        if (context.messages.isNotEmpty()) {
            context.messages.forEachIndexed { index, msg ->
                val role = if (msg.role == Role.USER) "👤 User" else "🤖 Assistant"
                val content = msg.content.take(100).replace("\n", " ")
                appendLine("│ ${index + 1}. $role: $content${if (msg.content.length > 100) "..." else ""}")
            }
        }

        // Показать summary если есть
        if (!summary.isNullOrBlank()) {
            appendLine("├─ Summary (${summary.length} симв.):")
            appendLine("│ $summary\n")
        } else {
            appendLine("├─ Summary (0 симв.)")
        }

        // Параметры сжатия и сколько осталось до сжатия
        appendLine("├─ Параметры сжатия:")
        if (msgLimit < Int.MAX_VALUE) {
            appendLine("│ • $MSG_PARAM: $msgLimit (сообщений хранится как есть)")
            appendLine("│ • threshold: $threshold (порог для сжатия)")
            appendLine("│ • До сжатия осталось: $messagesUntilCompression сообщения${if (messagesUntilCompression == 0) " ⚠️ СЖАТИЕ ГОТОВО!" else ""}")
        } else {
            appendLine("│ • Сжатие отключено ($MSG_PARAM=∞)")
        }
        appendLine("└─")
    }
}
