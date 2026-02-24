package com.example.day.core.core_features.chat.data.local.mapper

import com.example.day.core.core_features.chat.data.local.model.ChatDbConst
import com.example.day.core.core_features.chat.data.local.model.UserEntity
import com.example.day.core.core_features.chat.domain.model.User
import com.example.day.core.core_features.chat.domain.model.UserType
import javax.inject.Inject

internal class UserMapper @Inject constructor() {
    
    fun toDomain(entity: UserEntity): User = User(
        id = entity.id,
        name = entity.name,
        type = if (entity.type == ChatDbConst.BOT_TYPE) UserType.Bot else UserType.User
    )

    fun toEntity(user: User): UserEntity = UserEntity(
        id = user.id,
        name = user.name,
        type = if (user.type == UserType.Bot) ChatDbConst.BOT_TYPE else ChatDbConst.USER_TYPE
    )
}
