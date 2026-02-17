package com.example.day.core.core_features.chat.data

import com.example.day.core.core_features.chat.data.local.dao.ChatDao
import com.example.day.core.core_features.chat.data.local.dao.MessageDao
import com.example.day.core.core_features.chat.data.local.dao.UserDao
import com.example.day.core.core_features.chat.data.local.mapper.toDomain
import com.example.day.core.core_features.chat.data.local.model.ChatDbConst
import com.example.day.core.core_features.chat.data.local.model.ChatEntity
import com.example.day.core.core_features.chat.data.local.model.MessageEntity
import com.example.day.core.core_features.chat.data.local.model.UserEntity
import com.example.day.core.core_features.chat.domain.ChatRepository
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.model.ChatMessage
import com.example.day.core.core_features.chat.domain.model.ChatMessageStatus
import com.example.day.core.core_features.chat.domain.model.User
import com.example.day.core.core_features.chat.domain.model.UserType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

internal class ChatRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao
) : ChatRepository {

    private val mutex = Mutex()

    private fun ChatMessageStatus.toDbStatus(): Int = when (this) {
        ChatMessageStatus.Sending -> ChatDbConst.MESSAGE_STATUS_SENDING
        ChatMessageStatus.Delivered -> ChatDbConst.MESSAGE_STATUS_DELIVERED
        ChatMessageStatus.Viewed -> ChatDbConst.MESSAGE_STATUS_VIEWED
    }

    override suspend fun createChat(title: String): Long = mutex.withLock {
        getOrCreateDefaultUsers()
        return chatDao.insert(ChatEntity(title = title))
    }

    override fun getChatListAsFlow(): Flow<List<Chat>> {
        return chatDao.getAllChats().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getChatById(chatId: Long): Chat? {
        return chatDao.getChatById(chatId)?.toDomain()
    }

    override suspend fun addMessage(
        chatId: Long,
        timestamp: Long,
        userType: UserType,
        text: String,
        status: ChatMessageStatus
    ): Long {
        val users = getOrCreateDefaultUsers()
        val userEntity = when (userType) {
            UserType.User -> users.first
            UserType.Bot -> users.second
        }

        val entity = MessageEntity(
            chatId = chatId,
            userId = userEntity.id,
            timestamp = timestamp,
            text = text,
            status = status.toDbStatus()
        )

        return messageDao.insert(entity)
    }

    override suspend fun removeMessage(messageId: Long) {
        messageDao.deleteById(messageId)
    }

    override suspend fun changeMessageStatus(messageId: Long, status: ChatMessageStatus) {
        messageDao.updateStatus(messageId, status.toDbStatus())
    }

    override fun getChatMessagesAsFlow(chatId: Long): Flow<List<ChatMessage>> {
        return messageDao.getMessagesByChatId(chatId).map { entities ->
            entities.mapNotNull { entity ->
                val userEntity = userDao.getUserByType(ChatDbConst.USER_TYPE)?.let { user ->
                    if (entity.userId == user.id) user else userDao.getUserByType(ChatDbConst.BOT_TYPE)
                } ?: userDao.getUserByType(ChatDbConst.BOT_TYPE)

                userEntity?.let {
                    entity.toDomain(it.toDomain())
                }
            }
        }
    }

    override suspend fun getOrCreateDefaultUsers(): Pair<User, User> {
        var userEntity = userDao.getUserByType(ChatDbConst.USER_TYPE)
        if (userEntity == null) {
            val id = userDao.insert(UserEntity(name = ChatDbConst.DEFAULT_USER_NAME, type = ChatDbConst.USER_TYPE))
            userEntity = UserEntity(id = id, name = ChatDbConst.DEFAULT_USER_NAME, type = ChatDbConst.USER_TYPE)
        }

        var botEntity = userDao.getUserByType(ChatDbConst.BOT_TYPE)
        if (botEntity == null) {
            val id = userDao.insert(UserEntity(name = ChatDbConst.DEFAULT_BOT_NAME, type = ChatDbConst.BOT_TYPE))
            botEntity = UserEntity(id = id, name = ChatDbConst.DEFAULT_BOT_NAME, type = ChatDbConst.BOT_TYPE)
        }

        return Pair(
            userEntity.toDomain(),
            botEntity.toDomain()
        )
    }

    override suspend fun clearChat(chatId: Long) {
        messageDao.deleteByChatId(chatId)
    }

    override suspend fun clearChatNotViewedMessages(chatId: Long) {
        messageDao.deleteByChatIdAndStatusNotViewed(chatId, ChatDbConst.MESSAGE_STATUS_VIEWED)
    }

    override suspend fun dropChat(chatId: Long) {
        val chatEntity = chatDao.getChatById(chatId)
        chatEntity?.let {
            chatDao.delete(it)
        }
    }
}
