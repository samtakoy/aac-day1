# AGENTS.md - AI Assistant Guidelines for Day Project

> Open standard for code assistants: https://agents.md

## Project Overview

**Project Name:** Day - AI Chat Application  
**Platform:** Android  
**Language:** Kotlin  
**UI Framework:** Jetpack Compose + Material 3

This is an AI-powered chat application featuring multi-agent architecture with LLM integration, featuring a sophisticated worker system for different conversation modes.

---

## Tech Stack

| Component | Technology                    | Important Notes |
|-----------|-------------------------------|-----------------|
| **Language** | Kotlin 2.3.x                  | |
| **UI** | Jetpack Compose               | Material 3 design |
| **DI** | Dagger                        | **NOT Hilt** - manual DI |
| **Database** | Room                          | Version 3+ with relations |
| **Networking** | Ktor Client                   | **NOT Retrofit** - OkHttp engine |
| **Serialization** | Kotlinx Serialization         | JSON handling |
| **Async** | Coroutines + Flow             | |
| **Immutable Collections** | kotlinx.collections.immutable | |
| **Annotation Processing** | KSP                           | Not KAPT |

### Dependencies (Critical)

```
// ✅ CORRECT
implementation("io.ktor:ktor-client-okhttp")
implementation("androidx.navigation3:navigation3-runtime:1.1.0-alpha05")
implementation("androidx.navigation3:navigation3-ui:1.1.0-alpha05")

// ❌ WRONG - DO NOT USE
implementation("io.ktor:ktor-client-ios")        // iOS only
implementation("androidx.navigation3:compose")   // Navigation 2.x
implementation("com.squareup.retrofit2:retrofit")
```

---

## Environment
JAVA_HOME=/Users/samtakot/.sdkman/candidates/java/17.0.12-tem

Call before build:
export JAVA_HOME=/Users/samtakot/.sdkman/candidates/java/17.0.12-tem

## Architecture

### Feature-Based Structure

```
app/src/main/java/com/example/day/
├── app/                           # Application level
│   ├── di/
│   │   └── AppComponent.kt       # Main Dagger component
│   └── MyApp.kt
├── core/                         # Core modules (shared)
│   ├── core_features/           # Domain features
│   │   ├── agent/               # AI Agent system
│   │   ├── chat/                # Chat management
│   │   └── llm/                 # LLM integration
│   ├── di/                      # Core DI (NetworkModule)
│   ├── ui/                      # Theme + UI kit
│   └── feature_entries/         # Navigation entry points
└── features/                     # Feature UI modules
    ├── chats/                    # Chat list screen
    ├── group_choice/            # Group selection
    └── console/                 # Main AI console
```

### Clean Architecture Layers

Each feature follows:
- **Domain Layer**: Models, Use Cases, Repository interfaces, Tools interfaces
- **Data Layer**: Repository implementations, Room database, Ktor client, Mappers
- **UI Layer**: Compose screens, ViewModels, UiModels

### Key Principle: Layer Separation

| Layer | Model Suffix | Location |
|-------|--------------|----------|
| Data | `*Entity` | `*/data/local/model/` |
| Domain | No suffix | `*/domain/model/` |
| Presentation | `*UiModel` | `*/ui/*/model/` |

---

## Dependency Injection

### Dagger Components Structure

```
AppComponent (Singleton)
├── implements: ConsoleFeatureDeps, ChatsFeatureDeps, GroupChoiceFeatureDeps
├── includes: NetworkModule, ChatCoreFeatureModule, LlmCoreFeatureModule, AgentCoreFeatureModule
└── provides: ChatRepository, AgentRepository, LlmRepository, all UseCases

ConsoleFeatureComponent (ConsoleFeatureScope)
├── dependencies: ConsoleFeatureDeps
├── modules: ConsoleFeatureModule
└── provides: ConsoleViewModel.Factory, TalkDelegates, Workers
```

### DI Organization Rules

