# Navigation 3 Guide (alpha05)

## ⚠️ CRITICAL: Correct Imports

**DO NOT use:**
- `androidx.navigation3.compose.*`

**USE these imports:**
- `import androidx.navigation3.runtime.NavEntry` (core)
- `import androidx.navigation3.ui.NavDisplay` (compose)

## Strict Rules

1. **DO NOT use** `composable<T>` - This is Navigation 2.x API
2. **DO NOT use** `toRoute<T>()` - This is Navigation 2.x API  
3. **DO NOT use** `NavHostController` - This is Navigation 2.x API
4. **DO NOT use** `rememberNavController()` - Deprecated
5. **DO NOT use** `androidx.navigation.compose.*` - This is v2
6. **DO NOT use** `androidx.navigation3.compose.*` - Wrong package

## Correct Imports
- `import androidx.navigation3.runtime.NavEntry`
- `import androidx.navigation3.ui.NavDisplay`

## Correct Navigation 3 API

- `NavDisplay(backStack = list) { route -> NavEntry(route) { ... } }` for display
- `navigator.navigateTo(Route)` for navigation
- Use `rememberSaveable` to preserve navigator state
- Parameter name is `backStack` (capital S)

## Dependencies

```kotlin
dependencies {
    implementation("androidx.navigation3:navigation3-runtime:1.1.0-alpha05")
    implementation("androidx.navigation3:navigation3-ui:1.1.0-alpha05")
}
```

## Implementation

### 1. AppNavigator (Core)

`core/navigation/AppNavigator.kt`:
```kotlin
package com.example.agentsarch.core.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.snapshots.SnapshotStateList

interface AppNavigator {
    val backStack: SnapshotStateList<Any>
    fun navigateTo(route: Any)
    fun goBack()
    fun resetTo(route: Any)
}

class AppNavigatorImpl(startRoute: Any) : AppNavigator {
    override val backStack = mutableStateListOf(startRoute)

    override fun navigateTo(route: Any) {
        backStack.add(route)
    }

    override fun goBack() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    override fun resetTo(route: Any) {
        backStack.clear()
        backStack.add(route)
    }

    companion object {
        fun saver(startRoute: Any) = Saver<AppNavigatorImpl, List<Any>>(
            save = { it.backStack.toList() },
            restore = { saved ->
                AppNavigatorImpl(startRoute).apply {
                    backStack.clear()
                    backStack.addAll(saved)
                }
            }
        )
    }
}
```

### 2. Routes (Serializable)

`core/navigation/Routes.kt`:
```kotlin
package com.example.agentsarch.core.navigation

import kotlinx.serialization.Serializable

@Serializable
data object ChatList

@Serializable
data class Chat(val chatId: String)

@Serializable
data object Settings
```

### 3. MainActivity

```kotlin
package com.example.agentsarch

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.example.agentsarch.core.navigation.AppNavigatorImpl
import com.example.agentsarch.core.navigation.Chat
import com.example.agentsarch.core.navigation.ChatList
import com.example.agentsarch.core.navigation.Settings
import com.example.agentsarch.features.chat.presentation.ChatScreen
import com.example.agentsarch.features.chatlist.presentation.ChatListScreen
import com.example.agentsarch.features.settings.presentation.SettingsScreen
import com.example.agentsarch.ui.theme.AgentsArchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AgentsArchTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    // navigator must be preserved with rememberSaveable
    val navigator = rememberSaveable(saver = AppNavigatorImpl.saver(ChatList)) {
        AppNavigatorImpl(ChatList)
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        // Parameter is backStack (capital S)
        NavDisplay(
            backStack = navigator.backStack,
            modifier = Modifier.padding(innerPadding)
        ) { route ->
            // NavEntry takes route directly in alpha05
            when (route) {
                is ChatList -> NavEntry(route) {
                    ChatListScreen(
                        onChatClick = { chatId -> navigator.navigateTo(Chat(chatId)) }
                    )
                }
                is Chat -> NavEntry(route) {
                    ChatScreen(
                        chatId = route.chatId,
                        onBack = { navigator.goBack() }
                    )
                }
                is Settings -> NavEntry(route) {
                    SettingsScreen()
                }
                else -> NavEntry(route) { /* fallback */ }
            }
        }
    }
}
```

## Key Differences

| Navigation 2.x | Navigation 3 (alpha05) |
|----------------|-------------------------|
| `rememberNavController()` | `rememberSaveable(saver = ...)` |
| `navController.navigate(route)` | `navigator.navigateTo(route)` |
| `navController.popBackStack()` | `navigator.goBack()` |
| `composable<T> { }` | `NavEntry(route) { }` |
| `toRoute<T>()` | Access route in when block |
| `NavHost` | `NavDisplay` |

## Best Practices

1. Always use `@Serializable` routes
2. Use `rememberSaveable` with Saver to preserve state
3. Use `backStack` parameter (capital S)
4. Keep navigation logic in AppNavigator interface
