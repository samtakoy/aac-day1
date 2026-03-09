package com.example.day.core.ui.uikit.chat

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Схема цветов для чата на основе Material Design 3
 * 
 * Поддерживает светлую и тёмную темы через CompositionLocal
 */
@Immutable
data class ChatColorScheme(
    // User message colors
    val userBubble: Color,
    val userText: Color,
    val userAvatarBackground: Color,
    
    // Bot message colors
    val botBubble: Color,
    val botText: Color,
    val botAvatarBackground: Color,
    
    // Info message colors
    val infoBubble: Color,
    val infoText: Color,
    val infoAvatarBackground: Color,
    
    // Info message text size for collapsed/expanded states
    val infoCollapsedTextSize: TextUnit,
    val infoExpandedTextSize: TextUnit,
    val infoCollapsedPadding: Dp,
    val infoExpandedPadding: Dp,

    // Title message colors
    val titleBubble: Color,
    val titleText: Color,
    val titleAvatarBackground: Color,

    // Title message text size for collapsed/expanded states
    val titleCollapsedTextSize: TextUnit,
    val titleExpandedTextSize: TextUnit,
    val titleCollapsedPadding: Dp,
    val titleExpandedPadding: Dp,
    val titleCornerSize: Dp,

    // Buttons message colors
    val buttonsBubbleBackground: Color,
    val buttonsBubbleText: Color,
    
    // Button colors - Primary (green)
    val buttonPrimaryBackground: Color,
    val buttonPrimaryText: Color,
    
    // Button colors - Secondary (blue/gray)
    val buttonSecondaryBackground: Color,
    val buttonSecondaryText: Color,
    
    // Button colors - Cancel (red)
    val buttonCancelBackground: Color,
    val buttonCancelText: Color,
    
    // Status colors
    val sendingStatus: Color,
    val deliveredStatus: Color,
    val viewedStatus: Color,
    
    // Input bar colors
    val inputBackground: Color,
    val inputText: Color,
    val inputPlaceholder: Color,
    val sendButtonEnabled: Color,
    val sendButtonDisabled: Color,
    
    // General
    val background: Color,
    val error: Color,
    val contentColor: Color
)

/**
 * Светлая тема цветов для чата
 */
val LightChatColorScheme = ChatColorScheme(
    userBubble = Color(0xFF6C63FF),        // Purple
    userText = Color.White,
    userAvatarBackground = Color(0xFF7C4DFF), // Deep purple
    
    botBubble = Color(0xFFE8E8E8),         // Light gray
    botText = Color(0xFF1C1B1F),           // Dark
    botAvatarBackground = Color(0xFF00BCD4),  // Cyan
    
    infoBubble = Color(0xFFF5F5F5),         // Very light gray
    infoText = Color(0xFF1C1B1F),           // Almost black
    infoAvatarBackground = Color(0xFF9E9E9E), // Gray
    
    infoCollapsedTextSize = 12.sp,
    infoExpandedTextSize = 14.sp,
    infoCollapsedPadding = 8.dp,
    infoExpandedPadding = 12.dp,

    titleBubble = Color(0xFFFEF3C7),        // Приглушённый янтарный (светлая тема)
    titleText = Color(0xFF1C1B1F),           // Тёмный текст
    titleAvatarBackground = Color(0xFFF59E0B), // Янтарный

    titleCollapsedTextSize = 16.sp,
    titleExpandedTextSize = 18.sp,
    titleCollapsedPadding = 14.dp,
    titleExpandedPadding = 16.dp,
    titleCornerSize = 24.dp,

    buttonsBubbleBackground = Color(0xFFF5F5F5),
    buttonsBubbleText = Color(0xFF1C1B1F),
    
    buttonPrimaryBackground = Color(0xFF2E7D32),  // Darker green
    buttonPrimaryText = Color.White,
    
    buttonSecondaryBackground = Color(0xFF0D47A1),  // Very dark blue
    buttonSecondaryText = Color.White,
    
    buttonCancelBackground = Color(0xFFC62828),  // Darker red
    buttonCancelText = Color.White,
    
    sendingStatus = Color(0xFF9E9E9E),       // Gray
    deliveredStatus = Color(0xFF4CAF50),     // Green check
    viewedStatus = Color(0xFF2196F3),        // Blue double check
    
    inputBackground = Color(0xFFF5F5F5),
    inputText = Color(0xFF1C1B1F),
    inputPlaceholder = Color(0xFF9E9E9E),
    sendButtonEnabled = Color(0xFF6C63FF),
    sendButtonDisabled = Color(0xFFBDBDBD),
    
    background = Color.White,
    error = Color(0xFFE53935),
    contentColor = Color(0xFF1C1B1F)
)

/**
 * Тёмная тема цветов для чата
 */
val DarkChatColorScheme = ChatColorScheme(
    userBubble = Color(0xFF7C4DFF),
    userText = Color.White,
    userAvatarBackground = Color(0xFF9C27B0),
    
    botBubble = Color(0xFF424242),
    botText = Color.White,
    botAvatarBackground = Color(0xFF00ACC1),
    
    infoBubble = Color(0xFFF5F5F5),
    infoText = Color(0xFF1C1B1F),
    infoAvatarBackground = Color(0xFF9E9E9E),
    
    infoCollapsedTextSize = 12.sp,
    infoExpandedTextSize = 14.sp,
    infoCollapsedPadding = 8.dp,
    infoExpandedPadding = 12.dp,

    titleBubble = Color(0xFF694B1E),        // Приглушённый оранжево-янтарный (тёмная тема)
    titleText = Color.White,                 // Белый текст для тёмной темы
    titleAvatarBackground = Color(0xFFF59E0B), // Янтарный

    titleCollapsedTextSize = 16.sp,
    titleExpandedTextSize = 18.sp,
    titleCollapsedPadding = 14.dp,
    titleExpandedPadding = 16.dp,
    titleCornerSize = 24.dp,

    buttonsBubbleBackground = Color(0xFF424242),
    buttonsBubbleText = Color.White,
    
    buttonPrimaryBackground = Color(0xFF388E3C),  // Darker green for dark
    buttonPrimaryText = Color.White,
    
    buttonSecondaryBackground = Color(0xFF1976D2),  // Dark blue for dark
    buttonSecondaryText = Color.White,
    
    buttonCancelBackground = Color(0xFFD32F2F),  // Darker red for dark
    buttonCancelText = Color.White,
    
    sendingStatus = Color(0xFF757575),
    deliveredStatus = Color(0xFF81C784),
    viewedStatus = Color(0xFF64B5F6),
    
    inputBackground = Color(0xFF303030),
    inputText = Color.White,
    inputPlaceholder = Color(0xFF757575),
    sendButtonEnabled = Color(0xFF7C4DFF),
    sendButtonDisabled = Color(0xFF616161),
    
    background = Color(0xFF121212),
    error = Color(0xFFCF6679),
    contentColor = Color.White
)

/**
 * CompositionLocal для схемы цветов чата
 */
val LocalChatColorScheme = staticCompositionLocalOf { LightChatColorScheme }