| Type | Location | Example |
|------|----------|---------|
| Core Infrastructure | `core/di/` | Database, DAOs, Network, Json |
| Feature Repository | `core_features/X/data/di/` | AgentRepository, ChatRepository |
| Feature Environment | `core_features/X/data/di/` | AgentEnvironment |
| UseCase | `features/X/di/` | ChatUseCase |

### ViewModel Factory Pattern (CRITICAL)

Since using **manual Dagger** (not Hilt), all ViewModels MUST use the Factory pattern:

```kotlin
class ChatViewModelImpl(
    private val createChatUseCase: CreateChatUseCase,
    private val getChatsByGroupUseCase: GetChatsByGroupUseCase,
    private val groupId: Logn // External parameter
) : ViewModel(), ChatViewModel {
    
    // Factory as inner class
    class Factory @Inject constructor(
        private val createChatUseCase: CreateChatUseCase,  // Injected
        private val getChatsByGroupUseCase: GetChatsByGroupUseCase  // Injected
    ): ViewModelProvider.Factory {
        override fun <T : ViewModel> create(
            modelClass: Class<T>,
            extras: CreationExtras
        ): T {
            val groupId = extras[GROUP_ID_KEY] ?: error("GROUP_ID_KEY not found in extras")
            return ChatViewModelImpl(
                createChatUseCase,
                getChatsByGroupUseCase,
                groupId = groupId
            ) as T
        }
    }
}
```

**Usage in Compose:**
```kotlin
@Composable
override fun EntryPoint(groupId: Long, modifier: Modifier, onNavigateBack: (() -> Unit)?) {
    val appComponent = LocalAppComponent.current

    val featureComponent: ChatsFeatureComponent = retain {
        DaggerChatsFeatureComponent.factory().create(appComponent)
    }

    val extras = remember(groupId) {
        MutableCreationExtras().apply {
            set(ChatsViewModelImpl.GROUP_ID_KEY, groupId)
        }
    }

    val viewModel: ChatViewModelImpl = viewModel(
        key = "${ChatsViewModelImpl::class.qualifiedName}_$groupId",
        factory = featureComponent.getViewModelFactory(),
        extras = extras
    )

    ChatsScreen(
        viewModel = viewModel,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}
```

---

## Coding Standards

### 1. Kotlinx Serialization

**ALWAYS use @SerialName for all network models:**

```kotlin
@Serializable
data class ChatMessage(
    @SerialName("role")
    val role: String,
    
    @SerialName("content") 
    val content: String,
    
    @SerialName("name")
    val name: String? = null
)
```

### 2. Room Database

**ALWAYS use explicit table and column names:**

```kotlin
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    
    @ColumnInfo(name = "chat_id")
    val chatId: String,
    
    @ColumnInfo(name = "role")
    val role: String
)
```

### 3. Constants

**NEVER use magic constants - use objects:**

```kotlin
object AgentConstants {
    const val DEFAULT_NAME = "Assistant"
    const val DEFAULT_MODEL = "openai/gpt-4o-mini"
    
    object Role {
        const val SYSTEM = "system"
        const val USER = "user"
        const val ASSISTANT = "assistant"
    }
}
```

### 4. Immutable Collections

**Use ImmutableList in Composables:**

```kotlin
// ❌ Wrong
@Composable
fun MyComponent(items: List<Item>)

// ✅ Correct
@Composable
fun MyComponent(items: ImmutableList<Item>)

// Convert in ViewModel:
val uiItems = domainItems.toImmutableList()
```

### 5. Naming Conventions

| Type | Convention | Example |
|------|------------|---------|
| Table names | snake_case | `chat_messages` |
| Column names | snake_case | `chat_id` |
| Kotlin properties | camelCase | `chatId` |
| JSON fields | snake_case | `chat_id` |
| Interfaces | `<Name>Provider`, `<Name>Repository` | `AgentRepository` |
| Implementations | `<Name>ProviderImpl`, `<Name>RepositoryImpl` | `AgentRepositoryImpl` |

---

## Navigation

### Navigation 3 (alpha05)

**CRITICAL - Correct Imports:**

