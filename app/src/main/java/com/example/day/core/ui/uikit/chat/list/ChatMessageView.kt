package com.example.day.core.ui.uikit.chat.list

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.day.core.ui.theme.Day1Theme
import com.example.day.core.ui.uikit.chat.ChatColorScheme
import com.example.day.core.ui.uikit.chat.DarkChatColorScheme
import com.example.day.core.ui.uikit.chat.LightChatColorScheme
import com.example.day.core.ui.uikit.chat.LocalChatColorScheme
import com.example.day.core.ui.uikit.chat.list.model.ChatMessageUiModel
import com.example.day.core.ui.uikit.chat.list.model.ChatMessageUiType
import com.example.day.core.ui.uikit.chat.list.model.UiMessageStatus

/**
 * Composable для отображения одного сообщения чата
 * 
 * Поддерживает 4 типа сообщений:
 * - [ChatMessageUiType.User]: аватар справа, пузырёк слева
 * - [ChatMessageUiType.Bot]: аватар слева, пузырёк справа
 * - [ChatMessageUiType.Info]: аватар слева, текст с expand/collapse
 * - [ChatMessageUiType.Buttons]: кнопки действий
 * 
 * @param item Модель сообщения
 * @param modifier Модификатор
 * @param colors Схема цветов
 * @param onInfoMessageExpand Callback изменения состояния раскрытия
 * @param onMessageButtonClick Callback клика кнопки
 */
@Composable
fun ChatMessageView(
    item: ChatMessageUiModel,
    modifier: Modifier = Modifier,
    colors: ChatColorScheme = LocalChatColorScheme.current,
    onInfoMessageExpand: (id: Long, isExpanded: Boolean) -> Unit = { _, _ -> },
    onMessageButtonClick: (messageId: Long, actionId: String) -> Unit = { _, _ -> }
) {
    when (item.userType) {
        ChatMessageUiType.User -> UserMessageContent(
            item = item,
            colors = colors,
            modifier = modifier
        )

        ChatMessageUiType.Bot -> BotMessageContent(
            item = item,
            colors = colors,
            modifier = modifier
        )

        ChatMessageUiType.Info -> InfoMessageContent(
            item = item,
            colors = colors,
            onInfoMessageExpand = onInfoMessageExpand,
            modifier = modifier
        )

        ChatMessageUiType.Buttons -> ButtonsMessageContent(
            item = item,
            colors = colors,
            onInfoMessageExpand = onInfoMessageExpand,
            onMessageButtonClick = onMessageButtonClick,
            modifier = modifier.padding(bottom = 8.dp)
        )

        ChatMessageUiType.Title -> TitleMessageContent(
            item = item,
            colors = colors,
            onInfoMessageExpand = onInfoMessageExpand,
            modifier = modifier.padding(top = 8.dp)
        )
    }
}

/**
 * Контент пользовательского сообщения
 */
