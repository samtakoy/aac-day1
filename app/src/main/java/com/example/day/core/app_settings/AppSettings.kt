package com.example.day.core.app_settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class AppSettings @Inject constructor(private val context: Context) {

    val localServerUrl: Flow<String> = context.appSettingsDataStore.data.map { prefs ->
        prefs[LOCAL_SERVER_URL_KEY] ?: DEFAULT_LOCAL_SERVER_URL
    }

    suspend fun setLocalServerUrl(url: String) {
        context.appSettingsDataStore.edit { it[LOCAL_SERVER_URL_KEY] = url }
    }

    companion object {
        private val LOCAL_SERVER_URL_KEY = stringPreferencesKey("local_server_url")
        const val DEFAULT_LOCAL_SERVER_URL = "http://10.0.2.2:8081"
    }
}
