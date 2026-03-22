package com.example.day.core.feature_entries

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.example.day.features.chats.api.ChatsFeatureEntry
import com.example.day.features.console.api.AgentsConsoleFeatureEntry
import com.example.day.features.console.api.ConsoleFeatureEntry
import com.example.day.features.console.api.PlannerConsoleFeatureEntry
import com.example.day.features.console.api.RagConsoleFeatureEntry
import com.example.day.features.group_choice.api.GroupChoiceFeatureEntry
import com.example.day.features.mcp_settings.api.McpSettingsFeatureEntry
import com.example.day.features.user_settings.api.UserSettingsFeatureEntry

@Immutable
interface FeatureEntryProvider {
    @Stable
    fun getChatFeatureEntry(): ChatsFeatureEntry
    @Stable
    fun getGroupChoiceFeatureEntry(): GroupChoiceFeatureEntry
    @Stable
    fun getAgentsConsoleFeatureEntry(): AgentsConsoleFeatureEntry
    @Stable
    fun getConsoleFeatureEntry(): ConsoleFeatureEntry
    @Stable
    fun getPlannerConsoleFeatureEntry(): PlannerConsoleFeatureEntry
    @Stable
    fun getUserSettingsFeatureEntry(): UserSettingsFeatureEntry
    @Stable
    fun getMcpSettingsFeatureEntry(): McpSettingsFeatureEntry
    @Stable
    fun getRagConsoleFeatureEntry(): RagConsoleFeatureEntry
}