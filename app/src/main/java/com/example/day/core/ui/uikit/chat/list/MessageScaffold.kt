package com.example.day.core.ui.uikit.chat.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Базовый компонент компоновки сообщений
 * 
 * Предоставляет единую структуру для всех типов сообщений:
 * - leading: avatar или пустое пространство
 * - content: основной контент (пузырёк сообщения)
 * - trailing: кнопка копирования или другое действие
 * - supporting: статус или дополнительная информация
 * 
 * @param leading Leading контент (обычно аватар)
 * @param content Основной контент (пузырёк сообщения)
 * @param trailing Trailing контент (кнопка копирования)
 * @param supporting Supporting контент (статус)
 * @param isUserMessage true для пользовательских сообщений (выравнивание справа)
 * @param modifier Модификатор
 */
@Composable
fun MessageScaffold(
    leading: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
    supporting: (@Composable () -> Unit)? = null,
    isUserMessage: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = if (isUserMessage) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        // Leading (avatar для Bot/Info)
        if (leading != null && !isUserMessage) {
            leading()
        }
        
        // Центральная колонка: trailing/content/supporting для User
        // или content/supporting для Bot/Info
        if (isUserMessage) {
            // User сообщения: trailing слева, content, supporting, avatar справа
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    verticalAlignment = Alignment.Top
                ) {
                    // Copy кнопка слева от пузырька
                    if (trailing != null) {
                        trailing()
                    }
                    
                    // Пузырёк сообщения
                    content()
                }
                
                // Статус под сообщением
                if (supporting != null) {
                    supporting()
                }
            }
            
            // Avatar справа
            if (leading != null) {
                leading()
            }
        } else {
            // Bot/Info сообщения: avatar слева, content с trailing справа
            Column(
                modifier = Modifier.weight(1f, fill = false),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    verticalAlignment = Alignment.Top
                ) {
                    // Пузырёк сообщения
                    content()
                    
                    // Copy кнопка справа от пузырька
                    if (trailing != null) {
                        trailing()
                    }
                }
                
                // Статус под сообщением (если есть)
                if (supporting != null) {
                    supporting()
                }
            }
        }
        
        // Trailing для Bot/Info (если нужно снаружи)
        if (trailing != null && !isUserMessage) {
            // Уже отображается внутри Row выше
        }
    }
}

/**
 * Упрощённая версия MessageScaffold для сообщений без supporting контента
 */
@Composable
@NonRestartableComposable
fun MessageScaffold(
    leading: (@Composable () -> Unit)?,
    content: @Composable () -> Unit,
    trailing: (@Composable () -> Unit)?,
    isUserMessage: Boolean,
    modifier: Modifier = Modifier
) {
    MessageScaffold(
        leading = leading,
        content = content,
        trailing = trailing,
        supporting = null,
        isUserMessage = isUserMessage,
        modifier = modifier
    )
}
