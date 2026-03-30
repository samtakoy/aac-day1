# App Module

**Package:** `com.example.day.app`  
**Module:** `:app`  
**Type:** Android Application

Main Android application entry point and dependency injection configuration.

## Overview

The App module is the root package containing:
- Application class (`MyApp`)
- Dagger component (`AppComponent`)
- Main Activity (`MainActivity`)

## Purpose

The App module serves as the **application root** and **DI container** for the entire Android application. It:

1. **Initializes Dagger DI** - Creates and provides the `AppComponent` singleton that holds all dependencies
2. **Provides Feature Entries** - Exposes feature entry points via `FeatureEntryProvider` interface
3. **Wires Core Features** - Includes all core feature modules (chat, memory, agent, llm, mcp, reminder)
4. **Wires Feature Modules** - Includes all UI feature modules (console, chats, group_choice, user_settings, mcp_settings)
5. **Sets up Composition Local** - Provides `LocalAppComponent` for Compose hierarchy access

## Who Uses This Module

| Consumer | Purpose |
|----------|---------|
| `MainActivity` | Accesses `AppComponent` to set up navigation and features |
| `MyApp` | Initializes Dagger component at app startup |
| Android Framework | Creates `Application` instance on launch |

## How It Works

### 1. App Startup Sequence

```
1. Android creates MyApp (Application)
2. MyApp creates AppComponent via DaggerAppComponent.factory().create()
3. AppComponent includes all modules, binding them together
4. MainActivity reads LocalAppComponent.current to access dependencies
5. MainActivity navigates to features using appComponent.getXxxFeatureEntry()
```

### 2. Feature Entry Pattern

Features don't instantiate their own dependencies. Instead:
- `AppComponent` implements `*FeatureDeps` interfaces (e.g., `ConsoleFeatureDeps`)
- Features access deps through scoped `*FeatureComponent` instances
- Entry points are obtained from `AppComponent` via getter methods

```kotlin
// AppComponent provides:
fun getConsoleFeatureEntry(): ConsoleFeatureEntry
fun getChatFeatureEntry(): ChatsFeatureEntry
fun getGroupChoiceFeatureEntry(): GroupChoiceFeatureEntry
// etc.
```

### 3. Composition Local for DI

```kotlin
val LocalAppComponent = staticCompositionLocalOf<AppComponent> {
    error("No AppComponent provided")
}
```

This allows any Composable in the hierarchy to access `AppComponent` via `LocalAppComponent.current`.

## Key Classes

### [`MyApp.kt`](app/src/main/java/com/example/day/app/MyApp.kt)

Application class that initializes Dagger dependency injection.

```kotlin
class MyApp : Application() {
    val appComponent: AppComponent by lazy {
        DaggerAppComponent.factory().create(applicationContext)
    }
}
```

### [`AppComponent.kt`](app/src/main/java/com/example/day/app/di/AppComponent.kt)

Root Dagger component that:
- Includes all core feature modules
- Includes all feature modules
- Provides feature entry points
- Implements feature dependency interfaces

```kotlin
@Singleton
@Component(modules = [
    NetworkModule::class,
    ChatCoreFeatureModule::class,
    MemoryCoreFeatureModule::class,
    AgentCoreFeatureModule::class,
    ReminderCoreFeatureModule::class,
    ConsoleFeatureApiModule::class,
    ChatsFeatureApiModule::class,
    GroupChoiceFeatureApiModule::class,
    UserSettingsFeatureApiModule::class,
    LlmCoreFeatureModule::class,
    McpCoreFeatureModule::class,
    McpSettingsFeatureApiModule::class,
    AppSettingsModule::class
])
interface AppComponent : FeatureEntryProvider, 
    ConsoleFeatureDeps, ChatsFeatureDeps,
    GroupChoiceFeatureDeps, UserSettingsFeatureDeps, McpSettingsFeatureDeps
```

### Composition Local

```kotlin
val LocalAppComponent = staticCompositionLocalOf<AppComponent> {
    error("No AppComponent provided")
}
```

Used to provide `AppComponent` through Compose hierarchy.

### [`MainActivity.kt`](app/src/main/java/com/example/day/MainActivity.kt)

Main activity that sets up:
- Edge-to-edge display
- Compose UI with theme
- Bottom navigation
- Screen state management

**Navigation Flow:**
```
GroupChoice → Chats (per group)
Settings (MCP servers)
```

### AppSettingsModule

[`AppSettingsModule.kt`](app/src/main/java/com/example/day/app/di/AppSettingsModule.kt) - Provides app-level settings

## Dependency Injection

### Core Feature Modules

| Module | Provides |
|--------|----------|
| `NetworkModule` | HTTP client, JSON |
| `ChatCoreFeatureModule` | Chat repository, use cases |
| `MemoryCoreFeatureModule` | Memory repository, RAG |
| `AgentCoreFeatureModule` | AI agent, workers |
| `ReminderCoreFeatureModule` | Reminder system |
| `LlmCoreFeatureModule` | LLM integration |
| `McpCoreFeatureModule` | MCP client |

### Feature Modules

| Module | Feature |
|--------|---------|
| `ConsoleFeatureApiModule` | AI chat console |
| `ChatsFeatureApiModule` | Chat list |
| `GroupChoiceFeatureApiModule` | Group selection |
| `UserSettingsFeatureApiModule` | User preferences |
| `McpSettingsFeatureApiModule` | MCP server settings |

## Usage

### Accessing AppComponent in Compose

```kotlin
@Composable
fun MyScreen() {
    val appComponent = LocalAppComponent.current
    
    val featureEntry = appComponent.getChatFeatureEntry()
    featureEntry.EntryPoint(...)
}
```

### Creating Feature Screens

Features access dependencies through their `*FeatureDeps` interfaces:

```kotlin
interface ConsoleFeatureDeps {
    // Dependencies available to Console feature
    val llmRepository: LlmRepository
    val agentRepository: AgentRepository
    // ...
}
```

## Module Structure

```
app/src/main/java/com/example/day/
├── MainActivity.kt              # Main activity
└── app/
    ├── MyApp.kt                # Application class
    └── di/
        ├── AppComponent.kt     # Dagger component
        └── AppSettingsModule.kt # App settings DI
```
