package com.example.day.core.ui.uikit.chat.list

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Компонент кнопки копирования сообщения
 * 
 * @param text Текст для копирования
 * @param modifier Модификатор
 * @param contentDescription Описание для accessibility
 */
@Composable
fun MessageCopyButton(
    text: String,
    modifier: Modifier = Modifier,
    contentDescription: String = "Copy message"
) {
    val context = LocalContext.current
    val contentColor = LocalContentColor.current
    
    Box(
        modifier = modifier
            .padding(start = 4.dp, top = 4.dp)
            .clickable(
                onClick = {
                    ClipboardUtil.copyToClipboard(context, text)
                }
            )
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = contentDescription,
            modifier = Modifier.size(14.dp),
            tint = contentColor.copy(alpha = 0.6f)
        )
    }
}

/**
 * Утилита для работы с буфером обмена
 */
object ClipboardUtil {
    /**
     * Копирует текст в буфер обмена
     * 
     * @param context Context
     * @param text Текст для копирования
     * @param label Метка для буфера обмена (по умолчанию "message")
     */
    fun copyToClipboard(context: Context, text: String, label: String = "message") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }
}
