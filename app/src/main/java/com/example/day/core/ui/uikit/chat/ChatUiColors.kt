package com.example.day.core.ui.uikit.chat

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Chat UI colors - Material Design 3 inspired playful colors
 */
@Immutable
data class ChatUiColors(
    // User message colors
    val userBubble: Color = Color(0xFF6C63FF),        // Purple
    val userText: Color = Color.White,
    
    // Bot message colors
    val botBubble: Color = Color(0xFFE8E8E8),         // Light gray
    val botText: Color = Color(0xFF1C1B1F),           // Dark
    
    // Avatar colors
    val userAvatarBackground: Color = Color(0xFF7C4DFF), // Deep purple
    val botAvatarBackground: Color = Color(0xFF00BCD4),  // Cyan
    
    // Status colors
    val sendingStatus: Color = Color(0xFF9E9E9E),       // Gray
    val deliveredStatus: Color = Color(0xFF4CAF50),     // Green check
    val viewedStatus: Color = Color(0xFF2196F3),        // Blue double check
    
    // Input bar colors
    val inputBackground: Color = Color(0xFFF5F5F5),
    val inputText: Color = Color(0xFF1C1B1F),
    val inputPlaceholder: Color = Color(0xFF9E9E9E),
    val sendButtonEnabled: Color = Color(0xFF6C63FF),
    val sendButtonDisabled: Color = Color(0xFFBDBDBD),
    
    // General
    val background: Color = Color.White,
    val error: Color = Color(0xFFE53935)
)

/**
 * Alternative dark theme colors for chat
 */
val DarkChatUiColors = ChatUiColors(
    userBubble = Color(0xFF7C4DFF),
    userText = Color.White,
    botBubble = Color(0xFF424242),
    botText = Color.White,
    userAvatarBackground = Color(0xFF9C27B0),
    botAvatarBackground = Color(0xFF00ACC1),
    sendingStatus = Color(0xFF757575),
    deliveredStatus = Color(0xFF81C784),
    viewedStatus = Color(0xFF64B5F6),
    inputBackground = Color(0xFF303030),
    inputText = Color.White,
    inputPlaceholder = Color(0xFF757575),
    sendButtonEnabled = Color(0xFF7C4DFF),
    sendButtonDisabled = Color(0xFF616161),
    background = Color(0xFF121212),
    error = Color(0xFFCF6679)
)

/**
 * CompositionLocal for chat colors to support theming
 */
val LocalChatColors = staticCompositionLocalOf { ChatUiColors() }
