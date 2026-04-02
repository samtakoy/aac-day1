package com.example.day.core.core_features.memory.domain.provider

import android.util.Log
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.workers.task.states_config.toContextMessage
import com.example.day.core.core_features.state_machine.domain.StateContext
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.memory.domain.provider.base.MemoryProvider
import com.example.day.core.core_features.state_machine.domain.SM_TAG
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
        val curState = taskStateStore.getStateId(agentId) ?: return emptyList()
        val step = taskStateStore.getCurrentStage(agentId)
        Log.d(SM_TAG, "[$agentId] getMemoryContext: state=${curState.value}, step=$step")
        val stateData = taskStateStore.getStateData(agentId, curState, step)
        Log.d(SM_TAG, "[$agentId] stateData=${stateData?.javaClass?.simpleName ?: "null"}, historySize=${stateData?.history?.size ?: 0}")
        val context = StateContext(
            chatId = chat.id,
            agentId = agentId,
            store = taskStateStore
        )

        val handler = taskStateStore.getStateConfig().getHandler(curState) ?: return emptyList()
        val systemPrompt = SYSTEM_PROMPT_PREFIX + handler.buildSystemPrompt(context)
        val assistantPreFill = handler.buildAssistantPreFillPrompt(context)
        val assistantHistory = stateData?.history?.map { it.toContextMessage() }

        return buildList {
            add(AContextMessage(role = AContextMessage.Role.SYSTEM, content = systemPrompt))
            assistantPreFill?.let { add(AContextMessage(role = AContextMessage.Role.ASSISTANT, content = it)) }
            assistantHistory?.let { addAll(it) }
        }
    }
}
