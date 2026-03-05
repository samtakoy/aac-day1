package com.example.day.features.console.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.day.core.ui.uikit.components.ltm.LongTermFactUiItem
import com.example.day.core.ui.uikit.components.ltm.LongTermFactsListView
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

private val tabs = listOf("Краткосрочная", "Рабочая", "Долгосрочная")

@Composable
fun MemoryInspectorView(
    uiModel: MemoryInspectorUiModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = modifier.fillMaxWidth()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
        ) {
            when (selectedTab) {
                0 -> ShortTermContent(uiModel.shortTermMessages)
                1 -> WorkingMemoryContent(uiModel.workingMemory)
                2 -> LongTermFactsListView(
                    facts = uiModel.longTermFacts.map {
                        LongTermFactUiItem(it.id, it.memoryKey, it.category, it.fact, it.updatedAt)
                    }.toImmutableList(),
                    showPromptPreview = true
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Краткосрочная память (STM)
// ─────────────────────────────────────────────────────────

@Composable
private fun ShortTermContent(messages: ImmutableList<ShortTermMemoryItem>) {
    Column(modifier = Modifier.fillMaxSize()) {
        MemoryInfoBanner("Последние сообщения диалога. Агент видит их как историю разговора в текущем чате.")

        if (messages.isEmpty()) {
            EmptyPlaceholder("Сообщений пока нет")
            return@Column
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            reverseLayout = true
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            items(messages.reversed()) { msg ->
                val isUser = msg.role == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isUser) 12.dp else 2.dp,
                            bottomEnd = if (isUser) 2.dp else 12.dp
                        ),
                        color = if (isUser)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(
                                text = if (isUser) "Пользователь" else "Агент",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isUser)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = msg.content,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 5,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Рабочая память (Working Memory)
// ─────────────────────────────────────────────────────────

@Composable
private fun WorkingMemoryContent(workingMemory: String?) {
    Column(modifier = Modifier.fillMaxSize()) {
        MemoryInfoBanner("Накопленный контекст проекта. Пополняется при завершении каждого этапа и передаётся агенту в начале каждого нового разговора в этом чате.")

        if (workingMemory.isNullOrBlank()) {
            EmptyPlaceholder("Контекст пуст.\nПоявится после завершения первого этапа командой COMPLETE_STAGE.")
            return@Column
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Содержимое рабочей памяти",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                Spacer(Modifier.height(8.dp))
            }
            item {
                Text(
                    text = workingMemory,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Вспомогательные компоненты
// ─────────────────────────────────────────────────────────

@Composable
private fun MemoryInfoBanner(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
private fun EmptyPlaceholder(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(24.dp)
        )
    }
}
