package com.example.day.core.app_settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.day.core.core_features.llm.domain.model.ModelSettings
import com.example.day.core.core_features.pr_review.data.PrModelSettingsDto
import com.example.day.core.core_features.pr_review.data.toDto
import com.example.day.core.core_features.pr_review.data.toDomain
import com.example.day.core.core_features.pr_review.domain.model.PrHandleState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class AppSettings @Inject constructor(
    private val context: Context,
    private val json: Json
) {

    val localServerUrl: Flow<String> = context.appSettingsDataStore.data.map { prefs ->
        prefs[LOCAL_SERVER_URL_KEY] ?: DEFAULT_LOCAL_SERVER_URL
    }

    suspend fun setLocalServerUrl(url: String) {
        context.appSettingsDataStore.edit { it[LOCAL_SERVER_URL_KEY] = url }
    }

    val prHandleStateFlow: Flow<PrHandleState> = context.appSettingsDataStore.data.map { prefs ->
        val modelSettingsJson = prefs[HANDLE_PR_MODEL_SETTINGS_KEY]
        val modelSettings = modelSettingsJson
            ?.let { runCatching { json.decodeFromString<PrModelSettingsDto>(it).toDomain() }.getOrNull() }
        PrHandleState(
            isEnabled = prefs[HANDLE_PR_ENABLED_KEY] ?: false,
            chatId = prefs[HANDLE_PR_CHAT_ID_KEY] ?: -1L,
            modelSettings = modelSettings
        )
    }

    suspend fun setPrHandleState(isEnabled: Boolean, chatId: Long, modelSettings: ModelSettings?) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[HANDLE_PR_ENABLED_KEY] = isEnabled
            prefs[HANDLE_PR_CHAT_ID_KEY] = chatId
            if (modelSettings != null) {
                prefs[HANDLE_PR_MODEL_SETTINGS_KEY] = json.encodeToString(modelSettings.toDto())
            } else {
                prefs.remove(HANDLE_PR_MODEL_SETTINGS_KEY)
            }
        }
    }

    suspend fun getLastTelegramUpdateId(): Long {
        return context.appSettingsDataStore.data.first()[TELEGRAM_LAST_UPDATE_ID_KEY] ?: 0L
    }

    suspend fun saveLastTelegramUpdateId(id: Long) {
        context.appSettingsDataStore.edit { it[TELEGRAM_LAST_UPDATE_ID_KEY] = id }
    }

    companion object {
        private val LOCAL_SERVER_URL_KEY = stringPreferencesKey("local_server_url")
        const val DEFAULT_LOCAL_SERVER_URL = "http://10.0.2.2:8081"

        private val HANDLE_PR_ENABLED_KEY = booleanPreferencesKey("handle_pr_enabled")
        private val HANDLE_PR_CHAT_ID_KEY = longPreferencesKey("handle_pr_chat_id")
        private val TELEGRAM_LAST_UPDATE_ID_KEY = longPreferencesKey("telegram_last_update_id")
        private val HANDLE_PR_MODEL_SETTINGS_KEY = stringPreferencesKey("handle_pr_model_settings")
    }
}
