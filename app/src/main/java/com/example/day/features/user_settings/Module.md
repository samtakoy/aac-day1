# User Settings Feature Module

**Package:** `com.example.day.features.user_settings`  
**Module:** `:app`  
**Type:** Android Feature Module

User profile management feature for creating, editing, and managing user profiles and facts.

## Overview

The User Settings feature provides:
- User profile creation and management
- Profile facts (key-value attributes)
- Profile binding to chats
- Avatar generation

## Purpose

The User Settings feature enables users to **create and manage personal profiles** that can be attached to chat conversations. Profiles contain:
- **Facts** - Key-value attributes that describe the user (e.g., "name=John", "language=English")
- **Avatar** - AI-generated avatar image based on user description
- **Categories** - Facts can be organized by category

When a profile is bound to a chat, the AI agent can access these facts to provide **personalized responses** tailored to the user's context.

## Who Uses This Module

| Consumer | Purpose |
|----------|---------|
| `MainActivity` | Accesses via `appComponent.getUserSettingsFeatureEntry()` |
| `ConsoleFeatureEntry` | Binds profiles to chat context via `BindUserProfileUseCase` |
| `MemoryCoreFeature` | Stores profiles and facts via `UserProfileRepository` |
| AI Agents | Read profile facts through `UserProfileMemoryProvider` |

## Architecture Flow

```
┌─────────────────────────────────────────────────────────────────┐
│ UserSettingsScreen (UI)                                          │
│  ├── CreateProfileDialog  → CreateUserProfileUseCase           │
│  ├── FactEditDialog       → UpsertFactWithCategoryUseCase      │
│  └── SelectProfileDialog  → BindUserProfileUseCase             │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ UserSettingsViewModel                                            │
│  └── Manages UI state, exposes flows for profiles & facts      │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ Memory Core Feature (UserProfileRepository)                      │
│  ├── UserProfileDao (Room)                                       │
│  └── LongTermMemoryFactDao (Room)                               │
└─────────────────────────────────────────────────────────────────┘
```

## Key Use Cases

| UseCase | Purpose |
|---------|---------|
| `CreateUserProfileUseCase` | Creates a new profile with name and optional avatar |
| `GetCurrentUserProfileUseCase` | Gets the currently bound profile for a chat |
| `GetAllProfilesUseCase` | Lists all available profiles |
| `BindUserProfileUseCase` | Binds a profile to a chat context |
| `UnbindUserProfileUseCase` | Removes profile binding |
| `UpsertFactWithCategoryUseCase` | Adds or updates a fact in a category |
| `DeleteProfileFactUseCase` | Removes a fact |
| `GenerateProfileAvatarUseCase` | Uses LLM to generate an avatar description |

## Key Classes

### Entry Point

- [`UserSettingsFeatureEntry.kt`](app/src/main/java/com/example/day/features/user_settings/api/UserSettingsFeatureEntry.kt) - Feature entry interface

```kotlin
interface UserSettingsFeatureEntry {
    @Composable
    fun EntryPoint(modifier: Modifier = Modifier, onDismiss: () -> Unit)
}
```

- [`UserSettingsFeatureEntryImpl.kt`](app/src/main/java/com/example/day/features/user_settings/impl/UserSettingsFeatureEntryImpl.kt) - Implementation

### UI

- [`UserSettingsScreen.kt`](app/src/main/java/com/example/day/features/user_settings/impl/ui/UserSettingsScreen.kt) - Main settings screen

**Components:**
- [`CreateProfileDialog.kt`](app/src/main/java/com/example/day/features/user_settings/impl/ui/components/CreateProfileDialog.kt)
- [`FactEditDialog.kt`](app/src/main/java/com/example/day/features/user_settings/impl/ui/components/FactEditDialog.kt)
- [`SelectProfileDialog.kt`](app/src/main/java/com/example/day/features/user_settings/impl/ui/components/SelectProfileDialog.kt)

### ViewModel

- [`UserSettingsViewModel.kt`](app/src/main/java/com/example/day/features/user_settings/impl/ui/viewmodel/UserSettingsViewModel.kt) - Interface
- [`UserSettingsViewModelImpl.kt`](app/src/main/java/com/example/day/features/user_settings/impl/ui/viewmodel/UserSettingsViewModelImpl.kt) - Implementation

### Dependency Injection

- [`UserSettingsFeatureComponent.kt`](app/src/main/java/com/example/day/features/user_settings/impl/di/UserSettingsFeatureComponent.kt)
- [`UserSettingsFeatureDeps.kt`](app/src/main/java/com/example/day/features/user_settings/impl/di/UserSettingsFeatureDeps.kt)
- [`UserSettingsFeatureModule.kt`](app/src/main/java/com/example/day/features/user_settings/impl/di/UserSettingsFeatureModule.kt)

## Feature Dependencies

The feature requires these dependencies from core:

| UseCase | Purpose |
|---------|---------|
| `GetCurrentUserProfileUseCase` | Get currently selected profile |
| `GetAllProfilesUseCase` | List all profiles |
| `GetProfileFactsFlowUseCase` | Observe profile facts |
| `CreateUserProfileUseCase` | Create new profile |
| `BindUserProfileUseCase` | Bind profile to context |
| `UnbindUserProfileUseCase` | Unbind profile |
| `UpsertFactWithCategoryUseCase` | Add/update fact |
| `DeleteProfileFactUseCase` | Delete fact |
| `GenerateProfileAvatarUseCase` | Generate avatar via LLM |
| `UpdateProfileAvatarUseCase` | Update avatar |

## Usage

### Navigation

```kotlin
val userSettingsEntry = appComponent.getUserSettingsFeatureEntry()
userSettingsEntry.EntryPoint(
    modifier = Modifier,
    onDismiss = { /* handle dismiss */ }
)
```

## Module Structure

```
features/user_settings/
├── api/
│   └── UserSettingsFeatureEntry.kt      # Entry interface
├── impl/
│   ├── UserSettingsFeatureEntryImpl.kt   # Entry implementation
│   ├── di/
│   │   ├── UserSettingsFeatureComponent.kt
│   │   ├── UserSettingsFeatureDeps.kt    # Dependencies
│   │   ├── UserSettingsFeatureModule.kt
│   │   └── UserSettingsFeatureScope.kt
│   └── ui/
│       ├── UserSettingsScreen.kt         # Main screen
│       ├── components/
│       │   ├── CreateProfileDialog.kt
│       │   ├── FactEditDialog.kt
│       │   └── SelectProfileDialog.kt
│       └── viewmodel/
│           ├── UserSettingsViewModel.kt
│           └── UserSettingsViewModelImpl.kt
```
