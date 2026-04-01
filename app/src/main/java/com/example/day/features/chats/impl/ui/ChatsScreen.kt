package com.example.day.features.chats.impl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.day.app.di.LocalAppComponent
import com.example.day.core.core_features.chat.domain.model.ChatType
import com.example.day.core.ui.uikit.chat.DarkChatColorScheme
import com.example.day.core.ui.uikit.chat.LocalChatColorScheme
import com.example.day.features.chats.impl.ui.viewmodel.ChatsViewModel
import com.example.day.features.chats.impl.ui.viewmodel.ChatsViewModelImpl
import kotlinx.coroutines.launch

@Composable
internal fun ChatsScreen(
    viewModel: ChatsViewModelImpl,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.getStateAsFlow().collectAsStateWithLifecycle()

    CompositionLocalProvider(
        LocalChatColorScheme provides DarkChatColorScheme
    ) {
        ChatsScreenInternal(
            state = state,
            onEvent = viewModel::onEvent,
            onNavigateBack = onNavigateBack,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatsScreenInternal(
    state: ChatsViewModel.State,
    onEvent: (ChatsViewModel.Event) -> Unit,
    onNavigateBack: (() -> Unit)?,
    modifier: Modifier
) {
    val appComponent = LocalAppComponent.current
    val agentsChatEntry = appComponent.getAgentsConsoleFeatureEntry()
    val historyChatEntry = appComponent.getConsoleFeatureEntry()
    val plannerChatEntry = appComponent.getPlannerConsoleFeatureEntry()
    val ragChatEntry = appComponent.getRagConsoleFeatureEntry()
    val assistantChatEntry = appComponent.getAssistantConsoleFeatureEntry()
    val supportChatEntry = appComponent.getSupportConsoleFeatureEntry()
    val userSettingsEntry = appComponent.getUserSettingsFeatureEntry()

    // Create pager state at the top level to share between chips and pager
    val pagerState = rememberPagerState(initialPage = 0) { state.chips.size }
    val scope = rememberCoroutineScope()
    var showUserSettings by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(modifier = modifier) {
        // TopAppBar with back button and profile icon
        TopAppBar(
            title = { Text("Чаты") },
            navigationIcon = {
                if (onNavigateBack != null) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            },
            actions = {
                IconButton(onClick = { showUserSettings = true }) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Профиль пользователя"
                    )
                }
            }
        )

        // Chips row with add button - always visible at top
        ChipsRow(
            chips = state.chips,
            pagerState = pagerState,
            scope = scope,
            onChipClick = { chatId, index ->
                scope.launch {
                    pagerState.animateScrollToPage(index)
                }
                onEvent(ChatsViewModel.Event.ChatClick(chatId))
            },
            onAddClick = {
                onEvent(ChatsViewModel.Event.AddChatClick)
            }
        )

        // Content area - Pager when chips exist
        if (state.chips.isNotEmpty()) {
            Box(modifier = Modifier.weight(1f)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val chip = state.chips.getOrNull(pageIndex)
                    if (chip != null) {
                        when (chip.chatType) {
                            ChatType.SIMPLE_HISTORY -> {
                                historyChatEntry.EntryPoint(chatId = chip.id, modifier = Modifier.fillMaxSize())
                            }
                            ChatType.AGENT_COMMANDS -> {
                                agentsChatEntry.EntryPoint(chatId = chip.id, modifier = Modifier.fillMaxSize())
                            }
                            ChatType.PLANNER -> {
                                // PLANNER groups use PlannerTalkDelegate with PlannerWorker
                                plannerChatEntry.EntryPoint(chatId = chip.id, modifier = Modifier.fillMaxSize())
                            }
                            ChatType.RAG_CONTEXT -> {
                                // RAG_CONTEXT groups use RagTalkDelegate with RagWorker (AutoRag + ContextSummaryStrategy)
                                ragChatEntry.EntryPoint(chatId = chip.id, modifier = Modifier.fillMaxSize())
                            }
                            ChatType.ASSISTANT -> {
                                assistantChatEntry.EntryPoint(chatId = chip.id, modifier = Modifier.fillMaxSize())
                            }
                            ChatType.SUPPORT -> {
                                supportChatEntry.EntryPoint(chatId = chip.id, modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }

            // Detect page changes - send SwipeTo event
            LaunchedEffect(pagerState.currentPage) {
                val pageIndex = pagerState.currentPage
                val chatId = state.chips.getOrNull(pageIndex)?.id
                chatId?.let { onEvent(ChatsViewModel.Event.SwipeTo(it)) }
            }
        }
    }

    if (showUserSettings) {
        ModalBottomSheet(
            onDismissRequest = { showUserSettings = false },
            sheetState = sheetState
        ) {
            userSettingsEntry.EntryPoint(
                modifier = Modifier.fillMaxWidth(),
                onDismiss = { showUserSettings = false }
            )
        }
    }
}

@Composable
private fun ChipsRow(
    chips: List<ChatsViewModel.State.Chat>,
    pagerState: PagerState,
    scope: kotlinx.coroutines.CoroutineScope,
    onChipClick: (Long, Int) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (chips.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                itemsIndexed(
                    items = chips,
                    key = { _, chat -> chat.id }
                ) { index, chat ->
                    FilterChip(
                        selected = pagerState.currentPage == index,
                        onClick = { onChipClick(chat.id, index) },
                        label = {
                            val label = when {
                                chat.isCompleted -> "✓ ${chat.title}"
                                chat.isStageChat -> "↳ ${chat.title}"
                                else -> chat.title
                            }
                            Text(
                                text = label,
                                style = if (chat.isStageChat)
                                    MaterialTheme.typography.labelSmall
                                else
                                    MaterialTheme.typography.bodyMedium
                            )
                        },
                        colors = when {
                            chat.isCompleted -> FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                selectedContainerColor = MaterialTheme.colorScheme.tertiary
                            )
                            chat.isStageChat -> FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedContainerColor = MaterialTheme.colorScheme.secondary
                            )
                            else -> FilterChipDefaults.filterChipColors()
                        }
                    )
                }
            }
        } else {
            // No chats - show placeholder
            Text(
                text = "Create a new chat to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            )
        }

        // Add new chat button - always visible
        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier
                .padding(end = 16.dp, start = 8.dp)
                .size(40.dp),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add chat"
            )
        }
    }
}
