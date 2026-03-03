package com.example.day.core.core_features.memory.domain.provider.base

/**
 * типы памяти
 * @param dbName имя в БД
 * */
enum class MemoryType(val dbName: String) {
    /** Память из текущего UserProfile */
    UserProfile("user_profile"),
    /** Память из текущего чата */
    Chat("chat"),
    /** Память из текущей группы чатов */
    ChatGroup("chat_group"),
}