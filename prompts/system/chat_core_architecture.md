# Chat Core Feature - Architecture Specification

## Overview

This document describes the architecture for implementing a Chat core feature with domain and data layers using Room database.

## 1. Dependencies Required

### 1.1 Gradle Dependencies (libs.versions.toml)

Add the following to `[versions]` section:
```toml
room = "2.6.1"  # Latest stable version as of 2024
```

Add the following to `[libraries]` section:
```toml
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
```

### 1.2 Build Dependencies (app/build.gradle.kts)

Add to ksp dependencies:
```kotlin
ksp(libs.room.compiler)
```

Add to implementation dependencies:
```kotlin
implementation(libs.room.runtime)
implementation(libs.room.ktx)
```

---

## 2. Package Structure

The chat core feature will be located at:
```
com.example.day.core.core_features.chat
```

Full directory structure:
```
app/src/main/java/com/example/day/core/core_features/chat/
├── domain/
│   ├── model/
│   │   ├── Chat.kt
│   │   ├── UserType.kt
│   │   ├── User.kt
│   │   ├── ChatMessageStatus.kt
│   │   └── ChatMessage.kt
│   ├── usecase/
│   │   ├── CreateChatUseCase.kt
│   │   ├── GetChatListAsFlowUseCase.kt
│   │   ├── AddChatMessageUseCase.kt
│   │   ├── RemoveChatMessageUseCase.kt
│   │   ├── ChangeMessageStatusUseCase.kt
│   │   ├── GetChatMessagesAsFlowUseCase.kt
│   │   ├── ClearChatUseCase.kt
│   │   ├── ClearChatNotViewedMessageUseCase.kt
│   │   └── DropChatUseCase.kt
│   └── ChatRepository.kt
└── data/
    ├── local/
    │   ├── model/
    │   │   ├── UserEntity.kt
    │   │   ├── ChatEntity.kt
    │   │   └── MessageEntity.kt
    │   ├── mapper/
    │   │   ├── UserMapper.kt
    │   │   ├── ChatMapper.kt
    │   │   └── MessageMapper.kt
    │   ├── dao/
    │   │   ├── UserDao.kt
    │   │   ├── ChatDao.kt
    │   │   └── MessageDao.kt
    │   └── ChatDatabase.kt
    └── ChatRepositoryImpl.kt
```

---

## 3. Domain Layer

### 3.1 Domain Models

#### UserType.kt
```kotlin
enum class UserType {
    User,
    Bot
}
```

#### User.kt
```kotlin
data class User(
    val id: Long,
    val name: String,
    val type: UserType
)
```

#### Chat.kt
```kotlin
data class Chat(
    val id: Long,
    val title: String
)
```

#### ChatMessageStatus.kt
```kotlin
enum class ChatMessageStatus {
    Sending,    // Отправка
    Delivered, // Доставлено
    Viewed     // Просмотрено
}
```

#### ChatMessage.kt
```kotlin
data class ChatMessage(
    val id: Long,
    val chatId: Long,
    val timestamp: Long,
    val user: User,
    val text: String,
    val status: ChatMessageStatus
)
```

### 3.2 Repository Interface

#### ChatRepository.kt
```kotlin
interface ChatRepository {
    // Chat operations
    suspend fun createChat(title: String): Long
    fun getChatListAsFlow(): Flow<List<Chat>>
    suspend fun getChatById(chatId: Long): Chat?
    
    // Message operations
    suspend fun addMessage(
        chatId: Long,
        timestamp: Long,
        userType: UserType,
        text: String,
        status: ChatMessageStatus
    ): Long
    suspend fun removeMessage(messageId: Long)
    suspend fun changeMessageStatus(messageId: Long, status: ChatMessageStatus)
    fun getChatMessagesAsFlow(chatId: Long): Flow<List<ChatMessage>>
    
    // User operations (internal)
    suspend fun getOrCreateDefaultUsers(): Pair<User, User>
    
    // Chat management operations
    suspend fun clearChat(chatId: Long)
    suspend fun clearChatNotViewedMessages(chatId: Long)
    suspend fun dropChat(chatId: Long)
}
```

### 3.3 Use Cases

