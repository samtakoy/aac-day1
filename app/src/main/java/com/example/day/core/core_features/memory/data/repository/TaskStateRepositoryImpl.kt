package com.example.day.core.core_features.memory.data.repository

import android.util.Log
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.repository.AgentMemoryRepository
import com.example.day.core.core_features.memory.domain.provider.rag.RagLog
import com.example.day.core.core_features.memory.domain.provider.rag.TaskState
import com.example.day.core.core_features.memory.domain.provider.rag.TaskStateRepository
import com.example.day.core.core_features.memory.domain.provider.rag.TaskStateUpdateResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

class TaskStateRepositoryImpl @Inject constructor(
    private val httpClient: HttpClient,
    private val agentMemoryRepository: AgentMemoryRepository,
    private val json: Json,
) : TaskStateRepository {

    companion object {
        private val TAG = RagLog.TAG
        const val CATEGORY = "task_state"
        const val KEY = "current"
    }

    override suspend fun update(
        agentId: Long,
        lastMessages: List<AContextMessage>,
        serverUrl: String,
    ): TaskStateUpdateResult {
        val currentJson = agentMemoryRepository.getFact(agentId, KEY, CATEGORY)?.fact ?: "{}"

        val roles = lastMessages.map { it.role.name.lowercase() }
        Log.d(TAG, "TaskState update request → $serverUrl/task-state/update, messages=${lastMessages.size}, roles=$roles")

        val request = TaskStateUpdateRequestDto(
            currentState = currentJson,
            lastMessages = lastMessages.map { MessageDto(role = it.role.name.lowercase(), content = it.content) }
        )

        val httpStart = System.currentTimeMillis()
        val response = runCatching {
            httpClient.post("${serverUrl.trimEnd('/')}/task-state/update") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<TaskStateUpdateResponseDto>()
        }.getOrElse { e ->
            Log.w(TAG, "TaskState update failed (${System.currentTimeMillis() - httpStart}ms): ${e.message}, using cached state")
            return TaskStateUpdateResult(getCurrent(agentId))
        }
        Log.d(TAG, "TaskState HTTP done: ${System.currentTimeMillis() - httpStart}ms")

        // Сохранить обновлённый state в память агента
        agentMemoryRepository.upsertFact(agentId, KEY, CATEGORY, response.updatedState)

        val updatedState = parseState(response.updatedState)

        Log.d(TAG, "TaskState updated: intent=${updatedState.intent}, focus=${updatedState.currentFocus.className}, switched=${updatedState.contextSwitched}")

        return TaskStateUpdateResult(
            updatedState = updatedState,
        )
    }

    override suspend fun getCurrent(agentId: Long): TaskState {
        val raw = agentMemoryRepository.getFact(agentId, KEY, CATEGORY)?.fact
            ?: return TaskState.EMPTY
        return parseState(raw)
    }

    override suspend fun getCurrentJson(agentId: Long): String? {
        return agentMemoryRepository.getFact(agentId, KEY, CATEGORY)?.fact
    }

    private fun parseState(raw: String): TaskState {
        return runCatching {
            json.decodeFromString<TaskState>(raw)
        }.getOrElse {
            Log.w(TAG, "TaskState failed to parse state JSON: ${it.message}")
            TaskState.EMPTY
        }
    }

    // --- DTOs ---

    @Serializable
    private data class TaskStateUpdateRequestDto(
        val currentState: String,
        val lastMessages: List<MessageDto>,
    )

    @Serializable
    private data class MessageDto(val role: String, val content: String)

    @Serializable
    private data class TaskStateUpdateResponseDto(
        val updatedState: String,
    )
}
