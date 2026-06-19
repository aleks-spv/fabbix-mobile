package com.fabbixmb.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

@Singleton
class AppPreferences @Inject constructor(@ApplicationContext context: Context) {

    private val store = context.dataStore

    val activeServerId: Flow<Int?> = store.data.map { it[KEY_ACTIVE_SERVER] }
    val refreshIntervalMs: Flow<Long> = store.data.map { it[KEY_REFRESH_INTERVAL] ?: 60_000L }

    suspend fun setActiveServer(serverId: Int) {
        store.edit { it[KEY_ACTIVE_SERVER] = serverId }
    }

    suspend fun clearActiveServer() {
        store.edit { it.remove(KEY_ACTIVE_SERVER) }
    }

    suspend fun setRefreshInterval(ms: Long) {
        store.edit { it[KEY_REFRESH_INTERVAL] = ms }
    }

    companion object {
        private val KEY_ACTIVE_SERVER = intPreferencesKey("active_server_id")
        private val KEY_REFRESH_INTERVAL = longPreferencesKey("refresh_interval_ms")
    }
}