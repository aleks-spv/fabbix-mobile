package com.fabbixmb.app.presentation.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fabbixmb.app.R
import com.fabbixmb.app.data.local.AppPreferences
import com.fabbixmb.app.data.local.ServerEntity
import com.fabbixmb.app.domain.repository.ServerRepository
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServerExportData(
    val version: Int = 1,
    val exportDate: String = "",
    val servers: List<ServerExportEntry> = emptyList()
)

data class ServerExportEntry(
    val name: String = "",
    val url: String = "",
    val username: String = "",
    val ignoreSsl: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPrefs: AppPreferences,
    private val serverRepo: ServerRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val refreshInterval = appPrefs.refreshIntervalMs

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    fun setRefreshInterval(ms: Long) {
        viewModelScope.launch { appPrefs.setRefreshInterval(ms) }
    }

    fun exportServers(uri: Uri) {
        viewModelScope.launch {
            try {
                val servers = serverRepo.getAll().first()
                val data = ServerExportData(
                    exportDate = java.time.Instant.now().toString(),
                    servers = servers.map {
                        ServerExportEntry(it.name, it.url, it.username, it.ignoreSsl)
                    }
                )
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(Gson().toJson(data).toByteArray())
                }
                _message.value = context.getString(R.string.export_success, servers.size)
            } catch (e: Exception) {
                _message.value = context.getString(R.string.export_error, e.message ?: "")
            }
        }
    }

    fun importServers(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use {
                    it.bufferedReader().readText()
                } ?: throw Exception("Cannot read file")

                val data = Gson().fromJson(json, ServerExportData::class.java)
                var imported = 0
                for (entry in data.servers) {
                    serverRepo.add(ServerEntity(
                        name = entry.name,
                        url = entry.url,
                        username = entry.username,
                        hasPassword = false,
                        ignoreSsl = entry.ignoreSsl
                    ))
                    imported++
                }
                _message.value = context.getString(R.string.import_success, imported)
            } catch (e: Exception) {
                _message.value = context.getString(R.string.import_error, e.message ?: "")
            }
        }
    }

    fun clearMessage() { _message.value = null }
}