# UIKit Module

**Package:** `com.example.day.core.ui.uikit`  
**Module:** `:app`  
**Type:** Android UI Component Library

Reusable Compose UI components for chat-based interfaces.

## Overview

The UIKit provides:
- Chat UI components (message bubbles, lists, bars)
- Dialog components
- Settings components
- Reusable Compose modifiers and utilities

## Purpose

The UIKit module provides **reusable, themeable Compose components** specifically designed for chat-based interfaces. It follows Material 3 design principles and ensures **visual consistency** across all features.

## Who Uses This Module

| Consumer | Purpose |
|----------|---------|
| `ConsoleFeature` | Uses `ChatListView`, `ChatBarView`, `ChatMessageView` |
| `ChatsFeature` | Uses chat list components |
| `UserSettingsFeature` | Uses dialog components |
| All Features | Uses `ConfirmDialog`, `GroupEditDialog` |

## Component Hierarchy

```
ChatListView (Scrollable container)
├── ChatMessageBubble (Message wrapper with styling)
│   ├── MessageScaffold (Layout structure)
│   │   ├── AvatarView (User/AI avatar)
│   │   ├── ChatMessageView (Text content)
│   │   └── MessageStatusIndicator (Delivery status)
│   ├── MessageCopyButton (Copy to clipboard)
│   └── ButtonGroup (Action buttons)
└── ChatBarView (Input area)
    └── ChatSendButton (Send button)
```

## Chat UI Components

### Message Display

| Component | Purpose |
|-----------|---------|
| `ChatListView` | Scrollable container for messages, auto-scrolls to bottom |
| `ChatMessageView` | Displays message text with proper formatting |
| `ChatMessageBubble` | Container that applies bubble styling (colors, shapes) |
| `MessageScaffold` | Layout structure: avatar + content + status |
| `AvatarView` | Circular avatar with initials or icon |
| `MessageStatusIndicator` | Shows delivery status (sent, delivered, error) |
| `MessageCopyButton` | Copy message content to clipboard |
| `ButtonGroup` | Displays interactive buttons from bot messages |

### Input Components

| Component | Purpose |
|-----------|---------|
| `ChatBarView` | Text input field with send button |
| `ChatSendButton` | Animated send/stop button |

### Dialogs

| Component | Purpose |
|-----------|---------|
| `ConfirmDialog` | Yes/No confirmation with customizable text |
| `GroupEditDialog` | Create/edit chat groups with name and color |

## Styling System

Components use a dedicated styling system in `chat/` package:

| File | Purpose |
|------|---------|
| `ChatColorScheme` | User/AI bubble colors, text colors |
| `ChatTypography` | Message text styles |
| `ChatShapes` | Bubble corner radii |

## Key Components

### Chat Components (`chat/`)

#### Message List (`chat/list/`)

- [`ChatListView.kt`](app/src/main/java/com/example/day/core/ui/uikit/chat/list/ChatListView.kt) - Scrollable message list
- [`ChatMessageView.kt`](app/src/main/java/com/example/day/core/ui/uikit/chat/list/ChatMessageView.kt) - Single message display
- [`ChatMessageBubble.kt`](app/src/main/java/com/example/day/core/ui/uikit/chat/list/ChatMessageBubble.kt) - Message bubble container
- [`MessageScaffold.kt`](app/src/main/java/com/example/day/core/ui/uikit/chat/list/MessageScaffold.kt) - Message layout scaffold
- [`MessageButton.kt`](app/src/main/java/com/example/day/core/ui/uikit/chat/list/MessageButton.kt) - Clickable message button
- [`MessageCopyButton.kt`](app/src/main/java/com/example/day/core/ui/uikit/chat/list/MessageCopyButton.kt) - Copy to clipboard
- [`MessageStatusIndicator.kt`](app/src/main/java/com/example/day/core/ui/uikit/chat/list/MessageStatusIndicator.kt) - Delivery status
- [`AvatarView.kt`](app/src/main/java/com/example/day/core/ui/uikit/chat/list/AvatarView.kt) - User/AI avatar
- [`ButtonGroup.kt`](app/src/main/java/com/example/day/core/ui/uikit/chat/list/ButtonGroup.kt) - Button group layout

**Models:**
- [`ChatMessageUiModel.kt`](app/src/main/java/com/example/day/core/ui/uikit/chat/list/model/ChatMessageUiModel.kt)
- [`ChatListUiEvent.kt`](app/src/main/java/com/example/day/core/ui/uikit/chat/list/model/ChatListUiEvent.kt)
- [`UiMessageStatus.kt`](app/src/main/java/com/example/day/core/ui/uikit/chat/list/model/UiMessageStatus.kt)
- [`ChatMessageUiType.kt`](app/src/main/java/com/example/day/core/ui/uikit/chat/list/model/ChatMessageUiType.kt)

