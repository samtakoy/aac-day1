package com.example.day.core.core_features.mcp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.mcpDataStore: DataStore<Preferences> by preferencesDataStore(name = "mcp_secrets")

/**
 * Secure vault for MCP auth tokens.
 * Uses DataStore so tokens survive app restarts.
 * Tokens are stored under a key derived from serverId and never hardcoded.
 */
@Singleton
internal class SecretsVault @Inject constructor(
    private val context: Context
) {
    /** Persist a token for the given server */
    suspend fun saveToken(serverId: String, token: String) {
        context.mcpDataStore.edit { prefs ->
            prefs[tokenKey(serverId)] = token
        }
    }

    /** Retrieve a token (null if not set) */
    suspend fun getToken(serverId: String): String? =
        context.mcpDataStore.data.map { prefs -> prefs[tokenKey(serverId)] }.first()

    /** Delete token when server is removed */
    suspend fun deleteToken(serverId: String) {
        context.mcpDataStore.edit { prefs -> prefs.remove(tokenKey(serverId)) }
    }

    private fun tokenKey(serverId: String) = stringPreferencesKey("mcp_token_$serverId")
}
