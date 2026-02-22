package com.example.day.features.group_choice.impl.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.day.core.core_features.chat.domain.model.ChatGroup
import com.example.day.core.core_features.chat.domain.model.ChatGroupColors

@Composable
internal fun GroupsGrid(
    groups: List<ChatGroup>,
    onGroupClick: (ChatGroup) -> Unit,
    onEditClick: (ChatGroup) -> Unit,
    onDeleteClick: (ChatGroup) -> Unit,
    modifier: Modifier = Modifier
) {
    // TODO адаптивно по MAterial Design guide
    // Используем фиксированное количество колонок (2) для простоты
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(
            items = groups,
            key = { it.id }
        ) { group ->
            val color = ChatGroupColors.getColor(group.colorIndex)
            
            GroupCard(
                group = group,
                color = color,
                onClick = { onGroupClick(group) },
                onEdit = { onEditClick(group) },
                onDelete = { onDeleteClick(group) }
            )
        }
    }
}
