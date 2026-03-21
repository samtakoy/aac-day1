# Phase 2: UITL Core — User in The Loop

> **Статус**: TODO  
> **Приоритет**: P1 (High)  
> **Оценка сложности**: High (4-6 дней)

---

## Мотивация

**Проблема**: Все tool calls в текущей реализации выполняются автоматически без участия пользователя. Это небезопасно для MCP tools, которые могут:
- Выполнять запись файлов (`create_file`, `write_file`)
- Отправлять сетевые запросы (`http_request`, `send_email`)
- Удалять данные (`delete_file`, `drop_table`)

**Решение**: Реализовать **User in The Loop** — запрашивать подтверждение у пользователя перед выполнением tool call, с возможностью:
- Approve / Reject
- Remember for session (запомнить решение на время чата)
- Modify arguments (изменить аргументы перед выполнением)

**Architecture**: Использовать `AgentProtocol` абстракцию для поддержки future удалённого agent.

---

## Цели

### День 1-2: Domain Layer
- [ ] Создать `AgentProtocol` интерфейс
- [ ] Создать `AgentRequest`, `AgentEvent`, `ToolApproval` data classes
- [ ] Создать `ApprovalDecision` sealed class
- [ ] Создать `RiskLevel` enum
- [ ] Создать `SessionApprovalCache`
- [ ] Создать `ToolCallApprovalInfo`

### День 3-4: ToolCallOrchestrator Modification
- [ ] Добавить `ToolApprovalCallback` параметр в `ToolCallOrchestrator`
- [ ] Модифицировать `ToolCallOrchestratorImpl` для вызова callback
- [ ] Реализовать session cache lookup
- [ ] Создать `LocalAgentProtocol`

### День 5-6: UI Layer
- [ ] Создать `ToolApprovalBottomSheet` component
- [ ] Создать `RiskBadge` component
- [ ] Создать `JsonViewer` component
- [ ] Интегрировать с `ConsoleViewModel`
- [ ] Добавить `pendingApprovals` state management

---

## Пошаговый план

### Day 1-2: Domain Layer

#### 1.1 Создать пакет `protocol`

**Файл**: `app/src/main/java/com/example/day/core/core_features/agent/domain/protocol/AgentProtocol.kt`

```kotlin
package com.example.day.core.core_features.agent.domain.protocol

import com.example.day.core.core_features.agent.domain.tools.uith.ToolApprovalCallback
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.agent.domain.model.AContextMessage

interface AgentProtocol {
    fun execute(request: AgentRequest): Flow<AgentEvent>
    suspend fun submitApproval(approval: ToolApproval): Result<Unit>
    suspend fun cancel(): Result<Unit>
}

data class AgentRequest(
    val prompt: String,
    val chatId: Long,
    val agentId: Long,
    val systemPrompt: String?,
    val tools: List<ModelRequest.Tool>,
    val approvalCallback: ToolApprovalCallback? = null
)
```

#### 1.2 Создать AgentEvent

**Файл**: `app/src/main/java/com/example/day/core/core_features/agent/domain/protocol/AgentEvent.kt`

```kotlin
package com.example.day.core.core_features.agent.domain.protocol

import com.example.day.core.core_features.agent.domain.tools.uith.RiskLevel

sealed class AgentEvent {
    data class LlmStarted(val requestId: String) : AgentEvent()
    data class LlmCompleted(val response: String, val requestId: String) : AgentEvent()
    data class LlmError(val error: String) : AgentEvent()
    
    data class ToolCallRequested(
        val requestId: String,
        val toolCallId: String,
        val toolName: String,
        val arguments: String,
        val riskLevel: RiskLevel
    ) : AgentEvent()
    
    data class ToolCallCompleted(
        val requestId: String,
        val toolCallId: String,
        val result: String,
        val isError: Boolean
    ) : AgentEvent()
    
    data class ApprovalRequired(
        val requestId: String,
        val toolCallId: String,
        val toolName: String,
        val arguments: String,
        val riskLevel: RiskLevel,
        val waitingSince: Long
    ) : AgentEvent()
    
    data class ApprovalReceived(
        val requestId: String,
        val toolCallId: String,
        val decision: ApprovalDecision
    ) : AgentEvent()
}

data class ToolApproval(
    val requestId: String,
    val toolCallId: String,
    val decision: ApprovalDecision,
    val modifiedArgs: String? = null
)
```

