package com.example.day.core.ui.uikit.chat

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Токены форм для компонентов чата
 */
object ChatShapes {
    /**
     * Форма пузырька пользовательского сообщения
     * - topStart: 16.dp (закруглённый)
     * - topEnd: 4.dp (почти острый)
     * - bottomEnd: 16.dp (закруглённый)
     * - bottomStart: 16.dp (закруглённый)
     */
    val userBubble: RoundedCornerShape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 4.dp,
        bottomEnd = 16.dp,
        bottomStart = 16.dp
    )
    
    /**
     * Форма пузырька сообщения бота
     * - topStart: 4.dp (почти острый)
     * - topEnd: 16.dp (закруглённый)
     * - bottomEnd: 16.dp (закруглённый)
     * - bottomStart: 16.dp (закруглённый)
     */
    val botBubble: RoundedCornerShape = RoundedCornerShape(
        topStart = 4.dp,
        topEnd = 16.dp,
        bottomEnd = 16.dp,
        bottomStart = 16.dp
    )
    
    /**
     * Форма пузырька информационного сообщения
     * - Uniform: 16.dp
     */
    val infoBubble: RoundedCornerShape = RoundedCornerShape(16.dp)
    
    /**
     * Форма пузырька сообщений с кнопками
     * - Uniform: 12.dp
     */
    val buttonsBubble: RoundedCornerShape = RoundedCornerShape(12.dp)
    
    /**
     * Форма кнопок действий
     * - Uniform: 6.dp
     */
    val button: RoundedCornerShape = RoundedCornerShape(6.dp)
    
    /**
     * Форма аватара
     * - Circle
     */
    val avatar: androidx.compose.ui.graphics.Shape = androidx.compose.foundation.shape.CircleShape
}
