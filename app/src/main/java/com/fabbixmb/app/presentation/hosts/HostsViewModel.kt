package com.fabbixmb.app.presentation.hosts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fabbixmb.app.data.local.AppPreferences
import com.fabbixmb.app.data.local.SecurePreferences
import com.fabbixmb.app.domain.model.Host
import com.fabbixmb.app.domain.repository.ServerRepository
import com.fabbixmb.app.domain.repository.ZabbixRepository
import com.fabbixmb.app.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HostsViewModel @Inject constructor(
    private val zabbixRepo: ZabbixRepository,
    private val serverRepo: ServerRepository,
    private val securePrefs: SecurePreferences,
    private val appPrefs: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<Host>>>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    private var allHosts: List<Host> = emptyList()

    init { loadHosts() }

    fun loadHosts() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val serverId = appPrefs.activeServerId.first() ?: return@launch
            val server = serverRepo.getById(serverId) ?: return@launch
            val token = securePrefs.getToken(serverId) ?: run {
                _uiState.value = UiState.Error("SESSION_EXPIRED"); return@launch
            }
            zabbixRepo.getHosts(server.url, server.ignoreSsl, token)
                .onSuccess { hosts -> allHosts = hosts; applyFilter() }
                .onFailure { e ->
                    if (e.message?.contains("permission", ignoreCase = true) == true) {
                        _uiState.value = UiState.Error("SESSION_EXPIRED")
                    } else {
                        _uiState.value = UiState.Error(e.message ?: "Error")
                    }
                }
        }
    }

    fun setSearchQuery(q: String) { _searchQuery.value = q; applyFilter() }

    private fun applyFilter() {
        val q = _searchQuery.value.lowercase()
        val filtered = if (q.isBlank()) allHosts
        else allHosts.filter { it.visibleName.lowercase().contains(q) || it.technicalName.lowercase().contains(q) }
        _uiState.value = UiState.Success(filtered)
    }

    suspend fun getActiveServerId(): Int? = appPrefs.activeServerId.first()
}