#### 1.3 Создать UITL models

**Файл**: `app/src/main/java/com/example/day/core/core_features/agent/domain/tools/uith/ApprovalDecision.kt`

```kotlin
package com.example.day.core.core_features.agent.domain.tools.uith

sealed class ApprovalDecision {
    object Approved : ApprovalDecision()
    data class Rejected(val reason: String? = null) : ApprovalDecision()
    data class ApprovedWithModification(
        val originalArgs: String,
        val modifiedArgs: String
    ) : ApprovalDecision()
    data class RememberForSession(
        val toolName: String,
        val serverId: String,
        val decision: ApprovalDecision
    ) : ApprovalDecision()
}
```

**Файл**: `app/src/main/java/com/example/day/core/core_features/agent/domain/tools/uith/RiskLevel.kt`

```kotlin
package com.example.day.core.core_features.agent.domain.tools.uith

enum class RiskLevel {
    LOW,      // Read operations — get_user, search
    MEDIUM,   // Write operations — create, update, edit
    HIGH,     // External calls — http_request, send_email, upload
    CRITICAL  // Destructive — delete, drop, destroy
}
```

**Файл**: `app/src/main/java/com/example/day/core/core_features/agent/domain/tools/uith/ToolCallApprovalInfo.kt`

```kotlin
package com.example.day.core.core_features.agent.domain.tools.uith

data class ToolCallApprovalInfo(
    val toolCallId: String,
    val toolName: String,
    val serverId: String,
    val arguments: String,
    val parsedArgs: Map<String, Any?>,
    val riskLevel: RiskLevel
)
```

**Файл**: `app/src/main/java/com/example/day/core/core_features/agent/domain/tools/uith/SessionApprovalCache.kt`

```kotlin
package com.example.day.core.core_features.agent.domain.tools.uith

class SessionApprovalCache {
    private val cache = mutableMapOf<String, ApprovalDecision>()
    
    fun getDecision(serverId: String, toolName: String): ApprovalDecision? =
        cache["$serverId:$toolName"]
    
    fun setDecision(serverId: String, toolName: String, decision: ApprovalDecision) {
        cache["$serverId:$toolName"] = decision
    }
    
    fun clear() = cache.clear()
    fun getCachedTools(): Map<String, ApprovalDecision> = cache.toMap()
}
```

**Файл**: `app/src/main/java/com/example/day/core/core_features/agent/domain/tools/uith/ToolApprovalCallback.kt`

```kotlin
package com.example.day.core.core_features.agent.domain.tools.uith

interface ToolApprovalCallback {
    suspend fun requestApproval(toolCallInfo: ToolCallApprovalInfo): ApprovalDecision
    suspend fun requestBatchApproval(
        toolCalls: List<ToolCallApprovalInfo>
    ): Map<String, ApprovalDecision>
    suspend fun onToolResult(toolCallId: String, result: String, isError: Boolean)
}

object NoOpToolApprovalCallback : ToolApprovalCallback {
    override suspend fun requestApproval(toolCallInfo: ToolCallApprovalInfo) = ApprovalDecision.Approved
    override suspend fun requestBatchApproval(toolCalls: List<ToolCallApprovalInfo>) = emptyMap()
    override suspend fun onToolResult(toolCallId: String, result: String, isError: Boolean) {}
}
```

### Day 3-4: ToolCallOrchestrator Modification

