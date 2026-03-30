# Reminder Core Feature Module

**Package:** `com.example.day.core.core_features.reminder`  
**Module:** `:app`  
**Type:** Core Feature (Clean Architecture)

Reminder system for scheduling and executing timed notifications.

## Overview

The Reminder feature provides:
- Scheduled reminder execution
- WorkManager-based background processing
- Room database storage
- Repository pattern for reminder management

## Purpose

The Reminder feature enables **time-based notifications** triggered by AI agents or user requests. When an agent uses the `SetReminderTool`, it creates a reminder that fires after a specified delay, potentially sending a notification or performing an action.

This is useful for:
- "Remind me to review this in 30 minutes"
- "Check back on this task tomorrow"
- Scheduled follow-ups on complex tasks

## Who Uses This Module

| Consumer | Purpose |
|----------|---------|
| `McpCoreFeature` | Uses `SetReminderTool` to create reminders from MCP |
| `AgentCoreFeature` | May create reminders via agent tools |
| Android System | Executes `ReminderWorker` via WorkManager at scheduled time |
| Notifications | Shows notification when reminder fires |

## Architecture Flow

```
┌─────────────────────────────────────────────────────────────────┐
│ McpCoreFeature / AgentCoreFeature                               │
│  └── SetReminderTool → ScheduleReminderUseCase                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ ReminderRepository (Room)                                       │
│  └── Stores reminder with trigger time                          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ ReminderScheduler (WorkManager)                                 │
│  └── Schedules ReminderWorker for exact trigger time            │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼ (at trigger time)
┌─────────────────────────────────────────────────────────────────┐
│ ReminderWorker (WorkManager)                                     │
│  └── ExecuteReminderUseCase → Shows notification / performs     │
└─────────────────────────────────────────────────────────────────┘
```

## Execution Flow

1. **Schedule**: `ScheduleReminderUseCase(triggerAtMillis, message)` creates a `Reminder` entity and schedules it via `ReminderScheduler`
2. **Store**: `ReminderRepositoryImpl` persists to Room database
3. **Schedule Work**: `ReminderSchedulerImpl` creates a `WorkRequest` with delay
4. **Execute**: When time arrives, `ReminderWorker` runs `ExecuteReminderUseCase`
5. **Notify**: The system shows a notification or performs the reminder action

## Key Classes

### Domain Layer

#### Models

- [`Reminder.kt`](app/src/main/java/com/example/day/core/core_features/reminder/domain/model/Reminder.kt) - Domain model

#### Repository

- [`ReminderRepository.kt`](app/src/main/java/com/example/day/core/core_features/reminder/domain/repository/ReminderRepository.kt) - Repository interface

#### Scheduler

- [`ReminderScheduler.kt`](app/src/main/java/com/example/day/core/core_features/reminder/domain/scheduler/ReminderScheduler.kt) - Scheduler interface

#### Use Cases

- [`ScheduleReminderUseCase.kt`](app/src/main/java/com/example/day/core/core_features/reminder/domain/usecase/ScheduleReminderUseCase.kt) - Schedule a reminder
- [`ExecuteReminderUseCase.kt`](app/src/main/java/com/example/day/core/core_features/reminder/domain/usecase/ExecuteReminderUseCase.kt) - Execute reminder logic

### Data Layer

#### Repository Implementation

- [`ReminderRepositoryImpl.kt`](app/src/main/java/com/example/day/core/core_features/reminder/data/ReminderRepositoryImpl.kt) - Room-based implementation
- [`ReminderSchedulerImpl.kt`](app/src/main/java/com/example/day/core/core_features/reminder/data/ReminderSchedulerImpl.kt) - WorkManager scheduler

#### Database

- [`ReminderEntity.kt`](app/src/main/java/com/example/day/core/core_features/reminder/data/local/model/ReminderEntity.kt) - Room entity
- [`ReminderDao.kt`](app/src/main/java/com/example/day/core/core_features/reminder/data/local/dao/ReminderDao.kt) - DAO interface
- [`ReminderMapper.kt`](app/src/main/java/com/example/day/core/core_features/reminder/data/local/mapper/ReminderMapper.kt) - Entity mapper

#### Worker

- [`ReminderWorker.kt`](app/src/main/java/com/example/day/core/core_features/reminder/data/worker/ReminderWorker.kt) - WorkManager worker
- [`ReminderWorkConstants.kt`](app/src/main/java/com/example/day/core/core_features/reminder/data/worker/ReminderWorkConstants.kt) - Work constants

### DI

- [`ReminderCoreFeatureModule.kt`](app/src/main/java/com/example/day/core/core_features/reminder/di/ReminderCoreFeatureModule.kt) - Dagger module

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Domain Layer                           │
│  ┌───────────────┐  ┌───────────────┐  ┌────────────────┐ │
│  │   Reminder    │  │   Reminder   │  │    Execute     │ │
│  │   (model)    │  │  Repository  │  │ ReminderUseCase│ │
│  └───────────────┘  └───────────────┘  └────────────────┘ │
│                      └───────────────┘                      │
│                      ReminderScheduler                      │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                       Data Layer                            │
│  ┌───────────────┐  ┌───────────────┐  ┌────────────────┐ │
│  │ ReminderImpl  │  │ ReminderDao  │  │ ReminderWorker │ │
│  │  (Room)      │  │   (Room)     │  │ (WorkManager)  │ │
│  └───────────────┘  └───────────────┘  └────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## Usage

### Scheduling a Reminder

```kotlin
val scheduleReminderUseCase: ScheduleReminderUseCase

scheduleReminderUseCase(
    triggerAtMillis = System.currentTimeMillis() + 3600000, // 1 hour
    message = "Time to check something!"
)
```

### Executing a Reminder

```kotlin
val executeReminderUseCase: ExecuteReminderUseCase

executeReminderUseCase(reminderId)
```

## Module Structure

```
core/core_features/reminder/
├── data/
│   ├── ReminderRepositoryImpl.kt
│   ├── ReminderSchedulerImpl.kt
│   ├── local/
│   │   ├── dao/
│   │   │   └── ReminderDao.kt
│   │   ├── mapper/
│   │   │   └── ReminderMapper.kt
│   │   └── model/
│   │       └── ReminderEntity.kt
│   └── worker/
│       ├── ReminderWorker.kt
│       └── ReminderWorkConstants.kt
├── di/
│   └── ReminderCoreFeatureModule.kt
└── domain/
    ├── ReminderConstants.kt
    ├── model/
    │   └── Reminder.kt
    ├── repository/
    │   └── ReminderRepository.kt
    ├── scheduler/
    │   └── ReminderScheduler.kt
    └── usecase/
        ├── ExecuteReminderUseCase.kt
        └── ScheduleReminderUseCase.kt
```
