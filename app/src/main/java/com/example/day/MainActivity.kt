package com.example.day

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.day.app.MyApp
import com.example.day.app.di.LocalAppComponent
import com.example.day.core.ui.theme.Day1Theme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
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
                    onNavigateBack = {
                        currentScreen = Screen.GroupChoice
                    },
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