#### 2.1 Обновить интерфейс ToolCallOrchestrator

**Файл**: `app/src/main/java/com/example/day/core/core_features/agent/domain/tools/ToolCallOrchestrator.kt`

Добавить параметр `approvalCallback` в метод `execute()`:

```kotlin
interface ToolCallOrchestrator {
    suspend fun execute(
        initialHistory: List<ModelRequest.Message>,
        memoryMessages: List<AContextMessage>,
        prompt: AContextMessage,
        systemPrompt: String?,
        modelSettings: ModelSettings,
        tools: List<ModelRequest.Tool>,
        context: ToolCallContext,
        onEvent: (suspend (WorkerEvent) -> Unit)?,
        approvalCallback: ToolApprovalCallback? = null  // ← ДОБАВИТЬ
    ): Result<ToolCallingResult>
}
```

#### 2.2 Модифицировать ToolCallOrchestratorImpl

**Файл**: `app/src/main/java/com/example/day/core/core_features/agent/domain/tools/impl/ToolCallOrchestratorImpl.kt`

```kotlin
class ToolCallOrchestratorImpl @Inject constructor(
    private val llmProvider: LlmRequestUseCase,
    private val toolProvider: ToolProvider,
    private val sessionApprovalCache: SessionApprovalCache  // ← ДОБАВИТЬ
) : ToolCallOrchestrator {

    override suspend fun execute(
        initialHistory: List<ModelRequest.Message>,
        memoryMessages: List<AContextMessage>,
        prompt: AContextMessage,
        systemPrompt: String?,
        modelSettings: ModelSettings,
        tools: List<ModelRequest.Tool>,
        context: ToolCallContext,
        onEvent: (suspend (WorkerEvent) -> Unit)?,
        approvalCallback: ToolApprovalCallback? = null  // ← ДОБАВИТЬ
    ): Result<ToolCallingResult> {
        
        // ... existing code ...
        
        for (call in toolCalls) {
            val serverId = context.toolToServer[call.function.name] ?: ""
            
            // 1. Check session cache
            val cachedDecision = sessionApprovalCache.getDecision(serverId, call.function.name)
            if (cachedDecision != null) {
                handleCachedDecision(call, cachedDecision)
                continue
            }
            
            // 2. Ask user via callback (if provided)
            if (approvalCallback != null) {
                val approvalInfo = buildApprovalInfo(call, serverId)
                val decision = approvalCallback.requestApproval(approvalInfo)
                handleDecision(call, decision)
                
                // 3. Cache if "Remember for session"
                if (decision is ApprovalDecision.RememberForSession) {
                    sessionApprovalCache.setDecision(
                        decision.serverId,
                        decision.toolName,
                        decision.decision
                    )
                }
            } else {
                // No callback — execute automatically (backward compatibility)
                executeToolCall(call)
            }
        }
    }
    
    private fun buildApprovalInfo(call: ToolCall, serverId: String): ToolCallApprovalInfo {
        val parsedArgs = parseArguments(call.function.arguments)
        return ToolCallApprovalInfo(
            toolCallId = call.id,
            toolName = call.function.name,
            serverId = serverId,
            arguments = call.function.arguments,
            parsedArgs = parsedArgs,
            riskLevel = classifyRisk(call.function.name)
        )
    }
    
    private fun classifyRisk(toolName: String): RiskLevel {
        val lower = toolName.lowercase()
        return when {
            lower.contains("delete") || lower.contains("drop") || lower.contains("destroy") -> RiskLevel.CRITICAL
            lower.contains("http") || lower.contains("send") || lower.contains("upload") -> RiskLevel.HIGH
            lower.contains("create") || lower.contains("write") || lower.contains("update") -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
    }
    
    private fun parseArguments(args: String): Map<String, Any?> {
        return try {
            json.parseToJsonElement(args).jsonObject
                .mapValues { it.value.toString() }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
```

