package com.example.day.core.core_features.memory.domain.provider.rag

import com.example.day.core.core_features.agent.domain.model.AContextMessage

interface TaskStateRepository {

    /**
     * Обновляет TaskState через rag-server POST /task-state/update (Ollama).
     * Сохраняет результат в AgentMemoryRepository.
     *
     * @param agentId идентификатор агента (для хранения в памяти)
     * @param lastMessages последние 4 сообщения из истории (user + assistant)
     * @param serverUrl базовый URL rag-server, например "http://10.0.2.2:3001"
     * @return обновлённый TaskState + резюме последнего ответа (Variant B: Short History)
     */
    suspend fun update(
        agentId: Long,
        lastMessages: List<AContextMessage>,
        serverUrl: String,
    ): TaskStateUpdateResult

    /**
     * Читает текущий TaskState из памяти агента (AgentMemoryRepository).
     * Не делает сетевых запросов.
     */
    suspend fun getCurrent(agentId: Long): TaskState

    /**
     * Возвращает сырую JSON-строку TaskState как она хранится в AgentMemoryRepository.
     * Используется для передачи в QueryOptimizer без повторной сериализации.
     */
    suspend fun getCurrentJson(agentId: Long): String?
}

data class TaskStateUpdateResult(
    val updatedState: TaskState,
)
