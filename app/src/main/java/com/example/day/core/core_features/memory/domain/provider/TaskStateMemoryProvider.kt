package com.example.day.core.core_features.memory.domain.provider

import android.util.Log
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.Role
import com.example.day.core.core_features.agent.domain.workers.task.states_config.TaskStateConfig
import com.example.day.core.core_features.agent.domain.workers.task.states_config.TaskStateData
import com.example.day.core.core_features.agent.domain.workers.task.states_config.toContextMessage
import com.example.day.core.core_features.agent.domain.workers.task.states_store.TaskContext
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.memory.domain.provider.base.MemoryProvider
import com.example.day.core.core_features.state_machine.domain.StateStore

class TaskStateMemoryProvider(
    private val chat: Chat,
    private val agentId: Long,
    private val taskStateStore: StateStore,
) : MemoryProvider {

    companion object {
        const val SYSTEM_PROMPT_PREFIX = "Ответ давай на русском языке. " +
            "Твой ответ должен быть валидным JSON объектом без markdown разметки, " +
            "следуя схеме в инструкциях.\n\n"
    }

    override suspend fun getMemoryContext(): List<AContextMessage> {
        Log.e("ktor", "getMemoryContext: ${agentId}")
        val curState = taskStateStore.getStateId(agentId) ?: return emptyList()
        Log.e("ktor", "getMemoryContext: agentId=$agentId, curState=$curState, step=${taskStateStore.getCurrentStage(agentId)}")
        val stateData = taskStateStore.getStateData(agentId, curState, taskStateStore.getCurrentStage(agentId)) as? TaskStateData
        Log.e("ktor", "stateData: $stateData")
        val context = TaskContext(
            chat = chat,
            agentId = agentId,
            store = taskStateStore
        )

        val handler = TaskStateConfig.config.getHandler(curState) ?: return emptyList()
        val systemPrompt = SYSTEM_PROMPT_PREFIX + handler.buildSystemPrompt(context)
        val assistantPreFill = handler.buildAssistantPreFillPrompt(context)
        val assistantHistory = stateData?.history?.map { it.toContextMessage() }

        return buildList {
            add(AContextMessage(role = Role.SYSTEM, content = systemPrompt))
            assistantPreFill?.let { add(AContextMessage(role = Role.ASSISTANT, content = it)) }
            assistantHistory?.let { addAll(it) }
        }
    }
}