#### 2.3 Создать LocalAgentProtocol

**Файл**: `app/src/main/java/com/example/day/core/core_features/agent/data/protocol/LocalAgentProtocol.kt`

```kotlin
package com.example.day.core.core_features.agent.data.protocol

import com.example.day.core.core_features.agent.domain.tools.uith.ToolApprovalCallback
import com.example.day.core.core_features.agent.domain.tools.uith.ToolCallApprovalInfo
import com.example.day.core.core_features.agent.domain.protocol.AgentEvent
import com.example.day.core.core_features.agent.domain.protocol.AgentProtocol
import com.example.day.core.core_features.agent.domain.protocol.AgentRequest
import com.example.day.core.core_features.agent.domain.protocol.ToolApproval
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class LocalAgentProtocol(
    private val toolOrchestrator: ToolCallOrchestrator,
    private val json: Json
) : AgentProtocol {
    
    private val approvalChannels = ConcurrentHashMap<String, Channel<ApprovalDecision>>()
    
    override fun execute(request: AgentRequest): Flow<AgentEvent> = flow {
        val requestId = generateRequestId()
        emit(AgentEvent.LlmStarted(requestId))
        
        val approvalCallback = object : ToolApprovalCallback {
            override suspend fun requestApproval(info: ToolCallApprovalInfo): ApprovalDecision {
                val channel = Channel<ApprovalDecision>(Channel.CONFLATED)
                approvalChannels[info.toolCallId] = channel
                
                emit(AgentEvent.ApprovalRequired(
                    requestId = requestId,
                    toolCallId = info.toolCallId,
                    toolName = info.toolName,
                    arguments = info.arguments,
                    riskLevel = info.riskLevel,
                    waitingSince = System.currentTimeMillis()
                ))
                
                val decision = channel.receive()
                approvalChannels.remove(info.toolCallId)
                
                emit(AgentEvent.ApprovalReceived(requestId, info.toolCallId, decision))
                return decision
            }
            
            override suspend fun requestBatchApproval(toolCalls: List<ToolCallApprovalInfo>) = 
                toolCalls.associate { it.toolCallId to ApprovalDecision.Approved }
            
            override suspend fun onToolResult(toolCallId: String, result: String, isError: Boolean) {}
        }
        
        val result = toolOrchestrator.execute(
            initialHistory = emptyList(),
            memoryMessages = emptyList(),
            prompt = AContextMessage(AContextMessage.Role.USER, request.prompt),
            systemPrompt = request.systemPrompt,
            modelSettings = ModelSettings.default(),
            tools = request.tools,
            context = ToolCallContext(agentId = request.agentId),
            onEvent = null,
            approvalCallback = if (request.approvalCallback != null) request.approvalCallback else approvalCallback
        )
        
        result.fold(
            onSuccess = { emit(AgentEvent.LlmCompleted(it.finalResponseText, requestId)) },
            onFailure = { emit(AgentEvent.LlmError(it.message ?: "Unknown")) }
        )
    }
    
    override suspend fun submitApproval(approval: ToolApproval): Result<Unit> {
        val channel = approvalChannels[approval.toolCallId]
        return if (channel != null) {
            channel.send(approval.decision)
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("No pending approval"))
        }
    }
    
    override suspend fun cancel(): Result<Unit> {
        // Implementation for cancellation
        approvalChannels.clear()
        return Result.success(Unit)
    }
    
    private fun generateRequestId() = "req_${System.currentTimeMillis()}_${(0..9999).random()}"
}
```

### Day 5-6: UI Layer

#### 3.1 Создать UITLInteractor

**Файл**: `app/src/main/java/com/example/day/core/core_features/agent/ui/uith/UITLInteractor.kt`

