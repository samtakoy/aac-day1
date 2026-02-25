package com.example.day.core.core_features.chat.data

import com.example.day.core.core_features.chat.data.local.dao.ChatDao
import com.example.day.core.core_features.chat.data.local.dao.ChatGroupDao
import com.example.day.core.core_features.chat.data.local.dao.ChatSettingsDao
import com.example.day.core.core_features.chat.data.local.dao.ChatTypeDao
import com.example.day.core.core_features.chat.data.local.dao.MessageDao
import com.example.day.core.core_features.chat.data.local.dao.UserDao
import com.example.day.core.core_features.chat.data.local.mapper.ChatGroupMapper
import com.example.day.core.core_features.chat.data.local.mapper.ChatMapper
import com.example.day.core.core_features.chat.data.local.mapper.ChatSettingsMapper
import com.example.day.core.core_features.chat.data.local.mapper.MessageMapper
import com.example.day.core.core_features.chat.data.local.mapper.UserMapper
import com.example.day.core.core_features.chat.data.local.model.ChatDbConst
import com.example.day.core.core_features.chat.data.local.model.ChatEntity
import com.example.day.core.core_features.chat.data.local.model.ChatGroupEntity
import com.example.day.core.core_features.chat.data.local.model.ChatTypeEntity
import com.example.day.core.core_features.chat.data.local.model.UserEntity
import com.example.day.core.core_features.chat.domain.ChatRepository
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.model.ChatGroup
import com.example.day.core.core_features.chat.domain.model.ChatMessage
import com.example.day.core.core_features.chat.domain.model.ChatMessageStatus
import com.example.day.core.core_features.chat.domain.model.ChatSettings
import com.example.day.core.core_features.chat.domain.model.ChatType
import com.example.day.core.core_features.chat.domain.model.User
import com.example.day.core.core_features.chat.domain.model.UserType
import com.example.day.core.core_features.llm.domain.ModelConst
import com.example.day.core.core_features.llm.domain.model.ModelSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

