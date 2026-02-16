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
import androidx.compose.ui.Modifier
import com.example.day.app.MyApp
import com.example.day.app.di.LocalAppComponent
import com.example.day.core.feature_entries.FeatureEntry
import com.example.day.core.feature_entries.find
import com.example.day.features.console.api.ConsoleFeatureEntry
import com.example.day.core.ui.theme.Day1Theme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appComponent = (application as MyApp).appComponent
        val featureEntries = appComponent.getFeatureEntries()
        val mainEntry = featureEntries.find<ConsoleFeatureEntry>()

        enableEdgeToEdge()
        setContent {
            Day1Theme {
                CompositionLocalProvider(LocalAppComponent provides appComponent) {
                    MainUi(mainEntry)
                }
            }
        }
    }

    @Composable
    private fun MainUi(mainEntry: FeatureEntry) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            mainEntry.ComposableEntryPoint(
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
