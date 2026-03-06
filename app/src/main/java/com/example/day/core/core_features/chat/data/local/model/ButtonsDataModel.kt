package com.example.day.core.core_features.chat.data.local.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ButtonsDataModel(
    @SerialName("list") val list: List<ButtonDataModel>,
    @SerialName("is_enabled") val isEnabled: Boolean
)

@Serializable
internal data class ButtonDataModel(
    @SerialName("action_id") val actionId: String,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String,
    @SerialName("reply_message") val replyMessage: String,
    @SerialName("is_enabled") val isEnabled: Boolean,
    @SerialName("is_pressed") val isPressed: Boolean
)
