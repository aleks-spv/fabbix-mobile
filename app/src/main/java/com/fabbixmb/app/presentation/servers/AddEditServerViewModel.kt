package com.fabbixmb.app.presentation.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fabbixmb.app.data.local.SecurePreferences
import com.fabbixmb.app.data.local.ServerEntity
import com.fabbixmb.app.domain.repository.ServerRepository
import com.fabbixmb.app.domain.repository.ZabbixRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditState(
    val id: Int? = null,
    val name: String = "",
    val url: String = "",
    val username: String = "",
    val password: String = "",
    val ignoreSsl: Boolean = false,
    val isTesting: Boolean = false,
    val testResult: String? = null,
    val testSuccess: Boolean = false
)

@HiltViewModel
class AddEditServerViewModel @Inject constructor(
    private val serverRepo: ServerRepository,
    private val securePrefs: SecurePreferences,
    private val zabbixRepo: ZabbixRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditState())
    val state = _state.asStateFlow()

    fun loadServer(id: Int) {
        viewModelScope.launch {
            val entity = serverRepo.getById(id) ?: return@launch
            _state.value = AddEditState(
                id = entity.id,
                name = entity.name,
                url = entity.url,
                username = entity.username,
                password = securePrefs.getPassword(entity.id) ?: "",
                ignoreSsl = entity.ignoreSsl
            )
        }
    }

    fun setName(v: String) { _state.update { it.copy(name = v) } }
    fun setUrl(v: String) { _state.update { it.copy(url = v) } }
    fun setUsername(v: String) { _state.update { it.copy(username = v) } }
    fun setPassword(v: String) { _state.update { it.copy(password = v) } }
    fun setIgnoreSsl(v: Boolean) { _state.update { it.copy(ignoreSsl = v) } }

    fun testConnection() {
        val s = _state.value
        _state.update { it.copy(isTesting = true, testResult = null) }
        viewModelScope.launch {
            val url = normalizeUrl(s.url)
            zabbixRepo.testConnection(url, s.ignoreSsl)
                .onSuccess { version ->
                    _state.update { it.copy(isTesting = false, testResult = "Zabbix $version", testSuccess = true) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isTesting = false, testResult = e.message ?: "Error", testSuccess = false) }
                }
        }
    }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        viewModelScope.launch {
            val url = normalizeUrl(s.url)
            val entity = ServerEntity(
                id = s.id ?: 0,
                name = s.name, url = url, username = s.username,
                hasPassword = s.password.isNotBlank(), ignoreSsl = s.ignoreSsl
            )
            val id = if (s.id != null) {
                serverRepo.update(entity)
                s.id
            } else {
                serverRepo.add(entity).toInt()
            }
            if (s.password.isNotBlank()) securePrefs.savePassword(id, s.password)
            onDone()
        }
    }

    private fun normalizeUrl(url: String): String =
        url.trimEnd('/').removeSuffix("/api_jsonrpc.php")
}