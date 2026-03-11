package com.example.day.features.mcp_settings.impl.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.day.core.core_features.mcp.domain.model.TransportType

/**
 * Диалог добавления или редактирования MCP-сервера.
 *
 * Поддерживаемые транспорты:
 * - **HTTP** — JSON-RPC POST на `{url}{urlPath}` (например `/message`)
 * - **SSE** — GET `{url}{urlPath}` → endpoint event → POST
 * - **STREAMABLE_HTTP** — POST с Accept: JSON/SSE (spec 2025)
 * - **STDIO** — локальный процесс через stdin/stdout
 */
@Composable
internal fun AddMcpServerDialog(
    serverName: String,
    serverUrl: String,
    serverToken: String,
    transportType: TransportType,
    urlPath: String,
    stdioCommand: String,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onTransportTypeChange: (TransportType) -> Unit,
    onUrlPathChange: (String) -> Unit,
    onStdioCommandChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isEditMode: Boolean = false,
) {
    var tokenVisible by remember { mutableStateOf(false) }
    val isStdio = transportType == TransportType.STDIO
    val isLocal = transportType == TransportType.LOCAL

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEditMode) "Редактировать MCP-сервер" else "Добавить MCP-сервер")
        },
        text = {
            Column {
                Text(
                    text = "MCP-сервер предоставляет инструменты ИИ-агентам через JSON-RPC протокол",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = serverName,
                    onValueChange = onNameChange,
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // URL — only for network transports
                if (!isStdio && !isLocal) {
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = onUrlChange,
                        label = { Text("URL сервера") },
                        placeholder = { Text("http://10.0.2.2:3000") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }

                // ── Transport selector ─────────────────────────────────────
                Text(
                    text = "Транспорт",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = transportType == TransportType.HTTP,
                        onClick = { onTransportTypeChange(TransportType.HTTP) },
                        label = { Text("HTTP") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    FilterChip(
                        selected = transportType == TransportType.SSE,
                        onClick = { onTransportTypeChange(TransportType.SSE) },
                        label = { Text("SSE") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    FilterChip(
                        selected = transportType == TransportType.STREAMABLE_HTTP,
                        onClick = { onTransportTypeChange(TransportType.STREAMABLE_HTTP) },
                        label = { Text("Stream") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    FilterChip(
                        selected = isStdio,
                        onClick = { onTransportTypeChange(TransportType.STDIO) },
                        label = { Text("STDIO") }
                    )
                }
                Text(
                    text = when (transportType) {
                        TransportType.HTTP -> "POST {url}{path} — классический JSON-RPC"
                        TransportType.SSE -> "GET {url}{path} → endpoint event → POST"
                        TransportType.STREAMABLE_HTTP -> "POST {url}{path}, Accept: JSON или SSE (spec 2025)"
                        TransportType.STDIO -> "Запуск локального процесса, общение через stdin/stdout"
                        TransportType.LOCAL -> "Встроенный локальный MCP (без сети)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                if (isStdio) {
                    // ── STDIO: command field ───────────────────────────────
                    OutlinedTextField(
                        value = stdioCommand,
                        onValueChange = onStdioCommandChange,
                        label = { Text("Команда") },
                        placeholder = { Text("/data/data/.../mcp-server --stdio") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                } else if (!isLocal) {
                    // ── Network transports: URL path ───────────────────────
                    OutlinedTextField(
                        value = urlPath,
                        onValueChange = onUrlPathChange,
                        label = { Text("Путь (URL Path)") },
                        placeholder = {
                            Text(
                                when (transportType) {
                                    TransportType.HTTP -> "/message"
                                    TransportType.SSE -> "/sse"
                                    TransportType.STREAMABLE_HTTP -> "/mcp"
                                    TransportType.STDIO -> ""
                                    TransportType.LOCAL -> ""
                                }
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )

                    // ── Auth token (only for network transports) ───────────
                    OutlinedTextField(
                        value = serverToken,
                        onValueChange = onTokenChange,
                        label = {
                            Text(if (isEditMode) "Auth Token (пусто = не менять)" else "Auth Token (необязательно)")
                        },
                        placeholder = { Text("Bearer токен для защищённых серверов") },
                        singleLine = true,
                        visualTransformation = if (tokenVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { tokenVisible = !tokenVisible }) {
                                Icon(
                                    if (tokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (tokenVisible) "Скрыть токен" else "Показать токен"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            val enabled = serverName.isNotBlank() &&
                    when {
                        isLocal -> true
                        isStdio -> stdioCommand.isNotBlank()
                        else -> serverUrl.isNotBlank()
                    }
            TextButton(onClick = onConfirm, enabled = enabled) {
                Text(if (isEditMode) "Сохранить" else "Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
