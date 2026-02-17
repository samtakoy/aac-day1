package com.example.day.core.core_features.chat.data.local.mapper

import com.example.day.core.core_features.chat.data.local.model.ChatDbConst
import com.example.day.core.core_features.chat.data.local.model.UserEntity
import com.example.day.core.core_features.chat.domain.model.User
import com.example.day.core.core_features.chat.domain.model.UserType

internal fun UserEntity.toDomain(): User = User(
    id = id,
    name = name,
    type = if (type == ChatDbConst.BOT_TYPE) UserType.Bot else UserType.User
)

internal fun User.toEntity(): UserEntity = UserEntity(
    id = id,
    name = name,
    type = if (type == UserType.Bot) ChatDbConst.BOT_TYPE else ChatDbConst.USER_TYPE
)
