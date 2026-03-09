package com.example.day.core.ui.uikit.chat

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Токены типографики для компонентов чата
 * 
 * Использует Material Theme Typography как основу
 */
object ChatTypography {
    /**
     * Основной текст сообщения
     * - Основан на MaterialTheme.typography.bodyMedium
     * - Размер: 14.sp
     */
    @Composable
    fun messageText(): TextStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = 14.sp
    )
    
    /**
     * Текст информационного сообщения в свёрнутом состоянии
     * - Основан на MaterialTheme.typography.labelMedium
     * - Размер: 12.sp
     */
    @Composable
    fun infoCollapsedText(): TextStyle = MaterialTheme.typography.labelMedium.copy(
        fontSize = 12.sp
    )
    
    /**
     * Текст информационного сообщения в раскрытом состоянии
     * - Основан на MaterialTheme.typography.bodySmall
     * - Размер: 14.sp
     */
    @Composable
    fun infoExpandedText(): TextStyle = MaterialTheme.typography.bodySmall.copy(
        fontSize = 14.sp
    )
    
    /**
     * Текст статуса сообщения
     * - Основан на MaterialTheme.typography.labelSmall
     * - Размер: 10.sp
     */
    @Composable
    fun statusText(): TextStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 10.sp
    )
    
    /**
     * Текст кнопок действий
     * - Основан на MaterialTheme.typography.labelLarge
     * - FontWeight: Medium
     * - Размер: 14.sp
     */
    @Composable
    fun buttonText(): TextStyle = MaterialTheme.typography.labelLarge.copy(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium
    )
    
    /**
     * Текст кнопок действий в нажатом состоянии
     * - FontWeight: Bold
     */
    @Composable
    fun buttonTextPressed(): TextStyle = buttonText().copy(
        fontWeight = FontWeight.Bold
    )
}

/**
 * Размеры текста для информационных сообщений
 */
object ChatTextSizes {
    val infoCollapsed: TextUnit = 12.sp
    val infoExpanded: TextUnit = 14.sp
    val message: TextUnit = 14.sp
    val status: TextUnit = 10.sp
    val button: TextUnit = 14.sp
}
