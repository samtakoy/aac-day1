package com.example.day.core.ui.uikit.chat.bar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.day.core.ui.uikit.chat.ChatUiColors
import com.example.day.core.ui.uikit.chat.LocalChatColors
import com.example.day.core.ui.uikit.chat.bar.model.ChatBarUiEvent
import com.example.day.core.ui.uikit.chat.bar.model.ChatBarUiModel

/**
 * Chat input bar with text field and send button
 */
@Composable
fun ChatBarView(
    model: ChatBarUiModel,
    onEvent: (ChatBarUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    colors: ChatUiColors = LocalChatColors.current
) {
    var text by remember { mutableStateOf(model.inputInitialValue) }
    val focusManager = LocalFocusManager.current
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Text field
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(colors.inputBackground)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = text,
                onValueChange = { newText ->
                    text = newText
                    onEvent(ChatBarUiEvent.TextChange(newText))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onPreviewKeyEvent { keyEvent ->
                        // Проверяем нажатие Enter
                        val isEnter = keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter
                        val isDown = keyEvent.type == KeyEventType.KeyDown

                        if (isEnter && isDown) {
                            // TODO на эмулаторе Shift не ловится
                            if (!keyEvent.isShiftPressed) {
                                // ОБЫЧНЫЙ ENTER: отправляем
                                if (text.isNotBlank()) {
                                    onEvent(ChatBarUiEvent.SendClick)
                                    text = ""
                                    // focusManager.clearFocus()
                                }
                                return@onPreviewKeyEvent true // Поглощаем событие
                            }
                            // SHIFT + ENTER: возвращаем false, чтобы TextField сам вставил \n
                            return@onPreviewKeyEvent false
                        }
                        false
                    },
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = colors.inputText
                ),
                cursorBrush = SolidColor(colors.sendButtonEnabled),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (text.isNotBlank()) {
                            onEvent(ChatBarUiEvent.SendClick)
                            text = ""
                            // focusManager.clearFocus()
                        }
                    }
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (text.isEmpty()) {
                            Text(
                                text = "Type a message...",
                                color = colors.inputPlaceholder
                            )
                        }
                        innerTextField()
                    }
                },
                singleLine = false
            )
            
            // Clear button (shown when text is not empty)
            if (text.isNotEmpty()) {
                IconButton(
                    onClick = {
                        text = ""
                        onEvent(ChatBarUiEvent.TextChange(""))
                    },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = colors.inputPlaceholder
                    )
                }
            }
        }
        
        // Send button
        ChatSendButton(
            type = model.buttonType,
            onClick = {
                if (text.isNotBlank()) {
                    onEvent(ChatBarUiEvent.SendClick)
                    text = ""
                    // focusManager.clearFocus()
                }
            }
        )
    }
}