internal class ChatRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val chatGroupDao: ChatGroupDao,
    private val chatTypeDao: ChatTypeDao,
    private val chatSettingsDao: ChatSettingsDao,
    private val chatMapper: ChatMapper,
    private val chatGroupMapper: ChatGroupMapper,
    private val chatSettingsMapper: ChatSettingsMapper,
    private val messageMapper: MessageMapper,
    private val userMapper: UserMapper
) : ChatRepository {

    private val mutex = Mutex()

    override suspend fun createChat(title: String, chatGroupId: Long): Long = mutex.withLock {
        return@withLock createChatInternal(title, chatGroupId)
    }

    override suspend fun getChatById(chatId: Long): Chat? {
        return chatDao.getChatById(chatId)?.let(chatMapper::toDomain)
    }

    override fun getChatByIdAsFlow(chatId: Long): Flow<Chat?> {
        return chatDao.getChatByIdAsFlow(chatId).map { it?.let(chatMapper::toDomain) }
    }

    override suspend fun addMessage(
        chatId: Long,
        timestamp: Long,
        userType: UserType,
        text: String,
        status: ChatMessageStatus
    ): Long {
        val users = getOrCreateDefaultUsers()
        val infoUser = getOrCreateInfoUser()
        val userEntity = when (userType) {
            UserType.User -> users.first
            UserType.Bot -> users.second
            UserType.Info -> infoUser
        }

        val entity = messageMapper.toEntity(
            ChatMessage(
                id = 0,
                chatId = chatId,
                timestamp = timestamp,
                user = userEntity,
                text = text,
                status = status
            ),
            userEntity.id
        )

        return messageDao.insert(entity)
    }

    override suspend fun removeMessage(messageId: Long) {
        messageDao.deleteById(messageId)
    }

    override suspend fun changeMessageStatus(messageId: Long, status: ChatMessageStatus) {
        messageDao.updateStatus(messageId, messageMapper.toDbStatus(status))
    }

    override fun getChatMessagesAsFlow(chatId: Long): Flow<List<ChatMessage>> {
        return messageDao.getMessagesByChatId(chatId).map { entities ->
            entities.mapNotNull { entity ->
                // TODO все такие запросы пользователей - убрать - запросить один раз
                val userEntity = userDao.getUserByType(ChatDbConst.USER_TYPE)?.let { user ->
                    if (entity.userId == user.id) user else userDao.getUserByType(ChatDbConst.BOT_TYPE)
                } ?: userDao.getUserByType(ChatDbConst.BOT_TYPE)

                // Also check for Info user
                val infoUserEntity = userDao.getUserByType(ChatDbConst.INFO_TYPE)
                val finalUserEntity = if (infoUserEntity != null && entity.userId == infoUserEntity.id) {
                    infoUserEntity
                } else {
                    userEntity
                }

                finalUserEntity?.let {
                    messageMapper.toDomain(entity, userMapper.toDomain(it))
                }
            }
        }
    }

    override suspend fun getChatMessages(
        chatId: Long,
        status: ChatMessageStatus
    ): List<ChatMessage> {
        return messageDao.getMessagesByChatIdAndStatus(chatId, messageMapper.toDbStatus(status)).mapNotNull { entity ->
            val userEntity = userDao.getUserByType(ChatDbConst.USER_TYPE)?.let { user ->
                if (entity.userId == user.id) user else userDao.getUserByType(ChatDbConst.BOT_TYPE)
            } ?: userDao.getUserByType(ChatDbConst.BOT_TYPE)

            // Also check for Info user
            val infoUserEntity = userDao.getUserByType(ChatDbConst.INFO_TYPE)
            val finalUserEntity = if (infoUserEntity != null && entity.userId == infoUserEntity.id) {
                infoUserEntity
            } else {
                userEntity
            }

            finalUserEntity?.let {
                messageMapper.toDomain(entity, userMapper.toDomain(it))
            }
        }
    }

    override suspend fun getOrCreateDefaultUsers(): Pair<User, User> {
        var userEntity = userDao.getUserByType(ChatDbConst.USER_TYPE)
        if (userEntity == null) {
            val id = userDao.insert(
                UserEntity(
                    name = ChatDbConst.DEFAULT_USER_NAME,
                    type = ChatDbConst.USER_TYPE,
                    avatar = ChatDbConst.DEFAULT_USER_AVATAR
                )
            )
            userEntity = UserEntity(
                id = id,
                name = ChatDbConst.DEFAULT_USER_NAME,
                type = ChatDbConst.USER_TYPE,
                avatar = ChatDbConst.DEFAULT_USER_AVATAR
            )
        }

        var botEntity = userDao.getUserByType(ChatDbConst.BOT_TYPE)
        if (botEntity == null) {
            val id = userDao.insert(
                UserEntity(
                    name = ChatDbConst.DEFAULT_BOT_NAME,
                    type = ChatDbConst.BOT_TYPE,
                    avatar = ChatDbConst.DEFAULT_BOT_AVATAR
                )
            )
            botEntity = UserEntity(
                id = id,
                name = ChatDbConst.DEFAULT_BOT_NAME,
                type = ChatDbConst.BOT_TYPE,
                avatar = ChatDbConst.DEFAULT_BOT_AVATAR
            )
        }

        return Pair(
            userMapper.toDomain(userEntity),
            userMapper.toDomain(botEntity)
        )
    }

    private suspend fun getOrCreateInfoUser(): User {
        var infoEntity = userDao.getUserByType(ChatDbConst.INFO_TYPE)
        if (infoEntity == null) {
            val id = userDao.insert(
                UserEntity(
                    name = ChatDbConst.DEFAULT_INFO_NAME,
                    type = ChatDbConst.INFO_TYPE,
                    avatar = ChatDbConst.DEFAULT_INFO_AVATAR
                )
            )
            infoEntity = UserEntity(
                id = id,
                name = ChatDbConst.DEFAULT_INFO_NAME,
                type = ChatDbConst.INFO_TYPE,
                avatar = ChatDbConst.DEFAULT_INFO_AVATAR
            )
        }
        return userMapper.toDomain(infoEntity)
    }

    override suspend fun clearChat(chatId: Long) {
        messageDao.deleteByChatId(chatId)
    }

    override suspend fun clearChatNotViewedMessages(chatId: Long) {
        messageDao.deleteByChatIdAndStatusNotViewed(chatId, ChatDbConst.MESSAGE_STATUS_VIEWED)
    }

    override suspend fun dropChat(chatId: Long) {
        chatDao.delete(chatId)
    }

    // New methods for groups
    override fun getAllChatGroups(): Flow<List<ChatGroup>> {
        return chatGroupDao.getAllGroupsWithTypeAsFlow().map { entities ->
            entities.map(chatGroupMapper::toDomain)
        }
    }

    override suspend fun getChatGroupWithMaxId(): ChatGroup? {
        return chatGroupDao.getGroupWithMaxId()?.let(chatGroupMapper::toDomain)
    }

    override suspend fun createChatGroup(title: String, chatType: ChatType, colorIndex: Int): Long {
        ensureChatTypesExist()
        
        val typeEntity = chatTypeDao.getTypeByName(chatType.dbType)
            ?: throw IllegalStateException("Chat type ${chatType.dbType} not found")
        
        val entity = ChatGroupEntity(title = title, typeId = typeEntity.id, colorIndex = colorIndex)
        return chatGroupDao.insertGroup(entity)
    }

    override suspend fun updateChatGroup(group: ChatGroup) {
        val entity = ChatGroupEntity(id = group.id, title = group.title, typeId = group.typeId, colorIndex = group.colorIndex)
        chatGroupDao.updateGroup(entity)
    }

    override suspend fun deleteChatGroup(groupId: Long) {
        chatGroupDao.deleteGroupById(groupId)
    }

    override suspend fun getAllChatTypes(): List<ChatType> {
        return enumValues<ChatType>().toList()
    }

    override suspend fun ensureChatTypesExist() {
        // Ensure all enum values exist in database
        val existingTypes = chatTypeDao.getAllTypes()
        val existingDbTypes = existingTypes.map { it.type }.toSet()
        
        enumValues<ChatType>().forEach { chatType ->
            if (chatType.dbType !in existingDbTypes) {
                chatTypeDao.insertType(ChatTypeEntity(type = chatType.dbType))
            }
        }
    }

    override fun getChatsByGroup(groupId: Long): Flow<List<Chat>> {
        return chatDao.getChatsByGroupAsFlow(groupId).map { list ->
            list.map(chatMapper::toDomain)
        }
    }

    override suspend fun getChatsCountInGroup(groupId: Long): Int {
        return chatDao.getChatsCountInGroup(groupId)
    }

    // ChatSettings methods
    override suspend fun getChatSettings(chatId: Long): ChatSettings? {
        return chatSettingsDao.getSettingsByChatId(chatId)?.let(chatSettingsMapper::toDomain)
    }

    override suspend fun updateChatSettings(settings: ChatSettings) {
        chatSettingsDao.insert(chatSettingsMapper.toEntity(settings))
    }

    override suspend fun updateChatTitle(chatId: Long, title: String) {
        chatDao.updateChatTitle(chatId, title)
    }

    override suspend fun getOrCreateChat(title: String, chatGroupId: Long): Chat = mutex.withLock {
        // Сначала пробуем найти существующий чат
        val existingChat = chatDao.getChatByTitleAndGroupId(title, chatGroupId)
        if (existingChat != null) {
            return@withLock chatMapper.toDomain(existingChat)
        }

        // Создаем новый чат, используя существующий метод
        createChatInternal(title, chatGroupId)

        // Возвращаем созданный чат
        return@withLock chatMapper.toDomain(chatDao.getChatByTitleAndGroupId(title, chatGroupId)!!)
    }

    // Внутренний метод для создания чата без возврата ID
    private suspend fun createChatInternal(title: String, chatGroupId: Long): Long {
        getOrCreateDefaultUsers()
        val chatId = chatDao.insert(ChatEntity(title = title, chatGroupId = chatGroupId))

        // Создаем настройки по умолчанию для нового чата
        val defaultSettings = ChatSettings(
            chatId = chatId,
            systemPromt = "",
            model = ModelSettings(name = ModelConst.DEFAULT_MODEL)
        )
        chatSettingsDao.insert(chatSettingsMapper.toEntity(defaultSettings))

        return chatId
    }
}
