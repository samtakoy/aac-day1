package com.example.day.core.core_features.agent.domain.workers.task.states_config.support

import com.example.day.core.core_features.agent.domain.workers.task.states_config.TaskStateMessage
import com.example.day.core.core_features.state_machine.domain.StateData
import com.example.day.core.core_features.state_machine.domain.model.StateId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface SupportStateData : StateData {

    @Serializable
    data class Init(
        @SerialName("state") override val state: StateId = SupportState.INIT,
        @SerialName("user_id") val userId: Long? = null,
        @SerialName("user_name") val userName: String? = null,
        @SerialName("history") override val history: List<TaskStateMessage> = emptyList()
    ) : SupportStateData

    @Serializable
    data class Planning(
        @SerialName("state") override val state: StateId = SupportState.PLANNING,
        @SerialName("ticket_id") val ticketId: Long? = null,
        @SerialName("ticket_title") val ticketTitle: String? = null,
        @SerialName("ticket_description") val ticketDescription: String? = null,
        @SerialName("history") override val history: List<TaskStateMessage> = emptyList()
    ) : SupportStateData

    @Serializable
    data class Execution(
        @SerialName("state") override val state: StateId = SupportState.EXECUTION,
        @SerialName("ticket_id") val ticketId: Long = 0L,
        @SerialName("history") override val history: List<TaskStateMessage> = emptyList(),
        @SerialName("summary") val summary: String? = null
    ) : SupportStateData

    @Serializable
    data class Verification(
        @SerialName("state") override val state: StateId = SupportState.VERIFICATION,
        @SerialName("ticket_id") val ticketId: Long = 0L,
        @SerialName("history") override val history: List<TaskStateMessage> = emptyList()
    ) : SupportStateData

    @Serializable
    data class Done(
        @SerialName("state") override val state: StateId = SupportState.DONE,
        @SerialName("ticket_id") val ticketId: Long? = null,
        @SerialName("is_escalation") val isEscalation: Boolean = false,
        @SerialName("history") override val history: List<TaskStateMessage> = emptyList()
    ) : SupportStateData
}
