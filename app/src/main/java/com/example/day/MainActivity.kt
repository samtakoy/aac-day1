package com.example.day

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.day.app.MyApp
import com.example.day.app.di.LocalAppComponent
import com.example.day.core.ui.theme.Day1Theme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* результат не используем */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val appComponent = (application as MyApp).appComponent

        enableEdgeToEdge()
        setContent {
            Day1Theme {
                CompositionLocalProvider(LocalAppComponent provides appComponent) {
                    MainUi(appComponent)
                }
            }
        }
    }
}

@Composable
private fun MainUi(appComponent: com.example.day.app.di.AppComponent) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.GroupChoice) }
    var selectedGroupId by remember { mutableLongStateOf(0L) }
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab.index,
                        onClick = { selectedTab = tab.index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> {
                // Home tab — существующая навигация по чатам
                when (val screen = currentScreen) {
                    is Screen.GroupChoice -> {
                        val groupChoiceEntry = appComponent.getGroupChoiceFeatureEntry()
                        groupChoiceEntry.EntryPoint(
                            onGroupSelected = { groupId ->
                                selectedGroupId = groupId
                                currentScreen = Screen.Chats(groupId)
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }

                    is Screen.Chats -> {
                        val chatsEntry = appComponent.getChatFeatureEntry()
                        chatsEntry.EntryPoint(
                            groupId = screen.groupId,
                            onNavigateBack = { currentScreen = Screen.GroupChoice },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }

            1 -> {
                // Settings tab — MCP-серверы
                val mcpEntry = appComponent.getMcpSettingsFeatureEntry()
                mcpEntry.EntryPoint(
                    onNavigateBack = { selectedTab = 0 },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

private sealed interface Screen {
    data object GroupChoice : Screen
    data class Chats(val groupId: Long) : Screen
}

private enum class MainTab(val index: Int, val label: String, val icon: ImageVector) {
    HOME(0, "Главная", Icons.Default.Home),
    SETTINGS(1, "MCP", Icons.Default.Settings)
}
