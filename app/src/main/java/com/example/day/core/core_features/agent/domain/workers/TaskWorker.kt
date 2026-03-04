package com.example.day.core.core_features.agent.domain.workers

import com.example.day.core.core_features.agent.domain.AIAgentFactory
import com.example.day.core.core_features.agent.domain.workers.base.AWorker
import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.chat.domain.ChatRepository
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import com.example.day.core.core_features.memory.domain.repository.ArtifactRepository
import com.example.day.core.core_features.memory.domain.usecase.GetFactsByChatGroupUseCase
import com.example.day.core.core_features.memory.domain.usecase.UpsertFactWithCategoryUseCase
import javax.inject.Inject

class TaskWorker @Inject constructor(
    private val aiAgentFactory: AIAgentFactory,
    // к агенту привяжем
    // private val getFactsByChatGroupUseCase: GetFactsByChatGroupUseCase,
    // private val upsertFactWithCategoryUseCase: UpsertFactWithCategoryUseCase,
    private val chatRepository: ChatRepository,
    private val artifactRepository: ArtifactRepository,
    private val chatTools: ChatTools
) : AWorker {

    companion object {
        const val AGENT_NAME = "task_state_agent"
    }

    override suspend fun doWork(
        task: String,
        chat: Chat,
        onEvent: (suspend (WorkerEvent) -> Unit)?
    ) {
        TODO("Not yet implemented")
    }
}