#### Chat Bar (`chat/bar/`)

- [`ChatBarView.kt`](app/src/main/java/com/example/day/core/ui/uikit/chat/bar/ChatBarView.kt) - Input bar with send button
- [`ChatSendButton.kt`](app/src/main/java/com/example/day/core/ui/uikit/chat/bar/ChatSendButton.kt) - Send button

**Models:**
- [`ChatBarUiModel.kt`](app/src/main/java/com/example/day/core/ui/uikit/chat/bar/model/ChatBarUiModel.kt)
- [`ChatBarUiEvent.kt`](app/src/main/java/com/example/day/core/ui/uikit/chat/bar/model/ChatBarUiEvent.kt)
- [`ChatSendButtonType.kt`](app/src/main/java/com/example/day/core/ui/uikit/chat/bar/model/ChatSendButtonType.kt)

#### Chat Styling

- [`ChatColorScheme.kt`](app/src/main/java/com/example/day/core/ui/uikit/chat/ChatColorScheme.kt) - Color scheme
- [`ChatTypography.kt`](app/src/main/java/com/example/day/core/ui/uikit/chat/ChatTypography.kt) - Text styles
- [`ChatShapes.kt`](app/src/main/java/com/example/day/core/ui/uikit/chat/ChatShapes.kt) - Corner radii

### Components

- [`SettingsTextFields.kt`](app/src/main/java/com/example/day/core/ui/uikit/components/SettingsTextFields.kt) - Settings input fields

#### LTM Components (`components/ltm/`)

- [`LongTermFactsListView.kt`](app/src/main/java/com/example/day/core/ui/uikit/components/ltm/LongTermFactsListView.kt) - Facts list
- [`LongTermFactUiItem.kt`](app/src/main/java/com/example/day/core/ui/uikit/components/ltm/LongTermFactUiItem.kt) - Single fact item

### Dialogs (`dialogs/`)

#### Confirm Dialog (`dialogs/confirm/`)

- [`ConfirmDialog.kt`](app/src/main/java/com/example/day/core/ui/uikit/dialogs/confirm/ConfirmDialog.kt) - Confirmation dialog

#### Group Dialog (`dialogs/group/`)

- [`GroupEditDialog.kt`](app/src/main/java/com/example/day/core/ui/uikit/dialogs/group/GroupEditDialog.kt) - Group edit dialog
- [`GroupEditDialogState.kt`](app/src/main/java/com/example/day/core/ui/uikit/dialogs/group/GroupEditDialogState.kt)

## Usage

### Chat Message

```kotlin
ChatMessageView(
    message = ChatMessageUiModel(
        id = "msg_1",
        content = "Hello!",
        role = "assistant",
        timestamp = System.currentTimeMillis()
    ),
    onCopy = { /* handle copy */ },
    onButtonClick = { button -> /* handle button */ }
)
```

### Chat List

```kotlin
ChatListView(
    messages = messagesList,
    onCopy = { id -> },
    onButtonClick = { id, action -> }
)
```

### Input Bar

```kotlin
ChatBarView(
    text = inputText,
    onTextChange = { text = it },
    onSend = { sendMessage() },
    buttonType = ChatSendButtonType.SEND
)
```

## Module Structure

```
core/ui/uikit/
├── uikit/
│   ├── chat/
│   │   ├── ChatColorScheme.kt
│   │   ├── ChatShapes.kt
│   │   ├── ChatTypography.kt
│   │   ├── bar/
│   │   │   ├── ChatBarView.kt
│   │   │   ├── ChatSendButton.kt
│   │   │   └── model/
│   │   │       ├── ChatBarUiEvent.kt
│   │   │       ├── ChatBarUiModel.kt
│   │   │       └── ChatSendButtonType.kt
│   │   └── list/
│   │       ├── AvatarView.kt
│   │       ├── ButtonGroup.kt
│   │       ├── ChatListView.kt
│   │       ├── ChatMessageBubble.kt
│   │       ├── ChatMessageView.kt
│   │       ├── MessageButton.kt
│   │       ├── MessageCopyButton.kt
│   │       ├── MessageScaffold.kt
│   │       ├── MessageStatusIndicator.kt
│   │       └── model/
│   │           ├── ChatListUiEvent.kt
│   │           ├── ChatListUiModel.kt
│   │           ├── ChatMessageUiModel.kt
│   │           ├── ChatMessageUiType.kt
│   │           └── UiMessageStatus.kt
│   ├── components/
│   │   ├── SettingsTextFields.kt
│   │   └── ltm/
│   │       ├── LongTermFactUiItem.kt
│   │       └── LongTermFactsListView.kt
│   └── dialogs/
│       ├── confirm/
│       │   └── ConfirmDialog.kt
│       └── group/
│           ├── GroupEditDialog.kt
│           └── GroupEditDialogState.kt
```
