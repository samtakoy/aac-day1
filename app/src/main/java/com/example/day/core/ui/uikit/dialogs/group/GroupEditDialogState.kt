package com.example.day.core.ui.uikit.dialogs.group

import androidx.compose.runtime.Immutable
import com.example.day.core.core_features.chat.domain.model.ChatType

@Immutable
data class GroupEditDialogState(
    val title: String,
    val selectedType: ChatType,
    val isCreateMode: Boolean
)
