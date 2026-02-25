# Agent Data Layer Implementation Plan

## Overview
This plan outlines the implementation of the Agent feature's data layer, including database entities, DAOs, mappers, and repository following the existing Clean Architecture patterns in the codebase.

## Domain Analysis

### Current State
- **ChatDatabase** exists with tables: users, chats, messages, chat_types, chat_groups, chat_settings
- **Agent domain layer** exists with:
  - `AContext` - context model with agentName, systemPrompt, messages
  - `AContextMessage` - message with role, content, orderNumber
  - `AContextOwner` - interface for context management
  - `InMemoryContextOwner` - in-memory implementation
- **Pattern**: Entity (data) ↔ Mapper ↔ Domain Model ↔ Repository Interface ↔ Repository Implementation

### Clarified Requirements (from user)
1. **chatUserId only** - single field for UserEntity reference (avatar = UserEntity)
2. **kotlinx.serialization** - use JSON format for context serialization
3. **Context per agent** - one context per agent globally (not per chat)
4. **Avatar** = UserEntity that the agent uses to communicate

---

## Implementation Plan

### Phase 1: Domain Layer (AgentRepository interface)

#### 1.1 Create Agent domain model
- File: `com.example.day.core.core_features.agent.domain.model.Agent`
- Properties: id, systemName, title, chatUserId, isCommon

#### 1.2 Create AgentRepository interface
- File: `com.example.day.core.core_features.agent.domain.AgentRepository`
- Methods:
  - `createAgent()` / `updateAgent()` / `deleteAgent()`
  - `getAgentById()` / `getAllAgents()` / `getCommonAgents()`
  - `bindAgentToChat()` / `unbindAgentFromChat()` / `getChatsForAgent()`
  - `saveAgentContext()` / `getAgentContext()` / `clearAgentContext()`

---

### Phase 2: Data Layer - Entities

#### 2.1 AgentEntity
```kotlin
@Entity(tableName = "agents")
data class AgentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val systemName: String,
    val title: String,
    val chatUserId: Long,        // FK to UserEntity
    val isCommon: Int            // 1 = available in all chats
)
```

#### 2.2 AgentToChatEntity
```kotlin
@Entity(
    tableName = "agent_to_chat",
    primaryKeys = ["agent_id", "chat_id"],
    foreignKeys = [
        ForeignKey(AgentEntity::class, ...),
        ForeignKey(ChatEntity::class, ...)
    ]
)
data class AgentToChatEntity(
    val agentId: Long,
    val chatId: Long
)
```

#### 2.3 AgentContextMemoryEntity
```kotlin
@Entity(tableName = "agent_context_memory")
data class AgentContextMemoryEntity(
    @PrimaryKey
    val agentId: Long,
    val context: String  // JSON serialized AContextEntityData
)
```

#### 2.4 AContextEntityData
- File: `com.example.day.core.core_features.agent.data.local.model`
- Data class mirroring `AContext` with `@Serializable` annotations
- Will be serialized to JSON for storage

---

### Phase 3: DAOs

#### 3.1 AgentDao
- `insert()`, `update()`, `delete()`, `getById()`
- `getAll()`, `getCommonAgents()` (where isCommon = 1)
- `getByUserId()`

#### 3.2 AgentToChatDao
- `insert()`, `delete()`
- `getChatsForAgent(agentId)`
- `getAgentsForChat(chatId)`
- `isAgentBoundToChat(agentId, chatId)`

#### 3.3 AgentContextMemoryDao
- `insertOrUpdate()`
- `getContext(agentId)`
- `delete(agentId)`

---

### Phase 4: Mappers

#### 4.1 AgentMapper
- `toDomain(AgentEntity): Agent`
- `toEntity(Agent): AgentEntity`

#### 4.2 AContextMapper
- `toEntityData(AContext): AContextEntityData`
- `toDomain(AContextEntityData): AContext`
- Uses kotlinx.serialization for JSON conversion

---

### Phase 5: Repository Implementation

#### 5.1 AgentRepositoryImpl
- Inject all DAOs and mappers
- Implement all interface methods
- Handle JSON serialization/deserialization of context

---

### Phase 6: Dependency Injection

#### 6.1 Create AgentCoreFeatureModule
- Similar to `ChatCoreFeatureModule`
- Provide DAOs from ChatDatabase
- Bind AgentRepository interface to implementation

#### 6.2 Update ChatDatabase
- Add new entities to @Database annotation
- Add new DAO abstract methods
- Increment version number

#### 6.3 Update AppComponent
- Add AgentCoreFeatureModule to component

---

## File Structure (Target)

```
app/src/main/java/com/example/day/core/core_features/agent/
├── data/
│   ├── AgentRepositoryImpl.kt
│   └── local/
│       ├── dao/
│       │   ├── AgentDao.kt
│       │   ├── AgentToChatDao.kt
│       │   └── AgentContextMemoryDao.kt
│       ├── mapper/
│       │   ├── AgentMapper.kt
│       │   └── AContextMapper.kt
│       └── model/
│           ├── AgentEntity.kt
│           ├── AgentToChatEntity.kt
│           ├── AgentContextMemoryEntity.kt
│           └── AContextEntityData.kt
├── di/
│   └── AgentCoreFeatureModule.kt
└── domain/
    ├── AgentRepository.kt
    └── model/
        └── Agent.kt  (new domain model)
```

---

## Key Design Decisions

1. **Single database (ChatDatabase)**: All agent tables will be added to existing ChatDatabase to maintain consistency with existing pattern

2. **Foreign keys**: AgentEntity.chatUserId references UserEntity (already exists), AgentToChatEntity references both AgentEntity and ChatEntity

3. **JSON serialization**: Using kotlinx.serialization for AContext ↔ JSON string conversion in AgentContextMemory

4. **isCommon flag**: When isCommon=1, agent can be used in any chat without explicit binding; when isCommon=0, agent is only available in bound chats

---

## Migration Strategy

1. Create all new files in parallel where possible
2. Update ChatDatabase with new entities and DAOs
3. Create AgentCoreFeatureModule
4. Update AppComponent
5. Verify build compiles successfully

---

## Notes
- Follow existing naming conventions (Entity suffix for data models, domain models without suffix)
- Use constructor injection with @Inject
- Keep mappers as simple as possible with single responsibility
- All entity classes should be `internal` (following existing pattern)
