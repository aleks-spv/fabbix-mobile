package com.fabbixmb.app.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fabbixmb.app.data.local.AppPreferences
import com.fabbixmb.app.data.local.SecurePreferences
import com.fabbixmb.app.domain.repository.ServerRepository
import com.fabbixmb.app.domain.repository.ZabbixRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginState(
    val serverId: Int = 0,
    val serverName: String = "",
    val serverUrl: String = "",
    val ignoreSsl: Boolean = false,
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isAutoLogging: Boolean = false,
    val error: String? = null,
    val loginSuccess: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val serverRepo: ServerRepository,
    private val securePrefs: SecurePreferences,
    private val zabbixRepo: ZabbixRepository,
    private val appPrefs: AppPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state = _state.asStateFlow()

    fun init(serverId: Int) {
        viewModelScope.launch {
            val server = serverRepo.getById(serverId) ?: return@launch
            val savedPassword = securePrefs.getPassword(serverId)
            _state.value = LoginState(
                serverId = serverId,
                serverName = server.name,
                serverUrl = server.url,
                ignoreSsl = server.ignoreSsl,
                username = server.username,
                password = savedPassword ?: ""
            )
            if (savedPassword != null) {
                _state.update { it.copy(isAutoLogging = true) }
                doLogin()
            }
        }
    }

    fun setUsername(v: String) { _state.update { it.copy(username = v) } }
    fun setPassword(v: String) { _state.update { it.copy(password = v) } }

    fun login() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch { doLogin() }
    }

    private suspend fun doLogin() {
        val s = _state.value
        zabbixRepo.login(s.serverUrl, s.ignoreSsl, s.username, s.password)
            .onSuccess { token ->
                securePrefs.saveToken(s.serverId, token)
                securePrefs.savePassword(s.serverId, s.password)
                appPrefs.setActiveServer(s.serverId)
                _state.update { it.copy(isLoading = false, isAutoLogging = false, loginSuccess = true) }
            }
            .onFailure { e ->
                _state.update { it.copy(
                    isLoading = false, isAutoLogging = false,
                    error = e.message ?: "Login failed"
                ) }
            }
    }
}