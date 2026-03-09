package com.example.day.features.mcp_settings.impl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.day.core.core_features.mcp.domain.model.TransportType
import com.example.day.features.mcp_settings.impl.ui.components.AddMcpServerDialog
import com.example.day.features.mcp_settings.impl.ui.components.McpServerCard
import com.example.day.features.mcp_settings.impl.ui.components.McpToolList
import com.example.day.features.mcp_settings.impl.ui.viewmodel.McpSettingsViewModelImpl

/**
 * Экран настройки MCP-серверов.
 *
 * MCP (Model Context Protocol) — стандарт для подключения внешних инструментов к ИИ-агентам.
 * Добавьте сервер, нажмите на карточку — приложение подключится и покажет список доступных
 * инструментов. Auth Token нужен только для защищённых серверов.
 */
@Composable
internal fun McpSettingsScreen(
    viewModel: McpSettingsViewModelImpl,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // ── Local form state for "Add" dialog ─────────────────────────────────────
    var newName by remember { mutableStateOf("") }
    var newUrl by remember { mutableStateOf("") }
    var newToken by remember { mutableStateOf("") }
    var newTransport by remember { mutableStateOf(TransportType.HTTP) }
    var newUrlPath by remember { mutableStateOf("/message") }
    var newStdioCommand by remember { mutableStateOf("") }

    // ── Local form state for "Edit" dialog ────────────────────────────────────
    var editName by remember { mutableStateOf("") }
    var editUrl by remember { mutableStateOf("") }
    var editToken by remember { mutableStateOf("") }
    var editTransport by remember { mutableStateOf(TransportType.HTTP) }
    var editUrlPath by remember { mutableStateOf("/message") }
    var editStdioCommand by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("MCP-серверы", style = MaterialTheme.typography.titleLarge)
            Button(onClick = viewModel::showAddDialog) { Text("+ Добавить") }
        }

        Text(
            text = "Нажмите на сервер, чтобы подключиться и получить список инструментов",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // ── Server list ──────────────────────────────────────────────────────
        if (state.servers.isEmpty()) {
            Text(
                text = "Нет настроенных серверов. Нажмите «+ Добавить», чтобы добавить первый.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else {
            state.servers.forEach { server ->
                McpServerCard(
                    server = server,
                    isSelected = server.id == state.selectedServerId,
                    onSelect = { viewModel.selectServer(server.id) },
                    onEdit = {
                        editName = server.name
                        editUrl = server.url
                        editToken = ""  // token not shown for security — leave blank to keep existing
                        editTransport = server.transportType
                        editUrlPath = server.urlPath
                        editStdioCommand = server.stdioCommand ?: ""
                        viewModel.showEditDialog(server)
                    },
                    onRefresh = { viewModel.refresh(server.id) },
                    onDelete = { viewModel.deleteServer(server.id) },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        // ── Tools section ────────────────────────────────────────────────────
        if (state.selectedServerId != null) {
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Text("Инструменты сервера", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Инструменты — функции, которые ИИ-агент может вызывать автоматически",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )

            if (state.isConnecting) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                McpToolList(tools = state.selectedServerTools)
            }
        }

        // ── Error banner ─────────────────────────────────────────────────────
        state.error?.let { error ->
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Ошибка: $error",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = viewModel::clearError,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) { Text("Закрыть") }
        }
    }

    // ── Add server dialog ─────────────────────────────────────────────────
    if (state.showAddDialog) {
        AddMcpServerDialog(
            serverName = newName,
            serverUrl = newUrl,
            serverToken = newToken,
            transportType = newTransport,
            urlPath = newUrlPath,
            stdioCommand = newStdioCommand,
            onNameChange = { newName = it },
            onUrlChange = { newUrl = it },
            onTokenChange = { newToken = it },
            onTransportTypeChange = { type ->
                newTransport = type
                newUrlPath = when (type) {
                    TransportType.HTTP -> "/message"
                    TransportType.SSE -> "/sse"
                    TransportType.STREAMABLE_HTTP -> "/mcp"
                    TransportType.STDIO -> ""
                }
            },
            onUrlPathChange = { newUrlPath = it },
            onStdioCommandChange = { newStdioCommand = it },
            onConfirm = {
                viewModel.addServer(newName, newUrl, newToken, newTransport, newUrlPath, newStdioCommand.ifBlank { null })
                newName = ""; newUrl = ""; newToken = ""
                newTransport = TransportType.HTTP; newUrlPath = "/message"; newStdioCommand = ""
            },
            onDismiss = {
                viewModel.hideAddDialog()
                newName = ""; newUrl = ""; newToken = ""
                newTransport = TransportType.HTTP; newUrlPath = "/message"; newStdioCommand = ""
            }
        )
    }

    // ── Edit server dialog ────────────────────────────────────────────────
    state.editingServer?.let { editing ->
        AddMcpServerDialog(
            serverName = editName,
            serverUrl = editUrl,
            serverToken = editToken,
            transportType = editTransport,
            urlPath = editUrlPath,
            stdioCommand = editStdioCommand,
            onNameChange = { editName = it },
            onUrlChange = { editUrl = it },
            onTokenChange = { editToken = it },
            onTransportTypeChange = { type ->
                editTransport = type
                editUrlPath = when (type) {
                    TransportType.HTTP -> "/message"
                    TransportType.SSE -> "/sse"
                    TransportType.STREAMABLE_HTTP -> "/mcp"
                    TransportType.STDIO -> ""
                }
            },
            onUrlPathChange = { editUrlPath = it },
            onStdioCommandChange = { editStdioCommand = it },
            isEditMode = true,
            onConfirm = {
                viewModel.editServer(editing.id, editName, editUrl, editToken, editTransport, editUrlPath, editStdioCommand.ifBlank { null })
                editName = ""; editUrl = ""; editToken = ""
                editTransport = TransportType.HTTP; editUrlPath = "/message"; editStdioCommand = ""
            },
            onDismiss = {
                viewModel.hideEditDialog()
                editName = ""; editUrl = ""; editToken = ""
                editTransport = TransportType.HTTP; editUrlPath = "/message"; editStdioCommand = ""
            }
        )
    }
}
