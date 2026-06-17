package com.fabbixmb.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fabbixmb.app.data.local.AppPreferences
import com.fabbixmb.app.data.local.SecurePreferences
import com.fabbixmb.app.domain.model.Dashboard
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
class DashboardViewModel @Inject constructor(
    private val zabbixRepo: ZabbixRepository,
    private val serverRepo: ServerRepository,
    private val securePrefs: SecurePreferences,
    private val appPrefs: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<Dashboard>>>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    init { loadDashboards() }

    fun loadDashboards() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val serverId = appPrefs.activeServerId.first() ?: return@launch
            val server = serverRepo.getById(serverId) ?: return@launch
            val token = securePrefs.getToken(serverId) ?: run {
                _uiState.value = UiState.Error("SESSION_EXPIRED"); return@launch
            }
            zabbixRepo.getDashboards(server.url, server.ignoreSsl, token)
                .onSuccess { dashboards -> _uiState.value = UiState.Success(dashboards) }
                .onFailure { e ->
                    if (e.message?.contains("permission", ignoreCase = true) == true) {
                        _uiState.value = UiState.Error("SESSION_EXPIRED")
                    } else {
                        _uiState.value = UiState.Error(e.message ?: "Error")
                    }
                }
        }
    }

    suspend fun getActiveServerId(): Int? = appPrefs.activeServerId.first()
}