@Composable
private fun UserMessageContent(
    item: ChatMessageUiModel,
    colors: ChatColorScheme,
    modifier: Modifier = Modifier
) {
    MessageScaffold(
        leading = {
            ChatAvatar(
                url = item.avatarUrl.orEmpty(),
                placeholderText = "U",
                backgroundColor = colors.userAvatarBackground,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        },
        content = {
            ChatMessageBubble(
                text = item.text,
                bubbleType = BubbleType.User,
                textColor = colors.userText,
                bubbleColor = colors.userBubble,
                modifier = Modifier.padding(end = 4.dp)
            )
        },
        trailing = {
            MessageCopyButton(
                text = item.text,
                contentDescription = "Copy user message"
            )
        },
        supporting = {
            MessageStatusIndicator(
                status = item.status,
                sendingColor = colors.sendingStatus,
                deliveredColor = colors.deliveredStatus,
                viewedColor = colors.viewedStatus
            )
        },
        isUserMessage = true,
        modifier = modifier
    )
}

/**
 * Контент сообщения бота
 */
@Composable
private fun BotMessageContent(
    item: ChatMessageUiModel,
    colors: ChatColorScheme,
    modifier: Modifier = Modifier
) {
    MessageScaffold(
        leading = {
            ChatAvatar(
                url = item.avatarUrl.orEmpty(),
                placeholderText = "B",
                backgroundColor = colors.botAvatarBackground,
                modifier = Modifier.padding(end = 8.dp, top = 4.dp)
            )
        },
        content = {
            ChatMessageBubble(
                text = item.text,
                bubbleType = BubbleType.Bot,
                textColor = colors.botText,
                bubbleColor = colors.botBubble,
                modifier = Modifier.padding(start = 4.dp)
            )
        },
        trailing = {
            MessageCopyButton(
                text = item.text,
                contentDescription = "Copy bot message"
            )
        },
        isUserMessage = false,
        modifier = modifier
    )
}

/**
 * Контент информационного сообщения
 */
@Composable
private fun InfoMessageContent(
    item: ChatMessageUiModel,
    colors: ChatColorScheme,
    onInfoMessageExpand: (id: Long, isExpanded: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val textSize = if (item.isExpanded) colors.infoExpandedTextSize else colors.infoCollapsedTextSize
    val horizontalPadding = if (item.isExpanded) colors.infoExpandedPadding else colors.infoCollapsedPadding
    val verticalPadding = if (item.isExpanded) 8.dp else 6.dp
    
    MessageScaffold(
        leading = {
            if (item.isExpanded) {
                ChatAvatar(
                    url = item.avatarUrl.orEmpty(),
                    placeholderText = "I",
                    backgroundColor = colors.infoAvatarBackground,
                    modifier = Modifier.padding(end = 8.dp, top = 4.dp)
                )
            }
        },
        content = {
            ChatMessageBubble(
                text = item.text,
                bubbleType = BubbleType.Info,
                textColor = colors.infoText,
                bubbleColor = colors.infoBubble,
                isExpanded = item.isExpanded,
                onExpandChange = { isExpanded ->
                    onInfoMessageExpand(item.id, isExpanded)
                },
                textSize = textSize,
                horizontalPadding = horizontalPadding,
                verticalPadding = verticalPadding,
                modifier = Modifier.padding(start = 4.dp)
            )
        },
        trailing = {
            MessageCopyButton(
                text = item.text,
                contentDescription = "Copy info message"
            )
        },
        isUserMessage = false,
        modifier = modifier
    )
}

/**
 * Контент сообщения-заголовка (Title)
 */
@Composable
private fun TitleMessageContent(
    item: ChatMessageUiModel,
    colors: ChatColorScheme,
    onInfoMessageExpand: (id: Long, isExpanded: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val textSize = if (item.isExpanded) colors.titleExpandedTextSize else colors.titleCollapsedTextSize
    val horizontalPadding = if (item.isExpanded) colors.titleExpandedPadding else colors.titleCollapsedPadding
    val verticalPadding = if (item.isExpanded) 10.dp else 8.dp

    MessageScaffold(
        leading = {
            if (item.isExpanded) {
                ChatAvatar(
                    url = item.avatarUrl.orEmpty(),
                    placeholderText = "T",
                    backgroundColor = colors.titleAvatarBackground,
                    modifier = Modifier.padding(end = 8.dp, top = 4.dp)
                )
            }
        },
        content = {
            ChatMessageBubble(
                text = item.text,
                bubbleType = BubbleType.Title,
                textColor = colors.titleText,
                bubbleColor = colors.titleBubble,
                isExpanded = item.isExpanded,
                onExpandChange = { isExpanded ->
                    onInfoMessageExpand(item.id, isExpanded)
                },
                textSize = textSize,
                horizontalPadding = horizontalPadding,
                verticalPadding = verticalPadding,
                modifier = Modifier.padding(start = 4.dp)
            )
        },
        trailing = {
            MessageCopyButton(
                text = item.text,
                contentDescription = "Copy title message"
            )
        },
        isUserMessage = false,
        modifier = modifier
    )
}

/**
 * Контент сообщения с кнопками
 */
@Composable
private fun ButtonsMessageContent(
    item: ChatMessageUiModel,
    colors: ChatColorScheme,
    onInfoMessageExpand: (id: Long, isExpanded: Boolean) -> Unit,
    onMessageButtonClick: (messageId: Long, actionId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val textSize = if (item.isExpanded) 14.sp else 12.sp
    val horizontalPadding = if (item.isExpanded) 12.dp else 8.dp
    val verticalPadding = if (item.isExpanded) 8.dp else 6.dp
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        // Текст (если есть)
        if (item.text.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.Top
            ) {
                ChatMessageBubble(
                    text = item.text,
                    bubbleType = BubbleType.Buttons,
                    textColor = colors.buttonsBubbleText,
                    bubbleColor = colors.buttonsBubbleBackground.copy(alpha = 0.15f),
                    isExpanded = item.isExpanded,
                    onExpandChange = { isExpanded ->
                        onInfoMessageExpand(item.id, isExpanded)
                    },
                    textSize = textSize,
                    horizontalPadding = horizontalPadding,
                    verticalPadding = verticalPadding,
                    modifier = Modifier.weight(1f, fill = false)
                )
                
                MessageCopyButton(
                    text = item.text,
                    contentDescription = "Copy buttons message"
                )
            }
        }
        
        // Кнопки
        if (item.buttons != null && item.buttons.list.isNotEmpty()) {
            ButtonGroup(
                buttons = item.buttons.list,
                onButtonClick = { actionId ->
                    onMessageButtonClick(item.id, actionId)
                },
                isEnabled = item.buttons.isEnabled,
                primaryBackground = colors.buttonPrimaryBackground,
                primaryText = colors.buttonPrimaryText,
                secondaryBackground = colors.buttonSecondaryBackground,
                secondaryText = colors.buttonSecondaryText,
                cancelBackground = colors.buttonCancelBackground,
                cancelText = colors.buttonCancelText,
                modifier = Modifier.padding(top = if (item.text.isNotEmpty()) 8.dp else 0.dp)
            )
        }
    }
}

// region Previews

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun ChatMessageViewPreview() = Day1Theme {
    Surface {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
        ) {
            // User message - short
            ChatMessageView(
                item = ChatMessageUiModel(
                    id = 1,
                    text = "Привет!",
                    userType = ChatMessageUiType.User,
                    status = UiMessageStatus.Delivered,
                    avatarUrl = "https://free-png.ru/wp-content/uploads/2021/07/free-png.ru-30.png"
                )
            )

            // User message - long
            ChatMessageView(
                item = ChatMessageUiModel(
                    id = 2,
                    text = "Это длинное сообщение от пользователя, которое демонстрирует, как выглядит сообщение с большим количеством текста.",
                    userType = ChatMessageUiType.User,
                    status = UiMessageStatus.Viewed,
                    avatarUrl = "https://free-png.ru/wp-content/uploads/2021/07/free-png.ru-30.png"
                )
            )

            // Bot message - short
            ChatMessageView(
                item = ChatMessageUiModel(
                    id = 3,
                    text = "Привет! Я бот.",
                    userType = ChatMessageUiType.Bot,
                    status = UiMessageStatus.Delivered,
                    avatarUrl = "https://cdnstatic.rg.ru/uploads/images/2023/02/17/bender_7b5.jpg"
                )
            )

            // Info message - collapsed
            ChatMessageView(
                item = ChatMessageUiModel(
                    id = 5,
                    text = "Короткое информационное сообщение",
                    userType = ChatMessageUiType.Info,
                    status = UiMessageStatus.Delivered,
                    isExpanded = false
                )
            )

            // Info message - expanded
            ChatMessageView(
                item = ChatMessageUiModel(
                    id = 6,
                    text = "Это длинное информационное сообщение в развернутом состоянии.",
                    userType = ChatMessageUiType.Info,
                    status = UiMessageStatus.Delivered,
                    isExpanded = true
                )
            )

            // Buttons message
            ChatMessageView(
                item = ChatMessageUiModel(
                    id = 8,
                    text = "Выберите опцию:",
                    userType = ChatMessageUiType.Buttons,
                    status = UiMessageStatus.Viewed,
                    isExpanded = false,
                    buttons = ChatMessageUiModel.Buttons(
                        list = listOf(
                            ChatMessageUiModel.Button("action1", "Да", "", false),
                            ChatMessageUiModel.Button("action2", "Нет", "", false)
                        ),
                        isEnabled = true
                    )
                )
            )
        }
    }
}

// endregion
