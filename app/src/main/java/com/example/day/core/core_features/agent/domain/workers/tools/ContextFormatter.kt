package com.example.day.core.core_features.agent.domain.workers.tools

import com.example.day.core.core_features.agent.domain.model.AContext
import com.example.day.core.core_features.agent.domain.model.ContextParameters

/**
 * Interface for formatting context-related messages for display in chat.
 * 
 * Separates presentation/formating logic from business logic (Workers).
 * This allows:
 * - Easy testing of formatting logic without dependencies
 * - Reusing formatting across different workers
 * - Changing output format without affecting domain logic
 */
interface ContextFormatter {
    
    /**
     * Format context settings information for display
     * @param params current context parameters
     * @return formatted string for user display
     */
    fun formatContextInfo(params: ContextParameters): String
    
    /**
     * Format full context content for display
     * @param agentName name of the agent
     * @param context the full context to display
     * @param messageCount total number of messages
     * @return formatted string for user display
     */
    fun formatFullContext(
        agentName: String,
        context: AContext,
        messageCount: Int
    ): String
    
    /**
     * Format settings saved confirmation
     * @param msgLimit message limit setting
     * @param extraLimit extra limit setting
     * @return formatted string for user display
     */
    fun formatSettingsSaved(msgLimit: Int, extraLimit: Int): String
    
    /**
     * Format compression started notification
     * @return formatted string for user display
     */
    fun formatCompressionStarted(): String
    
    /**
     * Format compression result
     * @param oldMessageCount messages before compression
     * @param newMessageCount messages after compression
     * @param oldSummaryLength length of previous summary (0 if none)
     * @param newSummaryLength length of new summary (0 if none)
     * @return formatted string for user display
     */
    fun formatCompressionResult(
        oldMessageCount: Int,
        newMessageCount: Int,
        oldSummaryLength: Int,
        newSummaryLength: Int
    ): String
}