```kotlin
package com.example.day.core.core_features.agent.ui.uith

import com.example.day.core.core_features.agent.domain.protocol.AgentEvent
import com.example.day.core.core_features.agent.domain.protocol.AgentProtocol
import com.example.day.core.core_features.agent.domain.protocol.AgentRequest
import com.example.day.core.core_features.agent.domain.protocol.ToolApproval
import com.example.day.core.core_features.agent.domain.tools.uith.ApprovalDecision
import com.example.day.core.core_features.agent.domain.tools.uith.SessionApprovalCache
import com.example.day.core.core_features.agent.domain.tools.uith.ToolCallApprovalInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class UITLInteractor(
    private val agentProtocol: AgentProtocol
) {
    private val _pendingApprovals = MutableStateFlow<Map<String, ToolCallApprovalInfo>>(emptyMap())
    val pendingApprovals: StateFlow<Map<String, ToolCallApprovalInfo>> = _pendingApprovals.asStateFlow()
    
    private val sessionCache = SessionApprovalCache()
    
    fun startExecution(request: AgentRequest) {
        agentProtocol.execute(request).collect { event ->
            when (event) {
                is AgentEvent.ApprovalRequired -> {
                    val cached = sessionCache.getDecision(event.serverId, event.toolName)
                    if (cached != null) {
                        agentProtocol.submitApproval(
                            ToolApproval(event.requestId, event.toolCallId, cached)
                        )
                    } else {
                        _pendingApprovals.update {
                            it + (event.toolCallId to event.toApprovalInfo())
                        }
                    }
                }
                
                is AgentEvent.LlmCompleted -> {
                    _pendingApprovals.value = emptyMap()
                }
                
                else -> { /* handle other events */ }
            }
        }
    }
    
    fun submitDecision(
        toolCallId: String,
        decision: ApprovalDecision,
        rememberForSession: Boolean = false
    ) {
        if (rememberForSession && decision is ApprovalDecision.Approved) {
            val info = _pendingApprovals.value[toolCallId]
            if (info != null) {
                sessionCache.setDecision(info.serverId, info.toolName, decision)
            }
        }
        
        agentProtocol.submitApproval(
            ToolApproval(requestId = "", toolCallId = toolCallId, decision = decision)
        )
        
        _pendingApprovals.update { it - toolCallId }
    }
    
    private fun AgentEvent.ApprovalRequired.toApprovalInfo() = ToolCallApprovalInfo(
        toolCallId = toolCallId,
        toolName = toolName,
        serverId = "",  // would need to extract from event
        arguments = arguments,
        parsedArgs = emptyMap(),  // would need to parse
        riskLevel = riskLevel
    )
}
```

#### 3.2 Создать ToolApprovalBottomSheet

**Файл**: `app/src/main/java/com/example/day/features/console/impl/ui/components/ToolApprovalBottomSheet.kt`

```kotlin
package com.example.day.features.console.impl.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.day.core.core_features.agent.domain.tools.uith.RiskLevel
import com.example.day.core.core_features.agent.domain.tools.uith.ToolCallApprovalInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolApprovalBottomSheet(
    toolInfo: ToolCallApprovalInfo,
    onApprove: () -> Unit,
    onApproveForSession: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = { /* Block dismiss */ },
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tool Approval",
                    style = MaterialTheme.typography.titleLarge
                )
                RiskBadge(toolInfo.riskLevel)
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Tool info
            InfoRow("Tool", toolInfo.toolName)
            InfoRow("Server", toolInfo.serverId)
            
            Spacer(Modifier.height(16.dp))
            
            // Arguments
            Text(
                text = "Arguments",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                JsonViewer(
                    args = toolInfo.parsedArgs,
                    modifier = Modifier.padding(12.dp)
                )
            }
            
            Spacer(Modifier.height(24.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onReject,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reject")
                }
                
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Approve")
                }
            }
            
            TextButton(
                onClick = onApproveForSession,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Approve All ${toolInfo.toolName} for This Session")
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun RiskBadge(level: RiskLevel) {
    val (color, icon) = when (level) {
        RiskLevel.LOW -> MaterialTheme.colorScheme.primary to Icons.Default.CheckCircle
        RiskLevel.MEDIUM -> MaterialTheme.colorScheme.tertiary to Icons.Default.Warning
        RiskLevel.HIGH -> MaterialTheme.colorScheme.error to Icons.Default.Error
        RiskLevel.CRITICAL -> MaterialTheme.colorScheme.error to Icons.Default.Dangerous
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Text(level.name, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun JsonViewer(
    args: Map<String, Any?>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        args.forEach { (key, value) ->
            Row {
                Text(
                    text = "$key: ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = value?.toString() ?: "null",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
```

