package com.example.day.core.core_features.chat.data.local.model

/**
 * Database constants for Chat feature.
 * Centralizes all magic numbers used in the database layer.
 */
internal object ChatDbConst {
    // User type constants (stored as Int in database)
    const val BOT_TYPE = 1
    const val USER_TYPE = 2
    const val INFO_TYPE = 3

    // Chat message status constants (stored as Int in database)
    const val MESSAGE_STATUS_SENDING = 1
    const val MESSAGE_STATUS_DELIVERED = 2
    const val MESSAGE_STATUS_VIEWED = 3

    // Default user names
    const val DEFAULT_USER_NAME = "User"
    const val DEFAULT_BOT_NAME = "LLM"
    const val DEFAULT_INFO_NAME = "Info"

    // Default avatar URLs
    const val DEFAULT_USER_AVATAR = "https://free-png.ru/wp-content/uploads/2021/07/free-png.ru-30.png"
    const val DEFAULT_BOT_AVATAR = "https://cdnstatic.rg.ru/uploads/images/2023/02/17/bender_7b5.jpg"
    // const val DEFAULT_INFO_AVATAR = "https://cs11.livemaster.ru/storage/topicavatar/600x450/3a/f9/fe1cd6c34f34f9b1a453d570d0976d15918clz.jpg?h=kDFY5CRyKvILyYaZlCzMUw"
    const val DEFAULT_INFO_AVATAR = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSbSz8D4fY-2ugpxZoLnJ5xaLb0Q2andNq9Q1Fk8IMuCw&s"
}
