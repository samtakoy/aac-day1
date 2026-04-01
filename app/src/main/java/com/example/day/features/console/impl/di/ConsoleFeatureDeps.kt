package com.example.day.features.console.impl.di

import com.example.day.core.core_features.agent.domain.usecase.GetAgentContextUseCase
import com.example.day.core.core_features.memory.domain.repository.LongTermMemoryRepository
import com.example.day.core.core_features.agent.domain.utils.ConsumptionCalculator
import com.example.day.core.core_features.agent.domain.workers.concrete.AssistantWorker
import com.example.day.core.core_features.agent.domain.workers.concrete.CompareWorker
import com.example.day.core.core_features.agent.domain.workers.concrete.PromptWorker
import com.example.day.core.core_features.agent.domain.workers.concrete.RagWorker
import com.example.day.core.core_features.agent.domain.workers.concrete.TaskWorker
import com.example.day.core.core_features.agent.domain.workers.concrete.RejectWorker
import com.example.day.core.core_features.agent.domain.workers.concrete.SimpleWorker
import com.example.day.core.core_features.agent.domain.workers.concrete.StepWorker
import com.example.day.core.core_features.agent.domain.workers.concrete.TalkWorker
import com.example.day.core.core_features.agent.domain.workers.concrete.TeamWorker
import com.example.day.core.core_features.memory.domain.repository.ArtifactRepository
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import com.example.day.core.core_features.chat.domain.usecase.AddChatMessageUseCase
import com.example.day.core.core_features.chat.domain.usecase.ChangeMessageStatusUseCase
import com.example.day.core.core_features.chat.domain.usecase.ClearChatNotViewedMessageUseCase
import com.example.day.core.core_features.chat.domain.usecase.CreateChatUseCase
import com.example.day.core.core_features.chat.domain.usecase.GetChatByIdAsFlowUseCase
import com.example.day.core.core_features.chat.domain.usecase.GetChatMessagesAsFlowUseCase
import com.example.day.core.core_features.chat.domain.usecase.GetChatMessagesWithStatusUseCase
import com.example.day.core.core_features.chat.domain.usecase.GetOrCreateChatUseCase
import com.example.day.core.core_features.chat.domain.usecase.HandleMessageButtonClickUseCase
import com.example.day.core.core_features.chat.domain.usecase.UpdateChatSettingsUseCase
import com.example.day.core.core_features.chat.domain.usecase.CreatePlannerStageChatUseCase
import com.example.day.core.core_features.chat.domain.usecase.UpdateChatTitleUseCase
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.mcp.domain.repository.McpRepository
import com.example.day.core.core_features.mcp.domain.tools.McpTools
import com.example.day.core.core_features.pr_review.domain.usecase.GetPrHandleStateUseCase
import com.example.day.core.core_features.pr_review.domain.usecase.SetPrHandleEnabledUseCase
import com.example.day.core.core_features.pr_review.domain.usecase.StartPrReviewUseCase
import kotlinx.serialization.json.Json
import javax.inject.Named

interface ConsoleFeatureDeps {
    val getMessagesUseCase: GetChatMessagesAsFlowUseCase
    val getMessagesWithStatusUseCase: GetChatMessagesWithStatusUseCase
    val clearUnviewedUseCase: ClearChatNotViewedMessageUseCase
    val addChatMessageUseCase: AddChatMessageUseCase
    val changeMessageUseCase: ChangeMessageStatusUseCase
    val llmRequestUseCase: LlmRequestUseCase
    val getChatByIdAsFlowUseCase: GetChatByIdAsFlowUseCase
    val updateChatSettingsUseCase: UpdateChatSettingsUseCase
    val createChatUseCase: CreateChatUseCase
    val getOrCreateChatUseCase: GetOrCreateChatUseCase
    val updateChatTitleUseCase: UpdateChatTitleUseCase
    fun getPrHandleStateUseCase(): GetPrHandleStateUseCase
    fun setPrHandleEnabledUseCase(): SetPrHandleEnabledUseCase

    val worker0: RejectWorker
    val worker1: CompareWorker
    val worker2: SimpleWorker
    val worker3: StepWorker
    val worker4: PromptWorker
    val worker5: TeamWorker
    val worker6: TalkWorker
    val consuption: ConsumptionCalculator
    
    // Task state machine dependencies
    @get:Named("task")
    val taskWorker: TaskWorker
    @get:Named("support")
    val supportWorker: TaskWorker
    // RAG context chat dependencies
    val ragWorker: RagWorker
    // Assistant chat dependencies
    val assistantWorker: AssistantWorker
    val longTermMemoryRepository: LongTermMemoryRepository
    val artifactRepository: ArtifactRepository
    val chatTools: ChatTools
    val createPlannerStageChatUseCase: CreatePlannerStageChatUseCase
    val getAgentContextUseCase: GetAgentContextUseCase
    val handleMessageButtonClickUseCase: HandleMessageButtonClickUseCase
    val mcpRepository: McpRepository
    val mcpTools: McpTools
    val json: Json
}