#### 3.3 Интеграция с ConsoleViewModel

**Файл**: `app/src/main/java/com/example/day/features/console/impl/ui/viewmodel/ConsoleViewModel.kt`

Добавить в интерфейс:

```kotlin
interface ConsoleViewModel {
    // ... existing
    
    val pendingToolApprovals: StateFlow<Map<String, ToolCallApprovalInfo>>
    
    fun onToolApprovalDecision(toolCallId: String, decision: ApprovalDecision, rememberForSession: Boolean)
}
```

В `ConsoleScreen`:

```kotlin
@Composable
fun ConsoleScreen(viewModel: ConsoleViewModel, ...) {
    val state by viewModel.getStateAsFlow().collectAsStateWithLifecycle()
    val pendingApprovals by viewModel.pendingToolApprovals.collectAsStateWithLifecycle()
    
    // Show bottom sheet for first pending approval
    val firstPending = pendingApprovals.values.firstOrNull()
    if (firstPending != null) {
        ToolApprovalBottomSheet(
            toolInfo = firstPending,
            onApprove = {
                viewModel.onToolApprovalDecision(firstPending.toolCallId, ApprovalDecision.Approved)
            },
            onApproveForSession = {
                viewModel.onToolApprovalDecision(
                    firstPending.toolCallId,
                    ApprovalDecision.Approved,
                    rememberForSession = true
                )
            },
            onReject = {
                viewModel.onToolApprovalDecision(firstPending.toolCallId, ApprovalDecision.Rejected())
            }
        )
    }
    
    // ... rest of screen
}
```

---

## Критерии завершения Phase 2

- [ ] `AgentProtocol` интерфейс создан
- [ ] `LocalAgentProtocol` реализует локальное выполнение
- [ ] `ToolCallOrchestrator` принимает `approvalCallback`
- [ ] `SessionApprovalCache` работает корректно
- [ ] `ToolApprovalBottomSheet` отображается в UI
- [ ] User может Approve/Reject tool calls
- [ ] "Remember for session" сохраняет решение
- [ ] Проект компилируется

---

## Файлы для создания

| Файл | Назначение |
|------|------------|
| `AgentProtocol.kt` | Интерфейс для agent execution |
| `AgentEvent.kt` | События от agent |
| `ToolApproval.kt` | Data class для approval |
| `ApprovalDecision.kt` | Sealed class для решений |
| `RiskLevel.kt` | Enum для уровней риска |
| `ToolCallApprovalInfo.kt` | Info для UI |
| `SessionApprovalCache.kt` | Кэш решений на сессию |
| `ToolApprovalCallback.kt` | Callback интерфейс |
| `LocalAgentProtocol.kt` | Локальная реализация |
| `UITLInteractor.kt` | UI координация |
| `ToolApprovalBottomSheet.kt` | UI component |

---

## Файлы для изменения

| Файл | Изменение |
|------|-----------|
| `ToolCallOrchestrator.kt` | Добавить `approvalCallback` параметр |
| `ToolCallOrchestratorImpl.kt` | Реализовать UITL логику |
| `ConsoleViewModel.kt` | Добавить `pendingToolApprovals` state |
| `ConsoleScreen.kt` | Интегрировать `ToolApprovalBottomSheet` |