```kotlin
// ✅ CORRECT
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay

// ❌ WRONG
import androidx.navigation3.compose.*
import androidx.navigation.compose.*
```

**Navigation Implementation:**

```kotlin
@Composable
fun MainApp() {
    val navigator = rememberSaveable(saver = AppNavigatorImpl.saver(ChatList)) {
        AppNavigatorImpl(ChatList)
    }

    Scaffold { innerPadding ->
        NavDisplay(
            backStack = navigator.backStack,  // capital S
            modifier = Modifier.padding(innerPadding)
        ) { route ->
            when (route) {
                is ChatList -> NavEntry(route) { ChatListScreen(...) }
                is Chat -> NavEntry(route) { ChatScreen(...) }
            }
        }
    }
}
```

### Navigation Flow

```
GroupChoiceFeature → select ChatGroup
         ↓
ChatsFeature → display chats in group, select or create chat
         ↓
ConsoleFeature → chat with LLM/agents
```

---

## Key Patterns

### 1. Tools Pattern

Domain layer interfaces for Worker dependencies:

```kotlin
// Agent domain
interface AgentTools {
    fun getOrCreateAgent(systemName, chatId, isCommonAgent)
    fun getContext(agentId)
    fun saveContext(agentId, context)
    fun clearAgentContext(agentId)
}

// Chat domain  
interface ChatTools {
    fun createChat(chatTitle, groupId)
    fun getOrCreateChat(chatTitle, groupId)
    fun addBotMessage(chatId, message)
}
```

### 2. Workers Pattern

Workers used for user command handling and agents calling:

| Worker | Command | Purpose |
|--------|---------|---------|
| `SimpleWorker` | `@@simple` | Direct execution |
| `StepWorker` | `@@steps` | Step-by-step reasoning |
| `PromptWorker` | `@@prompt` | Prompt generation |
| `TeamWorker` | `@@team` | Multi-agent collaboration |
| `TalkWorker` | `@@talk` | Context-aware conversation |
| `CompareWorker` | `@@compare` | Compare approaches |

### 3. Feature Entry Pattern

Navigation between features via FeatureEntry interfaces:

```kotlin
interface ConsoleFeatureEntry {
    fun createScreen(...):Composable
}
```

### 4. Room Relations

Use Room relations for loading related entities:

```kotlin
data class ChatWithGroupAndSettings(
    @Embedded val chat: ChatEntity,
    @Relation(...)
    val group: ChatGroupEntity,
    @Relation(...)
    val settings: ChatSettingsEntity
)
```

---

## Database Schema

### Tables

| Table | Purpose |
|-------|---------|
| `users` | User entities |
| `chats` | Chat entities |
| `messages` | Message entities |
| `chat_groups` | Chat group entities |
| `chat_types` | Chat type entities |
| `chat_settings` | Per-chat settings (modelSettings as JSON) |
| `agents` | AI agent entities |
| `agent_to_chat` | Agent-to-chat bindings |
| `agent_context_memory` | Agent context storage |

---

## Important Notes

1. **NO Hilt** - Using manual Dagger with scoped components
2. **NO Retrofit** - Using Ktor Client with OkHttp engine
3. **NO Navigation 2.x** - Using Navigation 3 (alpha05)
4. **Internal visibility** for data layer implementations
5. **Explicit annotations** - @SerialName, @ColumnInfo always
6. **Constants in objects** - No magic numbers
7. **Immutable collections** - Use kotlinx.collections.immutable
8. **No Data and Ui Models in domain layer** - Use data mappers in data layer and UI mappers in presentation layer

---

## Development Workflow

1. Follow Clean Architecture (domain/data/ui separation)
2. Use ViewModel Factory pattern for all ViewModels
3. Add @SerialName to all Kotlinx Serialization fields
4. Add @ColumnInfo to all Room entity fields
5. Use constants objects instead of magic values
6. Keep dependencies in correct DI layer locations
7. Update plans/TODO.md when tasks complete
8. Verify build compiles after each change