#### CreateChatUseCase.kt
- Input: `title: String`
- Output: `Long` (created chat ID)
- Logic: Creates new chat and returns its ID using Mutex.withLock

#### GetChatListAsFlowUseCase.kt
- Input: none
- Output: `Flow<List<Chat>>`
- Logic: Returns Flow of all chats

#### AddChatMessageUseCase.kt
- Input: `chatId: Long`, `timestamp: Long`, `userType: UserType`, `text: String`, `status: ChatMessageStatus`
- Output: `Long` (created message ID)
- Logic: Adds new message to chat

#### RemoveChatMessageUseCase.kt
- Input: `messageId: Long`
- Output: `Unit`
- Logic: Deletes message by ID

#### ChangeMessageStatusUseCase.kt
- Input: `messageId: Long`, `status: ChatMessageStatus`
- Output: `Unit`
- Logic: Updates message status

#### GetChatMessagesAsFlowUseCase.kt
- Input: `chatId: Long`
- Output: `Flow<List<ChatMessage>>`
- Logic: Returns Flow of messages for specific chat

#### ClearChatUseCase.kt
- Input: `chatId: Long`
- Output: `Unit`
- Logic: Deletes all messages for the specified chat ID

#### ClearChatNotViewedMessageUseCase.kt
- Input: `chatId: Long`
- Output: `Unit`
- Logic: Deletes all messages for the specified chat ID where ChatMessageStatus != Viewed

#### DropChatUseCase.kt
- Input: `chatId: Long`
- Output: `Unit`
- Logic: Deletes the chat and all its messages

---

## 4. Data Layer

### 4.1 Room Entities (internal)

All entities must have `@Entity` annotation and be internal.

#### UserEntity.kt
```kotlin
@Entity(tableName = "users")
internal data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: Int  // 1 = Bot, 2 = User
)
```

#### ChatEntity.kt
```kotlin
@Entity(tableName = "chats")
internal data class ChatEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String
)
```

#### MessageEntity.kt
```kotlin
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
internal data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val chatId: Long,
    val userId: Long,
    val timestamp: Long,
    val text: String,
    val status: Int  // 1 = Sending, 2 = Delivered, 3 = Viewed
)
```

### 4.2 DAOs (Data Access Objects)

All DAOs must be internal.

#### UserDao.kt
```kotlin
@Dao
internal interface UserDao {
    @Query("SELECT * FROM users WHERE type = :type LIMIT 1")
    suspend fun getUserByType(type: Int): UserEntity?
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(user: UserEntity): Long
    
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>
}
```

#### ChatDao.kt
```kotlin
@Dao
internal interface ChatDao {
    @Insert
    suspend fun insert(chat: ChatEntity): Long
    
    @Query("SELECT * FROM chats ORDER BY id DESC")
    fun getAllChats(): Flow<List<ChatEntity>>
    
    @Query("SELECT * FROM chats WHERE id = :chatId")
    suspend fun getChatById(chatId: Long): ChatEntity?
    
    @Delete
    suspend fun delete(chat: ChatEntity)
}
```

#### MessageDao.kt
```kotlin
@Dao
internal interface MessageDao {
    @Insert
    suspend fun insert(message: MessageEntity): Long
    
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesByChatId(chatId: Long): Flow<List<MessageEntity>>
    
    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: Long): MessageEntity?
    
    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateStatus(messageId: Long, status: Int)
    
    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteById(messageId: Long)
}
```

### 4.3 Room Database

#### ChatDatabase.kt
```kotlin
@Database(
    entities = [UserEntity::class, ChatEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false
)
internal abstract class ChatDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
}
```

### 4.4 Mappers (internal)

#### UserMapper.kt
```kotlin
internal fun UserEntity.toDomain(): User = User(
    id = id,
    name = name,
    type = if (type == 1) UserType.Bot else UserType.User
)

internal fun User.toEntity(): UserEntity = UserEntity(
    id = id,
    name = name,
    type = if (type == UserType.Bot) 1 else 2
)
```

#### ChatMapper.kt
```kotlin
internal fun ChatEntity.toDomain(): Chat = Chat(
    id = id,
    title = title
)

internal fun Chat.toEntity(): ChatEntity = ChatEntity(
    id = id,
    title = title
)
```

