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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.day.app.di.LocalAppComponent
import com.example.day.core.core_features.chat.domain.model.ChatType
import com.example.day.core.ui.uikit.chat.DarkChatUiColors
import com.example.day.core.ui.uikit.chat.LocalChatColors
import com.example.day.features.chats.impl.ui.viewmodel.ChatsViewModel
import com.example.day.features.chats.impl.ui.viewmodel.ChatsViewModelImpl
import kotlinx.coroutines.launch

@Composable
internal fun ChatsScreen(
    viewModel: ChatsViewModelImpl,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state = viewModel.getStateAsFlow().collectAsStateWithLifecycle().value

    CompositionLocalProvider(
        LocalChatColors provides DarkChatUiColors
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

    // Create pager state at the top level to share between chips and pager
    val pagerState = rememberPagerState(initialPage = 0) { state.chips.size }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier) {
        // TopAppBar with back button
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
                        onClick = {
                            onChipClick(chat.id, index)
                        },
                        label = { Text(text = chat.title) }
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
