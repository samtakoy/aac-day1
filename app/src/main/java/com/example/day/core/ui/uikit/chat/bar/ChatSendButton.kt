package com.example.day.core.ui.uikit.chat.bar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.day.core.ui.uikit.chat.ChatUiColors
import com.example.day.core.ui.uikit.chat.LocalChatColors
import com.example.day.core.ui.uikit.chat.bar.model.ChatSendButtonType

/**
 * Round send button with arrow icon
 */
@Composable
fun ChatSendButton(
    type: ChatSendButtonType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ChatUiColors = LocalChatColors.current
) {
    val isEnabled = type == ChatSendButtonType.Arrow
    val isLoading = type == ChatSendButtonType.Loading
    
    val buttonColor = if (isEnabled) colors.sendButtonEnabled else colors.sendButtonDisabled
    
    // Rotate animation for send arrow
    val rotation by animateFloatAsState(
        targetValue = if (isLoading) 0f else -45f,
        animationSpec = tween(300),
        label = "rotation"
    )
    
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(buttonColor)
            .then(
                if (isEnabled) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotation)
                )
            }
        }
    }
}