#### MessageMapper.kt
```kotlin
internal fun MessageEntity.toDomain(user: User): ChatMessage = ChatMessage(
    id = id,
    chatId = chatId,
    timestamp = timestamp,
    user = user,
    text = text,
    status = when (status) {
        1 -> ChatMessageStatus.Sending
        2 -> ChatMessageStatus.Delivered
        3 -> ChatMessageStatus.Viewed
        else -> ChatMessageStatus.Sending
    }
)

internal fun ChatMessage.toEntity(userId: Long): MessageEntity = MessageEntity(
    id = id,
    chatId = chatId,
    userId = userId,
    timestamp = timestamp,
    text = text,
    status = when (status) {
        ChatMessageStatus.Sending -> 1
        ChatMessageStatus.Delivered -> 2
        ChatMessageStatus.Viewed -> 3
    }
)
```

### 4.5 Repository Implementation

#### ChatRepositoryImpl.kt
```kotlin
internal class ChatRepositoryImpl(
    private val database: ChatDatabase,
    private val userDao: UserDao,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao
) : ChatRepository {
    
    companion object {
        const val DEFAULT_USER_NAME = "User"
        const val DEFAULT_BOT_NAME = "LLM"
        const val BOT_TYPE = 1
        const val USER_TYPE = 2
    }
    
    private val mutex = Mutex()
    
    override suspend fun createChat(title: String): Long = mutex.withLock {
        // Ensure default users exist
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
            status = status.ordinal + 1  // Convert enum to Int (1-based)
        )
        
        return messageDao.insert(entity)
    }
    
    override suspend fun removeMessage(messageId: Long) {
        messageDao.deleteById(messageId)
    }
    
    override suspend fun changeMessageStatus(messageId: Long, status: ChatMessageStatus) {
        messageDao.updateStatus(messageId, status.ordinal + 1)
    }
    
    override fun getChatMessagesAsFlow(chatId: Long): Flow<List<ChatMessage>> {
        return messageDao.getMessagesByChatId(chatId).map { entities ->
            entities.mapNotNull { entity ->
                val userEntity = userDao.getUserByType(
                    chatDao.getChatById(entity.chatId)?.let {
                        // Determine user type from entity
                    } ?: return@mapNotNull null
                )
                entity.toDomain(userEntity?.toDomain() ?: return@mapNotNull null)
            }
        }
    }
    
    override suspend fun getOrCreateDefaultUsers(): Pair<User, User> {
        var userEntity = userDao.getUserByType(USER_TYPE)
        if (userEntity == null) {
            val id = userDao.insert(UserEntity(name = DEFAULT_USER_NAME, type = USER_TYPE))
            userEntity = UserEntity(id = id, name = DEFAULT_USER_NAME, type = USER_TYPE)
        }
        
        var botEntity = userDao.getUserByType(BOT_TYPE)
        if (botEntity == null) {
            val id = userDao.insert(UserEntity(name = DEFAULT_BOT_NAME, type = BOT_TYPE))
            botEntity = UserEntity(id = id, name = DEFAULT_BOT_NAME, type = BOT_TYPE)
        }
        
        return Pair(
            userEntity.toDomain(),
            botEntity.toDomain()
        )
    }
}
```

---

## 5. Implementation Notes

1. **Default Users**: When creating a chat, the repository must ensure that two default users exist:
   - User with name "User" and type UserType.User
   - User with name "LLM" and type UserType.Bot

2. **Mutex Protection**: Chat creation must be protected by Mutex to prevent race conditions

3. **Internal Visibility**: All data layer classes (entities, DAOs, database, mappers, repository impl) must have `internal` visibility modifier

4. **Flow**: Use standard Room Flow queries which return cold flows

5. **Foreign Keys**: Messages have foreign keys to both Chat and User tables with CASCADE delete

6. **ID Generation**: All entities use auto-generated primary keys

7. **Integer Enums**: UserEntity.type uses Int (1=Bot, 2=User), MessageEntity.status uses Int (1=Sending, 2=Delivered, 3=Viewed)
