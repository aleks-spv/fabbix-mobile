package com.fabbixmb.app.presentation.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fabbixmb.app.data.local.AppPreferences
import com.fabbixmb.app.data.local.SecurePreferences
import com.fabbixmb.app.domain.model.HostAvailability
import com.fabbixmb.app.domain.model.Problem
import com.fabbixmb.app.domain.model.Severity
import com.fabbixmb.app.domain.repository.ServerRepository
import com.fabbixmb.app.domain.repository.ZabbixRepository
import com.fabbixmb.app.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OverviewData(
    val totalProblems: Int,
    val severityCounts: Map<Severity, Int>,
    val hostsAvailable: Int,
    val hostsUnavailable: Int,
    val hostsUnknown: Int,
    val totalHosts: Int,
    val recentProblems: List<Problem>
)

@HiltViewModel
class OverviewViewModel @Inject constructor(
    private val zabbixRepo: ZabbixRepository,
    private val serverRepo: ServerRepository,
    private val securePrefs: SecurePreferences,
    private val appPrefs: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<OverviewData>>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    init { load() }

    fun load() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val serverId = appPrefs.activeServerId.first() ?: return@launch
            val server = serverRepo.getById(serverId) ?: return@launch
            val token = securePrefs.getToken(serverId) ?: run {
                _uiState.value = UiState.Error("SESSION_EXPIRED"); return@launch
            }

            val problemsDeferred = async {
                zabbixRepo.getProblems(server.url, server.ignoreSsl, token)
            }
            val hostsDeferred = async {
                zabbixRepo.getHosts(server.url, server.ignoreSsl, token)
            }

            val problemsResult = problemsDeferred.await()
            val hostsResult = hostsDeferred.await()

            val error = problemsResult.exceptionOrNull() ?: hostsResult.exceptionOrNull()
            if (error != null) {
                if (error.message?.contains("permission", ignoreCase = true) == true) {
                    _uiState.value = UiState.Error("SESSION_EXPIRED")
                } else {
                    _uiState.value = UiState.Error(error.message ?: "Error")
                }
                return@launch
            }

            val problems = problemsResult.getOrDefault(emptyList())
            val hosts = hostsResult.getOrDefault(emptyList())

            val enabledHosts = hosts.filter { it.enabled }

            _uiState.value = UiState.Success(OverviewData(
                totalProblems = problems.size,
                severityCounts = Severity.entries
                    .filter { it != Severity.NOT_CLASSIFIED }
                    .associateWith { sev -> problems.count { it.severity == sev } }
                    .filter { it.value > 0 },
                hostsAvailable = enabledHosts.count { it.available == HostAvailability.AVAILABLE },
                hostsUnavailable = enabledHosts.count { it.available == HostAvailability.UNAVAILABLE },
                hostsUnknown = enabledHosts.count { it.available == HostAvailability.UNKNOWN },
                totalHosts = enabledHosts.size,
                recentProblems = problems.sortedByDescending { it.clock }.take(5)
            ))
        }
    }

    suspend fun getActiveServerId(): Int? = appPrefs.activeServerId.first()
}