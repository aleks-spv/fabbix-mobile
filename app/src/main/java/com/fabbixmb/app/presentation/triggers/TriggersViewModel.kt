package com.fabbixmb.app.presentation.triggers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fabbixmb.app.data.local.AppPreferences
import com.fabbixmb.app.data.local.SecurePreferences
import com.fabbixmb.app.domain.model.Severity
import com.fabbixmb.app.domain.model.Trigger
import com.fabbixmb.app.domain.repository.ServerRepository
import com.fabbixmb.app.domain.repository.ZabbixRepository
import com.fabbixmb.app.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TriggersViewModel @Inject constructor(
    private val zabbixRepo: ZabbixRepository,
    private val serverRepo: ServerRepository,
    private val securePrefs: SecurePreferences,
    private val appPrefs: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<Trigger>>>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private var allTriggers: List<Trigger> = emptyList()

    init { loadTriggers() }

    fun loadTriggers(onDone: (() -> Unit)? = null) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val serverId = appPrefs.activeServerId.first() ?: return@launch
            val server = serverRepo.getById(serverId) ?: return@launch
            val token = securePrefs.getToken(serverId)
            if (token == null) { _uiState.value = UiState.Error("No auth token"); return@launch }

            zabbixRepo.getTriggers(server.url, server.ignoreSsl, token)
                .onSuccess { triggers ->
                    allTriggers = triggers
                    applyFilter()
                    onDone?.invoke()
                }
                .onFailure { e ->
                    if (e.message?.contains("permission", ignoreCase = true) == true) {
                        _uiState.value = UiState.Error("SESSION_EXPIRED")
                        onDone?.invoke()
                    } else {
                        _uiState.value = UiState.Error(e.message ?: "Error")
                        onDone?.invoke()
                    }
                }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilter()
    }

    private fun applyFilter() {
        val q = _searchQuery.value.lowercase()
        val filtered = if (q.isBlank()) allTriggers
        else allTriggers.filter { it.description.lowercase().contains(q) }
        _uiState.value = UiState.Success(filtered)
    }

    fun startAutoRefresh() {
        viewModelScope.launch {
            appPrefs.refreshIntervalMs.collectLatest { interval ->
                while (true) {
                    delay(interval)
                    loadTriggers()
                }
            }
        }
    }

    suspend fun getActiveServerId(): Int? = appPrefs.activeServerId.first()
}